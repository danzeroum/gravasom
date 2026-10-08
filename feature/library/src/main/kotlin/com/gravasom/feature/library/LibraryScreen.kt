package com.gravasom.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gravasom.core.domain.model.Recording
import com.gravasom.feature.player.PlayerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(modifier: Modifier = Modifier, viewModel: LibraryViewModel = hiltViewModel()) {
    val items by viewModel.recordings.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val player = remember { PlayerController(context) }
    DisposableEffect(Unit) { onDispose { player.release() } }
    var current by remember { mutableStateOf<Recording?>(null) }
    var renaming by remember { mutableStateOf<Recording?>(null) }
    var saving by remember { mutableStateOf<Recording?>(null) }
    val savedOk = stringResource(R.string.library_saved_ok)
    val savedFail = stringResource(R.string.library_saved_fail)
    val playFail = stringResource(R.string.library_play_fail)
    val saveLauncher = rememberLauncherForActivityResult(CreateAudioDocument()) { uri ->
        val target = saving
        saving = null
        if (uri != null && target != null) scope.launch { snackbar.showSnackbar(if (viewModel.saveTo(target, uri)) savedOk else savedFail) }
    }
    val deletedText = stringResource(R.string.library_deleted)
    val undoText = stringResource(R.string.library_undo)
    val actions = RowActions(
        onPlay = { r ->
            scope.launch {
                val path = runCatching { viewModel.playablePath(r) }.getOrNull()
                if (path == null) {
                    snackbar.showSnackbar(playFail)
                } else {
                    current = r
                    player.load(path)
                    player.play()
                    viewModel.played()
                }
            }
        },
        onRename = { renaming = it },
        onFavorite = viewModel::toggleFavorite,
        onShare = { r -> scope.launch { runCatching { viewModel.shareIntent(r) }.onSuccess(context::startActivity) } },
        onSaveAs = { r -> saving = r; saveLauncher.launch("${r.title}.${r.format.extension}" to r.format.mimeType) },
        onDelete = { r ->
            if (current?.id == r.id) { player.pause(); current = null }
            viewModel.delete(r)
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val result = snackbar.showSnackbar(deletedText, actionLabel = undoText, duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(r)
            }
        },
    )
    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SearchField(query, viewModel::setQuery)
            Box(Modifier.weight(1f)) { RecordingList(items, query, current?.id, actions) }
            current?.let { MiniPlayer(it, player) }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    renaming?.let { RenameDialog(it, onDismiss = { renaming = null }, onConfirm = { t -> viewModel.rename(it, t); renaming = null }) }
}

private data class RowActions(
    val onPlay: (Recording) -> Unit,
    val onRename: (Recording) -> Unit,
    val onFavorite: (Recording) -> Unit,
    val onShare: (Recording) -> Unit,
    val onSaveAs: (Recording) -> Unit,
    val onDelete: (Recording) -> Unit,
)

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.library_search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) { Icon(Icons.Filled.Clear, stringResource(R.string.library_search_clear)) }
            }
        },
        modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("campo_busca"),
    )
}

@Composable
private fun RecordingList(items: List<Recording>, query: String, playingId: Long?, actions: RowActions) {
    if (items.isEmpty()) {
        val text = if (query.isBlank()) stringResource(R.string.library_empty) else stringResource(R.string.library_no_results, query)
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("lista_vazia"))
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().testTag("lista_gravacoes")) {
        items(items, key = { it.id }) { r ->
            RecordingRow(r, playing = r.id == playingId, actions)
            HorizontalDivider()
        }
    }
}

@Composable
private fun RecordingRow(r: Recording, playing: Boolean, actions: RowActions) {
    var menu by remember { mutableStateOf(false) }
    val playDesc = stringResource(R.string.library_play_item, r.title)
    Row(
        modifier = Modifier.fillMaxWidth().clickable { actions.onPlay(r) }
            .padding(horizontal = 16.dp, vertical = 8.dp).testTag("item_gravacao"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = playDesc)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("titulo_item"))
                if (r.isFavorite) Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                if (r.isPrivate) {
                    Icon(
                        Icons.Filled.Lock, contentDescription = stringResource(R.string.library_private_badge),
                        modifier = Modifier.padding(start = 4.dp).testTag("selo_cofre"),
                    )
                }
            }
            Text(
                stringResource(R.string.library_item_meta, formatDuration(r.durationMillis), r.format.name, formatSize(r.sizeBytes)),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("duracao_item"),
            )
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.testTag("mais_acoes")) {
                Icon(Icons.Filled.MoreVert, stringResource(R.string.library_more))
            }
            RowMenu(menu, { menu = false }, r, actions)
        }
    }
}

@Composable
private fun RowMenu(expanded: Boolean, dismiss: () -> Unit, r: Recording, a: RowActions) {
    DropdownMenu(expanded = expanded, onDismissRequest = dismiss) {
        MenuItem(R.string.library_action_share, "menu_compartilhar") { dismiss(); a.onShare(r) }
        MenuItem(R.string.library_action_save_as, "menu_salvar_em") { dismiss(); a.onSaveAs(r) }
        MenuItem(R.string.library_action_rename, "menu_renomear") { dismiss(); a.onRename(r) }
        val favLabel = if (r.isFavorite) R.string.library_action_unfavorite else R.string.library_action_favorite
        MenuItem(favLabel, "menu_favoritar") { dismiss(); a.onFavorite(r) }
        MenuItem(R.string.library_action_delete, "menu_excluir") { dismiss(); a.onDelete(r) }
    }
}

@Composable
private fun MenuItem(label: Int, tag: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = onClick, modifier = Modifier.testTag(tag))
}

@Composable
private fun MiniPlayer(recording: Recording, player: PlayerController) {
    val playing by player.isPlaying.collectAsStateWithLifecycle()
    val position by player.positionMillis.collectAsStateWithLifecycle()
    val duration by player.durationMillis.collectAsStateWithLifecycle()
    var speed by remember(recording.id) { mutableStateOf(1f) }
    LaunchedEffect(recording.id) {
        while (true) {
            player.tick()
            delay(TICK_MS)
        }
    }
    Card(Modifier.fillMaxWidth().padding(12.dp).testTag("mini_player")) {
        Row(
            Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = { if (playing) player.pause() else player.play() }, modifier = Modifier.testTag("mini_play_pause")) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    stringResource(if (playing) R.string.library_player_pause else R.string.library_player_play),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(recording.title, style = MaterialTheme.typography.titleSmall)
                val total = maxOf(duration, recording.durationMillis)
                Text("${formatDuration(position)} / ${formatDuration(total)}", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = position.coerceAtMost(total).toFloat(),
                    onValueChange = { player.seekTo(it.toLong()) },
                    valueRange = 0f..maxOf(total, 1L).toFloat(),
                    modifier = Modifier.testTag("mini_posicao"),
                )
            }
            TextButton(
                onClick = { speed = Speeds[(Speeds.indexOf(speed) + 1) % Speeds.size]; player.setSpeed(speed) },
                modifier = Modifier.testTag("mini_velocidade"),
            ) { Text(stringResource(R.string.library_player_speed, speed.toString().removeSuffix(".0"))) }
        }
    }
}

@Composable
private fun RenameDialog(recording: Recording, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(recording.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_rename_title)) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, modifier = Modifier.testTag("campo_renomear"))
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("confirmar_renomear"),
            ) { Text(stringResource(R.string.library_rename_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancelar_renomear")) {
                Text(stringResource(R.string.library_cancel))
            }
        },
    )
}

private const val TICK_MS = 250L
private val Speeds = listOf(1f, 1.5f, 2f, 0.5f)
