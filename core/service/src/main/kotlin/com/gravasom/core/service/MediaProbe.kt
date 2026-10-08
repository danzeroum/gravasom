package com.gravasom.core.service

import android.media.MediaExtractor
import android.media.MediaFormat
import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import java.io.File

/** Mede a duracao de um arquivo de audio pelo MediaExtractor (a mesma via do player). */
object MediaProbe {

    /** Duracao em ms da primeira faixa de audio, ou null se o arquivo nao abre. */
    fun durationMillis(file: File): Long? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            (0 until extractor.trackCount)
                .map { extractor.getTrackFormat(it) }
                .firstOrNull { it.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?.takeIf { it.containsKey(MediaFormat.KEY_DURATION) }
                ?.getLong(MediaFormat.KEY_DURATION)
                ?.div(MICROS_PER_MILLI)
        } catch (_: java.io.IOException) {
            null
        } catch (_: RuntimeException) {
            null
        } finally {
            extractor.release()
        }
    }

    /**
     * Descreve o arquivo como uma [RecordingConfig] (para o nome automatico): taxa e canais do cabecalho; bitrate do cabecalho
     * ou, sem ele, tamanho / duracao, arredondado para um valor usual.
     */
    fun config(file: File, format: RecordingFormat, durationMillis: Long): RecordingConfig {
        val track = runCatching {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(file.absolutePath)
                (0 until extractor.trackCount).map { extractor.getTrackFormat(it) }
                    .firstOrNull { it.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
            } finally {
                extractor.release()
            }
        }.getOrNull()
        val rate = track?.takeIf { it.containsKey(MediaFormat.KEY_SAMPLE_RATE) }?.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = track?.takeIf { it.containsKey(MediaFormat.KEY_CHANNEL_COUNT) }?.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val measured = track?.takeIf { it.containsKey(MediaFormat.KEY_BIT_RATE) }?.getInteger(MediaFormat.KEY_BIT_RATE)
            ?: if (durationMillis > 0) (file.length() * BITS_PER_BYTE * MILLIS_PER_SECOND / durationMillis).toInt() else DEFAULT_BITRATE_BPS
        return RecordingConfig(
            format = format,
            sampleRateHz = (rate ?: DEFAULT_RATE).coerceIn(RATE_RANGE),
            channels = (channels ?: 1).coerceIn(CHANNEL_RANGE),
            bitrateKbps = nearestBitrate(measured / MILLIS_PER_SECOND.toInt()),
        )
    }

    private fun nearestBitrate(kbps: Int): Int = USUAL_BITRATES.minBy { kotlin.math.abs(it - kbps) }

    fun isPlayable(file: File): Boolean = (durationMillis(file) ?: 0L) > 0L

    private const val MICROS_PER_MILLI = 1000L
    private const val BITS_PER_BYTE = 8
    private const val MILLIS_PER_SECOND = 1000L
    private const val DEFAULT_BITRATE_BPS = 128_000
    private const val DEFAULT_RATE = 44_100
    private val RATE_RANGE = 8_000..48_000
    private val CHANNEL_RANGE = 1..2
    private val USUAL_BITRATES = listOf(32, 48, 64, 96, 128, 160, 192, 256, 320)
}
