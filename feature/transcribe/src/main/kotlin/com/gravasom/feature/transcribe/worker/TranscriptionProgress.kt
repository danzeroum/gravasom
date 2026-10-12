package com.gravasom.feature.transcribe.worker

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * Cache transitorio do worker de transcricao (ADR-016 secao 4).
 *
 * A cada [FLUSH_FRASES] frases, o worker escreve o texto acumulado no Room e limpa o cache.
 * Se o processo morrer no meio, o worker reinicia do ultimo flush, nao do zero.
 *
 * Desvio consciente do plano original (que pedia DataStore proto): o DataStore Preferences ja
 * esta no catalogo do repo e evita adicionar o plugin protobuf so para um contador.
 */
private val Context.progressoStore by preferencesDataStore(name = "transcricao_progresso")

class TranscriptionProgress(private val context: Context) {

    suspend fun ler(recordingId: Long, language: String): Snapshot? {
        val prefs = context.progressoStore.data.first()
        val idSalvo = prefs[KEY_ID] ?: return null
        if (idSalvo != recordingId || prefs[KEY_LANG] != language) return null
        return Snapshot(
            recordingId = idSalvo,
            language = language,
            proximaFrase = prefs[KEY_PROXIMA] ?: 0,
            textoAcumulado = prefs[KEY_TEXTO].orEmpty(),
            updatedAtMillis = prefs[KEY_ATUALIZADO] ?: 0L,
        )
    }

    suspend fun gravar(recordingId: Long, language: String, proximaFrase: Int, textoAcumulado: String) {
        context.progressoStore.edit { prefs ->
            prefs[KEY_ID] = recordingId
            prefs[KEY_LANG] = language
            prefs[KEY_PROXIMA] = proximaFrase
            prefs[KEY_TEXTO] = textoAcumulado
            prefs[KEY_ATUALIZADO] = System.currentTimeMillis()
        }
    }

    suspend fun limpar() {
        context.progressoStore.edit { it.clear() }
    }

    data class Snapshot(
        val recordingId: Long,
        val language: String,
        val proximaFrase: Int,
        val textoAcumulado: String,
        val updatedAtMillis: Long,
    )

    companion object {
        const val FLUSH_FRASES = 10
        private val KEY_ID = longPreferencesKey("recordingId")
        private val KEY_LANG = stringPreferencesKey("language")
        private val KEY_PROXIMA = intPreferencesKey("proximaFrase")
        private val KEY_TEXTO = stringPreferencesKey("textoAcumulado")
        private val KEY_ATUALIZADO = longPreferencesKey("updatedAtMillis")
    }
}
