package com.gravasom.core.audio.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecordingStateMachineCoverageTest {

    @Test
    fun `fail em preparing vai para error`() {
        val fsm = RecordingStateMachine()
        fsm.transition(RecordingEvent.Start)
        fsm.transition(RecordingEvent.Fail(RecordingError.MIC_IN_USE))
        assertEquals(RecordingState.Error(RecordingError.MIC_IN_USE), fsm.state)
    }

    @Test
    fun `fail em recording e em paused vao para error`() {
        val fsm = RecordingStateMachine()
        fsm.transition(RecordingEvent.Start)
        fsm.transition(RecordingEvent.Prepared)
        fsm.transition(RecordingEvent.Fail(RecordingError.NO_SPACE))
        assertEquals(RecordingState.Error(RecordingError.NO_SPACE), fsm.state)
    }

    @Test
    fun `fail em stopping vai para error`() {
        val fsm = RecordingStateMachine()
        fsm.transition(RecordingEvent.Start)
        fsm.transition(RecordingEvent.Prepared)
        fsm.transition(RecordingEvent.Stop)
        fsm.transition(RecordingEvent.Fail(RecordingError.ENCODER_FAILURE))
        assertEquals(RecordingState.Error(RecordingError.ENCODER_FAILURE), fsm.state)
    }

    @Test
    fun `recover de error volta para idle`() {
        val fsm = RecordingStateMachine()
        fsm.transition(RecordingEvent.Start)
        fsm.transition(RecordingEvent.Fail(RecordingError.UNKNOWN))
        fsm.transition(RecordingEvent.Recover)
        assertEquals(RecordingState.Idle, fsm.state)
    }

    @Test
    fun `recover de saved volta para idle`() {
        val fsm = RecordingStateMachine()
        fsm.transition(RecordingEvent.Start)
        fsm.transition(RecordingEvent.Prepared)
        fsm.transition(RecordingEvent.Stop)
        fsm.transition(RecordingEvent.Saved("/tmp/a.wav"))
        fsm.transition(RecordingEvent.Recover)
        assertEquals(RecordingState.Idle, fsm.state)
    }

    @Test
    fun `preparing mais stop vai para stopping`() {
        val fsm = RecordingStateMachine()
        fsm.transition(RecordingEvent.Start)
        fsm.transition(RecordingEvent.Stop)
        assertEquals(RecordingState.Stopping, fsm.state)
    }

    @Test
    fun `eventos invalidos em varios estados retornam invalid`() {
        val fsm = RecordingStateMachine()
        assertTrue(fsm.transition(RecordingEvent.Prepared) is TransitionResult.Invalid)
        fsm.transition(RecordingEvent.Start)
        assertTrue(fsm.transition(RecordingEvent.Pause) is TransitionResult.Invalid)
    }
}
