package com.gravasom.feature.enhance

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.gravasom.core.ui.GravasomTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pt-rBR-w360dp-h780dp-xhdpi")
class EnhanceContentTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(state: EnhanceUiState, callbacks: EnhanceCallbacks = Amostras.callbacks()) {
        compose.setContent { GravasomTheme { EnhanceContent(state, callbacks) } }
        compose.waitForIdle()
    }

    private fun existe(tag: String) = compose.onAllNodesWithTagCompat(tag).isNotEmpty()

    @Test
    fun `pronto mostra titulo, o contrato do original e salvar desabilitado sem efeito`() {
        show(Amostras.pronto)
        compose.onNodeWithTag("titulo_melhorar").assertExists()
        compose.onNodeWithTag("duracao_melhorar").assertExists()
        compose.onAllNodesWithText("O original nunca é alterado. A melhoria é salva como uma gravação nova.").assertCountAtLeast(1)
        compose.onNodeWithTag("aviso_original").assertExists()
        compose.onNodeWithTag("botao_salvar_copia").assertIsNotEnabled()
        // Nada ligado: sem slider de intensidade e sem slider de alvo.
        assertFalse(existe("intensidade_redutor"))
        assertFalse(existe("alvo_volume"))
    }

    @Test
    fun `ajustado mostra os sliders e salvar habilitado`() {
        show(Amostras.ajustado)
        compose.onNodeWithTag("intensidade_redutor").assertExists()
        compose.onNodeWithTag("alvo_volume").assertExists()
        compose.onAllNodesWithText("40%", useUnmergedTree = true).assertCountAtLeast(1)
        compose.onAllNodesWithText("-16 LUFS", useUnmergedTree = true).assertCountAtLeast(1)
        compose.onNodeWithTag("botao_salvar_copia").assertIsEnabled()
        compose.onNodeWithTag("botao_previa_a").assertExists()
        compose.onNodeWithTag("botao_previa_b").assertExists()
        compose.onNodeWithTag("trecho_previa").assertExists()
    }

    @Test
    fun `exportando mostra progresso e cancelar, sem o botao de salvar`() {
        show(Amostras.exportando)
        compose.onNodeWithTag("progresso_melhorar").assertExists()
        compose.onAllNodesWithText("42%", useUnmergedTree = true).assertCountAtLeast(1)
        compose.onNodeWithTag("botao_cancelar_melhorar").assertIsEnabled()
        assertFalse(existe("botao_salvar_copia"))
    }

    @Test
    fun `salvo mostra o titulo da copia e concluir`() {
        show(Amostras.salvo)
        compose.onAllNodesWithText("Cópia salva: Aula de física (melhorada)").assertCountAtLeast(1)
        compose.onNodeWithTag("botao_concluir").assertIsEnabled()
    }

    @Test
    fun `tocar nos lados A e B chega aos callbacks`() {
        var lado: EnhanceSide? = null
        show(Amostras.ajustado, EnhanceCallbacks(onPreview = { lado = it }))
        compose.onNodeWithTag("botao_previa_a").performClick()
        compose.waitForIdle()
        assertEquals(EnhanceSide.A, lado)
        compose.onNodeWithTag("botao_previa_b").performClick()
        compose.waitForIdle()
        assertEquals(EnhanceSide.B, lado)
    }

    @Test
    fun `ligar o redutor pede a intensidade e ligar o volume pede o alvo`() {
        var denoise: Boolean? = null
        var normalize: Boolean? = null
        show(Amostras.pronto, EnhanceCallbacks(onDenoise = { denoise = it }, onNormalize = { normalize = it }))
        compose.onNodeWithTag("chave_redutor").performClick()
        compose.onNodeWithTag("chave_volume").performClick()
        compose.waitForIdle()
        assertEquals(true, denoise)
        assertEquals(true, normalize)
    }

    /** A previa tocando nunca depende so de cor: o texto diz qual lado esta tocando. */
    @Test
    fun `previa tocando B anuncia em texto`() {
        show(Amostras.previaB)
        compose.onAllNodesWithText("Tocando o melhorado").assertCountAtLeast(1)
        compose.onNodeWithTag("tocando_previa").assertExists()
    }

    @Test
    fun `carregando mostra o preparo e nao os ajustes`() {
        show(EnhanceUiState(status = EnhanceStatus.LOADING))
        compose.onNodeWithTag("preparando_melhorar").assertExists()
        assertFalse(existe("chave_redutor"))
    }

    @Test
    fun `falha mostra o erro em texto`() {
        show(EnhanceUiState(status = EnhanceStatus.FAILED, failedReason = R.string.enhance_fail_decode))
        compose.onAllNodesWithText("Não foi possível abrir esta gravação.").assertCountAtLeast(1)
        assertTrue(existe("erro_melhorar"))
    }
}
