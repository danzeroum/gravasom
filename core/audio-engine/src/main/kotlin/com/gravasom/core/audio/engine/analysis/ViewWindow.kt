package com.gravasom.core.audio.engine.analysis

/**
 * Janela visivel da onda: [startMillis, startMillis + spanMillis] dentro de uma gravacao de [durationMillis].
 * Imutavel e sem Android: o zoom por pinca e o arrasto da UI so chamam [zoom] e [pan].
 */
data class ViewWindow(val durationMillis: Long, val startMillis: Long, val spanMillis: Long) {

    val endMillis: Long get() = startMillis + spanMillis

    /** Aproxima (factor > 1) ou afasta (< 1) mantendo [focusMillis] na mesma posicao relativa da tela. */
    fun zoom(factor: Float, focusMillis: Long): ViewWindow {
        if (durationMillis <= 0 || factor <= 0f || factor.isNaN()) return this
        val newSpan = (spanMillis / factor.toDouble()).toLong().coerceIn(minSpan(), durationMillis)
        val relative = if (spanMillis > 0) (focusMillis - startMillis).toDouble() / spanMillis else 0.5
        val start = (focusMillis - relative * newSpan).toLong()
        return ViewWindow(durationMillis, start.coerceIn(0L, durationMillis - newSpan), newSpan)
    }

    /** Desloca a janela em [deltaMillis] (positivo = para o fim), presa as bordas. */
    fun pan(deltaMillis: Long): ViewWindow =
        copy(startMillis = (startMillis + deltaMillis).coerceIn(0L, (durationMillis - spanMillis).coerceAtLeast(0L)))

    /** Rola so se [positionMillis] saiu da janela; a nova janela deixa a posicao a 10% da borda esquerda. */
    fun follow(positionMillis: Long): ViewWindow {
        if (positionMillis in startMillis..endMillis) return this
        val start = (positionMillis - spanMillis / FOLLOW_LEAD).coerceIn(0L, (durationMillis - spanMillis).coerceAtLeast(0L))
        return copy(startMillis = start)
    }

    fun millisAt(x: Float, width: Float): Long {
        if (width <= 0f) return startMillis
        return (startMillis + (x / width).toDouble() * spanMillis).toLong().coerceIn(0L, durationMillis)
    }

    fun xFor(millis: Long, width: Float): Float =
        if (spanMillis <= 0L) 0f else ((millis - startMillis).toDouble() / spanMillis * width).toFloat()

    private fun minSpan(): Long = minOf(MIN_SPAN_MILLIS, durationMillis)

    companion object {
        /** Menor trecho visivel: 1 s (a onda tem uma janela por 10 ms, entao ainda ha ~100 pontos). */
        const val MIN_SPAN_MILLIS = 1_000L
        private const val FOLLOW_LEAD = 10

        fun full(durationMillis: Long): ViewWindow = ViewWindow(durationMillis.coerceAtLeast(0L), 0L, durationMillis.coerceAtLeast(0L))
    }
}
