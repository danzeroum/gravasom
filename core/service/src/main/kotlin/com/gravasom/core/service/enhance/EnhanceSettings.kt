package com.gravasom.core.service.enhance

import com.gravasom.core.audio.engine.edit.EditProject
import com.gravasom.core.audio.engine.edit.EditStep
import com.gravasom.core.audio.engine.edit.StepType
import com.gravasom.core.domain.preset.EffectDefaults

/** O que o usuario escolheu em "Melhorar som": redutor de ruido (com intensidade) e "melhorar volume" (com alvo em LUFS). */
data class EnhanceSettings(
    val denoise: Boolean,
    /** 0..1 */
    val denoiseIntensity: Float,
    val normalize: Boolean,
    val targetLufs: Float,
    val ceilingDbtp: Float = EffectDefaults.DEFAULT_CEILING_DBTP,
) {
    /** True quando nada seria alterado (nada ligado, ou so o redutor a 0%). */
    val isIdentity: Boolean get() = toProject().isIdentity

    /** Projeto de edicao nao destrutivo (ADR-008): o que e persistido e o que o exportador executa. Redutor antes da normalizacao. */
    fun toProject(): EditProject = EditProject(
        steps = listOf(
            EditStep(StepType.NOISE_REDUCTION, listOf(denoiseIntensity.coerceIn(0f, 1f)), enabled = denoise),
            EditStep(StepType.NORMALIZE, listOf(targetLufs, ceilingDbtp), enabled = normalize),
        ),
    )

    companion object {
        fun fromDefaults(d: EffectDefaults) = EnhanceSettings(
            denoise = d.denoise, denoiseIntensity = d.denoiseIntensity, normalize = d.normalize,
            targetLufs = d.targetLufs, ceilingDbtp = d.ceilingDbtp,
        )

        /** Reabre o projeto salvo; o que ele nao traz vem dos padroes. */
        fun fromProject(project: EditProject, defaults: EffectDefaults): EnhanceSettings {
            val nr = project.steps.firstOrNull { it.type == StepType.NOISE_REDUCTION }
            val norm = project.steps.firstOrNull { it.type == StepType.NORMALIZE }
            return EnhanceSettings(
                denoise = nr?.enabled ?: defaults.denoise,
                denoiseIntensity = nr?.params?.firstOrNull() ?: defaults.denoiseIntensity,
                normalize = norm?.enabled ?: defaults.normalize,
                targetLufs = norm?.params?.getOrNull(0) ?: defaults.targetLufs,
                ceilingDbtp = norm?.params?.getOrNull(1) ?: defaults.ceilingDbtp,
            )
        }
    }
}
