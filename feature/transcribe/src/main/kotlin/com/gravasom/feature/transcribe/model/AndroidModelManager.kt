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
         * Hash SHA-256 esperado de cada modelo. Preenchido quando o modelo e publicado.
         * Sem hash conhecido, o modelo e recusado (nunca aceita arquivo arbitrario).
         */
        val HASHES: Map<TranscriptionLang, String> = emptyMap()
    }
}
