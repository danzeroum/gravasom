package com.gravasom.core.audio.engine.analysis

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sin

class AnalysisPipelineTest {

    /** Classificador falso: um quadro e "fala" se a amplitude media passa de um limiar (so para testar o encanamento). */
    private class LoudnessClassifier : SpeechClassifier {
        var samplesSeen = 0
        private val buffer = ArrayList<Short>()
        override fun classify(pcm16k: ShortArray, length: Int): ByteArray {
            samplesSeen += length
            for (i in 0 until length) buffer += pcm16k[i]
            val out = ArrayList<Byte>()
            while (buffer.size >= FRAME) {
                val frame = buffer.subList(0, FRAME)
                out += if (frame.sumOf { kotlin.math.abs(it.toInt()) } / FRAME > 1_000) 1 else 0
                frame.clear()
            }
            return out.toByteArray()
        }
        override fun finish(): ByteArray {
            if (buffer.isEmpty()) return ByteArray(0)
            buffer.clear()
            return byteArrayOf(0)
        }
        override fun close() = Unit
        companion object { const val FRAME = 320 }
    }

    private fun tone(rate: Int, seconds: Double, amp: Double) =
        ShortArray((rate * seconds).toInt()) { (amp * sin(2 * PI * 300.0 * it / rate)).toInt().toShort() }

    @Test
    fun `onda e quadros de fala saem da mesma passada, em qualquer pedaco`() {
        // 1 s de silencio, 1 s de tom, 1 s de silencio a 48 kHz mono
        val pcm = ShortArray(48_000) + tone(48_000, 1.0, 20_000.0) + ShortArray(48_000)
        fun analyze(chunk: Int): RecordingAnalysis {
            val p = AnalysisPipeline(48_000, 1, listOf(LoudnessClassifier(), LoudnessClassifier()))
            var i = 0
            while (i < pcm.size) {
                val n = minOf(chunk, pcm.size - i)
                p.add(pcm, i, n)
                i += n
            }
            return p.finish()
        }
        val a = analyze(pcm.size)
        val b = analyze(4_099)
        assertEquals(300, a.waveform.peaks.size) // 3 s a 100 janelas/s
        assertTrue(a.waveform.peak(50) == 0 && a.waveform.peak(150) in 150..160 && a.waveform.peak(250) == 0, "picos ${a.waveform.peak(50)} ${a.waveform.peak(150)} ${a.waveform.peak(250)}")
        assertArrayEquals(a.waveform.peaks, b.waveform.peaks)
        assertEquals(2, a.speechByMode.size)
        val frames = a.speechFrames(0)
        assertEquals(20, a.frameMillis)
        assertTrue(kotlin.math.abs(frames.size - 150) <= 2, "quadros ${frames.size}")
        assertTrue(frames.slice(0 until 45).none { it }, "silencio inicial sem fala ${frames.toList()}")
        assertTrue(frames.slice(55 until 95).all { it }, "tom do meio e fala")
        assertTrue(frames.slice(105 until 148).none { it }, "silencio final sem fala")
        assertArrayEquals(a.speechByMode[0], b.speechByMode[0])
        assertEquals(3_000L, a.durationMillis)
    }

    @Test
    fun `estereo e reduzido a mono antes do classificador`() {
        val mono = tone(16_000, 1.0, 20_000.0)
        val stereo = ShortArray(mono.size * 2) { mono[it / 2] }
        val c = LoudnessClassifier()
        val p = AnalysisPipeline(16_000, 2, listOf(c))
        p.add(stereo, 0, stereo.size)
        val r = p.finish()
        assertEquals(16_000, c.samplesSeen)
        assertTrue(r.speechFrames(0).count { it } > 45)
    }

    @Test
    fun `empacotar e desempacotar bits preserva os quadros, inclusive o resto do ultimo byte`() {
        val flags = BooleanArray(37) { it % 3 == 0 }
        val packed = SpeechBits.pack(flags)
        assertEquals(5, packed.size)
        assertTrue(flags.contentEquals(SpeechBits.unpack(packed, flags.size)))
    }

    @Test
    fun `analise vazia nao quebra`() {
        val r = AnalysisPipeline(44_100, 1, listOf(LoudnessClassifier())).finish()
        assertEquals(0, r.waveform.peaks.size)
        assertEquals(0, r.speechFrames(0).size)
        assertEquals(0L, r.durationMillis)
    }
}
