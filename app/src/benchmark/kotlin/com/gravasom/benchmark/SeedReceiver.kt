package com.gravasom.benchmark

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gravasom.GravasomApplication
import com.gravasom.core.audio.engine.WavHeaderWriter
import com.gravasom.core.domain.audio.RecordingFormat
import com.gravasom.core.domain.marker.Marker
import com.gravasom.core.domain.model.Recording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.sin

/**
 * Semeia UMA gravacao de 1 hora (WAV 8 kHz mono: trechos de fala sintetica separados por pausas) com 12 marcadores e a
 * analise real (decodificacao + VAD) para o Macrobenchmark da onda. Existe so no build `benchmark`.
 */
class SeedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as GravasomApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (app.repo.observeAll().first().none { it.title == TITLE }) seed(context, app)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun seed(context: Context, app: GravasomApplication) {
        val file = File(context.filesDir, "recordings/benchmark_1h.wav")
        file.parentFile?.mkdirs()
        writeHour(file)
        val base = Recording(
            title = TITLE, path = file.path, format = RecordingFormat.WAV, durationMillis = HOUR_MS,
            sizeBytes = file.length(), createdAtMillis = System.currentTimeMillis(),
        )
        val id = app.repo.insert(base)
        repeat(MARKERS) { i ->
            app.markers.insert(Marker(recordingId = id, positionMillis = (i + 1) * HOUR_MS / (MARKERS + 1), label = "M${i + 1}", note = "benchmark"))
        }
        app.analyzer.ensure(id, file, persist = true)
    }

    private fun writeHour(file: File) {
        val rate = RATE
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.write(WavHeaderWriter.build(rate, 1, 16, HOUR_SECONDS.toLong() * rate * 2))
            val chunk = ByteArray(rate * 2)
            for (second in 0 until HOUR_SECONDS) {
                // 40 s de fala sintetica a cada 60 s; o resto e silencio
                val speech = second % 60 < 40
                for (i in 0 until rate) {
                    val t = (second * rate + i).toDouble() / rate
                    val v = if (speech) {
                        (0.6 + 0.4 * sin(2 * PI * 4 * t)) * (9_000 * sin(2 * PI * 120 * t) + 6_000 * sin(2 * PI * 700 * t))
                    } else {
                        0.0
                    }
                    val q = v.toInt().coerceIn(-32_768, 32_767)
                    chunk[2 * i] = (q and 0xFF).toByte()
                    chunk[2 * i + 1] = ((q shr 8) and 0xFF).toByte()
                }
                raf.write(chunk)
            }
        }
    }

    private companion object {
        const val TITLE = "Benchmark 1 h"
        const val HOUR_SECONDS = 3_600
        const val HOUR_MS = 3_600_000L
        const val RATE = 8_000
        const val MARKERS = 12
    }
}
