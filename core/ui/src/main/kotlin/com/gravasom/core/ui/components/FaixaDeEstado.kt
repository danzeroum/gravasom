@file:Suppress("MatchingDeclarationName") // o arquivo leva o nome do componente, não o do enum auxiliar

package com.gravasom.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import com.gravasom.core.ui.GravasomTheme
import com.gravasom.core.ui.R
import com.gravasom.core.ui.theme.GravasomMotion
import com.gravasom.core.ui.theme.GravasomShapes
import com.gravasom.core.ui.theme.GravasomSizes
import com.gravasom.core.ui.theme.GravasomSpacing
import com.gravasom.core.ui.theme.GravasomTextStyles

/** Situação mostrada na faixa. */
enum class EstadoFaixa { Gravando, Pausada }

/**
 * Faixa de estado (telas.md §0): aparece em todas as abas enquanto existe gravação. Gravando é uma faixa ESCURA com ponto cheio,
 * a palavra GRAVANDO e o tempo correndo; pausada é uma faixa CLARA com barras e a palavra PAUSADA. Cor, luminância, ícone, texto
 * e movimento mudam juntos, então o estado não depende de enxergar o vermelho.
 */
@Composable
fun FaixaDeEstado(estado: EstadoFaixa, elapsedMillis: Long, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val cores = GravasomTheme.extra.let { if (estado == EstadoFaixa.Gravando) it.gravando else it.pausado }
    val reduzir = rememberReduzirMovimento()
    val tempo = formatarTempo(elapsedMillis)
    val descricao = stringResource(
        if (estado == EstadoFaixa.Gravando) R.string.ui_strip_recording_desc else R.string.ui_strip_paused_desc,
        tempoFalado(elapsedMillis),
    )
    val respiracao = rememberRespiracao(ativa = estado == EstadoFaixa.Pausada)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(GravasomSizes.faixaEstadoAltura)
            .clip(GravasomShapes.faixaEstado)
            .background(cores.container)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = GravasomSpacing.lg)
            .clearAndSetSemantics {
                contentDescription = descricao
                liveRegion = LiveRegionMode.Polite
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (estado == EstadoFaixa.Gravando) {
            PontoGravando(cores.onContainer, animar = !reduzir)
        } else {
            Icon(
                Icons.Rounded.Pause, contentDescription = null, tint = cores.onContainer,
                modifier = Modifier.size(GravasomSizes.iconePequeno),
            )
        }
        Spacer(Modifier.width(GravasomSpacing.sm))
        Text(
            text = stringResource(if (estado == EstadoFaixa.Gravando) R.string.ui_strip_recording else R.string.ui_strip_paused)
                .uppercase(),
            style = GravasomTextStyles.faixaEstado,
            color = cores.onContainer,
            modifier = Modifier.weight(1f),
        )
        Text(text = tempo, style = GravasomTextStyles.tempo, color = cores.onContainer, modifier = Modifier.alpha(respiracao))
    }
}

/**
 * Opacidade do cronômetro parado (m:piscarPausado): "respira" entre 100% e 45% a 1 ciclo por segundo e nunca some. Sem animação
 * (escala do sistema 0) ou com [ativa] falso, fica em 1.
 */
@Composable
fun rememberRespiracao(ativa: Boolean): Float {
    val reduzir = rememberReduzirMovimento()
    val alfa by rememberInfiniteTransition(label = "respiracao").animateFloat(
        initialValue = 1f,
        targetValue = if (reduzir || !ativa) 1f else GravasomMotion.piscarPausadoAlfaAte,
        animationSpec = infiniteRepeatable(
            tween(GravasomMotion.piscarPausado.durationMillis / 2, easing = LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "alfa",
    )
    return alfa
}

/** O ponto ● parado e opaco, com um anel que sai dele e some (m:pulsoGravando). */
@Composable
private fun PontoGravando(cor: Color, animar: Boolean) {
    val transicao = rememberInfiniteTransition(label = "pulso")
    val pulso = infiniteRepeatable<Float>(
        tween(GravasomMotion.pulsoGravando.durationMillis, easing = GravasomMotion.pulsoGravando.easing),
        RepeatMode.Restart,
    )
    val escala by transicao.animateFloat(
        initialValue = GravasomMotion.pulsoGravandoEscalaDe,
        targetValue = if (animar) GravasomMotion.pulsoGravandoEscalaAte else GravasomMotion.pulsoGravandoEscalaDe,
        animationSpec = pulso,
        label = "escala",
    )
    val alfa by transicao.animateFloat(
        initialValue = GravasomMotion.pulsoGravandoAlfaDe,
        targetValue = if (animar) GravasomMotion.pulsoGravandoAlfaAte else GravasomMotion.pulsoGravandoAlfaDe,
        animationSpec = pulso,
        label = "alfa",
    )
    Box(Modifier.size(GravasomSizes.icone), contentAlignment = Alignment.Center) {
        if (animar) {
            Box(Modifier.size(GravasomSizes.pontoGravando).scale(escala).alpha(alfa).clip(CircleShape).background(cor))
        }
        Icon(Icons.Rounded.FiberManualRecord, contentDescription = null, tint = cor, modifier = Modifier.size(GravasomSizes.pontoGravando))
    }
}
