package com.gravasom.core.domain.marker

/** Cor do marcador na onda. A lista e fechada para que a cor nunca seja a unica pista (o rotulo sempre existe). */
enum class MarkerColor { AMBER, RED, GREEN, BLUE, PURPLE }

/**
 * Ponto marcado numa gravacao. [positionMillis] e o tempo do ARQUIVO (pausas nao contam).
 * [label] vazio significa "sem nome": a tela mostra "Marcador" com o tempo.
 */
data class Marker(
    val id: Long = 0L,
    val recordingId: Long,
    val positionMillis: Long,
    val label: String,
    val note: String,
    val color: MarkerColor = MarkerColor.AMBER,
)

enum class MarkerRejection { NOTE_TOO_LONG, OUT_OF_RANGE, RECORDING_NOT_FOUND }

sealed interface MarkerResult {
    data class Saved(val id: Long) : MarkerResult
    data class Rejected(val reason: MarkerRejection) : MarkerResult
}

object MarkerNote {
    /** Limite da nota, em caracteres que o usuario enxerga (pontos de codigo), sem contar espacos nas pontas. */
    const val MAX_LENGTH = 140

    fun normalize(raw: String): String = raw.trim()

    fun length(raw: String): Int = normalize(raw).let { it.codePointCount(0, it.length) }

    fun isValid(raw: String): Boolean = length(raw) <= MAX_LENGTH
}
