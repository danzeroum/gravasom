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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gravasom.core.audio.engine.analysis.SilenceSensitivity
import com.gravasom.core.audio.engine.analysis.ViewWindow
import com.gravasom.core.domain.clip.ClipTitle
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.marker.MarkerNavigation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Player da gravacao aberta: onda com zoom, marcadores, pular silencio, selecao de trecho e exportacao.
 * Etapa "Encontrar e rever" da jornada (docs/ux/jornada.md): achar um trecho marcado em <= 3 toques
 * (abrir a gravacao -> tocar no marcador -> ja esta tocando ali).
 */
@Composable
fun PlayerPanel(
    controller: PlayerPort,
    state: PlayerUiState,
    callbacks: PlayerCallbacks,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val recording = state.recording ?: return
    val duration = maxOf(recording.durationMillis, controller.durationMillis.value)
    var window by remember(recording.id, duration) { mutableStateOf(ViewWindow.full(duration)) }
    FollowPlayhead(controller, recording.id, { window }) { window = it }
    LaunchedEffect(state.plan, state.skipEnabled) { controller.setSkip(state.plan, state.skipEnabled) }
    Card(modifier.fillMaxWidth().testTag("mini_player")) {
        Column(
            Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Header(recording.title, callbacks.onClose)
            WaveformSection(controller, state, duration, WaveformViewport({ window }, { window = it }), callbacks)
            TimeRow(controller, duration, window) { window = it }
            Transport(controller, state, callbacks)
            Chips(controller, state, callbacks)
            if (state.selecting) SelectionBar(state, callbacks.selection)
            MarkerList(state.markers, controller, callbacks.markers)
        }
    }
}

/** Com zoom, a janela acompanha a reproducao. */
@Composable
private fun FollowPlayhead(controller: PlayerPort, recordingId: Long, window: () -> ViewWindow, onWindow: (ViewWindow) -> Unit) {
    LaunchedEffect(recordingId) {
        controller.positionMillis.collect { p ->
            val w = window()
            if (controller.isPlaying.value && w.spanMillis < w.durationMillis) onWindow(w.follow(p))
        }
    }
}

@Composable
private fun Header(title: String, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).testTag("titulo_player"),
        )
        IconButton(onClick = onClose, modifier = Modifier.testTag("fechar_player")) {
            Icon(Icons.Filled.Close, stringResource(R.string.player_close))
        }
    }
}

@Composable
private fun WaveformSection(
    controller: PlayerPort,
    state: PlayerUiState,
    duration: Long,
    viewport: WaveformViewport,
    callbacks: PlayerCallbacks,
) {
    val position by controller.positionMillis.collectAsStateWithLifecycle()
    val actions = remember(state.markers, controller, callbacks.selection) { waveformActions(controller, state.markers, callbacks) }
    val labels = WaveformLabels(
        description = stringResource(R.string.player_wave_desc),
        previousMarker = stringResource(R.string.player_wave_prev_marker),
        nextMarker = stringResource(R.string.player_wave_next_marker),
        zoomIn = stringResource(R.string.player_zoom_in),
        zoomOut = stringResource(R.string.player_zoom_out),
    )
    val data = WaveformData(
        waveform = state.waveform,
        durationMillis = duration,
        markers = state.markers,
        skipRanges = if (state.skipEnabled) state.plan?.ranges.orEmpty() else emptyList(),
        selection = state.selection,
    )
    Box(Modifier.fillMaxWidth().heightIn(min = WAVE_HEIGHT, max = WAVE_HEIGHT)) {
        WaveformView(
            data = data,
            viewport = viewport,
            positionMillis = { position },
            selecting = state.selecting,
            actions = actions,
            labels = labels,
            modifier = Modifier.fillMaxWidth().heightIn(min = WAVE_HEIGHT, max = WAVE_HEIGHT).testTag("mini_posicao"),
        )
        if (state.analyzing) {
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    stringResource(R.string.player_wave_analyzing), Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun waveformActions(controller: PlayerPort, markers: List<Marker>, callbacks: PlayerCallbacks) = WaveformActions(
    onSeek = { controller.seekTo(it) },
    onMarkerClick = { controller.seekToAndPlay(it.positionMillis) },
    onSelection = callbacks.selection.onChange,
    onPreviousMarker = { MarkerNavigation.previous(markers, controller.positionMillis.value)?.let { controller.jumpTo(it) } },
    onNextMarker = { MarkerNavigation.next(markers, controller.positionMillis.value)?.let { controller.jumpTo(it) } },
)

@Composable
private fun TimeRow(controller: PlayerPort, duration: Long, window: ViewWindow, onWindow: (ViewWindow) -> Unit) {
    // So recompõe quando o segundo muda (a posicao anda a ~25 Hz).
    val seconds by remember(controller) { controller.positionMillis.map { it / MILLIS }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(0L)
    val centre = window.startMillis + window.spanMillis / 2
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.player_time, ClipTitle.time(seconds * MILLIS), ClipTitle.time(duration)),
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).testTag("tempo_player"),
        )
        IconButton(onClick = { onWindow(window.zoom(1f / ZOOM_STEP, centre)) }, modifier = Modifier.testTag("zoom_menos")) {
            Icon(Icons.Filled.ZoomOut, stringResource(R.string.player_zoom_out))
        }
        IconButton(onClick = { onWindow(window.zoom(ZOOM_STEP, centre)) }, modifier = Modifier.testTag("zoom_mais")) {
            Icon(Icons.Filled.ZoomIn, stringResource(R.string.player_zoom_in))
        }
    }
}

