package com.gravasom.core.service

import android.media.MediaExtractor
import android.media.MediaFormat
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

    fun isPlayable(file: File): Boolean = (durationMillis(file) ?: 0L) > 0L

    private const val MICROS_PER_MILLI = 1000L
}
