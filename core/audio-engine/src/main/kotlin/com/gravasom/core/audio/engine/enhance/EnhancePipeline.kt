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
        val ctx = Run(original, destination, workDir, mutableListOf(), cancelled, onProgress)
        return try {
            execute(ctx, project, options)
        } catch (e: IOException) {
            File(destination.path + ".tmp").delete()
            EnhanceResult.Failed(e.message ?: "erro de E/S")
        } catch (e: IllegalArgumentException) {
            File(destination.path + ".tmp").delete()
            EnhanceResult.Failed(e.message ?: "arquivo invalido")
        } catch (e: IllegalStateException) {
            // DSP nativo indisponivel/falha no processamento (CancellationException continua subindo).
            if (e is kotlinx.coroutines.CancellationException) throw e
            File(destination.path + ".tmp").delete()
            EnhanceResult.Failed(e.message ?: "falha no processamento")
        } finally {
            ctx.created.forEach { it.delete() }
        }
    }

    /** Parametros que viajam juntos em toda execucao: arquivos, arquivos de trabalho apagaveis, cancelamento e progresso. */
    private class Run(
        val original: File,
        val destination: File,
        val workDir: File,
        val created: MutableList<File>,
        val cancelled: () -> Boolean,
        val onProgress: (Float) -> Unit,
    )

    private fun execute(ctx: Run, project: EditProject, options: EnhanceOptions): EnhanceResult {
        val active = activeSteps(project)
        if (isIdentity(project, active)) return copyIdentity(ctx)
        val norm = active.firstOrNull { it.type == StepType.NORMALIZE } ?: return denoiseOnly(ctx, active)
        return when (val reduced = denoise(ctx, active.firstOrNull { it.type == StepType.NOISE_REDUCTION })) {
            is Reduced.Finished -> reduced.result
            is Reduced.Feed -> normalize(ctx, reduced.file, norm, options)
        }
    }

    private fun activeSteps(project: EditProject): List<EditStep> = project.steps.filter { it.enabled && !it.isNoOp }

    private fun isIdentity(project: EditProject, active: List<EditStep>): Boolean =
        project.bypass || project.intensity <= 0f || active.isEmpty()

    /** Copia bit a bit: nada a processar. */
    private fun copyIdentity(ctx: Run): EnhanceResult = finish(
        ProjectExporter { _, _ -> error("identidade nao processa") }.export(ctx.original, EditProject(), ctx.destination),
        BLANK_REPORT,
    )

    /** So o redutor, direto no destino (sem normalizacao depois). */
    private fun denoiseOnly(ctx: Run, active: List<EditStep>): EnhanceResult {
        val nr = active.first { it.type == StepType.NOISE_REDUCTION }
        val r = nrExporter().export(ctx.original, EditProject(listOf(nr)), ctx.destination, ctx.cancelled) { ctx.onProgress(it) }
        return finish(r, BLANK_REPORT)
    }

    /** Sai do redutor: o arquivo que entra na normalizacao, ou o resultado final quando o export nao terminou. */
    private sealed interface Reduced {
        data class Feed(val file: File) : Reduced
        data class Finished(val result: EnhanceResult) : Reduced
    }

    private fun denoise(ctx: Run, nr: EditStep?): Reduced {
        if (nr == null) return Reduced.Feed(ctx.original)
        val target = File(ctx.workDir, "nr.wav").also { ctx.created += it }
        val r = nrExporter().export(ctx.original, EditProject(listOf(nr)), target, ctx.cancelled) { ctx.onProgress(it * HALF) }
        if (r !is ExportResult.Done) return Reduced.Finished(finish(r, BLANK_REPORT))
        return Reduced.Feed(target)
    }

    private fun nrExporter() = ProjectExporter { p, rate ->
        val step = p.steps.single { it.type == StepType.NOISE_REDUCTION }
        noiseReducer(step.params.first().coerceIn(0f, 1f), rate)
    }

    private fun normalize(ctx: Run, source: File, step: EditStep, options: EnhanceOptions): EnhanceResult {
        val params = NormParams(step, options)
        val input = LoudnessAnalyzer.measure(source)
        val lufsBefore = (if (source != ctx.original) LoudnessAnalyzer.measure(ctx.original).lufs else null) ?: input.lufs
        if (input.lufs == null) { // silencio: nada a normalizar
            replace(source, ctx.destination)
            return EnhanceResult.Done(ctx.destination, EnhanceReport(lufsBefore, null, input.truePeakDbtp, 0.0, 0))
        }
        val search = PassSearch(ctx, source, step, params, input.lufs, lufsBefore)
        return when (val outcome = search.run()) {
            is SearchOutcome.Found -> done(ctx, outcome.chosen, outcome.passes, lufsBefore)
            is SearchOutcome.Stopped -> outcome.result
        }
    }

    private fun done(ctx: Run, chosen: Attempt, passes: Int, lufsBefore: Double?): EnhanceResult {
        replace(chosen.file, ctx.destination)
        ctx.onProgress(1f)
        return EnhanceResult.Done(
            ctx.destination,
            EnhanceReport(lufsBefore, chosen.report.lufs, chosen.report.truePeakDbtp, chosen.gainDb, passes),
        )
    }

    /** Uma passagem de normalizacao: arquivo, medicao e o ganho com que foi feita. */
    private class Attempt(val file: File, val report: LoudnessReport, val gainDb: Double)

    private sealed interface SearchOutcome {
        data class Found(val chosen: Attempt, val passes: Int) : SearchOutcome
        data class Stopped(val result: EnhanceResult) : SearchOutcome
    }

    /** Alvo de loudness e teto de pico do passo (com a folga do medidor independente) e o ganho fixo da previa, se houver. */
    private class NormParams(step: EditStep, options: EnhanceOptions) {
        val target = (step.params.getOrNull(0) ?: DEFAULT_TARGET).toDouble()
        val ceiling = (step.params.getOrNull(1) ?: DEFAULT_CEILING).toDouble() - PEAK_GUARD_DB
        val fixedGain = options.fixedNormalizeGainDb
    }

    /**
     * Busca iterativa da normalizacao: cada passagem reescreve o arquivo e mede; para quando respeita o teto de pico verdadeiro e
     * chega ao alvo de loudness (ou ao fim de [MAX_PASSES] passagens, ficando com a melhor tentativa).
     */
    private inner class PassSearch(
        private val ctx: Run,
        private val source: File,
        private val step: EditStep,
        private val params: NormParams,
        inputLufs: Double,
        private val lufsBefore: Double?,
    ) {
        private var gain = params.fixedGain ?: NormalizePlan.gainDb(inputLufs, params.target)
        private var ceiling = params.ceiling
        private var best: Attempt? = null
        private var passes = 0

        fun run(): SearchOutcome {
            while (passes < MAX_PASSES) {
                passes++
                val out = File(ctx.workDir, "n$passes.wav").also { ctx.created += it }
                val r = exportPass(out)
                if (r !is ExportResult.Done) {
                    return SearchOutcome.Stopped(finish(r, EnhanceReport(lufsBefore, null, null, 0.0, passes)))
                }
                val attempt = Attempt(out, LoudnessAnalyzer.measure(out), gain)
                if (isBetter(attempt, best, ceiling, params.target)) best = attempt
                if (converged(attempt)) break
                refine(attempt)
            }
            return SearchOutcome.Found(checkNotNull(best), passes)
        }

        private fun exportPass(out: File): ExportResult {
            val g = gain
            val c = ceiling
            val exporter = ProjectExporter { _, rate -> NormalizeProcessor(rate, g, c) }
            return exporter.export(source, EditProject(listOf(step)), out, ctx.cancelled) {
                ctx.onProgress(HALF + it * HALF / MAX_PASSES)
            }
        }

        /** true quando a passagem respeita o teto e chegou ao alvo (ou nao ha loudness a refinar: silencio). */
        private fun converged(attempt: Attempt): Boolean {
            val lufs = attempt.report.lufs ?: return true
            val peakExcess = attempt.report.truePeakDbtp - ceiling
            val loudnessError = params.target - lufs
            val loudOk = params.fixedGain != null || abs(loudnessError) <= LUFS_TOLERANCE
            return peakExcess <= TP_TOLERANCE && loudOk
        }

        /** Prepara a proxima passagem: abaixa o teto pelo excesso de pico e aproxima o ganho do alvo. */
        private fun refine(attempt: Attempt) {
            val peakExcess = attempt.report.truePeakDbtp - ceiling
            if (peakExcess > TP_TOLERANCE) ceiling -= peakExcess + PEAK_STEP
            if (params.fixedGain == null) {
                val lufs = attempt.report.lufs ?: return
                val loudnessError = params.target - lufs
                if (abs(loudnessError) > LUFS_TOLERANCE) {
                    gain = (gain + loudnessError).coerceAtMost(NormalizePlan.MAX_GAIN_DB)
                }
            }
        }
    }

    /** Prefere quem respeita o teto de pico; entre iguais, quem erra menos o alvo (ou, sem alvo, o de menor pico). */
    private fun isBetter(a: Attempt, b: Attempt?, ceiling: Double, target: Double): Boolean {
        if (b == null) return true
        val aOk = a.report.truePeakDbtp <= ceiling + TP_TOLERANCE
        val bOk = b.report.truePeakDbtp <= ceiling + TP_TOLERANCE
        return when {
            aOk != bOk -> aOk
            !aOk -> a.report.truePeakDbtp < b.report.truePeakDbtp
            else -> loudnessError(a, target) < loudnessError(b, target)
        }
    }

    /** Distancia ate o alvo em LU; sem loudness medido (silencio) fica por ultimo. Antes comparava |LUFS| absoluto, ou seja, a distancia a 0 LUFS. */
    private fun loudnessError(a: Attempt, target: Double): Double = a.report.lufs?.let { abs(target - it) } ?: Double.MAX_VALUE

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
        private val BLANK_REPORT = EnhanceReport(null, null, null, 0.0, 0)
    }
}
