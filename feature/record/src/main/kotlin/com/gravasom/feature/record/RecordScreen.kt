package com.gravasom.feature.record

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gravasom.core.audio.engine.RecordingError
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.service.InputOption

private enum class PermissionUi { Unknown, Denied, Permanent }

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun hasMic(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/**
 * Tela Gravar (etapas Abrir e Gravar). Permissoes sao pedidas AQUI, no 1o toque em Gravar (em contexto).
 * [autoStart] vem do atalho "Gravar agora"; [onAutoStartHandled] avisa que ja foi consumido.
 */
@Composable
fun RecordScreen(
    modifier: Modifier = Modifier,
    autoStart: Boolean = false,
    onAutoStartHandled: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    viewModel: RecordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permission by rememberSaveable { mutableStateOf(PermissionUi.Unknown) }

    var storageAsked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        // A permissao de armazenamento (so ate API 28) e opcional: negada, a gravacao fica so no app.
        if (result[Manifest.permission.RECORD_AUDIO] == true || hasMic(context)) {
            permission = PermissionUi.Unknown
            viewModel.requestStart()
        } else {
            viewModel.onPermissionDenied()
            val rationale = context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == true
            permission = if (rationale) PermissionUi.Denied else PermissionUi.Permanent
        }
    }
    val onRecord = {
        val ask = permissionsBeforeRecording(context, storageAsked, permission == PermissionUi.Permanent)
        if (ask == null) {
            viewModel.requestStart()
        } else if (ask.isNotEmpty()) {
            storageAsked = true
            launcher.launch(ask)
        }
    }
    LaunchedEffect(autoStart) {
        if (autoStart) {
            onRecord()
            onAutoStartHandled()
        }
    }
    state.lowSpace?.let { warning ->
        LowSpaceDialog(warning, onConfirm = viewModel::confirmLowSpace, onDismiss = viewModel::dismissLowSpace)
    }
    RecordContent(
        state = state,
        permissionNotice = permission.toNotice(),
        callbacks = RecordCallbacks(
            onRecord = onRecord, onPause = viewModel::pause, onResume = viewModel::resume, onStop = viewModel::stop,
            onMarker = viewModel::addMarker,
            onFormat = viewModel::selectFormat, onInput = viewModel::selectInput,
            onOpenSettings = { openAppSettings(context) }, onOpenLibrary = onOpenLibrary,
        ),
        modifier = modifier,
    )
}

/** Ate a API 28 a copia publica em Music/Recordings exige WRITE_EXTERNAL_STORAGE (no manifesto so ate a API 28). */
private fun needsStoragePermission(context: Context): Boolean =
    Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

/**
 * O que pedir antes de gravar: null = nada (pode gravar); vazio = nao ha o que pedir (negada de vez); senao a lista.
 * Microfone (e notificacoes na API 33+) no 1o toque; so o armazenamento (API <= 28) se o microfone ja existe.
 */
private fun permissionsBeforeRecording(context: Context, storageAsked: Boolean, micPermanentlyDenied: Boolean): Array<String>? {
    val storageNeeded = needsStoragePermission(context)
    return when {
        hasMic(context) && !(storageNeeded && !storageAsked) -> null
        hasMic(context) -> arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        micPermanentlyDenied -> emptyArray()
        else -> buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            if (storageNeeded) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }.toTypedArray()
    }
}

private fun PermissionUi.toNotice(): PermissionNotice? = when (this) {
    PermissionUi.Unknown -> null
    PermissionUi.Denied -> PermissionNotice.Denied
    PermissionUi.Permanent -> PermissionNotice.Permanent
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

/** Prevencao de erro (Nielsen 5): avisa ANTES de gravar quanto tempo cabe, sem impedir. */
@Composable
private fun LowSpaceDialog(warning: LowSpaceWarning, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.record_low_space_title)) },
        text = {
            Text(
                stringResource(R.string.record_low_space_body, warning.freeMegabytes, warning.estimatedMinutes),
                modifier = Modifier.testTag("aviso_espaco"),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("gravar_mesmo_assim")) {
                Text(stringResource(R.string.record_low_space_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancelar_espaco")) {
                Text(stringResource(R.string.record_low_space_cancel))
            }
        },
    )
}

enum class PermissionNotice { Denied, Permanent }

data class RecordCallbacks(
    val onRecord: () -> Unit,
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onStop: () -> Unit,
    val onMarker: () -> Unit = {},
    val onFormat: (RecordingFormat) -> Unit,
    val onInput: (InputOption?) -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenLibrary: () -> Unit = {},
)

