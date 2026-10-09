package com.gravasom.feature.enhance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gravasom.core.audio.engine.enhance.EnhanceReport
import com.gravasom.core.domain.preset.EffectDefaults
import com.gravasom.core.domain.repository.RecordingRepository
import com.gravasom.core.service.enhance.EnhanceOutcome
import com.gravasom.core.service.enhance.EnhancePreview
import com.gravasom.core.service.enhance.EnhanceService
import com.gravasom.core.service.enhance.EnhanceSettings
import com.gravasom.core.service.enhance.PreparedRecording
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/** Lado da previa que esta tocando. */
enum class EnhanceSide { A, B }

enum class EnhanceStatus { LOADING, READY, EXPORTING, SAVED, FAILED }

/** Estado da tela "Melhorar som": ajustes, previa A/B e a exportacao da copia. */
data class EnhanceUiState(
    val status: EnhanceStatus = EnhanceStatus.LOADING,
    val title: String = "",
    val durationMillis: Long = 0L,
    val settings: EnhanceSettings = EnhanceSettings.fromDefaults(EffectDefaults.OFF),
    val previewStartMillis: Long = 0L,
    val previewMillis: Long = 0L,
    val previewing: EnhanceSide? = null,
    val previewBusy: Boolean = false,
    val progress: Float = 0f,
    val savedTitle: String? = null,
    val failedReason: Int? = null,
    /** Aviso curto que nao muda a fase (ex.: "ligue o redutor ou o volume"). */
    val notice: Int? = null,
    /** Numeros da copia salva, para o resumo. */
    val report: EnhanceReport? = null,
)

/**
 * Tela "Melhorar som": prepara a gravacao (decodifica para WAV e mede a loudness), deixa o usuario ajustar redutor e volume com
 * previa A/B e salva a copia melhorada. Cancelavel: sair da tela ou tocar em Cancelar NAO deixa arquivo novo (ADR-008).
 */
@HiltViewModel
class EnhanceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: RecordingRepository,
    private val service: EnhanceService,
    private val previewPlayer: PreviewPlayer,
) : ViewModel() {

    private val recordingId: Long = checkNotNull(savedStateHandle[ARG_RECORDING_ID])
    private val cancelled = AtomicBoolean(false)
    private var prepared: PreparedRecording? = null
    private var preview: EnhancePreview? = null

    private val _state = MutableStateFlow(EnhanceUiState())
    val state: StateFlow<EnhanceUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val recording = repo.getById(recordingId)
            if (recording == null) {
                _state.update { it.copy(status = EnhanceStatus.FAILED, failedReason = R.string.enhance_fail_missing) }
                return@launch
            }
            _state.update { it.copy(title = recording.title, durationMillis = recording.durationMillis) }
            cancelled.set(false)
            val p = service.prepare(recording) { cancelled.get() }
            if (p == null) {
                _state.update { it.copy(status = EnhanceStatus.FAILED, failedReason = R.string.enhance_fail_decode) }
                return@launch
            }
            prepared = p
            val window = minOf(EnhanceService.DEFAULT_WINDOW_MILLIS, p.wav.durationMillis)
            _state.update { it.copy(status = EnhanceStatus.READY, settings = service.settingsFor(recording), previewMillis = window) }
        }
    }

    fun setDenoise(on: Boolean) = updateSettings { it.copy(denoise = on) }

    fun setDenoiseIntensity(value: Float) = updateSettings { it.copy(denoiseIntensity = value.coerceIn(0f, 1f)) }

    fun setNormalize(on: Boolean) = updateSettings { it.copy(normalize = on) }

    fun setTargetLufs(value: Float) = updateSettings { it.copy(targetLufs = value) }

    /** Move a janela da previa; invalida os trechos gerados. */
    fun setPreviewStart(millis: Long) {
        if (_state.value.status != EnhanceStatus.READY) return
        _state.update {
            val max = maxOf(0L, it.durationMillis - it.previewMillis)
            it.copy(previewStartMillis = millis.coerceIn(0L, max))
        }
        invalidatePreview()
    }

    /** Toca o lado A ou B (toca de novo para o outro; tocar o mesmo lado para a). */
    fun preview(side: EnhanceSide) {
        val p = prepared ?: return
        if (_state.value.status != EnhanceStatus.READY || _state.value.previewBusy) return
        if (_state.value.previewing == side) {
            stopPreview()
            return
        }
        if (preview == null) {
            generatePreviewThen(p, side)
        } else {
            startPlayback(side)
        }
    }

    fun stopPreview() {
        previewPlayer.stop()
        _state.update { it.copy(previewing = null) }
    }

    /** Exporta a copia melhorada; progresso e cancelamento aparecem na tela. */
    fun export() {
        val p = prepared ?: return
        val s = _state.value
        if (s.status != EnhanceStatus.READY) return
        if (s.settings.isIdentity) {
            _state.update { it.copy(notice = R.string.enhance_notice_identity) }
            return
        }
        stopPreview()
        cancelled.set(false)
        _state.update { it.copy(status = EnhanceStatus.EXPORTING, progress = 0f, notice = null) }
        viewModelScope.launch {
            val outcome = service.export(p, s.settings, { cancelled.get() }) { f -> _state.update { it.copy(progress = f) } }
            when (outcome) {
                is EnhanceOutcome.Saved -> _state.update {
                    it.copy(status = EnhanceStatus.SAVED, savedTitle = outcome.title, report = outcome.report)
                }
                EnhanceOutcome.Cancelled -> _state.update { it.copy(status = EnhanceStatus.READY, progress = 0f) }
                is EnhanceOutcome.Failed -> _state.update {
                    it.copy(status = EnhanceStatus.FAILED, failedReason = R.string.enhance_fail_export)
                }
            }
        }
    }

    fun cancelExport() {
        cancelled.set(true)
    }

    override fun onCleared() {
        cancelled.set(true)
        previewPlayer.stop()
    }

    private fun updateSettings(change: (EnhanceSettings) -> EnhanceSettings) {
        val s = _state.value
        if (s.status != EnhanceStatus.READY) return
        val next = change(s.settings)
        if (next == s.settings) return
        _state.update { it.copy(settings = next) }
        invalidatePreview()
        val recording = prepared?.recording ?: return
        viewModelScope.launch { service.saveSettings(recording, next) }
    }

    private fun invalidatePreview() {
        preview = null
        previewPlayer.stop()
        _state.update { it.copy(previewing = null) }
    }

    private fun generatePreviewThen(p: PreparedRecording, side: EnhanceSide) {
        _state.update { it.copy(previewBusy = true) }
        viewModelScope.launch {
            val s = _state.value
            val generated = service.preview(p, s.settings, s.previewStartMillis)
            _state.update { it.copy(previewBusy = false) }
            if (generated == null) {
                _state.update { it.copy(notice = R.string.enhance_fail_preview) }
                return@launch
            }
            preview = generated
            startPlayback(side)
        }
    }

    private fun startPlayback(side: EnhanceSide) {
        val current = preview ?: return
        val file = if (side == EnhanceSide.A) current.original else current.enhanced
        previewPlayer.stop()
        _state.update { it.copy(previewing = side) }
        viewModelScope.launch {
            if (!previewPlayer.play(file) { stopPreview() }) {
                _state.update { it.copy(previewing = null, notice = R.string.enhance_fail_preview) }
            }
        }
    }

    private companion object {
        const val ARG_RECORDING_ID = "recordingId"
    }
}
