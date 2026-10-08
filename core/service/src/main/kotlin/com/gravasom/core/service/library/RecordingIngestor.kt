package com.gravasom.core.service.library

import com.gravasom.core.audio.engine.marker.MarkerSidecar
import com.gravasom.core.data.metrics.UsageMetrics
import com.gravasom.core.data.preferences.PreferencesRepository
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.marker.MarkerRepository
import com.gravasom.core.domain.metrics.UsageEventType
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.domain.tags.AutoTitle
import com.gravasom.core.service.MediaProbe
import com.gravasom.core.service.RecordingServiceState
import com.gravasom.core.service.analysis.RecordingAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
    private val markers: MarkerRepository,
    private val analyzer: RecordingAnalyzer,
) {

    private var scope: CoroutineScope? = null

    fun start(scope: CoroutineScope) {
        this.scope = scope
        scope.launch { RecordingServiceState.saved.collect { path -> runCatching { ingest(File(path)) } } }
    }

    /**
     * [title] substitui o nome automatico (ex.: trecho exportado). [forcePrivate] guarda no cofre mesmo com o cofre desligado
     * (um trecho de gravacao privada continua privado).
     */
    suspend fun ingest(file: File, title: String? = null, forcePrivate: Boolean = false): Long? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext null
        val format = formatOf(file) ?: return@withContext null
        val duration = MediaProbe.durationMillis(file) ?: 0L
        val now = System.currentTimeMillis()
        val size = file.length()
        val name = title ?: defaultTitle(now, MediaProbe.config(file, format, duration), Locale.getDefault())
        val vaultOn = forcePrivate || prefs.privateVault.first()
        // Cofre ligado: cifra e apaga o claro. Se a cifragem falhar, o claro fica no app, sem copia publica (nunca vaza).
        val stored = if (vaultOn) vaultStore.protect(file) ?: file else file
        val id = repo.insert(
            Recording(
                title = name, path = stored.path, format = format, durationMillis = duration,
                sizeBytes = size, createdAtMillis = now,
            ),
        )
        attachMarkers(id, file, duration)
        if (!vaultOn) {
            publisher.publish(file, "$name.${format.extension}", format.mimeType)?.let { repo.setPublishedUri(id, it) }
            // Onda e fala calculadas ao salvar (nao ao abrir): em segundo plano, o arquivo claro ainda existe aqui.
            scope?.launch(Dispatchers.Default) { runCatching { analyzer.ensure(id, file, persist = true) } }
        }
        metrics.record(UsageEventType.GRAVACAO_CONCLUIDA, duration, format)
        id
    }

    /** Marcadores feitos durante a gravacao (tela, notificacao, widget) ficam num arquivo lateral; passam para o banco. */
    private suspend fun attachMarkers(recordingId: Long, audio: File, durationMillis: Long) {
        val sidecar = MarkerSidecar.forAudio(audio)
        val limit = if (durationMillis > 0) durationMillis else Long.MAX_VALUE
        sidecar.read().forEach { m ->
            markers.insert(Marker(recordingId = recordingId, positionMillis = m.positionMillis.coerceIn(0L, limit), label = m.label, note = ""))
        }
        sidecar.delete()
    }

    companion object {
        fun formatOf(file: File): RecordingFormat? = RecordingFormat.entries.firstOrNull { it.extension == file.extension.lowercase() }

        /** Nome de audio importado sem nome de arquivo aproveitavel: "Gravação 2026-10-07 14h32". */
        fun defaultTitle(nowMillis: Long): String =
            "Gravação " + java.text.SimpleDateFormat("yyyy-MM-dd HH'h'mm", Locale.getDefault()).format(java.util.Date(nowMillis))

        /** "08/10/2026 14:32 · M4A 128 kbps mono" (data · preset; docs/ux/04-navegacao.md). */
        fun defaultTitle(nowMillis: Long, config: com.gravasom.core.domain.audio.RecordingConfig, locale: Locale): String =
            AutoTitle.build(nowMillis, config, locale)
    }
}
