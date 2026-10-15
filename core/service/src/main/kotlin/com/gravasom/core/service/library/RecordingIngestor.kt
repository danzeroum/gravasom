package com.gravasom.core.service.library

import com.gravasom.core.audio.engine.marker.MarkerSidecar
import com.gravasom.core.data.metrics.UsageMetrics
import com.gravasom.core.data.preferences.PreferencesRepository
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.marker.MarkerRepository
import com.gravasom.core.domain.metrics.UsageEventType
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.preset.FormatPreset
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.domain.tags.AutoTitle
import com.gravasom.core.service.MediaProbe
import com.gravasom.core.service.RecordingServiceState
import com.gravasom.core.service.analysis.RecordingAnalyzer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import timber.log.Timber

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
        scope.launch {
            RecordingServiceState.saved.collect { path ->
                try {
                    ingest(File(path), preset = prefs.preset.first())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // O audio ja esta em disco: registra a falha (antes sumia sem rastro) e segue ouvindo as proximas gravacoes.
                    Timber.e(e, "falha ao colocar a gravacao na biblioteca: %s", path)
                }
            }
        }
    }

    /**
     * [preset] e o preset de formato com que foi gravado (define os efeitos ligados por padrao); null em importacoes e derivados.
     * [title] substitui o nome automatico (ex.: trecho exportado). [forcePrivate] guarda no cofre mesmo com o cofre desligado
     * (um trecho de gravacao privada continua privado).
     */
    suspend fun ingest(
        file: File,
        title: String? = null,
        forcePrivate: Boolean = false,
        preset: FormatPreset? = null,
    ): Long? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext null
        val format = formatOf(file) ?: return@withContext null
        val duration = MediaProbe.durationMillis(file) ?: 0L
        val now = System.currentTimeMillis()
        val size = file.length()
        val name = title ?: defaultTitle(now, MediaProbe.config(file, format, duration), Locale.getDefault())
        val vaultOn = forcePrivate || prefs.privateVault.first()
        val base = Recording(
            title = name, path = file.path, format = format, durationMillis = duration, sizeBytes = size, createdAtMillis = now,
            preset = preset,
        )
        val id = store(file, base, vaultOn)
        attachMarkers(id, file, duration)
        if (!vaultOn) publishAndAnalyze(id, file, name, format)
        metrics.record(UsageEventType.GRAVACAO_CONCLUIDA, duration, format)
        id
    }

    /** Insere a gravacao. Cofre ligado: cifra e apaga o claro; se a cifragem falhar, o claro fica no app, sem copia publica. */
    private suspend fun store(file: File, base: Recording, vaultOn: Boolean): Long {
        val protegido = if (vaultOn) vaultStore.protect(file) else null
        if (vaultOn && protegido == null) {
            // O claro fica no app (sem copia publica), mas o usuario ligou o cofre: precisa saber que esta gravacao NAO esta cifrada.
            Timber.w("cofre ligado, mas a cifragem falhou: %s ficou em claro", file.name)
            LibraryNotices.vaultProtectFailed()
        }
        val stored = protegido ?: file
        return repo.insert(base.copy(path = stored.path))
    }

    /** Copia publica no MediaStore e, em segundo plano, onda e fala (calculadas ao salvar, nao ao abrir). */
    private suspend fun publishAndAnalyze(id: Long, file: File, name: String, format: RecordingFormat) {
        publisher.publish(file, "$name.${format.extension}", format.mimeType)?.let { repo.setPublishedUri(id, it) }
        scope?.launch(Dispatchers.Default) { runCatching { analyzer.ensure(id, file, persist = true) } }
    }

    /** Marcadores feitos durante a gravacao (tela, notificacao, widget) ficam num arquivo lateral; passam para o banco. */
    private suspend fun attachMarkers(recordingId: Long, audio: File, durationMillis: Long) {
        val sidecar = MarkerSidecar.forAudio(audio)
        val limit = if (durationMillis > 0) durationMillis else Long.MAX_VALUE
        sidecar.read().forEach { m ->
            val position = m.positionMillis.coerceIn(0L, limit)
            markers.insert(Marker(recordingId = recordingId, positionMillis = position, label = m.label, note = ""))
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
