package com.gravasom.core.audio.engine

/**
 * Corte sem perdas de WAV: copia os bytes do intervalo e reescreve o cabecalho.
 * Puro (trabalha com ByteArray), testavel so com o JDK.
 */
object WavTrimmer {

    fun trim(
        source: ByteArray,
        range: TrimRange,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
    ): ByteArray {
        require(range.isValid) { "intervalo de corte invalido" }
        require(source.size >= WavHeaderWriter.HEADER_SIZE) { "fonte menor que o cabecalho" }
        val startByte = WavHeaderWriter.HEADER_SIZE +
            TrimRange.byteOffsetFor(range.startMillis, sampleRate, channels, bitsPerSample).toInt()
        val endByte = WavHeaderWriter.HEADER_SIZE +
            TrimRange.byteOffsetFor(range.endMillis, sampleRate, channels, bitsPerSample).toInt()
        val from = startByte.coerceIn(WavHeaderWriter.HEADER_SIZE, source.size)
        val to = endByte.coerceIn(from, source.size)
        val dataSize = (to - from).toLong()
        val header = WavHeaderWriter.build(sampleRate, channels, bitsPerSample, dataSize)
        return header + source.copyOfRange(from, to)
    }

    fun durationMillis(wav: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): Long {
        val dataSize = WavHeaderWriter.dataSizeFor(wav.size.toLong())
        val bytesPerSecond = sampleRate.toLong() * channels * (bitsPerSample / 8)
        if (bytesPerSecond == 0L) return 0L
        return dataSize * 1000L / bytesPerSecond
    }
}
