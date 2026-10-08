package com.gravasom.core.service.enhance

import com.gravasom.core.audio.engine.WavHeaderWriter
import com.gravasom.core.audio.engine.edit.WavReader
import com.gravasom.core.service.analysis.PcmDecoder
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/** WAV PCM16 produzido a partir de qualquer audio que o app toca. */
class DecodedWav(val file: File, val sampleRate: Int, val channels: Int, val frames: Long) {
    val durationMillis: Long get() = frames * MILLIS_PER_SECOND / sampleRate

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}

/**
 * Decodifica M4A/MP3/WAV para um WAV PCM16 na TAXA E NOS CANAIS ORIGINAIS (o redutor e a normalizacao trabalham sobre WAV; a
 * conversao de volta nao existe: a copia melhorada e um WAV). Em streaming, sem guardar o audio na memoria. Mono ou estereo.
 */
object AudioToWav {

    private const val BYTES_PER_SAMPLE = 2
    private const val BUFFER = 1 shl 16

    /** null se nao decodifica, tem mais de 2 canais, muda de formato no meio ou foi cancelado (o destino nao fica pela metade). */
    fun convert(source: File, destination: File, cancelled: () -> Boolean = { false }): DecodedWav? {
        if (isPlainWav(source)) return copyOrReference(source, destination)
        val tmp = File(destination.path + ".tmp")
        destination.parentFile?.mkdirs()
        var rate = 0
        var channels = 0
        var bad = false
        var bytes = 0L
        val out = BufferedOutputStream(tmp.outputStream(), BUFFER)
        val ok = try {
            out.write(ByteArray(WavHeaderWriter.HEADER_SIZE)) // reserva o cabecalho: so se sabe o tamanho no fim
            PcmDecoder.decode(
                source,
                { r, c ->
                    if (rate == 0) {
                        rate = r
                        channels = c
                        if (c !in 1..2) bad = true
                    } else if (r != rate || c != channels) {
                        bad = true // taxa ou canais mudaram no meio do fluxo
                    }
                },
                { pcm, length ->
                    if (!bad) {
                        val buf = ByteArray(length * BYTES_PER_SAMPLE)
                        for (i in 0 until length) {
                            buf[2 * i] = (pcm[i].toInt() and 0xFF).toByte()
                            buf[2 * i + 1] = ((pcm[i].toInt() shr 8) and 0xFF).toByte()
                        }
                        out.write(buf)
                        bytes += buf.size
                    }
                },
                { bad || cancelled() },
            )
        } catch (e: IOException) {
            false
        } catch (e: RuntimeException) {
            false // arquivo que o MediaCodec/MediaExtractor nao entende
        } finally {
            runCatching { out.close() }
        }
        if (!ok || bad || rate == 0 || bytes == 0L) {
            tmp.delete()
            return null
        }
        RandomAccessFile(tmp, "rw").use { it.write(WavHeaderWriter.build(rate, channels, 16, bytes)) }
        if (!tmp.renameTo(destination)) {
            tmp.delete()
            return null
        }
        return DecodedWav(destination, rate, channels, bytes / (BYTES_PER_SAMPLE * channels))
    }

    /** WAV PCM16 mono/estereo ja serve como esta: nao copia (o arquivo e so lido). */
    private fun copyOrReference(source: File, destination: File): DecodedWav? = runCatching {
        val info = WavReader.read(source)
        DecodedWav(source, info.sampleRate, info.channels, info.frames)
    }.getOrNull().also { destination.delete() }

    private fun isPlainWav(file: File): Boolean =
        file.name.endsWith(".wav", ignoreCase = true) && runCatching { WavReader.read(file) }.isSuccess
}
