// Testes do VAD em fluxo no host (ASan/UBSan). Sai != 0 se algum falhar.
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <random>
#include <vector>

#include "../main/cpp/vad_stream.h"

using gravasom::dsp::VadStream;

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

static std::vector<int16_t> Ruido(double seg, double amp, unsigned seed) {
  std::mt19937 g(seed);
  std::normal_distribution<double> d(0.0, amp);
  std::vector<int16_t> v(static_cast<size_t>(seg * VadStream::kSampleRate));
  for (auto& s : v) s = static_cast<int16_t>(std::lround(std::clamp(d(g), -32768.0, 32767.0)));
  return v;
}

// Trem de pulsos glotais a 120 Hz atraves de dois formantes: soa como vogal sustentada.
static std::vector<int16_t> Vogal(double seg, double amp) {
  const int n = static_cast<int>(seg * VadStream::kSampleRate);
  std::vector<int16_t> v(n);
  double y1[2] = {0, 0}, y2[2] = {0, 0};
  const double fs = VadStream::kSampleRate;
  auto ress = [&](double f, double bw, double x, double* st) {
    const double r = std::exp(-M_PI * bw / fs);
    const double a1 = -2 * r * std::cos(2 * M_PI * f / fs), a2 = r * r;
    const double y = x - a1 * st[0] - a2 * st[1];
    st[1] = st[0];
    st[0] = y;
    return y;
  };
  const int periodo = static_cast<int>(fs / 120.0);
  double pico = 1e-9;
  std::vector<double> tmp(n);
  for (int i = 0; i < n; ++i) {
    const double x = (i % periodo == 0) ? 1.0 : 0.0;
    tmp[i] = ress(700, 110, x, y1) + ress(1200, 120, x, y2);
    pico = std::max(pico, std::fabs(tmp[i]));
  }
  for (int i = 0; i < n; ++i) v[i] = static_cast<int16_t>(std::lround(tmp[i] / pico * amp));
  return v;
}

static std::vector<uint8_t> Roda(int modo, const std::vector<int16_t>& pcm, size_t bloco) {
  VadStream vad(modo);
  std::vector<uint8_t> out;
  for (size_t i = 0; i < pcm.size(); i += bloco) {
    vad.Process(pcm.data() + i, static_cast<int>(std::min(bloco, pcm.size() - i)), &out);
  }
  return out;
}

static double Fracao(const std::vector<uint8_t>& f, size_t ini = 0) {
  if (f.size() <= ini) return 0;
  size_t s = 0;
  for (size_t i = ini; i < f.size(); ++i) s += f[i];
  return static_cast<double>(s) / static_cast<double>(f.size() - ini);
}

int main() {
  // 1. Modo invalido nao cria instancia.
  VERIFICA(!VadStream(-1).ok() && !VadStream(4).ok(), "modo fora de 0..3 deveria falhar");
  for (int m = 0; m <= 3; ++m) VERIFICA(VadStream(m).ok(), "modo %d deveria funcionar", m);

  // 2. Quadros: 1 s = 50 quadros, em qualquer tamanho de bloco (incluindo 1 amostra e primos).
  const auto um_seg = Ruido(1.0, 20.0, 1);
  for (size_t bloco : {1u, 7u, 319u, 320u, 321u, 4096u, 16000u}) {
    VERIFICA(Roda(2, um_seg, bloco).size() == 50, "bloco %zu: esperado 50 quadros", bloco);
  }

  // 3. O resultado nao depende de como o audio foi fatiado.
  const auto voz = Vogal(2.0, 8000);
  VERIFICA(Roda(2, voz, 1) == Roda(2, voz, 16000), "fatiamento mudou o resultado");

  // 4. Determinismo entre execucoes.
  VERIFICA(Roda(3, voz, 480) == Roda(3, voz, 480), "execucoes diferentes deram resultados diferentes");

  // 5. Silencio digital e ruido de sala muito baixo nunca sao fala (todos os modos).
  const std::vector<int16_t> zeros(16000, 0);
  const auto sala = Ruido(2.0, 6.0, 2);  // ~ -75 dBFS
  for (int m = 0; m <= 3; ++m) {
    VERIFICA(Fracao(Roda(m, zeros, 320)) == 0.0, "modo %d: silencio digital virou fala", m);
    // Modos permissivos (0 e 1) deixam passar alguns quadros de ruido: erro seguro (pula menos), mas limitado.
    const double teto = m >= 2 ? 0.0 : 0.05;
    VERIFICA(Fracao(Roda(m, sala, 320), 5) <= teto, "modo %d: ruido de sala virou fala (%.2f)", m, Fracao(Roda(m, sala, 320), 5));
  }

  // 6. Vogal forte e reconhecida como fala pela maior parte dos quadros (depois do aquecimento) em todos os modos.
  for (int m = 0; m <= 3; ++m) {
    const double f = Fracao(Roda(m, voz, 320), 10);
    VERIFICA(f > 0.6, "modo %d: so %.2f dos quadros da vogal foram fala", m, f);
  }

  // 7. Flush classifica o quadro parcial e zera o estado pendente.
  {
    VadStream vad(2);
    std::vector<uint8_t> out;
    vad.Process(voz.data(), 100, &out);
    VERIFICA(out.empty(), "100 amostras nao completam quadro");
    VERIFICA(vad.Flush(&out) == 1 && out.size() == 1, "flush deveria emitir 1 quadro");
    VERIFICA(vad.Flush(&out) == 0 && out.size() == 1, "segundo flush nao emite nada");
  }

  // 8. Argumentos invalidos sao ignorados sem quebrar.
  {
    VadStream vad(1);
    std::vector<uint8_t> out;
    VERIFICA(vad.Process(nullptr, 100, &out) == 0 && vad.Process(voz.data(), 0, &out) == 0 && out.empty(), "args invalidos");
  }

  if (g_falhas == 0) std::printf("OK: todos os testes do VAD passaram\n");
  return g_falhas == 0 ? 0 : 1;
}
