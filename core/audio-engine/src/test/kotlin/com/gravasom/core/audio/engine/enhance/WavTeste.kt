package com.gravasom.core.audio.engine.enhance

import com.gravasom.core.audio.engine.WavHeaderWriter
import com.gravasom.core.audio.engine.edit.WavInfo
import com.gravasom.core.audio.engine.edit.WavReader
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

/** WAV PCM16 de teste a partir de canais float. */
internal object WavTeste {

    fun escrever(file: File, taxa: Int, vararg canais: FloatArray): File {
        val n = canais[0].size
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.write(WavHeaderWriter.build(taxa, canais.size, 16, n.toLong() * canais.size * 2))
            val pcm = ByteArray(n * canais.size * 2)
            for (i in 0 until n) {
                for (c in canais.indices) {
                    val v = Math.round(canais[c][i].coerceIn(-1f, 1f) * 32768f).coerceIn(-32768, 32767)
                    val o = (i * canais.size + c) * 2
                    pcm[o] = (v and 0xFF).toByte()
                    pcm[o + 1] = ((v shr 8) and 0xFF).toByte()
                }
            }
            raf.write(pcm)
        }
        return file
    }

    fun ler(file: File): Array<FloatArray> {
        val info: WavInfo = WavReader.read(file)
        val bytes = file.readBytes()
        val out = Array(info.channels) { FloatArray(info.frames.toInt()) }
        for (i in 0 until info.frames.toInt()) {
            for (c in 0 until info.channels) {
                val o = info.dataOffset.toInt() + (i * info.channels + c) * 2
                out[c][i] = (((bytes[o].toInt() and 0xFF) or (bytes[o + 1].toInt() shl 8)).toShort()) / 32768f
            }
        }
        return out
    }

    fun sha256(f: File): String = MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString("") { "%02x".format(it) }
}
