package com.gravasom.core.audio.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LevelMeterTest {

    @Test
    fun `silencio retorna piso de -60 dB`() {
        val db = LevelMeter.rmsDb(ShortArray(100) { 0 })
        assertEquals(LevelMeter.MIN_DB, db)
    }

    @Test
    fun `array vazio retorna piso`() {
        assertEquals(LevelMeter.MIN_DB, LevelMeter.rmsDb(ShortArray(0)))
    }

    @Test
    fun `amplitude maxima fica proxima de 0 dB`() {
        val db = LevelMeter.rmsDb(ShortArray(100) { Short.MAX_VALUE })
        assertTrue(db > -1f, "esperado proximo de 0, veio $db")
    }

    @Test
    fun `meia amplitude fica proxima de -6 dB`() {
        val db = LevelMeter.rmsDb(ShortArray(100) { (Short.MAX_VALUE / 2).toShort() })
        assertTrue(db in -7f..-5f, "esperado ~-6, veio $db")
    }
}
