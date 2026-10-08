package com.gravasom.spike.whisper

import android.content.Context
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Segmento transcrito, em ms do audio. */
data class Segmento(val inicioMs: Long, val fimMs: Long, val texto: String)

/** Carrega o modelo embutido no APK (assets/modelo.bin) e transcreve WAV 16 kHz mono 16 bits. */
class Transcricao(private val context: Context) : AutoCloseable {

    private var handle = 0L

    /** Copia o asset para o cache (uma vez) e abre o modelo. Devolve o tamanho do arquivo em bytes. */
    fun abrirModeloEmbutido(): Long {
        val destino = File(context.cacheDir, "modelo.bin")
        if (!destino.exists()) {
            context.assets.open(NOME_MODELO).use { entrada -> destino.outputStream().use { entrada.copyTo(it, BLOCO) } }
        }
        handle = WhisperBridge.nativeInit(destino.path)
        check(handle != 0L) { "whisper nao abriu o modelo" }
        return destino.length()
    }

    fun transcrever(pcm: FloatArray, threads: Int = 0): List<Segmento> {
        val texto = checkNotNull(WhisperBridge.nativeTranscribe(handle, pcm, threads, "pt")) { "whisper_full falhou" }
        return texto.lineSequence().filter { it.isNotBlank() }.map {
            val (a, b, t) = it.split('\t', limit = 3)
            Segmento(a.toLong(), b.toLong(), t.trim())
        }.toList()
    }

    override fun close() {
        if (handle != 0L) WhisperBridge.nativeFree(handle)
        handle = 0L
    }

    companion object {
        const val NOME_MODELO = "modelo.bin"
        private const val BLOCO = 1 shl 20

        /** WAV PCM 16 bits mono 16 kHz -> floats [-1, 1]. */
        fun lerWav16k(arquivo: File): FloatArray {
            val bytes = arquivo.readBytes()
            val dados = ByteBuffer.wrap(bytes, CABECALHO, bytes.size - CABECALHO).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            return FloatArray(dados.remaining()) { dados.get(it) / ESCALA }
        }

        private const val CABECALHO = 44
        private const val ESCALA = 32768f
    }
}
