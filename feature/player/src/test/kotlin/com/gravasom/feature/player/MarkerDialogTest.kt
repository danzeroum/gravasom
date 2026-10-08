package com.gravasom.feature.player

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.marker.MarkerNote
import com.gravasom.core.domain.marker.MarkerResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Nota de marcador: 140 caracteres salvam; 141 sao rejeitadas COM mensagem (pt-BR e en). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MarkerDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val marker = Marker(id = 7, recordingId = 1, positionMillis = 723_000, label = "", note = "")
    private var saved: Pair<String, String>? = null
    private var dismissed = false

    private var deleted = false

    private fun mostrar(deletable: Boolean = true) {
        saved = null
        dismissed = false
        deleted = false
        compose.setContent {
            MaterialTheme {
                MarkerDialog(
                    marker = marker,
                    onDismiss = { dismissed = true },
                    onSave = { note, label -> saved = note to label; MarkerResult.Saved(marker.id) },
                    onDelete = { deleted = true },
                    deletable = deletable,
                )
            }
        }
    }

    private fun texto(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    @Test
    fun `nota de 140 caracteres salva`() {
        mostrar()
        compose.onNodeWithTag("campo_nota_marcador").performTextInput("a".repeat(MarkerNote.MAX_LENGTH))
        compose.onNodeWithTag("salvar_marcador").assertIsEnabled()
        compose.onNodeWithTag("contador_nota", useUnmergedTree = true).assertTextEquals("140/140")
        compose.onNodeWithTag("erro_nota", useUnmergedTree = true).assertTextEquals("")
        compose.onNodeWithTag("salvar_marcador").performClick()
        compose.waitForIdle()
        assertEquals("a".repeat(140) to "", saved)
        assertTrue(dismissed)
    }

    @Test
    fun `nota de 141 caracteres e rejeitada com mensagem e nada e salvo`() {
        mostrar()
        compose.onNodeWithTag("campo_nota_marcador").performTextInput("a".repeat(MarkerNote.MAX_LENGTH + 1))
        compose.onNodeWithTag("salvar_marcador").assertIsNotEnabled()
        compose.onNodeWithTag("erro_nota", useUnmergedTree = true)
            .assertTextEquals(texto(R.string.player_marker_note_too_long, MarkerNote.MAX_LENGTH))
        compose.onNodeWithTag("contador_nota", useUnmergedTree = true).assertTextEquals("141/140")
        compose.onNodeWithTag("salvar_marcador").performClick()
        compose.waitForIdle()
        assertEquals(null, saved)
        assertTrue(!dismissed)
    }

    @Test
    fun `voltar para 140 apaga o erro e libera salvar`() {
        mostrar()
        compose.onNodeWithTag("campo_nota_marcador").performTextInput("a".repeat(141))
        compose.onNodeWithTag("salvar_marcador").assertIsNotEnabled()
        compose.onNodeWithTag("campo_nota_marcador").performTextReplacement("a".repeat(140))
        compose.onNodeWithTag("salvar_marcador").assertIsEnabled()
        compose.onNodeWithTag("erro_nota", useUnmergedTree = true).assertTextEquals("")
    }

    @Test
    @Config(sdk = [34], qualifiers = "pt-rBR")
    fun `a mensagem de nota longa sai em portugues`() {
        mostrar()
        compose.onNodeWithTag("campo_nota_marcador").performTextInput("a".repeat(141))
        compose.onNodeWithTag("erro_nota", useUnmergedTree = true).assertTextEquals("Nota longa demais (máximo 140). Encurte para salvar.")
    }

    @Test
    @Config(sdk = [34], qualifiers = "en")
    fun `a mensagem de nota longa sai em ingles`() {
        mostrar()
        compose.onNodeWithTag("campo_nota_marcador").performTextInput("a".repeat(141))
        compose.onNodeWithTag("erro_nota", useUnmergedTree = true).assertTextEquals("Note too long (max 140). Shorten it to save.")
    }

    @Test
    fun `cancelar um marcador novo fecha sem salvar e sem excluir`() {
        mostrar(deletable = false)
        compose.onNodeWithTag("campo_nota_marcador").performTextInput("nota digitada")
        compose.onNodeWithTag("cancelar_marcador").performClick()
        assertTrue(dismissed)
        assertEquals(null, saved)
        assertTrue(!deleted)
    }

    @Test
    fun `marcador novo ainda rascunho nao oferece excluir`() {
        mostrar(deletable = false)
        compose.onAllNodesWithTag("excluir_marcador").assertCountEquals(0)
    }

    @Test
    fun `marcador existente oferece excluir e avisa quem trata o desfazer`() {
        mostrar(deletable = true)
        compose.onNodeWithTag("excluir_marcador").performClick()
        assertTrue(deleted)
        assertTrue(dismissed)
    }
}
