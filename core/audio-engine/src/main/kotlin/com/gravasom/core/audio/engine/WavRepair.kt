package com.gravasom.core.audio.engine

/**
 * Calcula o reparo do cabecalho de um WAV interrompido, sem tocar em disco:
 * pura, testavel so com o JDK. O servico aplica o resultado com RandomAccessFile.
 */
object WavRepair {

    data class Header(
        val bytes: ByteArray,
        val dataSizeBytes: Long,
    )

    /**
     * Gera o cabecalho correto para um arquivo WAV de [totalFileBytes] bytes.
     * Retorna null se o arquivo for menor que o cabecalho.
     */
    fun headerFor(
        totalFileBytes: Long,
        sampleRate: Int = 44_100,
        channels: Int = 1,
        bitsPerSample: Int = 16,
    ): Header? {
        if (totalFileBytes < WavHeaderWriter.HEADER_SIZE) return null
        val dataSize = WavHeaderWriter.dataSizeFor(totalFileBytes)
        return Header(
            bytes = WavHeaderWriter.build(sampleRate, channels, bitsPerSample, dataSize),
            dataSizeBytes = dataSize,
        )
    }

    /** Duracao em milissegundos a partir do dataSize e do formato. */
    fun durationMillis(dataSizeBytes: Long, sampleRate: Int, channels: Int, bitsPerSample: Int): Long {
        val bytesPerSecond = sampleRate.toLong() * channels * (bitsPerSample / 8)
        if (bytesPerSecond == 0L) return 0L
        return dataSizeBytes * 1000L / bytesPerSecond
    }
}
