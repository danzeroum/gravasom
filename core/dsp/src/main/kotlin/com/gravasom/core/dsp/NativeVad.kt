package com.gravasom.core.dsp

/** Ligacao JNI do WebRTC VAD (libgravasom_dsp.so). Use [WebRtcVad]; isto e baixo nivel. */
internal object NativeVad {
    external fun nativeCreate(mode: Int): Long
    external fun nativeProcess(handle: Long, pcm: ShortArray, offset: Int, length: Int, flags: ByteArray): Int
    external fun nativeFlush(handle: Long, flags: ByteArray): Int
    external fun nativeDestroy(handle: Long)
    external fun nativeLiveInstances(): Int
}
