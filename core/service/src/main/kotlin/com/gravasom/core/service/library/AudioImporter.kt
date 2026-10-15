package com.gravasom.core.service.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.gravasom.core.data.metrics.UsageMetrics
import com.gravasom.core.data.preferences.PreferencesRepository
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.metrics.UsageEventType
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.service.MediaProbe
import com.gravasom.core.service.RecordingStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
    private val prefs: PreferencesRepository,
    private val vaultStore: VaultStore,
) {

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        // file:// de outro app faria o Gravasom ler arquivos dele com as permissoes do proprio app (confused deputy).
        if (uri.scheme != "content") return@withContext ImportResult.Unsupported
        val name = displayName(uri)
        val format = formatFor(context.contentResolver.getType(uri), name) ?: return@withContext ImportResult.Unsupported
        val target = File(RecordingStorage.dir(context), "importada_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(8)}.${format.extension}")
        val duration = if (copyInto(uri, target)) MediaProbe.durationMillis(target) else null
        if (duration == null || duration <= 0L) {
            target.delete()
            return@withContext ImportResult.Invalid
        }
        val now = System.currentTimeMillis()
        val title = name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: RecordingIngestor.defaultTitle(now)
        val size = target.length()
        val vaultOn = prefs.privateVault.first()
        val protegido = if (vaultOn) vaultStore.protect(target) else null
        if (vaultOn && protegido == null) LibraryNotices.vaultProtectFailed() // ficou em claro: avisar o usuario
        val stored = protegido ?: target
        val id = repo.insert(
            Recording(
                title = title, path = stored.path, format = format, durationMillis = duration,
                sizeBytes = size, createdAtMillis = now,
            ),
        )
        metrics.record(UsageEventType.IMPORTADO, duration, format)
        ImportResult.Imported(id)
    }

    private fun copyInto(uri: Uri, target: File): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } != null
    }.getOrDefault(false)

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
