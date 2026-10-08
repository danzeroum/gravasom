package com.gravasom.feature.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.StrokeCap
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
import com.gravasom.core.domain.marker.Marker
import kotlin.math.abs
import kotlin.math.roundToInt

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

/**
 * Onda com zoom por pinca, arrasto, toque para ir ao ponto, marcadores e trecho selecionado.
 * O estado quente (janela e posicao) so e lido na fase de DESENHO: pinca e reproducao redesenham a tela sem recompor.
 */
@Composable
fun WaveformView(
    waveform: Waveform?,
    durationMillis: Long,
    window: () -> ViewWindow,
    onWindowChange: (ViewWindow) -> Unit,
    positionMillis: () -> Long,
    markers: List<Marker>,
    skipRanges: List<SkipRange>,
    selection: TimeSelection?,
    selecting: Boolean,
    actions: WaveformActions,
    labels: WaveformLabels,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val barColor = colors.primary
    val playedColor = colors.onSurface
    val skipColor = colors.onSurface.copy(alpha = SKIP_ALPHA)
    val markerColor = colors.tertiary
    val selectionColor = colors.secondary.copy(alpha = SELECTION_ALPHA)
    val handleColor = colors.secondary
    var size by remember { mutableStateOf(IntSize.Zero) }
    val currentWindow by rememberUpdatedState(window)
    val currentOnWindow by rememberUpdatedState(onWindowChange)
    val currentActions by rememberUpdatedState(actions)
    val currentSelection by rememberUpdatedState(selection)
    val density = LocalDensity.current
    val handleReach = with(density) { HANDLE_REACH_DP.dp.toPx() }
    val markerWidthPx = with(density) { MARKER_TARGET_DP.dp.roundToPx() }
    val columns = remember { FloatArray(MAX_COLUMNS) }

    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .semantics {
                contentDescription = labels.description
                progressBarRangeInfo = ProgressBarRangeInfo(positionMillis().toFloat(), 0f..durationMillis.toFloat().coerceAtLeast(1f))
                setProgress { target -> actions.onSeek(target.toLong()); true }
                customActions = listOf(
                    CustomAccessibilityAction(labels.previousMarker) { actions.onPreviousMarker(); true },
                    CustomAccessibilityAction(labels.nextMarker) { actions.onNextMarker(); true },
                    CustomAccessibilityAction(labels.zoomIn) { zoomBy(currentWindow(), currentOnWindow, ZOOM_STEP); true },
                    CustomAccessibilityAction(labels.zoomOut) { zoomBy(currentWindow(), currentOnWindow, 1f / ZOOM_STEP); true },
                )
            }
            .pointerInput(selecting, durationMillis) {
                if (selecting) {
                    selectionGestures(currentWindow, { currentSelection }, handleReach) { a, b -> currentActions.onSelection(a, b) }
                } else {
                    // Pinca (zoom ancorado no ponto entre os dedos) e arrasto com um dedo (rolagem).
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val w = currentWindow()
                        val width = this.size.width.toFloat()
                        var next = w
                        if (zoom != 1f) next = next.zoom(zoom, next.millisAt(centroid.x, width))
                        if (pan.x != 0f) next = next.pan((-pan.x / width * next.spanMillis).toLong())
                        if (next != w) currentOnWindow(next)
                    }
                }
            }
            .pointerInput(selecting, durationMillis) {
                detectTapGestures { offset ->
                    val millis = currentWindow().millisAt(offset.x, this.size.width.toFloat())
                    currentActions.onSeek(millis)
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = window()
            val width = this.size.width
            val height = this.size.height
            val mid = height / 2f
            val pos = positionMillis()
            // Trechos que o pular silencio vai saltar: mais apagados.
            skipRanges.forEach { r ->
                val x0 = w.xFor(r.startMillis, width).coerceIn(0f, width)
                val x1 = w.xFor(r.endMillis, width).coerceIn(0f, width)
                if (x1 > x0) drawRect(skipColor, Offset(x0, 0f), androidx.compose.ui.geometry.Size(x1 - x0, height))
            }
            selection?.let { s ->
                val x0 = w.xFor(s.startMillis, width).coerceIn(0f, width)
                val x1 = w.xFor(s.endMillis, width).coerceIn(0f, width)
                drawRect(selectionColor, Offset(x0, 0f), androidx.compose.ui.geometry.Size(x1 - x0, height))
            }
            if (waveform != null && w.spanMillis > 0) {
                val step = BAR_STEP_DP.dp.toPx()
                val n = (width / step).toInt().coerceIn(1, MAX_COLUMNS)
                val amps = waveform.columns(w.startMillis, w.endMillis, n, columns)
                val playedX = w.xFor(pos, width)
                for (i in 0 until n) {
                    val x = (i + 0.5f) * width / n
                    // Raiz quadrada: fala baixa continua visivel ao lado de picos altos.
                    val h = (kotlin.math.sqrt(amps[i].toDouble()).toFloat() * mid).coerceAtLeast(MIN_BAR_PX)
                    drawLine(if (x <= playedX) playedColor else barColor, Offset(x, mid - h), Offset(x, mid + h), step * BAR_FILL, StrokeCap.Butt)
                }
            }
            markers.forEach { m ->
                val x = w.xFor(m.positionMillis, width)
                if (x in 0f..width) {
                    drawLine(markerColor, Offset(x, 0f), Offset(x, height), MARKER_STROKE_DP.dp.toPx())
                    drawCircle(markerColor, MARKER_HEAD_DP.dp.toPx(), Offset(x, MARKER_HEAD_DP.dp.toPx()))
                }
            }
            selection?.let { s ->
                listOf(s.startMillis, s.endMillis).forEach { t ->
                    val x = w.xFor(t, width)
                    if (x in 0f..width) {
                        drawLine(handleColor, Offset(x, 0f), Offset(x, height), HANDLE_STROKE_DP.dp.toPx())
                        drawCircle(handleColor, HANDLE_HEAD_DP.dp.toPx(), Offset(x, height - HANDLE_HEAD_DP.dp.toPx()))
                    }
                }
            }
            val px = w.xFor(pos, width)
            if (px in 0f..width) drawLine(playedColor, Offset(px, 0f), Offset(px, height), PLAYHEAD_DP.dp.toPx())
        }
        // Alvos de toque de 48 dp sobre cada marcador visivel (acessibilidade e testes): a posicao so e lida no layout.
        val visible by remember(markers) {
            derivedStateOf { markers.filter { m -> window().let { w -> m.positionMillis in w.startMillis..w.endMillis } } }
        }
        visible.forEach { m ->
            Box(
                Modifier
                    .offset { IntOffset((window().xFor(m.positionMillis, size.width.toFloat()) - markerWidthPx / 2f).roundToInt(), 0) }
                    .size(MARKER_TARGET_DP.dp, MARKER_TARGET_DP.dp)
                    .testTag("marcador_onda_${m.id}")
                    .semantics { contentDescription = markerDescription(m) }
                    .clickable { actions.onMarkerClick(m) },
            )
        }
    }
}

