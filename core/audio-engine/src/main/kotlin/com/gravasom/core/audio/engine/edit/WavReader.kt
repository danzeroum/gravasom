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
                else -> raf.skipBytes((size + (size and 1L)).toInt())
            }
        }
    }

    private fun readFmt(raf: RandomAccessFile, size: Long): Fmt {
        val format = le16(raf)
        val channels = le16(raf)
        val rate = le32(raf).toInt()
        le32(raf)
        le16(raf)
        val bits = le16(raf)
        require(format == 1 && bits == WavInfo.BITS && channels in 1..2) { "so PCM 16 bits mono/estereo" }
        raf.skipBytes((size - FMT_BASE).toInt().coerceAtLeast(0))
        return Fmt(rate, channels)
    }

    private fun le16(raf: RandomAccessFile): Int = raf.read() or (raf.read() shl 8)

    private fun le32(raf: RandomAccessFile): Long = le16(raf).toLong() or (le16(raf).toLong() shl 16)

    private fun tag(raf: RandomAccessFile): String = ByteArray(4).also { raf.readFully(it) }.toString(Charsets.US_ASCII)
}