/** Parte sem estado da tela (testavel isoladamente e previsivel para o TalkBack). */
@Composable
fun RecordContent(
    state: RecordUiState,
    permissionNotice: PermissionNotice?,
    callbacks: RecordCallbacks,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormatChips(state, callbacks.onFormat)
        InputRow(state, callbacks.onInput)
        Timer(state.elapsedMillis)
        StatusCard(state)
        if (state.status == RecordStatus.Recording || state.status == RecordStatus.Paused) LevelBar(state.levelDb)
        permissionNotice?.let { PermissionCard(it, callbacks.onOpenSettings) }
        Controls(state, callbacks)
        if (state.justSaved && state.status == RecordStatus.Ready) {
            OutlinedButton(onClick = callbacks.onOpenLibrary, modifier = Modifier.testTag("ver_gravacoes")) {
                Text(stringResource(R.string.record_see_library))
            }
        }
        Text(
            text = stringResource(if (state.privateMode) R.string.record_where_private else R.string.record_where),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FormatChips(state: RecordUiState, onFormat: (RecordingFormat) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecordingFormat.entries.forEach { f ->
                FilterChip(
                    selected = state.format == f,
                    onClick = { onFormat(f) },
                    enabled = !state.isBusy,
                    label = { Text(f.name) },
                    modifier = Modifier.testTag("chip_${f.name}"),
                )
            }
        }
        if (state.isBusy) {
            Text(stringResource(R.string.record_format_locked), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun InputRow(state: RecordUiState, onInput: (InputOption?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = state.activeInput.label.ifBlank { stringResource(R.string.record_input_auto) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedButton(onClick = { open = true }, enabled = !state.isBusy, modifier = Modifier.testTag("entrada_audio")) {
            Text(stringResource(R.string.record_input_label, label))
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
private fun Timer(elapsedMillis: Long) {
    val text = formatElapsed(elapsedMillis)
    val description = stringResource(R.string.record_timer_desc, text)
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge,
        modifier = Modifier.testTag("cronometro").semantics { contentDescription = description },
    )
}

/** mm:ss (ou h:mm:ss a partir de 1 h). */
fun formatElapsed(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun StatusCard(state: RecordUiState) {
    val text = when (val s = state.status) {
        RecordStatus.Ready -> stringResource(if (state.justSaved) R.string.record_status_saved else R.string.record_status_ready)
        RecordStatus.Preparing -> stringResource(R.string.record_status_preparing)
        RecordStatus.Recording -> stringResource(R.string.record_status_recording)
        RecordStatus.Paused -> stringResource(R.string.record_status_paused)
        RecordStatus.Saving -> stringResource(R.string.record_status_saving)
        is RecordStatus.Failed -> stringResource(R.string.record_status_error, errorText(s.cause))
    }
    val failed = state.status is RecordStatus.Failed
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (failed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth().testTag("estado_gravacao").semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
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
private fun LevelBar(levelDb: Float) {
    val desc = stringResource(R.string.record_level_desc)
    LinearProgressIndicator(
        progress = { ((levelDb + 60f) / 60f).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).semantics { contentDescription = desc },
    )
}

@Composable
private fun PermissionCard(notice: PermissionNotice, onOpenSettings: () -> Unit) {
    val permanent = notice == PermissionNotice.Permanent
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().testTag("aviso_permissao"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(if (permanent) R.string.record_perm_permanent_title else R.string.record_perm_denied_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(if (permanent) R.string.record_perm_permanent_body else R.string.record_perm_denied_body))
            if (permanent) {
                Button(onClick = onOpenSettings, modifier = Modifier.testTag("abrir_configuracoes")) {
                    Text(stringResource(R.string.record_perm_open_settings))
                }
            }
        }
    }
}

@Composable
private fun Controls(state: RecordUiState, cb: RecordCallbacks) {
    when (state.status) {
        RecordStatus.Recording, RecordStatus.Paused -> Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val paused = state.status == RecordStatus.Paused
            LabeledRoundButton(
                icon = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                label = stringResource(if (paused) R.string.record_btn_resume else R.string.record_btn_pause),
                tag = "botao_pausar", onClick = if (paused) cb.onResume else cb.onPause,
            )
            LabeledRoundButton(
                Icons.Filled.Flag, stringResource(R.string.record_btn_marker), "botao_marcar", cb.onMarker,
                caption = if (state.markerCount > 0) pluralStringResource(R.plurals.record_markers, state.markerCount, state.markerCount) else null,
            )
            LabeledRoundButton(Icons.Filled.Stop, stringResource(R.string.record_btn_stop), "botao_parar", cb.onStop)
        }
        RecordStatus.Preparing, RecordStatus.Saving -> Unit
        else -> LabeledRoundButton(Icons.Filled.Mic, stringResource(R.string.record_btn_start), "botao_gravar", cb.onRecord, big = true)
    }
}

@Composable
private fun LabeledRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit,
    big: Boolean = false,
    caption: String? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(onClick = onClick, modifier = Modifier.size(if (big) 96.dp else 72.dp).testTag(tag)) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(if (big) 48.dp else 32.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("${tag}_contagem"))
        }
    }
}
