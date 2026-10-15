package com.gravasom.core.service

import com.gravasom.core.audio.engine.RecordingError
import com.gravasom.core.audio.engine.RecordingState
import com.gravasom.core.audio.engine.WavHeaderWriter
import com.gravasom.core.audio.engine.WavRepair
import java.io.File
import java.io.RandomAccessFile

/**
 * Recupera gravacoes interrompidas por kill do processo. No proximo inicio,
 * varre os temporarios: repara cabecalho WAV e finaliza os demais.
 */
class BootRecovery(
    private val tempDir: File,
    private val isPlayable: (File) -> Boolean = { true },
    private val onUnrecoverable: (File) -> Unit = {},
    /** Ignora temporarios modificados ha menos que isso: pode ser uma gravacao ao vivo (ex.: app aberto pelo Tile). */
    private val minAgeMillis: Long = 0L,
    private val onRecovered: (File) -> Unit,
) {

    fun run() {
        val agora = System.currentTimeMillis()
        val temporaries = tempDir.listFiles { f -> f.name.endsWith(".tmp") && agora - f.lastModified() >= minAgeMillis } ?: return
        for (tmp in temporaries) {
            val finalFile = File(tmp.parentFile, tmp.name.removeSuffix(".tmp"))
            val renamed = when {
                finalFile.name.endsWith(".wav") -> repairWav(tmp, finalFile)
                else -> finalizeOther(tmp, finalFile)
            }
            // Rename falhou (disco cheio, destino ocupado): o .tmp e o unico exemplar do audio. Mantem para a proxima inicializacao
            // em vez de cair no ramo que apaga.
            if (!renamed) continue
            when {
                finalFile.exists() && isPlayable(finalFile) -> onRecovered(finalFile)
                else -> {
                    finalFile.delete()
                    tmp.delete()
                    onUnrecoverable(finalFile)
                }
            }
        }
    }

    private fun repairWav(tmp: File, target: File): Boolean {
        RandomAccessFile(tmp, "rw").use { raf ->
            val total = raf.length()
            if (total > WavHeaderWriter.HEADER_SIZE) {
                // O formato vem do proprio cabecalho gravado no prepare(); so o dataSize fica defasado.
                val fmt = ByteArray(WavHeaderWriter.HEADER_SIZE).also { raf.seek(0); raf.readFully(it) }
                val buf = java.nio.ByteBuffer.wrap(fmt).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                val repaired = WavRepair.headerFor(
                    totalFileBytes = total - (total - WavHeaderWriter.HEADER_SIZE) % blockAlign(buf),
                    sampleRate = buf.getInt(SAMPLE_RATE_OFFSET),
                    channels = buf.getShort(CHANNELS_OFFSET).toInt(),
                    bitsPerSample = buf.getShort(BITS_OFFSET).toInt(),
                )
                if (repaired != null) {
                    raf.seek(0)
                    raf.write(repaired.bytes)
                    raf.setLength(WavHeaderWriter.HEADER_SIZE + repaired.dataSizeBytes)
                }
            }
        }
        return tmp.renameTo(target)
    }

    private fun blockAlign(header: java.nio.ByteBuffer): Int =
        (header.getShort(CHANNELS_OFFSET) * header.getShort(BITS_OFFSET) / BITS_PER_BYTE).coerceAtLeast(1)

    private fun finalizeOther(tmp: File, target: File): Boolean = tmp.renameTo(target)

    private companion object {
        const val CHANNELS_OFFSET = 22
        const val SAMPLE_RATE_OFFSET = 24
        const val BITS_OFFSET = 34
        const val BITS_PER_BYTE = 8
    }

    /** Estado sintetico apos recuperacao: a gravacao aparece como salva. */
    fun recoveredState(path: String): RecordingState = RecordingState.Saved(path)
}
