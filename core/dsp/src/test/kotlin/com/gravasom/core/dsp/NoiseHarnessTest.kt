package com.gravasom.core.dsp

import com.gravasom.core.audio.engine.edit.EditProject
import com.gravasom.core.audio.engine.edit.EditStep
import com.gravasom.core.audio.engine.edit.ExportResult
import com.gravasom.core.audio.engine.edit.ProjectExporter
import com.gravasom.core.audio.engine.edit.StepType
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Entrada do harness do redutor de ruido (scripts/onda2-ruido.sh): le os WAV de GRAVASOM_ONDA2_RUIDO_IN, aplica o redutor NATIVO (o mesmo
 * C++ do celular, via JNI no host) com a intensidade de GRAVASOM_ONDA2_INTENSIDADE e grava em GRAVASOM_ONDA2_RUIDO_OUT. Quem MEDE
 * (SI-SDRi, STOI) e o script Python. Sem as variaveis, o teste e ignorado.
 */
class NoiseHarnessTest {

    @Test
    fun reduzRuidoDoCorpus() {
        val input = System.getenv("GRAVASOM_ONDA2_RUIDO_IN") ?: return
        assumeHostLib()
        val out = File(System.getenv("GRAVASOM_ONDA2_RUIDO_OUT")!!).apply { mkdirs() }
        val intensity = (System.getenv("GRAVASOM_ONDA2_INTENSIDADE") ?: "0.4").toFloat()
        val project = EditProject(listOf(EditStep(StepType.NOISE_REDUCTION, listOf(intensity))))
        val exporter = ProjectExporter { p, rate -> NativeBlockProcessor(p, rate) }
        val files = File(input).listFiles { f -> f.extension == "wav" && !f.name.endsWith("_ref.wav") }!!.sortedBy { it.name }
        assertTrue("sem WAV em $input", files.isNotEmpty())
        for (f in files) {
            val r = exporter.export(f, project, File(out, f.name))
            check(r is ExportResult.Done) { "$f: $r" }
        }
    }
}
