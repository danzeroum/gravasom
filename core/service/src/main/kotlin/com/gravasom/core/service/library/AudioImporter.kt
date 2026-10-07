package com.gravasom.core.service.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.gravasom.core.data.metrics.UsageMetrics
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.metrics.UsageEventType
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.service.MediaProbe
import com.gravasom.core.service.RecordingStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ImportResult {
    data class Imported(val id: Long) : ImportResult
    data object Unsupported : ImportResult
    data object Invalid : ImportResult
}

/** Recebe audio de outros apps (ACTION_SEND com tipo de audio): copia para a area privada e entra na lista. */
class AudioImporter(
    private val context: Context,
    private val repo: RecordingRepository,
    private val metrics: UsageMetrics,
) {

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val name = displayName(uri)
        val format = formatFor(resolver.getType(uri), name) ?: return@withContext ImportResult.Unsupported
        val target = File(RecordingStorage.dir(context), "importada_${System.currentTimeMillis()}.${format.extension}")
        val copiou = runCatching {
            resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } != null
        }.getOrDefault(false)
        val duration = if (copiou) MediaProbe.durationMillis(target) else null
        if (duration == null || duration <= 0L) {
            target.delete()
            return@withContext ImportResult.Invalid
        }
        val now = System.currentTimeMillis()
        val title = name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: RecordingIngestor.defaultTitle(now)
        val id = repo.insert(
            Recording(
                title = title, path = target.path, format = format, durationMillis = duration,
                sizeBytes = target.length(), createdAtMillis = now,
            ),
        )
        metrics.record(UsageEventType.IMPORTADO, duration, format)
        ImportResult.Imported(id)
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment

    companion object {
        fun formatFor(mime: String?, name: String?): RecordingFormat? = when {
            mime in setOf("audio/mp4", "audio/x-m4a", "audio/m4a") -> RecordingFormat.M4A
            mime in setOf("audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave") -> RecordingFormat.WAV
            mime in setOf("audio/mpeg", "audio/mp3") -> RecordingFormat.MP3
            else -> RecordingFormat.entries.firstOrNull { name?.lowercase()?.endsWith("." + it.extension) == true }
        }
    }
}
