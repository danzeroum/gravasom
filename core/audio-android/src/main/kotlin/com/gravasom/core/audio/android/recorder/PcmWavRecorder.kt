package com.gravasom.core.audio.android.recorder

import android.media.AudioRecord
import com.gravasom.core.audio.engine.LevelMeter
import com.gravasom.core.audio.engine.WavHeaderWriter
import com.gravasom.core.domain.audio.AudioRecorder
import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Grava WAV (PCM 16 bits) com AudioRecord. Escreve o cabecalho RIFF no inicio,
 * grava as amostras incrementalmente e corrige o cabecalho ao parar.
 */
class PcmWavRecorder(
    private val scope: CoroutineScope,
    private val captureFactory: (RecordingConfig) -> PcmCapture = { cfg ->
        PcmCapture(cfg.sampleRateHz, cfg.channels, cfg.inputSource)
    },
) : AudioRecorder {

    override val format: RecordingFormat = RecordingFormat.WAV

    private val levelFlow = MutableStateFlow(LevelMeter.MIN_DB)
    private var job: Job? = null
    private var capture: PcmCapture? = null
    private var file: RandomAccessFile? = null
    private var outputPath: String = ""
    private var config: RecordingConfig = RecordingConfig(format = RecordingFormat.WAV)

    override suspend fun prepare(config: RecordingConfig, outputPath: String): Result<Unit> = runCatching {
        this.config = config
        this.outputPath = outputPath
        val f = File(tempPathFor(outputPath))
        f.parentFile?.mkdirs()
        val raf = RandomAccessFile(f, "rw")
        raf.setLength(0)
        raf.write(
            WavHeaderWriter.build(
                sampleRate = config.sampleRateHz,
                channels = config.channels,
                bitsPerSample = 16,
                dataSizeBytes = 0L,
            ),
        )
        file = raf
        capture = captureFactory(config)
    }

    override suspend fun start(): Result<Unit> = runCatching {
        val cap = requireNotNull(capture) { "prepare() nao chamado" }
        val record = cap.open()
        record.startRecording()
        job = scope.launch(Dispatchers.IO) { captureLoop(record, cap.bufferSizeBytes) }
    }

    private fun CoroutineScope.captureLoop(record: AudioRecord, bufferSizeBytes: Int) {
        val buffer = ShortArray(bufferSizeBytes / 2)
        val bytes = ByteArray(buffer.size * 2)
        while (isActive) {
            val read = record.read(buffer, 0, buffer.size)
            if (read > 0 && !writeBlock(buffer, bytes, read)) return
            // read < 0 (ERROR_DEAD_OBJECT, ex.: fone Bluetooth desconectado): sem pausa o laco girava a 100% de CPU sem gravar nada.
            if (read < 0) Thread.sleep(RETRY_MS)
        }
    }

    /** Escreve [read] amostras; false quando o arquivo ja foi fechado por stop()/release() (nao e erro de gravacao). */
    private fun CoroutineScope.writeBlock(buffer: ShortArray, bytes: ByteArray, read: Int): Boolean {
        levelFlow.value = LevelMeter.rmsDb(buffer.copyOf(read))
        for (k in 0 until read) {
            bytes[2 * k] = (buffer[k].toInt() and 0xFF).toByte()
            bytes[2 * k + 1] = ((buffer[k].toInt() shr 8) and 0xFF).toByte()
        }
        val raf = file ?: return false
        return try {
            raf.write(bytes, 0, read * 2)
            true
        } catch (e: IOException) {
            if (isActive) throw e else false
        }
    }

    override suspend fun pause(): Result<Unit> = runCatching { stopWriter() }

    override suspend fun resume(): Result<Unit> = start()

    override suspend fun stop(): Result<String> = runCatching {
        stopWriter()
        val raf = file
        if (raf != null) {
            val total = raf.length()
            val dataSize = minOf(WavHeaderWriter.dataSizeFor(total), WavHeaderWriter.MAX_DATA_BYTES)
            raf.seek(0)
            raf.write(
                WavHeaderWriter.build(
                    sampleRate = config.sampleRateHz,
                    channels = config.channels,
                    bitsPerSample = 16,
                    dataSizeBytes = dataSize,
                ),
            )
            raf.close()
        }
        file = null
        capture = null
        check(File(tempPathFor(outputPath)).renameTo(File(outputPath))) { "falha ao finalizar $outputPath" }
        outputPath
    }

    /**
     * Para a captura e ESPERA o escritor terminar antes de qualquer outro uso do arquivo. Sem a espera, o ultimo bloco podia
     * ser escrito depois do cabecalho final (WAV corrompido) ou num arquivo ja fechado (EBADF).
     * `capture.close()` vem antes do join porque e ele que destrava o `AudioRecord.read()` bloqueado.
     */
    private suspend fun stopWriter() {
        val writer = job
        writer?.cancel()
        capture?.close()
        writer?.join()
        job = null
    }

    override fun level(): Flow<Float> = levelFlow.asStateFlow()

    override fun release() {
        job?.cancel()
        capture?.close()
        runCatching { file?.close() }
        file = null
    }

    private companion object {
        fun tempPathFor(path: String): String = "$path.tmp"
        const val RETRY_MS = 20L
    }
}
