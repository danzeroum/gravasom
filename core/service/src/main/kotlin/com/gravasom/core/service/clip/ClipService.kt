package com.gravasom.core.service.clip

import android.content.Context
import com.gravasom.core.audio.engine.edit.ClipResult
import com.gravasom.core.domain.clip.ClipTitle
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.service.RecordingStorage
import com.gravasom.core.service.library.RecordingFiles
import com.gravasom.core.service.library.RecordingIngestor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

sealed interface ClipOutcome {
    data class Saved(val recordingId: Long, val title: String) : ClipOutcome
    data class Failed(val reason: String) : ClipOutcome
}

/** "Exportar trecho": corta [startMillis, endMillis) e acrescenta o resultado a biblioteca como uma gravacao nova. */
class ClipService(
    private val context: Context,
    private val repo: RecordingRepository,
    private val files: RecordingFiles,
    private val ingestor: RecordingIngestor,
    private val exporter: ClipExporter = ClipExporter(),
) {

    suspend fun export(recording: Recording, startMillis: Long, endMillis: Long): ClipOutcome = withContext(Dispatchers.IO) {
        // Copia em claro de uso unico: apagada logo apos o corte (antes ficava em cache/vault-play ate a proxima abertura do app).
        val plain = runCatching { files.openPlain(recording) }.getOrNull()
            ?: return@withContext ClipOutcome.Failed("origem ilegivel")
        val target = File(RecordingStorage.dir(context), "trecho_${System.currentTimeMillis()}.${recording.format.extension}")
        val exported = plain.use { exporter.export(it.file, recording.format, startMillis, endMillis, target) }
        when (val r = exported) {
            is ClipResult.Failed -> ClipOutcome.Failed(r.reason)
            is ClipResult.Done -> {
                val title = ClipTitle.build(recording.title, startMillis, endMillis, pt = Locale.getDefault().language == "pt")
                // Um trecho de gravacao do cofre continua no cofre, mesmo que o cofre tenha sido desligado depois.
                val id = ingestor.ingest(r.file, title = title, forcePrivate = recording.isPrivate, preset = recording.preset)
                if (id == null) {
                    target.delete()
                    ClipOutcome.Failed("nao foi possivel salvar o trecho")
                } else {
                    if (recording.tags.isNotEmpty()) repo.setTags(id, recording.tags)
                    ClipOutcome.Saved(id, title)
                }
            }
        }
    }
}
