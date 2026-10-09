package com.gravasom.feature.player

import com.gravasom.core.audio.engine.analysis.SkipPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pular silencio sem Android: o relogio chama tick(); o alvo falso toca em tempo simulado. */
class SilenceSkipperTest {

    private class Fake(var playing: Boolean = true, var position: Long = 0L) : SkipTarget {
        override val isPlaying get() = playing
        override val positionMillis get() = position
        val seeks = mutableListOf<Long>()
        override fun seekTo(millis: Long) {
            seeks += millis
            position = millis
        }
    }

    /** 20 s: fala de 0 a 5 s, 10 s de silencio, fala de 15 a 20 s (quadros de 20 ms do VAD). */
    private fun plan(): SkipPlan {
        val frames = BooleanArray(1_000) { it < 250 || it >= 750 }
        return SkipPlan.build(frames, 20_000L)
    }

    /** Toca o arquivo todo em passos de 40 ms (como o relogio do hub) e devolve quanto tocou de verdade. */
    private fun playThrough(skipper: SilenceSkipper, target: Fake): Long {
        var played = 0L
        while (target.position < 20_000L) {
            skipper.tick()
            target.position += 40
            played += 40
        }
        return played
    }

    @Test
    fun `10 s de silencio entre as falas sao pulados e sobra no maximo 1 s a mais que a fala`() {
        val target = Fake()
        val skipper = SilenceSkipper(target).also { it.configure(plan(), enabled = true) }
        val played = playThrough(skipper, target)
        val speech = 10_000L // 5 s + 5 s
        assertTrue("tocou ${played / 1000.0} s de uma gravacao com 10 s de fala", played - speech <= 1_000L)
        assertTrue(skipper.skippedMillis >= 9_000L)
        assertEquals(20_000L - played, skipper.skippedMillis, )
    }

    @Test
    fun `desligado nao pula nada`() {
        val target = Fake()
        val skipper = SilenceSkipper(target).also { it.configure(plan(), enabled = false) }
        val played = playThrough(skipper, target)
        assertEquals(20_000L, played)
        assertTrue(target.seeks.isEmpty())
    }

    @Test
    fun `pausado nao pula mesmo dentro da lacuna`() {
        val target = Fake(playing = false, position = 8_000L)
        val skipper = SilenceSkipper(target).also { it.configure(plan(), enabled = true) }
        assertEquals(8_000L, skipper.tick())
        assertTrue(target.seeks.isEmpty())
    }

    @Test
    fun `sem plano nao pula`() {
        val target = Fake(position = 8_000L)
        val skipper = SilenceSkipper(target).also { it.configure(null, enabled = true) }
        assertEquals(8_000L, skipper.tick())
        assertTrue(target.seeks.isEmpty())
    }

    @Test
    fun `dentro da lacuna salta para o fim dela e soma o que pulou`() {
        val target = Fake(position = 8_000L)
        val skipper = SilenceSkipper(target).also { it.configure(plan(), enabled = true) }
        val after = skipper.tick()
        assertTrue("saltou para $after", after in 14_000L..15_000L)
        assertEquals(after - 8_000L, skipper.skippedMillis)
    }

    @Test
    fun `reset esquece plano ligado e total pulado`() {
        val target = Fake(position = 8_000L)
        val skipper = SilenceSkipper(target).also { it.configure(plan(), enabled = true) }
        skipper.tick()
        skipper.reset()
        assertNull(skipper.plan)
        assertFalse(skipper.enabled)
        assertEquals(0L, skipper.skippedMillis)
    }
}
