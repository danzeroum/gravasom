package com.gravasom

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import com.gravasom.core.audio.engine.RecordingState
import com.gravasom.core.service.MediaProbe
import com.gravasom.core.service.RecordingService
import com.gravasom.core.service.RecordingServiceState
import com.gravasom.core.service.RecordingStorage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Aceitacao (Given/When/Then): atalho do Tile/widget so envia START. Tocar de novo com a gravacao em curso nao pode
 * criar um segundo gravador (microfone aberto duas vezes, `.tmp` orfao); Parar entrega um unico arquivo reproduzivel.
 */
class TileGravacaoTest {

    @get:Rule
    val permissoes: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)

    private lateinit var antes: Set<String>

    private fun arquivosDeGravacao(): Set<String> =
        RecordingStorage.dir(Suporte.contexto).listFiles { f -> f.name.startsWith("gravacao_") }
            ?.map { it.name }?.toSet().orEmpty()

    @Before
    fun preparar() {
        ControleServico.reiniciar()
        antes = arquivosDeGravacao()
    }

    @After
    fun limpar() = ControleServico.reiniciar()

    @Test
    fun dadoGravandoQuandoOStartChegaDeNovoEntaoSoExisteUmaGravacao() {
        ControleServico.enviar(RecordingService.ACTION_START, "WAV")
        assertTrue("nao entrou em Recording: ${RecordingServiceState.state.value}", ControleServico.esperarGravando())

        // Given gravando / When o Tile (ou widget) envia START novamente, varias vezes
        repeat(3) { ControleServico.enviar(RecordingService.ACTION_START, "WAV") }
        Thread.sleep(1_500)

        // Then continua gravando, em primeiro plano como microfone, com UM arquivo novo em disco
        assertTrue("saiu de Recording: ${RecordingServiceState.state.value}", ControleServico.gravando())
        assertTrue(ControleServico.emPrimeiroPlanoComoMicrofone())
        val novos = arquivosDeGravacao() - antes
        assertEquals("START duplicado criou mais de um gravador: $novos", 1, novos.size)

        // When para / Then um unico arquivo, reproduzivel
        ControleServico.enviar(RecordingService.ACTION_STOP)
        assertTrue(
            "nao chegou a Saved: ${RecordingServiceState.state.value}",
            Suporte.aguardar { RecordingServiceState.state.value is RecordingState.Saved },
        )
        val salvo = File((RecordingServiceState.state.value as RecordingState.Saved).path)
        assertTrue("arquivo salvo nao reproduz: $salvo", MediaProbe.isPlayable(salvo))
        val orfaos = (RecordingStorage.dir(Suporte.contexto).listFiles { f -> f.name.endsWith(".tmp") }?.toList()).orEmpty()
        assertTrue("sobrou .tmp orfao: $orfaos", orfaos.isEmpty())
    }
}
