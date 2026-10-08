package com.gravasom.spike.whisper

/** Ligacao JNI com libgravasom_whisper.so (whisper.cpp). Sem tela: so o necessario para medir. */
object WhisperBridge {
    init {
        System.loadLibrary("gravasom_whisper")
    }

    external fun nativeInit(path: String): Long
    external fun nativeFree(handle: Long)
    external fun nativeTranscribe(handle: Long, pcm: FloatArray, threads: Int, language: String): String?
    external fun nativeSystemInfo(): String
}
