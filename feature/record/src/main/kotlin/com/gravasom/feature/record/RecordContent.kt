package com.gravasom.feature.record

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.gravasom.core.audio.engine.RecordingError
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.service.InputOption
import com.gravasom.core.ui.GravasomTheme
import com.gravasom.core.ui.components.EstadoFaixa
import com.gravasom.core.ui.components.FaixaDeEstado
import com.gravasom.core.ui.components.MedidorDeNivel
import com.gravasom.core.ui.components.formatarTempo
import com.gravasom.core.ui.components.rememberRespiracao
import com.gravasom.core.ui.theme.GravasomShapes
import com.gravasom.core.ui.theme.GravasomSizes
import com.gravasom.core.ui.theme.GravasomSpacing
import com.gravasom.core.ui.theme.GravasomTextStyles

/** Parte sem estado da tela (testavel isoladamente e previsivel para o TalkBack). Desenho em docs/design/telas.md, secao 1. */
@Composable
fun RecordContent(
    state: RecordUiState,
    permissionNotice: PermissionNotice?,
    callbacks: RecordCallbacks,
    modifier: Modifier = Modifier,
) {
    val active = state.status == RecordStatus.Recording || state.status == RecordStatus.Paused
    BoxWithConstraints(modifier.fillMaxSize()) {
        // Duas pilhas: estado e tempo no alto; botao principal, formato e aviso embaixo, ao alcance do polegar (o centro do botao
        // fica a ~30% da altura util, contada de baixo). Em tela baixa ou fonte grande o conjunto rola.
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).heightIn(min = maxHeight)
                .padding(horizontal = GravasomSpacing.margemTela, vertical = GravasomSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(GravasomSpacing.lg)) {
                if (active) {
                    FaixaDeEstado(
                        estado = if (state.status == RecordStatus.Paused) EstadoFaixa.Pausada else EstadoFaixa.Gravando,
                        elapsedMillis = state.elapsedMillis,
                        modifier = Modifier.testTag("estado_gravacao"),
                    )
                }
                InputRow(state, callbacks.onInput)
                Timer(state)
                MedidorDeNivel(nivelDb = state.levelDb, ativo = state.status == RecordStatus.Recording)
                permissionNotice?.let { PermissionCard(it, callbacks.onOpenSettings) }
                (state.status as? RecordStatus.Failed)?.let { ErrorCard(it.cause) }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(GravasomSpacing.lg),
                modifier = Modifier.padding(top = GravasomSpacing.xl),
            ) {
                Controls(state, callbacks)
                if (state.justSaved && state.status == RecordStatus.Ready) SavedNotice(callbacks.onOpenLibrary)
                FormatChips(state, callbacks.onFormat)
                TranscribeToggle(state, callbacks.onTranscribe)
                Where(state.privateMode)
            }
        }
    }
}

@Composable
private fun SavedNotice(onOpenLibrary: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(GravasomSpacing.sm)) {
        Text(
            stringResource(R.string.record_status_saved),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("estado_gravacao").semantics { liveRegion = LiveRegionMode.Polite },
        )
        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.heightIn(min = GravasomSizes.alvoToqueMin).testTag("ver_gravacoes")) {
            Text(stringResource(R.string.record_see_library), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * "Transcrever ao gravar" (Onda 3, ADR-016): a promessa de offline tem de estar visivel ANTES de ligar.
 * Nao muda durante a gravacao (a escolha vale para a proxima), entao fica desabilitado quando ocupado.
 */
@Composable
private fun TranscribeToggle(state: RecordUiState, onTranscribe: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().testTag("toggle_transcrever"),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.record_transcribe), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.record_transcribe_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = state.transcribeEnabled,
            onCheckedChange = onTranscribe,
            enabled = !state.isBusy,
            modifier = Modifier.testTag("toggle_transcrever_switch"),
        )
    }
}

