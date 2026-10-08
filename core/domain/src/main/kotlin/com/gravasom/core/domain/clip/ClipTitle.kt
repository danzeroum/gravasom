package com.gravasom.core.domain.clip

import java.util.Locale

/** Nome de um trecho exportado: "<original> · trecho 12:03–12:40". */
object ClipTitle {
    const val MAX_LENGTH = 120

    fun build(original: String, startMillis: Long, endMillis: Long, pt: Boolean): String {
        val suffix = " · ${if (pt) "trecho" else "clip"} ${time(startMillis)}–${time(endMillis)}"
        return original.take(MAX_LENGTH - suffix.length).trimEnd() + suffix
    }

    /** mm:ss, ou h:mm:ss a partir de 1 h. */
    fun time(millis: Long): String {
        val total = millis / MILLIS
        val h = total / SECONDS_PER_HOUR
        val m = total % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        val s = total % SECONDS_PER_MINUTE
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%02d:%02d", m, s)
    }

    private const val MILLIS = 1000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3600L
}
