package com.gravasom.core.audio.android.recorder

import android.media.MediaRecorder
import com.gravasom.core.audio.engine.LevelMeter
import com.gravasom.core.domain.audio.AudioRecorder
import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Grava M4A (MP4/AAC-LC) com MediaRecorder. Pausa e retomada sao nativas (API 24+).
 * O VU nao vem das amostras: o MediaRecorder nao as expoe.
 */
class MediaRecorderM4aRecorder : AudioRecorder {

    override val format: RecordingFormat = RecordingFormat.M4A

    private val levelFlow = MutableStateFlow(LevelMeter.MIN_DB)
    private var recorder: MediaRecorder? = null
    private var outputPath: String = ""

    override suspend fun prepare(config: RecordingConfig, outputPath: String): Result<Unit> = runCatching {
        this.outputPath = outputPath
        File(outputPath).parentFile?.mkdirs()
        val r = MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(config.sampleRateHz)
        r.setAudioChannels(config.channels)
        r.setAudioEncodingBitRate(config.bitrateKbps * 1000)
        r.setOutputFile(outputPath)
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
        outputPath
    }

    override fun level(): Flow<Float> = levelFlow.asStateFlow()

    override fun release() {
        runCatching { recorder?.release() }
        recorder = null
    }
}
