package com.gravasom.core.audio.android.mp3

/**
 * Ponte JNI para o LAME. O codigo nativo NAO esta vendorizado (ADR-002).
 * Sem a biblioteca, nativeInit retorna 0 e o MP3 fica indisponivel, mantendo
 * a feature flag desligada por padrao.
 */
object LameBridge {

    @Volatile
    private var loaded = false

    fun ensureLoaded(): Boolean {
        if (loaded) return true
        loaded = runCatching { System.loadLibrary("gravasom_mp3") }.isSuccess
        return loaded
    }

    external fun nativeInit(sampleRate: Int, channels: Int, bitrateKbps: Int, quality: Int): Long
    external fun nativeEncode(handle: Long, pcm: ShortArray, frames: Int, out: ByteArray): Int
    external fun nativeFlush(handle: Long, out: ByteArray): Int
    external fun nativeClose(handle: Long)
}
