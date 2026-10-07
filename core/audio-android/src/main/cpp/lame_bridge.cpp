// Stub JNI para o LAME. O LAME nao esta vendorizado (ADR-002).
// nativeInit retorna 0, mantendo o MP3 atras de feature flag.
#include <jni.h>
#include <android/log.h>

extern "C" JNIEXPORT jlong JNICALL
Java_com_gravasom_core_audio_android_mp3_LameBridge_nativeInit(
    JNIEnv*, jobject, jint, jint, jint, jint) { return 0L; }

extern "C" JNIEXPORT jint JNICALL
Java_com_gravasom_core_audio_android_mp3_LameBridge_nativeEncode(
    JNIEnv*, jobject, jlong, jshortArray, jint, jbyteArray) { return -1; }

extern "C" JNIEXPORT jint JNICALL
Java_com_gravasom_core_audio_android_mp3_LameBridge_nativeFlush(
    JNIEnv*, jobject, jlong, jbyteArray) { return -1; }

extern "C" JNIEXPORT void JNICALL
Java_com_gravasom_core_audio_android_mp3_LameBridge_nativeClose(
    JNIEnv*, jobject, jlong) {}
