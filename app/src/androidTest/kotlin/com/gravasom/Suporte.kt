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

    /**
     * WAV mono 16 bits de 16 kHz feito de trechos: (segundos, fala?). Fala = sinal harmonico (f0 120 Hz, formantes em 700 e 1200 Hz)
     * com amplitude modulada a 4 Hz, que o WebRTC VAD reconhece; silencio = zeros digitais.
     */
    fun escreverWavSegmentos(arquivo: File, vararg trechos: Pair<Double, Boolean>, taxa: Int = 16_000): File {
        val total = trechos.sumOf { (it.first * taxa).toInt() }
        arquivo.parentFile?.mkdirs()
        RandomAccessFile(arquivo, "rw").use { raf ->
            raf.setLength(0)
            raf.write(WavHeaderWriter.build(taxa, 1, 16, total * 2L))
            var n = 0
            for ((segundos, fala) in trechos) {
                val amostras = (segundos * taxa).toInt()
                val pcm = ByteArray(amostras * 2)
                if (fala) {
                    for (i in 0 until amostras) {
                        val t = (n + i).toDouble() / taxa
                        val envelope = 0.6 + 0.4 * sin(2 * PI * 4 * t)
                        val v = envelope * (9_000 * sin(2 * PI * 120 * t) + 6_000 * sin(2 * PI * 700 * t) + 4_000 * sin(2 * PI * 1_200 * t))
                        val q = v.toInt().coerceIn(-32_768, 32_767)
                        pcm[2 * i] = (q and 0xFF).toByte()
                        pcm[2 * i + 1] = ((q shr 8) and 0xFF).toByte()
                    }
                }
                raf.write(pcm)
                n += amostras
            }
        }
        return arquivo
    }

    /** M4A (AAC-LC, mono) com um seno, codificado pelo MediaCodec do sistema e gravado pelo MediaMuxer. */
    fun escreverM4aSeno(arquivo: File, segundos: Double, freqHz: Double = 1000.0, taxa: Int = 44_100): File {
        val formato = android.media.MediaFormat.createAudioFormat(android.media.MediaFormat.MIMETYPE_AUDIO_AAC, taxa, 1).apply {
            setInteger(android.media.MediaFormat.KEY_AAC_PROFILE, android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(android.media.MediaFormat.KEY_BIT_RATE, 64_000)
            setInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE, 1 shl 16)
        }
        arquivo.parentFile?.mkdirs()
        arquivo.delete()
        val codec = android.media.MediaCodec.createEncoderByType(android.media.MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(formato, null, null, android.media.MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val muxer = android.media.MediaMuxer(arquivo.path, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var faixa = -1
        val info = android.media.MediaCodec.BufferInfo()
        val total = (segundos * taxa).toInt()
        var enviado = 0
        var fimEntrada = false
        var fimSaida = false
        try {
            while (!fimSaida) {
                if (!fimEntrada) {
                    val i = codec.dequeueInputBuffer(10_000)
                    if (i >= 0) {
                        val buf = codec.getInputBuffer(i)!!
                        val n = minOf(total - enviado, buf.capacity() / 2, 4_096)
                        if (n <= 0) {
                            codec.queueInputBuffer(i, 0, 0, enviado * 1_000_000L / taxa, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            fimEntrada = true
                        } else {
                            buf.clear()
                            for (k in 0 until n) buf.putShort((sin(2 * PI * freqHz * (enviado + k) / taxa) * 0.5 * Short.MAX_VALUE).toInt().toShort())
                            codec.queueInputBuffer(i, 0, n * 2, enviado * 1_000_000L / taxa, 0)
                            enviado += n
                        }
                    }
                }
                val o = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    o == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> faixa = muxer.addTrack(codec.outputFormat).also { muxer.start() }
                    o >= 0 -> {
                        val buf = codec.getOutputBuffer(o)!!
                        if (info.flags and android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                        if (info.size > 0 && faixa >= 0) muxer.writeSampleData(faixa, buf, info)
                        codec.releaseOutputBuffer(o, false)
                        if (info.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) fimSaida = true
                    }
                }
            }
        } finally {
            runCatching { codec.stop() }
            codec.release()
            runCatching { muxer.stop() }
            muxer.release()
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
