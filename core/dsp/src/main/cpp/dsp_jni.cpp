// Ponte JNI fina sobre gravasom::dsp::Chain. Sem logica de DSP aqui.
#include <jni.h>

#include <atomic>
#include <memory>

#include "dsp_chain.h"

using gravasom::dsp::Chain;

namespace {
std::atomic<int> g_cadeias{0};
Chain* From(jlong h) { return reinterpret_cast<Chain*>(h); }
constexpr jint kTipoPassaAltas = 1;
}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_gravasom_core_dsp_NativeDsp_nativeCreate(JNIEnv*, jobject, jint sample_rate) {
  if (sample_rate < 8000 || sample_rate > 96000) return 0;
  g_cadeias.fetch_add(1);
  return reinterpret_cast<jlong>(new Chain(sample_rate));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_gravasom_core_dsp_NativeDsp_nativeAddStep(JNIEnv* env, jobject, jlong h, jint type, jfloatArray params) {
  Chain* c = From(h);
  if (c == nullptr) return JNI_FALSE;
  jfloat p0 = 0;
  if (params != nullptr && env->GetArrayLength(params) > 0) env->GetFloatArrayRegion(params, 0, 1, &p0);
  switch (type) {
    case kTipoPassaAltas: {
      auto step = gravasom::dsp::MakeHighPass(p0, c->SampleRate());
      if (!step) return JNI_FALSE;
      c->Add(std::move(step));
      return JNI_TRUE;
    }
    default:
      return JNI_FALSE;
  }
}

extern "C" JNIEXPORT void JNICALL
Java_com_gravasom_core_dsp_NativeDsp_nativeSetBypass(JNIEnv*, jobject, jlong h, jboolean bypass) {
  if (Chain* c = From(h)) c->SetBypass(bypass == JNI_TRUE);
}

extern "C" JNIEXPORT void JNICALL
Java_com_gravasom_core_dsp_NativeDsp_nativeSetIntensity(JNIEnv*, jobject, jlong h, jfloat intensity) {
  if (Chain* c = From(h)) c->SetIntensity(intensity);
}

extern "C" JNIEXPORT void JNICALL
Java_com_gravasom_core_dsp_NativeDsp_nativeReset(JNIEnv*, jobject, jlong h) {
  if (Chain* c = From(h)) c->Reset();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_gravasom_core_dsp_NativeDsp_nativeLatency(JNIEnv*, jobject, jlong h) {
  Chain* c = From(h);
  return c == nullptr ? -1 : c->LatencySamples();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_gravasom_core_dsp_NativeDsp_nativeProcess(JNIEnv* env, jobject, jlong h, jfloatArray buf, jint offset, jint n) {
  Chain* c = From(h);
  if (c == nullptr || buf == nullptr || offset < 0 || n < 0) return -1;
  if (static_cast<jlong>(offset) + n > env->GetArrayLength(buf)) return -1;
  jfloat* data = env->GetFloatArrayElements(buf, nullptr);
  if (data == nullptr) return -1;
  c->Process(data + offset, n);
  env->ReleaseFloatArrayElements(buf, data, 0);
  return n;
}

extern "C" JNIEXPORT void JNICALL
Java_com_gravasom_core_dsp_NativeDsp_nativeDestroy(JNIEnv*, jobject, jlong h) {
  if (Chain* c = From(h)) {
    delete c;
    g_cadeias.fetch_sub(1);
  }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_gravasom_core_dsp_NativeDsp_nativeLiveChains(JNIEnv*, jobject) { return g_cadeias.load(); }
