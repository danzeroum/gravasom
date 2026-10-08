package com.gravasom.core.dsp

import com.gravasom.core.audio.engine.analysis.AnalysisPipeline
import com.gravasom.core.audio.engine.analysis.RecordingAnalysis
import com.gravasom.core.audio.engine.analysis.SilenceSensitivity
import com.gravasom.core.audio.engine.analysis.SkipParams
import com.gravasom.core.audio.engine.analysis.SkipPlan
import com.gravasom.core.audio.engine.edit.WavReader
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

/**
 * Harness do VAD (ADR-014): roda o caminho REAL (decodificado -> reamostra 16 kHz -> WebRTC VAD por JNI -> SkipPlan)
 * sobre o corpus pt-BR com ruido gerado por scripts/vad_corpus.py e mede, contra o oraculo do sinal limpo, quanta fala o
 * "pular silencio" removeria e quanto tempo economiza. So roda com GRAVASOM_VAD_CORPUS.
 */
class VadCorpusEvalTest {

    private class Entry(val name: String, val wav: String, val oracle: String, val scenario: String)

    private class Row(
        val entry: Entry, val mode: Int, val params: SkipParams, val score: Score,
        val savedMs: Long, val idealMs: Long, val totalMs: Long,
    )

    private val rows = ArrayList<Row>()
    private val plans = ArrayList<String>()

    @Test
    fun `nenhum trecho de fala do corpus pt-BR e pulado nos niveis oferecidos e o tempo economizado e medido`() {
        val dir = System.getProperty("gravasom.vad.corpus").orEmpty()
        assumeTrue("corpus do VAD ausente (GRAVASOM_VAD_CORPUS)", dir.isNotEmpty())
        assumeHostLib()
        val manifest = File(dir, "manifest.tsv").readLines().filter { it.isNotBlank() }.map { it.split('\t') }
        for (m in manifest) {
            val entry = Entry(m[0], m[1], m[2], m[3])
            val analysis = analyze(File(dir, entry.wav))
            val oracle = File(dir, entry.oracle).readText().map { it == '1' }.toBooleanArray()
            for (mode in MODES) for (p in PARAM_GRID) rows += scoreOne(entry, mode, p, analysis, oracle)
        }
        report()
        // Politica (ADR-014): com as margens padrao, NENHUM nivel oferecido ao usuario corta um trecho de fala, em nenhum cenario.
        val defaults = rows.filter { it.params == SkipParams() && it.mode in OFFERED }
        val cut = defaults.filter { it.score.runsCut > 0 }.map { "${it.entry.name}/modo ${it.mode}: ${it.score.runsCut}" }
        assertTrue("fala cortada: $cut", cut.isEmpty())
        // E o recurso serve para alguma coisa: em sala silenciosa o nivel normal captura a maior parte da economia ideal.
        val quiet = defaults.filter { it.entry.scenario == "limpo" && it.mode == SilenceSensitivity.NORMAL.vadMode }
        assertTrue("economia insuficiente em sala silenciosa", quiet.sumOf { it.savedMs } > quiet.sumOf { it.idealMs } * MIN_CAPTURE)
    }

    private fun scoreOne(entry: Entry, mode: Int, p: SkipParams, analysis: RecordingAnalysis, oracle: BooleanArray): Row {
        val plan = SkipPlan.build(analysis.speechFrames(MODES.indexOf(mode)), analysis.durationMillis, p)
        if (p == SkipParams() && mode in OFFERED) {
            plans += "${entry.name}\t$mode\t" + plan.ranges.joinToString(";") { "${it.startMillis}-${it.endMillis}" }
        }
        val ideal = SkipPlan.build(oracle, analysis.durationMillis, p)
        return Row(entry, mode, p, VadScoring.score(oracle, plan), plan.skippedMillis, ideal.skippedMillis, analysis.durationMillis)
    }

    private fun analyze(wav: File): RecordingAnalysis {
        val info = WavReader.read(wav)
        val classifiers = MODES.map { WebRtcVad(it) }
        try {
            val pipeline = AnalysisPipeline(info.sampleRate, info.channels, classifiers)
            RandomAccessFile(wav, "r").use { raf ->
                raf.seek(info.dataOffset)
                val bytes = ByteArray(CHUNK_FRAMES * info.channels * 2)
                var left = info.dataBytes
                while (left > 0) {
                    val n = minOf(left, bytes.size.toLong()).toInt()
                    raf.readFully(bytes, 0, n)
                    val pcm = ShortArray(n / 2) { ((bytes[it * 2 + 1].toInt() shl 8) or (bytes[it * 2].toInt() and 0xFF)).toShort() }
                    pipeline.add(pcm, 0, pcm.size)
                    left -= n
                }
            }
            return pipeline.finish()
        } finally {
            classifiers.forEach { it.close() }
        }
    }

    private fun report() {
        val path = System.getProperty("gravasom.vad.report")?.takeIf { it.isNotEmpty() } ?: return
        val header = listOf(
            "arquivo", "cenario", "modo", "pad_ms", "trechos", "trechos_cortados", "quadros_fala", "quadros_cortados",
            "economia_ms", "economia_ideal_ms", "duracao_ms",
        ).joinToString("\t")
        val lines = rows.map { r ->
            listOf(
                r.entry.name, r.entry.scenario, r.mode, "${r.params.headPadMillis}/${r.params.tailPadMillis}",
                r.score.runs, r.score.runsCut, r.score.frames, r.score.framesCut, r.savedMs, r.idealMs, r.totalMs,
            ).joinToString("\t")
        }
        File(path).apply { parentFile?.mkdirs() }.writeText((listOf(header) + lines).joinToString("\n") + "\n")
        File("$path.planos").writeText(plans.joinToString("\n"))
    }

    private companion object {
        /** Os niveis oferecidos mais o modo 3 do VAD, medido so para registrar por que NAO e oferecido (ADR-014). */
        val OFFERED = SilenceSensitivity.entries.map { it.vadMode }
        val MODES = OFFERED + 3

        /** Padrao primeiro; os demais mostram o que as margens custam e por que a cauda e maior que a cabeca. */
        val PARAM_GRID = listOf(
            SkipParams(),
            SkipParams(headPadMillis = 300, tailPadMillis = 300),
            SkipParams(headPadMillis = 150, tailPadMillis = 400),
            SkipParams(headPadMillis = 250, tailPadMillis = 700),
        )
        const val MIN_CAPTURE = 0.7
        const val CHUNK_FRAMES = 4096
    }
}
