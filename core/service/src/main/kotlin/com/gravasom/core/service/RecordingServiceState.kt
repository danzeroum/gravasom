package com.gravasom.core.service

import com.gravasom.core.audio.engine.RecordingError
import com.gravasom.core.audio.engine.RecordingState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fonte unica de verdade do estado da gravacao (observada pela UI). */
object RecordingServiceState {

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private val _levelDb = MutableStateFlow(-60f)
    val levelDb: StateFlow<Float> = _levelDb.asStateFlow()

    private val _markerCount = MutableStateFlow(0)

    /** Marcadores feitos na gravacao em curso (tela, notificacao ou widget). */
    val markerCount: StateFlow<Int> = _markerCount.asStateFlow()

    fun updateMarkerCount(count: Int) {
        _markerCount.value = count
    }

    private val _saved = MutableSharedFlow<String>(extraBufferCapacity = SAVED_BUFFER)

    /** Caminho de cada arquivo finalizado (inclusive quando a gravacao termina por erro apos preservar o audio). */
    val saved: SharedFlow<String> = _saved.asSharedFlow()

    fun emitSaved(path: String) {
        _saved.tryEmit(path)
    }

    fun update(state: RecordingState) {
        _state.value = state
    }

    fun updateElapsed(millis: Long) {
        _elapsedMillis.value = millis
    }

    fun updateLevel(db: Float) {
        _levelDb.value = db
    }

    fun error(cause: RecordingError) {
        _state.value = RecordingState.Error(cause)
    }

    private const val SAVED_BUFFER = 16
}
