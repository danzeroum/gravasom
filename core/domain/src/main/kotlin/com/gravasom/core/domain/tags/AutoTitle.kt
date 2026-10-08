package com.gravasom.core.domain.tags

import com.gravasom.core.domain.audio.RecordingConfig
import com.gravasom.core.domain.audio.RecordingFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Nome automatico "data · preset". Ainda nao ha presets (Onda 2), entao o "preset" e a descricao
 * do formato escolhido. Quando houver transcricao (Onda 3), as primeiras palavras entram aqui.
 */
object AutoTitle {

    fun build(nowMillis: Long, config: RecordingConfig, locale: Locale, zone: ZoneId = ZoneId.systemDefault()): String {
        val pt = locale.language == "pt"
        val pattern = if (pt) "dd/MM/yyyy HH:mm" else "yyyy-MM-dd HH:mm"
        val date = DateTimeFormatter.ofPattern(pattern, locale).withZone(zone).format(Instant.ofEpochMilli(nowMillis))
        return "$date · ${preset(config, pt)}"
    }

    fun preset(config: RecordingConfig, pt: Boolean): String {
        val quality = when (config.format) {
            RecordingFormat.WAV -> "${config.sampleRateHz / KHZ} kHz"
            else -> "${config.bitrateKbps} kbps"
        }
        val channels = when {
            config.channels == 1 -> "mono"
            pt -> "estéreo"
            else -> "stereo"
        }
        return "${config.format.name} $quality $channels"
    }

    private const val KHZ = 1000
}
