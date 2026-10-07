package com.gravasom.core.audio.android.recorder

import com.gravasom.core.audio.android.mp3.LameBridge
import com.gravasom.core.audio.engine.LevelMeter
import com.gravasom.core.domain.audio.AudioRecorder
import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Grava MP3 (PCM + LAME via JNI). Atrás de feature flag: se a biblioteca nativa
 * nao estiver presente, prepare() falha com Mp3NotAvailable, sem quebrar o app.
 */
class PcmMp3Recorder : AudioRecorder {

    override val format: RecordingFormat = RecordingFormat.MP3

    private val levelFlow = MutableStateFlow(LevelMeter.MIN_DB)
    private var handle: Long = 0L

    override suspend fun prepare(config: RecordingConfig, outputPath: String): Result<Unit> = runCatching {
        if (!LameBridge.ensureLoaded()) throw Mp3NotAvailable()
        handle = LameBridge.nativeInit(config.sampleRateHz, config.channels, config.bitrateKbps, 5)
        if (handle == 0L) throw Mp3NotAvailable()
    }

    override suspend fun start(): Result<Unit> = Result.success(Unit)

    override suspend fun pause(): Result<Unit> = Result.success(Unit)

    override suspend fun resume(): Result<Unit> = Result.success(Unit)

    override suspend fun stop(): Result<String> = Result.success("")

    override fun level(): Flow<Float> = levelFlow.asStateFlow()

    override fun release() {
        if (handle != 0L && LameBridge.ensureLoaded()) {
            runCatching { LameBridge.nativeClose(handle) }
        }
        handle = 0L
    }
}

class Mp3NotAvailable : IllegalStateException("Codificador MP3 (LAME) indisponivel nesta build")
