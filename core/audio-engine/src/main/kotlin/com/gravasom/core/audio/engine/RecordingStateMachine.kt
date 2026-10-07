package com.gravasom.core.audio.engine

/**
 * Maquina de estados pura da gravacao. Cada transicao retorna TransitionResult
 * sem lancar excecao: transicao invalida mantem o estado e sinaliza Invalid.
 */
class RecordingStateMachine {

    var state: RecordingState = RecordingState.Idle
        private set

    fun transition(event: RecordingEvent): TransitionResult {
        val next = nextState(state, event)
        return if (next == null) {
            TransitionResult.Invalid(state, event)
        } else {
            state = next
            TransitionResult.Ok
        }
    }

    private fun nextState(current: RecordingState, event: RecordingEvent): RecordingState? = when (current) {
        RecordingState.Idle -> fromIdle(event)
        RecordingState.Preparing -> fromPreparing(event)
        is RecordingState.Recording -> fromRecording(event)
        RecordingState.Paused -> fromPaused(event)
        RecordingState.Stopping -> fromStopping(event)
        is RecordingState.Saved -> fromSaved(event)
        is RecordingState.Error -> fromError(event)
    }

    private fun fromIdle(event: RecordingEvent): RecordingState? = when (event) {
        RecordingEvent.Start -> RecordingState.Preparing
        else -> null
    }

    private fun fromPreparing(event: RecordingEvent): RecordingState? = when (event) {
        RecordingEvent.Prepared -> RecordingState.Recording()
        RecordingEvent.Stop -> RecordingState.Stopping
        is RecordingEvent.Fail -> RecordingState.Error(event.cause)
        else -> null
    }

    private fun fromRecording(event: RecordingEvent): RecordingState? = when (event) {
        RecordingEvent.Pause -> RecordingState.Paused
        RecordingEvent.Stop -> RecordingState.Stopping
        is RecordingEvent.Fail -> RecordingState.Error(event.cause)
        else -> null
    }

    private fun fromPaused(event: RecordingEvent): RecordingState? = when (event) {
        RecordingEvent.Resume -> RecordingState.Recording()
        RecordingEvent.Stop -> RecordingState.Stopping
        is RecordingEvent.Fail -> RecordingState.Error(event.cause)
        else -> null
    }

    private fun fromStopping(event: RecordingEvent): RecordingState? = when (event) {
        is RecordingEvent.Saved -> RecordingState.Saved(event.path)
        is RecordingEvent.Fail -> RecordingState.Error(event.cause)
        else -> null
    }

    private fun fromSaved(event: RecordingEvent): RecordingState? = when (event) {
        RecordingEvent.Recover -> RecordingState.Idle
        else -> null
    }

    private fun fromError(event: RecordingEvent): RecordingState? = when (event) {
        RecordingEvent.Recover -> RecordingState.Idle
        else -> null
    }
}
