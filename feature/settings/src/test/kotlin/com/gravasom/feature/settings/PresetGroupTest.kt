package com.gravasom.feature.settings

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.gravasom.core.domain.preset.FormatPreset
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.test.assertTextEquals as assertText

/** Presets de gravacao na tela de ajustes: escolher, resumo e o que ligam por padrao. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pt-rBR")
class PresetGroupTest {

    @get:Rule
    val compose = createComposeRule()

    private val chosen = mutableListOf<FormatPreset>()

    private fun show(selected: FormatPreset?) {
        chosen.clear()
        compose.setContent { MaterialTheme { androidx.compose.foundation.layout.Column { PresetGroup(selected, chosen::add) } } }
    }

    private fun text(id: Int, vararg args: Any) = ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    @Test
    fun `tocar num preset o escolhe`() {
        show(null)
        compose.onNodeWithTag("preset_CLASS").performClick()
        compose.onNodeWithTag("preset_MUSIC").performClick()
        assertEquals(listOf(FormatPreset.CLASS, FormatPreset.MUSIC), chosen)
    }

    @Test
    fun `o preset escolhido aparece selecionado com o resumo de taxa bitrate e canais`() {
        show(FormatPreset.MUSIC)
        compose.onNodeWithTag("preset_MUSIC").assertIsSelected()
        compose.onNodeWithTag("preset_resumo").assertText("48 kHz · 192 kbps · estéreo")
    }

    @Test
    fun `aula liga os efeitos por padrao`() {
        show(FormatPreset.CLASS)
        compose.onNodeWithTag("preset_efeitos").assertText(text(R.string.settings_preset_effects_on))
    }

    @Test
    fun `musica avisa que nao tem efeitos de voz`() {
        show(FormatPreset.MUSIC)
        compose.onNodeWithTag("preset_efeitos").assertText(text(R.string.settings_preset_effects_off))
    }

    @Test
    fun `natureza grava em banda cheia estereo e vem sem efeitos de voz`() {
        show(FormatPreset.NATURE)
        compose.onNodeWithTag("preset_NATURE").assertIsSelected()
        compose.onNodeWithTag("preset_resumo").assertText("48 kHz · 256 kbps · estéreo")
        compose.onNodeWithTag("preset_efeitos").assertText(text(R.string.settings_preset_effects_off))
    }

    @Test
    fun `agua tambem grava em banda cheia e nao liga efeitos de voz`() {
        show(FormatPreset.WATER)
        compose.onNodeWithTag("preset_WATER").assertIsSelected()
        compose.onNodeWithTag("preset_resumo").assertText("48 kHz · 256 kbps · estéreo")
        compose.onNodeWithTag("preset_efeitos").assertText(text(R.string.settings_preset_effects_off))
    }

    @Test
    fun `sem preset a tela diz que esta personalizado e nao mostra resumo`() {
        show(null)
        compose.onNodeWithTag("preset_efeitos").assertText(text(R.string.settings_preset_custom))
        assertEquals(0, compose.onAllNodesWithTagCount("preset_resumo"))
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTagCount(tag: String) =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().size
}
