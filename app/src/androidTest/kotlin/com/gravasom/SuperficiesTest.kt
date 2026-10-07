package com.gravasom

import android.Manifest
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
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

    @Test
    fun widgetIniciaAGravacao() {
        val contexto = Suporte.contexto
        val awm = AppWidgetManager.getInstance(contexto)
        val ua = instr.uiAutomation
        ua.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
        val host = AppWidgetHost(contexto, HOST_ID)
        var id = -1
        try {
            val info = awm.installedProviders.firstOrNull { it.provider.packageName == contexto.packageName }
            assertNotNull("widget nao registrado", info)
            host.startListening()
            id = host.allocateAppWidgetId()
            assertTrue("bind negado", awm.bindAppWidgetIdIfAllowed(id, info!!.provider))
            lateinit var visao: AppWidgetHostView
            instr.runOnMainSync { visao = host.createView(contexto, id, info) }
            contexto.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .setComponent(ComponentName(contexto, "com.gravasom.core.service.widget.RecordWidgetReceiver"))
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id)),
            )
            var clicavel: View? = null
            assertTrue("widget nao renderizou/nao tem alvo clicavel", Suporte.aguardar(15_000) {
                instr.runOnMainSync { clicavel = achar(visao) }
                clicavel != null
            })
            instr.runOnMainSync { clicavel!!.performClick() }
            assertTrue("widget nao iniciou a gravacao", ControleServico.esperarGravando())
        } finally {
            if (id >= 0) host.deleteAppWidgetId(id)
            host.stopListening()
            ua.dropShellPermissionIdentity()
        }
    }

    private fun achar(v: View): View? {
        if (v !is AppWidgetHostView && v.hasOnClickListeners()) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) achar(v.getChildAt(i))?.let { return it }
        return null
    }

    private companion object {
        const val HOST_ID = 4242
    }
}
