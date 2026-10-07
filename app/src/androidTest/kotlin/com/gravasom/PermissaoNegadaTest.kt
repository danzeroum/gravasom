package com.gravasom

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Permissao negada de verdade pelo dialogo do sistema. Roda em INSTALACAO LIMPA (o CI executa esta classe separada das
 * demais, que concedem a permissao). Duas negacoes seguidas viram "negada permanentemente" no Android 11+.
 */
class PermissaoNegadaTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /** Nega todos os dialogos de permissao que aparecerem (microfone e, no Android 13+, notificacoes). */
    private fun negarDialogos() {
        val negar = By.res(java.util.regex.Pattern.compile("com\\.android\\.permissioncontroller:id/permission_deny(_and_dont_ask_again)?_button"))
        repeat(MAX_DIALOGOS) {
            val botao = device.wait(Until.findObject(negar), DIALOGO_MS) ?: return
            botao.click()
            Thread.sleep(PAUSA_ENTRE_DIALOGOS_MS) // o proximo dialogo (notificacoes) abre logo apos o fechamento do anterior
        }
    }

    @Test
    fun negadaDuasVezesMostraMensagemComBotaoParaConfiguracoesSemCrash() {
        compose.onNodeWithTag("botao_gravar").performClick()
        negarDialogos()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "aviso_permissao") }
        assertTrue("1a negacao: ainda pode pedir de novo", !UiSuporte.existe(compose, "abrir_configuracoes"))

        compose.onNodeWithTag("botao_gravar").performClick()
        negarDialogos()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "abrir_configuracoes") }

        compose.onNodeWithTag("abrir_configuracoes").performClick()
        val configuracoes = device.wait(Until.findObject(By.pkg("com.android.settings")), SETTINGS_MS)
        assertNotNull("nao abriu as configuracoes do sistema", configuracoes)
        device.pressBack()
    }

    private companion object {
        const val MAX_DIALOGOS = 3
        const val DIALOGO_MS = 6_000L
        const val PAUSA_ENTRE_DIALOGOS_MS = 1_200L
        const val SETTINGS_MS = 8_000L
    }
}
