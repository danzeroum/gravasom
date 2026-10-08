package com.gravasom.core.audio.engine.analysis

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class Resampler16kTest {

    private fun sine(rate: Int, hz: Double, seconds: Double, amp: Double = 10_000.0) =
        ShortArray((rate * seconds).toInt()) { (amp * sin(2 * PI * hz * it / rate)).toInt().toShort() }

    private fun run(rate: Int, input: ShortArray, chunk: Int = input.size): ShortArray {
        val r = Resampler16k(rate)
        val out = ArrayList<Short>()
        var i = 0
        while (i < input.size) {
            val n = minOf(chunk, input.size - i)
            val buf = ShortArray(r.maxOutput(n))
            val produced = r.process(input, i, n, buf)
            for (k in 0 until produced) out += buf[k]
            i += n
        }
        val tail = ShortArray(2)
        val t = r.finish(tail)
        for (k in 0 until t) out += tail[k]
        return out.toShortArray()
    }

    @Test
    fun `16 kHz passa direto`() {
        val x = sine(16_000, 440.0, 0.5)
        assertArrayEquals(x, run(16_000, x))
    }

    @Test
    fun `48 kHz para 16 kHz mantem a duracao e o nivel de um tom de fala`() {
        val x = sine(48_000, 1_000.0, 1.0)
        val y = run(48_000, x)
        assertTrue(abs(y.size - 16_000) <= 1, "tamanho ${y.size}")
        val peak = y.drop(100).maxOf { abs(it.toInt()) }
        assertTrue(peak in 9_500..10_100, "pico $peak")
    }

    @Test
    fun `44,1 kHz para 16 kHz tem o numero certo de amostras`() {
        val y = run(44_100, sine(44_100, 500.0, 2.0))
        assertTrue(abs(y.size - 32_000) <= 1, "tamanho ${y.size}")
    }

    @Test
    fun `8 kHz sobe para 16 kHz sem perder o tom`() {
        val y = run(8_000, sine(8_000, 400.0, 1.0))
        assertTrue(abs(y.size - 16_000) <= 1)
        assertTrue(y.drop(50).maxOf { abs(it.toInt()) } > 9_000)
    }

    @Test
    fun `tom acima de 8 kHz e bastante atenuado ao descer de 48 kHz`() {
        val y = run(48_000, sine(48_000, 12_000.0, 1.0))
        assertTrue(y.drop(100).maxOf { abs(it.toInt()) } < 4_000)
    }

    @Test
    fun `resultado nao depende do tamanho dos pedacos`() {
        val x = sine(44_100, 700.0, 1.0)
        assertArrayEquals(run(44_100, x), run(44_100, x, chunk = 333))
        assertArrayEquals(run(44_100, x), run(44_100, x, chunk = 1))
    }

    @Test
    fun `corrente continua e preservada`() {
        val y = run(48_000, ShortArray(48_000) { 5_000 })
        assertTrue(y.drop(2).all { abs(it - 5_000) <= 1 })
        assertTrue(abs(y.size - 16_000) <= 1)
    }
}
