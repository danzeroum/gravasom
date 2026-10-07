package com.gravasom.core.audio.engine

/**
 * Estado da gravação. Puro, sem dependência de Android.
 * Idle -> Preparing -> Recording <-> Paused -> Stopping -> Saved, com Error alcancavel.
 */
sealed interface RecordingState {
    data object Idle : RecordingState
    data object Preparing : RecordingState
    data class Recording(val startedAtMillis: Long = 0L) : RecordingState
    data object Paused : RecordingState
    data object Stopping : RecordingState
    data class Saved(val path: String) : RecordingState
    data class Error(val cause: RecordingError) : RecordingState
}
