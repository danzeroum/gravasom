package com.gravasom.core.audio.android.recorder

import android.media.MediaRecorder
import android.os.Build
import com.gravasom.core.audio.android.InputRouting
import com.gravasom.core.audio.engine.LevelMeter
import com.gravasom.core.domain.audio.AudioRecorder
import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.math.log10
import java.io.File

/**
 * Grava M4A (MP4/AAC-LC) com MediaRecorder. Pausa e retomada sao nativas (API 24+).
 * O VU nao vem das amostras: o MediaRecorder nao as expoe.
 */
class MediaRecorderM4aRecorder : AudioRecorder {

    override val format: RecordingFormat = RecordingFormat.M4A

    private var recorder: MediaRecorder? = null
    private var outputPath: String = ""

    override suspend fun prepare(config: RecordingConfig, outputPath: String): Result<Unit> = runCatching {
        this.outputPath = outputPath
        File(outputPath).parentFile?.mkdirs()
        File("$outputPath.tmp").delete()
        val r = MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(config.sampleRateHz)
        r.setAudioChannels(config.channels)
        r.setAudioEncodingBitRate(config.bitrateKbps * 1000)
        r.setOutputFile("$outputPath.tmp")
        // MediaRecorder.setPreferredDevice existe a partir da API 28; antes, o sistema escolhe a entrada.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) InputRouting.preferred?.let { r.setPreferredDevice(it) }
        r.prepare()
        recorder = r
    }

    override suspend fun start(): Result<Unit> = runCatching {
        requireNotNull(recorder) { "prepare() nao chamado" }.start()
    }

    override suspend fun pause(): Result<Unit> = runCatching {
        requireNotNull(recorder) { "prepare() nao chamado" }.pause()
    }

    override suspend fun resume(): Result<Unit> = runCatching {
        requireNotNull(recorder) { "prepare() nao chamado" }.resume()
    }

    override suspend fun stop(): Result<String> = runCatching {
        recorder?.stop()
        recorder?.release()
        recorder = null
        check(File("$outputPath.tmp").renameTo(File(outputPath))) { "falha ao finalizar $outputPath" }
        outputPath
    }

    /** VU do M4A: o MediaRecorder nao expoe amostras, mas informa a amplitude maxima desde a ultima leitura. */
    override fun level(): Flow<Float> = flow {
        while (currentCoroutineContext().isActive) {
            val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
            emit(amplitudeToDb(amplitude))
            delay(LEVEL_POLL_MS)
        }
    }

    override fun release() {
        runCatching { recorder?.release() }
        recorder = null
    }

    private fun amplitudeToDb(amplitude: Int): Float =
        if (amplitude <= 0) LevelMeter.MIN_DB else (20.0 * log10(amplitude / MAX_AMPLITUDE)).toFloat().coerceAtLeast(LevelMeter.MIN_DB)

    private companion object {
        const val MAX_AMPLITUDE = 32_767.0
        const val LEVEL_POLL_MS = 100L
    }
}
