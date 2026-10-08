package com.gravasom

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.metrics.ShareChannel
import com.gravasom.core.domain.metrics.UsageEventType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** ADR-011: metricas locais so com opt-in, esquema fechado, e a tela "Seus numeros" offline. */
class MetricasLocaisTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val metrics get() = UiSuporte.app.usageMetrics
    private val dao get() = UiSuporte.app.database.usageEventDao()

    @Before
    fun preparar() = UiSuporte.limparTudo()

    private fun total(): Int = runBlocking { dao.all().size }

    @Test
    fun desativadoNaoGravaEReativadoOsContadoresVoltamASubir() = runBlocking {
        metrics.record(UsageEventType.BUSCA_USADA)
        assertEquals("desligado: nenhum evento", 0, total())
        UiSuporte.app.preferences.setTelemetryOptIn(true)
        metrics.record(UsageEventType.BUSCA_USADA)
        metrics.record(UsageEventType.BUSCA_USADA)
        assertEquals(2, metrics.count(UsageEventType.BUSCA_USADA).first())
        UiSuporte.app.preferences.setTelemetryOptIn(false)
        metrics.record(UsageEventType.BUSCA_USADA)
        assertEquals("desligar de novo congela", 2, metrics.count(UsageEventType.BUSCA_USADA).first())
        UiSuporte.app.preferences.setTelemetryOptIn(true)
        metrics.record(UsageEventType.BUSCA_USADA)
        assertEquals(3, metrics.count(UsageEventType.BUSCA_USADA).first())
    }

    @Test
    fun nenhumEventoContemNomeDeArquivoOuAudio() {
        runBlocking { UiSuporte.app.preferences.setTelemetryOptIn(true) }
        val segredo = "SEGREDO_Entrevista_Joao"
        val gravacao = UiSuporte.inserirGravacao(segredo, segundos = 1.0)
        // fluxo real: buscar pelo nome, compartilhar e registrar todos os tipos de evento
        compose.onNodeWithTag("aba_gravacoes").performClick()
        compose.onNodeWithTag("campo_busca").performTextInput("SEGREDO")
        UiSuporte.aguardar(compose) { UiSuporte.textos(compose, "titulo_item") == listOf(segredo) }
        UiSuporte.app.files.shareIntent(gravacao)
        runBlocking {
            UsageEventType.entries.forEach { metrics.record(it, value = 1234L, label = RecordingFormat.M4A) }
            metrics.record(UsageEventType.COMPARTILHADO, label = ShareChannel.SALVAR_EM)
        }
        val linhas = runBlocking { dao.all() }
        assertTrue("sem eventos", linhas.isNotEmpty())
        val tipos = UsageEventType.entries.map { it.name }.toSet()
        val rotulos = (RecordingFormat.entries.map { it.name } + ShareChannel.entries.map { it.name }).toSet()
        linhas.forEach { e ->
            assertTrue("tipo fora do enum: ${e.type}", e.type in tipos)
            assertTrue("rotulo fora dos enums: ${e.label}", e.label == null || e.label in rotulos)
        }
        val csv = runBlocking { metrics.exportCsv() }
        listOf("SEGREDO", "Joao", ".wav", ".m4a", ".mp3", "/", "content:").forEach { proibido ->
            assertFalse("CSV contem '$proibido'", csv.contains(proibido))
        }
        assertEquals("tipo,momento_ms,valor,rotulo", csv.lines().first())
    }

    @Test
    fun seusNumerosFuncionaOfflineELigaOuDesligaOsContadores() {
        UiSuporte.inserirGravacao("Uma", 3.0)
        UiSuporte.inserirGravacao("Duas", 5.0)
        compose.onNodeWithTag("aba_ajustes").performClick()
        compose.onNodeWithTag("abrir_numeros").performClick()
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "num_gravacoes") }
        assertEquals(listOf("2"), UiSuporte.textos(compose, "num_gravacoes"))
        assertEquals(listOf("0:00:08"), UiSuporte.textos(compose, "num_tempo_total"))
        assertEquals(listOf("WAV"), UiSuporte.textos(compose, "num_formato"))
        assertTrue("sem opt-in deve avisar que o registro esta desligado", UiSuporte.existe(compose, "registro_desligado"))
        assertFalse(UiSuporte.existe(compose, "num_iniciadas"))
        runBlocking { UiSuporte.app.preferences.setTelemetryOptIn(true) }
        UiSuporte.aguardar(compose) { UiSuporte.existe(compose, "num_iniciadas") }
    }
}
