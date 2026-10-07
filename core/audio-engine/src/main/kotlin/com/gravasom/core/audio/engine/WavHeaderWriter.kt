package com.gravasom.core.audio.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Escreve o cabecalho RIFF/WAVE de 44 bytes para PCM 16 bits.
 * Correcao ao final: o dataSize e derivado do tamanho real do arquivo.
 */
object WavHeaderWriter {

    const val HEADER_SIZE = 44

    fun build(
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        dataSizeBytes: Long,
    ): ByteArray {
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
