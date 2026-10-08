package com.gravasom.feature.library

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gravasom.core.domain.marker.MarkerNote
import com.gravasom.core.domain.marker.MarkerRejection
import com.gravasom.core.domain.marker.MarkerResult
import com.gravasom.core.domain.model.Recording
import com.gravasom.feature.player.MarkerCallbacks
import com.gravasom.feature.player.MarkerDialog
import com.gravasom.feature.player.PlayerCallbacks
import com.gravasom.feature.player.PlayerController
import com.gravasom.feature.player.PlayerEvent
import com.gravasom.feature.player.PlayerPanel
import com.gravasom.feature.player.PlayerViewModel
import com.gravasom.feature.player.SelectionCallbacks
import com.gravasom.feature.player.SkipCallbacks
import com.gravasom.feature.player.TagsDialog
import kotlinx.coroutines.launch
import com.gravasom.feature.player.R as PlayerR

private const val PANEL_FRACTION = 0.62f

/** Painel do player da gravacao aberta, com os dialogos de marcador e de etiquetas. */
@Composable
internal fun PlayerDock(current: Recording, player: PlayerController, vm: PlayerViewModel, onClose: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editingMarkerId by remember { mutableStateOf<Long?>(null) }
    var editingTags by remember { mutableStateOf(false) }
    if (state.recording?.id != current.id) return
    val callbacks = PlayerCallbacks(
        onClose = onClose,
        onEditTags = { editingTags = true },
        markers = MarkerCallbacks(
            onAdd = { position -> scope.launch { (vm.addMarker(position) as? MarkerResult.Saved)?.let { editingMarkerId = it.id } } },
            onEdit = { editingMarkerId = it.id },
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
    state.markers.firstOrNull { it.id == editingMarkerId }?.let { marker ->
        MarkerDialog(
            marker = marker,
            onDismiss = { editingMarkerId = null },
            onSave = { note, label -> vm.saveMarker(marker, note, label) },
            onDelete = { vm.deleteMarker(marker) },
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
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            val text = when (e) {
                is PlayerEvent.MarkerRejected -> when (e.reason) {
                    MarkerRejection.NOTE_TOO_LONG -> noteTooLong
                    MarkerRejection.OUT_OF_RANGE -> outOfRange
                    MarkerRejection.RECORDING_NOT_FOUND -> missing
                }
                is PlayerEvent.ClipSaved -> clipSaved.format(e.title)
                PlayerEvent.ClipFailed -> clipFailed
                PlayerEvent.AnalysisFailed -> analysisFailed
            }
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }
}
