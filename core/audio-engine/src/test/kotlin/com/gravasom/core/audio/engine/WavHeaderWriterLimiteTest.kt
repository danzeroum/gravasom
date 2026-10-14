package com.gravasom.core.audio.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Limite de 4 GiB do RIFF: o cabecalho nunca pode ser gravado com o tamanho truncado (wrap de Int). */
class WavHeaderWriterLimiteTest {

    private val gib = 1L shl 30

    @Test
    fun `dado dataSize acima do limite do RIFF quando construir o cabecalho entao recusa em vez de truncar`() {
        assertThrows(IllegalArgumentException::class.java) {
            WavHeaderWriter.build(sampleRate = 44_100, channels = 1, bitsPerSample = 16, dataSizeBytes = 5 * gib)
        }
    }

    @Test
    fun `dado dataSize exatamente no maior valor permitido quando construir entao o campo e lido sem sinal`() {
        val maximo = 0xFFFF_FFFFL - 36
        val header = WavHeaderWriter.build(44_100, 1, 16, maximo)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0xFFFF_FFFFL, buf.getInt(4).toLong() and 0xFFFF_FFFFL)
        assertEquals(maximo, buf.getInt(40).toLong() and 0xFFFF_FFFFL)
    }

    @Test
    fun `dado dataSize negativo quando construir entao recusa`() {
        assertThrows(IllegalArgumentException::class.java) {
            WavHeaderWriter.build(44_100, 1, 16, -1L)
        }
    }

    @Test
    fun `dado arquivo menor que o cabecalho quando calcular dataSize entao zero`() {
        assertEquals(0L, WavHeaderWriter.dataSizeFor(10L))
        assertEquals(0L, WavHeaderWriter.dataSizeFor(0L))
    }
}
