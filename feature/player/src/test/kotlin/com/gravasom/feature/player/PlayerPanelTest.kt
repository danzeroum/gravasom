package com.gravasom.feature.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.gravasom.core.audio.engine.analysis.SilenceSensitivity
import com.gravasom.core.audio.engine.analysis.SkipPlan
import com.gravasom.core.audio.engine.analysis.Waveform
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.model.Recording
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** O painel do player com um player falso: o que cada toque pede ao player e aos callbacks. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerPanelTest {

    @get:Rule
    val compose = createComposeRule()

    private val hour = 3_600_000L
    private val recording = Recording(
        id = 1, title = "Aula de fisica", path = "/x/a.m4a", format = RecordingFormat.M4A, durationMillis = hour,
        sizeBytes = 1, createdAtMillis = 1, tags = listOf("aula"),
    )
    private val markers = listOf(
        Marker(1, 1, 600_000, "Pergunta", "voltar aqui"),
        Marker(2, 1, 2_710_000, "", ""),
    )
    private val player = FakePlayerPort(hour)
    private val log = mutableListOf<String>()

    private val callbacks = PlayerCallbacks(
        onClose = { log += "fechar" },
        onEditTags = { log += "etiquetas" },
        markers = MarkerCallbacks(
            onAdd = { log += "marcar($it)" },
            onEdit = { log += "editar(${it.id})" },
            onSelectToNext = { log += "trecho_ate_proximo(${it.id})" },
        ),
        selection = SelectionCallbacks(
            onToggle = { on, pos -> log += "selecionar($on,$pos)" },
            onChange = { a, b -> log += "trecho($a,$b)" },
            onExport = { log += "exportar" },
        ),
        skip = SkipCallbacks(onEnabled = { log += "pular($it)" }, onSensitivity = { log += "sensibilidade($it)" }),
    )

    private fun mostrar(state: PlayerUiState) {
        compose.setContent {
            MaterialTheme { PlayerPanel(player, state, callbacks, maxHeight = 900.dp) }
        }
    }

    private fun estado(vararg ajustes: (PlayerUiState) -> PlayerUiState): PlayerUiState =
        ajustes.fold(PlayerUiState(recording = recording, waveform = Waveform(ByteArray(360_000) { 50 }), markers = markers)) { s, f ->
            f(s)
        }

    @Test
    fun `tocar num marcador da lista leva o player ate ele e ja toca (achar o trecho em 2 toques)`() {
        mostrar(estado())
        compose.onNodeWithTag("marcador_item_2", useUnmergedTree = true).performClick()
        assertEquals(listOf("seekToAndPlay(2710000)"), player.calls)
        assertTrue(player.isPlaying.value)
    }

    @Test
    fun `tocar num marcador desenhado na onda faz o mesmo`() {
        mostrar(estado())
        compose.onNodeWithTag("marcador_onda_1", useUnmergedTree = true).performClick()
        assertEquals(listOf("seekToAndPlay(600000)"), player.calls)
    }

    @Test
    fun `anterior e proximo marcador seguem a posicao e desligam nas pontas`() {
        player.positionMillis.value = 1_000_000
        mostrar(estado())
        compose.onNodeWithTag("marcador_anterior").assertIsEnabled().performClick()
        assertEquals("seekToAndPlay(600000)", player.calls.last())
        compose.onNodeWithTag("marcador_proximo").assertIsEnabled().performClick()
        assertEquals("seekToAndPlay(2710000)", player.calls.last())
        compose.waitForIdle()
        compose.onNodeWithTag("marcador_proximo").assertIsNotEnabled() // ja esta no ultimo
    }

    @Test
    fun `marcar aqui pede o marcador na posicao atual`() {
        player.positionMillis.value = 125_000
        mostrar(estado())
        compose.onNodeWithTag("adicionar_marcador").performClick()
        assertEquals(listOf("marcar(125000)"), log)
    }

    @Test
    fun `pular silencio liga, mostra a sensibilidade e a economia prevista`() {
        val plan = SkipPlan.build(BooleanArray(1_000) { it in 0..100 }, 20_000)
        mostrar(estado({ it.copy(skipEnabled = true, plan = plan) }))
        compose.onNodeWithTag("sensibilidade_silencio").performClick()
        assertEquals(listOf("sensibilidade(${SilenceSensitivity.GENTLE})"), log)
        compose.onNodeWithTag("economia_silencio", useUnmergedTree = true).assertExists()
        assertEquals(plan, player.skipPlan)
        assertTrue(player.skipEnabled)
    }

    @Test
    fun `chip de pular silencio alterna`() {
        mostrar(estado())
        compose.onNodeWithTag("pular_silencio").performClick()
        assertEquals(listOf("pular(true)"), log)
        compose.onAllNodesWithTag("sensibilidade_silencio").assertCountEquals(0)
    }

    @Test
    fun `selecionar trecho mostra a barra, so exporta com intervalo e cancela`() {
        mostrar(estado({ it.copy(selecting = true, selection = TimeSelection(10_000, 40_000)) }))
        compose.onNodeWithTag("exportar_trecho").assertIsEnabled().performClick()
        compose.onNodeWithTag("cancelar_trecho").performClick()
        assertEquals(listOf("exportar", "selecionar(false,0)"), log)
    }

    @Test
    fun `sem intervalo o botao exportar fica desativado`() {
        mostrar(estado({ it.copy(selecting = true, selection = TimeSelection(10_000, 10_000)) }))
        compose.onNodeWithTag("exportar_trecho").assertIsNotEnabled()
    }

    @Test
    fun `sem marcadores explica como criar`() {
        mostrar(estado({ it.copy(markers = emptyList()) }))
        compose.onNodeWithTag("sem_marcadores", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `trecho do marcador ate o proximo e editar marcador chegam aos callbacks`() {
        mostrar(estado())
        compose.onNodeWithTag("trecho_do_marcador_1", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("editar_marcador_2", useUnmergedTree = true).performClick()
        assertEquals(listOf("trecho_ate_proximo(1)", "editar(2)"), log)
    }
}
