package com.gravasom.core.audio.engine.enhance

import com.gravasom.core.audio.engine.edit.EditProject
import com.gravasom.core.audio.engine.edit.EditStep
import com.gravasom.core.audio.engine.edit.StepType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File

/**
 * Entrada do harness de "Melhorar volume" (scripts/onda2-normalizacao.sh): le os WAV de GRAVASOM_ONDA2_IN, aplica a normalizacao
 * e grava em GRAVASOM_ONDA2_OUT. Quem MEDE e o script Python (pyloudnorm + ffmpeg ebur128), independente deste codigo.
 * So roda quando as variaveis existem.
 */
@EnabledIfEnvironmentVariable(named = "GRAVASOM_ONDA2_IN", matches = ".+")
class NormalizeHarnessTest {

    @Test
    fun normalizaOCorpus() {
        val entrada = File(System.getenv("GRAVASOM_ONDA2_IN"))
        val saida = File(System.getenv("GRAVASOM_ONDA2_OUT")).apply { mkdirs() }
        val trabalho = File(saida, ".work")
        val alvo = (System.getenv("GRAVASOM_ONDA2_ALVO") ?: "-16").toFloat()
        val arquivos = entrada.listFiles { f -> f.extension == "wav" }!!.sortedBy { it.name }
        assertTrue(arquivos.isNotEmpty(), "sem WAV em $entrada")
        val pipeline = EnhancePipeline { _, _ -> error("sem redutor neste harness") }
        val projeto = EditProject(listOf(EditStep(StepType.NORMALIZE, listOf(alvo, -1f))))
        val linhas = StringBuilder("arquivo\tganho_db\tlufs_antes\tlufs_depois\tpico_dbtp\tpassagens\n")
        for (f in arquivos) {
            val r = pipeline.run(f, projeto, File(saida, f.name), trabalho)
            check(r is EnhanceResult.Done) { "$f: $r" }
            val rel = r.report
            val linha = "${f.name}\t%.3f\t%.3f\t%.3f\t%.3f\t%d\n"
            linhas.append(linha.format(rel.gainDb, rel.lufsBefore, rel.lufsAfter, rel.truePeakAfterDbtp, rel.passes))
        }
        File(saida, "interno.tsv").writeText(linhas.toString())
        trabalho.deleteRecursively()
    }
}
