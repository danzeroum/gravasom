package com.gravasom.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gravasom.core.audio.engine.analysis.SilenceSensitivity
import com.gravasom.core.audio.engine.analysis.ViewWindow
import com.gravasom.core.domain.clip.ClipTitle
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.marker.MarkerNavigation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Tudo que o painel pede ao resto do app. */
class PlayerCallbacks(
    val onClose: () -> Unit,
    val onAddMarker: (positionMillis: Long) -> Unit,
    val onEditMarker: (Marker) -> Unit,
    val onToggleSelecting: (on: Boolean, positionMillis: Long) -> Unit,
    val onSelection: (Long, Long) -> Unit,
    val onExport: () -> Unit,
    val onSkipEnabled: (Boolean) -> Unit,
    val onSensitivity: (SilenceSensitivity) -> Unit,
    val onEditTags: () -> Unit,
    val onSelectToNextMarker: (Marker) -> Unit,
)

/**
 * Player da gravacao aberta: onda com zoom, marcadores, pular silencio, selecao de trecho e exportacao.
 * Etapa "Encontrar e rever" da jornada (docs/ux/jornada.md): achar um trecho marcado em <= 3 toques
 * (abrir a gravacao -> tocar no marcador -> ja esta tocando ali).
 */
@Composable
fun PlayerPanel(
    controller: PlayerController,
    state: PlayerUiState,
    callbacks: PlayerCallbacks,
    maxHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val recording = state.recording ?: return
    val playing by controller.isPlaying.collectAsStateWithLifecycle()
    val seconds by remember(controller) { controller.positionMillis.map { it / MILLIS }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(0L)
    val position by controller.positionMillis.collectAsStateWithLifecycle()
    val skippedNow by controller.skippedMillis.collectAsStateWithLifecycle()
    val duration = maxOf(recording.durationMillis, controller.durationMillis.value)
    var window by remember(recording.id, duration) { mutableStateOf(ViewWindow.full(duration)) }
    var speed by remember(recording.id) { mutableStateOf(1f) }

    // Com zoom, a janela acompanha a reproducao.
    LaunchedEffect(recording.id) {
        controller.positionMillis.collect { p -> if (controller.isPlaying.value && window.spanMillis < window.durationMillis) window = window.follow(p) }
    }
    LaunchedEffect(state.plan, state.skipEnabled) { controller.setSkip(state.plan, state.skipEnabled) }

    val actions = remember(state.markers, controller) {
        WaveformActions(
            onSeek = { controller.seekTo(it) },
            onMarkerClick = { controller.seekToAndPlay(it.positionMillis) },
            onSelection = callbacks.onSelection,
            onPreviousMarker = { MarkerNavigation.previous(state.markers, controller.positionMillis.value)?.let { controller.seekToAndPlay(it.positionMillis) } },
            onNextMarker = { MarkerNavigation.next(state.markers, controller.positionMillis.value)?.let { controller.seekToAndPlay(it.positionMillis) } },
        )
    }
    val labels = WaveformLabels(
        description = stringResource(R.string.player_wave_desc),
        previousMarker = stringResource(R.string.player_wave_prev_marker),
        nextMarker = stringResource(R.string.player_wave_next_marker),
        zoomIn = stringResource(R.string.player_zoom_in),
        zoomOut = stringResource(R.string.player_zoom_out),
    )

    Card(modifier.fillMaxWidth().testTag("mini_player")) {
        Column(
            Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    recording.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).testTag("titulo_player"),
                )
                IconButton(onClick = callbacks.onClose, modifier = Modifier.testTag("fechar_player")) {
                    Icon(Icons.Filled.Close, stringResource(R.string.player_close))
                }
            }
            Box(Modifier.fillMaxWidth().heightIn(min = WAVE_HEIGHT, max = WAVE_HEIGHT)) {
                WaveformView(
                    waveform = state.waveform,
                    durationMillis = duration,
                    window = { window },
                    onWindowChange = { window = it },
                    positionMillis = { position },
                    markers = state.markers,
                    skipRanges = if (state.skipEnabled) state.plan?.ranges.orEmpty() else emptyList(),
                    selection = state.selection,
                    selecting = state.selecting,
                    actions = actions,
                    labels = labels,
                    modifier = Modifier.fillMaxWidth().heightIn(min = WAVE_HEIGHT, max = WAVE_HEIGHT).testTag("mini_posicao"),
                )
                if (state.analyzing) {
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.player_wave_analyzing), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.player_time, ClipTitle.time(seconds * MILLIS), ClipTitle.time(duration)),
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).testTag("tempo_player"),
                )
                IconButton(onClick = { window = window.zoom(1f / ZOOM_STEP, window.startMillis + window.spanMillis / 2) }, modifier = Modifier.testTag("zoom_menos")) {
                    Icon(Icons.Filled.ZoomOut, stringResource(R.string.player_zoom_out))
                }
                IconButton(onClick = { window = window.zoom(ZOOM_STEP, window.startMillis + window.spanMillis / 2) }, modifier = Modifier.testTag("zoom_mais")) {
                    Icon(Icons.Filled.ZoomIn, stringResource(R.string.player_zoom_in))
                }
            }
            Transport(controller, state, callbacks, playing, position)
            Chips(state, callbacks, speed, skippedNow) { speed = it; controller.setSpeed(it) }
            if (state.selecting) SelectionBar(state, callbacks)
            MarkerList(state.markers, controller, callbacks)
        }
    }
}

