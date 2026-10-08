// Testes do redutor de ruido SEM o RNNoise: o pipeline (reamostragem, quadros de 10 ms, latencia, mistura seco/molhado) roda com um
// "denoiser" injetado. Sai != 0 se algum falhar.
#include <algorithm>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <memory>
#include <random>
#include <vector>

#include "../main/cpp/noise_reduce.h"
#include "../main/cpp/resampler.h"

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

// Denoiser de teste: devolve a entrada atrasada em `atraso` amostras (como o RNNoise, que atrasa 960), com ganho opcional.
class Atrasa final : public FrameDenoiser {
 public:
  explicit Atrasa(int atraso = 2 * kNrFrame, float ganho = 1.0f) : ganho_(ganho), fila_(atraso, 0.0f) {}
  void Reset() override { std::fill(fila_.begin(), fila_.end(), 0.0f); pos_ = 0; }
  void Process(float* out, const float* in) override {
    for (int i = 0; i < kNrFrame; ++i) {
      out[i] = fila_.empty() ? in[i] * ganho_ : fila_[pos_] * ganho_;
      if (!fila_.empty()) { fila_[pos_] = in[i]; pos_ = (pos_ + 1) % fila_.size(); }
    }
  }
 private:
  float ganho_;
  std::vector<float> fila_;
  size_t pos_ = 0;
};

static std::vector<float> Seno(int taxa, double seg, double freq, double amp = 0.5) {
  std::vector<float> v(static_cast<size_t>(seg * taxa));
  for (size_t i = 0; i < v.size(); ++i) v[i] = static_cast<float>(amp * std::sin(2 * M_PI * freq * i / taxa));
  return v;
}
static std::vector<float> Ruido(int taxa, double seg, double amp, unsigned semente = 1) {
  std::mt19937 g(semente);
  std::uniform_real_distribution<float> d(-1.f, 1.f);
  std::vector<float> v(static_cast<size_t>(seg * taxa));
  for (auto& x : v) x = static_cast<float>(amp) * d(g);
  return v;
}
static std::vector<float> Processa(Step& s, std::vector<float> x, size_t bloco) {
  size_t passo = bloco == 0 ? x.size() : bloco;
  for (size_t i = 0; i < x.size(); i += passo) s.Process(x.data() + i, static_cast<int>(std::min(passo, x.size() - i)));
  return x;
}
static std::unique_ptr<Step> Passo(int taxa, float intensidade, float ganho = 1.0f) {
  return MakeNoiseReductionWith(std::make_unique<Atrasa>(2 * kNrFrame, ganho), intensidade, taxa);
}
static double ErroMax(const std::vector<float>& saida, const std::vector<float>& ent, int atraso, size_t ini, size_t fim) {
  double e = 0;
  for (size_t i = std::max<size_t>(ini, atraso); i < std::min(fim, saida.size()); ++i) e = std::max(e, std::fabs(double(saida[i]) - ent[i - atraso]));
  return e;
}

static void TestaReamostradorPreservaSenoEAmplitude() {
  const int pares[][2] = {{44100, 48000}, {48000, 44100}, {16000, 48000}, {48000, 16000}, {22050, 48000}, {8000, 48000}};
  for (auto& p : pares) {
    RationalResampler r(p[0], p[1]);
    auto x = Seno(p[0], 1.0, 0.1 * std::min(p[0], p[1]));  // tom a 20% da menor taxa: bem dentro da banda
    std::vector<float> y;
    r.Push(x.data(), x.size(), &y);
    // sai ~ taxa de saida * 1 s (menos o que ainda depende de amostras futuras)
    const double esperado = double(p[1]);
    VERIFICA(std::fabs(double(y.size()) - esperado) < 0.01 * esperado + r.LookaheadIn() * double(p[1]) / p[0] + 4, "%d->%d: %zu saidas (esperado ~%.0f)",
             p[0], p[1], y.size(), esperado);
    double erro = 0;
    const double f = 0.1 * std::min(p[0], p[1]);
    for (size_t n = y.size() / 4; n < 3 * y.size() / 4; ++n) {
      erro = std::max(erro, std::fabs(double(y[n]) - 0.5 * std::sin(2 * M_PI * f * n / p[1])));
    }
    VERIFICA(erro < 0.01, "%d->%d: erro do seno reamostrado %.4f (sem atraso: zero fase)", p[0], p[1], erro);
  }
}

static void TestaReamostradorMesmoResultadoEmQualquerBloco() {
  auto x = Ruido(44100, 1.0, 0.3);
  RationalResampler a(44100, 48000), b(44100, 48000);
  std::vector<float> ya, yb;
  a.Push(x.data(), x.size(), &ya);
  for (size_t i = 0; i < x.size();) {
    size_t n = std::min<size_t>((i % 7) + 1 + (i % 913), x.size() - i);
    b.Push(x.data() + i, n, &yb);
    i += n;
  }
  VERIFICA(ya.size() == yb.size() && std::memcmp(ya.data(), yb.data(), ya.size() * sizeof(float)) == 0, "blocos mudaram o resultado do reamostrador");
}

static void TestaReamostradorIdentidadeQuandoTaxasIguais() {
  RationalResampler r(48000, 48000);
  auto x = Ruido(48000, 0.1, 0.5);
  std::vector<float> y;
  r.Push(x.data(), x.size(), &y);
  VERIFICA(y == x && r.LookaheadIn() == 0, "taxas iguais devem passar direto");
}

