#include "noise_reduce.h"

#include <algorithm>
#include <cmath>

#include "resampler.h"

namespace gravasom::dsp {

namespace {

constexpr int kDenoiserDelay = 2 * kNrFrame;  // latencia do RNNoise v0.2: 2 quadros (medida por correlacao no spike, ADR-015)
constexpr size_t kCompactAt = 1 << 16;

class NoiseReduction final : public Step {
 public:
  NoiseReduction(std::unique_ptr<FrameDenoiser> denoiser, float intensity, int rate)
      : denoiser_(std::move(denoiser)),
        intensity_(std::isfinite(intensity) ? std::clamp(intensity, 0.0f, 1.0f) : 0.0f),
        up_(rate, kSampleRate),
        down_(kSampleRate, rate) {
    // Disponibilidade do molhado alinhado: Wup (filtro de subida) + (2 quadros do RNNoise + 479 de espera do quadro + Wdown) a 48 kHz,
    // convertidos para a taxa do passo, com folga de arredondamento. O passo atrasa a saida (e o seco) por esse total.
    const double to_rate = static_cast<double>(rate) / kSampleRate;
    latency_ = up_.LookaheadIn() +
               static_cast<int>(std::ceil((kDenoiserDelay + kNrFrame - 1 + down_.LookaheadIn() + 2) * to_rate)) + 3;
    dry_.assign(latency_, 0.0f);
    Reset();
  }

  int LatencySamples() const override { return latency_; }

  void Reset() override {
    denoiser_->Reset();
    up_.Reset();
    down_.Reset();
    std::fill(dry_.begin(), dry_.end(), 0.0f);
    dry_pos_ = 0;
    in48_.clear();
    in48_pos_ = 0;
    wet_.clear();
    wet_base_ = 0;
    skip_ = kDenoiserDelay;
    out_index_ = 0;
  }

  void Process(float* x, int n) override {
    if (n <= 0) return;
    const bool wet_needed = intensity_ > 0.0f;
    if (wet_needed) Analyze(x, n);
    for (int i = 0; i < n; ++i) {
      const float dry = dry_[dry_pos_];
      dry_[dry_pos_] = x[i];
      dry_pos_ = (dry_pos_ + 1) % dry_.size();
      float y = dry;
      if (wet_needed) {
        const long long idx = out_index_ - latency_;
        const float w = idx < 0 ? 0.0f : WetAt(idx);
        y = dry + intensity_ * (w - dry);
      }
      x[i] = y;
      ++out_index_;
    }
    Compact();
  }

 private:
  // Entrada -> 48 kHz -> quadros do RNNoise -> molhado a 48 kHz (sem os 2 quadros de latencia) -> taxa do passo.
  void Analyze(const float* x, int n) {
    up_.Push(x, static_cast<size_t>(n), &in48_);
    std::vector<float> chunk;
    while (in48_.size() - in48_pos_ >= static_cast<size_t>(kNrFrame)) {
      denoiser_->Process(frame_, in48_.data() + in48_pos_);
      in48_pos_ += kNrFrame;
      const int drop = std::min(skip_, kNrFrame);
      skip_ -= drop;
      chunk.insert(chunk.end(), frame_ + drop, frame_ + kNrFrame);
    }
    if (!chunk.empty()) down_.Push(chunk.data(), chunk.size(), &wet_);
  }

  float WetAt(long long idx) const {
    const long long k = idx - wet_base_;
    return (k >= 0 && k < static_cast<long long>(wet_.size())) ? wet_[static_cast<size_t>(k)] : 0.0f;
  }

  void Compact() {
    if (in48_pos_ > kCompactAt) {
      in48_.erase(in48_.begin(), in48_.begin() + in48_pos_);
      in48_pos_ = 0;
    }
    const long long consumed = std::max<long long>(0, out_index_ - latency_);
    if (consumed - wet_base_ > static_cast<long long>(kCompactAt)) {
      const size_t drop = static_cast<size_t>(consumed - wet_base_);
      wet_.erase(wet_.begin(), wet_.begin() + std::min(drop, wet_.size()));
      wet_base_ += static_cast<long long>(drop);
    }
  }

  std::unique_ptr<FrameDenoiser> denoiser_;
  float intensity_;
  RationalResampler up_;
  RationalResampler down_;
  int latency_ = 0;
  std::vector<float> dry_;
  size_t dry_pos_ = 0;
  std::vector<float> in48_;
  size_t in48_pos_ = 0;
  std::vector<float> wet_;
  long long wet_base_ = 0;
  int skip_ = kDenoiserDelay;
  long long out_index_ = 0;
  float frame_[kNrFrame];
};

}  // namespace

std::unique_ptr<Step> MakeNoiseReductionWith(std::unique_ptr<FrameDenoiser> denoiser, float intensity, int sample_rate) {
  if (!denoiser || sample_rate < 8000 || sample_rate > 96000) return nullptr;
  return std::make_unique<NoiseReduction>(std::move(denoiser), intensity, sample_rate);
}

}  // namespace gravasom::dsp
