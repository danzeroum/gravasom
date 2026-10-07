package com.gravasom

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.gravasom.core.audio.engine.WavHeaderWriter
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.sin

/** Utilidades compartilhadas pelos testes instrumentados. */
object Suporte {

    const val TAG = "GRAVASOM_MEDIDA"

    val contexto: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** Pasta de saida dos artefatos de teste (baixada pelo CI com adb pull). */
    fun testsOut(): File {
        // O AGP recolhe additionalTestOutputDir apos os testes, mesmo com o app desinstalado.
        val agp = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val base = agp?.let(::File) ?: contexto.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        return File(base, "tests-out").apply { mkdirs() }
    }

    /** Registra uma medida no logcat e em medidas.csv (artefato do CI). */
    fun medida(teste: String, nome: String, valor: Any) {
        Log.i(TAG, "$teste;$nome;$valor")
        File(testsOut(), "medidas.csv").appendText("$teste;$nome;$valor\n")
    }

    /** WAV PCM 16 bits mono com seno de [freqHz] e [segundos] de duracao exata. */
    fun escreverWavSeno(arquivo: File, segundos: Double, freqHz: Double = 1000.0, taxa: Int = 44_100): File {
        val amostras = (segundos * taxa).toInt()
        arquivo.parentFile?.mkdirs()
        RandomAccessFile(arquivo, "rw").use { raf ->
            raf.setLength(0)
            raf.write(WavHeaderWriter.build(taxa, 1, 16, amostras * 2L))
            val pcm = ByteArray(amostras * 2)
            for (i in 0 until amostras) {
                val v = (sin(2 * PI * freqHz * i / taxa) * 0.5 * Short.MAX_VALUE).toInt()
                pcm[2 * i] = (v and 0xFF).toByte()
                pcm[2 * i + 1] = ((v shr 8) and 0xFF).toByte()
            }
            raf.write(pcm)
        }
        return arquivo
    }

    /** Espera [condicao] ficar verdadeira ate [timeoutMs]; retorna se ficou. */
    fun aguardar(timeoutMs: Long = 10_000, passoMs: Long = 50, condicao: () -> Boolean): Boolean {
        val fim = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < fim) {
            if (condicao()) return true
            Thread.sleep(passoMs)
        }
        return condicao()
    }

    fun shell(comando: String): String {
        val ua = InstrumentationRegistry.getInstrumentation().uiAutomation
        return ua.executeShellCommand(comando).use { fd ->
            java.io.FileInputStream(fd.fileDescriptor).bufferedReader().readText()
        }
    }
}
