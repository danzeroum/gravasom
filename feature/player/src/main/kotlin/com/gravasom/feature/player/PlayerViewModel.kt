package com.gravasom.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gravasom.core.audio.engine.analysis.SilenceSensitivity
import com.gravasom.core.audio.engine.analysis.SkipPlan
import com.gravasom.core.audio.engine.analysis.Waveform
import com.gravasom.core.data.preferences.PreferencesRepository
import com.gravasom.core.domain.marker.AddMarker
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.marker.MarkerRejection
import com.gravasom.core.domain.marker.MarkerRepository
import com.gravasom.core.domain.marker.MarkerResult
import com.gravasom.core.domain.marker.UpdateMarker
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.domain.tags.Tags
import com.gravasom.core.service.analysis.RecordingAnalyzer
import com.gravasom.core.service.clip.ClipOutcome
import com.gravasom.core.service.clip.ClipService
import com.gravasom.core.service.clip.SplitOutcome
import com.gravasom.core.service.clip.SplitService
import com.gravasom.core.service.library.RecordingFiles
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Trecho escolhido na onda, em ms do arquivo. */
data class TimeSelection(val startMillis: Long, val endMillis: Long) {
    val lengthMillis: Long get() = endMillis - startMillis
}

data class PlayerUiState(
    val recording: Recording? = null,
    val waveform: Waveform? = null,
    val analyzing: Boolean = false,
    val markers: List<Marker> = emptyList(),
    val skipEnabled: Boolean = false,
    val sensitivity: SilenceSensitivity = SilenceSensitivity.DEFAULT,
    val plan: SkipPlan? = null,
    val selecting: Boolean = false,
    val selection: TimeSelection? = null,
    val exporting: Boolean = false,
    val splitting: Boolean = false,
)

/** Avisos pontuais para a tela (snackbar). */
sealed interface PlayerEvent {
    data class MarkerRejected(val reason: MarkerRejection) : PlayerEvent
    data object MarkerDeleted : PlayerEvent
    data class ClipSaved(val title: String) : PlayerEvent
    data object ClipFailed : PlayerEvent
    data object AnalysisFailed : PlayerEvent
    data class SplitSaved(val firstTitle: String, val secondTitle: String) : PlayerEvent
    data object SplitFailed : PlayerEvent
}

