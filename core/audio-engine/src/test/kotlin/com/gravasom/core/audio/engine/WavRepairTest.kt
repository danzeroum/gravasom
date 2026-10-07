package com.gravasom.core.audio.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class WavRepairTest {

    @Test
    fun `arquivo menor que o cabecalho nao tem reparo`() {
        assertNull(WavRepair.headerFor(40L))
    }

    @Test
    fun `arquivo com 10 s de PCM 44100 mono 16 bits tem duracao 10000 ms`() {
        val dataSize = 44_100L * 2 * 10
        val total = dataSize + WavHeaderWriter.HEADER_SIZE
        val header = WavRepair.headerFor(total)
        assertNotNull(header)
        assertEquals(10_000L, WavRepair.durationMillis(header!!.dataSizeBytes, 44_100, 1, 16))
    }

    @Test
    fun `reparo de cabecalho de 30 s fecha a duracao`() {
        val dataSize = 44_100L * 2 * 30
        val total = dataSize + WavHeaderWriter.HEADER_SIZE
        val header = WavRepair.headerFor(total)
        assertEquals(dataSize, header!!.dataSizeBytes)
        assertEquals(30_000L, WavRepair.durationMillis(header.dataSizeBytes, 44_100, 1, 16))
    }
}
