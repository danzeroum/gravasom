package com.gravasom.core.audio.engine.edit

import com.gravasom.core.audio.engine.WavHeaderWriter
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

sealed interface ExportResult {
    data class Done(val file: File, val samples: Long) : ExportResult
    data object Cancelled : ExportResult
    data class Failed(val reason: String) : ExportResult
}

/**
 * Exporta em streaming (blocos de 10 ms): o original e so lido; o resultado e um arquivo novo,
 * escrito em `.tmp` e renomeado ao fim. Cancelamento ou erro apagam o `.tmp`.
 * Duracao identica em amostras: o atraso (look-ahead) dos processadores e compensado.
 */
class ProjectExporter(private val processorFor: (EditProject, sampleRate: Int) -> AudioBlockProcessor) {

    fun export(
        original: File,
        project: EditProject,
        destination: File,
        cancelled: () -> Boolean = { false },
        onProgress: (Float) -> Unit = {},
    ): ExportResult {
        val tmp = File(destination.path + ".tmp")
        return try {
            destination.parentFile?.mkdirs()
            val info = WavReader.read(original)
            val result = if (project.isIdentity) {
                Files.copy(original.toPath(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING)
                ExportResult.Done(destination, info.frames)
            } else {
                process(original, info, project, tmp, destination, cancelled, onProgress)
            }
            if (result is ExportResult.Done) {
                Files.move(tmp.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } else {
                tmp.delete()
            }
            result
        } catch (e: IOException) {
            tmp.delete()
            ExportResult.Failed(e.message ?: "erro de E/S")
        } catch (e: IllegalArgumentException) {
            tmp.delete()
            ExportResult.Failed(e.message ?: "arquivo invalido")
        }
    }

    private fun process(
        original: File,
        info: WavInfo,
        project: EditProject,
        tmp: File,
        destination: File,
        cancelled: () -> Boolean,
        onProgress: (Float) -> Unit,
    ): ExportResult {
        val processors = List(info.channels) { processorFor(project, info.sampleRate) }
        try {
            if (!stream(original, tmp, info, processors, cancelled, onProgress)) return ExportResult.Cancelled
            return ExportResult.Done(destination, info.frames)
        } finally {
            processors.forEach { it.close() }
        }
    }

    private fun stream(
        original: File,
        tmp: File,
        info: WavInfo,
        processors: List<AudioBlockProcessor>,
        cancelled: () -> Boolean,
        onProgress: (Float) -> Unit,
    ): Boolean {
        val source = original.inputStream().also { it.skip(info.dataOffset) }
        DataInputStream(BufferedInputStream(source, BUFFER)).use { input ->
            BufferedOutputStream(tmp.outputStream(), BUFFER).use { out ->
                out.write(WavHeaderWriter.build(info.sampleRate, info.channels, WavInfo.BITS, info.dataBytes))
                return pump(input, out, info, processors, cancelled, onProgress)
            }
        }
    }

    /** Laco de blocos. Retorna false se cancelado. */
    private fun pump(
        input: DataInputStream,
        out: BufferedOutputStream,
        info: WavInfo,
        processors: List<AudioBlockProcessor>,
        cancelled: () -> Boolean,
        onProgress: (Float) -> Unit,
    ): Boolean {
        val block = info.sampleRate / BLOCKS_PER_SECOND
        val latency = processors.maxOf { it.latencySamples }.toLong()
        val floats = Array(info.channels) { FloatArray(block) }
        val raw = ByteArray(block * info.channels * WavInfo.BYTES_PER_SAMPLE)
        var skip = latency
        var read = 0L
        var written = 0L
        while (written < info.frames) {
            if (cancelled()) return false
            val n = minOf(block.toLong(), info.frames - read + latency).toInt()
            PcmBlocos.ler(input, raw, floats, n, available = (info.frames - read).coerceAtLeast(0))
            read += n
            processors.forEachIndexed { c, p -> p.process(floats[c], n) }
            val descartar = minOf(skip, n.toLong()).toInt()
            skip -= descartar
            val emitir = minOf((n - descartar).toLong(), info.frames - written).toInt()
            PcmBlocos.escrever(out, floats, descartar, emitir)
            written += emitir
            onProgress(written.toFloat() / info.frames)
        }
        return true
    }

    private companion object {
        const val BLOCKS_PER_SECOND = 100
        const val BUFFER = 1 shl 16
    }
}
