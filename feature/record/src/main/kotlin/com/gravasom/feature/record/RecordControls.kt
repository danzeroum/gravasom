package com.gravasom.feature.record

import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gravasom.core.ui.GravasomTheme
import com.gravasom.core.ui.theme.GravasomMotion
import com.gravasom.core.ui.theme.GravasomShapeRadii
import com.gravasom.core.ui.theme.GravasomShapes
import com.gravasom.core.ui.theme.GravasomSizes
import com.gravasom.core.ui.theme.GravasomSpacing

/**
 * Pausar | botao principal | Marcar. O principal e UMA peca que muda de forma: circulo petroleo "Gravar" -> quadrado arredondado
 * vermelho "Parar" (m:morfarBotao); os laterais entram dos dois lados. Forma, icone, rotulo e cor mudam juntos.
 */
@Composable
internal fun Controls(state: RecordUiState, cb: RecordCallbacks) {
    val active = state.status == RecordStatus.Recording || state.status == RecordStatus.Paused
    val paused = state.status == RecordStatus.Paused
    val cores = GravasomTheme.extra
    Row(horizontalArrangement = Arrangement.spacedBy(GravasomSpacing.xl), verticalAlignment = Alignment.Top) {
        SideSlot(visible = active) {
            val container = if (paused) cores.pausado.container else MaterialTheme.colorScheme.secondaryContainer
            val content = if (paused) cores.pausado.onContainer else MaterialTheme.colorScheme.onSecondaryContainer
            TransportButton(
                icon = if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                label = stringResource(if (paused) R.string.record_btn_resume else R.string.record_btn_pause),
                tag = "botao_pausar", container = container, content = content, onClick = if (paused) cb.onResume else cb.onPause,
            )
        }
        MainButton(state, cb)
        SideSlot(visible = active) {
            TransportButton(
                icon = Icons.Rounded.Flag,
                label = stringResource(R.string.record_btn_marker),
                tag = "botao_marcar",
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = cb.onMarker,
                caption = state.markerCount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.record_markers, it, it) },
            )
        }
    }
}

/** Reserva a largura do botao lateral mesmo escondido, para o principal ficar sempre no centro. */
@Composable
private fun SideSlot(visible: Boolean, content: @Composable () -> Unit) {
    Box(
        Modifier.width(GravasomSizes.botaoTransporte).heightIn(min = GravasomSizes.botaoTransporte),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(GravasomMotion.morfarBotao.durationMillis)) + scaleIn(tween(GravasomMotion.morfarBotao.durationMillis)),
            exit = fadeOut(tween(GravasomMotion.faixaEstadoSai.durationMillis)),
        ) { content() }
    }
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    label: String,
    tag: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    caption: String? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            shape = GravasomShapes.botaoTransporte,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = container, contentColor = content),
            modifier = Modifier.size(GravasomSizes.botaoTransporte).testTag(tag),
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(GravasomSizes.iconeTransporte))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        if (caption != null) {
            // Sem quebra de linha: "2 marcadores" e mais largo que o botao e pode passar um pouco dos lados.
            Text(
                caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, softWrap = false, textAlign = TextAlign.Center,
                modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally, unbounded = true).testTag("${tag}_contagem"),
            )
        }
    }
}

/** Aparência do botão principal em cada situação: tudo muda junto (cor, ícone, rótulo, forma). */
private data class MainLook(val container: Color, val content: Color, val rounded: Boolean, @StringRes val label: Int, val tag: String)

@Composable
private fun mainLook(status: RecordStatus): MainLook {
    val cores = GravasomTheme.extra
    val scheme = MaterialTheme.colorScheme
    return when (status) {
        RecordStatus.Preparing ->
            MainLook(scheme.surfaceContainerHigh, scheme.primary, true, R.string.record_status_preparing, "botao_aguarde")
        RecordStatus.Saving -> MainLook(scheme.surfaceContainerHigh, scheme.primary, true, R.string.record_status_saving, "botao_aguarde")
        RecordStatus.Recording, RecordStatus.Paused ->
            MainLook(cores.gravando.container, cores.gravando.onContainer, true, R.string.record_btn_stop, "botao_parar")
        else -> MainLook(cores.pronto.container, cores.pronto.onContainer, false, R.string.record_btn_start, "botao_gravar")
    }
}

@Composable
private fun MainButton(state: RecordUiState, cb: RecordCallbacks) {
    val look = mainLook(state.status)
    val working = state.status == RecordStatus.Preparing || state.status == RecordStatus.Saving
    val active = state.status == RecordStatus.Recording || state.status == RecordStatus.Paused
    val motion = GravasomMotion.morfarBotao
    val container by animateColorAsState(look.container, tween(motion.durationMillis, easing = motion.easing), label = "container")
    val content by animateColorAsState(look.content, tween(motion.durationMillis, easing = motion.easing), label = "conteudo")
    // Circulo (raio = metade do lado) -> quadrado arredondado (raio do token botaoParar).
    val alvo = if (look.rounded) GravasomShapeRadii.botaoParar.value else GravasomSizes.botaoGravar.value / 2f
    val raio by animateFloatAsState(alvo, tween(motion.durationMillis, easing = motion.easing), label = "raio")
    val label = stringResource(look.label)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = if (active) cb.onStop else cb.onRecord,
            enabled = !working,
            shape = RoundedCornerShape(raio.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = container, contentColor = content, disabledContainerColor = container, disabledContentColor = content,
            ),
            modifier = Modifier.size(GravasomSizes.botaoGravar).testTag(look.tag),
        ) {
            MainIcon(working, active, label)
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MainIcon(working: Boolean, active: Boolean, label: String) {
    when {
        working -> CircularProgressIndicator(Modifier.size(PROGRESS_SIZE), strokeWidth = PROGRESS_STROKE)
        active -> Icon(Icons.Rounded.Stop, contentDescription = label, modifier = Modifier.size(GravasomSizes.iconeBotaoGravar))
        else -> Icon(Icons.Rounded.FiberManualRecord, contentDescription = label, modifier = Modifier.size(GravasomSizes.iconeBotaoGravar))
    }
}

private val PROGRESS_SIZE = 48.dp
private val PROGRESS_STROKE = 4.dp
