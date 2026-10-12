package com.gravasom.feature.transcribe.model

/** Ligacao JNI com libgravasom_transcribe.so (whisper.cpp). Mesma interface do spike; aqui o resultado e usado em producao. */
internal object WhisperBridge {
    init {
        System.loadLibrary("gravasom_transcribe")
    }

    external fun nativeInit(path: String): Long
    external fun nativeFree(handle: Long)
    external fun nativeTranscribe(handle: Long, pcm: FloatArray, threads: Int, language: String): String?
}
