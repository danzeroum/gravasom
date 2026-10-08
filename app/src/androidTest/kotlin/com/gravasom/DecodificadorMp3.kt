package com.gravasom

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.gravasom.core.audio.engine.WavHeaderWriter
import java.io.ByteArrayOutputStream
import java.io.File

/** Decodifica um arquivo de audio comprimido para PCM 16 bits com o MediaCodec do sistema. */
object DecodificadorMp3 {

    class Pcm(val amostras: ShortArray, val taxa: Int, val canais: Int)

    fun decodificar(arquivo: File): Pcm {
        val extractor = MediaExtractor().apply { setDataSource(arquivo.path) }
        try {
            val faixa = (0 until extractor.trackCount).first {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("audio/")
            }
            extractor.selectTrack(faixa)
            val formato = extractor.getTrackFormat(faixa)
            val codec = MediaCodec.createDecoderByType(formato.getString(MediaFormat.KEY_MIME)!!)
            codec.configure(formato, null, null, 0)
            codec.start()
            val saida = ByteArrayOutputStream()
            val info = MediaCodec.BufferInfo()
            var fimEntrada = false
            var fimSaida = false
            var taxa = formato.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var canais = formato.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            while (!fimSaida) {
                if (!fimEntrada) {
                    val i = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (i >= 0) {
                        val buf = codec.getInputBuffer(i)!!
                        val n = extractor.readSampleData(buf, 0)
                        if (n < 0) {
                            codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            fimEntrada = true
                        } else {
                            codec.queueInputBuffer(i, 0, n, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val o = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                if (o >= 0) {
                    val buf = codec.getOutputBuffer(o)!!
                    val bytes = ByteArray(info.size)
                    buf.get(bytes)
                    saida.write(bytes)
                    codec.releaseOutputBuffer(o, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) fimSaida = true
                } else if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val novo = codec.outputFormat
                    taxa = novo.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    canais = novo.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }
            codec.stop()
            codec.release()
            val b = saida.toByteArray()
            val pcm = ShortArray(b.size / 2) { ((b[2 * it].toInt() and 0xFF) or (b[2 * it + 1].toInt() shl 8)).toShort() }
            return Pcm(pcm, taxa, canais)
        } finally {
            extractor.release()
        }
    }

    /** Grava o PCM decodificado como WAV (entrada do audio-suite no CI). */
    fun salvarWav(pcm: Pcm, destino: File): File {
        destino.parentFile?.mkdirs()
        val dados = ByteArray(pcm.amostras.size * 2)
        pcm.amostras.forEachIndexed { i, s ->
            dados[2 * i] = (s.toInt() and 0xFF).toByte()
            dados[2 * i + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
        }
        destino.outputStream().use {
            it.write(WavHeaderWriter.build(pcm.taxa, pcm.canais, 16, dados.size.toLong()))
            it.write(dados)
        }
        return destino
    }

    private const val TIMEOUT_US = 10_000L
}
