#include "dsp_chain.h"

#include <algorithm>
#include <cmath>

namespace gravasom::dsp {

namespace {

constexpr int kMaxSubBlock = 4096;
constexpr double kDenormalFloor = 1e-30;

// Passa-altas Butterworth 2a ordem (RBJ cookbook), forma direta II transposta, estado em double.
class HighPass final : public Step {
 public:
  HighPass(double cutoff_hz, int sample_rate) {
    const double w0 = 2.0 * M_PI * cutoff_hz / sample_rate;
    const double cosw = std::cos(w0);
    const double alpha = std::sin(w0) / (2.0 * M_SQRT1_2);
    const double a0 = 1.0 + alpha;
    b0_ = ((1.0 + cosw) / 2.0) / a0;
    b1_ = -(1.0 + cosw) / a0;
    b2_ = b0_;
    a1_ = (-2.0 * cosw) / a0;
    a2_ = (1.0 - alpha) / a0;
  }
  void Reset() override { z1_ = z2_ = 0.0; }
  void Process(float* x, int n) override {
    for (int i = 0; i < n; ++i) {
      const double in = x[i];
      const double out = b0_ * in + z1_;
      z1_ = b1_ * in - a1_ * out + z2_;
      z2_ = b2_ * in - a2_ * out;
      if (std::fabs(z1_) < kDenormalFloor) z1_ = 0.0;
      if (std::fabs(z2_) < kDenormalFloor) z2_ = 0.0;
      x[i] = static_cast<float>(out);
    }
  }

 private:
  double b0_, b1_, b2_, a1_, a2_;
  double z1_ = 0.0, z2_ = 0.0;
};

}  // namespace

std::unique_ptr<Step> MakeHighPass(double cutoff_hz, int sample_rate) {
  if (cutoff_hz <= 0.0 || sample_rate <= 0 || cutoff_hz >= sample_rate / 2.0) return nullptr;
  return std::make_unique<HighPass>(cutoff_hz, sample_rate);
}

struct Chain::Impl {
  std::vector<std::unique_ptr<Step>> steps;
  bool bypass = false;
  float intensity = 1.0f;
  std::vector<float> dry = std::vector<float>(kMaxSubBlock);
  std::vector<float> delay;  // linha de atraso do sinal seco (compensa a latencia da cadeia)
  size_t delay_pos = 0;

  int Latency() const {
    int total = 0;
    for (const auto& s : steps) total += s->LatencySamples();
    return total;
  }
};

Chain::Chain() : impl_(new Impl) {}
Chain::~Chain() = default;

void Chain::Add(std::unique_ptr<Step> step) {
  if (!step) return;
  impl_->steps.push_back(std::move(step));
  impl_->delay.assign(static_cast<size_t>(impl_->Latency()), 0.0f);
  impl_->delay_pos = 0;
}

void Chain::Reset() {
  for (auto& s : impl_->steps) s->Reset();
  std::fill(impl_->delay.begin(), impl_->delay.end(), 0.0f);
  impl_->delay_pos = 0;
}

void Chain::SetBypass(bool bypass) { impl_->bypass = bypass; }

void Chain::SetIntensity(float intensity) {
  impl_->intensity = std::isfinite(intensity) ? std::clamp(intensity, 0.0f, 1.0f) : 0.0f;
}

int Chain::LatencySamples() const { return impl_->Latency(); }

void Chain::Process(float* x, int n) {
  Impl& s = *impl_;
  // Bypass e intensidade 0: caminho identico, sem tocar nos bytes.
  if (s.bypass || s.intensity <= 0.0f || x == nullptr || n <= 0) return;
  for (int off = 0; off < n; off += kMaxSubBlock) {
    float* p = x + off;
    const int m = std::min(kMaxSubBlock, n - off);
    const bool mistura = s.intensity < 1.0f;
    if (mistura) std::copy(p, p + m, s.dry.begin());
    for (auto& step : s.steps) step->Process(p, m);
    if (mistura) {
      const float k = s.intensity;
      for (int i = 0; i < m; ++i) {
        float seco = s.dry[i];
        if (!s.delay.empty()) {
          std::swap(seco, s.delay[s.delay_pos]);
          s.delay_pos = (s.delay_pos + 1) % s.delay.size();
        }
        p[i] = seco + k * (p[i] - seco);
      }
    }
    for (int i = 0; i < m; ++i) {
      const float v = p[i];
      p[i] = std::isfinite(v) ? std::clamp(v, -1.0f, 1.0f) : 0.0f;
    }
  }
}

}  // namespace gravasom::dsp
