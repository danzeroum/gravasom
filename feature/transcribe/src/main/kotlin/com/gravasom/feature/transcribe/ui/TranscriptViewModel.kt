package com.gravasom.feature.transcribe.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gravasom.core.domain.transcript.ModelManager
import com.gravasom.core.domain.transcript.ModelState
import com.gravasom.core.domain.transcript.Transcript
import com.gravasom.core.domain.transcript.TranscriptRepository
import com.gravasom.core.domain.transcript.TranscriptStatus
import com.gravasom.core.domain.transcript.TranscriptionLang
import com.gravasom.core.domain.transcript.TranscriptionScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado da aba Transcricao de uma gravacao. */
data class TranscriptUiState(
    val language: TranscriptionLang = TranscriptionLang.PT_BR,
    val transcripts: List<Transcript> = emptyList(),
    val model: ModelState = ModelState.Missing,
) {
    /** A transcricao do idioma atual, se existir. */
    val atual: Transcript? get() = transcripts.firstOrNull { it.language == language }
    val rodando: Boolean get() = atual?.status == TranscriptStatus.RUNNING || atual?.status == TranscriptStatus.PENDING
}

/**
 * Aba "Transcricao" do detalhe. Mostra o texto (ou o progresso/erro) e permite transcrever de novo.
 * O modelo nao e baixado aqui: o download fica em Ajustes -> Gerenciar modelos (ADR-016 secao 2).
 */
@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val transcripts: TranscriptRepository,
    private val models: ModelManager,
    private val scheduler: TranscriptionScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(TranscriptUiState())
    val state: StateFlow<TranscriptUiState> = _state.asStateFlow()

    private var recordingId: Long = -1L

    fun bind(recordingId: Long, language: TranscriptionLang) {
        if (this.recordingId == recordingId && _state.value.language == language) return
        this.recordingId = recordingId
        _state.update { it.copy(language = language) }
        viewModelScope.launch {
            transcripts.observeForRecording(recordingId).collect { lista ->
                _state.update { it.copy(transcripts = lista) }
            }
        }
        viewModelScope.launch { _state.update { it.copy(model = models.state(language)) } }
    }

    /** (Re)transcreve a gravacao no idioma atual. Idempotente enquanto roda. */
    fun transcrever() {
        if (recordingId <= 0 || _state.value.rodando) return
        scheduler.enqueue(recordingId, _state.value.language)
    }

    /** Cancela o trabalho em andamento. */
    fun cancelar() {
        if (recordingId <= 0) return
        scheduler.cancel(recordingId)
    }
}
