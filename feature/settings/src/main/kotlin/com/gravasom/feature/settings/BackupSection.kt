package com.gravasom.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** O que ficou esperando a senha depois do usuario escolher o arquivo (SAF). */
private sealed interface Pendente {
    data class Criar(val uri: Uri) : Pendente
    data class Restaurar(val uri: Uri) : Pendente
}

/**
 * Backup cifrado da biblioteca (Onda 2): UM pacote `.gravasom` com senha do usuario, salvo onde ele escolher (SAF), e a
 * restauracao do pacote — que nunca apaga nada, so acrescenta gravacoes de volta.
 */
@Composable
internal fun BackupSection(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val fase by viewModel.backupFase.collectAsStateWithLifecycle()
    var pendente by remember { mutableStateOf<Pendente?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    val criarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME)) { uri ->
        if (uri != null) pendente = Pendente.Criar(uri)
    }
    val restaurarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendente = Pendente.Restaurar(uri)
    }
    val avisos = BackupAvisos(
        ok = stringResource(R.string.backup_ok),
        restauradas = stringResource(R.string.backup_restored),
        senhaErrada = stringResource(R.string.backup_wrong_password),
        falhou = stringResource(R.string.backup_failed),
    )
    LaunchedEffect(viewModel) {
        viewModel.backupEventos.collect { e -> aviso = avisos.of(e) }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.settings_backup_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.settings_backup_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BackupAcoes(
            fase = fase,
            aviso = aviso,
            onCriar = { criarLauncher.launch(nomeSugerido()) },
            onRestaurar = { restaurarLauncher.launch(arrayOf(MIME, "*/*")) },
        )
    }
    pendente?.let { p ->
        PasswordDialog(
            criar = p is Pendente.Criar,
            onDismiss = { pendente = null },
            onConfirm = { senha ->
                when (p) {
                    is Pendente.Criar -> viewModel.criarBackup(p.uri, senha)
                    is Pendente.Restaurar -> viewModel.restaurarBackup(p.uri, senha)
                }
                pendente = null
            },
        )
    }
}

/** Os dois botoes (desabilitados enquanto roda), o indicador de andamento e o aviso do resultado. */
@Composable
private fun BackupAcoes(fase: BackupFase, aviso: String?, onCriar: () -> Unit, onRestaurar: () -> Unit) {
    OutlinedButton(
        onClick = onCriar,
        enabled = fase == BackupFase.PARADO,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("criar_backup"),
    ) { Text(stringResource(R.string.settings_backup_create)) }
    OutlinedButton(
        onClick = onRestaurar,
        enabled = fase == BackupFase.PARADO,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("restaurar_backup"),
    ) { Text(stringResource(R.string.settings_backup_restore)) }
    if (fase != BackupFase.PARADO) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp).testTag("backup_andamento"))
            Text(
                stringResource(
                    if (fase == BackupFase.CRIANDO) R.string.settings_backup_running else R.string.settings_backup_restoring
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    aviso?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("aviso_backup").semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** Dialogo de senha (criar e restaurar usam a mesma peça); a confirmacao exige as duas iguais. */
@Composable
private fun PasswordDialog(criar: Boolean, onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var senha by remember { mutableStateOf("") }
    var confirmacao by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (criar) R.string.settings_backup_create else R.string.settings_backup_restore
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text(stringResource(R.string.backup_password)) },
                    modifier = Modifier.fillMaxWidth().testTag("campo_senha_backup"),
                )
                OutlinedTextField(
                    value = confirmacao,
                    onValueChange = { confirmacao = it },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text(stringResource(R.string.backup_password_confirm)) },
                    isError = senha != confirmacao,
                    supportingText = if (senha != confirmacao && confirmacao.isNotEmpty()) {
                        { Text(stringResource(R.string.backup_password_mismatch)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth().testTag("campo_confirmar_senha"),
                )
                Text(
                    stringResource(R.string.backup_password_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(senha.toCharArray()) },
                enabled = senha.length >= SENHA_MINIMA && senha == confirmacao,
                modifier = Modifier.testTag("confirmar_backup"),
            ) { Text(stringResource(R.string.backup_continue)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancelar_backup")) {
                Text(stringResource(R.string.settings_backup_cancel))
            }
        },
    )
}

/** Avisos resolvidos uma vez (o coletor do LaunchedEffect nao pode chamar stringResource). */
private class BackupAvisos(
    private val ok: String,
    private val restauradas: String,
    private val senhaErrada: String,
    private val falhou: String,
) {
    fun of(e: BackupEvento): String = when (e) {
        is BackupEvento.Criado -> ok.format(e.gravacoes)
        is BackupEvento.Restauradas -> restauradas.format(e.gravacoes)
        BackupEvento.SenhaErrada -> senhaErrada
        is BackupEvento.Falhou -> falhou.format(e.detalhe ?: "")
    }
}

private fun nomeSugerido(): String =
    "gravasom-backup-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date()) + EXTENSAO


private const val MIME = "application/octet-stream"
private const val EXTENSAO = ".gravasom"
private const val SENHA_MINIMA = 6
