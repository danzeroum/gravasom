package com.gravasom.feature.record

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gravasom.core.audio.engine.RecordingError
import com.gravasom.core.audio.engine.RecordingState
import com.gravasom.core.data.metrics.UsageMetrics
import com.gravasom.core.data.preferences.PreferencesRepository
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.metrics.UsageEventType
import com.gravasom.core.service.AudioInputs
import com.gravasom.core.service.InputOption
import com.gravasom.core.service.RecordingService
import com.gravasom.core.service.RecordingServiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Situacao da gravacao vista pela tela (status sempre visivel: heuristica 1 de Nielsen). */
sealed interface RecordStatus {
    data object Ready : RecordStatus
    data object Preparing : RecordStatus
    data object Recording : RecordStatus
    data object Paused : RecordStatus
    data object Saving : RecordStatus
    data class Failed(val cause: RecordingError) : RecordStatus
}

data class RecordUiState(
    val status: RecordStatus = RecordStatus.Ready,
    val justSaved: Boolean = false,
    val elapsedMillis: Long = 0L,
    val levelDb: Float = MIN_DB,
    val format: RecordingFormat = RecordingFormat.M4A,
    val privateMode: Boolean = false,
    val activeInput: InputOption = InputOption(-1, ""),
    val inputs: List<InputOption> = emptyList(),
) {
    val isBusy: Boolean get() = status != RecordStatus.Ready && status !is RecordStatus.Failed

    companion object {
        const val MIN_DB = -60f
    }
}

/**
 * Tela Gravar. Etapas da jornada: Abrir e Gravar. Hipotese H1 (docs/ux/jornada.md): do toque em Gravar ate o estado
 * Gravando em <= 2 s (medido: TEMPO_ATE_GRAVAR).
 */
@HiltViewModel
class RecordViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesRepository,
    private val metrics: UsageMetrics,
) : ViewModel() {

    private val _state = MutableStateFlow(RecordUiState())
    val state: StateFlow<RecordUiState> = _state.asStateFlow()

    private var tappedAt = 0L

    /** Ultimo tempo toque->Gravando em ms (0 = ainda nao medido); lido pelos testes de UI. */
    private val _lastTimeToRecord = MutableStateFlow(0L)
    val lastTimeToRecordMillis: StateFlow<Long> = _lastTimeToRecord.asStateFlow()

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refreshInputs()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refreshInputs()
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        refreshInputs()
        viewModelScope.launch { prefs.format.collect { f -> _state.update { it.copy(format = f) } } }
        viewModelScope.launch { prefs.privateVault.collect { v -> _state.update { it.copy(privateMode = v) } } }
        viewModelScope.launch { prefs.preferredInputId.collect { refreshInputs() } }
        viewModelScope.launch { RecordingServiceState.state.collect(::onServiceState) }
        viewModelScope.launch { RecordingServiceState.levelDb.collect { db -> _state.update { it.copy(levelDb = db) } } }
        viewModelScope.launch { tickElapsed() }
    }

    private fun refreshInputs() {
        viewModelScope.launch {
            val preferred = prefs.preferredInputId.first()
            _state.update { it.copy(inputs = AudioInputs.available(context), activeInput = AudioInputs.active(context, preferred)) }
        }
    }

    private suspend fun onServiceState(s: RecordingState) {
        val status = s.toStatus()
        _state.update { it.copy(status = status, justSaved = s is RecordingState.Saved || (it.justSaved && status == RecordStatus.Ready)) }
        if (s is RecordingState.Recording && tappedAt != 0L) {
            val ms = SystemClock.elapsedRealtime() - tappedAt
            tappedAt = 0L
            _lastTimeToRecord.value = ms
            metrics.record(UsageEventType.TEMPO_ATE_GRAVAR, ms)
        }
    }

    /** Acumula o tempo gravado (so avanca em Gravando; pausa congela; novo inicio zera). */
    private suspend fun tickElapsed() {
        var last = SystemClock.elapsedRealtime()
        while (true) {
            delay(TICK_MS)
            val now = SystemClock.elapsedRealtime()
            val status = _state.value.status
            _state.update {
                when (status) {
                    RecordStatus.Recording -> it.copy(elapsedMillis = it.elapsedMillis + (now - last))
                    RecordStatus.Preparing -> it.copy(elapsedMillis = 0L)
                    else -> it
                }
            }
            last = now
        }
    }

    fun selectFormat(format: RecordingFormat) {
        if (_state.value.isBusy) return
        viewModelScope.launch {
            if (prefs.format.first() != format) {
                prefs.setFormat(format)
                metrics.record(UsageEventType.FORMATO_ALTERADO, label = format)
            }
        }
    }

    fun selectInput(option: InputOption?) = viewModelScope.launch {
        prefs.setPreferredInputId(option?.id ?: PreferencesRepository.NO_INPUT)
    }

    fun start() {
        tappedAt = SystemClock.elapsedRealtime()
        _state.update { it.copy(justSaved = false) }
        send(RecordingService.ACTION_START)
        viewModelScope.launch { metrics.record(UsageEventType.GRAVACAO_INICIADA, label = _state.value.format) }
    }

    fun pause() = send(RecordingService.ACTION_PAUSE)
    fun resume() = send(RecordingService.ACTION_RESUME)
    fun stop() = send(RecordingService.ACTION_STOP)

    fun onPermissionDenied() {
        viewModelScope.launch { metrics.record(UsageEventType.PERMISSAO_NEGADA) }
    }

    private fun send(action: String) {
        val intent = Intent(context, RecordingService::class.java).setAction(action)
        if (action == RecordingService.ACTION_START) ContextCompat.startForegroundService(context, intent) else context.startService(intent)
    }

    override fun onCleared() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }

    private companion object {
        const val TICK_MS = 200L
    }
}

private fun RecordingState.toStatus(): RecordStatus = when (this) {
    RecordingState.Idle, is RecordingState.Saved -> RecordStatus.Ready
    RecordingState.Preparing -> RecordStatus.Preparing
    is RecordingState.Recording -> RecordStatus.Recording
    RecordingState.Paused -> RecordStatus.Paused
    RecordingState.Stopping -> RecordStatus.Saving
    is RecordingState.Error -> RecordStatus.Failed(cause)
}
