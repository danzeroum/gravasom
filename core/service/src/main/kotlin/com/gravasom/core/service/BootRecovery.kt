package com.gravasom.core.service

import com.gravasom.core.audio.engine.RecordingError
import com.gravasom.core.audio.engine.RecordingState
import com.gravasom.core.audio.engine.WavHeaderWriter
import java.io.File
import java.io.RandomAccessFile

/**
 * Recupera gravacoes interrompidas por kill do processo. No proximo inicio,
 * varre os temporarios: repara cabecalho WAV e finaliza os demais.
 */
class BootRecovery(
    private val tempDir: File,
    private val onRecovered: (File) -> Unit,
) {

    fun run() {
        val temporaries = tempDir.listFiles { f -> f.name.endsWith(".tmp") } ?: return
        for (tmp in temporaries) {
            val finalFile = File(tmp.parentFile, tmp.name.removeSuffix(".tmp"))
            when {
                finalFile.name.endsWith(".wav") -> repairWav(tmp, finalFile)
                else -> finalizeOther(tmp, finalFile)
            }
            onRecovered(finalFile)
        }
    }

    private fun repairWav(tmp: File, target: File) {
        RandomAccessFile(tmp, "rw").use { raf ->
            val total = raf.length()
            if (total >= WavHeaderWriter.HEADER_SIZE) {
                val dataSize = WavHeaderWriter.dataSizeFor(total)
                raf.seek(0)
                raf.write(
                    WavHeaderWriter.build(
                        sampleRate = 44_100,
                        channels = 1,
                        bitsPerSample = 16,
                        dataSizeBytes = dataSize,
                    ),
                )
            }
        }
        tmp.renameTo(target)
    }

    private fun finalizeOther(tmp: File, target: File) {
        tmp.renameTo(target)
    }

    /** Estado sintetico apos recuperacao: a gravacao aparece como salva. */
    fun recoveredState(path: String): RecordingState = RecordingState.Saved(path)
}
