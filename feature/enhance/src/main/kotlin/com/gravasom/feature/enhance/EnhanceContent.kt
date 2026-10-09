package com.gravasom.feature.enhance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.gravasom.core.ui.GravasomTheme
import com.gravasom.core.ui.components.formatarTempo
import com.gravasom.core.ui.theme.GravasomShapes
import com.gravasom.core.ui.theme.GravasomSizes
import com.gravasom.core.ui.theme.GravasomSpacing
import kotlin.math.roundToInt

/** Parte sem estado da tela "Melhorar som" (testavel isoladamente e previsivel para o TalkBack). */
@Composable
fun EnhanceContent(state: EnhanceUiState, callbacks: EnhanceCallbacks, modifier: Modifier = Modifier) {
    when (state.status) {
        EnhanceStatus.LOADING -> LoadingBox(modifier)
        EnhanceStatus.FAILED -> FailedBox(state, modifier)
        else -> Form(state, callbacks, modifier)
    }
}

@Composable
private fun LoadingBox(modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(GravasomSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.testTag("preparando_melhorar"))
        Text(
            stringResource(R.string.enhance_loading),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = GravasomSpacing.lg).testTag("preparando_texto"),
        )
    }
}

@Composable
private fun FailedBox(state: EnhanceUiState, modifier: Modifier) {
    val reason = state.failedReason ?: R.string.enhance_fail_decode
    Column(
        modifier.fillMaxSize().padding(GravasomSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(GravasomSizes.icone),
        )
        Text(
            stringResource(reason),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = GravasomSpacing.md).testTag("erro_melhorar")
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun Form(state: EnhanceUiState, cb: EnhanceCallbacks, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = GravasomSpacing.margemTela, vertical = GravasomSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(GravasomSpacing.lg),
    ) {
        Summary(state)
        DenoiseCard(state, cb)
        NormalizeCard(state, cb)
        PreviewCard(state, cb)
        Notice(state)
        Footer(state, cb)
    }
}

/** Titulo, duracao e o contrato da tela: o original nunca e alterado (ADR-008). */
@Composable
private fun Summary(state: EnhanceUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(GravasomSpacing.xs)) {
        Text(
            state.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth().testTag("titulo_melhorar"),
        )
        Text(
            formatarTempo(state.durationMillis),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("duracao_melhorar"),
        )
        Text(
            stringResource(R.string.enhance_original_safe),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("aviso_original"),
        )
    }
}

@Composable
private fun DenoiseCard(state: EnhanceUiState, cb: EnhanceCallbacks) {
    SectionCard {
        SwitchRow(R.string.enhance_denoise, R.string.enhance_denoise_desc, state.settings.denoise, "chave_redutor", cb.onDenoise)
        if (state.settings.denoise) {
            LabeledSlider(
                label = R.string.enhance_denoise_intensity,
                value = state.settings.denoiseIntensity,
                onValueChange = cb.onDenoiseIntensity,
                valueLabel = stringResource(R.string.enhance_intensity_percent, (state.settings.denoiseIntensity * 100).roundToInt()),
                tag = "intensidade_redutor",
            )
        }
    }
}

@Composable
private fun NormalizeCard(state: EnhanceUiState, cb: EnhanceCallbacks) {
    SectionCard {
        SwitchRow(R.string.enhance_normalize, R.string.enhance_normalize_desc, state.settings.normalize, "chave_volume", cb.onNormalize)
        if (state.settings.normalize) {
            LabeledSlider(
                label = R.string.enhance_target,
                value = state.settings.targetLufs,
                onValueChange = cb.onTargetLufs,
                valueLabel = stringResource(R.string.enhance_target_value, state.settings.targetLufs.roundToInt()),
                tag = "alvo_volume",
                range = TARGET_MIN_LUFS..TARGET_MAX_LUFS,
                steps = LUFS_STEPS,
            )
        }
    }
}

