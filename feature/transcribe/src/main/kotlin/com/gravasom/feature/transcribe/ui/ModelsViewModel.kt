package com.gravasom.feature.transcribe.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gravasom.core.domain.transcript.ModelState
import com.gravasom.core.domain.transcript.TranscriptionLang
import com.gravasom.feature.transcribe.model.AndroidModelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/** Linha da lista de modelos: idioma + estado atual. */
data class ModelRow(val lang: TranscriptionLang, val state: ModelState)

/** Evento curto para a UI (Snackbar). */
sealed interface ModelsEvent {
    data class Importado(val lang: TranscriptionLang) : ModelsEvent
    data object HashInvalido : ModelsEvent
    data object Falhou : ModelsEvent
    data class Removido(val lang: TranscriptionLang) : ModelsEvent
}

/**
 * Ajustes -> Transcricao -> Gerenciar modelos. O app NAO usa INTERNET: o modelo chega pelo
 * seletor de arquivos do sistema (SAF), o usuario aponta o arquivo baixado e o [AndroidModelManager]
 * confere o SHA-256 antes de aceitar (ADR-016 secao 2).
 */
@HiltViewModel
class ModelsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val models: AndroidModelManager,
) : ViewModel() {

    private val _rows = MutableStateFlow<List<ModelRow>>(emptyList())
    val rows: StateFlow<List<ModelRow>> = _rows.asStateFlow()

    private val _events = MutableStateFlow<ModelsEvent?>(null)
    val events: StateFlow<ModelsEvent?> = _events.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        val lista = TranscriptionLang.entries.map { lang -> ModelRow(lang, models.state(lang)) }
        _rows.value = lista
    }

    /**
     * Copia o arquivo apontado em [uri] (SAF) para um temporario, verifica o SHA-256 e, se bate,
     * move para o lugar definitivo. Nada toca a rede.
     */
    fun importar(uri: Uri, lang: TranscriptionLang) = viewModelScope.launch {
        val ok = runCatching {
            withContext(Dispatchers.IO) {
                val temp = File(context.cacheDir, "modelo-import-${lang.tag}.bin")
                context.contentResolver.openInputStream(uri)?.use { entrada ->
                    temp.outputStream().use { saida -> entrada.copyTo(saida, 1 shl 16) }
                } ?: return@withContext false
                val aceitou = models.importar(temp, lang)
                temp.delete()
                aceitou
            }
        }.getOrElse {
            Timber.w(it, "falha ao importar modelo (lang=%s)", lang.tag)
            false
        }
        _events.value = if (ok) ModelsEvent.Importado(lang) else ModelsEvent.HashInvalido
        refresh()
    }

    fun remover(lang: TranscriptionLang) = viewModelScope.launch {
        models.remove(lang)
        _events.value = ModelsEvent.Removido(lang)
        refresh()
    }

    fun eventoConsumido() {
        _events.value = null
    }

    /**
     * Variante para o teste: importa de um [File] local (sem SAF). Usada pelos testes
     * instrumentados e pelo roteiro do dono (`adb push` + copia manual).
     */
    fun importarDeArquivo(origem: File, lang: TranscriptionLang) = viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) { models.importar(origem, lang) }
        _events.value = if (ok) ModelsEvent.Importado(lang) else ModelsEvent.HashInvalido
        refresh()
    }
}
