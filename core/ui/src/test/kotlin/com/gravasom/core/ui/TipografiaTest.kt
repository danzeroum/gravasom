package com.gravasom.core.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.gravasom.core.ui.theme.GravasomPesosEmpacotados
import com.gravasom.core.ui.theme.GravasomTextStyles
import com.gravasom.core.ui.theme.GravasomTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Só Regular (400) e SemiBold (600) estão no APK: qualquer outro peso faria o Android desenhar um negrito falso. */
class TipografiaTest {

    /** Pesos usados por [estilos] que não existem no APK, com o nome do estilo. */
    private fun violacoes(estilos: Map<String, TextStyle>): List<String> =
        estilos.filter { (_, s) -> s.fontWeight?.weight !in GravasomPesosEmpacotados }.map { (nome, s) -> "$nome: ${s.fontWeight?.weight}" }

    private val escala: Map<String, TextStyle> = mapOf(
        "displayLarge" to GravasomTypography.displayLarge, "displayMedium" to GravasomTypography.displayMedium,
        "displaySmall" to GravasomTypography.displaySmall, "headlineLarge" to GravasomTypography.headlineLarge,
        "headlineMedium" to GravasomTypography.headlineMedium, "headlineSmall" to GravasomTypography.headlineSmall,
        "titleLarge" to GravasomTypography.titleLarge, "titleMedium" to GravasomTypography.titleMedium,
        "titleSmall" to GravasomTypography.titleSmall, "bodyLarge" to GravasomTypography.bodyLarge,
        "bodyMedium" to GravasomTypography.bodyMedium, "bodySmall" to GravasomTypography.bodySmall,
        "labelLarge" to GravasomTypography.labelLarge, "labelMedium" to GravasomTypography.labelMedium,
        "labelSmall" to GravasomTypography.labelSmall,
    )

    @Test
    fun `a escala e os estilos extras usam so 400 e 600`() {
        val extras = GravasomTextStyles.todos.mapIndexed { i, s -> "extra$i" to s }.toMap()
        assertEquals(15, escala.size)
        assertEquals(5, extras.size)
        assertEquals(emptyList<String>(), violacoes(escala + extras))
    }

    @Test
    fun `peso 500 ou 700 e reprovado`() {
        val medio = TextStyle(fontWeight = FontWeight.Medium)
        val negrito = TextStyle(fontWeight = FontWeight.Bold)
        val bom = TextStyle(fontWeight = FontWeight(600))
        assertEquals(listOf("medio: 500", "negrito: 700"), violacoes(mapOf("medio" to medio, "negrito" to negrito, "bom" to bom)))
    }

    @Test
    fun `so dois arquivos de fonte vao no APK`() {
        val arquivos = File("src/main/res/font").listFiles().orEmpty().map { it.name }.sorted()
        assertEquals(listOf("atkinson_next_regular.ttf", "atkinson_next_semibold.ttf"), arquivos)
        assertTrue(File("src/main/assets/licencas/OFL-atkinson-hyperlegible-next.txt").isFile)
    }

    @Test
    fun `algarismos tabulares no cronometro e no tempo`() {
        assertEquals("tnum", GravasomTextStyles.cronometro.fontFeatureSettings)
        assertEquals("tnum", GravasomTextStyles.tempo.fontFeatureSettings)
    }
}
