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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.gravasom.feature.player.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LibraryScreen(
    onOpenEnhance: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val items by viewModel.recordings.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val availableTags by viewModel.availableTags.collectAsStateWithLifecycle()
    val selectedTags by viewModel.selectedTags.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val player = rememberPlayer()
    var currentId by remember { mutableStateOf<Long?>(null) }
    var renaming by remember { mutableStateOf<Recording?>(null) }
    val actions = rememberRowActions(
        viewModel, playerViewModel, player, snackbar,
        PlayerSlot({ currentId }, { currentId = it }),
        onRename = { renaming = it },
        onEnhance = onOpenEnhance,
    )
    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SearchField(query, viewModel::setQuery)
            TagFilterRow(availableTags, selectedTags, viewModel::toggleTag)
            Box(Modifier.weight(1f)) { RecordingList(items, query, selectedTags, viewModel::clearTags, currentId, actions) }
            // A gravacao aberta e sempre lida da lista: renomear ou mudar etiquetas aparece no painel sem reabrir.
            val current = items.firstOrNull { it.id == currentId }
            if (current != null) {
                // Fechar o painel NAO pausa: o audio segue em segundo plano (PlaybackService) e pausa-se pela notificacao.
                PlayerDock(current, player, playerViewModel, onEnhance = onOpenEnhance) { currentId = null; playerViewModel.close() }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    PlayerEventsEffect(playerViewModel, snackbar)
    renaming?.let { RenameDialog(it, onDismiss = { renaming = null }, onConfirm = { t -> viewModel.rename(it, t); renaming = null }) }
}

@Composable
private fun rememberPlayer(): PlayerController {
    val context = LocalContext.current
    // Sair da aba tambem nao interrompe o audio: o ExoPlayer e o do PlaybackHub, nao deste controlador.
    val player = remember { PlayerController(context) }
    DisposableEffect(Unit) { onDispose { player.release() } }
    return player
}

/** A gravacao aberta no painel: leitura e troca, agrupadas para nao espalhar callbacks. */
private class PlayerSlot(val currentId: () -> Long?, val setCurrentId: (Long?) -> Unit)

/** Acoes de cada linha: tocar (abre o player), melhorar o som, renomear, favoritar, compartilhar, salvar em... e excluir com DESFAZER. */
@Composable
private fun rememberRowActions(
    viewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    player: PlayerController,
    snackbar: SnackbarHostState,
    slot: PlayerSlot,
    onRename: (Recording) -> Unit,
    onEnhance: (Long) -> Unit,
): RowActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf<Recording?>(null) }
    val savedOk = stringResource(R.string.library_saved_ok)
    val savedFail = stringResource(R.string.library_saved_fail)
    val playFail = stringResource(R.string.library_play_fail)
    val deletedText = stringResource(R.string.library_deleted)
    val undoText = stringResource(R.string.library_undo)
    val saveLauncher = rememberLauncherForActivityResult(CreateAudioDocument()) { uri ->
        val target = saving
        saving = null
        if (uri != null && target != null) scope.launch { snackbar.showSnackbar(if (viewModel.saveTo(target, uri)) savedOk else savedFail) }
    }
    return RowActions(
        onPlay = { r ->
            scope.launch {
                if (openAndPlay(viewModel, player, r)) {
                    slot.setCurrentId(r.id)
                    playerViewModel.open(r)
                } else {
                    snackbar.showSnackbar(playFail)
                }
            }
        },
        onEnhance = { r -> onEnhance(r.id) },
        onRename = onRename,
        onFavorite = viewModel::toggleFavorite,
        onShare = { r -> scope.launch { runCatching { viewModel.shareIntent(r) }.onSuccess(context::startActivity) } },
        onSaveAs = { r -> saving = r; saveLauncher.launch("${r.title}.${r.format.extension}" to r.format.mimeType) },
        onDelete = { r ->
            if (slot.currentId() == r.id) {
                player.pause()
                slot.setCurrentId(null)
                playerViewModel.close()
            }
            viewModel.delete(r)
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val result = snackbar.showSnackbar(deletedText, actionLabel = undoText, duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(r)
            }
        },
    )
}

/** Filtro por etiquetas: toque numa etiqueta para ver so as gravacoes que a tem (varias = todas). */
@Composable
private fun TagFilterRow(available: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    if (available.isEmpty()) return
    LazyRow(
        modifier = Modifier.fillMaxWidth().testTag("filtro_etiquetas"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(available, key = { it }) { tag ->
            FilterChip(
                selected = tag in selected,
                onClick = { onToggle(tag) },
                label = { Text(tag) },
                modifier = Modifier.testTag("filtro_etiqueta_$tag"),
            )
        }
    }
}

/** Decifra (se preciso) fora da thread principal e toca NA principal (o ExoPlayer exige); false se nao abriu. */
private suspend fun openAndPlay(viewModel: LibraryViewModel, player: PlayerController, recording: Recording): Boolean {
    val path = runCatching { viewModel.playablePath(recording) }.getOrNull() ?: return false
    withContext(Dispatchers.Main) {
        player.load(path, title = recording.title.takeUnless { recording.isPrivate })
        player.play()
        viewModel.played()
    }
    return true
}

private data class RowActions(
    val onPlay: (Recording) -> Unit,
    val onEnhance: (Recording) -> Unit,
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
private fun RecordingList(
    items: List<Recording>,
    query: String,
    selectedTags: Set<String>,
    onClearTags: () -> Unit,
    playingId: Long?,
    actions: RowActions,
) {
    if (items.isEmpty()) {
        val text = when {
            selectedTags.isNotEmpty() -> stringResource(R.string.library_no_results_tags)
            query.isBlank() -> stringResource(R.string.library_empty)
            else -> stringResource(R.string.library_no_results, query)
        }
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("lista_vazia"))
            if (selectedTags.isNotEmpty()) {
                TextButton(onClick = onClearTags, modifier = Modifier.testTag("limpar_filtro_etiquetas")) {
                    Text(stringResource(R.string.library_clear_tag_filter))
                }
            }
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
            if (r.tags.isNotEmpty()) {
                Text(
                    r.tags.joinToString("  ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("etiquetas_item"),
                )
            }
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
        MenuItem(R.string.library_action_enhance, "menu_melhorar_som") { dismiss(); a.onEnhance(r) }
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

