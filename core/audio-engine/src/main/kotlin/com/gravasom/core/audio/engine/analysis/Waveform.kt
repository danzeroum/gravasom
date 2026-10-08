package com.gravasom.core.audio.engine.analysis

/**
 * Onda calculada ao salvar a gravacao: 1 byte (0..255) por janela de 10 ms com o maior valor absoluto de qualquer canal.
 * 1 hora = 360 mil bytes. Imutavel; [columns] e o caminho quente do desenho (zoom e rolagem).
 */
class Waveform(val peaks: ByteArray, val peaksPerSecond: Int = PEAKS_PER_SECOND) {

    val durationMillis: Long get() = peaks.size * MILLIS_PER_SECOND / peaksPerSecond

    /** Pico da janela [index], 0..255. */
    fun peak(index: Int): Int = peaks[index].toInt() and BYTE_MASK

    /**
     * Amplitude 0..1 de cada uma de [columns] colunas entre [startMillis] e [endMillis] (maximo das janelas da coluna).
     * Fora da gravacao a coluna e 0. Se a coluna e mais fina que a janela, repete o valor da janela.
     */
    fun columns(startMillis: Long, endMillis: Long, columns: Int, into: FloatArray = FloatArray(columns)): FloatArray {
        require(columns > 0 && endMillis > startMillis) { "intervalo ou colunas invalidos" }
        val span = (endMillis - startMillis).toDouble()
        val pps = peaksPerSecond.toDouble()
        for (c in 0 until columns) {
            val from = (startMillis + span * c / columns) * pps / MILLIS_PER_SECOND
            val to = (startMillis + span * (c + 1) / columns) * pps / MILLIS_PER_SECOND
            val first = kotlin.math.floor(from).toInt()
            val lastExclusive = maxOf(kotlin.math.ceil(to).toInt(), first + 1)
            var max = 0
            var i = maxOf(first, 0)
            val stop = minOf(lastExclusive, peaks.size)
            while (i < stop) {
                val v = peaks[i].toInt() and BYTE_MASK
                if (v > max) max = v
                i++
            }
            into[c] = max / MAX_PEAK
        }
        return into
    }

    companion object {
        const val PEAKS_PER_SECOND = 100
        const val MAX_PEAK = 255f
        private const val BYTE_MASK = 0xFF
        private const val MILLIS_PER_SECOND = 1000L
    }
}

/** Acumula PCM 16 bits intercalado (qualquer taxa) e produz a [Waveform]. Aceita pedacos de qualquer tamanho. */
class WaveformBuilder(sampleRate: Int, private val channels: Int) {

    private val windowFrames = sampleRate / Waveform.PEAKS_PER_SECOND
    private var out = ByteArray(INITIAL)
    private var size = 0
    private var framesInWindow = 0
    private var windowPeak = 0

    init {
        require(windowFrames > 0 && channels > 0) { "taxa ou canais invalidos" }
    }

    fun add(pcm: ShortArray, offset: Int, length: Int) {
        var i = offset
        val end = offset + length - (length % channels)
        while (i < end) {
            var peak = 0
            for (c in 0 until channels) {
                val v = pcm[i + c].toInt()
                val a = if (v < 0) -v else v
                if (a > peak) peak = a
            }
            if (peak > windowPeak) windowPeak = peak
            i += channels
            if (++framesInWindow == windowFrames) flushWindow()
        }
    }

    fun finish(): Waveform {
        if (framesInWindow > 0) flushWindow()
        return Waveform(out.copyOf(size))
    }

    private fun flushWindow() {
        if (size == out.size) out = out.copyOf(out.size * 2)
        // 32768 -> 255 (fundo de escala negativo cabe); arredonda para cima para que qualquer som > 0 apareca.
        out[size++] = ((windowPeak * MAX + FULL_SCALE - 1) / FULL_SCALE).coerceAtMost(MAX).toByte()
        windowPeak = 0
        framesInWindow = 0
    }

    private companion object {
        const val INITIAL = 1 shl 12
        const val MAX = 255
        const val FULL_SCALE = 32_768
    }
}
