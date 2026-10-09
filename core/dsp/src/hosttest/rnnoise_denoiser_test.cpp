// Comportamento do RNNoise real dentro do pipeline (compilado com o RNNoise baixado por hash). Sai != 0 se algum falhar.
#include <algorithm>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <random>
#include <vector>

#include "../main/cpp/noise_reduce.h"

using namespace gravasom::dsp;

static int g_falhas = 0;
#define VERIFICA(cond, ...)                              \
  do {                                                   \
    if (!(cond)) {                                       \
      ++g_falhas;                                        \
      std::printf("FALHOU %s:%d: ", __FILE__, __LINE__); \
      std::printf(__VA_ARGS__);                          \
      std::printf("\n");                                 \
    }                                                    \
  } while (0)

static std::vector<float> Ruido(int taxa, double seg, double amp, unsigned semente = 1) {
  std::mt19937 g(semente);
  std::normal_distribution<float> d(0.f, 1.f);
  std::vector<float> v(static_cast<size_t>(seg * taxa));
  for (auto& x : v) x = static_cast<float>(amp) * d(g);
  return v;
}
// Vogal sintetica modulada em silabas (4 Hz): f0 de 130 Hz com formantes.
static std::vector<float> Voz(int taxa, double seg, double amp) {
  std::vector<float> v(static_cast<size_t>(seg * taxa));
  for (size_t i = 0; i < v.size(); ++i) {
    const double t = double(i) / taxa;
    double s = 0;
    for (int h = 1; h <= 30; ++h) {
      const double f = 130.0 * h;
      if (f > 0.45 * taxa) break;
      const double formante = std::exp(-std::pow((f - 700) / 400, 2)) + 0.7 * std::exp(-std::pow((f - 1300) / 500, 2)) + 0.3 * std::exp(-std::pow((f - 2500) / 700, 2));
      s += formante / h * std::sin(2 * M_PI * f * t);
    }
    const double silaba = std::pow(std::fabs(std::sin(2 * M_PI * 3.0 * t)), 1.2);
    v[i] = static_cast<float>(amp * s * silaba);
  }
  return v;
}
static double Rms(const std::vector<float>& v, size_t ini, size_t fim) {
  double s = 0;
  for (size_t i = ini; i < fim; ++i) s += double(v[i]) * v[i];
  return std::sqrt(s / double(fim - ini));
}
static double Db(double x) { return 20 * std::log10(x + 1e-12); }
static std::vector<float> Processa(Step& s, std::vector<float> x, size_t bloco) {
  size_t passo = bloco == 0 ? x.size() : bloco;
  for (size_t i = 0; i < x.size(); i += passo) s.Process(x.data() + i, static_cast<int>(std::min(passo, x.size() - i)));
  return x;
}

static void TestaRuidoEstacionarioCaiMuito() {
  for (int taxa : {48000, 44100, 16000}) {
    auto x = Ruido(taxa, 4.0, 0.03);  // ~ -30 dBFS, branco
    auto passo = MakeNoiseReduction(1.0f, taxa);
    const int L = passo->LatencySamples();
    auto y = Processa(*passo, x, taxa / 100);
    const size_t ini = size_t(taxa) * 2, fim = x.size() - 1;  // depois do transiente
    const double queda = Db(Rms(x, ini, fim)) - Db(Rms(y, ini, fim));
    VERIFICA(queda > 10.0, "taxa %d (L=%d): ruido branco caiu so %.1f dB", taxa, L, queda);
  }
}

static void TestaVozNaoEMudaDepoisDoRedutor() {
  for (int taxa : {48000, 16000}) {
    auto x = Voz(taxa, 3.0, 0.4);
    auto passo = MakeNoiseReduction(1.0f, taxa);
    const int L = passo->LatencySamples();
    auto y = Processa(*passo, x, taxa / 100);
    const double ganho = Db(Rms(y, size_t(taxa), x.size())) - Db(Rms(x, size_t(taxa) - L, x.size() - L));
    VERIFICA(ganho > -10.0, "taxa %d: voz sintetica caiu %.1f dB (nao pode ser tratada como ruido)", taxa, ganho);
  }
}

static void TestaIntensidadeEMonotonaNaEnergiaDoRuido() {
  auto x = Ruido(48000, 3.0, 0.03, 4);
  double anterior = 1e9;
  for (float k : {0.0f, 0.4f, 0.7f, 1.0f}) {
    auto passo = MakeNoiseReduction(k, 48000);
    auto y = Processa(*passo, x, 480);
    const double r = Rms(y, 48000, x.size() - 1);
    VERIFICA(r <= anterior + 1e-6, "k=%.1f: energia %.5f subiu em relacao a intensidade menor (%.5f)", k, r, anterior);
    anterior = r;
  }
}

static void TestaDeterministicoEIndependenteDoBloco() {
  auto x = Ruido(44100, 1.5, 0.05, 8);
  for (size_t i = 0; i < x.size(); ++i) x[i] += 0.2f * static_cast<float>(std::sin(2 * M_PI * 220 * i / 44100.0)) * float(i % 20000) / 20000.f;
  auto a = Processa(*MakeNoiseReduction(0.6f, 44100), x, 0);
  auto b = Processa(*MakeNoiseReduction(0.6f, 44100), x, 0);
  VERIFICA(std::memcmp(a.data(), b.data(), a.size() * sizeof(float)) == 0, "duas execucoes diferem");
  for (size_t bloco : {size_t(441), size_t(1), size_t(997), size_t(4096)}) {
    auto c = Processa(*MakeNoiseReduction(0.6f, 44100), x, bloco);
    VERIFICA(std::memcmp(a.data(), c.data(), a.size() * sizeof(float)) == 0, "bloco %zu muda o resultado", bloco);
  }
}

static void TestaIntensidadeZeroEIdenticaAoSecoAtrasado() {
  auto x = Ruido(16000, 1.0, 0.2, 2);
  auto passo = MakeNoiseReduction(0.0f, 16000);
  const int L = passo->LatencySamples();
  auto y = Processa(*passo, x, 160);
  bool igual = true;
  for (size_t i = 0; i < x.size(); ++i) {
    const float e = i >= size_t(L) ? x[i - L] : 0.0f;
    if (std::memcmp(&y[i], &e, sizeof(float)) != 0) igual = false;
  }
  VERIFICA(igual, "intensidade 0 nao devolveu o seco atrasado bit a bit");
}

static void TestaTaxaInvalidaERecusada() {
  VERIFICA(MakeNoiseReduction(1.0f, 4000) == nullptr, "4 kHz deveria ser recusado");
  VERIFICA(MakeNoiseReduction(1.0f, 192000) == nullptr, "192 kHz deveria ser recusado");
}

int main() {
  TestaRuidoEstacionarioCaiMuito();
  TestaVozNaoEMudaDepoisDoRedutor();
  TestaIntensidadeEMonotonaNaEnergiaDoRuido();
  TestaDeterministicoEIndependenteDoBloco();
  TestaIntensidadeZeroEIdenticaAoSecoAtrasado();
  TestaTaxaInvalidaERecusada();
  if (g_falhas == 0) std::printf("rnnoise_denoiser_test: OK\n");
  return g_falhas == 0 ? 0 : 1;
}
