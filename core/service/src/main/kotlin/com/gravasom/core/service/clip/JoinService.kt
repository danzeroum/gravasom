package com.gravasom.core.service.clip

import android.content.Context
import com.gravasom.core.audio.engine.edit.ClipResult
import com.gravasom.core.domain.clip.JoinTitle
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.service.RecordingStorage
import com.gravasom.core.service.library.RecordingFiles
import com.gravasom.core.service.library.VaultStore
import com.gravasom.core.service.library.RecordingIngestor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

sealed interface JoinOutcome {
    data class Saved(val recordingId: Long, val title: String) : JoinOutcome
    data class Failed(val reason: String) : JoinOutcome
}

/**
 * "Juntar": concatena gravacoes do mesmo formato, NA ORDEM dada, numa gravacao nova ([JoinExporter], sem reencodar). Os
 * originais nunca sao alterados (ADR-008); a juncao herda a uniao das etiquetas e vai para o cofre se qualquer parte vier
 * de la.
 */
class JoinService(
    private val context: Context,
    private val repo: RecordingRepository,
    private val files: RecordingFiles,
    private val ingestor: RecordingIngestor,
    private val exporter: JoinExporter = JoinExporter(),
) {

    suspend fun join(recordings: List<Recording>): JoinOutcome = withContext(Dispatchers.IO) {
        if (recordings.size < 2) return@withContext JoinOutcome.Failed("precisa de ao menos duas gravacoes")
        val distinctFormats = recordings.map { it.format }.distinct()
        if (distinctFormats.size > 1) return@withContext JoinOutcome.Failed("so da para juntar gravacoes do mesmo formato")
        val format = distinctFormats.single()
        val target = File(RecordingStorage.dir(context), "juncao_${System.currentTimeMillis()}.${format.extension}")
        // Copias em claro de uso unico: todas apagadas ao fim da juncao, mesmo se uma das origens falhar ao abrir.
        val copies = mutableListOf<VaultStore.PlainCopy>()
        val joined = try {
            for (r in recordings) {
                copies += runCatching { files.openPlain(r) }.getOrNull() ?: return@withContext JoinOutcome.Failed("origem ilegivel")
            }
            exporter.join(copies.map { it.file }, format, target)
        } finally {
            copies.forEach { it.close() }
        }
        val outcome = when (val r = joined) {
            is ClipResult.Failed -> JoinOutcome.Failed(r.reason)
            is ClipResult.Done -> save(recordings, r.file)
        }
        if (outcome !is JoinOutcome.Saved) target.delete()
        outcome
    }

    private suspend fun save(recordings: List<Recording>, file: File): JoinOutcome {
        val title = JoinTitle.build(recordings.map { it.title }, Locale.getDefault().language == "pt")
        val anyPrivate = recordings.any { it.isPrivate }
        val id = ingestor.ingest(file, title = title, forcePrivate = anyPrivate, preset = null)
            ?: return JoinOutcome.Failed("nao foi possivel guardar a juncao")
        val tags = recordings.flatMap { it.tags }.distinct()
        if (tags.isNotEmpty()) repo.setTags(id, tags)
        return JoinOutcome.Saved(id, title)
    }
}
