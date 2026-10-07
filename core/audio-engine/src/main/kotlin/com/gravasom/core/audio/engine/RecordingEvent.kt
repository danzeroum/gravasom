package com.gravasom.core.audio.engine

/** Eventos que disparam transicoes na maquina de estados. */
sealed interface RecordingEvent {
    data object Start : RecordingEvent
    data object Prepared : RecordingEvent
    data object Pause : RecordingEvent
    data object Resume : RecordingEvent
    data object Stop : RecordingEvent
    data class Saved(val path: String) : RecordingEvent
    data class Fail(val cause: RecordingError) : RecordingEvent
    data object Recover : RecordingEvent
}
