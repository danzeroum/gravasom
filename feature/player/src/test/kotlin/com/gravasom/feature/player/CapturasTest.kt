package com.gravasom.feature.player

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.gravasom.core.audio.engine.analysis.SkipPlan
import com.gravasom.core.audio.engine.analysis.Waveform
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.model.Recording
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Gera capturas de tela das telas do player para a avaliacao heuristica (docs/ux/heuristica.md). Renderiza os mesmos
 * composables do app, com o tema do app, em 360x780 dp. Roda so com -Dgravasom.capturas=<pasta>.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xhdpi")
class CapturasTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val hour = 3_600_000L
    private val recording = Recording(
        id = 1, title = "Aula de física · 08/10/2026 14:32", path = "/x/a.m4a", format = RecordingFormat.M4A, durationMillis = hour,
        sizeBytes = 1, createdAtMillis = 1, tags = listOf("aula", "fisica"),
    )
    private val markers = listOf(
        Marker(1, 1, 600_000, "Pergunta da prova", "Voltar aqui: o professor diz o que cai na prova."),
        Marker(2, 1, 1_800_000, "", ""),
        Marker(3, 1, 2_710_000, "Exemplo do pêndulo", "Anotar a fórmula"),
    )

    /** Onda de aula: blocos de fala de ~40 s com pausas, modulados como silabas. */
    private val waveform = Waveform(
        ByteArray(360_000) { i ->
            val speaking = (i / 100) % 60 < 40
            if (!speaking) 3 else (30 + 120 * abs(sin(i * 2 * PI / 37.0)) * (0.5 + 0.5 * abs(sin(i * 2 * PI / 411.0)))).toInt().toByte()
        },
    )

    private val callbacks = PlayerCallbacks(
        onClose = {}, onEditTags = {},
        markers = MarkerCallbacks({}, {}, {}),
        selection = SelectionCallbacks({ _, _ -> }, { _, _ -> }, {}),
        skip = SkipCallbacks({}, {}),
    )

    private fun capture(name: String, content: @Composable () -> Unit) {
        val dir = (System.getProperty("gravasom.capturas") ?: System.getenv("GRAVASOM_CAPTURAS")).orEmpty()
        assumeTrue("defina -Dgravasom.capturas", dir.isNotEmpty())
        compose.setContent {
            MaterialTheme { Surface(Modifier.fillMaxSize()) { Box(Modifier.padding(12.dp).width(336.dp)) { content() } } }
        }
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val widthPx = view.resources.displayMetrics.widthPixels
        val heightPx = view.resources.displayMetrics.heightPixels
        val exactWidth = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
        val exactHeight = View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        view.measure(exactWidth, exactHeight)
        view.layout(0, 0, widthPx, heightPx)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(dir).mkdirs()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
    }

    private fun panel(state: PlayerUiState, player: FakePlayerPort = FakePlayerPort(hour)) = @Composable {
        PlayerPanel(player, state, callbacks, maxHeight = 700.dp)
    }

    private fun base() = PlayerUiState(recording = recording, waveform = waveform, markers = markers)

    @Test
    fun player60MinComMarcadores() {
        val p = FakePlayerPort(hour).also { it.positionMillis.value = 1_810_000 }
        capture("01-player-60min-marcadores", panel(base(), p))
    }

    @Test
    fun playerPularSilencioLigado() {
        val frames = BooleanArray(180_000) { (it / 50) % 60 < 40 }
        val plan = SkipPlan.build(frames, hour)
        val p = FakePlayerPort(hour).also { it.positionMillis.value = 600_000; it.skippedMillis.value = 154_000 }
        capture("02-player-pular-silencio", panel(base().copy(skipEnabled = true, plan = plan), p))
    }

    @Test
    fun playerSelecaoDeTrecho() {
        val state = base().copy(selecting = true, selection = TimeSelection(2_710_000, 2_770_000))
        capture("03-player-selecao-trecho", panel(state, FakePlayerPort(hour).also { it.positionMillis.value = 2_710_000 }))
    }

    @Test
    fun playerCalculandoOnda() {
        capture("04-player-calculando-onda", panel(base().copy(waveform = null, analyzing = true, markers = emptyList())))
    }

    private companion object {
        const val PNG_QUALITY = 100
    }
}
