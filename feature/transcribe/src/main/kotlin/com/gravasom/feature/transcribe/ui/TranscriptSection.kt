package com.gravasom.feature.transcribe.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gravasom.core.domain.transcript.ModelState
import com.gravasom.core.domain.transcript.TranscriptFormat
import com.gravasom.core.domain.transcript.TranscriptStatus
import com.gravasom.core.domain.transcript.TranscriptionLang
import com.gravasom.feature.transcribe.R

/**
 * Aba "Transcricao" embutida no detalhe/player. Mostra o texto (PLAIN), o progresso, o erro com
 * "Tentar novamente" e a exportacao (.txt/.vtt/.srt). A copy reforca a promessa: tudo local.
 *
 * Se o modelo do idioma nao esta baixado, aparece o aviso com atalho para Ajustes.
 */
@Composable
fun TranscriptSection(
    recordingId: Long,
    language: TranscriptionLang,
    modifier: Modifier = Modifier,
    positionMillis: Long = 0L,
    onSeek: (Long) -> Unit = {},
    onOpenModels: () -> Unit = {},
    viewModel: TranscriptViewModel = hiltViewModel(),
) {
    viewModel.bind(recordingId, language)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exportEvent by viewModel.exportEvent.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var consent by remember { mutableStateOf(false) }

    LaunchedEffect(exportEvent) {
        val ok = exportEvent ?: return@LaunchedEffect
        snackbar.showSnackbar(context.getString(if (ok) R.string.transcribe_exportado else R.string.transcribe_exportar_falhou))
        viewModel.exportEventConsumido()
    }

    val exportarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) viewModel.exportar(uri, formatoDoNome(uri.lastPathSegment))
    }

    Column(modifier.fillMaxWidth().testTag("secao_transcricao"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider()
        Text(stringResource(R.string.transcribe_titulo), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.transcribe_offline_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val modelo = state.model
        val t = state.atual
        when {
            // Modelo ausente: bloqueia com aviso claro, nunca com transcricao vazia.
            modelo !is ModelState.Ready && t?.status != TranscriptStatus.DONE -> ModeloAusente(modelo, onOpenModels)
            t == null || t.status == TranscriptStatus.CANCELLED -> Aviso(stringResource(R.string.transcribe_vazio)) {
                Acao(stringResource(R.string.transcribe_acao), Icons.Filled.Download, viewModel::transcrever, "transcrever")
            }
            t.status == TranscriptStatus.RUNNING -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.padding(end = 8.dp).testTag("transcricao_progresso"), strokeWidth = 2.dp)
                Text(stringResource(R.string.transcribe_rodando), style = MaterialTheme.typography.bodySmall)
                Acao(stringResource(R.string.transcribe_cancelar), Icons.Filled.Refresh, viewModel::cancelar, "cancelar")
            }
            t.status == TranscriptStatus.FAILED -> Aviso(stringResource(R.string.transcribe_falhou, t.errorMessage ?: "erro")) {
                Acao(stringResource(R.string.transcribe_tentar_novamente), Icons.Filled.Refresh, viewModel::transcrever, "tentar")
            }
            t.status == TranscriptStatus.DONE -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (t.segments.isNotEmpty()) {
                    SegmentosComTempo(t.segments, positionMillis, onSeek)
                } else {
                    Text(t.text.orEmpty(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("texto_transcricao"))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { clipboard.setText(AnnotatedString(t.text.orEmpty())) },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                        label = { Text(stringResource(R.string.transcribe_copiar)) },
                        modifier = Modifier.testTag("copiar_transcricao"),
                    )
                    AssistChip(
                        onClick = { exportarLauncher.launch("transcricao.txt") },
                        leadingIcon = { Icon(Icons.Filled.Download, null) },
                        label = { Text(stringResource(R.string.transcribe_exportar_txt)) },
                        modifier = Modifier.testTag("exportar_txt"),
                    )
                    AssistChip(
                        onClick = { exportarLauncher.launch("transcricao.vtt") },
                        leadingIcon = { Icon(Icons.Filled.Download, null) },
                        label = { Text(stringResource(R.string.transcribe_exportar_vtt)) },
                        modifier = Modifier.testTag("exportar_vtt"),
                    )
                }
            }
            else -> Text(stringResource(R.string.transcribe_preparando), style = MaterialTheme.typography.bodySmall)
        }
        SnackbarHost(snackbar)
    }

    if (consent) {
        AlertDialog(
            onDismissRequest = { consent = false },
            title = { Text(stringResource(R.string.transcribe_consent_title)) },
            text = { Text(stringResource(R.string.transcribe_consent_body)) },
            confirmButton = { TextButton(onClick = { consent = false }) { Text(stringResource(R.string.transcribe_consent_ok)) } },
        )
    }
}

/** Modelo ausente: avisa e aponta para Ajustes -> Gerenciar modelos (o download e por SAF). */
@Composable
private fun ModeloAusente(modelo: ModelState, onOpenModels: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag("modelo_ausente")) {
        Text(
            stringResource(R.string.models_ausente),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AssistChip(
            onClick = onOpenModels,
            leadingIcon = { Icon(Icons.Filled.Download, null) },
            label = { Text(stringResource(R.string.models_title)) },
            modifier = Modifier.testTag("abrir_modelos"),
        )
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
private fun Acao(rotulo: String, icone: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, tag: String) {
    AssistChip(
        onClick = onClick,
        leadingIcon = { Icon(icone, null) },
        label = { Text(rotulo) },
        modifier = Modifier.testTag("acao_transcricao_$tag"),
    )
}

/**
 * Frases com tempo: a atual fica destacada e o toque pula o player para ela (player sincronizado,
 * ADR-016 secao 7). O destaque usa negrito + cor de destaque (nao so cor: funciona para daltonicos).
 */
@Composable
private fun SegmentosComTempo(
    segments: List<com.gravasom.core.domain.transcript.TranscriptSegment>,
    positionMillis: Long,
    onSeek: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.testTag("segmentos_transcricao")) {
        segments.forEach { s ->
            val atual = positionMillis in s.startMillis until s.endMillis
            Text(
                text = s.text,
                style = if (atual) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium,
                color = if (atual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().clickable { onSeek(s.startMillis) }.testTag("segmento_${s.startMillis}"),
            )
        }
    }
}

/** Deduz o formato pela extensao do nome escolhido no seletor (padrao: PLAIN). */
private fun formatoDoNome(nome: String?): TranscriptFormat = when (nome?.substringAfterLast('.', "")?.lowercase()) {
    "vtt" -> TranscriptFormat.VTT
    "srt" -> TranscriptFormat.SRT
    else -> TranscriptFormat.PLAIN
}
