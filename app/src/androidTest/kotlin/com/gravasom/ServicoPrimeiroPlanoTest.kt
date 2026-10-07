package com.gravasom

import android.Manifest
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.gravasom.core.audio.engine.RecordingState
import com.gravasom.core.service.MediaProbe
import com.gravasom.core.service.RecordingService
import com.gravasom.core.service.RecordingServiceState
import com.gravasom.core.service.RecordingStorage
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Fase 4: foregroundServiceType=microphone sobrevive a rotacao e a ida para segundo plano. */
class ServicoPrimeiroPlanoTest {

    @get:Rule(order = 0)
    val permissoes: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val tela = ActivityScenarioRule(MainActivity::class.java)

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun preparar() = ControleServico.reiniciar()

    @After
    fun limpar() {
        device.setOrientationNatural()
        ControleServico.reiniciar()
    }

    @Test
    fun sobreviveARotacaoEAoSegundoPlano() {
        ControleServico.enviar(RecordingService.ACTION_START, "WAV")
        assertTrue("nao entrou em Recording: ${RecordingServiceState.state.value}", ControleServico.esperarGravando())
        verificarPrimeiroPlano("apos iniciar")

        device.setOrientationLeft()
        Thread.sleep(2_000)
        verificarPrimeiroPlano("apos rotacao")

        device.setOrientationNatural()
        device.pressHome()
        Thread.sleep(4_000)
        verificarPrimeiroPlano("em segundo plano")

        ControleServico.enviar(RecordingService.ACTION_STOP)
        assertTrue("nao chegou a Saved: ${RecordingServiceState.state.value}",
            Suporte.aguardar { RecordingServiceState.state.value is RecordingState.Saved })
        val salvo = File((RecordingServiceState.state.value as RecordingState.Saved).path)
        assertTrue("arquivo salvo nao reproduz: $salvo", MediaProbe.isPlayable(salvo))
        assertTrue(salvo.parentFile == RecordingStorage.dir(Suporte.contexto))
    }

    private fun verificarPrimeiroPlano(momento: String) {
        val dump = ControleServico.dumpsysServico()
        File(Suporte.testsOut(), "dumpsys_${momento.replace(' ', '_')}.txt").writeText(dump)
        assertTrue("gravacao parou $momento: ${RecordingServiceState.state.value}", ControleServico.gravando())
        assertTrue("servico fora do primeiro plano microphone $momento:\n$dump", ControleServico.emPrimeiroPlanoComoMicrofone())
    }
}
