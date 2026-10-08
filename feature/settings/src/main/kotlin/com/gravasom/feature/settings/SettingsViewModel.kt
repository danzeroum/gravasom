package com.gravasom.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gravasom.core.data.metrics.UsageMetrics
import com.gravasom.core.data.preferences.PreferencesRepository
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.metrics.LibraryStats
import com.gravasom.core.domain.metrics.UsageEventType
import com.gravasom.core.domain.preset.FormatPreset
import com.gravasom.core.domain.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val format: RecordingFormat = RecordingFormat.M4A,
    val bitrate: Int = 128,
    val privateMode: Boolean = false,
    val numbersOptIn: Boolean = false,
    /** Preset de formato escolhido; null = personalizado. */
    val preset: FormatPreset? = null,
)

/** Contadores de uso (so existem com opt-in) mais as estatisticas da biblioteca (sempre disponiveis, locais). */
data class NumbersUiState(
    val library: LibraryStats = LibraryStats(0, 0L, null),
    val optIn: Boolean = false,
    val started: Int = 0,
    val shared: Int = 0,
    val searches: Int = 0,
    val imported: Int = 0,
)

/**
 * Ajustes e "Seus numeros". Etapas: Descobrir (confianca) e Gravar (formato). Hipoteses H5 e "trocar para MP3 sem ajuda"
 * em docs/ux/jornada.md.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val repo: RecordingRepository,
    private val metrics: UsageMetrics,
) : ViewModel() {

    val settings: StateFlow<SettingsUiState> = combine(
        prefs.format, prefs.bitrate, prefs.privateVault, prefs.telemetryOptIn, prefs.preset,
    ) { f, b, p, n, preset -> SettingsUiState(f, b, p, n, preset) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    private val counters = combine(
        metrics.count(UsageEventType.GRAVACAO_INICIADA), metrics.count(UsageEventType.COMPARTILHADO),
        metrics.count(UsageEventType.BUSCA_USADA), metrics.count(UsageEventType.IMPORTADO),
    ) { a, b, c, d -> listOf(a, b, c, d) }

    val numbers: StateFlow<NumbersUiState> = combine(
        repo.observeAll().map { LibraryStats.of(it) }, prefs.telemetryOptIn, counters,
    ) { lib, opt, c -> NumbersUiState(lib, opt, c[0], c[1], c[2], c[3]) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), NumbersUiState())

    fun setFormat(format: RecordingFormat) = viewModelScope.launch {
        if (settings.value.format != format) metrics.record(UsageEventType.FORMATO_ALTERADO, label = format)
        prefs.setFormat(format)
    }

    fun setBitrate(kbps: Int) = viewModelScope.launch { prefs.setBitrate(kbps) }
    fun setPreset(preset: FormatPreset) = viewModelScope.launch { prefs.setPreset(preset) }
    fun setPrivate(enabled: Boolean) = viewModelScope.launch { prefs.setPrivateVault(enabled) }
    fun setNumbersOptIn(enabled: Boolean) = viewModelScope.launch { prefs.setTelemetryOptIn(enabled) }

    fun numbersOpened() = viewModelScope.launch { metrics.record(UsageEventType.NUMEROS_ABERTO) }
    fun clearNumbers() = viewModelScope.launch { metrics.clear() }
    suspend fun exportCsv(): String = metrics.exportCsv()

    suspend fun medianTimeToRecord(): Long? = metrics.median(UsageEventType.TEMPO_ATE_GRAVAR)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
