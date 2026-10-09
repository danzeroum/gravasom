package com.gravasom.core.dsp

import com.gravasom.core.audio.engine.edit.AudioBlockProcessor
import com.gravasom.core.audio.engine.edit.EditProject
import com.gravasom.core.audio.engine.edit.EditStep
import com.gravasom.core.audio.engine.edit.StepType

/**
 * Redutor de producao do "Melhorar som": a cadeia RNNoise nativa do celular (ADR-015), um processador por intensidade e taxa.
 * Implementa `(intensity, sampleRate) -> AudioBlockProcessor`, o tipo que o servico de melhoria espera.
 */
class NoiseReducerFactory : (Float, Int) -> AudioBlockProcessor {

    override fun invoke(intensity: Float, sampleRate: Int): AudioBlockProcessor = NativeBlockProcessor(
        EditProject(steps = listOf(EditStep(StepType.NOISE_REDUCTION, listOf(intensity.coerceIn(0f, 1f))))),
        sampleRate,
    )
}
