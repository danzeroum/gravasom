package com.gravasom.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gravasom.core.domain.transcript.TranscriptFormat
import com.gravasom.core.domain.transcript.TranscriptionLang

/**
 * Ajustes -> Transcricao. Toggle geral ("Transcrever ao gravar"), idioma padrao e formato de
 * saida. O gerenciamento de modelos fica em tela propria (Ajustes -> Gerenciar modelos).
 *
 * Tudo local: o switch e as escolhas so gravam no DataStore; nada sai do aparelho (ADR-016).
 */
@Composable
internal fun TranscriptionSettingsSection(
    habilitado: Boolean,
    lang: TranscriptionLang,
    format: TranscriptFormat,
    onHabilitado: (Boolean) -> Unit,
    onLang: (TranscriptionLang) -> Unit,
    onFormat: (TranscriptFormat) -> Unit,
    onOpenModels: () -> Unit,
) {
    Text(stringResource(R.string.settings_section_transcription), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    SwitchRowSettings(
        title = R.string.settings_transcribe_enabled,
        desc = R.string.settings_transcribe_enabled_desc,
        checked = habilitado,
        tag = "chave_transcrever",
        onChange = onHabilitado,
    )
    Text(stringResource(R.string.settings_transcribe_lang), style = MaterialTheme.typography.titleSmall)
    Column(Modifier.selectableGroup()) {
        TranscriptionLang.entries.forEach { l ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .selectable(lang == l, onClick = { onLang(l) }, role = Role.RadioButton)
                    .testTag("idioma_${l.tag}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = lang == l, onClick = null)
                Text(l.displayName, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
    Text(stringResource(R.string.settings_transcribe_format), style = MaterialTheme.typography.titleSmall)
    Column(Modifier.selectableGroup()) {
        listOf(TranscriptFormat.PLAIN, TranscriptFormat.VTT, TranscriptFormat.SRT).forEach { f ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .selectable(format == f, onClick = { onFormat(f) }, role = Role.RadioButton)
                    .testTag("formato_saida_${f.name}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = format == f, onClick = null)
                Text(stringResource(formatLabel(f)), modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
    OutlinedButton(onClick = onOpenModels, modifier = Modifier.fillMaxWidth().testTag("abrir_modelos")) {
        Text(stringResource(R.string.settings_manage_models))
    }
}

private fun formatLabel(f: TranscriptFormat) = when (f) {
    TranscriptFormat.PLAIN -> R.string.settings_transcribe_format_plain
    TranscriptFormat.VTT -> R.string.settings_transcribe_format_vtt
    TranscriptFormat.SRT -> R.string.settings_transcribe_format_srt
}

@Composable
private fun SwitchRowSettings(
    title: Int,
    desc: Int,
    checked: Boolean,
    tag: String,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}
