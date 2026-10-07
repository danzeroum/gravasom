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
        RecordingState.Idle -> when (event) {
            RecordingEvent.Start -> RecordingState.Preparing
            else -> null
        }
        RecordingState.Preparing -> when (event) {
            RecordingEvent.Prepared -> RecordingState.Recording()
            RecordingEvent.Stop -> RecordingState.Stopping
            is RecordingEvent.Fail -> RecordingState.Error(event.cause)
            else -> null
        }
        is RecordingState.Recording -> when (event) {
            RecordingEvent.Pause -> RecordingState.Paused
            RecordingEvent.Stop -> RecordingState.Stopping
            is RecordingEvent.Fail -> RecordingState.Error(event.cause)
            else -> null
        }
        RecordingState.Paused -> when (event) {
            RecordingEvent.Resume -> RecordingState.Recording()
            RecordingEvent.Stop -> RecordingState.Stopping
            is RecordingEvent.Fail -> RecordingState.Error(event.cause)
            else -> null
        }
        RecordingState.Stopping -> when (event) {
            is RecordingEvent.Saved -> RecordingState.Saved(event.path)
            is RecordingEvent.Fail -> RecordingState.Error(event.cause)
            else -> null
        }
        is RecordingState.Saved -> when (event) {
            RecordingEvent.Recover -> RecordingState.Idle
            else -> null
        }
        is RecordingState.Error -> when (event) {
            RecordingEvent.Recover -> RecordingState.Idle
            else -> null
        }
    }
}
