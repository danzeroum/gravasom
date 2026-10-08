// VAD em fluxo sobre o WebRTC VAD vendorizado (webrtc_vad/). Sem JNI: o mesmo codigo roda no host (harness) e no app.
#pragma once

#include <cstdint>
#include <vector>

struct WebRtcVadInst;

namespace gravasom::dsp {

/**
 * Classifica fala/nao-fala a 16 kHz mono em quadros de 20 ms (320 amostras).
 * mode: 0 (menos agressivo) .. 3 (mais agressivo com o que chama de fala; corta mais).
 * Nao e thread-safe; uma instancia por fluxo.
 */
class VadStream {
 public:
  static constexpr int kSampleRate = 16000;
  static constexpr int kFrameSamples = 320;
  static constexpr int kFrameMillis = 20;

  explicit VadStream(int mode);
  ~VadStream();
  VadStream(const VadStream&) = delete;
  VadStream& operator=(const VadStream&) = delete;

  bool ok() const { return inst_ != nullptr; }

  // Consome n amostras e anexa 1 (fala) ou 0 em `out` por quadro completo. Devolve quantos quadros anexou.
  int Process(const int16_t* samples, int n, std::vector<uint8_t>* out);

  // Completa o quadro parcial com zeros e o classifica (fim do arquivo). Devolve 0 ou 1 quadro.
  int Flush(std::vector<uint8_t>* out);

 private:
  WebRtcVadInst* inst_ = nullptr;
  int16_t pending_[kFrameSamples] = {};
  int pending_len_ = 0;
};

}  // namespace gravasom::dsp
