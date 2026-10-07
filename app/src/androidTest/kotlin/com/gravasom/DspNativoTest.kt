package com.gravasom

import com.gravasom.core.audio.engine.edit.EditProject
import com.gravasom.core.audio.engine.edit.EditStep
import com.gravasom.core.audio.engine.edit.ExportResult
import com.gravasom.core.audio.engine.edit.ProjectExporter
import com.gravasom.core.audio.engine.edit.StepType
import com.gravasom.core.dsp.NativeBlockProcessor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import kotlin.math.abs

/** Fase 13: o DSP C++ (JNI) no emulador: bypass identico, determinismo, sem vazamento, integracao com o exportador e tempo. */
class DspNativoTest {

    private val taxa = 48_000
    private val quadro = taxa / 100
    private val hp = EditProject(listOf(EditStep(StepType.HIGH_PASS, listOf(80f))))

    private fun sha(f: File) = MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString("") { "%02x".format(it) }

    private fun ruido(n: Int, semente: Long): FloatArray {
        val r = java.util.Random(semente)
        return FloatArray(n) { (r.nextGaussian() * 0.2).toFloat().coerceIn(-1f, 1f) }
    }

    @Test
    fun bypassEIntensidadeZeroNaoTocamNosBytes() {
        val entrada = ruido(quadro * 10, 1).also { it[7] = Float.NaN }
        for (proj in listOf(hp.copy(bypass = true), hp.copy(intensity = 0f))) {
            NativeBlockProcessor(proj, taxa).use { p ->
                val x = entrada.copyOf()
                p.process(x, x.size)
                assertArrayEquals(entrada.map { it.toRawBits() }.toIntArray(), x.map { it.toRawBits() }.toIntArray())
            }
        }
    }

    @Test
    fun determinismoEBlocosIguaisAoInteiro() {
        val entrada = ruido(taxa, 2)
        // determinismo: duas execucoes com o mesmo bloco
        fun porBlocos(): FloatArray = NativeBlockProcessor(hp, taxa).use { p ->
            val x = entrada.copyOf(); var i = 0
            while (i < x.size) { val b = x.copyOfRange(i, minOf(i + quadro, x.size)); p.process(b, b.size); b.copyInto(x, i); i += quadro }; x
        }
        fun inteiro(): FloatArray = NativeBlockProcessor(hp, taxa).use { p -> entrada.copyOf().also { p.process(it, it.size) } }
        assertArrayEquals(porBlocos().map { it.toRawBits() }.toIntArray(), porBlocos().map { it.toRawBits() }.toIntArray())
        val a = porBlocos(); val b = inteiro()
        assertTrue("blocos de 10 ms divergem do inteiro", a.indices.all { abs(a[it] - b[it]) <= 1f / 32768 })
    }

    @Test
    fun semVazamentoNativo() {
        val antes = NativeBlockProcessor.liveChains()
        repeat(300) { NativeBlockProcessor(hp, taxa).use { p -> p.process(ruido(quadro, it.toLong()), quadro) } }
        assertEquals("cadeias vivas", antes, NativeBlockProcessor.liveChains())
    }

    @Test
    fun exportComDspNativoReproduzMesmoHashEPreservaOriginal() {
        val original = Suporte.escreverWavSeno(File(Suporte.testsOut(), "dsp_original.wav"), 3.0, freqHz = 40.0, taxa = taxa)
        val antes = sha(original)
        val exportador = ProjectExporter { proj, rate -> NativeBlockProcessor(proj, rate) }
        val r1 = exportador.export(original, hp, File(Suporte.testsOut(), "dsp_export_1.wav"))
        val r2 = exportador.export(original, hp, File(Suporte.testsOut(), "dsp_export_2.wav"))
        assertTrue(r1 is ExportResult.Done && r2 is ExportResult.Done)
        assertEquals(sha(File(Suporte.testsOut(), "dsp_export_1.wav")), sha(File(Suporte.testsOut(), "dsp_export_2.wav")))
        assertNotEquals("40 Hz deveria ser atenuado", antes, sha(File(Suporte.testsOut(), "dsp_export_1.wav")))
        assertEquals(antes, sha(original))
        val bypass = exportador.export(original, hp.copy(bypass = true), File(Suporte.testsOut(), "dsp_export_bypass.wav"))
        assertTrue(bypass is ExportResult.Done)
        assertEquals(antes, sha(File(Suporte.testsOut(), "dsp_export_bypass.wav")))
    }

    @Test
    fun tempoPorQuadroP95AbaixoDaMetadeDoQuadroERtfDoExport() {
        val x = ruido(quadro, 3)
        NativeBlockProcessor(hp, taxa).use { p ->
            repeat(500) { p.process(x, quadro) } // aquecimento (JIT/caches)
            val tempos = DoubleArray(MEDIDAS) {
                val t0 = System.nanoTime(); p.process(x, quadro); (System.nanoTime() - t0) / 1e6
            }.sorted()
            val p50 = tempos[MEDIDAS / 2]; val p95 = tempos[(MEDIDAS * 95) / 100]; val p99 = tempos[(MEDIDAS * 99) / 100]
            Suporte.medida("dsp_quadro_10ms", "p50_ms", p50); Suporte.medida("dsp_quadro_10ms", "p95_ms", p95)
            Suporte.medida("dsp_quadro_10ms", "p99_ms", p99)
            assertTrue("p95=$p95 ms >= 5 ms (metade do quadro)", p95 < 5.0)
        }
        val longo = Suporte.escreverWavSeno(File(Suporte.testsOut(), "dsp_60s.wav"), 60.0, taxa = taxa)
        val t0 = System.nanoTime()
        val r = ProjectExporter { proj, rate -> NativeBlockProcessor(proj, rate) }.export(longo, hp, File(Suporte.testsOut(), "dsp_60s_out.wav"))
        val rtf = (System.nanoTime() - t0) / 1e9 / 60.0
        Suporte.medida("dsp_export_60s", "rtf", rtf)
        assertTrue(r is ExportResult.Done)
        assertTrue("RTF=$rtf", rtf < 0.3)
    }

    private companion object {
        const val MEDIDAS = 2_000
    }
}
