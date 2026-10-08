package com.gravasom.core.domain.audio

/** Parametros de gravacao escolhidos pelo usuario. */
data class RecordingConfig(
    val format: RecordingFormat = RecordingFormat.M4A,
    val sampleRateHz: Int = 44_100,
    val channels: Int = 1,
    val bitrateKbps: Int = 128,
    val inputSource: InputSource = InputSource.MIC,
    /** "Pular silencio" ao OUVIR (ADR-014): o gravador nao a usa; o player a le de PreferencesRepository. */
    val skipSilence: Boolean = false,
    val noiseSuppressor: Boolean = true,
    val autoGainControl: Boolean = true,
) {
    init {
        require(sampleRateHz in 8_000..48_000) { "sampleRateHz fora da faixa 8-48 kHz" }
        require(channels in 1..2) { "channels deve ser 1 ou 2" }
    }
}

enum class InputSource {
    MIC,
    UNPROCESSED,
    BLUETOOTH,
    USB,
}
