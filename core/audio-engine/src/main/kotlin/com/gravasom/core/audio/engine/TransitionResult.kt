package com.gravasom.core.audio.engine

sealed interface TransitionResult {
    data object Ok : TransitionResult
    data class Invalid(val from: RecordingState, val event: RecordingEvent) : TransitionResult
}
