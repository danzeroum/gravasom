package com.gravasom

import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.model.Recording
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

typealias RegraCompose = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

/** Atalhos dos testes de UI: acesso aos servicos do app, biblioteca limpa e o fluxo "gravar pela tela". */
object UiSuporte {

    /** Permissoes que um usuario ja teria concedido: microfone, notificacoes (API 33+) e armazenamento (ate a API 28). */
    fun permissoes(): androidx.test.rule.GrantPermissionRule {
        val lista = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) lista += android.Manifest.permission.POST_NOTIFICATIONS
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) lista += android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        return androidx.test.rule.GrantPermissionRule.grant(*lista.toTypedArray())
    }

    /** Texto de recurso no idioma do aparelho (o emulador de CI roda em ingles). */
    fun rotulo(nome: String): String {
        val ctx = Suporte.contexto
        return ctx.getString(ctx.resources.getIdentifier(nome, "string", ctx.packageName))
    }

    val app: GravasomApplication get() = Suporte.contexto.applicationContext as GravasomApplication

    /** Esvazia biblioteca (arquivos + copia publicada + linhas) e eventos de uso: cada teste parte do zero. */
    fun limparTudo() {
        runBlocking {
            (app.repo.observeAll().first() + app.repo.observeTrash().first()).forEach { app.files.deleteForever(it) }
            app.usageMetrics.clear()
            app.preferences.setFormat(RecordingFormat.M4A)
            app.preferences.setPrivateVault(false)
            app.preferences.setTelemetryOptIn(false)
        }
    }

    fun textos(rule: SemanticsNodeInteractionsProvider, tag: String): List<String> =
        rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().map { n ->
            n.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()
        }

    /** False tambem quando nao ha hierarquia Compose ativa (ex.: dialogo do sistema por cima do app). */
    fun existe(rule: SemanticsNodeInteractionsProvider, tag: String): Boolean =
        runCatching { rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)

    /** Espera a condicao; no estouro, anexa a arvore de semantica ao erro para o log de CI mostrar o que havia na tela. */
    fun aguardar(rule: RegraCompose, timeoutMs: Long = 15_000, condicao: () -> Boolean) {
        try {
            rule.waitUntil(timeoutMs) { condicao() }
        } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
            val arvore = runCatching { rule.onRoot(useUnmergedTree = true).printToString(maxDepth = 12) }.getOrDefault("(sem hierarquia)")
            throw AssertionError("${e.message}\nArvore:\n${arvore.take(3_000)}", e)
        }
    }

    /** Maior duracao possivel da ultima gravacao pela tela: do toque em Gravar ate o retorno do toque em Parar. */
    @Volatile var ultimaJanelaMaxMs: Long = 0L

    /** Toca em Gravar, espera [segundos] reais e toca em Parar. Retorna (ms ate o estado Gravando, ms gravados medidos pelo relogio). */
    fun gravarPelaTela(rule: RegraCompose, segundos: Int): Pair<Long, Long> {
        val toque = SystemClock.elapsedRealtime()
        rule.onNodeWithTag("botao_gravar").performClick()
        aguardar(rule, 10_000) { existe(rule, "botao_parar") }
        val ateGravar = SystemClock.elapsedRealtime() - toque
        val inicio = SystemClock.elapsedRealtime()
        Thread.sleep(segundos * 1000L)
        val gravado = SystemClock.elapsedRealtime() - inicio
        rule.onNodeWithTag("botao_parar").performClick()
        ultimaJanelaMaxMs = SystemClock.elapsedRealtime() - toque
        aguardar(rule, 15_000) { existe(rule, "botao_gravar") }
        return ateGravar to gravado
    }

    fun segundosDe(texto: String): Long {
        val m = Regex("(\\d+):(\\d{2})(?::(\\d{2}))?").find(texto) ?: error("sem duracao em '$texto'")
        val (a, b, c) = m.destructured
        return if (c.isEmpty()) a.toLong() * 60 + b.toLong() else a.toLong() * 3600 + b.toLong() * 60 + c.toLong()
    }

    /** Insere uma gravacao de teste com WAV real de [segundos] s. */
    fun inserirGravacao(titulo: String, segundos: Double = 2.0, favorita: Boolean = false): Recording = runBlocking {
        val arquivo = Suporte.escreverWavSeno(File(Suporte.contexto.filesDir, "recordings/${titulo.hashCode()}_${System.nanoTime()}.wav"), segundos)
        val base = Recording(
            title = titulo, path = arquivo.path, format = RecordingFormat.WAV, durationMillis = (segundos * 1000).toLong(),
            sizeBytes = arquivo.length(), createdAtMillis = System.currentTimeMillis(), isFavorite = favorita,
        )
        base.copy(id = app.repo.insert(base))
    }

    class Publicado(val uri: Uri, val nome: String, val pasta: String, val duracaoMs: Long)

    /** Procura no MediaStore a copia publicada mais recente cujo nome termina em [extensao]. */
    fun publicadoMaisRecente(extensao: String): Publicado? {
        val resolver = Suporte.contexto.contentResolver
        // Antes da API 29 nao existe RELATIVE_PATH: a pasta vem do caminho (DATA).
        val legado = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q
        @Suppress("DEPRECATION")
        val colunaPasta = if (legado) MediaStore.Audio.Media.DATA else MediaStore.Audio.Media.RELATIVE_PATH
        val colunas = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, colunaPasta)
        resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, colunas, "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?", arrayOf("%.$extensao"),
            "${MediaStore.Audio.Media.DATE_ADDED} DESC",
        )?.use { c ->
            if (!c.moveToFirst()) return null
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, c.getLong(0))
            val mmr = MediaMetadataRetriever()
            val dur = try {
                mmr.setDataSource(Suporte.contexto, uri)
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            } finally {
                mmr.release()
            }
            val bruto = c.getString(2).orEmpty()
            @Suppress("DEPRECATION")
            val pasta = if (legado) {
                bruto.substringBeforeLast('/').removePrefix(android.os.Environment.getExternalStorageDirectory().path + "/") + "/"
            } else {
                bruto
            }
            return Publicado(uri, c.getString(1), pasta, dur)
        }
        return null
    }
}
