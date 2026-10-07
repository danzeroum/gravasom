package com.gravasom

import android.Manifest
import android.appwidget.AppWidgetManager
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Fase 7: Tile e widget acionando a gravacao de ponta a ponta. */
class SuperficiesTest {

    @get:Rule(order = 0)
    val permissoes: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val tela = ActivityScenarioRule(MainActivity::class.java)

    private val instr = InstrumentationRegistry.getInstrumentation()
    private val tile = "com.gravasom/com.gravasom.core.service.tile.RecordTileService"

    @Before
    fun preparar() = ControleServico.reiniciar()

    @After
    fun limpar() {
        Suporte.shell("cmd statusbar remove-tile $tile")
        ControleServico.reiniciar()
    }

    @Test
    fun tileIniciaAGravacao() {
        val log = StringBuilder()
        log.appendLine("add: " + Suporte.shell("cmd statusbar add-tile $tile"))
        Thread.sleep(1_500)
        // Tentativa 1: clique direto. Tentativa 2: com o painel de Ajustes Rapidos aberto (a tile so "escuta" assim em algumas APIs).
        log.appendLine("click1: " + Suporte.shell("cmd statusbar click-tile $tile"))
        var tentativa = 1
        var ok = ControleServico.esperarGravando(5_000)
        if (!ok) {
            tentativa = 2
            log.appendLine("expandir: " + Suporte.shell("cmd statusbar expand-settings"))
            Thread.sleep(2_500)
            log.appendLine("click2: " + Suporte.shell("cmd statusbar click-tile $tile"))
            ok = ControleServico.esperarGravando(8_000)
        }
        log.appendLine("estado final: ${com.gravasom.core.service.RecordingServiceState.state.value}")
        if (!ok) log.appendLine("ui: " + Suporte.shell("uiautomator dump /dev/stdout").take(4_000))
        java.io.File(Suporte.testsOut(), "tile_shell.txt").writeText(log.toString())
        Suporte.medida("tile", "tentativa_que_funcionou", if (ok) tentativa else 0)
        assertTrue("tile nao iniciou a gravacao:\n$log", ok)
        assertTrue(ControleServico.emPrimeiroPlanoComoMicrofone())
        Suporte.shell("cmd statusbar collapse")
    }

    /**
     * Caminho de um usuario: pede ao launcher para fixar o widget na tela inicial, confirma o dialogo e toca no widget.
     * (O host de teste em processo nao serve: o clique de servico exige o tratador do launcher.)
     */
    @Test
    fun widgetIniciaAGravacao() {
        val contexto = Suporte.contexto
        val device = UiDevice.getInstance(instr)
        val awm = AppWidgetManager.getInstance(contexto)
        val provider = awm.installedProviders.firstOrNull { it.provider.packageName == contexto.packageName }
        assertNotNull("widget nao registrado", provider)
        assertTrue("launcher nao suporta fixar widgets", awm.isRequestPinAppWidgetSupported)
        try {
            device.pressHome()
            device.waitForIdle()
            awm.requestPinAppWidget(provider!!.provider, null, null)
            val botoes = By.clazz("android.widget.Button").text(Pattern.compile("(?i)(add|adicionar).*"))
            device.wait(Until.findObject(botoes), DIALOGO_MS)?.click()
            val widget = device.wait(Until.findObject(By.text("Gravar")), WIDGET_MS)
            assertNotNull("widget nao apareceu na tela inicial", widget)
            widget!!.click()
            assertTrue("widget nao iniciou a gravacao: ${com.gravasom.core.service.RecordingServiceState.state.value}",
                ControleServico.esperarGravando())
        } finally {
            val pasta = Suporte.testsOut()
            runCatching { device.takeScreenshot(java.io.File(pasta, "widget_tela.png")) }
            runCatching { device.dumpWindowHierarchy(java.io.File(pasta, "widget_ui.xml")) }
        }
    }

    private companion object {
        const val DIALOGO_MS = 10_000L
        const val WIDGET_MS = 15_000L
    }
}
