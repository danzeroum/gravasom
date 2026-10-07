package com.gravasom.core.audio.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TrimRangeTest {

    @Test
    fun `trim de 10 a 20 s dura 10 s`() {
        val r = TrimRange(startMillis = 10_000, endMillis = 20_000)
        assertEquals(10_000L, r.durationMillis)
    }

    @Test
    fun `range invalido quando fim antes do inicio`() {
        val r = TrimRange(startMillis = 20_000, endMillis = 10_000)
        assertFalse(r.isValid)
    }

    @Test
    fun `range valido quando inicio antes do fim`() {
        assertTrue(TrimRange(0, 1).isValid)
    }

    @Test
    fun `byte offset para WAV 44100 mono 16 bits`() {
        // 1 s = 88200 bytes; 10 s = 882000 bytes
        assertEquals(882_000L, TrimRange.byteOffsetFor(millis = 10_000, sampleRate = 44_100, channels = 1, bits = 16))
    }

    @Test
    fun `byte offset limitado a zero quando negativo`() {
        assertEquals(0L, TrimRange.byteOffsetFor(millis = -5, sampleRate = 44_100, channels = 1, bits = 16))
    }
}
