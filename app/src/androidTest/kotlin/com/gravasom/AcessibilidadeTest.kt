package com.gravasom

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.gravasom.core.service.FreeSpace
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Percorre as telas e estados principais com as checagens do Accessibility Test Framework ligadas
 * ([RegraAcessibilidade] reprova com a lista COMPLETA de erros). Roda em ingles e em pt-BR (passada da CI).
 * VERIFICACAO automatica; TalkBack com uma pessoa fica no roteiro docs/teste-aparelho-real.md.
 */
class AcessibilidadeTest {

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

    /** Roda as checagens sobre a hierarquia inteira no estado atual (os achados sao anotados pela regra). */
    private fun verificar(provider: SemanticsNodeInteractionsProvider = compose) {
        compose.waitForIdle()
        runCatching { provider.onAllNodes(isRoot()).tryPerformAccessibilityChecks() }
    }

    @Test
    fun gravarEmTodosOsEstados() {
        verificar() // pronto para gravar
        FreeSpace.overrideBytes = 50L * 1024 * 1024
        compose.onNodeWithTag("botao_gravar").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "aviso_espaco") }
        verificar() // dialogo de pouco espaco
        compose.onNodeWithTag("gravar_mesmo_assim").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "botao_parar") }
        Thread.sleep(1_500)
        verificar() // gravando
        compose.onNodeWithTag("botao_pausar").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "botao_pausar") }
        Thread.sleep(500)
        verificar() // pausada
        compose.onNodeWithTag("botao_parar").performClick()
        UiSuporte.aguardar(compose, 15_000) { UiSuporte.existe(compose, "botao_gravar") }
        verificar() // salva (com "Ver gravacoes")
    }

    @Test
    fun gravacoesEmTodosOsEstados() {
        compose.onNodeWithTag("aba_gravacoes").performClick()
        verificar() // lista vazia
        UiSuporte.inserirGravacao("Reuniao de planejamento", segundos = 2.0, favorita = true)
        UiSuporte.inserirGravacao("Ideia violao riff", segundos = 3.0)
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "item_gravacao") }
        verificar() // com itens
        compose.onNodeWithTag("campo_busca").performClick()
        compose.onNodeWithTag("campo_busca").performTextInput("violao")
        compose.waitForIdle()
        verificar() // busca com resultado
        compose.onNodeWithTag("mais_acoes").performClick()
        verificar() // menu de acoes aberto
        compose.onNodeWithTag("menu_renomear").performClick()
        verificar() // dialogo de renomear
        androidx.test.uiautomator.UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.waitForIdle()
        compose.onNodeWithTag("item_gravacao").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "mini_player") }
        verificar() // mini-player
    }

    @Test
    fun ajustesENumeros() {
        compose.onNodeWithTag("aba_ajustes").performClick()
        compose.waitForIdle()
        verificar() // Ajustes
        runBlocking { UiSuporte.app.preferences.setTelemetryOptIn(true) }
        compose.onNodeWithTag("abrir_numeros").performClick()
        compose.waitForIdle()
        verificar() // Seus numeros
    }
}
