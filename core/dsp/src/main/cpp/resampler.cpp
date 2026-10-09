#include "resampler.h"

#include <algorithm>
#include <cmath>
#include <numeric>

namespace gravasom::dsp {

namespace {
constexpr double kBandEdge = 0.95;   // fracao de Nyquist (da menor taxa) que passa
constexpr int kLobes = 12;           // cruzamentos por zero do sinc de cada lado
constexpr double kKaiserBeta = 8.0;
constexpr size_t kCompactAt = 1 << 16;

double Bessel0(double x) {
  double sum = 1.0, term = 1.0;
  for (int k = 1; k <= 40; ++k) {
    term *= (x / (2.0 * k)) * (x / (2.0 * k));
    sum += term;
  }
  return sum;
}
double Sinc(double x) { return x == 0.0 ? 1.0 : std::sin(M_PI * x) / (M_PI * x); }
}  // namespace

RationalResampler::RationalResampler(int in_rate, int out_rate) : passthrough_(in_rate == out_rate) {
  if (passthrough_) return;
  const int g = std::gcd(in_rate, out_rate);
  up_ = out_rate / g;
  down_ = in_rate / g;
  const double lp = std::min(1.0, static_cast<double>(out_rate) / in_rate) * kBandEdge;  // corte relativo a Nyquist da entrada
  half_ = static_cast<int>(std::ceil(kLobes / lp));
  coef_.assign(up_, std::vector<float>(2 * half_));
  const double norm = Bessel0(kKaiserBeta);
  for (int p = 0; p < up_; ++p) {
    const double frac = static_cast<double>(p) / up_;
    double sum = 0.0;
    std::vector<double> c(2 * half_);
    for (int j = 0; j < 2 * half_; ++j) {
      const double d = frac + half_ - 1 - j;  // distancia (em amostras de entrada) da saida a amostra i = m - half_ + 1 + j
      const double r = d / half_;
      const double w = std::fabs(r) >= 1.0 ? 0.0 : Bessel0(kKaiserBeta * std::sqrt(1.0 - r * r)) / norm;
      c[j] = lp * Sinc(lp * d) * w;
      sum += c[j];
    }
    for (int j = 0; j < 2 * half_; ++j) coef_[p][j] = static_cast<float>(c[j] / sum);  // ganho DC exatamente 1
  }
  Reset();
}

void RationalResampler::Reset() {
  buf_.assign(passthrough_ ? 0 : half_, 0.0f);  // zeros "antes do tempo": indices -half_ .. -1
  lo_ = -half_;
  total_in_ = 0;
  next_out_ = 0;
}

void RationalResampler::Push(const float* in, size_t n, std::vector<float>* out) {
  if (passthrough_) {
    out->insert(out->end(), in, in + n);
    return;
  }
  buf_.insert(buf_.end(), in, in + n);
  total_in_ += static_cast<long long>(n);
  for (;;) {
    const long long pos = next_out_ * down_;
    const long long m = pos / up_;
    if (m + half_ >= total_in_) break;  // falta entrada futura
    const std::vector<float>& c = coef_[static_cast<size_t>(pos % up_)];
    const float* x = buf_.data() + (m - half_ + 1 - lo_);
    float acc = 0.0f;
    for (int j = 0; j < 2 * half_; ++j) acc += c[j] * x[j];
    out->push_back(acc);
    ++next_out_;
  }
  // descarta o que nenhuma saida futura vai usar
  const long long keep_from = (next_out_ * down_) / up_ - half_ + 1;
  if (keep_from - lo_ > static_cast<long long>(kCompactAt)) {
    buf_.erase(buf_.begin(), buf_.begin() + (keep_from - lo_));
    lo_ = keep_from;
  }
}

}  // namespace gravasom::dsp
