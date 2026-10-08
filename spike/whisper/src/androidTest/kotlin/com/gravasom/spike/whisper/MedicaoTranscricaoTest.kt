package com.gravasom.spike.whisper

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.concurrent.thread

/**
 * Mede, no aparelho, o custo de transcrever 10 min de fala pt-BR com o modelo EMBUTIDO neste APK:
 * tempo, fator de tempo real, bateria gasta, temperatura e estado termico, memoria. Escreve spike_<modelo>.json em
 * tests-out/. O WER sai do texto, calculado no host (scripts/asr_wer.py) contra a transcricao de referencia.
 *
 * Valido so com o aparelho FORA da tomada (o contador de carga sobe se estiver carregando) e com a tela apagada ou
 * o brilho no minimo; o JSON diz se a medida e valida (`carregando`).
 * Argumento opcional: -e gravasom.spike.maxSeconds 60 (fumaca rapida, usada pela CI no emulador).
 */
class MedicaoTranscricaoTest {

    private val instr = InstrumentationRegistry.getInstrumentation()
    private val app: Context get() = instr.targetContext

    private fun temModelo(): Boolean = runCatching { app.assets.open(Transcricao.NOME_MODELO).close(); true }.getOrDefault(false)

    @Test
    fun apkNaoPedeInternet() {
        val info = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
        val pedidas = info.requestedPermissions?.toList().orEmpty()
        assertFalse("o spike nao pode ter INTERNET: $pedidas", pedidas.any { it.endsWith("INTERNET") })
    }

    @Test
    fun transcreve10MinutosEMede() {
        assumeTrue("sem modelo embutido (use -PmodeloWhisper=...)", temModelo())
        val wav = File(app.cacheDir, "clip.wav")
        instr.context.assets.open("clip10min.wav").use { i -> wav.outputStream().use { i.copyTo(it) } }
        val max = InstrumentationRegistry.getArguments().getString("gravasom.spike.maxSeconds")?.toIntOrNull()
        var pcm = Transcricao.lerWav16k(wav)
        if (max != null) pcm = pcm.copyOf(minOf(pcm.size, max * TAXA))
        val segundosDeAudio = pcm.size.toDouble() / TAXA

        val antes = bateria()
        val tempos = mutableListOf<Int>()
        var termicoMax = termico()
        @Volatile var correndo = true
        val amostrador = thread(name = "amostrador-termico") {
            while (correndo) {
                tempos += bateria().temperaturaDecimos
                termicoMax = maxOf(termicoMax, termico())
                Thread.sleep(AMOSTRA_MS)
            }
        }

        Transcricao(app).use { t ->
            val t0 = SystemClock.elapsedRealtime()
            val bytes = t.abrirModeloEmbutido()
            val abrirMs = SystemClock.elapsedRealtime() - t0
            val t1 = SystemClock.elapsedRealtime()
            val segmentos = t.transcrever(pcm)
            val transcreverMs = SystemClock.elapsedRealtime() - t1
            correndo = false
            amostrador.join()
            val depois = bateria()
            val texto = segmentos.joinToString(" ") { it.texto }
            val json = relatorio(
                Medida(antes, depois, tempos, termicoMax, bytes, segundosDeAudio, abrirMs, transcreverMs, segmentos.size, texto),
            )
            val pasta = File(instr.targetContext.getExternalFilesDir(null), "tests-out").apply { mkdirs() }
            val dest = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let { File(it).apply { mkdirs() } } ?: pasta
            File(dest, "spike_${System.getProperty("gravasom.modelo") ?: "modelo"}.json").writeText(json)
            File(pasta, "spike_ultimo.json").writeText(json)
            assertTrue("nada foi transcrito", texto.isNotBlank())
        }
    }

    private class Medida(
        val antes: Bateria, val depois: Bateria, val temperaturas: List<Int>, val termicoMax: Int, val modeloBytes: Long,
        val audioSegundos: Double, val abrirMs: Long, val transcreverMs: Long, val segmentos: Int, val texto: String,
    )

    private fun relatorio(m: Medida): String {
        fun aspas(t: String) = t.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
        val tempMax = (m.temperaturas.maxOrNull() ?: m.antes.temperaturaDecimos) / DECIMOS
        val campos = linkedMapOf<String, Any>(
            "dispositivo" to "\"${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT}, ${Build.SUPPORTED_ABIS.first()})\"",
            "sistema_whisper" to "\"${aspas(WhisperBridge.nativeSystemInfo().trim())}\"",
            "modelo_bytes" to m.modeloBytes,
            "audio_s" to "%.1f".format(m.audioSegundos),
            "abrir_modelo_ms" to m.abrirMs,
            "transcrever_ms" to m.transcreverMs,
            "fator_tempo_real" to "%.3f".format(m.transcreverMs / MILLIS / m.audioSegundos),
            "carregando" to (m.antes.carregando || m.depois.carregando),
            "bateria_pct_antes" to m.antes.percentual, "bateria_pct_depois" to m.depois.percentual,
            "carga_uAh_antes" to m.antes.cargaUah, "carga_uAh_depois" to m.depois.cargaUah,
            "gasto_uAh" to (m.antes.cargaUah - m.depois.cargaUah),
            "temp_c_antes" to m.antes.temperaturaDecimos / DECIMOS, "temp_c_max" to tempMax, "temp_c_depois" to m.depois.temperaturaDecimos / DECIMOS,
            "estado_termico_max" to m.termicoMax,
            "memoria_nativa_mib" to Debug.getNativeHeapAllocatedSize() / MIB,
            "pico_rss_mib" to picoRssMib(),
            "segmentos" to m.segmentos,
            "texto" to "\"${aspas(m.texto)}\"",
        )
        return campos.entries.joinToString(",\n", "{\n", "\n}\n") { "  \"${it.key}\": ${it.value}" }
    }

    private class Bateria(val percentual: Int, val cargaUah: Long, val temperaturaDecimos: Int, val carregando: Boolean)

    private fun bateria(): Bateria {
        val bm = app.getSystemService(BatteryManager::class.java)
        val sticky: Intent? = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return Bateria(
            percentual = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            cargaUah = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
            temperaturaDecimos = sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0,
            carregando = bm.isCharging,
        )
    }

    /** 0 = nenhum ... 6 = desligamento (PowerManager.THERMAL_STATUS_*); -1 antes da API 29. */
    private fun termico(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) app.getSystemService(PowerManager::class.java).currentThermalStatus else -1

    private fun picoRssMib(): Long =
        File("/proc/self/status").readLines().firstOrNull { it.startsWith("VmHWM:") }?.filter { it.isDigit() }?.toLongOrNull()?.div(KIB) ?: -1L

    private companion object {
        const val TAXA = 16_000
        const val AMOSTRA_MS = 5_000L
        const val MIB = 1024L * 1024L
        const val KIB = 1024L
        const val DECIMOS = 10.0
        const val MILLIS = 1000.0
    }
}
