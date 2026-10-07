package com.gravasom.core.domain.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RecordingConfigTest {

    @Test
    fun `padrao e m4a 44100 mono 128`() {
        val c = RecordingConfig()
        assertEquals(RecordingFormat.M4A, c.format)
        assertEquals(44_100, c.sampleRateHz)
        assertEquals(1, c.channels)
        assertEquals(128, c.bitrateKbps)
    }

    @Test
    fun `taxa fora da faixa falha`() {
        assertThrows(IllegalArgumentException::class.java) { RecordingConfig(sampleRateHz = 4_000) }
        assertThrows(IllegalArgumentException::class.java) { RecordingConfig(sampleRateHz = 96_000) }
    }

    @Test
    fun `canais invalidos falham`() {
        assertThrows(IllegalArgumentException::class.java) { RecordingConfig(channels = 0) }
        assertThrows(IllegalArgumentException::class.java) { RecordingConfig(channels = 3) }
    }

    @Test
    fun `limites validos sao aceitos`() {
        assertEquals(8_000, RecordingConfig(sampleRateHz = 8_000).sampleRateHz)
        assertEquals(48_000, RecordingConfig(sampleRateHz = 48_000).sampleRateHz)
        assertEquals(2, RecordingConfig(channels = 2).channels)
    }

    @Test
    fun `formatos tem extensao e mime`() {
        assertEquals("m4a", RecordingFormat.M4A.extension)
        assertEquals("audio/wav", RecordingFormat.WAV.mimeType)
        assertEquals("audio/mpeg", RecordingFormat.MP3.mimeType)
        assertEquals(3, RecordingFormat.entries.size)
    }
}
