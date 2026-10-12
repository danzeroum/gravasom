package com.gravasom.feature.transcribe.model

import android.content.Context
import com.gravasom.core.domain.transcript.ModelManager
import com.gravasom.core.domain.transcript.ModelState
import com.gravasom.core.domain.transcript.TranscriptionLang
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerencia os modelos de idioma no sandbox do app (ADR-016 secao 2). Sem INTERNET:
 * o download e feito fora do app (navegador + SAF) e o arquivo chega em [importar].
 *
 * Layout: `filesDir/modelos/<tag>/modelo.bin`. Cada modelo e verificado por SHA-256 contra o
 * hash conhecido antes de ser aceito.
 */
@Singleton
class AndroidModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : ModelManager {

    override suspend fun state(lang: TranscriptionLang): ModelState = withContext(Dispatchers.IO) {
        val arquivo = arquivoDe(lang)
        when {
            !arquivo.exists() -> ModelState.Missing
            verificar(arquivo, lang) -> ModelState.Ready(arquivo.length())
            else -> ModelState.Corrupted
        }
    }

    override suspend fun verify(lang: TranscriptionLang): Boolean = withContext(Dispatchers.IO) {
        val arquivo = arquivoDe(lang)
        arquivo.exists() && verificar(arquivo, lang)
    }

    override suspend fun remove(lang: TranscriptionLang) = withContext(Dispatchers.IO) {
        arquivoDe(lang).delete()
        Unit
    }

    /** Caminho do modelo do [lang] (pode nao existir). O transcriber usa para carregar. */
    fun caminho(lang: TranscriptionLang): File = arquivoDe(lang)

    /**
     * Importa um arquivo de modelo ja baixado pelo usuario (via SAF). Valida o SHA-256 e, se bate,
     * move para o lugar definitivo. Devolve `true` se aceitou.
     */
    suspend fun importar(origem: File, lang: TranscriptionLang): Boolean = withContext(Dispatchers.IO) {
        if (!verificar(origem, lang)) return@withContext false
        val destino = arquivoDe(lang)
        destino.parentFile?.mkdirs()
        origem.copyTo(destino, overwrite = true)
        true
    }

    private fun arquivoDe(lang: TranscriptionLang): File =
        File(context.filesDir, "modelos/${lang.tag}/modelo.bin")

    private fun verificar(arquivo: File, lang: TranscriptionLang): Boolean {
        val esperado = HASHES[lang] ?: return false
        val digest = MessageDigest.getInstance("SHA-256")
        arquivo.inputStream().buffered(1 shl 16).use { entrada ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = entrada.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        val hex = digest.digest().joinToString("") { "%02x".format(it) }
        return hex == esperado
    }

    companion object {
        /**
         * Hash SHA-256 esperado de cada modelo. Sem hash conhecido, o modelo e recusado
         * (nunca aceita arquivo arbitrario).
         *
         * Fonte: `corpus/modelos.sha256` (ggml-base-q5_1.bin, ADR-013). Os outros idiomas usam
         * o MESMO modelo base multilingue (o whisper.cpp base fala pt/en/es/fr); o que muda e a
         * escolha do idioma na transcricao, nao o arquivo. Por isso o mapa tem so um hash.
         */
        private const val HASH_BASE_Q5_1 = "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898"

        val HASHES: Map<TranscriptionLang, String> = TranscriptionLang.entries.associateWith { HASH_BASE_Q5_1 }
    }
}
