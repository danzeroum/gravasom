package com.gravasom.core.dsp

/** Ligacao JNI com libgravasom_dsp.so. Use [NativeBlockProcessor]; isto e baixo nivel. */
internal object NativeDsp {

    @Volatile
    private var loaded = false

    fun ensureLoaded(): Boolean {
        if (loaded) return true
        loaded = runCatching { System.loadLibrary("gravasom_dsp") }.isSuccess
        return loaded
    }

    external fun nativeCreate(sampleRate: Int): Long
    external fun nativeAddStep(handle: Long, type: Int, params: FloatArray): Boolean
    external fun nativeSetBypass(handle: Long, bypass: Boolean)
    external fun nativeSetIntensity(handle: Long, intensity: Float)
    external fun nativeReset(handle: Long)
    external fun nativeLatency(handle: Long): Int
    external fun nativeProcess(handle: Long, buffer: FloatArray, offset: Int, length: Int): Int
    external fun nativeDestroy(handle: Long)
    external fun nativeLiveChains(): Int
}