@Composable
private fun Transport(controller: PlayerPort, state: PlayerUiState, cb: PlayerCallbacks) {
    val playing by controller.isPlaying.collectAsStateWithLifecycle()
    val position by controller.positionMillis.collectAsStateWithLifecycle()
    val previous = MarkerNavigation.previous(state.markers, position)
    val next = MarkerNavigation.next(state.markers, position)
    val addDescription = stringResource(R.string.player_add_marker)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        IconButton(
            onClick = { previous?.let { controller.jumpTo(it) } },
            enabled = previous != null,
            modifier = Modifier.testTag("marcador_anterior"),
        ) { Icon(Icons.Filled.SkipPrevious, stringResource(R.string.player_wave_prev_marker)) }
        FilledIconButton(
            onClick = { if (playing) controller.pause() else controller.play() },
            modifier = Modifier.size(PLAY_BUTTON).testTag("mini_play_pause"),
        ) {
            val label = stringResource(if (playing) R.string.player_pause else R.string.player_play)
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, label)
        }
        IconButton(
            onClick = { next?.let { controller.jumpTo(it) } },
            enabled = next != null,
            modifier = Modifier.testTag("marcador_proximo"),
        ) { Icon(Icons.Filled.SkipNext, stringResource(R.string.player_wave_next_marker)) }
        Box(Modifier.weight(1f))
        FilledTonalButton(
            onClick = { cb.markers.onAdd(controller.positionMillis.value) },
            modifier = Modifier.testTag("adicionar_marcador").semantics { contentDescription = addDescription },
        ) {
            Icon(Icons.Filled.Flag, null, Modifier.size(18.dp))
            Text(stringResource(R.string.player_add_marker_short), Modifier.padding(start = 6.dp))
        }
        IconToggleButton(
            checked = state.selecting,
            onCheckedChange = { cb.selection.onToggle(it, controller.positionMillis.value) },
            modifier = Modifier.testTag("selecionar_trecho"),
        ) {
            val label = stringResource(if (state.selecting) R.string.player_select_clip_off else R.string.player_select_clip)
            Icon(Icons.Filled.ContentCut, label)
        }
    }
}

@Composable
private fun Chips(controller: PlayerPort, state: PlayerUiState, cb: PlayerCallbacks) {
    var speed by remember { mutableStateOf(1f) }
    val skippedNow by controller.skippedMillis.collectAsStateWithLifecycle()
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = state.skipEnabled,
            onClick = { cb.skip.onEnabled(!state.skipEnabled) },
            label = { Text(stringResource(R.string.player_skip_silence)) },
            modifier = Modifier.testTag("pular_silencio"),
        )
        if (state.skipEnabled) {
            AssistChip(
                onClick = { cb.skip.onSensitivity(state.sensitivity.next()) },
                label = { Text(stringResource(R.string.player_sensitivity, sensitivityName(state.sensitivity))) },
                modifier = Modifier.testTag("sensibilidade_silencio"),
            )
        }
        AssistChip(
            onClick = { speed = Speeds[(Speeds.indexOf(speed) + 1) % Speeds.size].also(controller::setSpeed) },
            label = { Text(stringResource(R.string.player_speed, speed.toString().removeSuffix(".0"))) },
            modifier = Modifier.testTag("mini_velocidade"),
        )
        AssistChip(
            onClick = cb.onEditTags,
            label = { Text(stringResource(R.string.player_tags_button)) },
            modifier = Modifier.testTag("editar_etiquetas"),
        )
    }
    if (state.skipEnabled) SkipSummary(state, skippedNow)
}

