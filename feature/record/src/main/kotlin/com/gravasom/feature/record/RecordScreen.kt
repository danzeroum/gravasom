package com.gravasom.feature.record

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            onTranscribe = viewModel::selectTranscribe,
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
    val onTranscribe: (Boolean) -> Unit = {},
    val onOpenSettings: () -> Unit,
    val onOpenLibrary: () -> Unit = {},
)