/** Onde a gravacao fica: com o cofre ligado, ganha o cadeado e a cor do cofre (nunca a do tema). */
@Composable
private fun Where(privateMode: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GravasomSpacing.sm)) {
        if (privateMode) {
            Icon(
                Icons.Rounded.Lock, contentDescription = null, tint = GravasomTheme.extra.cofre.base,
                modifier = Modifier.size(GravasomSizes.iconePequeno),
            )
        }
        Text(
            text = stringResource(if (privateMode) R.string.record_where_private else R.string.record_where),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FormatChips(state: RecordUiState, onFormat: (RecordingFormat) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(GravasomSpacing.xs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(GravasomSpacing.sm)) {
            RecordingFormat.entries.forEach { f ->
                val selected = state.format == f
                FilterChip(
                    selected = selected,
                    onClick = { onFormat(f) },
                    enabled = !state.isBusy,
                    label = { Text(f.name, style = MaterialTheme.typography.labelLarge) },
                    // O visto marca o escolhido: o formato nao depende so da cor do chip.
                    leadingIcon = if (selected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
                    shape = GravasomShapes.chip,
                    modifier = Modifier.heightIn(min = GravasomSizes.alvoToqueMin).testTag("chip_${f.name}"),
                )
            }
        }
        if (state.isBusy) {
            Text(
                stringResource(R.string.record_format_locked),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InputRow(state: RecordUiState, onInput: (InputOption?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = state.activeInput.label.ifBlank { stringResource(R.string.record_input_auto) }
    val text = stringResource(R.string.record_input_label, label)
    if (state.isBusy) {
        // Gravando: so informa (a entrada nao muda no meio da gravacao).
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedButton(
            onClick = { open = true },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.heightIn(min = GravasomSizes.alvoToqueMin).testTag("entrada_audio"),
        ) {
            Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(GravasomSizes.icone))
            Spacer(Modifier.width(GravasomSpacing.sm))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.record_input_auto)) }, onClick = { onInput(null); open = false })
            state.inputs.forEach { option ->
                DropdownMenuItem(text = { Text(option.label) }, onClick = { onInput(option); open = false })
            }
        }
    }
}

@Composable
private fun Timer(state: RecordUiState) {
    val text = formatElapsed(state.elapsedMillis)
    val description = stringResource(R.string.record_timer_desc, text)
    val active = state.status == RecordStatus.Recording || state.status == RecordStatus.Paused
    val respiracao = rememberRespiracao(ativa = state.status == RecordStatus.Paused)
    Text(
        text = text,
        style = GravasomTextStyles.cronometro,
        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.alpha(respiracao).testTag("cronometro").semantics { contentDescription = description },
    )
}

/** mm:ss (ou h:mm:ss a partir de 1 h). */
fun formatElapsed(ms: Long): String = formatarTempo(ms)

@Composable
private fun ErrorCard(cause: RecordingError) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        shape = GravasomShapes.cartao,
        modifier = Modifier.fillMaxWidth().testTag("estado_gravacao").semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(Modifier.padding(GravasomSpacing.lg), horizontalArrangement = Arrangement.spacedBy(GravasomSpacing.md)) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(GravasomSizes.icone))
            Text(stringResource(R.string.record_status_error, errorText(cause)), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun errorText(cause: RecordingError): String = stringResource(
    when (cause) {
        RecordingError.MIC_IN_USE -> R.string.record_err_MIC_IN_USE
        RecordingError.NO_SPACE -> R.string.record_err_NO_SPACE
        RecordingError.PERMISSION_DENIED -> R.string.record_err_PERMISSION_DENIED
        RecordingError.ENCODER_FAILURE -> R.string.record_err_ENCODER_FAILURE
        RecordingError.UNKNOWN -> R.string.record_err_UNKNOWN
    },
)

@Composable
private fun PermissionCard(notice: PermissionNotice, onOpenSettings: () -> Unit) {
    val permanent = notice == PermissionNotice.Permanent
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        shape = GravasomShapes.cartao,
        modifier = Modifier.fillMaxWidth().testTag("aviso_permissao"),
    ) {
        Column(Modifier.padding(GravasomSpacing.lg), verticalArrangement = Arrangement.spacedBy(GravasomSpacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(GravasomSpacing.md)) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(GravasomSizes.icone))
                Text(
                    stringResource(if (permanent) R.string.record_perm_permanent_title else R.string.record_perm_denied_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                stringResource(if (permanent) R.string.record_perm_permanent_body else R.string.record_perm_denied_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (permanent) {
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.heightIn(min = GravasomSizes.alvoToqueMin).testTag("abrir_configuracoes"),
                ) {
                    Text(stringResource(R.string.record_perm_open_settings), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
