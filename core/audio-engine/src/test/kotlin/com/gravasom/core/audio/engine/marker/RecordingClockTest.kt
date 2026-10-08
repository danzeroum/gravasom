package com.gravasom.core.audio.engine.marker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecordingClockTest {

    private var now = 0L
    private val clock = RecordingClock { now }

    @Test
    fun `antes de comecar o tempo e zero`() {
        now = 5_000
        assertEquals(0L, clock.elapsedMillis())
    }

    @Test
    fun `conta so o tempo gravando e nao o de pausa`() {
        now = 1_000
        clock.start()
        now = 6_000
        assertEquals(5_000L, clock.elapsedMillis())
        clock.pause()
        now = 9_000
        assertEquals(5_000L, clock.elapsedMillis())
        clock.resume()
        now = 10_000
        assertEquals(6_000L, clock.elapsedMillis())
    }

    @Test
    fun `pausar duas vezes ou retomar sem pausar nao altera o tempo`() {
        now = 0
        clock.start()
        now = 2_000
        clock.resume() // ja gravando
        clock.pause()
        now = 3_000
        clock.pause() // ja pausado
        now = 4_000
        assertEquals(2_000L, clock.elapsedMillis())
    }

    @Test
    fun `parar congela o tempo`() {
        now = 0
        clock.start()
        now = 7_000
        clock.stop()
        now = 20_000
        assertEquals(7_000L, clock.elapsedMillis())
    }

    @Test
    fun `relogio que anda para tras nunca da tempo negativo`() {
        now = 5_000
        clock.start()
        now = 1_000
        assertEquals(0L, clock.elapsedMillis())
    }
}
