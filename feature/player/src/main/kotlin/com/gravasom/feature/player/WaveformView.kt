package com.gravasom.feature.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gravasom.core.audio.engine.analysis.SkipRange
import com.gravasom.core.audio.engine.analysis.ViewWindow
import com.gravasom.core.audio.engine.analysis.Waveform
import com.gravasom.core.domain.clip.ClipTitle
import com.gravasom.core.domain.marker.Marker
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Acoes que a onda oferece (tambem como acoes de acessibilidade: pinca nao e possivel no TalkBack). */
class WaveformActions(
    val onSeek: (Long) -> Unit,
    val onMarkerClick: (Marker) -> Unit,
    val onSelection: (Long, Long) -> Unit,
    val onPreviousMarker: () -> Unit,
    val onNextMarker: () -> Unit,
)

/** Textos da onda para leitores de tela (vem de recursos, ja no idioma do aparelho). */
class WaveformLabels(
    val description: String,
    val previousMarker: String,
    val nextMarker: String,
    val zoomIn: String,
    val zoomOut: String,
)

/** O que a onda desenha. */
class WaveformData(
    val waveform: Waveform?,
    val durationMillis: Long,
    val markers: List<Marker>,
    val skipRanges: List<SkipRange>,
    val selection: TimeSelection?,
)

/** Janela visivel (lida so na fase de desenho/layout) e como muda-la. */
class WaveformViewport(val window: () -> ViewWindow, val onWindowChange: (ViewWindow) -> Unit)

private class Palette(
    val bar: Color, val played: Color, val skip: Color, val marker: Color, val selection: Color, val handle: Color,
)

/**
 * Onda com zoom por pinca, arrasto, toque para ir ao ponto, marcadores e trecho selecionado.
 * O estado quente (janela e posicao) so e lido na fase de DESENHO: pinca e reproducao redesenham a tela sem recompor.
 */
@Composable
fun WaveformView(
    data: WaveformData,
    viewport: WaveformViewport,
    positionMillis: () -> Long,
    selecting: Boolean,
    actions: WaveformActions,
    labels: WaveformLabels,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val palette = Palette(
        bar = colors.primary, played = colors.onSurface, skip = colors.onSurface.copy(alpha = SKIP_ALPHA), marker = colors.tertiary,
        selection = colors.secondary.copy(alpha = SELECTION_ALPHA), handle = colors.secondary,
    )
    var size by remember { mutableStateOf(IntSize.Zero) }
    val currentData by rememberUpdatedState(data)
    val currentActions by rememberUpdatedState(actions)
    val reachPx = with(LocalDensity.current) { HANDLE_REACH_DP.dp.toPx() }
    val columns = remember { FloatArray(MAX_COLUMNS) }
    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .waveformSemantics(data.durationMillis, viewport, positionMillis, actions, labels)
            .waveformGestures(selecting, data.durationMillis, viewport, { currentData.selection }, reachPx) { currentActions }
            .pointerInput(selecting, data.durationMillis) {
                detectTapGestures { currentActions.onSeek(viewport.window().millisAt(it.x, this.size.width.toFloat())) }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) { drawWaveform(data, viewport.window(), positionMillis(), palette, columns) }
        MarkerTargets(data.markers, viewport.window, { size.width }, actions.onMarkerClick)
    }
}

private fun Modifier.waveformSemantics(
    durationMillis: Long,
    viewport: WaveformViewport,
    positionMillis: () -> Long,
    actions: WaveformActions,
    labels: WaveformLabels,
): Modifier = semantics {
    contentDescription = labels.description
    progressBarRangeInfo = ProgressBarRangeInfo(positionMillis().toFloat(), 0f..durationMillis.toFloat().coerceAtLeast(1f))
    setProgress { target -> actions.onSeek(target.toLong()); true }
    customActions = listOf(
        CustomAccessibilityAction(labels.previousMarker) { actions.onPreviousMarker(); true },
        CustomAccessibilityAction(labels.nextMarker) { actions.onNextMarker(); true },
        CustomAccessibilityAction(labels.zoomIn) { zoomBy(viewport, ZOOM_STEP); true },
        CustomAccessibilityAction(labels.zoomOut) { zoomBy(viewport, 1f / ZOOM_STEP); true },
    )
}

