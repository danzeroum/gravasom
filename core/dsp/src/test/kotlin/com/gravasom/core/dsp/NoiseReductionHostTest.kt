package com.gravasom.core.dsp

import com.gravasom.core.audio.engine.WavHeaderWriter
import com.gravasom.core.audio.engine.edit.EditProject
import com.gravasom.core.audio.engine.edit.EditStep
import com.gravasom.core.audio.engine.edit.ExportResult
import com.gravasom.core.audio.engine.edit.ProjectExporter
import com.gravasom.core.audio.engine.edit.StepType
import com.gravasom.core.audio.engine.edit.WavReader
import com.gravasom.core.audio.engine.enhance.EnhancePipeline
import com.gravasom.core.audio.engine.enhance.EnhanceResult
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Redutor de ruido (RNNoise) pela ponte JNI, no host: o mesmo Kotlin + C++ que roda no celular. */
class NoiseReductionHostTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun reducao(intensity: Float) = EditProject(listOf(EditStep(StepType.NOISE_REDUCTION, listOf(intensity))))

    private fun exporter() = ProjectExporter { p, rate -> NativeBlockProcessor(p, rate) }

    private var counter = 0

    private fun wav(rate: Int, seconds: Double, noiseAmp: Double, toneAmp: Double = 0.25, seed: Int = 1): File {
        val n = (rate * seconds).toInt()
        val rnd = Random(seed)
        val f = tmp.newFile("in_${seed}_${counter++}.wav")
        RandomAccessFile(f, "rw").use { raf ->
            raf.write(WavHeaderWriter.build(rate, 1, 16, n * 2L))
            val pcm = ByteArray(n * 2)
            for (i in 0 until n) {
                val tone = toneAmp * sin(2 * PI * 300 * i / rate) * sin(2 * PI * 3 * i / rate)
                val v = ((tone + noiseAmp * (rnd.nextDouble() * 2 - 1)) * 32767).toInt().coerceIn(-32768, 32767)
                pcm[2 * i] = (v and 0xFF).toByte()
                pcm[2 * i + 1] = ((v shr 8) and 0xFF).toByte()
            }
            raf.write(pcm)
        }
        return f
    }

    private fun samples(f: File): ShortArray {
        val info = WavReader.read(f)
        val bytes = f.readBytes()
        return ShortArray(info.frames.toInt()) { i ->
            val o = info.dataOffset.toInt() + 2 * i
            ((bytes[o].toInt() and 0xFF) or (bytes[o + 1].toInt() shl 8)).toShort()
        }
    }

    private fun rms(x: ShortArray, from: Int, to: Int): Double {
        var s = 0.0
        for (i in from until to) s += x[i].toDouble() * x[i]
        return sqrt(s / (to - from))
    }

    private fun sha(f: File) = MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString("") { "%02x".format(it) }

    @Test
    fun `o passo declara latencia e o exportador mantem a duracao em amostras`() {
        assumeHostLib()
        for (rate in intArrayOf(48_000, 44_100, 16_000)) {
            val input = wav(rate, 2.0, 0.05)
            NativeBlockProcessor(reducao(0.5f), rate).use { assertTrue("latencia em $rate Hz", it.latencySamples > 0) }
            val out = File(tmp.root, "out_$rate.wav")
            assertTrue(exporter().export(input, reducao(0.5f), out) is ExportResult.Done)
            assertEquals(WavReader.read(input).frames, WavReader.read(out).frames)
        }
    }

    @Test
    fun `ruido branco constante cai com 100 por cento e o original nao muda`() {
        assumeHostLib()
        val input = wav(44_100, 4.0, 0.08, toneAmp = 0.0)
        val antes = sha(input)
        val out = File(tmp.root, "out.wav")
        exporter().export(input, reducao(1f), out)
        assertEquals(antes, sha(input))
        val a = samples(input)
        val b = samples(out)
        val queda = 20 * kotlin.math.log10(rms(a, 44_100 * 2, a.size) / rms(b, 44_100 * 2, b.size))
        assertTrue("queda de so $queda dB", queda > 10.0)
    }

    @Test
    fun `zero por cento e bit a bit identico e dois exports dao o mesmo hash`() {
        assumeHostLib()
        val input = wav(44_100, 2.0, 0.05)
        val zero = File(tmp.root, "zero.wav")
        exporter().export(input, reducao(0f), zero)
        assertArrayEquals(input.readBytes(), zero.readBytes())
        val a = File(tmp.root, "a.wav")
        val b = File(tmp.root, "b.wav")
        exporter().export(input, reducao(0.4f), a)
        exporter().export(input, reducao(0.4f), b)
        assertEquals(sha(a), sha(b))
        assertFalse(sha(a) == sha(input))
    }

    @Test
    fun `o pipeline de efeitos usa o redutor nativo e depois normaliza`() {
        assumeHostLib()
        val input = wav(16_000, 6.0, 0.03)
        val pipeline = EnhancePipeline { intensity, rate ->
            NativeBlockProcessor(reducao(intensity), rate)
        }
        val project = EditProject(
            listOf(EditStep(StepType.NOISE_REDUCTION, listOf(0.4f)), EditStep(StepType.NORMALIZE, listOf(-16f, -1f))),
        )
        val r = pipeline.run(input, project, File(tmp.root, "melhorado.wav"), File(tmp.root, "work"))
        assertTrue(r.toString(), r is EnhanceResult.Done)
        r as EnhanceResult.Done
        assertEquals(-16.0, r.report.lufsAfter!!, 1.0)
        assertTrue(r.report.truePeakAfterDbtp!! <= -1.0)
        assertEquals(WavReader.read(input).frames, WavReader.read(r.file).frames)
    }

    @Test
    fun `taxa fora de 8 a 96 kHz e recusada`() {
        assumeHostLib()
        org.junit.Assert.assertThrows(IllegalStateException::class.java) {
            NativeBlockProcessor(reducao(1f), 4_000)
        }
    }
}
