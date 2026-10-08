package com.gravasom.core.domain.metrics

import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.model.Recording

/** Numeros do proprio usuario, derivados da biblioteca (valem mesmo sem opt-in; nada sai do aparelho). */
data class LibraryStats(
    val recordings: Int,
    val totalDurationMillis: Long,
    val mostUsedFormat: RecordingFormat?,
) {
    companion object {
        fun of(recordings: List<Recording>): LibraryStats {
            val active = recordings.filter { !it.isDeleted }
            val top = active.groupingBy { it.format }.eachCount().maxByOrNull { it.value }?.key
            return LibraryStats(active.size, active.sumOf { it.durationMillis }, top)
        }
    }
}
