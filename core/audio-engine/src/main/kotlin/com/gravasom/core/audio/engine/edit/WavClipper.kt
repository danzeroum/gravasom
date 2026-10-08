package com.gravasom.core.audio.engine.edit

import com.gravasom.core.audio.engine.WavHeaderWriter
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

sealed interface ClipResult {
    data class Done(val file: File, val durationMillis: Long) : ClipResult
    data class Failed(val reason: String) : ClipResult
}

/**
 * Recorta um trecho de um WAV PCM 16 bits para um arquivo NOVO, sem perdas (copia de bytes em quadros inteiros) e em
 * blocos: o original so e lido. Escreve em `.tmp` e renomeia; qualquer falha apaga o `.tmp`.
 */
object WavClipper {

    fun clip(source: File, startMillis: Long, endMillis: Long, destination: File): ClipResult {
        val tmp = File(destination.path + ".tmp")
        return try {
            val info = WavReader.read(source)
            val span = frameSpan(info, startMillis, endMillis)
            if (span == null) {
                ClipResult.Failed("intervalo vazio")
            } else {
                destination.parentFile?.mkdirs()
                copyFrames(source, info, span, tmp)
                java.nio.file.Files.move(tmp.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                ClipResult.Done(destination, span.frames * MILLIS / info.sampleRate)
            }
        } catch (e: IOException) {
            tmp.delete()
            ClipResult.Failed(e.message ?: "erro de E/S")
        } catch (e: IllegalArgumentException) {
            tmp.delete()
            ClipResult.Failed(e.message ?: "arquivo invalido")
        }
    }

    /** Quadros [primeiro, ultimo) que o intervalo cobre, limitados ao arquivo; null se vazio. */
    private fun frameSpan(info: WavInfo, startMillis: Long, endMillis: Long): FrameSpan? {
        val totalMillis = info.frames * MILLIS / info.sampleRate
        val from = startMillis.coerceIn(0L, totalMillis)
        val to = endMillis.coerceIn(0L, totalMillis)
        val first = from * info.sampleRate / MILLIS
        val last = (to * info.sampleRate / MILLIS).coerceAtMost(info.frames)
        return if (to > from && last > first) FrameSpan(first, last) else null
    }

    /** Copia os quadros de [span] para [tmp] com cabecalho novo, em blocos. */
    private fun copyFrames(source: File, info: WavInfo, span: FrameSpan, tmp: File) {
        val bytesPerFrame = info.channels * WavInfo.BYTES_PER_SAMPLE
        val bytes = span.frames * bytesPerFrame
        RandomAccessFile(source, "r").use { input ->
            input.seek(info.dataOffset + span.first * bytesPerFrame)
            tmp.outputStream().buffered(BUFFER).use { out ->
                out.write(WavHeaderWriter.build(info.sampleRate, info.channels, WavInfo.BITS, bytes))
                pump(input, out, bytes)
            }
        }
    }

    private fun pump(input: RandomAccessFile, out: java.io.OutputStream, bytes: Long) {
        val buf = ByteArray(BUFFER)
        var left = bytes
        while (left > 0) {
            val n = input.read(buf, 0, minOf(left, buf.size.toLong()).toInt())
            if (n < 0) throw IOException("arquivo menor que o cabecalho declara")
            out.write(buf, 0, n)
            left -= n
        }
    }

    /** Quadros [first, endExclusive). */
    private class FrameSpan(val first: Long, val endExclusive: Long) {
        val frames: Long get() = endExclusive - first
    }

    private const val MILLIS = 1000L
    private const val BUFFER = 1 shl 16
}
