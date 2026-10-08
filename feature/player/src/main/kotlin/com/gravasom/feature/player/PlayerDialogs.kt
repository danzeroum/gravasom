package com.gravasom.feature.player

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gravasom.core.domain.clip.ClipTitle
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.marker.MarkerNote
import com.gravasom.core.domain.marker.MarkerResult
import kotlinx.coroutines.launch

/**
 * Editar nota, nome e (se quiser) excluir um marcador. A nota aceita ate [MarkerNote.MAX_LENGTH] caracteres: acima disso o
 * campo mostra o erro, o contador fica vermelho e Salvar fica desativado (prevencao de erro com mensagem clara).
 */
@Composable
fun MarkerDialog(
    marker: Marker,
    onDismiss: () -> Unit,
    onSave: suspend (note: String, label: String) -> MarkerResult,
    onDelete: () -> Unit,
) {
    var note by rememberSaveable(marker.id) { mutableStateOf(marker.note) }
    var label by rememberSaveable(marker.id) { mutableStateOf(marker.label) }
    var rejected by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val tooLong = MarkerNote.length(note) > MarkerNote.MAX_LENGTH || rejected
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_marker_dialog_title, ClipTitle.time(marker.positionMillis))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label, onValueChange = { label = it.take(LABEL_MAX) }, singleLine = true,
                    label = { Text(stringResource(R.string.player_marker_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("campo_nome_marcador"),
                )
                NoteField(note, tooLong) { note = it; rejected = false }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { scope.launch { if (onSave(note, label) is MarkerResult.Saved) onDismiss() else rejected = true } },
                enabled = !tooLong,
                modifier = Modifier.testTag("salvar_marcador"),
            ) { Text(stringResource(R.string.player_marker_save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDelete(); onDismiss() }, modifier = Modifier.testTag("excluir_marcador")) {
                    Text(stringResource(R.string.player_marker_delete))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancelar_marcador")) {
                    Text(stringResource(R.string.player_marker_cancel))
                }
            }
        },
    )
}

/** Campo da nota: contador n/140 e, acima do limite, o erro em texto (nao so na cor). */
@Composable
private fun NoteField(note: String, tooLong: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = note,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.player_marker_note)) },
        isError = tooLong,
        minLines = 2,
        supportingText = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (tooLong) stringResource(R.string.player_marker_note_too_long, MarkerNote.MAX_LENGTH) else "",
                    modifier = Modifier.weight(1f).testTag("erro_nota"),
                )
                Text(
                    stringResource(R.string.player_marker_note_counter, MarkerNote.length(note), MarkerNote.MAX_LENGTH),
                    modifier = Modifier.testTag("contador_nota"),
                )
            }
        },
        modifier = Modifier.fillMaxWidth().testTag("campo_nota_marcador"),
    )
}

/** Etiquetas da gravacao: texto separado por virgula, com as ja usadas como atalho. */
@Composable
fun TagsDialog(
    currentTags: List<String>,
    knownTags: suspend () -> List<String>,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(currentTags.joinToString(", ")) }
    var known by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(Unit) { known = knownTags() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_tags_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.player_tags_hint)) },
                    modifier = Modifier.fillMaxWidth().testTag("campo_etiquetas"),
                )
                val suggestions = known.filter { it !in text.split(',').map { t -> t.trim().lowercase() } }
                if (suggestions.isNotEmpty()) {
                    Text(stringResource(R.string.player_tags_existing), style = MaterialTheme.typography.labelMedium)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        suggestions.forEach { tag ->
                            AssistChip(
                                onClick = { text = if (text.isBlank()) tag else text.trimEnd().trimEnd(',') + ", " + tag },
                                label = { Text(tag) },
                                modifier = Modifier.testTag("sugestao_etiqueta_$tag"),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text); onDismiss() }, modifier = Modifier.testTag("salvar_etiquetas")) {
                Text(stringResource(R.string.player_tags_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancelar_etiquetas")) {
                Text(stringResource(R.string.player_tags_cancel))
            }
        },
    )
}

private const val LABEL_MAX = 40