static void TestaLatenciaDeclaradaAlinhaASaidaComAEntrada() {
  for (int taxa : {48000, 44100, 22050, 16000, 8000}) {
    auto passo = Passo(taxa, 1.0f);
    const int L = passo->LatencySamples();
    VERIFICA(L > 0 && L < taxa / 5, "taxa %d: latencia %d fora do razoavel", taxa, L);
    auto x = Seno(taxa, 2.0, 0.08 * taxa);  // tom ate 8% da taxa
    std::vector<float> sai = Processa(*passo, x, kFrameSamples);
    // saida(N) = entrada(N - L): o denoiser de teste so atrasa (compensado pelo pipeline), entao o molhado e a entrada atrasada
    double e = ErroMax(sai, x, L, 3 * taxa / 10, x.size());
    VERIFICA(e < 0.02, "taxa %d: saida nao bate com a entrada atrasada em L=%d (erro max %.4f)", taxa, L, e);
    // e nao bate se o atraso estiver errado em 3 amostras: o teste de fato discrimina
    double errado = ErroMax(sai, x, L + 3, 3 * taxa / 10, x.size());
    VERIFICA(errado > 3 * e + 0.01, "taxa %d: teste nao discrimina (erro %.4f, com atraso errado %.4f)", taxa, e, errado);
  }
}

static void TestaMesmoResultadoEmQualquerTamanhoDeBloco() {
  for (int taxa : {48000, 44100, 16000}) {
    auto x = Ruido(taxa, 1.5, 0.3, 5);
    auto ref = Processa(*Passo(taxa, 0.7f), x, 0);
    for (size_t bloco : {size_t(1), size_t(77), size_t(480), size_t(1001), size_t(4096)}) {
      auto y = Processa(*Passo(taxa, 0.7f), x, bloco);
      VERIFICA(std::memcmp(ref.data(), y.data(), ref.size() * sizeof(float)) == 0, "taxa %d bloco %zu: saida difere", taxa, bloco);
    }
  }
}

static void TestaIntensidadeZeroDevolveOSecoAtrasadoExatamente() {
  for (int taxa : {48000, 44100, 16000}) {
    auto x = Ruido(taxa, 1.0, 0.4, 9);
    auto passo = Passo(taxa, 0.0f, /*ganho do denoiser*/ 2.0f);
    const int L = passo->LatencySamples();
    auto y = Processa(*passo, x, kFrameSamples);
    bool igual = true;
    for (size_t i = 0; i < x.size(); ++i) {
      const float esperado = i >= static_cast<size_t>(L) ? x[i - L] : 0.0f;
      if (std::memcmp(&y[i], &esperado, sizeof(float)) != 0) igual = false;
    }
    VERIFICA(igual, "taxa %d: intensidade 0 nao devolveu o seco atrasado bit a bit", taxa);
  }
}

static void TestaIntensidadeMisturaSecoEMolhado() {
  // O "denoiser" silencia tudo (ganho 0): com intensidade k a saida e (1-k) * seco atrasado.
  auto x = Ruido(48000, 1.0, 0.4, 3);
  for (float k : {0.25f, 0.5f, 1.0f}) {
    auto passo = Passo(48000, k, 0.0f);
    const int L = passo->LatencySamples();
    auto y = Processa(*passo, x, kFrameSamples);
    double e = 0;
    for (size_t i = L; i < x.size(); ++i) e = std::max(e, std::fabs(double(y[i]) - (1.0 - k) * x[i - L]));
    VERIFICA(e < 1e-6, "k=%.2f: erro de mistura %.8f", k, e);
  }
}

static void TestaResetReproduzOMesmoResultado() {
  auto x = Ruido(44100, 0.6, 0.3, 11);
  auto passo = Passo(44100, 0.8f);
  auto a = Processa(*passo, x, 700);
  passo->Reset();
  auto b = Processa(*passo, x, 700);
  VERIFICA(std::memcmp(a.data(), b.data(), a.size() * sizeof(float)) == 0, "Reset nao devolveu o estado inicial");
}

static void TestaEntradaExtremaNaoGeraNaN() {
  auto passo = Passo(16000, 1.0f);
  std::vector<float> x(16000, 1.0f);
  for (size_t i = 0; i < x.size(); i += 2) x[i] = -1.0f;  // Nyquist em fundo de escala
  auto y = Processa(*passo, x, 333);
  bool finito = std::all_of(y.begin(), y.end(), [](float v) { return std::isfinite(v); });
  VERIFICA(finito, "saida com NaN/Inf");
}

static void TestaChainComRedutorCompensaLatenciaECompoeComPassaAltas() {
  Chain c(44100);
  c.Add(MakeNoiseReductionWith(std::make_unique<Atrasa>(), 1.0f, 44100));
  const int L = c.LatencySamples();
  VERIFICA(L > 0, "a cadeia precisa declarar a latencia do redutor (%d)", L);
  auto x = Seno(44100, 1.5, 700, 0.3);
  auto y = Processa(c, x, c.FrameSamples());
  VERIFICA(ErroMax(y, x, L, 20000, x.size()) < 0.02, "a cadeia nao alinhou com a latencia declarada");
}

int main() {
  TestaReamostradorPreservaSenoEAmplitude();
  TestaReamostradorMesmoResultadoEmQualquerBloco();
  TestaReamostradorIdentidadeQuandoTaxasIguais();
  TestaLatenciaDeclaradaAlinhaASaidaComAEntrada();
  TestaMesmoResultadoEmQualquerTamanhoDeBloco();
  TestaIntensidadeZeroDevolveOSecoAtrasadoExatamente();
  TestaIntensidadeMisturaSecoEMolhado();
  TestaResetReproduzOMesmoResultado();
  TestaEntradaExtremaNaoGeraNaN();
  TestaChainComRedutorCompensaLatenciaECompoeComPassaAltas();
  if (g_falhas == 0) std::printf("noise_reduce_test: OK\n");
  return g_falhas == 0 ? 0 : 1;
}
