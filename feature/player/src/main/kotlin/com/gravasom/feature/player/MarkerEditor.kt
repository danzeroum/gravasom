package com.gravasom.feature.player

import com.gravasom.core.domain.marker.AddMarker
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.marker.MarkerRepository
import com.gravasom.core.domain.marker.MarkerResult
import com.gravasom.core.domain.marker.UpdateMarker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** O que o dialogo de marcador esta editando. */
sealed interface MarkerDialogTarget {
    /** Marcador novo: NADA foi gravado ainda; so [MarkerEditor.save] cria a linha (cancelar nao deixa marcador vazio). */
    data class Draft(val recordingId: Long, val positionMillis: Long) : MarkerDialogTarget

    data class Existing(val marker: Marker) : MarkerDialogTarget
}

/**
 * Regras de edicao de marcadores da tela do player, sem Android para poder ser testada na JVM:
 * - criar e um RASCUNHO ate Salvar (cancelar o dialogo nao grava nada);
 * - excluir e desfazivel por [UNDO_WINDOW_MILLIS]: o marcador volta com o mesmo id, posicao, nota, nome e cor.
 */
class MarkerEditor(
    private val repo: MarkerRepository,
    private val add: AddMarker,
    private val update: UpdateMarker,
    private val scope: CoroutineScope,
    private val undoWindowMillis: Long = UNDO_WINDOW_MILLIS,
) {
    private val _dialog = MutableStateFlow<MarkerDialogTarget?>(null)
    val dialog: StateFlow<MarkerDialogTarget?> = _dialog.asStateFlow()

    private val _undo = MutableStateFlow<Marker?>(null)

    /** Ultimo marcador excluido que ainda pode voltar; some depois de [UNDO_WINDOW_MILLIS]. */
    val undo: StateFlow<Marker?> = _undo.asStateFlow()

    private var undoExpiry: Job? = null

    fun beginAdd(recordingId: Long, positionMillis: Long) {
        _dialog.value = MarkerDialogTarget.Draft(recordingId, positionMillis)
    }

    fun beginEdit(marker: Marker) {
        _dialog.value = MarkerDialogTarget.Existing(marker)
    }

    fun dismiss() {
        _dialog.value = null
    }

    /** Grava o rascunho (cria) ou a edicao (atualiza). O dialogo decide se fecha (salvo) ou mostra o erro (rejeitado). */
    suspend fun save(target: MarkerDialogTarget, note: String, label: String): MarkerResult = when (target) {
        is MarkerDialogTarget.Draft -> add(target.recordingId, target.positionMillis, note, label)
        is MarkerDialogTarget.Existing -> update(target.marker.copy(note = note, label = label))
    }

    fun delete(marker: Marker) {
        _undo.value = marker
        undoExpiry?.cancel()
        undoExpiry = scope.launch {
            delay(undoWindowMillis)
            if (_undo.value?.id == marker.id) _undo.value = null
        }
        scope.launch { repo.delete(marker.id) }
    }

    /** Devolve o ultimo marcador excluido (se ainda dentro da janela). Retorna se algo foi restaurado. */
    suspend fun undoDelete(): Boolean {
        val marker = _undo.value ?: return false
        undoExpiry?.cancel()
        _undo.value = null
        repo.insert(marker)
        return true
    }

    companion object {
        /** Tempo que o "Desfazer" fica disponivel depois de excluir um marcador. */
        const val UNDO_WINDOW_MILLIS = 5_000L
    }
}
