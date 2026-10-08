package com.gravasom.feature.record

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.gravasom.core.ui.GravasomTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pt-rBR-w360dp-h780dp-xhdpi")
class RecordContentTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(state: RecordUiState, callbacks: RecordCallbacks = Amostras.callbacks()) {
        compose.setContent { GravasomTheme { RecordContent(state, permissionNotice = null, callbacks = callbacks) } }
        compose.waitForIdle()
    }

    private fun existe(tag: String) = compose.onAllNodesWithTagCompat(tag).isNotEmpty()

    private fun descricao(tag: String): String? =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()

    @Test
    fun `pronto mostra Gravar, sem faixa e com o medidor apagado`() {
        show(Amostras.pronto)
        assertTrue(existe("botao_gravar"))
        assertFalse(existe("botao_parar"))
        assertFalse(existe("estado_gravacao"))
        assertEquals("Tempo de gravação 00:00", descricao("cronometro"))
        compose.onAllNodesWithText("Gravar", useUnmergedTree = true).assertCountAtLeast(1)
        compose.onNodeWithTag("chip_M4A").assertIsEnabled()
    }

    @Test
    fun `gravando mostra a faixa, Parar, Pausar e Marcar`() {
        show(Amostras.gravando)
        assertEquals("Gravando, 12 segundos", descricao("estado_gravacao"))
        assertTrue(existe("botao_parar"))
        assertFalse(existe("botao_gravar"))
        assertEquals("Pausar", descricao("botao_pausar"))
        assertEquals("Marcar", descricao("botao_marcar"))
        compose.onNodeWithTag("botao_marcar_contagem", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("chip_WAV").assertIsNotEnabled()
    }

    @Test
    fun `pausado mostra a faixa clara, Retomar e o medidor apagado`() {
        show(Amostras.pausado)
        assertEquals("Gravação pausada em 12 segundos", descricao("estado_gravacao"))
        assertEquals("Retomar", descricao("botao_pausar"))
        assertTrue(existe("botao_parar"))
    }

    /** Gravando nunca depende so do vermelho: palavra, rotulo do botao e descricao mudam junto com a cor. */
    @Test
    fun `os tres estados diferem em texto, rotulo e descricao e nao so em cor`() {
        val state = androidx.compose.runtime.mutableStateOf(Amostras.pronto)
        compose.setContent { GravasomTheme { RecordContent(state.value, null, Amostras.callbacks()) } }
        fun sinais(): List<String?> {
            compose.waitForIdle()
            val palavra = listOf("GRAVANDO", "PAUSADA").firstOrNull {
                compose.onAllNodesWithText(it, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            val faixa = if (existe("estado_gravacao")) descricao("estado_gravacao") else null
            val botao = descricao(if (existe("botao_parar")) "botao_parar" else "botao_gravar")
            val lateral = if (existe("botao_pausar")) descricao("botao_pausar") else null
            return listOf(palavra, faixa, botao, lateral)
        }
        val pronto = sinais()
        state.value = Amostras.gravando
        val gravando = sinais()
        state.value = Amostras.pausado
        val pausado = sinais()
        assertEquals(listOf(null, null, "Gravar", null), pronto)
        assertEquals(listOf("GRAVANDO", "Gravando, 12 segundos", "Parar", "Pausar"), gravando)
        assertEquals(listOf("PAUSADA", "Gravação pausada em 12 segundos", "Parar", "Retomar"), pausado)
        assertNotEquals(gravando, pausado)
    }

    @Test
    fun `toques chamam os callbacks`() {
        var gravou = 0
        var parou = 0
        var pausou = 0
        var marcou = 0
        val state = androidx.compose.runtime.mutableStateOf(Amostras.pronto)
        compose.setContent {
            GravasomTheme {
                RecordContent(
                    state.value, null,
                    Amostras.callbacks(onRecord = { gravou++ }, onStop = { parou++ }, onPause = { pausou++ }, onMarker = { marcou++ }),
                )
            }
        }
        compose.onNodeWithTag("botao_gravar").performClick()
        state.value = Amostras.gravando
        compose.waitForIdle()
        compose.onNodeWithTag("botao_pausar").performClick()
        compose.onNodeWithTag("botao_marcar").performClick()
        compose.onNodeWithTag("botao_parar").performClick()
        assertEquals(listOf(1, 1, 1, 1), listOf(gravou, pausou, marcou, parou))
    }

    @Test
    fun `salvando mostra progresso e bloqueia o botao`() {
        show(Amostras.salvando)
        compose.onNodeWithTag("botao_gravar").assertDoesNotExist()
        compose.onNodeWithTag("botao_aguarde").assertIsNotEnabled()
        compose.onAllNodesWithText("Salvando…", useUnmergedTree = true).assertCountAtLeast(1)
    }

    @Test
    fun `salvo oferece Ver em Gravacoes`() {
        show(Amostras.salvo)
        compose.onNodeWithTag("ver_gravacoes").assertExists()
    }

    @Test
    fun `nivel alto demais aparece com texto`() {
        show(Amostras.gravando.copy(levelDb = -0.5f))
        compose.onAllNodesWithText("Alto demais", useUnmergedTree = true).assertCountAtLeast(1)
    }
}
