package com.gravasom.core.audio.android.recorder

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
        job = scope.launch(Dispatchers.IO) {
            val buffer = ShortArray(cap.bufferSizeBytes / 2)
            val bytes = ByteArray(buffer.size * 2)
            while (isActive) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    levelFlow.value = LevelMeter.rmsDb(buffer.copyOf(read))
                    var i = 0
                    for (s in buffer) {
                        if (i / 2 >= read * 2) break
                        bytes[i++] = (s.toInt() and 0xFF).toByte()
                        bytes[i++] = ((s.toInt() shr 8) and 0xFF).toByte()
                    }
                    file?.write(bytes, 0, read * 2)
                }
            }
        }
    }

    override suspend fun pause(): Result<Unit> = runCatching {
        capture?.close()
        job?.cancel()
    }

    override suspend fun resume(): Result<Unit> = start()

    override suspend fun stop(): Result<String> = runCatching {
        job?.cancel()
        capture?.close()
        val raf = file
        if (raf != null) {
            val total = raf.length()
            val dataSize = WavHeaderWriter.dataSizeFor(total)
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

    override fun level(): Flow<Float> = levelFlow.asStateFlow()

    override fun release() {
        job?.cancel()
        capture?.close()
        runCatching { file?.close() }
        file = null
    }

    private companion object {
        fun tempPathFor(path: String): String = "$path.tmp"
    }
}
