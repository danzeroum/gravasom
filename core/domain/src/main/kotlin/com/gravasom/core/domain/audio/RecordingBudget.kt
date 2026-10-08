package com.gravasom.core.domain.audio

/** Quanto espaco uma gravacao consome e quando avisar que o armazenamento esta acabando. */
object RecordingBudget {

    /** Abaixo disto (100 MB) o app avisa antes de gravar; o usuario ainda pode gravar mesmo assim. */
    const val LOW_SPACE_BYTES: Long = 100L * 1024L * 1024L

    private const val BITS_PER_BYTE = 8
    private const val BYTES_PER_KILOBIT = 1000L
    private const val PCM_BYTES_PER_SAMPLE = 2L
    private const val SECONDS_PER_MINUTE = 60L

    /** WAV: PCM 16 bits mono. M4A/MP3: bitrate em kbps (mono). */
    fun bytesPerSecond(format: RecordingFormat, sampleRateHz: Int, bitrateKbps: Int): Long = when (format) {
        RecordingFormat.WAV -> sampleRateHz.coerceAtLeast(1) * PCM_BYTES_PER_SAMPLE
        RecordingFormat.M4A, RecordingFormat.MP3 -> (bitrateKbps.coerceAtLeast(1) * BYTES_PER_KILOBIT / BITS_PER_BYTE).coerceAtLeast(1L)
    }

    /** Minutos inteiros (para baixo) que cabem em [freeBytes]. */
    fun estimatedMinutes(freeBytes: Long, format: RecordingFormat, sampleRateHz: Int, bitrateKbps: Int): Int {
        if (freeBytes <= 0L) return 0
        val seconds = freeBytes / bytesPerSecond(format, sampleRateHz, bitrateKbps)
        return (seconds / SECONDS_PER_MINUTE).toInt()
    }

    fun isLow(freeBytes: Long): Boolean = freeBytes < LOW_SPACE_BYTES
}