@Composable
private fun Transport(controller: PlayerController, state: PlayerUiState, cb: PlayerCallbacks, playing: Boolean, position: Long) {
    val addMarkerDescription = stringResource(R.string.player_add_marker)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        IconButton(
            onClick = { MarkerNavigation.previous(state.markers, controller.positionMillis.value)?.let { controller.seekToAndPlay(it.positionMillis) } },
            enabled = MarkerNavigation.previous(state.markers, position) != null,
            modifier = Modifier.testTag("marcador_anterior"),
        ) { Icon(Icons.Filled.SkipPrevious, stringResource(R.string.player_wave_prev_marker)) }
        FilledIconButton(onClick = { if (playing) controller.pause() else controller.play() }, modifier = Modifier.size(56.dp).testTag("mini_play_pause")) {
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, stringResource(if (playing) R.string.player_pause else R.string.player_play))
        }
        IconButton(
            onClick = { MarkerNavigation.next(state.markers, controller.positionMillis.value)?.let { controller.seekToAndPlay(it.positionMillis) } },
            enabled = MarkerNavigation.next(state.markers, position) != null,
            modifier = Modifier.testTag("marcador_proximo"),
        ) { Icon(Icons.Filled.SkipNext, stringResource(R.string.player_wave_next_marker)) }
        Box(Modifier.weight(1f))
        FilledTonalButton(
            onClick = { cb.onAddMarker(controller.positionMillis.value) },
            modifier = Modifier.testTag("adicionar_marcador").semantics { contentDescription = addMarkerDescription },
        ) {
            Icon(Icons.Filled.Flag, null, Modifier.size(18.dp))
            Text(stringResource(R.string.player_add_marker_short), Modifier.padding(start = 6.dp))
        }
        IconToggleButton(
            checked = state.selecting,
            onCheckedChange = { cb.onToggleSelecting(it, controller.positionMillis.value) },
            modifier = Modifier.testTag("selecionar_trecho"),
        ) {
            Icon(Icons.Filled.ContentCut, stringResource(if (state.selecting) R.string.player_select_clip_off else R.string.player_select_clip))
        }
    }
}

