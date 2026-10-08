package com.gravasom

import android.net.Uri
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
        val cabecalho = arquivo.inputStream().use { it.readNBytes(12) }
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
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "mini_player") }
        val andou = Suporte.aguardar(10_000) { posicaoDoPlayerMs() > POSICAO_MINIMA_MS }
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
        assertTrue("duracao decifrada $duracao ms", kotlin.math.abs(duracao - 3_000) <= 500)

        val nova = gravar(segundos = 3, total = 2)
        assertFalse("a nova nao deveria estar no cofre", nova.isPrivate)
        assertNotNull("a nova deveria ter copia publica", nova.publishedUri)
        assertTrue(File(nova.path).extension == "m4a")
    }

    @Test
    fun compartilharESalvarEmEntregamOClaro() {
        ligarCofre(true)
        val r = gravar(segundos = 3, total = 1)

        val uri = runBlocking { UiSuporte.app.files.shareUri(r) }
        assertEquals("content", uri.scheme)
        assertFalse("caminho bruto do cofre exposto: $uri", uri.toString().contains("/vault/") || uri.toString().contains(".gvs"))
        val recebido = Suporte.contexto.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        assertEquals("ftyp", String(recebido, 4, 4, Charsets.ISO_8859_1))

        val destino = File(Suporte.testsOut(), "salvo_do_cofre.m4a").also { it.delete() }
        assertTrue(UiSuporte.app.files.copyTo(r, Uri.fromFile(destino)))
        val duracao = checkNotNull(MediaProbe.durationMillis(destino)) { "o arquivo salvo nao abre como audio" }
        assertTrue("duracao salva $duracao ms", kotlin.math.abs(duracao - 3_000) <= 500)
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
        const val POSICAO_MINIMA_MS = 500f
    }
}
