package com.gravasom.core.audio.engine.enhance

import com.gravasom.core.audio.engine.edit.AudioBlockProcessor
import com.gravasom.core.audio.engine.edit.EditProject
import com.gravasom.core.audio.engine.edit.EditStep
import com.gravasom.core.audio.engine.edit.ExportResult
import com.gravasom.core.audio.engine.edit.ProjectExporter
import com.gravasom.core.audio.engine.edit.StepType
import com.gravasom.core.audio.engine.loudness.LoudnessAnalyzer
import com.gravasom.core.audio.engine.loudness.LoudnessReport
import com.gravasom.core.audio.engine.loudness.NormalizePlan
import com.gravasom.core.audio.engine.loudness.NormalizeProcessor
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.math.abs

/** Numeros da melhoria, para mostrar ao usuario e para os testes/harness. */
data class EnhanceReport(
    val lufsBefore: Double?,
    val lufsAfter: Double?,
    val truePeakAfterDbtp: Double?,
    /** Ganho aplicado pela normalizacao (0 se nao houve). */
    val gainDb: Double,
    /** Passagens de normalizacao (cada uma reescreve o arquivo para conferir o pico verdadeiro e a loudness). */
    val passes: Int,
)

/**
 * [fixedNormalizeGainDb]: usa EXATAMENTE este ganho na normalizacao (so confere o pico verdadeiro), em vez de medir o arquivo. A
 * previa de um trecho usa o ganho que a gravacao INTEIRA pede; medir so o trecho daria outro ganho.
 */
data class EnhanceOptions(val fixedNormalizeGainDb: Double? = null)

sealed interface EnhanceResult {
    data class Done(val file: File, val report: EnhanceReport) : EnhanceResult
    data object Cancelled : EnhanceResult
    data class Failed(val reason: String) : EnhanceResult
}

/**
 * Aplica o projeto de efeitos (redutor de ruido e "melhorar volume") a um WAV, gravando um arquivo NOVO; o original so e lido.
 *
 * Ordem: redutor primeiro, depois a normalizacao, que mede a loudness do que SAIU do redutor (o redutor tira energia).
 * A normalizacao confere o resultado: se o pico verdadeiro passar do teto ou a loudness errar o alvo por mais de 0,25 LU
 * (o limitador tira energia), refaz com teto/ganho ajustados, ate [MAX_PASSES] passagens. Efeito sem efeito (redutor a 0%,
 * passo desligado, bypass) devolve uma copia identica bit a bit.
 */
class EnhancePipeline(private val noiseReducer: (intensity: Float, sampleRate: Int) -> AudioBlockProcessor) {

    fun run(
        original: File,
        project: EditProject,
        destination: File,
        workDir: File,
        cancelled: () -> Boolean = { false },
        onProgress: (Float) -> Unit = {},
        options: EnhanceOptions = EnhanceOptions(),
    ): EnhanceResult {
        workDir.mkdirs()
        val created = mutableListOf<File>()
        return try {
            execute(original, project, destination, workDir, created, cancelled, onProgress, options)
        } catch (e: IOException) {
            File(destination.path + ".tmp").delete()
            EnhanceResult.Failed(e.message ?: "erro de E/S")
        } catch (e: IllegalArgumentException) {
            File(destination.path + ".tmp").delete()
            EnhanceResult.Failed(e.message ?: "arquivo invalido")
        } finally {
            created.forEach { it.delete() }
        }
    }

    private fun execute(
        original: File,
        project: EditProject,
        destination: File,
        workDir: File,
        created: MutableList<File>,
        cancelled: () -> Boolean,
        onProgress: (Float) -> Unit,
        options: EnhanceOptions,
    ): EnhanceResult {
        val active = project.steps.filter { it.enabled && !it.isNoOp }
        val nr = active.firstOrNull { it.type == StepType.NOISE_REDUCTION }
        val norm = active.firstOrNull { it.type == StepType.NORMALIZE }
        if (project.bypass || project.intensity <= 0f || (nr == null && norm == null)) {
            val copy = ProjectExporter { _, _ -> error("identidade nao processa") }.export(original, EditProject(), destination)
            return finish(copy, EnhanceReport(null, null, null, 0.0, 0))
        }
        val partial = if (norm == null) 1f else HALF
        val denoised = if (nr == null) original else {
            val target = if (norm == null) destination else File(workDir, "nr.wav").also { created += it }
            val r = nrExporter().export(original, EditProject(listOf(nr)), target, cancelled) { onProgress(it * partial) }
            if (r !is ExportResult.Done) return finish(r, EnhanceReport(null, null, null, 0.0, 0))
            if (norm == null) return EnhanceResult.Done(destination, EnhanceReport(null, null, null, 0.0, 0))
            target
        }
        return normalize(original, denoised, norm!!, destination, workDir, created, cancelled, onProgress, options)
    }

