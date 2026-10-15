package com.gravasom.core.service.clip

import android.content.Context
import com.gravasom.core.audio.engine.edit.ClipResult
import com.gravasom.core.domain.clip.SplitTitle
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.service.RecordingStorage
import com.gravasom.core.service.library.RecordingFiles
import com.gravasom.core.service.library.RecordingIngestor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

sealed interface SplitOutcome {
    data class Saved(val firstId: Long, val secondId: Long, val titles: Pair<String, String>) : SplitOutcome
    data class Failed(val reason: String) : SplitOutcome
}

/**
 * "Dividir": corta a gravacao em [atMillis] e acrescenta DUAS gravacoes novas a biblioteca (antes e depois do ponto), no
 * mesmo formato e sem reencodar ([ClipExporter]). O original nunca e alterado (ADR-008); as partes herdam etiquetas e cofre.
 * Os arquivos exportados viram o arquivo da gravacao nova (o ingestor os guarda como estao), entao so se apaga o que NAO
 * entrou na biblioteca.
 */
class SplitService(
    private val context: Context,
    private val repo: RecordingRepository,
    private val files: RecordingFiles,
    private val ingestor: RecordingIngestor,
    private val exporter: ClipExporter = ClipExporter(),
) {

    suspend fun split(recording: Recording, atMillis: Long): SplitOutcome = withContext(Dispatchers.IO) {
        val duration = recording.durationMillis
        if (atMillis <= MIN_PART_MILLIS || atMillis >= duration - MIN_PART_MILLIS) {
            return@withContext SplitOutcome.Failed("o ponto de divisao precisa ter audio dos dois lados")
        }
        // Uma unica copia em claro de uso unico para as duas partes, apagada assim que o ultimo corte termina.
        val plain = runCatching { files.openPlain(recording) }.getOrNull()
            ?: return@withContext SplitOutcome.Failed("origem ilegivel")
        val stamp = System.currentTimeMillis()
        val first = File(RecordingStorage.dir(context), "parte_${stamp}_1.${recording.format.extension}")
        val second = File(RecordingStorage.dir(context), "parte_${stamp}_2.${recording.format.extension}")
        val cut = plain.use { copy ->
            val r1 = exporter.export(copy.file, recording.format, 0L, atMillis, first)
            if (r1 !is ClipResult.Done) {
                SplitCut.FirstFailed
            } else if (exporter.export(copy.file, recording.format, atMillis, duration, second) !is ClipResult.Done) {
                SplitCut.SecondFailed
            } else {
                SplitCut.Done
            }
        }
        if (cut != SplitCut.Done) {
            first.delete()
            second.delete()
            return@withContext SplitOutcome.Failed(if (cut == SplitCut.FirstFailed) "primeira parte nao saiu" else "segunda parte nao saiu")
        }
        ingest(recording, first, second)
    }

    /** Guarda as duas partes; em falha total nada entra, em falha na parte 2 so ela nao entra. */
    private suspend fun ingest(recording: Recording, first: File, second: File): SplitOutcome {
        val pt = Locale.getDefault().language == "pt"
        val title1 = SplitTitle.build(recording.title, 1, pt)
        val title2 = SplitTitle.build(recording.title, 2, pt)
        // Uma parte de gravacao do cofre continua no cofre, mesmo que o cofre tenha sido desligado depois.
        val id1 = ingestor.ingest(first, title = title1, forcePrivate = recording.isPrivate, preset = recording.preset)
        if (id1 == null) {
            first.delete()
            second.delete()
            return SplitOutcome.Failed("nao foi possivel guardar a parte 1")
        }
        val id2 = ingestor.ingest(second, title = title2, forcePrivate = recording.isPrivate, preset = recording.preset)
        if (id2 == null) {
            second.delete()
            return SplitOutcome.Failed("nao foi possivel guardar a parte 2")
        }
        if (recording.tags.isNotEmpty()) {
            repo.setTags(id1, recording.tags)
            repo.setTags(id2, recording.tags)
        }
        return SplitOutcome.Saved(id1, id2, title1 to title2)
    }

    private enum class SplitCut { Done, FirstFailed, SecondFailed }

    private companion object {
        const val MIN_PART_MILLIS = 200L
    }
}
