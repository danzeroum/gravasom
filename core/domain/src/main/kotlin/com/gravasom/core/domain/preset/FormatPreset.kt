package com.gravasom.core.domain.preset

import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat

/** Como gravar (formato, taxa, bitrate, canais) para cada uso. */
data class PresetSpec(val format: RecordingFormat, val sampleRateHz: Int, val bitrateKbps: Int, val channels: Int)

/**
 * Efeitos "Melhorar som" que ja vem ligados para as gravacoes feitas com o preset. Atuam como efeitos nao destrutivos
 * (o original nunca muda); isto so decide o que aparece marcado ao abrir o painel de melhoria.
 */
data class EffectDefaults(
    val denoise: Boolean,
    val normalize: Boolean,
    /** 0..1. 40% e a intensidade em que toda fala limpa do corpus fica acima do teto de STOI 0,9909 (ADR-015). */
    val denoiseIntensity: Float = DEFAULT_DENOISE_INTENSITY,
    val targetLufs: Float = DEFAULT_TARGET_LUFS,
    val ceilingDbtp: Float = DEFAULT_CEILING_DBTP,
) {
    companion object {
        const val DEFAULT_DENOISE_INTENSITY = 0.4f
        const val DEFAULT_TARGET_LUFS = -16f
        const val DEFAULT_CEILING_DBTP = -1f
        val OFF = EffectDefaults(denoise = false, normalize = false)
    }
}

/**
 * Presets de formato. As taxas (16, 24 e 48 kHz) dividem 48 kHz: o redutor de ruido trabalha a 48 kHz e a reamostragem fica trivial.
 * Voz e Aula: fala, arquivos pequenos. Entrevista: mais fidelidade para transcrever e editar. Musica, Natureza e Agua: banda cheia,
 * estereo e SEM efeitos de voz por padrao (o redutor e a normalizacao foram calibrados para fala: o redutor trata cantos de passaros,
 * insetos, sapos e agua corrente como ruido, e a normalizacao de voz estraga a dinamica de musica e de sons de bichos).
 */
enum class FormatPreset(val id: String, val spec: PresetSpec, val effects: EffectDefaults) {
    VOICE("voice", PresetSpec(RecordingFormat.M4A, 24_000, 64, 1), EffectDefaults(denoise = true, normalize = true)),
    CLASS("class", PresetSpec(RecordingFormat.M4A, 16_000, 32, 1), EffectDefaults(denoise = true, normalize = true)),
    INTERVIEW("interview", PresetSpec(RecordingFormat.M4A, 48_000, 96, 1), EffectDefaults(denoise = true, normalize = true)),
    MUSIC("music", PresetSpec(RecordingFormat.M4A, 48_000, 192, 2), EffectDefaults.OFF),

    /** Sons de bichos: agudos de passaros e insetos passam de 10 kHz, entao banda cheia a 48 kHz, estereo e bitrate alto. */
    NATURE("nature", PresetSpec(RecordingFormat.M4A, 48_000, 256, 2), EffectDefaults.OFF),

    /**
     * Sons de agua (rio, chuva, mar, cachoeira): ruido de banda larga ate o topo do espectro. O AAC gasta muitos bits em sinais
     * assim (nao ha estrutura para comprimir), entao o mesmo 48 kHz estereo a 256 kbps da Natureza; fica como opcao propria
     * porque o redutor de ruido trataria a agua inteira como ruido e a apagaria.
     */
    WATER("water", PresetSpec(RecordingFormat.M4A, 48_000, 256, 2), EffectDefaults.OFF),
    ;

    companion object {
        fun fromId(id: String?): FormatPreset? = entries.firstOrNull { it.id == id }
    }
}

object FormatPresets {
    /** Configuracao de gravacao do preset, mantendo os demais campos de [base]. */
    fun recordingConfig(preset: FormatPreset, base: RecordingConfig = RecordingConfig()): RecordingConfig = base.copy(
        format = preset.spec.format,
        sampleRateHz = preset.spec.sampleRateHz,
        channels = preset.spec.channels,
        bitrateKbps = preset.spec.bitrateKbps,
    )

    /** Efeitos ligados por padrao; gravacao sem preset (antiga ou importada) nao liga nada. */
    fun effects(preset: FormatPreset?): EffectDefaults = preset?.effects ?: EffectDefaults.OFF
}
