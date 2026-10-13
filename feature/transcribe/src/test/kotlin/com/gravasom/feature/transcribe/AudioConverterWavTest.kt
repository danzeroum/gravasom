package com.gravasom.feature.transcribe

import com.gravasom.core.audio.engine.WavHeaderWriter
import com.gravasom.feature.transcribe.convert.AudioConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.sin

/**
 * Conversao WAV -> PCM 16 kHz mono 16 bits (o caminho que o worker usa para gravar em WAV).
 * O caminho M4A/MP3 passa por MediaCodec e so roda no instrumentado; aqui o foco e o streaming,
 * a reamostragem e o cancelamento.
 */
class AudioConverterWavTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun wav(segundos: Double, taxa: Int, canais: Int = 1): File {
        val arquivo = tmp.newFile("in.wav")
        val amostras = (segundos * taxa).toInt()
        RandomAccessFile(arquivo, "rw").use { raf ->
            raf.setLength(0)
            raf.write(WavHeaderWriter.build(taxa, canais, 16, amostras * canais * 2L))
            val pcm = ByteArray(amostras * canais * 2)
            for (i in 0 until amostras) {
                val v = (sin(2 * PI * 1000.0 * i / taxa) * 0.5 * Short.MAX_VALUE).toInt()
                for (c in 0 until canais) {
                    val off = (i * canais + c) * 2
                    pcm[off] = (v and 0xFF).toByte()
                    pcm[off + 1] = ((v shr 8) and 0xFF).toByte()
                }
            }
            raf.write(pcm)
        }
        return arquivo
    }

    @Test
    fun `wav 44_1 kHz vira PCM 16 kHz mono do tamanho certo`() {
        val entrada = wav(segundos = 1.0, taxa = 44_100)
        val saida = tmp.newFile("out.pcm")
        val ms = AudioConverter().converter(entrada, saida)
        // 1 s de audio, tolerancia de 20 ms para o arredondamento do reamostrador.
        assertTrue("ms=$ms", ms in 980..1_020)
        // 16 kHz * 2 bytes = 32 000 bytes por segundo.
        assertTrue("bytes=${saida.length()}", saida.length() in 31_000..33_000)
    }

    @Test
    fun `wav 16 kHz ja esta na taxa alvo (passthrough)`() {
        val entrada = wav(segundos = 0.5, taxa = 16_000)
        val saida = tmp.newFile("out.pcm")
        val ms = AudioConverter().converter(entrada, saida)
        assertEquals(500L, ms)
        // 0,5 s * 16 kHz * 2 bytes = 16 000 bytes.
        assertEquals(500 * 16 * 2, saida.length())
    }

    @Test
    fun `estereo e somado para mono`() {
        val entrada = wav(segundos = 0.5, taxa = 16_000, canais = 2)
        val saida = tmp.newFile("out.pcm")
        val ms = AudioConverter().converter(entrada, saida)
        assertEquals(500L, ms)
        // Mono 16 kHz: o arquivo tem metade dos bytes do estereo (nao o dobro).
        assertEquals(500 * 16 * 2, saida.length())
    }

    @Test
    fun `cancelamento no meio para e nao termina o arquivo`() {
        val entrada = wav(segundos = 2.0, taxa = 44_100)
        val saida = tmp.newFile("out.pcm")
        var chamadas = 0
        val conv = AudioConverter(cancelado = { chamadas++ >= 2 })
        try {
            conv.converter(entrada, saida)
        } catch (e: AudioConverter.CancellationException) {
            assertTrue("cancelou depois de pelo menos um bloco", chamadas >= 2)
            return
        }
        throw AssertionError("deveria ter cancelado")
    }
}
