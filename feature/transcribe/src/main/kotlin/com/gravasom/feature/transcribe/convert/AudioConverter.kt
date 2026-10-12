package com.gravasom.feature.transcribe.convert

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.gravasom.core.audio.engine.analysis.Resampler16k
import com.gravasom.core.audio.engine.edit.WavReader
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

/**
 * Decodifica M4A/AAC/MP3/WAV para PCM 16 bits mono 16 kHz em um arquivo temporario, em streaming
 * (bloco por bloco), para nao carregar arquivos longos na memoria (ADR-016 secao 6).
 *
 * Formato de saida: PCM cru 16 bits little-endian, mono 16 kHz. O transcriber le esse arquivo
 * direto (sem cabecalho WAV) para nao pagar decodificacao de cabecalho em arquivos grandes.
 *
 * Cancelavel: [cancelado] e checado a cada bloco; o arquivo parcial e apagado por quem chamou.
 */
class AudioConverter(
    private val cancelado: () -> Boolean = { false },
) {

    /**
     * Converte [entrada] para PCM 16 kHz mono 16 bits em [saida]. Devolve os milissegundos de audio
     * convertido. Se [entrada] for WAV PCM 16 bits, decodifica sem MediaCodec.
     */
    fun converter(entrada: File, saida: File): Long {
        if (ehWav(entrada)) return converterWav(entrada, saida)
        return converterMedia(entrada, saida)
    }

    private fun ehWav(arquivo: File): Boolean = runCatching {
        WavReader.read(arquivo)
        true
    }.getOrDefault(false)

    /** WAV 16 bits -> PCM mono 16 kHz. Reusa [WavReader] (aceita mono/estereo) e [Resampler16k]. */
    private fun converterWav(entrada: File, saida: File): Long {
        val info = WavReader.read(entrada)
        val bytesPorFrame = info.channels * 2
        val resampler = Resampler16k(info.sampleRate)
        val buffer = ShortArray(BLOCO_AMOSTRAS)
        val mono = ShortArray(BLOCO_AMOSTRAS)
        val resampleado = ShortArray(resampler.maxOutput(BLOCO_AMOSTRAS))
        var framesTotais = 0L
        RandomAccessFile(entrada, "r").use { raf ->
            saida.outputStream().buffered(1 shl 16).use { out ->
                raf.seek(info.dataOffset)
                var restantes = info.dataBytes
                val cru = ByteArray(BLOCO_AMOSTRAS * bytesPorFrame)
                while (restantes > 0) {
                    checarCancelamento()
                    val ler = minOf(restantes, cru.size.toLong()).toInt() / bytesPorFrame * bytesPorFrame
                    if (ler == 0) break
                    raf.readFully(cru, 0, ler)
                    restantes -= ler
                    val frames = ler / bytesPorFrame
                    val shorts = ByteBuffer.wrap(cru, 0, ler).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    shorts.get(buffer, 0, frames * info.channels)
                    val n = downmix(buffer, frames, info.channels, mono)
                    val produzidos = resampler.process(mono, 0, n, resampleado)
                    escreverShorts(out, resampleado, produzidos)
                    framesTotais += frames
                }
                val cauda = ShortArray(1)
                if (resampler.finish(cauda) == 1) escreverShorts(out, cauda, 1)
            }
        }
        return framesTotais * 1000 / info.sampleRate
    }

    /** M4A/AAC/MP3 -> PCM mono 16 kHz via MediaExtractor + MediaCodec. */
    private fun converterMedia(entrada: File, saida: File): Long {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(entrada.absolutePath)
            val faixa = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IllegalArgumentException("sem faixa de audio")
            val formato = extractor.getTrackFormat(faixa)
            val mime = formato.getString(MediaFormat.KEY_MIME)!!
            extractor.selectTrack(faixa)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(formato, null, null, 0)
            codec.start()

            val taxa = formato.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val canais = formato.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val resampler = Resampler16k(taxa)
            val info = MediaCodec.BufferInfo()
            var framesTotais = 0L
            var entradaEsgotada = false
            var saidaEsgotada = false
            val mono = ShortArray(BLOCO_AMOSTRAS)
            val resampleado = ShortArray(resampler.maxOutput(BLOCO_AMOSTRAS))
            saida.outputStream().buffered(1 shl 16).use { out ->
                while (!saidaEsgotada) {
                    checarCancelamento()
                    if (!entradaEsgotada) {
                        val idx = codec.dequeueInputBuffer(TIMEOUT_US)
                        if (idx >= 0) {
                            val buf = codec.getInputBuffer(idx)!!
                            val n = extractor.readSampleData(buf, 0)
                            if (n < 0) {
                                codec.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                entradaEsgotada = true
                            } else {
                                codec.queueInputBuffer(idx, 0, n, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    var idx = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                    while (idx >= 0) {
                        val buf = codec.getOutputBuffer(idx)!!
                        if (info.size > 0) {
                            val shorts = buf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            val frames = info.size / (2 * canais)
                            val amostras = ShortArray(frames * canais)
                            shorts.get(amostras, 0, amostras.size)
                            val n = downmix(amostras, frames, canais, mono)
                            val produzidos = resampler.process(mono, 0, n, resampleado)
                            escreverShorts(out, resampleado, produzidos)
                            framesTotais += frames
                        }
                        codec.releaseOutputBuffer(idx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            saidaEsgotada = true
                            break
                        }
                        idx = codec.dequeueOutputBuffer(info, 0)
                    }
                }
                val cauda = ShortArray(1)
                if (resampler.finish(cauda) == 1) escreverShorts(out, cauda, 1)
            }
            return framesTotais * 1000 / taxa
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun downmix(entrada: ShortArray, frames: Int, canais: Int, saida: ShortArray): Int {
        if (canais == 1) {
            System.arraycopy(entrada, 0, saida, 0, frames)
            return frames
        }
        for (f in 0 until frames) {
            var soma = 0
            for (c in 0 until canais) soma += entrada[f * canais + c]
            saida[f] = (soma / canais).toShort()
        }
        return frames
    }

    private fun escreverShorts(out: java.io.OutputStream, dados: ShortArray, n: Int) {
        val bytes = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)
        bytes.asShortBuffer().put(dados, 0, n)
        out.write(bytes.array())
    }

    private fun checarCancelamento() {
        if (cancelado()) throw CancellationException("conversao cancelada")
    }

    class CancellationException(message: String) : Exception(message)

    companion object {
        private const val BLOCO_AMOSTRAS = 16 * 1024
        private const val TIMEOUT_US = 10_000L
    }
}