private fun markerDescription(m: Marker): String =
    listOf(com.gravasom.core.domain.clip.ClipTitle.time(m.positionMillis), m.label, m.note).filter { it.isNotBlank() }.joinToString(" · ")

private fun zoomBy(w: ViewWindow, set: (ViewWindow) -> Unit, factor: Float) {
    set(w.zoom(factor, w.startMillis + w.spanMillis / 2))
}

/** Arrastar cria o trecho (ou move a ponta mais proxima se o toque comecou perto de uma). */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.selectionGestures(
    window: () -> ViewWindow,
    selection: () -> TimeSelection?,
    reachPx: Float,
    onChange: (Long, Long) -> Unit,
) {
    var anchor = 0L
    var moving: Int = NONE
    detectDragGestures(
        onDragStart = { start ->
            val w = window()
            val width = size.width.toFloat()
            val s = selection()
            val t = w.millisAt(start.x, width)
            moving = when {
                s != null && abs(w.xFor(s.startMillis, width) - start.x) <= reachPx -> START
                s != null && abs(w.xFor(s.endMillis, width) - start.x) <= reachPx -> END
                else -> NEW
            }
            anchor = when (moving) {
                START -> s!!.endMillis
                END -> s!!.startMillis
                else -> t
            }
            if (moving == NEW) onChange(t, t)
        },
        onDrag = { change, _ ->
            change.consume()
            onChange(anchor, window().millisAt(change.position.x, size.width.toFloat()))
        },
    )
}

private const val NONE = 0
private const val START = 1
private const val END = 2
private const val NEW = 3
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
