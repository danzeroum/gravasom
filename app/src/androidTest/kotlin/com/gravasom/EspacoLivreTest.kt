package com.gravasom

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.gravasom.core.domain.audio.RecordingBudget
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.service.FreeSpace
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Prevencao de erro (Nielsen 5): com pouco espaco o app avisa ANTES de gravar, com tempo estimado, e ainda deixa gravar. */
class EspacoLivreTest {

    @get:Rule(order = 0)
    val permissoes = UiSuporte.permissoes()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val acessibilidade = RegraAcessibilidade(compose)

    @Before
    fun preparar() = UiSuporte.limparTudo()

    @After
    fun restaurar() {
        FreeSpace.overrideBytes = null
    }

    @Test
    fun cinquentaMegasLivresAvisamComTempoEstimadoEGravarMesmoAssimGrava() {
        FreeSpace.overrideBytes = 50L * 1024 * 1024
        compose.onNodeWithTag("botao_gravar").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "aviso_espaco") }
        assertFalse("nao pode gravar antes de confirmar", UiSuporte.existe(compose, "botao_parar"))

        val minutos = RecordingBudget.estimatedMinutes(50L * 1024 * 1024, RecordingFormat.M4A, 44_100, 128)
        val esperado = UiSuporte.rotulo("record_low_space_body").let { String.format(it, 50L, minutos) }
        assertEquals(esperado, UiSuporte.textos(compose, "aviso_espaco").single())
        Suporte.medida("espaco", "minutos_estimados_50mb_m4a", minutos)

        compose.onNodeWithTag("gravar_mesmo_assim").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "botao_parar") }
        Thread.sleep(3_000)
        compose.onNodeWithTag("botao_parar").performClick()
        UiSuporte.aguardar(compose, 15_000) { UiSuporte.existe(compose, "botao_gravar") }
        compose.onNodeWithTag("aba_gravacoes").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "item_gravacao") }
        assertEquals(1, UiSuporte.textos(compose, "titulo_item").size)
    }

    @Test
    fun cancelarNoAvisoNaoGrava() {
        FreeSpace.overrideBytes = 50L * 1024 * 1024
        compose.onNodeWithTag("botao_gravar").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "aviso_espaco") }
        compose.onNodeWithTag("cancelar_espaco").performClick()
        UiSuporte.aguardar(compose) { !UiSuporte.existe(compose, "aviso_espaco") }
        Thread.sleep(1_000)
        assertFalse(UiSuporte.existe(compose, "botao_parar"))
        assertTrue(UiSuporte.existe(compose, "botao_gravar"))
    }

    @Test
    fun comEspacoSuficienteNaoHaAviso() {
        FreeSpace.overrideBytes = 500L * 1024 * 1024
        compose.onNodeWithTag("botao_gravar").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "botao_parar") }
        assertFalse(UiSuporte.existe(compose, "aviso_espaco"))
        compose.onNodeWithTag("botao_parar").performClick()
        UiSuporte.aguardar(compose, 15_000) { UiSuporte.existe(compose, "botao_gravar") }
    }
}
