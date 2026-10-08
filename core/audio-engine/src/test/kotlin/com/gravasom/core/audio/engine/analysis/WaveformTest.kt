package com.gravasom.core.audio.engine.analysis

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WaveformTest {

    @Test
    fun `pico por janela de 10 ms em 0 a 255, usando todos os canais`() {
        val b = WaveformBuilder(sampleRate = 1_000, channels = 2) // janela = 10 quadros
        val pcm = ShortArray(40) // 20 quadros estereo = 2 janelas
        pcm[3] = 16_384 // quadro 1, canal R: metade da escala
        pcm[30] = (-32_768).toShort() // quadro 15, canal L: fundo de escala negativo
        b.add(pcm, 0, pcm.size)
        val w = b.finish()
        assertEquals(100, w.peaksPerSecond)
        assertEquals(2, w.peaks.size)
        assertEquals(128, w.peak(0))
        assertEquals(255, w.peak(1))
    }

    @Test
    fun `janela parcial no fim vira a ultima janela e entrada em pedacos da o mesmo resultado`() {
        val whole = ShortArray(10_050) { ((it * 37) % 20_000 - 10_000).toShort() } // 125 janelas de 80 + 50 amostras
        val a = WaveformBuilder(8_000, 1).apply { add(whole, 0, whole.size) }.finish()
        val b = WaveformBuilder(8_000, 1).apply {
            var i = 0
            val cuts = listOf(1, 7, 79, 80, 81, 1_000)
            var k = 0
            while (i < whole.size) {
                val n = minOf(cuts[k++ % cuts.size], whole.size - i)
                add(whole, i, n)
                i += n
            }
        }.finish()
        assertArrayEquals(a.peaks, b.peaks)
        assertEquals(126, a.peaks.size)
    }

    @Test
    fun `silencio digital tem pico zero e nunca negativo em byte`() {
        val w = WaveformBuilder(48_000, 1).apply { add(ShortArray(48_000), 0, 48_000) }.finish()
        assertEquals(100, w.peaks.size)
        assertTrue((0 until w.peaks.size).all { w.peak(it) == 0 })
    }

    @Test
    fun `colunas pegam o maximo de cada faixa normalizado entre 0 e 1`() {
        val peaks = ByteArray(1_000) { if (it == 450) 200.toByte() else 10 } // 10 s, um pico aos 4,5 s
        val cols = Waveform(peaks, peaksPerSecond = 100).columns(startMillis = 0, endMillis = 10_000, columns = 10)
        assertEquals(10, cols.size)
        cols.forEachIndexed { i, v -> assertEquals(if (i == 4) 200f / 255f else 10f / 255f, v, 1e-6f, "coluna $i") }
    }

    @Test
    fun `colunas fora do intervalo ficam zeradas e o intervalo e limitado a duracao`() {
        val w = Waveform(ByteArray(100) { 200.toByte() }, 100) // 1 s
        val cols = w.columns(startMillis = -500, endMillis = 1_500, columns = 4)
        assertEquals(0f, cols[0])
        assertTrue(cols[1] > 0.7f && cols[2] > 0.7f)
        assertEquals(0f, cols[3])
    }

    @Test
    fun `zoom em coluna mais fina que a janela repete o valor da janela`() {
        val w = Waveform(byteArrayOf(0, 100, 0), 100) // 30 ms
        val cols = w.columns(0, 30, 30)
        assertEquals(30, cols.size)
        assertTrue(cols.slice(10 until 20).all { it > 0.39f && it < 0.4f })
        assertTrue(cols.slice(0 until 10).all { it == 0f })
    }

    @Test
    fun `duracao em ms vem do numero de janelas`() {
        assertEquals(3_600_000L, Waveform(ByteArray(360_000), 100).durationMillis)
    }
}
