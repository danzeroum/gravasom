package com.gravasom.core.service.enhance

import com.gravasom.core.audio.engine.edit.AudioBlockProcessor
import com.gravasom.core.audio.engine.edit.ClipResult
import com.gravasom.core.audio.engine.edit.EditProject
import com.gravasom.core.audio.engine.edit.WavClipper
import com.gravasom.core.audio.engine.enhance.EnhanceOptions
import com.gravasom.core.audio.engine.enhance.EnhancePipeline
import com.gravasom.core.audio.engine.enhance.EnhanceReport
import com.gravasom.core.audio.engine.enhance.EnhanceResult
import com.gravasom.core.audio.engine.loudness.LoudnessAnalyzer
import com.gravasom.core.audio.engine.loudness.NormalizePlan
import com.gravasom.core.domain.enhance.EnhancedTitle
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.preset.FormatPresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** O que o [EnhanceService] precisa do resto do app (arquivos, biblioteca, projeto salvo): costura fina, trocavel nos testes. */
interface EnhanceHost {
    /** Arquivo legivel da gravacao (no cofre: copia em claro no cache privado). */
    suspend fun playableFile(recording: Recording): File?

    /** Acrescenta [file] a biblioteca como gravacao nova derivada de [source]; devolve o id, ou null se falhou. */
    suspend fun saveDerived(file: File, title: String, source: Recording): Long?

    suspend fun loadProject(recordingId: Long): EditProject?
    suspend fun saveProject(recordingId: Long, project: EditProject)

    /** Pasta de trabalho (cache privado) e onde a copia nasce antes de entrar na biblioteca. */
    val workDir: File
    val outputDir: File
}

/** Gravacao pronta para melhorar: o WAV de trabalho e a loudness da gravacao INTEIRA (base do ganho da previa). */
class PreparedRecording(val recording: Recording, val wav: DecodedWav, val referenceLufs: Double?)

/** Trechos de previa: o MESMO trecho do original (A) e melhorado (B), com a mesma duracao, para alternar sem corte. */
class EnhancePreview(val original: File, val enhanced: File, val startMillis: Long, val durationMillis: Long, val gainDb: Double)

sealed interface EnhanceOutcome {
    data class Saved(val recordingId: Long, val title: String, val report: EnhanceReport) : EnhanceOutcome
    data object Cancelled : EnhanceOutcome
    data class Failed(val reason: String) : EnhanceOutcome
}

/**
 * "Melhorar som": prepara (decodifica para WAV e mede a loudness da gravacao inteira), gera a previa A/B de um trecho e exporta a
 * COPIA melhorada como gravacao nova. Efeitos nao destrutivos (ADR-008): o original nunca muda e o projeto fica salvo.
 */
class EnhanceService(
    private val host: EnhanceHost,
    noiseReducer: (intensity: Float, sampleRate: Int) -> AudioBlockProcessor,
    private val portuguese: () -> Boolean = { java.util.Locale.getDefault().language == "pt" },
) {

    private val pipeline = EnhancePipeline(noiseReducer)

    /** Configuracao salva da gravacao, ou os padroes do preset com que ela foi gravada (sem preset: nada ligado). */
    suspend fun settingsFor(recording: Recording): EnhanceSettings {
        val defaults = FormatPresets.effects(recording.preset)
        return host.loadProject(recording.id)?.let { EnhanceSettings.fromProject(it, defaults) } ?: EnhanceSettings.fromDefaults(defaults)
    }

    suspend fun saveSettings(recording: Recording, settings: EnhanceSettings) = host.saveProject(recording.id, settings.toProject())

    /** null se a gravacao nao abre ou nao decodifica (mais de 2 canais, por exemplo). */
    suspend fun prepare(recording: Recording, cancelled: () -> Boolean = { false }): PreparedRecording? = withContext(Dispatchers.IO) {
        val source = host.playableFile(recording) ?: return@withContext null
        val decoded = AudioToWav.convert(source, File(host.workDir, "dec_${recording.id}_${recording.sizeBytes}.wav"), cancelled)
            ?: return@withContext null
        val lufs = runCatching { LoudnessAnalyzer.measure(decoded.file).lufs }.getOrNull()
        PreparedRecording(recording, decoded, lufs)
    }

    /**
     * Previa de [windowMillis] a partir de [startMillis]. A normalizacao usa o ganho da gravacao INTEIRA (nao o do trecho), entao o
     * que se ouve em B e o que a copia final tera (a menos do efeito do redutor sobre a loudness: ate ~1 LU).
     */
    suspend fun preview(
        prepared: PreparedRecording,
        settings: EnhanceSettings,
        startMillis: Long,
        windowMillis: Long = DEFAULT_WINDOW_MILLIS,
    ): EnhancePreview? = withContext(Dispatchers.IO) {
        val total = prepared.wav.durationMillis
        val length = minOf(windowMillis, total)
        val start = startMillis.coerceIn(0L, (total - length).coerceAtLeast(0L))
        val dir = File(host.workDir, "previa").apply { deleteRecursively(); mkdirs() }
        val a = File(dir, "a.wav")
        val b = File(dir, "b.wav")
        val clip = WavClipper.clip(prepared.wav.file, start, start + length, a)
        if (clip !is ClipResult.Done) return@withContext null
        val fixedGain = if (settings.normalize) NormalizePlan.gainDb(prepared.referenceLufs, settings.targetLufs.toDouble()) else 0.0
        val result = pipeline.run(
            a, settings.toProject(), b, File(dir, "work"), options = EnhanceOptions(fixedNormalizeGainDb = fixedGain),
        )
        if (result !is EnhanceResult.Done) return@withContext null
        EnhancePreview(a, b, start, length, fixedGain)
    }

    /** Exporta a copia melhorada (WAV) para a biblioteca. Cancelavel; nunca deixa arquivo pela metade. */
    suspend fun export(
        prepared: PreparedRecording,
        settings: EnhanceSettings,
        cancelled: () -> Boolean = { false },
        onProgress: (Float) -> Unit = {},
    ): EnhanceOutcome = withContext(Dispatchers.IO) {
        val recording = prepared.recording
        if (settings.isIdentity) return@withContext EnhanceOutcome.Failed("nada para melhorar: ligue o redutor ou o volume")
        val out = File(host.outputDir, "melhorada_${System.currentTimeMillis()}.wav")
        val work = File(host.workDir, "export_${recording.id}")
        try {
            when (val r = pipeline.run(prepared.wav.file, settings.toProject(), out, work, cancelled, onProgress)) {
                EnhanceResult.Cancelled -> EnhanceOutcome.Cancelled
                is EnhanceResult.Failed -> EnhanceOutcome.Failed(r.reason)
                is EnhanceResult.Done -> {
                    val title = EnhancedTitle.build(recording.title, portuguese())
                    val id = host.saveDerived(r.file, title, recording)
                    if (id == null) {
                        out.delete()
                        EnhanceOutcome.Failed("nao foi possivel salvar a copia")
                    } else {
                        host.saveProject(recording.id, settings.toProject())
                        EnhanceOutcome.Saved(id, title, r.report)
                    }
                }
            }
        } finally {
            work.deleteRecursively()
        }
    }

    companion object {
        const val DEFAULT_WINDOW_MILLIS = 20_000L
    }
}
