package com.gravasom.feature.player

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import com.gravasom.core.audio.engine.analysis.ViewWindow
import com.gravasom.core.audio.engine.analysis.Waveform
import com.gravasom.core.domain.marker.Marker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WaveformViewTest {

    @get:Rule
    val compose = createComposeRule()

    private val hour = 3_600_000L
    private val markers = listOf(
        Marker(1, 1, 600_000, "Pergunta", "voltar aqui"),
        Marker(2, 1, 1_800_000, "", ""),
        Marker(3, 1, 3_000_000, "Fim", ""),
    )

    private var window by mutableStateOf(ViewWindow.full(hour))
    private var position by mutableStateOf(0L)
    private val clicked = mutableListOf<Long>()
    private val sought = mutableListOf<Long>()
    private var prev = 0
    private var next = 0

    private fun mostrar(visibleMarkers: List<Marker> = markers) {
        compose.setContent {
            MaterialTheme {
                WaveformView(
                    waveform = Waveform(ByteArray(360_000) { (it % 200).toByte() }),
                    durationMillis = hour,
                    window = { window },
                    onWindowChange = { window = it },
                    positionMillis = { position },
                    markers = visibleMarkers,
                    skipRanges = emptyList(),
                    selection = null,
                    selecting = false,
                    actions = WaveformActions(
                        onSeek = { sought += it },
                        onMarkerClick = { clicked += it.positionMillis },
                        onSelection = { _, _ -> },
                        onPreviousMarker = { prev++ },
                        onNextMarker = { next++ },
                    ),
                    labels = WaveformLabels("onda", "anterior", "proximo", "ampliar", "reduzir"),
                    modifier = Modifier.fillMaxWidth().height(112.dp).testTagWave(),
                )
            }
        }
    }

    private fun Modifier.testTagWave() = this.testTag("onda")

    @Test
    fun `cada marcador visivel vira um alvo de toque que leva ao ponto marcado`() {
        mostrar()
        compose.onAllNodesWithTag("marcador_onda_1", useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithTag("marcador_onda_2", useUnmergedTree = true).performClick()
        assertEquals(listOf(1_800_000L), clicked)
    }

    @Test
    fun `com zoom so os marcadores dentro da janela aparecem como alvos`() {
        mostrar()
        // janela de 10 min ao redor do marcador 1 (600 s): o 2 (1800 s) e o 3 (3000 s) saem
        window = ViewWindow(hour, startMillis = 300_000, spanMillis = 600_000)
        compose.waitForIdle()
        compose.onAllNodesWithTag("marcador_onda_1", useUnmergedTree = true).assertCountEquals(1)
        compose.onAllNodesWithTag("marcador_onda_2", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("marcador_onda_3", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `a onda expoe posicao e acoes de acessibilidade, ja que pinca nao e possivel no leitor de tela`() {
        mostrar()
        position = 125_000
        compose.waitForIdle()
        val node = compose.onNodeWithTag("onda").fetchSemanticsNode()
        assertEquals(125_000f, node.config[SemanticsProperties.ProgressBarRangeInfo].current)
        assertEquals(hour.toFloat(), node.config[SemanticsProperties.ProgressBarRangeInfo].range.endInclusive)
        val names = node.config[SemanticsActions.CustomActions].map { it.label }
        assertEquals(listOf("anterior", "proximo", "ampliar", "reduzir"), names)
        assertNotNull(node.config.getOrNull(SemanticsActions.SetProgress))
    }

    @Test
    fun `acao ampliar da acessibilidade reduz a janela e reduzir a devolve`() {
        mostrar()
        val ampliar = compose.onNodeWithTag("onda").fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == "ampliar" }
        compose.runOnUiThread { ampliar.action() }
        compose.waitForIdle()
        assertEquals(hour / 2, window.spanMillis)
        val reduzir = compose.onNodeWithTag("onda").fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == "reduzir" }
        compose.runOnUiThread { reduzir.action() }
        compose.waitForIdle()
        assertEquals(hour, window.spanMillis)
    }

    @Test
    fun `acoes de marcador anterior e proximo chegam ao chamador`() {
        mostrar()
        val acoes = compose.onNodeWithTag("onda").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnUiThread { acoes.first { it.label == "anterior" }.action(); acoes.first { it.label == "proximo" }.action() }
        assertTrue(prev == 1 && next == 1)
    }

    @Test
    fun `acao de progresso do leitor de tela busca a posicao`() {
        mostrar()
        compose.onNodeWithTag("onda").performSemanticsAction(SemanticsActions.SetProgress) { it(90_000f) }
        assertEquals(listOf(90_000L), sought)
    }
}
