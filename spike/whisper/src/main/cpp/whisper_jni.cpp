// Ponte JNI minima para o spike: carregar o modelo, transcrever PCM float 16 kHz mono e devolver "t0\tt1\ttexto" por segmento.
#include <jni.h>

#include <string>
#include <thread>

#include "whisper.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_gravasom_spike_whisper_WhisperBridge_nativeInit(JNIEnv* env, jobject, jstring path) {
  const char* p = env->GetStringUTFChars(path, nullptr);
  whisper_context_params cparams = whisper_context_default_params();
  cparams.use_gpu = false;
  whisper_context* ctx = whisper_init_from_file_with_params(p, cparams);
  env->ReleaseStringUTFChars(path, p);
  return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT void JNICALL
Java_com_gravasom_spike_whisper_WhisperBridge_nativeFree(JNIEnv*, jobject, jlong handle) {
  if (handle != 0) whisper_free(reinterpret_cast<whisper_context*>(handle));
}

// Devolve os segmentos como "t0_ms\tt1_ms\ttexto\n"..., ou null em falha.
extern "C" JNIEXPORT jstring JNICALL
Java_com_gravasom_spike_whisper_WhisperBridge_nativeTranscribe(JNIEnv* env, jobject, jlong handle, jfloatArray pcm, jint threads,
                                                               jstring language) {
  auto* ctx = reinterpret_cast<whisper_context*>(handle);
  if (ctx == nullptr) return nullptr;
  const char* lang = env->GetStringUTFChars(language, nullptr);
  whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
  params.print_realtime = false;
  params.print_progress = false;
  params.print_timestamps = false;
  params.print_special = false;
  params.translate = false;
  params.language = lang;
  params.n_threads = threads > 0 ? threads : static_cast<int>(std::thread::hardware_concurrency());
  params.no_context = true;
  const jsize n = env->GetArrayLength(pcm);
  jfloat* samples = env->GetFloatArrayElements(pcm, nullptr);
  const int rc = whisper_full(ctx, params, samples, n);
  env->ReleaseFloatArrayElements(pcm, samples, JNI_ABORT);
  env->ReleaseStringUTFChars(language, lang);
  if (rc != 0) return nullptr;
  std::string out;
  const int segments = whisper_full_n_segments(ctx);
  for (int i = 0; i < segments; ++i) {
    out += std::to_string(whisper_full_get_segment_t0(ctx, i) * 10) + "\t" + std::to_string(whisper_full_get_segment_t1(ctx, i) * 10) +
           "\t" + whisper_full_get_segment_text(ctx, i) + "\n";
  }
  return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_gravasom_spike_whisper_WhisperBridge_nativeSystemInfo(JNIEnv* env, jobject) {
  return env->NewStringUTF(whisper_print_system_info());
}
