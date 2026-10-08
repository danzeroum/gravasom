package com.gravasom.core.domain.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecordingBudgetTest {

    private val mb = 1024L * 1024L

    @Test
    fun `wav mono 44,1 kHz consome 88200 bytes por segundo`() {
        assertEquals(88_200L, RecordingBudget.bytesPerSecond(RecordingFormat.WAV, 44_100, 128))
    }

    @Test
    fun `m4a e mp3 usam o bitrate em kbps`() {
        assertEquals(16_000L, RecordingBudget.bytesPerSecond(RecordingFormat.M4A, 44_100, 128))
        assertEquals(24_000L, RecordingBudget.bytesPerSecond(RecordingFormat.MP3, 44_100, 192))
    }

    @Test
    fun `50 MB livres em m4a 128 kbps rendem cerca de 54 minutos`() {
        val minutes = RecordingBudget.estimatedMinutes(50 * mb, RecordingFormat.M4A, 44_100, 128)
        assertEquals(54, minutes)
    }

    @Test
    fun `50 MB livres em wav rendem cerca de 9 minutos`() {
        assertEquals(9, RecordingBudget.estimatedMinutes(50 * mb, RecordingFormat.WAV, 44_100, 128))
    }

    @Test
    fun `abaixo de 100 MB e pouco espaco, a partir de 100 MB nao`() {
        assertTrue(RecordingBudget.isLow(50 * mb))
        assertTrue(RecordingBudget.isLow(100 * mb - 1))
        assertFalse(RecordingBudget.isLow(100 * mb))
    }

    @Test
    fun `entradas invalidas nao dividem por zero`() {
        assertEquals(0, RecordingBudget.estimatedMinutes(0, RecordingFormat.M4A, 44_100, 128))
        assertEquals(0, RecordingBudget.estimatedMinutes(-5, RecordingFormat.M4A, 44_100, 128))
        assertTrue(RecordingBudget.bytesPerSecond(RecordingFormat.M4A, 44_100, 0) > 0)
    }
}
