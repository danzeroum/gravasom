package com.gravasom.core.service.enhance

import com.gravasom.core.audio.engine.WavHeaderWriter
import com.gravasom.core.audio.engine.edit.WavReader
import com.gravasom.core.service.analysis.PcmDecoder
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
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
        val sink = WavSink(tmp.outputStream(), cancelled)
        val ok = try {
            sink.reserveHeader()
            PcmDecoder.decode(source, sink::onFormat, sink::write, sink::stopped)
        } catch (_: IOException) {
            false
        } catch (_: RuntimeException) {
            false // arquivo que o MediaCodec/MediaExtractor nao entende
        } finally {
            runCatching { sink.close() }
        }
        if (!ok || !sink.valid || sink.bytes == 0L) {
            tmp.delete()
            return null
        }
        return finalize(tmp, destination, sink)
    }

    /** Escreve o cabecalho definitivo (taxa, canais e tamanho agora conhecidos) e renomeia para o destino. */
    private fun finalize(tmp: File, destination: File, sink: WavSink): DecodedWav? {
        RandomAccessFile(tmp, "rw").use { it.write(WavHeaderWriter.build(sink.rate, sink.channels, 16, sink.bytes)) }
        if (!tmp.renameTo(destination)) {
            tmp.delete()
            return null
        }
        return DecodedWav(destination, sink.rate, sink.channels, sink.frames)
    }

    /** WAV PCM16 mono/estereo ja serve como esta: nao copia (o arquivo e so lido). */
    private fun copyOrReference(source: File, destination: File): DecodedWav? = runCatching {
        val info = WavReader.read(source)
        DecodedWav(source, info.sampleRate, info.channels, info.frames)
    }.getOrNull().also { destination.delete() }

    private fun isPlainWav(file: File): Boolean =
        file.name.endsWith(".wav", ignoreCase = true) && runCatching { WavReader.read(file) }.isSuccess

    /**
     * Recebe o PCM decodificado e grava os bytes little-endian, contando o que saiu. Recusa (e para de escrever) se a taxa ou os
     * canais mudarem no meio do fluxo, se vierem mais de 2 canais, ou se [cancelled] disparar.
     */
    private class WavSink(raw: OutputStream, private val cancelled: () -> Boolean) {
        private val out = BufferedOutputStream(raw, BUFFER)
        var rate = 0
            private set
        var channels = 0
            private set
        var bytes = 0L
            private set
        private var bad = false

        val frames: Long get() = bytes / (BYTES_PER_SAMPLE * channels)
        val valid: Boolean get() = !bad && rate > 0

        /** Reserva o espaco do cabecalho: so se sabe o tamanho no fim. */
        fun reserveHeader() {
            out.write(ByteArray(WavHeaderWriter.HEADER_SIZE))
        }

        /** A primeira chamada fixa o formato; mudanca no meio do fluxo (ou mais de 2 canais) marca o fluxo como ruim. */
        fun onFormat(r: Int, c: Int) {
            if (rate != 0 && (r != rate || c != channels)) bad = true // taxa ou canais mudaram no meio do fluxo
            if (rate == 0) {
                rate = r
                channels = c
            }
            if (c !in 1..2) bad = true
        }

        fun write(pcm: ShortArray, length: Int) {
            if (bad) return
            val buf = ByteArray(length * BYTES_PER_SAMPLE)
            for (i in 0 until length) {
                buf[2 * i] = (pcm[i].toInt() and 0xFF).toByte()
                buf[2 * i + 1] = ((pcm[i].toInt() shr 8) and 0xFF).toByte()
            }
            out.write(buf)
            bytes += buf.size
        }

        fun stopped() = bad || cancelled()

        fun close() = out.close()
    }
}
