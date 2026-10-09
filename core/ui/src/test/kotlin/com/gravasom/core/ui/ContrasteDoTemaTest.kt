package com.gravasom.core.ui

import androidx.compose.material3.ColorScheme
import androidx.test.core.app.ApplicationProvider
import com.gravasom.core.ui.theme.GravasomExtraColors
import com.gravasom.core.ui.theme.ParDeContraste
import com.gravasom.core.ui.theme.ParesContrasteGerado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Contraste de TODOS os pares do tokens.json, calculado sobre os objetos Kotlin gerados (não sobre o JSON): texto >= 4,5:1 e
 * ícone, traço e borda >= 3:1, nas três paletas e nos modos claro e escuro.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContrasteDoTemaTest {

    private fun reprovados(modo: String, esquema: ColorScheme, extras: GravasomExtraColors): List<String> =
        ParesContrasteGerado.porModo.getValue(modo).mapNotNull { par: ParDeContraste ->
            val razao = Contraste.razao(par.primeiroPlano(esquema, extras), par.fundo(esquema, extras))
            if (razao + EPSILON >= par.minimo) null else "%s: %.2f < %.1f".format(par.nome, razao, par.minimo)
        }

    private fun exigeTodos(modo: String, paleta: Paleta, escuro: Boolean) {
        val esquema = esquemaDeCores(paleta, escuro, ApplicationProvider.getApplicationContext())
        val lista = reprovados(modo, esquema, coresExtras(paleta, escuro))
        assertTrue("$modo: ${lista.size} par(es) abaixo do AA:\n${lista.joinToString("\n")}", lista.isEmpty())
    }

    @Test
    fun `gravasom claro passa no AA`() = exigeTodos("light", Paleta.GRAVASOM, escuro = false)

    @Test
    fun `gravasom escuro passa no AA`() = exigeTodos("dark", Paleta.GRAVASOM, escuro = true)

    @Test
    fun `mixlirous claro passa no AA`() = exigeTodos("mixlirous-light", Paleta.MIXLIROUS, escuro = false)

    @Test
    fun `mixlirous escuro passa no AA`() = exigeTodos("mixlirous-dark", Paleta.MIXLIROUS, escuro = true)

    /** Cores do sistema (Android 12+): o esquema vem do sistema e os estados continuam os da Gravasom (D1). */
    @Test
    fun `cores do sistema claro passam no AA`() = exigeTodos("light", Paleta.SISTEMA, escuro = false)

    @Test
    fun `cores do sistema escuro passam no AA`() = exigeTodos("dark", Paleta.SISTEMA, escuro = true)

    @Test
    fun `os pares do icone passam`() {
        val vazio = esquemaDeCores(Paleta.GRAVASOM, false)
        assertTrue(reprovados("icone", vazio, coresExtras(Paleta.GRAVASOM, false)).isEmpty())
    }

    @Test
    fun `todos os pares foram gerados`() {
        // 85 pares por modo do tokens.json (59 de texto, 23 de ícone/traço, 3 informativos) e 43+23+... nos temas: conta fixa
        // para o teste acusar se o gerador perder pares.
        assertEquals(85, ParesContrasteGerado.porModo.getValue("light").size)
        assertEquals(85, ParesContrasteGerado.porModo.getValue("dark").size)
        assertEquals(66, ParesContrasteGerado.porModo.getValue("mixlirous-light").size)
        assertEquals(66, ParesContrasteGerado.porModo.getValue("mixlirous-dark").size)
        assertEquals(2, ParesContrasteGerado.porModo.getValue("icone").size)
    }

    @Test
    fun `o medidor de contraste bate com valores conhecidos`() {
        assertEquals(21.0, Contraste.razao(androidx.compose.ui.graphics.Color.Black, androidx.compose.ui.graphics.Color.White), 0.001)
        assertEquals(1.0, Contraste.razao(androidx.compose.ui.graphics.Color.White, androidx.compose.ui.graphics.Color.White), 0.001)
    }

    private companion object {
        const val EPSILON = 0.005
    }
}
