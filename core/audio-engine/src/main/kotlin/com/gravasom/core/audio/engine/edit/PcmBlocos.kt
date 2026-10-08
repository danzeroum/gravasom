package com.gravasom.core.audio.engine.edit

import java.io.DataInputStream
import java.io.OutputStream

/** Conversao entre bytes PCM16 intercalados e blocos float por canal. */
object PcmBlocos {

    private const val SCALE = 32768f
    private const val BYTES = WavInfo.BYTES_PER_SAMPLE

    /** Le ate [n] quadros; os que passam de [available] viram silencio (cauda de compensacao de latencia). */
    fun ler(input: DataInputStream, raw: ByteArray, floats: Array<FloatArray>, n: Int, available: Long) {
        val channels = floats.size
        val real = minOf(n.toLong(), available).toInt()
        if (real > 0) input.readFully(raw, 0, real * channels * BYTES)
        for (i in 0 until n) {
            for (c in 0 until channels) {
                floats[c][i] = if (i < real) {
                    val o = (i * channels + c) * BYTES
                    ((raw[o].toInt() and 0xFF) or (raw[o + 1].toInt() shl 8)).toShort() / SCALE
                } else {
                    0f
                }
            }
        }
    }

    fun escrever(out: OutputStream, floats: Array<FloatArray>, start: Int, count: Int) {
        val channels = floats.size
        val buf = ByteArray(count * channels * BYTES)
        for (i in 0 until count) {
            for (c in 0 until channels) {
                val v = Math.round(floats[c][start + i].coerceIn(-1f, 1f) * SCALE).coerceIn(-32768, 32767)
                val o = (i * channels + c) * BYTES
                buf[o] = (v and 0xFF).toByte()
                buf[o + 1] = ((v shr 8) and 0xFF).toByte()
            }
        }
        out.write(buf)
    }
}
