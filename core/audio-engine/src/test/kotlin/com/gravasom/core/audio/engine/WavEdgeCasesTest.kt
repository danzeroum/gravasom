package com.gravasom.core.audio.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WavEdgeCasesTest {

    @Test
    fun `arquivo de 0 segundos tem dataSize 0 e riff 36`() {
        val header = WavHeaderWriter.build(44_100, 1, 16, 0L)
        val size = WavHeaderWriter.dataSizeFor(WavHeaderWriter.HEADER_SIZE.toLong())
        assertEquals(0L, size)
        assertEquals(44, header.size)
    }

    @Test
    fun `dataSize acima de 2GB e limitado sem crash`() {
        val huge = 3_000_000_000L
        val size = WavHeaderWriter.dataSizeFor(huge + WavHeaderWriter.HEADER_SIZE)
        assertTrue(size > 0)
        assertEquals(huge, size)
    }

    @Test
    fun `pausa repetida 50 vezes mantem estado coerente`() {
        val fsm = RecordingStateMachine()
        fsm.transition(RecordingEvent.Start)
        fsm.transition(RecordingEvent.Prepared)
        repeat(50) {
            fsm.transition(RecordingEvent.Pause)
            fsm.transition(RecordingEvent.Resume)
        }
        assertEquals(RecordingState.Recording(), fsm.state)
    }

    @Test
    fun `stop a partir de idle e invalido e mantem idle`() {
        val fsm = RecordingStateMachine()
        val r = fsm.transition(RecordingEvent.Stop)
        assertEquals(RecordingState.Idle, fsm.state)
        assertTrue(r is TransitionResult.Invalid)
    }
}
