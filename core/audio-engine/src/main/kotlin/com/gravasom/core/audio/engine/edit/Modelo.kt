package com.gravasom.core.audio.engine.edit

/** Tipos de passo da cadeia. O id numerico e estavel (persistido e usado no JNI). */
enum class StepType(val id: Int) {
    HIGH_PASS(1),

    /** Redutor de ruido (RNNoise). Parametros: [intensidade 0..1]; 0 nao altera nada. */
    NOISE_REDUCTION(2),

    /** "Melhorar volume". Parametros: [alvo em LUFS, teto de pico verdadeiro em dBTP]. */
    NORMALIZE(3),
    ;

    companion object {
        fun fromId(id: Int): StepType? = entries.firstOrNull { it.id == id }
    }
}

/** Um passo com parametros. `params` e posicional e especifico de cada [StepType]. */
data class EditStep(
    val type: StepType,
    val params: List<Float> = emptyList(),
    val enabled: Boolean = true,
) {
    /** Passo ligado que, com estes parametros, nao muda o audio (ex.: redutor a 0%). */
    val isNoOp: Boolean
        get() = type == StepType.NOISE_REDUCTION && (params.firstOrNull() ?: 0f) <= 0f
}

/**
 * Projeto de edicao: descreve como transformar um original (somente leitura) num arquivo novo.
 * [bypass] e [intensity] 0 devolvem os bytes do original.
 */
data class EditProject(
    val steps: List<EditStep> = emptyList(),
    val bypass: Boolean = false,
    val intensity: Float = 1f,
) {
    /** True quando o export tem de ser a copia exata do original. */
    val isIdentity: Boolean
        get() = bypass || intensity <= 0f || steps.none { it.enabled && !it.isNoOp }
}

/** Processa blocos de amostras float [-1, 1] in-place. Um por canal. */
interface AudioBlockProcessor : AutoCloseable {
    /** Atraso em amostras que o processador introduz (look-ahead). */
    val latencySamples: Int
    fun process(block: FloatArray, length: Int)
    override fun close() = Unit
}
