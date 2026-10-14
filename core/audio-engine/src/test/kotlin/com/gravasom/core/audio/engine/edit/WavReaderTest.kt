package com.gravasom.core.audio.engine.edit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Leitor de cabecalho WAV com arquivos malformados: nunca deve travar, divergir nem ler lixo. */
class WavReaderTest {

    @TempDir
    lateinit var dir: File

    private fun le(bytes: Int = 4096) = ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN)

    /** WAV com `fmt ` de [fmtSize] bytes (16 = padrao) seguido de [extra] chunk opcional e `data` de [dados] bytes. */
    private fun wav(
        rate: Int = 44_100,
        channels: Int = 1,
        fmtSize: Int = 16,
        extraChunkSize: Int = -1,
        dados: Int = 400,
        declaradoData: Long = dados.toLong(),
    ): File {
        val b = le()
        b.putInt(0x46464952).putInt(0).putInt(0x45564157) // RIFF, tamanho (ignorado), WAVE
        b.putInt(0x20746D66).putInt(fmtSize)
        b.putShort(1).putShort(channels.toShort()).putInt(rate).putInt(rate * channels * 2)
        b.putShort((channels * 2).toShort()).putShort(16)
        repeat(fmtSize - 16) { b.put(0) }
        if (fmtSize % 2 == 1) b.put(0) // padding RIFF
        if (extraChunkSize >= 0) {
            b.putInt(0x5453494C).putInt(extraChunkSize) // "LIST"
            repeat(extraChunkSize) { b.put(0x41) }
            if (extraChunkSize % 2 == 1) b.put(0)
        }
        b.putInt(0x61746164).putInt(declaradoData.toInt())
        repeat(dados) { b.put(1) }
        return File(dir, "t_${System.nanoTime()}.wav").apply { writeBytes(b.array().copyOf(b.position())) }
    }

    @Test
    fun `dado WAV valido quando ler entao devolve formato e posicao dos dados`() {
        val info = WavReader.read(wav(rate = 16_000, channels = 2, dados = 400))
        assertEquals(16_000, info.sampleRate)
        assertEquals(2, info.channels)
        assertEquals(100L, info.frames)
        assertEquals(44L, info.dataOffset)
    }

    @Test
    fun `dado fmt com bytes extras e chunk LIST de tamanho impar antes do data quando ler entao acha o data`() {
        val info = WavReader.read(wav(fmtSize = 18, extraChunkSize = 5, dados = 200))
        assertEquals(100L, info.frames)
    }

    @Test
    fun `dado taxa de amostragem zero quando ler entao recusa`() {
        assertThrows(IllegalArgumentException::class.java) { WavReader.read(wav(rate = 0)) }
    }

    @Test
    fun `dado taxa abaixo de 100 Hz quando ler entao recusa para nao gerar bloco de 10 ms vazio`() {
        assertThrows(IllegalArgumentException::class.java) { WavReader.read(wav(rate = 50)) }
    }

    @Test
    fun `dado taxa negativa por overflow de Int quando ler entao recusa`() {
        assertThrows(IllegalArgumentException::class.java) { WavReader.read(wav(rate = 0x8000_0000.toInt())) }
    }

    @Test
    fun `dado data declarado maior que o arquivo quando ler entao limita ao que existe e alinha ao quadro`() {
        val info = WavReader.read(wav(channels = 2, dados = 403, declaradoData = 1_000_000))
        assertEquals(100L, info.frames)
        assertEquals(400L, info.dataBytes)
    }

    @Test
    fun `dado fmt truncado quando ler entao erro de WAV invalido e nao leitura de lixo`() {
        val completo = wav()
        val truncado = File(dir, "trunc.wav").apply { writeBytes(completo.readBytes().copyOf(30)) }
        assertThrows(IllegalArgumentException::class.java) { WavReader.read(truncado) }
    }

    @Test
    fun `dado chunk desconhecido gigante quando ler entao falha sem pular para fora do arquivo`() {
        val b = le(64)
        b.putInt(0x46464952).putInt(0).putInt(0x45564157)
        b.putInt(0x54534554).putInt(Int.MIN_VALUE) // "TEST" com tamanho >= 2^31
        val f = File(dir, "gigante.wav").apply { writeBytes(b.array().copyOf(b.position())) }
        assertThrows(IllegalArgumentException::class.java) { WavReader.read(f) }
    }

    @Test
    fun `dado arquivo que nao e RIFF quando ler entao recusa`() {
        val f = File(dir, "lixo.wav").apply { writeText("isto nao e um wav de verdade, so texto") }
        assertThrows(IllegalArgumentException::class.java) { WavReader.read(f) }
    }
}
