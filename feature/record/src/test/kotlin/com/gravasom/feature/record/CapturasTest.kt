package com.gravasom.feature.record

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.gravasom.core.ui.GravasomTheme
import com.gravasom.core.ui.Paleta
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Gera as capturas da tela Gravar (pronto, gravando, pausado) em claro e escuro, nas paletas Gravasom e Mixlirous, para
 * docs/design/capturas/. Renderiza os mesmos composables do app em 360x780 dp. Roda so com GRAVASOM_CAPTURAS=<pasta>
 * (ou -Dgravasom.capturas=<pasta>); ver docs/design/ferramentas/capturar.sh.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "pt-rBR-w360dp-h780dp-xhdpi")
class CapturasTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var estado by mutableStateOf(Amostras.pronto)
    private var paleta by mutableStateOf(Paleta.GRAVASOM)
    private var escuro by mutableStateOf(false)

    private val estados = listOf(
        "pronto" to Amostras.pronto,
        "gravando" to Amostras.gravando,
        "pausado" to Amostras.pausado,
    )

    @Test
    fun telaGravarEmTodosOsTemas() {
        val dir = (System.getProperty("gravasom.capturas") ?: System.getenv("GRAVASOM_CAPTURAS")).orEmpty()
        assumeTrue("defina GRAVASOM_CAPTURAS", dir.isNotEmpty())
        compose.setContent {
            GravasomTheme(darkTheme = escuro, paleta = paleta) {
                Surface(color = MaterialTheme.colorScheme.surface) { RecordContent(estado, null, Amostras.callbacks()) }
            }
        }
        File(dir).mkdirs()
        for (p in listOf(Paleta.GRAVASOM, Paleta.MIXLIROUS)) {
            for (modoEscuro in listOf(false, true)) {
                for ((nome, amostra) in estados) {
                    capturar(File(dir, "gravar-$nome-${p.id}-${if (modoEscuro) "escuro" else "claro"}.png"), p, modoEscuro, amostra)
                }
            }
        }
    }

    private fun capturar(destino: File, p: Paleta, modoEscuro: Boolean, amostra: RecordUiState) {
        compose.runOnIdle {
            paleta = p
            escuro = modoEscuro
            estado = amostra
        }
        compose.mainClock.advanceTimeBy(ESPERA_ANIMACAO_MS)
        compose.waitForIdle()
        salvar(destino)
    }

    private fun salvar(destino: File) {
        val view = compose.activity.window.decorView
        val largura = view.resources.displayMetrics.widthPixels
        val altura = view.resources.displayMetrics.heightPixels
        val exato = { px: Int -> View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.EXACTLY) }
        view.measure(exato(largura), exato(altura))
        view.layout(0, 0, largura, altura)
        val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        destino.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALIDADE, it) }
    }

    private companion object {
        const val PNG_QUALIDADE = 100
        const val ESPERA_ANIMACAO_MS = 1_000L
    }
}
