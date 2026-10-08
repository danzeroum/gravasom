package com.gravasom

import android.app.NotificationManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.marker.MarkerNote
import com.gravasom.core.service.RecordingNotification
import com.gravasom.core.service.RecordingServiceState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Onda 1, "Achar e revisar": marcadores na onda e na lista, nota de ate 140 caracteres e a busca de um trecho marcado
 * numa gravacao de 60 min em <= 3 toques.
 */
class OndaMarcadoresTest {

    @get:Rule(order = 0)
    val permissoes = UiSuporte.permissoes()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val acessibilidade = RegraAcessibilidade(compose)

    @Before
    fun preparar() = UiSuporte.limparTudo()

    private fun acoes(): List<android.app.Notification.Action> {
        val nm = Suporte.contexto.getSystemService(NotificationManager::class.java)
        val n = nm.activeNotifications.firstOrNull { it.id == RecordingNotification.NOTIFICATION_ID }
        return n?.notification?.actions?.toList().orEmpty()
    }

    private fun abrirNaBiblioteca() {
        compose.onNodeWithTag("aba_gravacoes").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "item_gravacao") }
        compose.onNodeWithTag("item_gravacao").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "mini_player") }
    }

    /** Verificacao do item "marcador criado pela notificacao aparece na onda". */
    @Test
    fun marcadorCriadoPelaNotificacaoApareceNaOnda() {
        compose.onNodeWithTag("botao_gravar").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "botao_parar") }
        Thread.sleep(2_000)
        val marcar = acoes().first { it.title.toString() == UiSuporte.rotulo("notif_action_marker") }
        marcar.actionIntent.send()
        assertTrue("o servico nao registrou o marcador", Suporte.aguardar { RecordingServiceState.markerCount.value == 1 })
        compose.onNodeWithTag("botao_marcar_contagem", useUnmergedTree = true).assertIsDisplayed()
        Thread.sleep(1_500)
        compose.onNodeWithTag("botao_parar").performClick()
        UiSuporte.aguardar(compose, 15_000) { UiSuporte.existe(compose, "botao_gravar") }

        abrirNaBiblioteca()
        UiSuporte.aguardar(compose) { UiSuporte.existePrefixo(compose, "marcador_onda_") }
        val gravacao = runBlocking { UiSuporte.app.repo.observeAll().first().single() }
        val marcadores = runBlocking { UiSuporte.app.markers.list(gravacao.id) }
        assertEquals(1, marcadores.size)
        // Criado ~2 s depois do inicio: a posicao no arquivo (sem pausas) cai entre 1,3 s e 3,5 s.
        Suporte.medida("marcador_notificacao", "posicao_ms", marcadores.single().positionMillis)
        assertTrue("posicao ${marcadores.single().positionMillis}", marcadores.single().positionMillis in 1_300..3_500)
        assertTrue(UiSuporte.existe(compose, "marcador_item_${marcadores.single().id}"))
    }

    /** Criterio de aceite: achar o trecho marcado de uma gravacao de 60 min em no maximo 3 toques. */
    @Test
    fun acharTrechoMarcadoEmGravacaoDe60MinutosEmAteTresToques() {
        val arquivo = Suporte.escreverWavSeno(File(Suporte.contexto.filesDir, "recordings/uma_hora.wav"), 3_600.0, taxa = 8_000)
        val gravacao = runBlocking {
            val base = com.gravasom.core.domain.model.Recording(
                title = "Aula de 60 min", path = arquivo.path, format = com.gravasom.core.domain.audio.RecordingFormat.WAV,
                durationMillis = 3_600_000, sizeBytes = arquivo.length(), createdAtMillis = System.currentTimeMillis(),
            )
            base.copy(id = UiSuporte.app.repo.insert(base))
        }
        val alvo = 2_710_000L // 45:10
        val marcador = runBlocking {
            UiSuporte.app.markers.insert(Marker(recordingId = gravacao.id, positionMillis = alvo, label = "Pergunta da prova", note = "revisar depois"))
        }
        compose.onNodeWithTag("aba_gravacoes").performClick() // navegacao entre abas nao conta: a busca parte da biblioteca
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "item_gravacao") }

        var toques = 0
        compose.onNodeWithTag("item_gravacao").performClick(); toques++ // 1: abre e toca a gravacao
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "marcador_item_$marcador") }
        compose.onNodeWithTag("marcador_item_$marcador", useUnmergedTree = true).performClick(); toques++ // 2: vai ao marcador
        UiSuporte.aguardar(compose, 20_000) { UiSuporte.posicaoDoPlayerMs(compose) >= alvo - 1_000 }
        Suporte.medida("achar_trecho_60min", "toques", toques)
        Suporte.medida("achar_trecho_60min", "posicao_ms", UiSuporte.posicaoDoPlayerMs(compose))
        assertTrue("$toques toques (maximo 3)", toques <= 3)
        assertTrue("posicao ${UiSuporte.posicaoDoPlayerMs(compose)} nao esta no marcador", UiSuporte.posicaoDoPlayerMs(compose) in (alvo - 1_000f)..(alvo + 15_000f))
    }

    /** Nota de 141 caracteres e rejeitada com mensagem; a de 140 e salva. */
    @Test
    fun nota141CaracteresRejeitadaE140Salva() {
        UiSuporte.inserirGravacao("Reuniao", segundos = 20.0)
        compose.onNodeWithTag("aba_gravacoes").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "item_gravacao") }
        compose.onNodeWithTag("item_gravacao").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "adicionar_marcador") }

        compose.onNodeWithTag("adicionar_marcador").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "campo_nota_marcador") }
        compose.onNodeWithTag("campo_nota_marcador").performTextInput("a".repeat(MarkerNote.MAX_LENGTH + 1))
        val erro = UiSuporte.rotuloComArgs("player_marker_note_too_long", MarkerNote.MAX_LENGTH)
        assertEquals(listOf(erro), UiSuporte.textos(compose, "erro_nota"))
        compose.onNodeWithTag("salvar_marcador").assertIsNotEnabled()

        compose.onNodeWithTag("campo_nota_marcador").performTextReplacement("b".repeat(MarkerNote.MAX_LENGTH))
        compose.onNodeWithTag("salvar_marcador").performClick()
        UiSuporte.aguardar(compose) { !UiSuporte.existe(compose, "campo_nota_marcador") }
        val salvo = runBlocking { UiSuporte.app.markers.list(UiSuporte.app.repo.observeAll().first().single().id) }.single()
        assertEquals("b".repeat(140), salvo.note)
    }

    /** "Marcar aqui" no player cria o marcador na posicao atual e ele passa a ser listado e desenhado na onda. */
    @Test
    fun marcarAquiPeloPlayerCriaMarcadorNaOnda() {
        UiSuporte.inserirGravacao("Ideia", segundos = 20.0)
        compose.onNodeWithTag("aba_gravacoes").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "item_gravacao") }
        compose.onNodeWithTag("item_gravacao").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "adicionar_marcador") }
        compose.onNodeWithTag("adicionar_marcador").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "campo_nota_marcador") }
        compose.onNodeWithTag("salvar_marcador").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existePrefixo(compose, "marcador_onda_") && UiSuporte.existePrefixo(compose, "marcador_item_") }
    }
}
