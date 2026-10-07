package com.gravasom.core.audio.android.mp3

/**
 * Ponte JNI para o LAME (libmp3lame.so, LGPL, biblioteca separada; ver ADR-007).
 * Se a biblioteca nativa nao carregar, o MP3 fica indisponivel sem derrubar o app.
 */
object LameBridge {

    @Volatile
    private var loaded = false

    fun ensureLoaded(): Boolean {
        if (loaded) return true
        loaded = runCatching {
            System.loadLibrary("mp3lame")
            System.loadLibrary("gravasom_mp3")
        }.isSuccess
        return loaded
    }

    /** Retorna um handle nativo (0 se parametros invalidos ou falha). Exige nativeClose. */
    external fun nativeInit(sampleRate: Int, channels: Int, bitrateKbps: Int, quality: Int): Long

    /** [frames] = amostras por canal. Retorna bytes MP3 escritos em [out], ou < 0 em erro. */
    external fun nativeEncode(handle: Long, pcm: ShortArray, frames: Int, out: ByteArray): Int

    external fun nativeFlush(handle: Long, out: ByteArray): Int

    /** Quadro Xing/Info final: sobrescreve o primeiro quadro do arquivo. */
    external fun nativeTagFrame(handle: Long, out: ByteArray): Int

    external fun nativeClose(handle: Long)

    /** Handles ainda vivos no lado nativo; 0 prova ausencia de vazamento. */
    external fun nativeLiveHandles(): Int

    /** Tamanho seguro do buffer de saida para [frames] amostras (regra do LAME: 1,25x + 7200). */
    fun outputCapacityFor(frames: Int): Int = (frames * OUT_FACTOR).toInt() + OUT_MARGIN

    private const val OUT_FACTOR = 1.25
    private const val OUT_MARGIN = 7200
}
