// Cola entre o RNNoise v0.2 (C, BSD-3-Clause; baixado por hash: scripts/buscar-rnnoise.sh) e o FrameDenoiser do redutor.
#include <memory>

#include "noise_reduce.h"
extern "C" {
#include "rnnoise.h"
}

namespace gravasom::dsp {

namespace {

// O RNNoise trabalha com amostras na escala de 16 bits (+-32768); a cadeia, em [-1, 1].
constexpr float kScale = 32768.0f;

class RnnoiseDenoiser final : public FrameDenoiser {
 public:
  RnnoiseDenoiser() : state_(rnnoise_create(nullptr)) {}
  ~RnnoiseDenoiser() override { if (state_ != nullptr) rnnoise_destroy(state_); }
  RnnoiseDenoiser(const RnnoiseDenoiser&) = delete;
  RnnoiseDenoiser& operator=(const RnnoiseDenoiser&) = delete;

  void Reset() override {
    if (state_ != nullptr) rnnoise_destroy(state_);
    state_ = rnnoise_create(nullptr);
  }

  void Process(float* out, const float* in) override {
    float scaled_in[kNrFrame];
    float scaled_out[kNrFrame];
    for (int i = 0; i < kNrFrame; ++i) scaled_in[i] = in[i] * kScale;
    rnnoise_process_frame(state_, scaled_out, scaled_in);
    for (int i = 0; i < kNrFrame; ++i) out[i] = scaled_out[i] / kScale;
  }

 private:
  DenoiseState* state_;
};

}  // namespace

std::unique_ptr<Step> MakeNoiseReduction(float intensity, int sample_rate) {
  return MakeNoiseReductionWith(std::make_unique<RnnoiseDenoiser>(), intensity, sample_rate);
}

}  // namespace gravasom::dsp