private fun zoomBy(viewport: WaveformViewport, factor: Float) {
    val w = viewport.window()
    viewport.onWindowChange(w.zoom(factor, w.startMillis + w.spanMillis / 2))
}

/** Pinca (zoom ancorado no ponto entre os dedos) e arrasto (rolagem); em modo de selecao, o arrasto escolhe o trecho. */
private fun Modifier.waveformGestures(
    selecting: Boolean,
    durationMillis: Long,
    viewport: WaveformViewport,
    selection: () -> TimeSelection?,
    reachPx: Float,
    actions: () -> WaveformActions,
): Modifier = pointerInput(selecting, durationMillis) {
    if (selecting) {
        selectionGestures(viewport.window, selection, reachPx) { a, b -> actions().onSelection(a, b) }
    } else {
        detectTransformGestures { centroid, pan, zoom, _ ->
            val w = viewport.window()
            val width = this.size.width.toFloat()
            var next = w
            if (zoom != 1f) next = next.zoom(zoom, next.millisAt(centroid.x, width))
            if (pan.x != 0f) next = next.pan((-pan.x / width * next.spanMillis).toLong())
            if (next != w) viewport.onWindowChange(next)
        }
    }
}

private enum class Grab { START, END, NEW }

/** Qual ponta do trecho esta sob o dedo (ou nenhuma: comeca um trecho novo). */
private fun grabAt(selection: TimeSelection?, window: ViewWindow, x: Float, width: Float, reachPx: Float): Grab = when {
    selection != null && abs(window.xFor(selection.startMillis, width) - x) <= reachPx -> Grab.START
    selection != null && abs(window.xFor(selection.endMillis, width) - x) <= reachPx -> Grab.END
    else -> Grab.NEW
}

/** Arrastar cria o trecho (ou move a ponta mais proxima se o toque comecou perto de uma). */
private suspend fun PointerInputScope.selectionGestures(
    window: () -> ViewWindow,
    selection: () -> TimeSelection?,
    reachPx: Float,
    onChange: (Long, Long) -> Unit,
) {
    var anchor = 0L
    detectDragGestures(
        onDragStart = { start ->
            val w = window()
            val current = selection()
            val touched = w.millisAt(start.x, size.width.toFloat())
            anchor = when (grabAt(current, w, start.x, size.width.toFloat(), reachPx)) {
                Grab.START -> current?.endMillis ?: touched
                Grab.END -> current?.startMillis ?: touched
                Grab.NEW -> touched.also { onChange(it, it) }
            }
        },
        onDrag = { change, _ ->
            change.consume()
            onChange(anchor, window().millisAt(change.position.x, size.width.toFloat()))
        },
    )
}

private fun DrawScope.drawWaveform(data: WaveformData, w: ViewWindow, position: Long, palette: Palette, columns: FloatArray) {
    drawRanges(data.skipRanges.map { it.startMillis to it.endMillis }, w, palette.skip)
    data.selection?.let { drawRanges(listOf(it.startMillis to it.endMillis), w, palette.selection) }
    if (data.waveform != null && w.spanMillis > 0) drawBars(data.waveform, w, position, palette, columns)
    drawMarkers(data.markers, w, palette.marker)
    data.selection?.let { drawHandles(it, w, palette.handle) }
    val x = w.xFor(position, size.width)
    if (x in 0f..size.width) drawLine(palette.played, Offset(x, 0f), Offset(x, size.height), PLAYHEAD_DP.dp.toPx())
}

