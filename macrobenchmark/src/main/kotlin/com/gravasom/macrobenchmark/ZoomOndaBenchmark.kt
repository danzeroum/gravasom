package com.gravasom.macrobenchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pinca na forma de onda de uma gravacao de 1 hora (Onda 1): mede o tempo de quadro do zoom.
 *
 * A gravacao de 1 h e os marcadores vem do receptor de semeadura do build `benchmark` (app/src/benchmark), que usa o mesmo
 * analisador de producao. O criterio e 60 fps: p90 do tempo de CPU por quadro <= 16,6 ms. No emulador do CI (renderizacao por
 * software) os numeros so INFORMAM (o job anexa o JSON do benchmark); quem VALIDA e o aparelho real, rodando com
 * `-Pandroid.testInstrumentationRunnerArguments.gravasom.fps.enforce=true` (docs/uso.md, roteiro da Onda 1).
 */
@RunWith(AndroidJUnit4::class)
class ZoomOndaBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    private val device: UiDevice get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun pincaNaOndaDeUmaHora() {
        val seed = device.executeShellCommand("am broadcast -a com.gravasom.benchmark.SEED -n com.gravasom/.benchmark.SeedReceiver")
        check("result=-1" in seed || "Broadcast completed" in seed) { "semeadura falhou: $seed" }
        rule.measureRepeated(
            packageName = "com.gravasom",
            metrics = listOf(FrameTimingMetric()),
            iterations = ITERATIONS,
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                device.findObject(By.res("aba_gravacoes"))?.click() ?: device.findObject(By.desc("aba_gravacoes"))?.click()
                device.wait(Until.hasObject(By.res("item_gravacao")), WAIT_MS)
                device.findObject(By.res("item_gravacao")).click()
                device.wait(Until.hasObject(By.res("mini_posicao")), WAIT_MS)
                device.findObject(By.res("mini_play_pause"))?.click() // pausa: mede so o zoom
            },
        ) {
            val onda = device.wait(Until.findObject(By.res("mini_posicao")), WAIT_MS)
            repeat(PINCHES) {
                onda.pinchOpen(PERCENT, SPEED)
                onda.pinchClose(PERCENT, SPEED)
            }
        }
    }

    private companion object {
        const val ITERATIONS = 5
        const val PINCHES = 4
        const val PERCENT = 0.6f
        const val SPEED = 800
        const val WAIT_MS = 10_000L
    }
}
