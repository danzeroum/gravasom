package com.gravasom

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.rule.GrantPermissionRule
import com.gravasom.core.domain.audio.RecordingFormat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Jornada ponta a ponta com permissao concedida (docs/ux/jornada.md, hipoteses H1 e H2). */
class FluxoUsuarioTest {

    @get:Rule(order = 0)
    val permissoes: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun preparar() = UiSuporte.limparTudo()

    @Test
    fun gravar10sMostraUmItemDe10sNaListaETocaComUmToque() {
        val (ateGravar, gravadoMs) = UiSuporte.gravarPelaTela(compose, segundos = 10)
        Suporte.medida("ui_gravar", "tempo_ate_gravar_ms", ateGravar)
        Suporte.medida("ui_gravar", "gravado_relogio_ms", gravadoMs)
        assertTrue("H1: tempo ate gravar ${ateGravar} ms > 2000", ateGravar <= 2_000)

        compose.onNodeWithTag("aba_gravacoes").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "item_gravacao") }
        assertEquals("deveria haver 1 gravacao", 1, UiSuporte.textos(compose, "titulo_item").size)
        val segundos = UiSuporte.segundosDe(UiSuporte.textos(compose, "duracao_item").single())
        Suporte.medida("ui_gravar", "duracao_na_lista_s", segundos)
        assertTrue("duracao na lista = $segundos s (esperado ~10)", segundos in 9..11)

        compose.onNodeWithTag("item_gravacao").performClick() // 1 toque para ouvir
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "mini_player") }
        compose.onNodeWithTag("mini_player").assertIsDisplayed()
    }

    @Test
    fun gravacaoPublicadaNoMediaStoreEmMusicRecordings() {
        UiSuporte.gravarPelaTela(compose, segundos = 3)
        Thread.sleep(1_500)
        val pub = checkNotNull(UiSuporte.publicadoMaisRecente("m4a")) { "nada publicado no MediaStore" }
        Suporte.medida("mediastore_m4a", "pasta", pub.pasta)
        assertTrue("pasta=${pub.pasta}", pub.pasta.startsWith("Music/Recordings"))
        assertTrue("duracao ${pub.duracaoMs} ms", kotlin.math.abs(pub.duracaoMs - 3_000) <= 500)
    }

    @Test
    fun mp3GravadoFicaNoMediaStoreComDuracaoDentroDe500ms() {
        runBlocking { UiSuporte.app.preferences.setFormat(RecordingFormat.MP3) }
        val (_, gravadoMs) = UiSuporte.gravarPelaTela(compose, segundos = 4)
        Thread.sleep(2_000)
        val pub = checkNotNull(UiSuporte.publicadoMaisRecente("mp3")) { "nenhum .mp3 no MediaStore" }
        Suporte.medida("mediastore_mp3", "relogio_ms", gravadoMs)
        Suporte.medida("mediastore_mp3", "duracao_ms", pub.duracaoMs)
        assertTrue(pub.nome.endsWith(".mp3"))
        assertTrue("duracao ${pub.duracaoMs} ms x relogio $gravadoMs ms", kotlin.math.abs(pub.duracaoMs - gravadoMs) <= 500)
    }

    @Test
    fun vinteGravacoesAchaPorNomeParcialEmAte3Toques() {
        repeat(19) { UiSuporte.inserirGravacao("Aula de historia ${it + 1}") }
        UiSuporte.inserirGravacao("Ideia violao riff")
        var toques = 0
        compose.onNodeWithTag("aba_gravacoes").performClick(); toques++
        UiSuporte.aguardar(compose) { UiSuporte.textos(compose, "titulo_item").size >= 20 }
        compose.onNodeWithTag("campo_busca").performClick(); toques++
        compose.onNodeWithTag("campo_busca").performTextInput("violao")
        UiSuporte.aguardar(compose) { UiSuporte.textos(compose, "titulo_item") == listOf("Ideia violao riff") }
        Suporte.medida("ui_busca", "toques_ate_achar", toques)
        assertTrue("toques=$toques", toques <= 3)
    }

    @Test
    fun excluirOferecerDesfazerERestaura() {
        UiSuporte.inserirGravacao("Para excluir")
        compose.onNodeWithTag("aba_gravacoes").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "item_gravacao") }
        compose.onNodeWithTag("mais_acoes").performClick()
        compose.onNodeWithTag("menu_excluir").performClick()
        UiSuporte.aguardar(compose) { !UiSuporte.existe(compose, "item_gravacao") }
        UiSuporte.aguardar(compose) { compose.onAllNodesWithText("DESFAZER").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("DESFAZER").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "item_gravacao") }
    }
}

private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onNodeWithText(t: String) =
    onNode(androidx.compose.ui.test.hasText(t))