/** Previa A/B: a MESMA janela do original (A) e do melhorado (B), para alternar sem corte. */
@Composable
private fun PreviewCard(state: EnhanceUiState, cb: EnhanceCallbacks) {
    SectionCard {
        Text(stringResource(R.string.enhance_preview_section), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.enhance_preview_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WindowSlider(state, cb)
        Text(
            stringResource(R.string.enhance_preview_window, windowLabel(state)),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("janela_previa"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(GravasomSpacing.sm)) {
            SideChip(EnhanceSide.A, state, cb, Modifier.weight(1f))
            SideChip(EnhanceSide.B, state, cb, Modifier.weight(1f))
        }
        if (state.previewBusy) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GravasomSpacing.sm)) {
                CircularProgressIndicator(modifier = Modifier.size(GravasomSizes.iconePequeno).testTag("gerando_previa"))
                Text(stringResource(R.string.enhance_preview_busy), style = MaterialTheme.typography.bodySmall)
            }
        }
        state.previewing?.let {
            Text(
                stringResource(if (it == EnhanceSide.A) R.string.enhance_playing_a else R.string.enhance_playing_b),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("tocando_previa").semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun WindowSlider(state: EnhanceUiState, cb: EnhanceCallbacks) {
    val maxStart = maxOf(0L, state.durationMillis - state.previewMillis)
    if (maxStart <= 0L) return
    Slider(
        value = state.previewStartMillis.toFloat(),
        onValueChange = { cb.onPreviewStart(it.toLong()) },
        valueRange = 0f..maxStart.toFloat(),
        modifier = Modifier.fillMaxWidth().testTag("trecho_previa"),
    )
}

@Composable
private fun SideChip(side: EnhanceSide, state: EnhanceUiState, cb: EnhanceCallbacks, modifier: Modifier) {
    FilterChip(
        selected = state.previewing == side,
        onClick = { cb.onPreview(side) },
        enabled = !state.previewBusy,
        label = {
            Text(
                stringResource(if (side == EnhanceSide.A) R.string.enhance_side_a else R.string.enhance_side_b),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier = modifier.heightIn(min = GravasomSizes.alvoToqueMin)
            .testTag(if (side == EnhanceSide.A) "botao_previa_a" else "botao_previa_b"),
    )
}

@Composable
private fun Notice(state: EnhanceUiState) {
    val res = state.notice ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GravasomSpacing.sm),
        modifier = Modifier.fillMaxWidth().testTag("aviso_melhorar"),
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(res), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Footer(state: EnhanceUiState, cb: EnhanceCallbacks) {
    when (state.status) {
        EnhanceStatus.EXPORTING -> ExportingBox(state, cb)
        EnhanceStatus.SAVED -> SavedBox(state, cb)
        else -> SaveButton(state, cb)
    }
}

@Composable
private fun SaveButton(state: EnhanceUiState, cb: EnhanceCallbacks) {
    Button(
        onClick = cb.onExport,
        enabled = !state.settings.isIdentity,
        modifier = Modifier.fillMaxWidth().heightIn(min = GravasomSizes.alvoToqueMin).testTag("botao_salvar_copia"),
    ) {
        Text(stringResource(R.string.enhance_save), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ExportingBox(state: EnhanceUiState, cb: EnhanceCallbacks) {
    Column(verticalArrangement = Arrangement.spacedBy(GravasomSpacing.sm), modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.enhance_saving),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.testTag("salvando_copia"),
        )
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth().heightIn(min = GravasomSizes.iconePequeno).testTag("progresso_melhorar"),
        )
        Text(
            stringResource(R.string.enhance_progress, (state.progress * 100).roundToInt()),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("percentual_melhorar"),
        )
        OutlinedButton(
            onClick = cb.onCancelExport,
            modifier = Modifier.fillMaxWidth().heightIn(min = GravasomSizes.alvoToqueMin).testTag("botao_cancelar_melhorar"),
        ) {
            Text(stringResource(R.string.enhance_cancel))
        }
    }
}

@Composable
private fun SavedBox(state: EnhanceUiState, cb: EnhanceCallbacks) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GravasomSpacing.sm),
        modifier = Modifier.fillMaxWidth().testTag("copia_salva"),
    ) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = GravasomTheme.extra.pronto.base,
            modifier = Modifier.size(GravasomSizes.icone),
        )
        Text(
            stringResource(R.string.enhance_saved, state.savedTitle.orEmpty()),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("titulo_salvo"),
        )
        Text(
            stringResource(R.string.enhance_saved_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = cb.onDone,
            modifier = Modifier.fillMaxWidth().heightIn(min = GravasomSizes.alvoToqueMin).testTag("botao_concluir"),
        ) {
            Text(stringResource(R.string.enhance_done))
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = GravasomShapes.cartao, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(GravasomSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(GravasomSpacing.sm),
            content = content,
        )
    }
}

@Composable
private fun SwitchRow(title: Int, desc: Int, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun LabeledSlider(
    label: Int,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueLabel: String,
    tag: String,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
    }
}

private fun windowLabel(state: EnhanceUiState): String =
    formatarTempo(state.previewStartMillis) + " – " + formatarTempo(state.previewStartMillis + state.previewMillis)

/** Alvo de volume entre −24 e −12 LUFS, de 1 em 1 (12 pontos: 11 casas intermediarias no slider). */
private const val TARGET_MIN_LUFS = -24f
private const val TARGET_MAX_LUFS = -12f
private const val LUFS_STEPS = 11
