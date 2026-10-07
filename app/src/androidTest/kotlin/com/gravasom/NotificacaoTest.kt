package com.gravasom

import android.Manifest
import android.app.NotificationManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.gravasom.core.audio.engine.RecordingState
import com.gravasom.core.service.RecordingNotification
import com.gravasom.core.service.RecordingServiceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Jornada do Bruno (tela bloqueada): os botoes da notificacao tem de FUNCIONAR (antes eram intents vazios). */
class NotificacaoTest {

    @get:Rule(order = 0)
    val permissoes: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun preparar() = UiSuporte.limparTudo()

    private fun rotulo(nome: String) = UiSuporte.rotulo(nome)

    private fun acoes(): List<android.app.Notification.Action> {
        val nm = Suporte.contexto.getSystemService(NotificationManager::class.java)
        val n = nm.activeNotifications.firstOrNull { it.id == RecordingNotification.NOTIFICATION_ID }
        return n?.notification?.actions?.toList().orEmpty()
    }

    @Test
    fun pausarRetomarEPararPelaNotificacaoFuncionam() {
        compose.onNodeWithTag("botao_gravar").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "botao_parar") }
        assertTrue(Suporte.aguardar { acoes().size == 2 })
        assertEquals(listOf(rotulo("notif_action_pause"), rotulo("notif_action_stop")), acoes().map { it.title.toString() })

        acoes()[0].actionIntent.send()
        assertTrue("nao pausou", Suporte.aguardar { RecordingServiceState.state.value == RecordingState.Paused })
        assertTrue(Suporte.aguardar { acoes().map { it.title.toString() } == listOf(rotulo("notif_action_resume"), rotulo("notif_action_stop")) })

        acoes()[0].actionIntent.send()
        assertTrue("nao retomou", ControleServico.esperarGravando())

        Thread.sleep(1_000)
        acoes()[1].actionIntent.send()
        assertTrue("nao parou", Suporte.aguardar { RecordingServiceState.state.value is RecordingState.Saved })
    }
}
