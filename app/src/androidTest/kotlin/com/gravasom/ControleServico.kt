package com.gravasom

import android.app.ActivityManager
import android.content.Intent
import androidx.core.content.ContextCompat
import com.gravasom.core.audio.engine.RecordingState
import com.gravasom.core.service.RecordingService
import com.gravasom.core.service.RecordingServiceState
import java.io.File

/** Comandos e consultas ao RecordingService usados pelos testes. */
object ControleServico {

    fun enviar(acao: String, formato: String? = null) {
        val intent = Intent(Suporte.contexto, RecordingService::class.java).setAction(acao)
        formato?.let { intent.putExtra(RecordingService.EXTRA_FORMAT, it) }
        if (acao == RecordingService.ACTION_START) {
            ContextCompat.startForegroundService(Suporte.contexto, intent)
        } else {
            Suporte.contexto.startService(intent)
        }
    }

    @Suppress("DEPRECATION")
    fun servicoVivo(): Boolean =
        Suporte.contexto.getSystemService(ActivityManager::class.java)
            .getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == RecordingService::class.java.name }

    fun gravando(): Boolean = RecordingServiceState.state.value is RecordingState.Recording

    fun esperarGravando(timeoutMs: Long = 10_000): Boolean = Suporte.aguardar(timeoutMs) { gravando() }

    fun esperarEncerrado(timeoutMs: Long = 10_000): Boolean = Suporte.aguardar(timeoutMs) { !servicoVivo() }

    /** Para o servico (se vivo) e devolve o estado global ao inicio. */
    fun reiniciar() {
        if (servicoVivo()) {
            enviar(RecordingService.ACTION_STOP)
            esperarEncerrado(15_000)
        }
        RecordingServiceState.update(RecordingState.Idle)
    }

    /** Trecho do `dumpsys activity services` referente ao RecordingService. */
    fun dumpsysServico(): String {
        val saida = Suporte.shell("dumpsys activity services com.gravasom")
        val linhas = saida.lines()
        val inicio = linhas.indexOfFirst { it.contains("ServiceRecord") && it.contains("RecordingService") }
        if (inicio < 0) return ""
        val fim = (inicio + 1 until linhas.size)
            .firstOrNull { linhas[it].contains("ServiceRecord") } ?: linhas.size
        return linhas.subList(inicio, fim).joinToString("\n")
    }

    fun emPrimeiroPlanoComoMicrofone(): Boolean {
        val bloco = dumpsysServico()
        return bloco.contains("isForeground=true") && Regex("0x0*80\\b").containsMatchIn(bloco)
    }

    fun gravacoesValidas(dir: File, ehPlayable: (File) -> Boolean): List<File> =
        dir.listFiles()?.filter { !it.name.endsWith(".tmp") && ehPlayable(it) }.orEmpty()
}
