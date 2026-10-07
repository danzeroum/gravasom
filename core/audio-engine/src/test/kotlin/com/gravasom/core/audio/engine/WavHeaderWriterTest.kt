package com.gravasom.core.audio.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavHeaderWriterTest {

    @Test
    fun `cabecalho de 44100 mono 16 bits 10 segundos tem dataSize 882000 e riff size 882036`() {
        val header = WavHeaderWriter.build(
            sampleRate = 44_100,
            channels = 1,
            bitsPerSample = 16,
            dataSizeBytes = 882_000L,
        )
        assertEquals(44, header.size)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0x46464952, buf.getInt(0)) // "RIFF"
        assertEquals(882_036, buf.getInt(4))
        assertEquals(0x45564157, buf.getInt(8)) // "WAVE"
        assertEquals(882_000, buf.getInt(40))
    }
}
