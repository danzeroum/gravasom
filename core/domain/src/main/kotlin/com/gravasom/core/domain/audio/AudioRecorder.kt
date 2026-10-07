package com.gravasom.core.domain.audio

import kotlinx.coroutines.flow.Flow

/** Contrato comum a todas as estrategias de gravacao (Strategy por formato). */
interface AudioRecorder {

    val format: RecordingFormat

    suspend fun prepare(config: RecordingConfig, outputPath: String): Result<Unit>

    suspend fun start(): Result<Unit>

    suspend fun pause(): Result<Unit>

    suspend fun resume(): Result<Unit>

    suspend fun stop(): Result<String>

    /** Nivel em dB (0 a -60), amostrado a 20 Hz. */
    fun level(): Flow<Float>

    fun release()
}
