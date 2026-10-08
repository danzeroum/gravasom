package com.gravasom.core.service.library

import com.gravasom.core.data.metrics.UsageMetrics
import com.gravasom.core.data.preferences.PreferencesRepository
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.metrics.UsageEventType
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.service.MediaProbe
import com.gravasom.core.service.RecordingServiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Leva cada gravacao finalizada para a biblioteca: Room + copia publica no MediaStore (cofre desligado)
 * ou arquivo cifrado no cofre (cofre ligado).
 * Escuta [RecordingServiceState.saved], entao funciona tambem para gravacoes iniciadas pelo Tile, widget ou atalho.
 */
class RecordingIngestor(
    private val repo: RecordingRepository,
    private val prefs: PreferencesRepository,
    private val metrics: UsageMetrics,
    private val publisher: MediaStorePublisher,
    private val vaultStore: VaultStore,
) {

    fun start(scope: CoroutineScope) {
        scope.launch { RecordingServiceState.saved.collect { path -> runCatching { ingest(File(path)) } } }
    }

    suspend fun ingest(file: File): Long? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext null
        val format = formatOf(file) ?: return@withContext null
        val duration = MediaProbe.durationMillis(file) ?: 0L
        val now = System.currentTimeMillis()
        val title = defaultTitle(now)
        val size = file.length()
        val vaultOn = prefs.privateVault.first()
        // Cofre ligado: cifra e apaga o claro. Se a cifragem falhar, o claro fica no app, sem copia publica (nunca vaza).
        val stored = if (vaultOn) vaultStore.protect(file) ?: file else file
        val id = repo.insert(
            Recording(
                title = title, path = stored.path, format = format, durationMillis = duration,
                sizeBytes = size, createdAtMillis = now,
            ),
        )
        if (!vaultOn) {
            publisher.publish(file, "$title.${format.extension}", format.mimeType)?.let { repo.setPublishedUri(id, it) }
        }
        metrics.record(UsageEventType.GRAVACAO_CONCLUIDA, duration, format)
        id
    }

    companion object {
        fun formatOf(file: File): RecordingFormat? = RecordingFormat.entries.firstOrNull { it.extension == file.extension.lowercase() }

        /** "Gravacao 2026-10-07 14h32" (docs/ux/04-navegacao.md). */
        fun defaultTitle(nowMillis: Long): String =
            "Gravação " + SimpleDateFormat("yyyy-MM-dd HH'h'mm", Locale.getDefault()).format(Date(nowMillis))
    }
}
