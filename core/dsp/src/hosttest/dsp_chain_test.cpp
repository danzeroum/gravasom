// Testes do nucleo DSP no host. Sai != 0 se algum falhar.
#include <algorithm>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <random>
#include <vector>

#include "../main/cpp/dsp_chain.h"

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

static std::vector<float> Seno(double seg, double freq, double amp = 0.5) {
  std::vector<float> v(static_cast<size_t>(seg * kSampleRate));
  for (size_t i = 0; i < v.size(); ++i) v[i] = static_cast<float>(amp * std::sin(2 * M_PI * freq * i / kSampleRate));
  return v;
}
static double RmsDb(const std::vector<float>& v, size_t ini = 0) {
  double s = 0;
  size_t n = 0;
  for (size_t i = ini; i < v.size(); ++i) {
    s += double(v[i]) * v[i];
    ++n;
  }
  return 10 * std::log10(s / n + 1e-30);
}
static std::unique_ptr<Chain> CadeiaHp(double fc = 80.0) {
  auto c = std::make_unique<Chain>();
  c->Add(MakeHighPass(fc));
  return c;
}
static std::vector<float> Processa(Chain& c, std::vector<float> x, size_t bloco) {
  size_t passo = bloco == 0 ? x.size() : bloco;
  for (size_t i = 0; i < x.size(); i += passo) c.Process(x.data() + i, static_cast<int>(std::min(passo, x.size() - i)));
  return x;
}

static void TestaBypassEIntensidadeZeroIdenticos() {
  auto x = Seno(1.0, 440);
  x[10] = std::nanf("");  // nem NaN pode ser tocado no bypass
  auto c = CadeiaHp();
  c->SetBypass(true);
  auto y = Processa(*c, x, kFrameSamples);
  VERIFICA(std::memcmp(x.data(), y.data(), x.size() * sizeof(float)) == 0, "bypass alterou bytes");
  auto c2 = CadeiaHp();
  c2->SetIntensity(0.0f);
  auto z = Processa(*c2, Seno(1.0, 440), kFrameSamples);
  auto ref = Seno(1.0, 440);
  VERIFICA(std::memcmp(ref.data(), z.data(), ref.size() * sizeof(float)) == 0, "intensidade 0 alterou bytes");
}
static void TestaSilencioEntraSilencioSai() {
  auto c = CadeiaHp();
  auto y = Processa(*c, std::vector<float>(48000, 0.0f), kFrameSamples);
  bool zero = true;
  for (float v : y) zero &= (v == 0.0f);
  VERIFICA(zero, "silencio gerou sinal");
}
static void TestaSenoMilHzNaoAtenuado() {
  auto x = Seno(1.0, 1000);
  auto c = CadeiaHp(80.0);
  auto y = Processa(*c, x, kFrameSamples);
  double d = RmsDb(y, 4800) - RmsDb(x, 4800);
  std::printf("medida: seno 1 kHz apos passa-altas 80 Hz: %.3f dB\n", d);
  VERIFICA(std::fabs(d) <= 0.5, "atenuacao %.3f dB > 0,5 dB", d);
}
static void TestaPassaAltasAtenuaGrave() {
  auto x = Seno(1.0, 20);
  auto c = CadeiaHp(80.0);
  auto y = Processa(*c, x, kFrameSamples);
  double d = RmsDb(y, 9600) - RmsDb(x, 9600);
  std::printf("medida: seno 20 Hz apos passa-altas 80 Hz: %.2f dB\n", d);
  VERIFICA(d < -20.0, "20 Hz so atenuou %.2f dB", d);
}
static void TestaDeterminismo() {
  std::mt19937 rng(7);
  std::vector<float> x(48000);
  for (auto& v : x) v = static_cast<float>((rng() / double(rng.max()) - 0.5) * 0.8);
  auto c1 = CadeiaHp();
  auto c2 = CadeiaHp();
  auto a = Processa(*c1, x, kFrameSamples);
  auto b = Processa(*c2, x, kFrameSamples);
  VERIFICA(std::memcmp(a.data(), b.data(), a.size() * 4) == 0, "execucoes diferentes deram saidas diferentes");
}
static void TestaContinuidadeEntreBlocos() {
  std::mt19937 rng(11);
  std::vector<float> x(48000);
  for (auto& v : x) v = static_cast<float>((rng() / double(rng.max()) - 0.5) * 0.9);
  auto c1 = CadeiaHp();
  auto c2 = CadeiaHp();
  auto inteiro = Processa(*c1, x, 0);
  auto blocos = Processa(*c2, x, kFrameSamples);
  double maxd = 0;
  for (size_t i = 0; i < x.size(); ++i) maxd = std::max(maxd, double(std::fabs(inteiro[i] - blocos[i])));
  std::printf("medida: maior diferenca bloco x inteiro = %.3g (1 LSB16 = %.3g)\n", maxd, 1.0 / 32768);
  VERIFICA(maxd <= 1.0 / 32768, "blocos divergem do inteiro em %.3g", maxd);
}
static void TestaFuzz() {
  std::mt19937 rng(2024);
  auto c = CadeiaHp();
  int ruins = 0;
  for (int i = 0; i < 1000; ++i) {
    int n = 1 + static_cast<int>(rng() % 1024);
    std::vector<float> b(n);
    switch (rng() % 6) {
      case 0:
        for (auto& v : b) v = 0.7f;  // DC
        break;
      case 1:
        for (int k = 0; k < n; ++k) b[k] = (k & 1) ? 1.0f : -1.0f;  // clipping total
        break;
      case 2:
        for (auto& v : b) v = (rng() % 2) ? 1.0f : -1.0f;
        break;
      case 3:
        for (auto& v : b) v = static_cast<float>((rng() / double(rng.max()) * 2 - 1) * 4.0);  // fora da faixa
        break;
      case 4:
        for (int k = 0; k < n; ++k) b[k] = (k % 7 == 0) ? std::nanf("") : (k % 11 == 0 ? INFINITY : 0.1f);
        break;
      default:
        for (auto& v : b) v = static_cast<float>(rng() / double(rng.max()) * 2 - 1);
    }
    c->Process(b.data(), n);
    for (float v : b) {
      if (!std::isfinite(v) || v < -1.0f || v > 1.0f) {
        ++ruins;
        break;
      }
    }
  }
  VERIFICA(ruins == 0, "%d blocos com NaN/Inf/fora de [-1,1]", ruins);
}
static void TestaLatenciaDeclarada() {
  auto c = CadeiaHp();
  VERIFICA(c->LatencySamples() == 0, "passa-altas causal deveria ter latencia 0, tem %d", c->LatencySamples());
}

int main() {
  TestaBypassEIntensidadeZeroIdenticos();
  TestaSilencioEntraSilencioSai();
  TestaSenoMilHzNaoAtenuado();
  TestaPassaAltasAtenuaGrave();
  TestaDeterminismo();
  TestaContinuidadeEntreBlocos();
  TestaFuzz();
  TestaLatenciaDeclarada();
  std::printf(g_falhas == 0 ? "OK: todos os testes passaram\n" : "FALHAS: %d\n", g_falhas);
  return g_falhas == 0 ? 0 : 1;
}
