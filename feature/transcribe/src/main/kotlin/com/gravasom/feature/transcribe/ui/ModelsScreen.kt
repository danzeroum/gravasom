package com.gravasom.feature.transcribe.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gravasom.core.domain.transcript.ModelState
import com.gravasom.core.domain.transcript.TranscriptionLang
import com.gravasom.feature.transcribe.R
import kotlinx.coroutines.launch

/**
 * Ajustes -> Transcricao -> Gerenciar modelos. Sem INTERNET: o usuario aponta o arquivo do
 * modelo (baixado por fora, ex.: navegador) pelo seletor do sistema e o app confere o SHA-256.
 * Uma vez aceito, o modelo fica em `filesDir/modelos/<tag>/modelo.bin`.
 */
@Composable
fun ModelsScreen(
    modifier: Modifier = Modifier,
    viewModel: ModelsViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val event by viewModel.events.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Um launcher por idioma e resolvido dentro do item: o Compose exige o rememberLauncher
    // no escopo do composable; o callback recebe o idioma por closure.
    LaunchedEffect(event) {
        val e = event ?: return@LaunchedEffect
        val texto = when (e) {
            is ModelsEvent.Importado -> context.getString(R.string.models_importado)
            is ModelsEvent.Removido -> context.getString(R.string.models_removido)
            ModelsEvent.HashInvalido -> context.getString(R.string.models_hash_invalido)
            ModelsEvent.Falhou -> context.getString(R.string.transcribe_exportar_falhou)
        }
        snackbar.showSnackbar(texto)
        viewModel.eventoConsumido()
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.models_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.models_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        rows.forEach { row -> ModelItem(row, viewModel) }
        SnackbarHost(snackbar)
    }
}

@Composable
private fun ModelItem(row: ModelRow, viewModel: ModelsViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importar(uri, row.lang)
    }
    Card(Modifier.fillMaxWidth().testTag("modelo_${row.lang.tag}")) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(row.lang.displayName, style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.models_tamanho, TAMANHO_ESTIMADO),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(estadoTexto(row.state), style = MaterialTheme.typography.labelSmall, color = corEstado(row.state))
            }
            when (row.state) {
                is ModelState.Ready -> AssistChip(
                    onClick = { viewModel.remover(row.lang) },
                    leadingIcon = { Icon(Icons.Filled.CheckCircle, null) },
                    label = { Text(stringResource(R.string.models_remover)) },
                    modifier = Modifier.testTag("remover_${row.lang.tag}"),
                )
                else -> AssistChip(
                    onClick = { launcher.launch(arrayOf("application/octet-stream", "*/*")) },
                    leadingIcon = { Icon(Icons.Filled.UploadFile, null) },
                    label = { Text(stringResource(R.string.models_baixar)) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("importar_${row.lang.tag}"),
                )
            }
        }
    }
}

@Composable
private fun estadoTexto(state: ModelState): String = when (state) {
    is ModelState.Ready -> stringResource(R.string.models_baixado)
    ModelState.Missing -> stringResource(R.string.models_ausente)
    ModelState.Corrupted -> stringResource(R.string.models_corrompido)
    is ModelState.Downloading -> stringResource(R.string.transcribe_rodando)
}

@Composable
private fun corEstado(state: ModelState) = when (state) {
    is ModelState.Ready -> MaterialTheme.colorScheme.primary
    ModelState.Corrupted -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private const val TAMANHO_ESTIMADO = "57 MB"
