// Ponte JNI fina sobre Mp3Encoder (nucleo C++ testado no host). Sem logica de codificacao aqui.
#include <jni.h>

#include <cstdint>
#include <memory>

#include "mp3_encoder.h"

using gravasom::Mp3Encoder;

namespace {
Mp3Encoder* From(jlong handle) { return reinterpret_cast<Mp3Encoder*>(handle); }

// Executa `fn(ptr, capacidade)` sobre o ByteArray `out` e libera sem copia de volta em caso de erro.
template <typename Fn>
jint WithBytes(JNIEnv* env, jbyteArray out, Fn fn) {
  if (out == nullptr) return -1;
  jbyte* bytes = env->GetByteArrayElements(out, nullptr);
  if (bytes == nullptr) return -1;
  const jint cap = env->GetArrayLength(out);
  const jint written = fn(reinterpret_cast<uint8_t*>(bytes), cap);
  env->ReleaseByteArrayElements(out, bytes, written > 0 ? 0 : JNI_ABORT);
  return written;
}
}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_gravasom_core_audio_android_mp3_LameBridge_nativeInit(
    JNIEnv*, jobject, jint sample_rate, jint channels, jint bitrate_kbps, jint quality) {
  std::unique_ptr<Mp3Encoder> enc = Mp3Encoder::Create(sample_rate, channels, bitrate_kbps, quality);
  return reinterpret_cast<jlong>(enc.release());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_gravasom_core_audio_android_mp3_LameBridge_nativeEncode(
    JNIEnv* env, jobject, jlong handle, jshortArray pcm, jint frames, jbyteArray out) {
  Mp3Encoder* enc = From(handle);
  if (enc == nullptr || pcm == nullptr || frames < 0) return -1;
  // `frames` = amostras por canal: o array precisa ter frames * canais shorts, senao o LAME leria alem dele.
  if (static_cast<jlong>(env->GetArrayLength(pcm)) < static_cast<jlong>(frames) * enc->Channels()) return -1;
  jshort* samples = env->GetShortArrayElements(pcm, nullptr);
  if (samples == nullptr) return -1;
  const jint written = WithBytes(env, out, [&](uint8_t* buf, jint cap) {
    return enc->Encode(reinterpret_cast<const int16_t*>(samples), frames, buf, cap);
  });
  env->ReleaseShortArrayElements(pcm, samples, JNI_ABORT);
  return written;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_gravasom_core_audio_android_mp3_LameBridge_nativeFlush(
    JNIEnv* env, jobject, jlong handle, jbyteArray out) {
  Mp3Encoder* enc = From(handle);
  if (enc == nullptr) return -1;
  return WithBytes(env, out, [&](uint8_t* buf, jint cap) { return enc->Flush(buf, cap); });
}

extern "C" JNIEXPORT jint JNICALL
Java_com_gravasom_core_audio_android_mp3_LameBridge_nativeTagFrame(
    JNIEnv* env, jobject, jlong handle, jbyteArray out) {
  Mp3Encoder* enc = From(handle);
  if (enc == nullptr) return -1;
  return WithBytes(env, out, [&](uint8_t* buf, jint cap) { return enc->GetTagFrame(buf, cap); });
}

extern "C" JNIEXPORT void JNICALL
Java_com_gravasom_core_audio_android_mp3_LameBridge_nativeClose(JNIEnv*, jobject, jlong handle) {
  delete From(handle);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_gravasom_core_audio_android_mp3_LameBridge_nativeLiveHandles(JNIEnv*, jobject) {
  return Mp3Encoder::LiveInstances();
}