@Composable
private fun SkipSummary(state: PlayerUiState, skippedNow: Long) {
    val plan = state.plan ?: return
    val text = when {
        plan.skippedMillis <= 0L -> stringResource(R.string.player_skip_none)
        skippedNow > 0L -> stringResource(R.string.player_skip_saved_now, ClipTitle.time(skippedNow))
        else -> stringResource(R.string.player_skip_saves, ClipTitle.time(plan.skippedMillis))
    }
    Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("economia_silencio"))
}

@Composable
private fun sensitivityName(s: SilenceSensitivity): String = stringResource(
    when (s) {
        SilenceSensitivity.GENTLE -> R.string.player_sens_gentle
        SilenceSensitivity.NORMAL -> R.string.player_sens_normal
    },
)

@Composable
private fun SelectionBar(state: PlayerUiState, cb: SelectionCallbacks) {
    val s = state.selection
    Column(Modifier.fillMaxWidth().testTag("barra_trecho"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (s == null || s.lengthMillis <= 0L) {
            Text(stringResource(R.string.player_clip_hint), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                stringResource(
                    R.string.player_clip_range,
                    ClipTitle.time(s.startMillis), ClipTitle.time(s.endMillis), ClipTitle.time(s.lengthMillis),
                ),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("intervalo_trecho"),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = cb.onExport,
                enabled = s != null && s.lengthMillis > 0L && !state.exporting,
                modifier = Modifier.testTag("exportar_trecho"),
            ) { Text(stringResource(if (state.exporting) R.string.player_clip_exporting else R.string.player_clip_export)) }
            OutlinedButton(onClick = { cb.onToggle(false, 0L) }, modifier = Modifier.testTag("cancelar_trecho")) {
                Text(stringResource(R.string.player_clip_cancel))
            }
        }
    }
}

@Composable
private fun MarkerList(markers: List<Marker>, controller: PlayerPort, cb: MarkerCallbacks) {
    Text(
        stringResource(R.string.player_markers_title, markers.size),
        style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("titulo_marcadores"),
    )
    if (markers.isEmpty()) {
        Text(
            stringResource(R.string.player_markers_empty),
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("sem_marcadores"),
        )
        return
    }
    markers.forEach { m -> MarkerRow(m, controller, cb) }
}

@Composable
private fun MarkerRow(m: Marker, controller: PlayerPort, cb: MarkerCallbacks) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { controller.jumpTo(m) }.testTag("marcador_item_${m.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(ClipTitle.time(m.positionMillis), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                m.label.ifBlank { stringResource(R.string.player_marker_default) },
                style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (m.note.isNotBlank()) {
                Text(m.note, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = { cb.onSelectToNext(m) }, modifier = Modifier.testTag("trecho_do_marcador_${m.id}")) {
            Icon(Icons.Filled.ContentCut, stringResource(R.string.player_marker_select))
        }
        IconButton(onClick = { cb.onEdit(m) }, modifier = Modifier.testTag("editar_marcador_${m.id}")) {
            Icon(Icons.Filled.Edit, stringResource(R.string.player_marker_edit, ClipTitle.time(m.positionMillis)))
        }
    }
}

private val WAVE_HEIGHT = 112.dp
private val PLAY_BUTTON = 56.dp

private fun PlayerPort.jumpTo(marker: Marker) = seekToAndPlay(marker.positionMillis)

private fun SilenceSensitivity.next(): SilenceSensitivity = SilenceSensitivity.entries[(ordinal + 1) % SilenceSensitivity.entries.size]
private const val MILLIS = 1000L
private const val ZOOM_STEP = 2f
private val Speeds = listOf(1f, 1.5f, 2f, 0.5f)
