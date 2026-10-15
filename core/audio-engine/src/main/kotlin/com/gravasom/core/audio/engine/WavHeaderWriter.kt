package com.gravasom.core.audio.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Escreve o cabecalho RIFF/WAVE de 44 bytes para PCM 16 bits.
 * Correcao ao final: o dataSize e derivado do tamanho real do arquivo.
 */
object WavHeaderWriter {

    const val HEADER_SIZE = 44

    /** Maior area de dados que o RIFF de 32 bits consegue declarar (o campo e sem sinal). */
    const val MAX_DATA_BYTES = 0xFFFF_FFFFL - 36

    fun build(
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        dataSizeBytes: Long,
    ): ByteArray {
        // Acima do limite o campo daria a volta (toInt) e o leitor perderia o fim da gravacao em silencio.
        require(dataSizeBytes in 0..MAX_DATA_BYTES) { "dados WAV fora do limite de 4 GiB: $dataSizeBytes bytes" }
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val riffSize = (36 + dataSizeBytes).toInt()
        val buf = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(0x46464952) // "RIFF"
        buf.putInt(riffSize)
        buf.putInt(0x45564157) // "WAVE"
        buf.putInt(0x20746D66) // "fmt "
        buf.putInt(16)
        buf.putShort(1) // PCM
        buf.putShort(channels.toShort())
        buf.putInt(sampleRate)
        buf.putInt(byteRate)
        buf.putShort(blockAlign.toShort())
        buf.putShort(bitsPerSample.toShort())
        buf.putInt(0x61746164) // "data"
        buf.putInt(dataSizeBytes.toInt())
        return buf.array()
    }

    /** dataSize correto para um arquivo WAV de tamanho total informado. */
    fun dataSizeFor(totalFileBytes: Long): Long = (totalFileBytes - HEADER_SIZE).coerceAtLeast(0L)
}
