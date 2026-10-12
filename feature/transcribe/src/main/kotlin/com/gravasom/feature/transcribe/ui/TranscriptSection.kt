package com.gravasom.feature.transcribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gravasom.core.domain.transcript.TranscriptStatus
import com.gravasom.core.domain.transcript.TranscriptionLang

/**
 * Aba "Transcricao" embutida no detalhe/player. Mostra o texto (PLAIN), o progresso, o erro com
 * "Tentar novamente", e o aviso de modelo ausente. A copy reforca a promessa: tudo local.
 */
@Composable
fun TranscriptSection(
    recordingId: Long,
    language: TranscriptionLang,
    modifier: Modifier = Modifier,
    viewModel: TranscriptViewModel = hiltViewModel(),
) {
    viewModel.bind(recordingId, language)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    Column(modifier.fillMaxWidth().testTag("secao_transcricao"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider()
        Text("Transcricao", style = MaterialTheme.typography.titleSmall)
        Text(
            "Feita no aparelho, sem internet",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val t = state.atual
        when {
            t == null || t.status == TranscriptStatus.CANCELLED -> Aviso("Sem transcricao ainda.") { Acao("Transcrever", viewModel::transcrever) }
            t.status == TranscriptStatus.RUNNING -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                Text("Transcrevendo...", style = MaterialTheme.typography.bodySmall)
                Acao("Cancelar", viewModel::cancelar)
            }
            t.status == TranscriptStatus.FAILED -> Aviso("Nao foi possivel transcrever: ${t.errorMessage ?: "erro"}") {
                Acao("Tentar novamente", viewModel::transcrever)
            }
            t.status == TranscriptStatus.DONE -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(t.text.orEmpty(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("texto_transcricao"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { clipboard.setText(AnnotatedString(t.text.orEmpty())) },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                        label = { Text("Copiar tudo") },
                        modifier = Modifier.testTag("copiar_transcricao"),
                    )
                }
            }
            else -> Text("Preparando...", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Aviso(texto: String, acao: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(texto, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        acao()
    }
}

@Composable
private fun Acao(rotulo: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        leadingIcon = { Icon(Icons.Filled.Refresh, null) },
        label = { Text(rotulo) },
        modifier = Modifier.testTag("acao_transcricao_$rotulo"),
    )
}