/**
 * Estado do player da gravacao aberta: onda (lida do banco, calculada ao salvar), marcadores, plano de pulo de silencio,
 * trecho selecionado e exportacao. O ExoPlayer em si fica no [PlayerController].
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val markerRepo: MarkerRepository,
    private val analyzer: RecordingAnalyzer,
    private val files: RecordingFiles,
    private val repo: RecordingRepository,
    private val prefs: PreferencesRepository,
    private val clips: ClipService,
    private val splits: SplitService,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()

    private val addMarker = AddMarker(markerRepo, durationOf = { repo.getById(it)?.durationMillis })
    private val updateMarker = UpdateMarker(markerRepo)

    /** Dialogo de marcador (rascunho ate Salvar) e desfazer da exclusao. */
    val markerEditor = MarkerEditor(markerRepo, addMarker, updateMarker, viewModelScope)
    private var loading: Job? = null
    private var markersJob: Job? = null
    private var analysisSpeech: Map<SilenceSensitivity, BooleanArray> = emptyMap()

    init {
        viewModelScope.launch { prefs.skipSilence.collect { on -> _state.update { it.copy(skipEnabled = on) } } }
        viewModelScope.launch {
            prefs.skipSensitivity.collect { mode ->
                _state.update { it.copy(sensitivity = SilenceSensitivity.fromVadMode(mode)) }
                rebuildPlan()
            }
        }
    }

    /** Abre uma gravacao: carrega marcadores e a analise (ou a calcula, se faltar). */
    fun open(recording: Recording) {
        if (_state.value.recording?.id == recording.id) {
            _state.update { it.copy(recording = recording) } // tags/titulo podem ter mudado
            return
        }
        loading?.cancel()
        markersJob?.cancel()
        analysisSpeech = emptyMap()
        _state.update {
            it.copy(
                recording = recording, waveform = null, analyzing = true, markers = emptyList(), plan = null,
                selecting = false, selection = null, exporting = false,
            )
        }
        markersJob = viewModelScope.launch { markerRepo.observe(recording.id).collect { m -> _state.update { it.copy(markers = m) } } }
        loading = viewModelScope.launch {
            val analysis = runCatching {
                val audio = java.io.File(files.playablePath(recording))
                analyzer.ensure(recording.id, audio, persist = !recording.isPrivate)
            }.getOrNull()
            if (analysis == null) {
                _state.update { it.copy(analyzing = false) }
                _events.tryEmit(PlayerEvent.AnalysisFailed)
            } else {
                analysisSpeech = SilenceSensitivity.entries.mapIndexed { i, s -> s to analysis.speechFrames(i) }.toMap()
                _state.update { it.copy(waveform = analysis.waveform, analyzing = false) }
                rebuildPlan()
            }
        }
    }

    fun close() {
        loading?.cancel()
        markersJob?.cancel()
        analysisSpeech = emptyMap()
        markerEditor.dismiss()
        _state.value = _state.value.copy(
            recording = null, waveform = null, analyzing = false, markers = emptyList(), plan = null,
            selecting = false, selection = null, exporting = false,
        )
    }

    fun setSkipEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setSkipSilence(enabled) }
    }

    fun setSensitivity(sensitivity: SilenceSensitivity) {
        viewModelScope.launch { prefs.setSkipSensitivity(sensitivity.vadMode) }
    }

    private fun rebuildPlan() {
        val s = _state.value
        val frames = analysisSpeech[s.sensitivity]
        val duration = s.recording?.durationMillis ?: 0L
        _state.update { it.copy(plan = if (frames != null && duration > 0) SkipPlan.build(frames, duration) else null) }
    }

    // --- marcadores ---

    /** "Marcar aqui": abre o dialogo com um RASCUNHO na posicao dada; so Salvar grava (cancelar nao deixa marcador vazio). */
    fun beginMarker(positionMillis: Long) {
        val recording = _state.value.recording ?: return
        markerEditor.beginAdd(recording.id, positionMillis.coerceIn(0L, recording.durationMillis))
    }

    fun editMarker(marker: Marker) = markerEditor.beginEdit(marker)

    fun dismissMarkerDialog() = markerEditor.dismiss()

    /** Devolve o resultado para o dialogo decidir se fecha (salvo) ou mostra o erro (rejeitado). */
    suspend fun saveMarker(target: MarkerDialogTarget, note: String, label: String): MarkerResult {
        val result = markerEditor.save(target, note, label)
        if (result is MarkerResult.Rejected) _events.tryEmit(PlayerEvent.MarkerRejected(result.reason))
        return result
    }

    /** Exclui com "Desfazer" por [MarkerEditor.UNDO_WINDOW_MILLIS]. */
    fun deleteMarker(marker: Marker) {
        markerEditor.delete(marker)
        _events.tryEmit(PlayerEvent.MarkerDeleted)
    }

    fun undoMarkerDelete() {
        viewModelScope.launch { markerEditor.undoDelete() }
    }

    // --- tags ---

    fun setTags(text: String) {
        val recording = _state.value.recording ?: return
        viewModelScope.launch { repo.setTags(recording.id, Tags.parse(text)) }
    }

    // --- trecho ---

    fun setSelecting(on: Boolean, positionMillis: Long = 0L) {
        _state.update {
            if (!on) {
                it.copy(selecting = false, selection = null)
            } else {
                val duration = it.recording?.durationMillis ?: 0L
                val start = positionMillis.coerceIn(0L, duration)
                it.copy(selecting = true, selection = TimeSelection(start, minOf(start + DEFAULT_SELECTION_MILLIS, duration)))
            }
        }
    }

    fun setSelection(startMillis: Long, endMillis: Long) {
        val duration = _state.value.recording?.durationMillis ?: return
        val a = minOf(startMillis, endMillis).coerceIn(0L, duration)
        val b = maxOf(startMillis, endMillis).coerceIn(0L, duration)
        _state.update { it.copy(selection = TimeSelection(a, b)) }
    }

    /** Seleciona do marcador dado ate o proximo (ou o fim): "exportar daqui ate o proximo marcador" em 1 toque. */
    fun selectFromMarker(marker: Marker) {
        val s = _state.value
        val next = s.markers.filter { it.positionMillis > marker.positionMillis }.minByOrNull { it.positionMillis }
        val end = next?.positionMillis ?: s.recording?.durationMillis ?: return
        _state.update { it.copy(selecting = true, selection = TimeSelection(marker.positionMillis, end)) }
    }

    fun exportSelection() {
        val s = _state.value
        val recording = s.recording ?: return
        val selection = s.selection ?: return
        if (s.exporting || selection.lengthMillis < MIN_CLIP_MILLIS) return
        _state.update { it.copy(exporting = true) }
        viewModelScope.launch {
            val outcome = clips.export(recording, selection.startMillis, selection.endMillis)
            _state.update { it.copy(exporting = false, selecting = false, selection = null) }
            _events.tryEmit(if (outcome is ClipOutcome.Saved) PlayerEvent.ClipSaved(outcome.title) else PlayerEvent.ClipFailed)
        }
    }

    /** Onda 2, "dividir aqui": duas gravacoes NOVAS (antes e depois do ponto); o original fica intacto. */
    fun splitHere(positionMillis: Long) {
        val s = _state.value
        val recording = s.recording ?: return
        if (s.splitting || s.exporting) return
        _state.update { it.copy(splitting = true) }
        viewModelScope.launch {
            val outcome = splits.split(recording, positionMillis)
            _state.update { it.copy(splitting = false) }
            when (outcome) {
                is SplitOutcome.Saved -> _events.tryEmit(PlayerEvent.SplitSaved(outcome.titles.first, outcome.titles.second))
                is SplitOutcome.Failed -> _events.tryEmit(PlayerEvent.SplitFailed)
            }
        }
    }

    /** Tags existentes, para sugerir no dialogo. */
    suspend fun knownTags(): List<String> = com.gravasom.core.domain.tags.TagFilter.available(repo.observeAll().first())

    private companion object {
        const val EVENT_BUFFER = 8
        const val DEFAULT_SELECTION_MILLIS = 10_000L
        const val MIN_CLIP_MILLIS = 200L
    }
}