private fun DrawScope.drawRanges(ranges: List<Pair<Long, Long>>, w: ViewWindow, color: Color) {
    ranges.forEach { (start, end) ->
        val x0 = w.xFor(start, size.width).coerceIn(0f, size.width)
        val x1 = w.xFor(end, size.width).coerceIn(0f, size.width)
        if (x1 > x0) drawRect(color, Offset(x0, 0f), Size(x1 - x0, size.height))
    }
}

private fun DrawScope.drawBars(waveform: Waveform, w: ViewWindow, position: Long, palette: Palette, columns: FloatArray) {
    val step = BAR_STEP_DP.dp.toPx()
    val n = (size.width / step).toInt().coerceIn(1, MAX_COLUMNS)
    val amps = waveform.columns(w.startMillis, w.endMillis, n, columns)
    val mid = size.height / 2f
    val playedX = w.xFor(position, size.width)
    for (i in 0 until n) {
        val x = (i + 0.5f) * size.width / n
        // Raiz quadrada: fala baixa continua visivel ao lado de picos altos.
        val h = (sqrt(amps[i].toDouble()).toFloat() * mid).coerceAtLeast(MIN_BAR_PX)
        drawLine(if (x <= playedX) palette.played else palette.bar, Offset(x, mid - h), Offset(x, mid + h), step * BAR_FILL, StrokeCap.Butt)
    }
}

private fun DrawScope.drawMarkers(markers: List<Marker>, w: ViewWindow, color: Color) {
    val head = MARKER_HEAD_DP.dp.toPx()
    markers.forEach { m ->
        val x = w.xFor(m.positionMillis, size.width)
        if (x in 0f..size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height), MARKER_STROKE_DP.dp.toPx())
            drawCircle(color, head, Offset(x, head))
        }
    }
}

private fun DrawScope.drawHandles(selection: TimeSelection, w: ViewWindow, color: Color) {
    val head = HANDLE_HEAD_DP.dp.toPx()
    listOf(selection.startMillis, selection.endMillis).forEach { t ->
        val x = w.xFor(t, size.width)
        if (x in 0f..size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height), HANDLE_STROKE_DP.dp.toPx())
            drawCircle(color, head, Offset(x, size.height - head))
        }
    }
}

/** Alvos de toque de 48 dp sobre cada marcador visivel (acessibilidade e testes): a posicao so e lida no layout. */
@Composable
private fun MarkerTargets(markers: List<Marker>, window: () -> ViewWindow, widthPx: () -> Int, onClick: (Marker) -> Unit) {
    val visible by remember(markers) {
        derivedStateOf { markers.filter { m -> window().let { w -> m.positionMillis in w.startMillis..w.endMillis } } }
    }
    val half = with(LocalDensity.current) { MARKER_TARGET_DP.dp.toPx() / 2f }
    visible.forEach { m ->
        Box(
            Modifier
                .offset { IntOffset((window().xFor(m.positionMillis, widthPx().toFloat()) - half).roundToInt(), 0) }
                .size(MARKER_TARGET_DP.dp, MARKER_TARGET_DP.dp)
                .testTag("marcador_onda_${m.id}")
                .semantics { contentDescription = markerDescription(m) }
                .clickable { onClick(m) },
        )
    }
}

private fun markerDescription(m: Marker): String =
    listOf(ClipTitle.time(m.positionMillis), m.label, m.note).filter { it.isNotBlank() }.joinToString(" · ")

private const val MAX_COLUMNS = 1_600
private const val BAR_STEP_DP = 3f
private const val BAR_FILL = 0.7f
private const val MIN_BAR_PX = 1f
private const val SKIP_ALPHA = 0.12f
private const val SELECTION_ALPHA = 0.28f
private const val MARKER_STROKE_DP = 2f
private const val MARKER_HEAD_DP = 5f
private const val MARKER_TARGET_DP = 48
private const val HANDLE_STROKE_DP = 3f
private const val HANDLE_HEAD_DP = 8f
private const val HANDLE_REACH_DP = 28f
private const val PLAYHEAD_DP = 2f
private const val ZOOM_STEP = 2f
