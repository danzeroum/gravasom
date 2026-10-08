package com.gravasom.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gravasom.core.domain.audio.RecordingFormat

private val Bitrates = listOf(64, 128, 192)

@Composable
fun SettingsScreen(onOpenNumbers: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    var showLicenses by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Section(R.string.settings_section_recording)
        FormatGroup(s.format, viewModel::setFormat)
        Text(stringResource(R.string.settings_quality), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Bitrates.forEach { kbps ->
                FilterChip(
                    selected = s.bitrate == kbps,
                    onClick = { viewModel.setBitrate(kbps) },
                    label = { Text("$kbps") },
                    modifier = Modifier.testTag("qualidade_$kbps"),
                )
            }
        }
        Text(stringResource(R.string.settings_quality_note), style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Section(R.string.settings_section_privacy)
        SwitchRow(R.string.settings_private, R.string.settings_private_desc, s.privateMode, "chave_privado", viewModel::setPrivate)
        SwitchRow(
            R.string.settings_numbers_optin, R.string.settings_numbers_optin_desc,
            s.numbersOptIn, "chave_numeros", viewModel::setNumbersOptIn,
        )
        OutlinedButton(onClick = onOpenNumbers, modifier = Modifier.fillMaxWidth().testTag("abrir_numeros")) {
            Text(stringResource(R.string.settings_numbers_open))
        }
        HorizontalDivider()
        Section(R.string.settings_section_about)
        Text(stringResource(R.string.settings_no_internet), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { showLicenses = true }, modifier = Modifier.testTag("abrir_licencas")) {
            Text(stringResource(R.string.settings_licenses))
        }
    }
    if (showLicenses) LicensesDialog { showLicenses = false }
}

@Composable
private fun Section(title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun FormatGroup(selected: RecordingFormat, onSelect: (RecordingFormat) -> Unit) {
    Text(stringResource(R.string.settings_format), style = MaterialTheme.typography.titleSmall)
    Column(Modifier.selectableGroup()) {
        RecordingFormat.entries.forEach { f ->
            Row(
                Modifier.fillMaxWidth()
                    .selectable(selected == f, onClick = { onSelect(f) }, role = Role.RadioButton)
                    .padding(vertical = 4.dp).testTag("formato_${f.name}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == f, onClick = null)
                Text(stringResource(formatLabel(f)), modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

private fun formatLabel(f: RecordingFormat) = when (f) {
    RecordingFormat.M4A -> R.string.settings_format_M4A
    RecordingFormat.WAV -> R.string.settings_format_WAV
    RecordingFormat.MP3 -> R.string.settings_format_MP3
}

@Composable
private fun SwitchRow(title: Int, desc: Int, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}

/** Aviso LGPL do LAME + oferta de fonte (ADR-007), lido de assets/licencas. */
@Composable
private fun LicensesDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val text = remember { runCatching { context.assets.open("licencas/LAME-LGPL.txt").bufferedReader().readText() }.getOrDefault("") }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.settings_licenses_close)) } },
        title = { Text(stringResource(R.string.settings_licenses)) },
        text = { Text(text, modifier = Modifier.verticalScroll(rememberScrollState()).testTag("texto_licencas")) },
    )
}
