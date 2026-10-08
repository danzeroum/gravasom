package com.gravasom.core.audio.engine.edit

import com.gravasom.core.audio.engine.WavHeaderWriter
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class WavClipperTest {

    @TempDir
    lateinit var dir: File

    /** WAV mono 16 bits a 8 kHz cujos bytes sao uma rampa previsivel: (indice % 251). */
    private fun wav(seconds: Int, rate: Int = 8_000, channels: Int = 1): File {
        val data = ByteArray(seconds * rate * channels * 2) { (it % 251).toByte() }
        return File(dir, "src.wav").also {
            it.writeBytes(WavHeaderWriter.build(rate, channels, 16, data.size.toLong()) + data)
        }
    }

    @Test
    fun `recorta o intervalo em bytes identicos e escreve cabecalho novo`() {
        val src = wav(10)
        val out = File(dir, "clip.wav")
        val r = WavClipper.clip(src, startMillis = 2_000, endMillis = 3_500, destination = out) as ClipResult.Done
        assertEquals(1_500L, r.durationMillis)
        val info = WavReader.read(out)
        assertEquals(8_000, info.sampleRate)
        assertEquals(1, info.channels)
        assertEquals(1_500L * 16, info.dataBytes) // 16 bytes por ms
        val expected = src.readBytes().copyOfRange(44 + 2_000 * 16, 44 + 3_500 * 16)
        assertArrayEquals(expected, out.readBytes().copyOfRange(44, out.readBytes().size))
    }

    @Test
    fun `estereo corta em quadros inteiros`() {
        val src = wav(4, rate = 8_000, channels = 2)
        val out = File(dir, "clip.wav")
        WavClipper.clip(src, 1_001, 2_003, out)
        val info = WavReader.read(out)
        assertEquals(0L, info.dataBytes % 4)
        assertTrue(info.frames in 8_000L..8_030L)
    }

    @Test
    fun `fim alem da gravacao e limitado ao fim e inicio negativo vira zero`() {
        val src = wav(3)
        val out = File(dir, "clip.wav")
        val r = WavClipper.clip(src, -500, 99_000, out) as ClipResult.Done
        assertEquals(3_000L, r.durationMillis)
    }

    @Test
    fun `intervalo vazio ou invertido falha sem deixar arquivo`() {
        val src = wav(3)
        val out = File(dir, "clip.wav")
        assertTrue(WavClipper.clip(src, 2_000, 2_000, out) is ClipResult.Failed)
        assertTrue(WavClipper.clip(src, 2_500, 1_000, out) is ClipResult.Failed)
        assertTrue(WavClipper.clip(src, 5_000, 6_000, out) is ClipResult.Failed) // depois do fim
        assertFalse(out.exists())
        assertFalse(File(out.path + ".tmp").exists())
    }

    @Test
    fun `arquivo que nao e wav falha com mensagem e sem lixo`() {
        val bad = File(dir, "x.wav").also { it.writeText("isto nao e um wav") }
        val out = File(dir, "clip.wav")
        val r = WavClipper.clip(bad, 0, 1_000, out)
        assertTrue(r is ClipResult.Failed)
        assertFalse(out.exists())
    }

    @Test
    fun `trecho grande e copiado em blocos sem estourar memoria`() {
        val src = wav(600) // 9,6 MB
        val out = File(dir, "clip.wav")
        val r = WavClipper.clip(src, 10_000, 590_000, out) as ClipResult.Done
        assertEquals(580_000L, r.durationMillis)
        assertEquals(580_000L * 16, WavReader.read(out).dataBytes)
    }
}
