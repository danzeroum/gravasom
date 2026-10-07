package com.gravasom.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Tela "Seus numeros" (funciona offline). Parte de cima: derivada da propria biblioteca. Parte de baixo: contadores de
 * eventos, SO com opt-in. Exportacao manual via "Salvar em..." (SAF).
 */
@Composable
fun NumbersScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val n by viewModel.numbers.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var median by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { viewModel.numbersOpened() }
    LaunchedEffect(n.started, n.optIn) { median = if (n.optIn) viewModel.medianTimeToRecord() else null }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) scope.launch {
            val csv = viewModel.exportCsv()
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) } }
        }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val none = stringResource(R.string.numbers_none)
        Stat(R.string.numbers_recordings, n.library.recordings.toString(), "num_gravacoes")
        Stat(R.string.numbers_total_time, formatTotal(n.library.totalDurationMillis), "num_tempo_total")
        Stat(R.string.numbers_top_format, n.library.mostUsedFormat?.name ?: none, "num_formato")
        Text(
            stringResource(R.string.numbers_usage_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (n.optIn) {
            Stat(R.string.numbers_started, n.started.toString(), "num_iniciadas")
            Stat(R.string.numbers_shared, n.shared.toString(), "num_compartilhadas")
            Stat(R.string.numbers_searches, n.searches.toString(), "num_buscas")
            Stat(R.string.numbers_imported, n.imported.toString(), "num_importadas")
            Stat(R.string.numbers_time_to_record, median?.let { stringResource(R.string.numbers_ms, it) } ?: none, "num_tempo_gravar")
            Button(onClick = { export.launch("gravasom-numeros.csv") }, modifier = Modifier.fillMaxWidth().testTag("exportar_numeros")) {
                Text(stringResource(R.string.numbers_export))
            }
            OutlinedButton(onClick = viewModel::clearNumbers, modifier = Modifier.fillMaxWidth().testTag("apagar_numeros")) {
                Text(stringResource(R.string.numbers_clear))
            }
        } else {
            Text(stringResource(R.string.numbers_usage_off), modifier = Modifier.testTag("registro_desligado"))
        }
        Text(stringResource(R.string.numbers_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Stat(label: Int, value: String, tag: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag(tag))
    }
}

private fun formatTotal(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60)
}
