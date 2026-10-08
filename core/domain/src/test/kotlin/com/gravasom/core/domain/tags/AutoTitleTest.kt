package com.gravasom.core.domain.tags

import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class AutoTitleTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val at = ZonedDateTime.of(2026, 10, 8, 14, 32, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `nome e data e depois preset em pt-BR`() {
        val config = RecordingConfig(format = RecordingFormat.M4A, bitrateKbps = 128, channels = 1)
        assertEquals("08/10/2026 14:32 · M4A 128 kbps mono", AutoTitle.build(at, config, Locale.forLanguageTag("pt-BR"), zone))
    }

    @Test
    fun `wav nao tem bitrate e mostra taxa de amostragem`() {
        val config = RecordingConfig(format = RecordingFormat.WAV, sampleRateHz = 48_000, channels = 2)
        assertEquals("08/10/2026 14:32 · WAV 48 kHz estéreo", AutoTitle.build(at, config, Locale.forLanguageTag("pt-BR"), zone))
    }

    @Test
    fun `fora do portugues usa data ISO e palavras em ingles`() {
        val config = RecordingConfig(format = RecordingFormat.MP3, bitrateKbps = 192, channels = 2)
        assertEquals("2026-10-08 14:32 · MP3 192 kbps stereo", AutoTitle.build(at, config, Locale.US, zone))
    }
}
