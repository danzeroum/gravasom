package com.gravasom.core.dsp

import com.gravasom.core.audio.engine.analysis.AnalysisPipeline
import com.gravasom.core.audio.engine.analysis.SkipParams
import com.gravasom.core.audio.engine.analysis.SilenceSensitivity
import com.gravasom.core.audio.engine.analysis.SkipPlan
import com.gravasom.core.audio.engine.edit.WavReader
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

/**
 * Harness do VAD (ADR-014): roda o caminho REAL (decodificado -> reamostra 16 kHz -> WebRTC VAD por JNI -> SkipPlan)
 * sobre o corpus pt-BR com ruido gerado por scripts/vad_corpus.py e mede, contra o oraculo de energia do sinal limpo,
 * quanta fala o "pular silencio" removeria e quanto tempo economiza. So roda com GRAVASOM_VAD_CORPUS.
 */
class VadCorpusEvalTest {

    /** Trecho de fala do oraculo (em quadros) que o plano removeria. */
    private class Cut(val name: String, val mode: Int, val pad: Int, val startFrame: Int, val endFrame: Int)

    private val cuts = ArrayList<Cut>()

    private class Row(
        val name: String, val scenario: String, val mode: Int, val params: SkipParams,
        val runs: Int, val runsCut: Int, val frames: Int, val framesCut: Int, val savedMs: Long, val idealMs: Long, val totalMs: Long,
    )

    @Test
    fun `nenhum trecho de fala do corpus pt-BR e pulado no modo padrao e o tempo economizado e medido`() {
        val dir = System.getProperty("gravasom.vad.corpus").orEmpty()
        assumeTrue("corpus do VAD ausente (GRAVASOM_VAD_CORPUS)", dir.isNotEmpty())
        assumeHostLib()
        val manifest = File(dir, "manifest.tsv").readLines().filter { it.isNotBlank() }.map { it.split('\t') }
        val paramGrid = listOf(SkipParams(), SkipParams(headPadMillis = 300, tailPadMillis = 300), SkipParams(headPadMillis = 200, tailPadMillis = 500), SkipParams(headPadMillis = 300, tailPadMillis = 500))
        val rows = ArrayList<Row>()
        for (m in manifest) {
            val analysis = analyze(File(dir, m[1]))
            val oracle = File(dir, m[2]).readText().map { it == '1' }.toBooleanArray()
            for (mode in MODES) for (p in paramGrid) rows += score(m[0], m[3], mode, p, analysis.speechFrames(MODES.indexOf(mode)), oracle, analysis.durationMillis)
        }
        report(rows)
        // Politica (ADR-014): com os parametros padrao, NENHUM modo oferecido ao usuario corta um trecho de fala, em nenhum cenario.
        val defaults = rows.filter { it.params == SkipParams() }
        assertTrue("fala cortada: " + defaults.filter { it.runsCut > 0 }.map { "${it.name}/modo ${it.mode}: ${it.runsCut}" }, defaults.all { it.runsCut == 0 })
        // E o recurso serve para alguma coisa: em sala silenciosa o modo normal captura a maior parte da economia ideal.
        val limpo = defaults.filter { it.scenario == "limpo" && it.mode == SilenceSensitivity.NORMAL.vadMode }
        assertTrue("economia insuficiente em sala silenciosa", limpo.sumOf { it.savedMs } > limpo.sumOf { it.idealMs } * MIN_CAPTURE)
    }

    private fun analyze(wav: File): com.gravasom.core.audio.engine.analysis.RecordingAnalysis {
        val info = WavReader.read(wav)
        val classifiers = MODES.map { WebRtcVad(it) }
        try {
            val pipeline = AnalysisPipeline(info.sampleRate, info.channels, classifiers)
            RandomAccessFile(wav, "r").use { raf ->
                raf.seek(info.dataOffset)
                val chunkFrames = 4096
                val bytes = ByteArray(chunkFrames * info.channels * 2)
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

    private fun score(name: String, scenario: String, mode: Int, p: SkipParams, vad: BooleanArray, oracle: BooleanArray, durationMs: Long): Row {
        val plan = SkipPlan.build(vad, durationMs, p)
        val ideal = SkipPlan.build(oracle, durationMs, p)
        // trechos de fala do oraculo: quadros de fala unidos quando a lacuna e <= 100 ms; ignora trechos < 60 ms (estalos)
        var runs = 0
        var runsCut = 0
        var frames = 0
        var framesCut = 0
        var i = 0
        while (i < oracle.size) {
            if (!oracle[i]) { i++; continue }
            var end = i
            var j = i
            while (j < oracle.size && j - end <= MERGE_GAP_FRAMES) { if (oracle[j]) end = j; j++ }
            if (end - i + 1 >= MIN_RUN_FRAMES) {
                runs++
                var cut = false
                for (k in i..end) if (oracle[k]) {
                    frames++
                    if (plan.ranges.any { r -> k * FRAME_MS.toLong() < r.endMillis && (k + 1) * FRAME_MS.toLong() > r.startMillis }) { framesCut++; cut = true }
                }
                if (cut) {
                    runsCut++
                    cuts += Cut(name, mode, p.headPadMillis, i, end)
                }
            }
            i = end + 1
        }
        return Row(name, scenario, mode, p, runs, runsCut, frames, framesCut, plan.skippedMillis, ideal.skippedMillis, durationMs)
    }

    private fun report(rows: List<Row>) {
        val out = StringBuilder("arquivo\tcenario\tmodo\tpad_ms\ttrechos\ttrechos_cortados\tquadros_fala\tquadros_cortados\teconomia_ms\teconomia_ideal_ms\tduracao_ms\n")
        rows.forEach { out.append("${it.name}\t${it.scenario}\t${it.mode}\t${it.params.headPadMillis}/${it.params.tailPadMillis}\t${it.runs}\t${it.runsCut}\t${it.frames}\t${it.framesCut}\t${it.savedMs}\t${it.idealMs}\t${it.totalMs}\n") }
        System.getProperty("gravasom.vad.report")?.takeIf { it.isNotEmpty() }?.let { path ->
            File(path).apply { parentFile?.mkdirs() }.writeText(out.toString())
            File("$path.cortes").writeText(cuts.joinToString("\n") { "${it.name}\t${it.mode}\t${it.pad}\t${it.startFrame}\t${it.endFrame}" })
        }
    }

    private companion object {
        val MODES = SilenceSensitivity.entries.map { it.vadMode }
        const val MIN_CAPTURE = 0.7
        const val FRAME_MS = 20
        const val MERGE_GAP_FRAMES = 5
        const val MIN_RUN_FRAMES = 3
    }
}
