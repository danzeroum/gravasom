package com.gravasom

import android.net.Uri
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.gravasom.core.domain.model.Recording
import com.gravasom.core.service.MediaProbe
import com.gravasom.core.service.RecordingStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Cofre cifrado (Tink, AES-256-GCM em fluxo). VERIFICACAO em emulador: o arquivo em disco nao e audio, o app toca,
 * desligar o cofre nao perde nada e compartilhar/salvar entregam o claro. A validacao com chave real do aparelho
 * (Keystore em hardware) fica no roteiro docs/teste-aparelho-real.md.
 */
class CofreTest {

    @get:Rule(order = 0)
    val permissoes = UiSuporte.permissoes()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val acessibilidade = RegraAcessibilidade(compose)

    @Before
    fun preparar() = UiSuporte.limparTudo()

    private fun ligarCofre(ligado: Boolean) = runBlocking { UiSuporte.app.preferences.setPrivateVault(ligado) }

    private fun gravacoes(): List<Recording> = runBlocking { UiSuporte.app.repo.observeAll().first() }

    /** Grava pela tela e espera a gravacao entrar na biblioteca. */
    private fun gravar(segundos: Int, total: Int): Recording {
        UiSuporte.gravarPelaTela(compose, segundos)
        assertTrue("a gravacao nao entrou na biblioteca", Suporte.aguardar(20_000) { gravacoes().size == total })
        // A copia publica (cofre desligado) so e registrada depois da insercao: espera o ingestor terminar.
        if (!runBlocking { UiSuporte.app.preferences.privateVault.first() }) {
            assertTrue("a copia publica nao foi registrada", Suporte.aguardar(20_000) { gravacoes().maxBy { it.createdAtMillis }.publishedUri != null })
        }
        return gravacoes().maxBy { it.createdAtMillis }
    }

    @Test
    fun cofreLigadoGravaCifradoForaDaListaPublicaETocaNoApp() {
        ligarCofre(true)
        val r = gravar(segundos = 3, total = 1)
        val arquivo = File(r.path)

        assertTrue("deveria estar no cofre: ${r.path}", r.isPrivate && r.path.endsWith(".gvs"))
        assertTrue(arquivo.length() > 0)
        assertNull("o arquivo cifrado nao pode abrir como audio", MediaProbe.durationMillis(arquivo))
        val cabecalho = ByteArray(CABECALHO_BYTES).also { buffer -> arquivo.inputStream().use { it.read(buffer) } }
        assertNotEquals("ftyp", String(cabecalho, 4, 4, Charsets.ISO_8859_1))
        val sobrouClaro = RecordingStorage.dir(Suporte.contexto).listFiles().orEmpty().filter { it.extension == "m4a" || it.extension == "tmp" }
        assertTrue("sobrou arquivo em claro: $sobrouClaro", sobrouClaro.isEmpty())

        assertNull("cofre nao publica no MediaStore", r.publishedUri)
        val publicado = UiSuporte.publicadoMaisRecente("m4a")
        assertTrue("apareceu na pasta publica: ${publicado?.nome}", publicado == null || !publicado.pasta.startsWith("Music/Recordings"))

        compose.onNodeWithTag("aba_gravacoes").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "item_gravacao") }
        assertTrue("sem o selo do cofre na linha", UiSuporte.existe(compose, "selo_cofre"))
        compose.onNodeWithTag("item_gravacao").performClick()
        // O snackbar de falha dura poucos segundos: observa os dois desfechos juntos para saber qual aconteceu.
        val textoFalha = UiSuporte.rotulo("library_play_fail")
        var falhou = false
        val abriu = Suporte.aguardar(15_000) {
            falhou = falhou || compose.onAllNodesWithText(textoFalha).fetchSemanticsNodes().isNotEmpty()
            falhou || UiSuporte.existe(compose, "mini_player")
        }
        assertFalse("o app mostrou '$textoFalha' ao tocar a gravacao do cofre", falhou)
        assertTrue("nem mini-player nem aviso de falha em 15 s", abriu)
        // O laco que atualiza a posicao usa delay() dentro do Compose, que roda no relogio do teste: ele so anda dentro de
        // waitUntil/waitForIdle (um sleep real nao o faz avancar).
        val andou = runCatching { compose.waitUntil(10_000) { posicaoDoPlayerMs() > POSICAO_MINIMA_MS } }.isSuccess
        Suporte.medida("cofre", "posicao_apos_tocar_ms", posicaoDoPlayerMs().toLong())
        assertTrue("o player nao avancou: ${posicaoDoPlayerMs()} ms", andou)
    }

    @Test
    fun desligarOCofreNaoPerdeGravacoesENovasSaoPublicas() {
        ligarCofre(true)
        val noCofre = gravar(segundos = 3, total = 1)
        ligarCofre(false)

        val ainda = gravacoes().single()
        assertEquals(noCofre.id, ainda.id)
        val claro = File(runBlocking { UiSuporte.app.files.playablePath(ainda) })
        val duracao = checkNotNull(MediaProbe.durationMillis(claro)) { "a gravacao do cofre nao decifra apos desligar" }
        assertTrue("duracao decifrada $duracao ms", duracao in 2_500..(UiSuporte.ultimaJanelaMaxMs + 500))

        val nova = gravar(segundos = 3, total = 2)
        assertFalse("a nova nao deveria estar no cofre", nova.isPrivate)
        assertNotNull("a nova deveria ter copia publica", nova.publishedUri)
        assertTrue(File(nova.path).extension == "m4a")
    }

    @Test
    fun compartilharESalvarEmEntregamOClaro() {
        ligarCofre(true)
        val r = gravar(segundos = 3, total = 1)
        val janelaMaxMs = UiSuporte.ultimaJanelaMaxMs

        val uri = runBlocking { UiSuporte.app.files.shareUri(r) }
        assertEquals("content", uri.scheme)
        assertFalse("caminho bruto do cofre exposto: $uri", uri.toString().contains("/vault/") || uri.toString().contains(".gvs"))
        val recebido = Suporte.contexto.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        assertEquals("ftyp", String(recebido, 4, 4, Charsets.ISO_8859_1))

        val destino = File(Suporte.testsOut(), "salvo_do_cofre.m4a").also { it.delete() }
        assertTrue(UiSuporte.app.files.copyTo(r, Uri.fromFile(destino)))
        val duracao = checkNotNull(MediaProbe.durationMillis(destino)) { "o arquivo salvo nao abre como audio" }
        // O arquivo comeca antes de a tela mostrar "Parar" e termina quando o Parar e processado: janela ate o retorno do toque.
        assertTrue("duracao salva $duracao ms fora de [2500, ${janelaMaxMs + 500}] ms", duracao in 2_500..(janelaMaxMs + 500))
    }

    @Test
    fun excluirDeVezRemoveCifradoECopiaEmClaro() {
        ligarCofre(true)
        val r = gravar(segundos = 2, total = 1)
        val tocavel = File(runBlocking { UiSuporte.app.files.playablePath(r) })
        assertTrue(tocavel.isFile)
        runBlocking { UiSuporte.app.files.deleteForever(r) }
        assertFalse(File(r.path).exists())
        assertFalse("copia em claro sobrou no cache", tocavel.exists())
    }

    private fun posicaoDoPlayerMs(): Float = runCatching {
        compose.onNodeWithTag("mini_posicao", useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.current ?: 0f
    }.getOrDefault(0f).let { if (it.isNaN()) 0f else it }

    private companion object {
        const val CABECALHO_BYTES = 12
        const val POSICAO_MINIMA_MS = 500f
    }
}
