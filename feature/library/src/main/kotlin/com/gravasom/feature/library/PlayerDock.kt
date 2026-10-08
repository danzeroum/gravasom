package com.gravasom.feature.library

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.marker.MarkerNote
import com.gravasom.core.domain.marker.MarkerRejection
import com.gravasom.core.domain.marker.MarkerResult
import com.gravasom.core.domain.model.Recording
import com.gravasom.feature.player.MarkerCallbacks
import com.gravasom.feature.player.MarkerDialog
import com.gravasom.feature.player.MarkerDialogTarget
import com.gravasom.feature.player.MarkerEditor
import com.gravasom.feature.player.PlayerCallbacks
import com.gravasom.feature.player.PlayerController
import com.gravasom.feature.player.PlayerEvent
import com.gravasom.feature.player.PlayerPanel
import com.gravasom.feature.player.PlayerViewModel
import com.gravasom.feature.player.SelectionCallbacks
import com.gravasom.feature.player.SkipCallbacks
import com.gravasom.feature.player.TagsDialog
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.gravasom.feature.player.R as PlayerR

private const val PANEL_FRACTION = 0.62f

/** Painel do player da gravacao aberta, com os dialogos de marcador e de etiquetas. */
@Composable
internal fun PlayerDock(current: Recording, player: PlayerController, vm: PlayerViewModel, onClose: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val target by vm.markerEditor.dialog.collectAsStateWithLifecycle()
    var editingTags by remember { mutableStateOf(false) }
    if (state.recording?.id != current.id) return
    val callbacks = PlayerCallbacks(
        onClose = onClose,
        onEditTags = { editingTags = true },
        markers = MarkerCallbacks(
            onAdd = vm::beginMarker,
            onEdit = vm::editMarker,
            onSelectToNext = vm::selectFromMarker,
        ),
        selection = SelectionCallbacks(
            onToggle = { on, position -> vm.setSelecting(on, position) },
            onChange = vm::setSelection,
            onExport = vm::exportSelection,
        ),
        skip = SkipCallbacks(onEnabled = vm::setSkipEnabled, onSensitivity = vm::setSensitivity),
    )
    BoxWithConstraints {
        PlayerPanel(player, state, callbacks, maxHeight = maxHeight * PANEL_FRACTION, modifier = Modifier.padding(12.dp))
    }
    target?.let { dialogTarget ->
        val marker = when (dialogTarget) {
            is MarkerDialogTarget.Draft -> Marker(recordingId = dialogTarget.recordingId, positionMillis = dialogTarget.positionMillis, label = "", note = "")
            is MarkerDialogTarget.Existing -> state.markers.firstOrNull { it.id == dialogTarget.marker.id } ?: dialogTarget.marker
        }
        MarkerDialog(
            marker = marker,
            onDismiss = vm::dismissMarkerDialog,
            onSave = { note, label -> vm.saveMarker(dialogTarget, note, label) },
            onDelete = { vm.deleteMarker(marker) },
            deletable = dialogTarget is MarkerDialogTarget.Existing,
        )
    }
    if (editingTags) {
        TagsDialog(currentTags = current.tags, knownTags = vm::knownTags, onSave = vm::setTags, onDismiss = { editingTags = false })
    }
}

/** Avisos do player (marcador rejeitado, trecho exportado...) como snackbar. */
@Composable
internal fun PlayerEventsEffect(vm: PlayerViewModel, snackbar: SnackbarHostState) {
    val noteTooLong = stringResource(PlayerR.string.player_marker_rejected_note, MarkerNote.MAX_LENGTH)
    val outOfRange = stringResource(PlayerR.string.player_marker_rejected_range)
    val missing = stringResource(PlayerR.string.player_marker_rejected_missing)
    val clipFailed = stringResource(PlayerR.string.player_clip_failed)
    val analysisFailed = stringResource(PlayerR.string.player_analysis_failed)
    val clipSaved = stringResource(PlayerR.string.player_clip_saved, "%s")
    val markerDeleted = stringResource(PlayerR.string.player_marker_deleted)
    val undo = stringResource(PlayerR.string.player_marker_undo)
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            if (e == PlayerEvent.MarkerDeleted) {
                launch { showUndo(snackbar, markerDeleted, undo, vm::undoMarkerDelete) }
                return@collect
            }
            val text = when (e) {
                is PlayerEvent.MarkerRejected -> when (e.reason) {
                    MarkerRejection.NOTE_TOO_LONG -> noteTooLong
                    MarkerRejection.OUT_OF_RANGE -> outOfRange
                    MarkerRejection.RECORDING_NOT_FOUND -> missing
                }
                is PlayerEvent.ClipSaved -> clipSaved.format(e.title)
                PlayerEvent.ClipFailed -> clipFailed
                PlayerEvent.AnalysisFailed -> analysisFailed
                PlayerEvent.MarkerDeleted -> markerDeleted
            }
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }
}

/** "Marcador excluido" com Desfazer por 5 s (a mesma janela do [com.gravasom.feature.player.MarkerEditor]). */
private suspend fun showUndo(snackbar: SnackbarHostState, message: String, undo: String, onUndo: () -> Unit) {
    snackbar.currentSnackbarData?.dismiss()
    coroutineScope {
        val timer = launch {
            delay(MarkerEditor.UNDO_WINDOW_MILLIS)
            snackbar.currentSnackbarData?.dismiss()
        }
        val result = snackbar.showSnackbar(message, actionLabel = undo, duration = SnackbarDuration.Indefinite)
        timer.cancel()
        if (result == SnackbarResult.ActionPerformed) onUndo()
    }
}
