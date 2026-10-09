package com.gravasom.core.service.enhance

import com.gravasom.core.audio.engine.edit.StepType
import com.gravasom.core.domain.preset.EffectDefaults
import com.gravasom.core.domain.preset.FormatPreset
import com.gravasom.core.domain.preset.FormatPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhanceSettingsTest {

    @Test
    fun `preset Aula liga redutor e volume e Musica nao liga nada`() {
        val aula = EnhanceSettings.fromDefaults(FormatPresets.effects(FormatPreset.CLASS))
        assertTrue(aula.denoise)
        assertTrue(aula.normalize)
        assertEquals(0.4f, aula.denoiseIntensity, 0f)
        assertEquals(-16f, aula.targetLufs, 0f)
        val musica = EnhanceSettings.fromDefaults(FormatPresets.effects(FormatPreset.MUSIC))
        assertFalse(musica.denoise)
        assertFalse(musica.normalize)
        assertTrue(musica.isIdentity)
    }

    @Test
    fun `o projeto tem o redutor antes da normalizacao e respeita os liga-desliga`() {
        val p = EnhanceSettings(denoise = true, denoiseIntensity = 0.7f, normalize = false, targetLufs = -14f).toProject()
        assertEquals(listOf(StepType.NOISE_REDUCTION, StepType.NORMALIZE), p.steps.map { it.type })
        assertEquals(listOf(true, false), p.steps.map { it.enabled })
        assertEquals(listOf(0.7f), p.steps[0].params)
        assertEquals(listOf(-14f, -1f), p.steps[1].params)
    }

    @Test
    fun `projeto e configuracao fazem ida e volta`() {
        val s = EnhanceSettings(denoise = true, denoiseIntensity = 0.55f, normalize = true, targetLufs = -19f, ceilingDbtp = -2f)
        assertEquals(s, EnhanceSettings.fromProject(s.toProject(), EffectDefaults.OFF))
    }

    @Test
    fun `redutor a zero por cento e identidade`() {
        assertTrue(EnhanceSettings(true, 0f, false, -16f).isIdentity)
        assertFalse(EnhanceSettings(true, 0.01f, false, -16f).isIdentity)
        assertFalse(EnhanceSettings(false, 0f, true, -16f).isIdentity)
    }

    @Test
    fun `intensidade fora de 0 a 1 e limitada`() {
        val p = EnhanceSettings(true, 3f, false, -16f).toProject()
        assertEquals(1f, p.steps[0].params[0], 0f)
    }
}
