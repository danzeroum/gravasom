package com.gravasom.core.audio.engine

/**
 * Intervalo de corte em milissegundos. Puro e testavel so com o JDK.
 * Para WAV, o corte e sem perdas (copia de bytes); para M4A/MP3, exige reencode.
 */
data class TrimRange(
    val startMillis: Long,
    val endMillis: Long,
) {
    val durationMillis: Long get() = (endMillis - startMillis).coerceAtLeast(0L)
    val isValid: Boolean get() = endMillis > startMillis && startMillis >= 0L

    companion object {
        fun byteOffsetFor(millis: Long, sampleRate: Int, channels: Int, bits: Int): Long {
            val bytesPerSecond = sampleRate.toLong() * channels * (bits / 8)
            return (millis.coerceAtLeast(0L)) * bytesPerSecond / 1000L
        }
    }
}
