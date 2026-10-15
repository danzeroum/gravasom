package com.gravasom.core.audio.engine.edit

import java.io.EOFException
import java.io.File
import java.io.RandomAccessFile

/** Formato e posicao dos dados de um WAV PCM 16 bits. */
class WavInfo(val sampleRate: Int, val channels: Int, val dataOffset: Long, val dataBytes: Long) {
    val frames: Long get() = dataBytes / (BYTES_PER_SAMPLE * channels)

    companion object {
        const val BYTES_PER_SAMPLE = 2
        const val BITS = 16
    }
}

/** Le o cabecalho percorrendo os chunks ate "data"; aceita so PCM 16 bits mono/estereo. */
object WavReader {

    private const val FMT_BASE = 16L
    private const val MIN_RATE = 100L
    private const val MAX_RATE = 384_000L

    private class Fmt(val rate: Int, val channels: Int)

    fun read(file: File): WavInfo = RandomAccessFile(file, "r").use { raf ->
        try {
            require(tag(raf) == "RIFF") { "nao e RIFF" }
            le32(raf)
            require(tag(raf) == "WAVE") { "nao e WAVE" }
            findData(raf)
        } catch (e: EOFException) {
            throw IllegalArgumentException("WAV truncado", e)
        }
    }

    private fun findData(raf: RandomAccessFile): WavInfo {
        var fmt: Fmt? = null
        while (true) {
            val id = tag(raf)
            val size = le32(raf)
            when (id) {
                "fmt " -> fmt = readFmt(raf, size)
                "data" -> {
                    val f = requireNotNull(fmt) { "chunk data antes de fmt" }
                    val frameBytes = (WavInfo.BYTES_PER_SAMPLE * f.channels).toLong()
                    val bytes = minOf(size, raf.length() - raf.filePointer) / frameBytes * frameBytes
                    return WavInfo(f.rate, f.channels, raf.filePointer, bytes)
                }
                else -> skipChunk(raf, size)
            }
        }
    }

    private fun readFmt(raf: RandomAccessFile, size: Long): Fmt {
        val format = le16(raf)
        val channels = le16(raf)
        val rateLong = le32(raf)
        le32(raf)
        le16(raf)
        val bits = le16(raf)
        require(format == 1 && bits == WavInfo.BITS && channels in 1..2) { "so PCM 16 bits mono/estereo" }
        // Taxa fora da faixa: 0 dividia por zero, <100 Hz zerava o bloco de 10 ms do export (laco infinito) e >= 2^31 virava negativa.
        require(rateLong in MIN_RATE..MAX_RATE) { "taxa de amostragem invalida: $rateLong Hz" }
        if (size > FMT_BASE) skipChunk(raf, size - FMT_BASE, padding = false)
        if (size % 2 == 1L) skipChunk(raf, 1, padding = false)
        return Fmt(rateLong.toInt(), channels)
    }

    /** Pula [size] bytes (+1 de preenchimento RIFF se impar), recusando o que passa do fim do arquivo (size >= 2^31 virava negativo em toInt). */
    private fun skipChunk(raf: RandomAccessFile, size: Long, padding: Boolean = true) {
        val total = if (padding) size + (size and 1L) else size
        require(total <= raf.length() - raf.filePointer) { "chunk WAV maior que o arquivo" }
        raf.seek(raf.filePointer + total)
    }

    /** EOF vira EOFException (traduzida em "WAV truncado"); read() devolvia -1 e o erro aparecia como formato errado. */
    private fun le16(raf: RandomAccessFile): Int = raf.readUnsignedByte() or (raf.readUnsignedByte() shl 8)

    private fun le32(raf: RandomAccessFile): Long = le16(raf).toLong() or (le16(raf).toLong() shl 16)

    private fun tag(raf: RandomAccessFile): String = ByteArray(4).also { raf.readFully(it) }.toString(Charsets.US_ASCII)
}
