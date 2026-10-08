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
            val bytesPerFrame = info.channels * WavInfo.BYTES_PER_SAMPLE
            val totalMillis = info.frames * MILLIS / info.sampleRate
            val from = startMillis.coerceIn(0L, totalMillis)
            val to = endMillis.coerceIn(0L, totalMillis)
            if (to <= from) return ClipResult.Failed("intervalo vazio")
            val firstFrame = from * info.sampleRate / MILLIS
            val lastFrame = (to * info.sampleRate / MILLIS).coerceAtMost(info.frames)
            val bytes = (lastFrame - firstFrame) * bytesPerFrame
            if (bytes <= 0) return ClipResult.Failed("intervalo vazio")
            destination.parentFile?.mkdirs()
            RandomAccessFile(source, "r").use { input ->
                input.seek(info.dataOffset + firstFrame * bytesPerFrame)
                tmp.outputStream().buffered(BUFFER).use { out ->
                    out.write(WavHeaderWriter.build(info.sampleRate, info.channels, WavInfo.BITS, bytes))
                    val buf = ByteArray(BUFFER)
                    var left = bytes
                    while (left > 0) {
                        val n = input.read(buf, 0, minOf(left, buf.size.toLong()).toInt())
                        if (n < 0) throw IOException("arquivo menor que o cabecalho declara")
                        out.write(buf, 0, n)
                        left -= n
                    }
                }
            }
            java.nio.file.Files.move(tmp.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            ClipResult.Done(destination, (lastFrame - firstFrame) * MILLIS / info.sampleRate)
        } catch (e: IOException) {
            tmp.delete()
            ClipResult.Failed(e.message ?: "erro de E/S")
        } catch (e: IllegalArgumentException) {
            tmp.delete()
            ClipResult.Failed(e.message ?: "arquivo invalido")
        }
    }

    private const val MILLIS = 1000L
    private const val BUFFER = 1 shl 16
}
