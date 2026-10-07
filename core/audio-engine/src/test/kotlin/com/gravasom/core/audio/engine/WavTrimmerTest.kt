package com.gravasom.core.audio.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WavTrimmerTest {

    @Test
    fun `corte de 10 a 20 s em WAV gera 10 s`() {
        // 30 s de PCM 44100 mono 16 bits
        val header = WavHeaderWriter.build(44_100, 1, 16, 44_100L * 2 * 30)
        val pcm = ByteArray(44_100 * 2 * 30) { (it % 127).toByte() }
        val source = header + pcm
        val result = WavTrimmer.trim(source, TrimRange(10_000, 20_000), 44_100, 1, 16)
        assertEquals(10_000L, WavTrimmer.durationMillis(result, 44_100, 1, 16))
    }

    @Test
    fun `corte preserva cabecalho RIFF valido`() {
        val header = WavHeaderWriter.build(44_100, 1, 16, 44_100L * 2 * 5)
        val pcm = ByteArray(44_100 * 2 * 5)
        val result = WavTrimmer.trim(header + pcm, TrimRange(1_000, 2_000), 44_100, 1, 16)
        assertTrue(result.size >= WavHeaderWriter.HEADER_SIZE)
    }
}
