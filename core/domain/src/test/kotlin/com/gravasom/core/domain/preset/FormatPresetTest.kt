package com.gravasom.core.domain.preset

import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FormatPresetTest {

    @Test
    fun cadaPresetProduzUmaConfiguracaoDeGravacaoValida() {
        for (p in FormatPreset.entries) {
            val c = FormatPresets.recordingConfig(p)
            assertEquals(p.spec.format, c.format)
            assertEquals(p.spec.sampleRateHz, c.sampleRateHz)
            assertEquals(p.spec.channels, c.channels)
            assertEquals(p.spec.bitrateKbps, c.bitrateKbps)
        }
    }

    @Test
    fun taxaBitrateECanaisDosQuatroPresets() {
        assertEquals(Triple(24_000, 64, 1), FormatPreset.VOICE.spec.let { Triple(it.sampleRateHz, it.bitrateKbps, it.channels) })
        assertEquals(Triple(16_000, 32, 1), FormatPreset.CLASS.spec.let { Triple(it.sampleRateHz, it.bitrateKbps, it.channels) })
        assertEquals(Triple(48_000, 96, 1), FormatPreset.INTERVIEW.spec.let { Triple(it.sampleRateHz, it.bitrateKbps, it.channels) })
        assertEquals(Triple(48_000, 192, 2), FormatPreset.MUSIC.spec.let { Triple(it.sampleRateHz, it.bitrateKbps, it.channels) })
        assertEquals(Triple(48_000, 256, 2), FormatPreset.NATURE.spec.let { Triple(it.sampleRateHz, it.bitrateKbps, it.channels) })
        assertEquals(Triple(48_000, 256, 2), FormatPreset.WATER.spec.let { Triple(it.sampleRateHz, it.bitrateKbps, it.channels) })
    }

    @Test
    fun presetsMusicaNaturezaEAguaDesligamRedutorENormalizacaoDeVoz() {
        for (p in listOf(FormatPreset.MUSIC, FormatPreset.NATURE, FormatPreset.WATER)) {
            val e = FormatPresets.effects(p)
            assertFalse(e.denoise, "$p: redutor")
            assertFalse(e.normalize, "$p: normalizacao")
        }
    }

    @Test
    fun naturezaEAguaGravamBandaCheiaEmEstereoComOMaiorBitrate() {
        for (p in listOf(FormatPreset.NATURE, FormatPreset.WATER)) {
            val n = p.spec
            assertEquals(48_000, n.sampleRateHz, "$p") // passaros, insetos e agua passam de 10 kHz: nada de 16/24 kHz
            assertEquals(2, n.channels, "$p")
            val outros = FormatPreset.entries.filter { it != FormatPreset.NATURE && it != FormatPreset.WATER }
            assertTrue(outros.all { it.spec.bitrateKbps < n.bitrateKbps }, "$p")
        }
    }

    @Test
    fun presetsDeVozLigamRedutorENormalizacao() {
        for (p in listOf(FormatPreset.VOICE, FormatPreset.CLASS, FormatPreset.INTERVIEW)) {
            val e = FormatPresets.effects(p)
            assertTrue(e.denoise, "$p: redutor")
            assertTrue(e.normalize, "$p: normalizacao")
        }
    }

    @Test
    fun efeitosPadraoUsamAIntensidadeCalibradaEOAlvoDeLoudness() {
        val e = FormatPresets.effects(FormatPreset.CLASS)
        assertEquals(0.4f, e.denoiseIntensity) // ADR-015: a fala limpa fica acima do teto de STOI com 40%
        assertEquals(-16f, e.targetLufs)
        assertEquals(-1f, e.ceilingDbtp)
    }

    @Test
    fun idsSaoEstaveisEReconhecidos() {
        assertEquals(listOf("voice", "class", "interview", "music", "nature", "water"), FormatPreset.entries.map { it.id })
        for (p in FormatPreset.entries) assertEquals(p, FormatPreset.fromId(p.id))
        assertNull(FormatPreset.fromId("outro"))
        assertNull(FormatPreset.fromId(null))
    }

    @Test
    fun aGravacaoSemPresetNaoLigaEfeitosPorPadrao() {
        // Gravacao antiga ou importada (sem preset): nada de efeitos ligados sem o usuario pedir.
        val e = FormatPresets.effects(null)
        assertFalse(e.denoise)
        assertFalse(e.normalize)
    }

    @Test
    fun presetMantemOsOutrosCamposDaConfiguracaoBase() {
        val base = RecordingConfig(format = RecordingFormat.WAV, skipSilence = true)
        val c = FormatPresets.recordingConfig(FormatPreset.MUSIC, base)
        assertTrue(c.skipSilence)
        assertEquals(RecordingFormat.M4A, c.format)
    }

    @Test
    fun todoPresetCabeNosLimitesDeRecordingConfig() {
        for (p in FormatPreset.entries) {
            assertTrue(p.spec.sampleRateHz in 8_000..48_000)
            assertTrue(p.spec.channels in 1..2)
        }
    }
}
