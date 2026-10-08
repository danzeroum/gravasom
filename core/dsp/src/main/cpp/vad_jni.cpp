// Ponte JNI fina sobre gravasom::dsp::VadStream. Sem logica de VAD aqui.
#include <jni.h>

#include <atomic>
#include <vector>

#include "vad_stream.h"

using gravasom::dsp::VadStream;

namespace {
std::atomic<int> g_vads{0};
VadStream* From(jlong h) { return reinterpret_cast<VadStream*>(h); }
}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_gravasom_core_dsp_NativeVad_nativeCreate(JNIEnv*, jobject, jint mode) {
  auto* vad = new VadStream(mode);
  if (!vad->ok()) {
    delete vad;
    return 0;
  }
  g_vads.fetch_add(1);
  return reinterpret_cast<jlong>(vad);
}

// Devolve o numero de quadros escritos em `flags`, ou -1 se a capacidade nao basta.
extern "C" JNIEXPORT jint JNICALL
Java_com_gravasom_core_dsp_NativeVad_nativeProcess(JNIEnv* env, jobject, jlong h, jshortArray pcm, jint offset,
                                                   jint length, jbyteArray flags) {
  VadStream* vad = From(h);
  if (vad == nullptr || pcm == nullptr || flags == nullptr || offset < 0 || length < 0) return -1;
  if (static_cast<jlong>(offset) + length > env->GetArrayLength(pcm)) return -1;
  const jint capacity = env->GetArrayLength(flags);
  // Quadros possiveis = (amostras pendentes + novas) / 320; o chamador dimensiona `flags` com folga.
  std::vector<uint8_t> out;
  out.reserve(static_cast<size_t>(length) / VadStream::kFrameSamples + 2);
  jshort* data = env->GetShortArrayElements(pcm, nullptr);
  if (data == nullptr) return -1;
  vad->Process(reinterpret_cast<const int16_t*>(data) + offset, length, &out);
  env->ReleaseShortArrayElements(pcm, data, JNI_ABORT);
  if (static_cast<jint>(out.size()) > capacity) return -1;
  env->SetByteArrayRegion(flags, 0, static_cast<jsize>(out.size()), reinterpret_cast<const jbyte*>(out.data()));
  return static_cast<jint>(out.size());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_gravasom_core_dsp_NativeVad_nativeFlush(JNIEnv* env, jobject, jlong h, jbyteArray flags) {
  VadStream* vad = From(h);
  if (vad == nullptr || flags == nullptr || env->GetArrayLength(flags) < 1) return -1;
  std::vector<uint8_t> out;
  vad->Flush(&out);
  if (!out.empty()) env->SetByteArrayRegion(flags, 0, 1, reinterpret_cast<const jbyte*>(out.data()));
  return static_cast<jint>(out.size());
}

extern "C" JNIEXPORT void JNICALL
Java_com_gravasom_core_dsp_NativeVad_nativeDestroy(JNIEnv*, jobject, jlong h) {
  if (VadStream* vad = From(h)) {
    delete vad;
    g_vads.fetch_sub(1);
  }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_gravasom_core_dsp_NativeVad_nativeLiveInstances(JNIEnv*, jobject) {
  return g_vads.load();
}