@Composable
private fun Chips(state: PlayerUiState, cb: PlayerCallbacks, speed: Float, skippedNow: Long, onSpeed: (Float) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        FilterChip(
            selected = state.skipEnabled,
            onClick = { cb.onSkipEnabled(!state.skipEnabled) },
            label = { Text(stringResource(R.string.player_skip_silence)) },
            modifier = Modifier.testTag("pular_silencio"),
        )
        if (state.skipEnabled) {
            val sens = state.sensitivity
            AssistChip(
                onClick = { cb.onSensitivity(SilenceSensitivity.entries[(sens.ordinal + 1) % SilenceSensitivity.entries.size]) },
                label = { Text(stringResource(R.string.player_sensitivity, sensitivityName(sens))) },
                modifier = Modifier.testTag("sensibilidade_silencio"),
            )
        }
        AssistChip(
            onClick = { onSpeed(Speeds[(Speeds.indexOf(speed) + 1) % Speeds.size]) },
            label = { Text(stringResource(R.string.player_speed, speed.toString().removeSuffix(".0"))) },
            modifier = Modifier.testTag("mini_velocidade"),
        )
        AssistChip(
            onClick = cb.onEditTags,
            label = { Text(stringResource(R.string.player_tags_button)) },
            modifier = Modifier.testTag("editar_etiquetas"),
        )
    }
    if (state.skipEnabled) {
        val plan = state.plan
        val text = when {
            plan == null -> null
            plan.skippedMillis <= 0L -> stringResource(R.string.player_skip_none)
            skippedNow > 0L -> stringResource(R.string.player_skip_saved_now, ClipTitle.time(skippedNow))
            else -> stringResource(R.string.player_skip_saves, ClipTitle.time(plan.skippedMillis))
        }
        if (text != null) Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("economia_silencio"))
    }
}

@Composable
private fun sensitivityName(s: SilenceSensitivity): String = stringResource(
    when (s) {
        SilenceSensitivity.GENTLE -> R.string.player_sens_gentle
        SilenceSensitivity.NORMAL -> R.string.player_sens_normal
    },
)

@Composable
private fun SelectionBar(state: PlayerUiState, cb: PlayerCallbacks) {
    val s = state.selection
    Column(Modifier.fillMaxWidth().testTag("barra_trecho"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (s == null || s.lengthMillis <= 0L) {
            Text(stringResource(R.string.player_clip_hint), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                stringResource(R.string.player_clip_range, ClipTitle.time(s.startMillis), ClipTitle.time(s.endMillis), ClipTitle.time(s.lengthMillis)),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("intervalo_trecho"),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = cb.onExport,
                enabled = s != null && s.lengthMillis > 0L && !state.exporting,
                modifier = Modifier.testTag("exportar_trecho"),
            ) { Text(stringResource(if (state.exporting) R.string.player_clip_exporting else R.string.player_clip_export)) }
            OutlinedButton(onClick = { cb.onToggleSelecting(false, 0L) }, modifier = Modifier.testTag("cancelar_trecho")) {
                Text(stringResource(R.string.player_clip_cancel))
            }
        }
    }
}

@Composable
private fun MarkerList(markers: List<Marker>, controller: PlayerController, cb: PlayerCallbacks) {
    Text(stringResource(R.string.player_markers_title, markers.size), style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("titulo_marcadores"))
    if (markers.isEmpty()) {
        Text(stringResource(R.string.player_markers_empty), style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("sem_marcadores"))
        return
    }
    markers.forEach { m ->
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { controller.seekToAndPlay(m.positionMillis) }.testTag("marcador_item_${m.id}"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(ClipTitle.time(m.positionMillis), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.label.ifBlank { stringResource(R.string.player_marker_default) }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (m.note.isNotBlank()) Text(m.note, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { cb.onSelectToNextMarker(m) }, modifier = Modifier.testTag("trecho_do_marcador_${m.id}")) {
                Icon(Icons.Filled.ContentCut, stringResource(R.string.player_marker_select))
            }
            IconButton(onClick = { cb.onEditMarker(m) }, modifier = Modifier.testTag("editar_marcador_${m.id}")) {
                Icon(Icons.Filled.Edit, stringResource(R.string.player_marker_edit, ClipTitle.time(m.positionMillis)))
            }
        }
    }
}

private val WAVE_HEIGHT = 112.dp
private const val MILLIS = 1000L
private const val ZOOM_STEP = 2f
private val Speeds = listOf(1f, 1.5f, 2f, 0.5f)
