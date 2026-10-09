package com.gravasom.core.service.clip

import com.gravasom.core.audio.engine.WavHeaderWriter
import com.gravasom.core.audio.engine.edit.ClipResult
import com.gravasom.core.domain.audio.RecordingFormat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile

/** Juntar WAV do mesmo formato: bytes PCM concatenados NA ORDEM, duracao somada e originais intocados. */
class JoinExporterTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val rate = 16_000

    /** PCM com padrao reconhecivel: a amostra i vale (base + i) para provar a ordem. */
    private fun wav(name: String, frames: Int, base: Int): File {
        val f = tmp.newFile(name)
        RandomAccessFile(f, "rw").use { raf ->
            raf.write(WavHeaderWriter.build(rate, 1, 16, frames * 2L))
            val pcm = ByteArray(frames * 2)
            for (i in 0 until frames) {
                val v = (base + i) % 256
                pcm[2 * i] = v.toByte()
                pcm[2 * i + 1] = 0
            }
            raf.write(pcm)
        }
        return f
    }

    private fun data(f: File): ByteArray = f.inputStream().use { it.readBytes() }.copyOfRange(HEADER_BYTES, f.length().toInt())

    @Test
    fun `junta na ordem dada com os PCM concatenados e a duracao somada`() {
        val a = wav("a.wav", rate * 2, base = 10) // 2 s
        val b = wav("b.wav", rate, base = 200) // 1 s
        val antesA = a.readBytes()
        val antesB = b.readBytes()
        val saida = tmp.newFile("juncao.wav")
        val r = JoinExporter().join(listOf(b, a), RecordingFormat.WAV, saida) // ordem explicita: b primeiro
        assertTrue(r.toString(), r is ClipResult.Done)
        r as ClipResult.Done
        assertEquals(3_000L, r.durationMillis)
        assertArrayEquals(data(b) + data(a), data(saida))
        assertArrayEquals(antesA, a.readBytes()) // originais intactos (criterio de aceite)
        assertArrayEquals(antesB, b.readBytes())
    }

    @Test
    fun `recusa taxas diferentes`() {
        val a = wav("a.wav", 100, base = 0)
        val outra = tmp.newFile("outra.wav")
        RandomAccessFile(outra, "rw").use { raf ->
            raf.write(WavHeaderWriter.build(8_000, 1, 16, 200L))
            raf.write(ByteArray(200))
        }
        val r = JoinExporter().join(listOf(a, outra), RecordingFormat.WAV, tmp.newFile("j.wav"))
        assertTrue(r.toString(), r is ClipResult.Failed)
    }

    @Test
    fun `recusa menos de duas gravacoes`() {
        val a = wav("a.wav", 100, base = 0)
        val r = JoinExporter().join(listOf(a), RecordingFormat.WAV, tmp.newFile("j.wav"))
        assertTrue(r.toString(), r is ClipResult.Failed)
    }

    private companion object {
        const val HEADER_BYTES = 44
    }
}
