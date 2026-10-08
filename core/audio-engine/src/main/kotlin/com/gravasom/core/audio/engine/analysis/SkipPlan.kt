package com.gravasom.core.audio.engine.analysis

/** Faixa [startMillis, endMillis) que a reproducao pode pular. */
data class SkipRange(val startMillis: Long, val endMillis: Long) {
    constructor(startMillis: Int, endMillis: Int) : this(startMillis.toLong(), endMillis.toLong())

    val lengthMillis: Long get() = endMillis - startMillis
}

/**
 * Parametros do "pular silencio".
 * - [padMillis]: margem mantida de cada lado de toda fala (nao corta o inicio nem o fim das frases).
 * - [minSkipMillis]: so pula se, depois das margens, sobrar pelo menos isto (pausas curtas entre palavras ficam).
 */
data class SkipParams(
    val frameMillis: Int = DEFAULT_FRAME_MILLIS,
    val padMillis: Int = DEFAULT_PAD_MILLIS,
    val minSkipMillis: Int = DEFAULT_MIN_SKIP_MILLIS,
) {
    init {
        require(frameMillis > 0 && padMillis >= 0 && minSkipMillis > 0) { "parametros invalidos" }
    }

    companion object {
        const val DEFAULT_FRAME_MILLIS = 20
        const val DEFAULT_PAD_MILLIS = 300
        const val DEFAULT_MIN_SKIP_MILLIS = 800
    }
}

/**
 * Onde pular numa gravacao, calculado dos quadros fala/nao-fala do VAD. Qualquer quadro de fala, por isolado que seja,
 * protege a vizinhanca ([SkipParams.padMillis]): erra para o lado de NAO pular, nunca para o de cortar fala.
 */
class SkipPlan private constructor(val ranges: List<SkipRange>, val totalMillis: Long) {

    val skippedMillis: Long = ranges.sumOf { it.lengthMillis }
    val remainingMillis: Long get() = totalMillis - skippedMillis

    /** Se [positionMillis] esta dentro de uma lacuna, onde a reproducao deve continuar; senao null. */
    fun skipTarget(positionMillis: Long): Long? =
        ranges.firstOrNull { positionMillis >= it.startMillis && positionMillis < it.endMillis }?.endMillis

    /** Primeira lacuna que termina depois de [positionMillis] (para agendar o proximo salto). */
    fun nextRange(positionMillis: Long): SkipRange? = ranges.firstOrNull { it.endMillis > positionMillis }

    /** Tempo ja economizado quando a reproducao chega a [positionMillis] do arquivo. */
    fun skippedBefore(positionMillis: Long): Long = ranges.sumOf {
        (minOf(positionMillis, it.endMillis) - it.startMillis).coerceAtLeast(0L)
    }

    companion object {
        val EMPTY = SkipPlan(emptyList(), 0L)

        fun build(speechFrames: BooleanArray, durationMillis: Long, params: SkipParams = SkipParams()): SkipPlan {
            if (durationMillis <= 0) return EMPTY
            val keep = keptIntervals(speechFrames, durationMillis, params)
            val gaps = mutableListOf<SkipRange>()
            var cursor = 0L
            for ((start, end) in keep) {
                if (start - cursor >= params.minSkipMillis) gaps += SkipRange(cursor, start)
                cursor = maxOf(cursor, end)
            }
            if (durationMillis - cursor >= params.minSkipMillis) gaps += SkipRange(cursor, durationMillis)
            return SkipPlan(gaps, durationMillis)
        }

        /** Faixas com fala, expandidas pela margem e fundidas, limitadas a [0, durationMillis]. */
        private fun keptIntervals(frames: BooleanArray, durationMillis: Long, p: SkipParams): List<Pair<Long, Long>> {
            val out = mutableListOf<Pair<Long, Long>>()
            for (i in frames.indices) {
                if (!frames[i]) continue
                val start = (i.toLong() * p.frameMillis - p.padMillis).coerceAtLeast(0L)
                if (start >= durationMillis) break
                val end = ((i + 1L) * p.frameMillis + p.padMillis).coerceAtMost(durationMillis)
                val last = out.lastOrNull()
                if (last != null && start <= last.second) out[out.size - 1] = last.first to maxOf(last.second, end) else out += start to end
            }
            return out
        }
    }
}
