package com.gravasom.core.audio.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecordingStateMachineTest {

    @Test
    fun `idle para preparing ao iniciar`() {
        val fsm = RecordingStateMachine()
        val result = fsm.transition(RecordingEvent.Start)
        assertEquals(RecordingState.Preparing, fsm.state)
        assertTrue(result is TransitionResult.Ok)
    }

    @Test
    fun `paused mais stop vai para stopping e depois saved`() {
        val fsm = RecordingStateMachine()
        fsm.transition(RecordingEvent.Start)
        fsm.transition(RecordingEvent.Prepared)
        fsm.transition(RecordingEvent.Pause)
        assertEquals(RecordingState.Paused, fsm.state)
        fsm.transition(RecordingEvent.Stop)
        assertEquals(RecordingState.Stopping, fsm.state)
        fsm.transition(RecordingEvent.Saved("/tmp/x.m4a"))
        assertEquals(RecordingState.Saved("/tmp/x.m4a"), fsm.state)
    }

    @Test
    fun `idle mais pause e transicao invalida e mantem o estado`() {
        val fsm = RecordingStateMachine()
        val result = fsm.transition(RecordingEvent.Pause)
        assertEquals(RecordingState.Idle, fsm.state)
        assertTrue(result is TransitionResult.Invalid)
    }
}
