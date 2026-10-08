package com.gravasom.core.dsp

import com.gravasom.core.audio.engine.analysis.AnalysisPipeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class WebRtcVadHostTest {

    /** Vogal sintetica: f0 de 120 Hz com formantes em 700 e 1200 Hz. */
    private fun voice(t: Double): Double =
        9_000 * sin(2 * PI * 120 * t) + 6_000 * sin(2 * PI * 700 * t) + 4_000 * sin(2 * PI * 1_200 * t)

    private fun noise(seconds: Double, amp: Int, seed: Int): ShortArray {
        val r = Random(seed)
        return ShortArray((16_000 * seconds).toInt()) { (r.nextInt(-amp, amp + 1)).toShort() }
    }

    @Test
    fun `modo fora de 0 a 3 e recusado`() {
        assumeHostLib()
        assertThrows(IllegalArgumentException::class.java) { WebRtcVad(4) }
        assertThrows(IllegalArgumentException::class.java) { WebRtcVad(-1) }
    }

    @Test
    fun `silencio digital nao e fala e os quadros batem com 20 ms`() {
        assumeHostLib()
        WebRtcVad(2).use { vad ->
            val flags = vad.classify(ShortArray(16_000), 16_000)
            assertEquals(50, flags.size)
            assertTrue(flags.all { it.toInt() == 0 })
            assertEquals(0, vad.finish().size)
        }
    }

    @Test
    fun `pedacos que nao fecham quadro acumulam para a proxima chamada`() {
        assumeHostLib()
        WebRtcVad(1).use { vad ->
            val pcm = noise(0.1, 3, 1)
            assertEquals(0, vad.classify(pcm, 100).size)
            assertEquals(0, vad.classify(pcm, 100).size)
            assertEquals(1, vad.classify(pcm, 200).size) // 400 amostras = 1 quadro + 80
            assertEquals(1, vad.finish().size)
        }
    }

    @Test
    fun `fechar libera o lado nativo e a segunda chamada falha com clareza`() {
        assumeHostLib()
        val before = WebRtcVad.liveInstances()
        val vad = WebRtcVad(3)
        assertEquals(before + 1, WebRtcVad.liveInstances())
        vad.close()
        vad.close()
        assertEquals(before, WebRtcVad.liveInstances())
        assertThrows(IllegalStateException::class.java) { vad.classify(ShortArray(320), 320) }
    }

    @Test
    fun `pipeline completo em 44,1 kHz estereo com os 4 modos`() {
        assumeHostLib()
        val rate = 44_100
        val classifiers = (0..3).map { WebRtcVad(it) }
        val pipeline = AnalysisPipeline(rate, 2, classifiers)
        // 2 s de tom pulsado a 120 Hz (parece voz sustentada) entre silencios; estereo.
        val frames = rate * 2
        val pcm = ShortArray(frames * 2) { i ->
            val t = (i / 2).toDouble() / rate
            if (t < 0.5 || t > 1.5) 0 else voice(t).toInt().toShort()
        }
        pipeline.add(pcm, 0, pcm.size)
        val analysis = pipeline.finish()
        classifiers.forEach { it.close() }
        assertEquals(4, analysis.speechByMode.size)
        assertEquals(2_000L, analysis.durationMillis)
        val f = analysis.speechFrames(2)
        assertTrue("silencio inicial", f.slice(0 until 20).none { it })
        assertTrue("trecho com voz sintetica", f.slice(40 until 70).count { it } > 15)
        assertEquals(0, WebRtcVad.liveInstances())
    }
}