    private fun nrExporter() = ProjectExporter { p, rate ->
        val step = p.steps.single { it.type == StepType.NOISE_REDUCTION }
        noiseReducer(step.params.first().coerceIn(0f, 1f), rate)
    }

    private class Attempt(val file: File, val report: LoudnessReport, val gainDb: Double)

    private fun normalize(
        original: File,
        source: File,
        step: EditStep,
        destination: File,
        workDir: File,
        created: MutableList<File>,
        cancelled: () -> Boolean,
        onProgress: (Float) -> Unit,
        options: EnhanceOptions,
    ): EnhanceResult {
        val target = (step.params.getOrNull(0) ?: DEFAULT_TARGET).toDouble()
        val requested = (step.params.getOrNull(1) ?: DEFAULT_CEILING).toDouble()
        // Folga de 0,05 dB: um medidor independente pode divergir do nosso em centesimos de dB; o teto pedido vale para ele tambem.
        val ceiling = requested - PEAK_GUARD_DB
        val before = if (source == original) null else LoudnessAnalyzer.measure(original).lufs
        val input = LoudnessAnalyzer.measure(source)
        val lufsBefore = before ?: input.lufs
        if (input.lufs == null) { // silencio: nada a normalizar
            replace(source, destination)
            return EnhanceResult.Done(destination, EnhanceReport(lufsBefore, null, input.truePeakDbtp, 0.0, 0))
        }
        val fixed = options.fixedNormalizeGainDb
        var gain = fixed ?: NormalizePlan.gainDb(input.lufs, target)
        var effectiveCeiling = ceiling
        var best: Attempt? = null
        var passes = 0
        while (passes < MAX_PASSES) {
            passes++
            val out = File(workDir, "n$passes.wav").also { created += it }
            val g = gain
            val c = effectiveCeiling
            val exporter = ProjectExporter { _, rate -> NormalizeProcessor(rate, g, c) }
            val r = exporter.export(source, EditProject(listOf(step)), out, cancelled) {
                onProgress(HALF + it * HALF / MAX_PASSES)
            }
            if (r !is ExportResult.Done) return finish(r, EnhanceReport(lufsBefore, null, null, 0.0, passes))
            val attempt = Attempt(out, LoudnessAnalyzer.measure(out), g)
            if (isBetter(attempt, best, ceiling)) best = attempt
            val lufs = attempt.report.lufs ?: break
            val peakExcess = attempt.report.truePeakDbtp - ceiling
            val loudnessError = target - lufs
            if (peakExcess <= TP_TOLERANCE && (fixed != null || abs(loudnessError) <= LUFS_TOLERANCE)) break
            if (peakExcess > TP_TOLERANCE) effectiveCeiling -= peakExcess + PEAK_STEP
            if (fixed == null && abs(loudnessError) > LUFS_TOLERANCE) gain = (gain + loudnessError).coerceAtMost(NormalizePlan.MAX_GAIN_DB)
        }
        val chosen = checkNotNull(best)
        replace(chosen.file, destination)
        onProgress(1f)
        return EnhanceResult.Done(
            destination,
            EnhanceReport(lufsBefore, chosen.report.lufs, chosen.report.truePeakDbtp, chosen.gainDb, passes),
        )
    }

    /** Prefere quem respeita o teto de pico; entre iguais, quem erra menos o alvo (ou, sem alvo, o de menor pico). */
    private fun isBetter(a: Attempt, b: Attempt?, ceiling: Double): Boolean {
        if (b == null) return true
        val aOk = a.report.truePeakDbtp <= ceiling + TP_TOLERANCE
        val bOk = b.report.truePeakDbtp <= ceiling + TP_TOLERANCE
        return when {
            aOk != bOk -> aOk
            !aOk -> a.report.truePeakDbtp < b.report.truePeakDbtp
            else -> abs(a.report.lufs ?: Double.MAX_VALUE) < abs(b.report.lufs ?: Double.MAX_VALUE)
        }
    }

    /** Copia para `<destino>.tmp` e renomeia: nunca deixa um destino pela metade. */
    private fun replace(from: File, destination: File) {
        destination.parentFile?.mkdirs()
        val tmp = File(destination.path + ".tmp")
        Files.copy(from.toPath(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING)
        Files.move(tmp.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    private fun finish(r: ExportResult, report: EnhanceReport): EnhanceResult = when (r) {
        is ExportResult.Done -> EnhanceResult.Done(r.file, report)
        ExportResult.Cancelled -> EnhanceResult.Cancelled
        is ExportResult.Failed -> EnhanceResult.Failed(r.reason)
    }

    companion object {
        const val DEFAULT_TARGET = -16f
        const val DEFAULT_CEILING = -1f
        const val MAX_PASSES = 4
        private const val HALF = 0.5f
        private const val TP_TOLERANCE = 0.01
        private const val LUFS_TOLERANCE = 0.25
        private const val PEAK_STEP = 0.05
        private const val PEAK_GUARD_DB = 0.05
    }
}
