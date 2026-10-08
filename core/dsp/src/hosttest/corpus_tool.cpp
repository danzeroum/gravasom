// Gera o corpus sintetico, a saida da cadeia DSP e os controles positivos como WAV em um diretorio.
// Uso: corpus_tool <diretorio_saida>
// Tudo deterministico: PRNG proprio com sementes fixas (mesmo binario => mesmos bytes).
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <sys/stat.h>
#include <vector>

#include "../main/cpp/dsp_chain.h"

namespace {

constexpr int kRate = 48000;

// xorshift64*: independente da libstdc++ (reprodutivel entre plataformas).
struct Rng {
  uint64_t s;
  explicit Rng(uint64_t seed) : s(seed * 0x9E3779B97F4A7C15ull + 1) {}
  uint64_t Next() {
    s ^= s >> 12; s ^= s << 25; s ^= s >> 27;
    return s * 0x2545F4914F6CDD1Dull;
  }
  double Uniform() { return (Next() >> 11) * (1.0 / 9007199254740992.0); }          // [0,1)
  double Gauss() {                                                                    // Box-Muller
    double u1 = std::max(Uniform(), 1e-12), u2 = Uniform();
    return std::sqrt(-2.0 * std::log(u1)) * std::cos(2 * M_PI * u2);
  }
};

using Sig = std::vector<float>;

void WriteWav16(const std::string& path, const Sig& x, int rate = kRate) {
  FILE* f = std::fopen(path.c_str(), "wb");
  if (!f) { std::perror(path.c_str()); std::exit(2); }
  uint32_t data = static_cast<uint32_t>(x.size() * 2), riff = 36 + data, fmt = 16, br = rate * 2, sr = rate;
  uint16_t pcm = 1, ch = 1, ba = 2, bits = 16;
  std::fwrite("RIFF", 1, 4, f); std::fwrite(&riff, 4, 1, f); std::fwrite("WAVEfmt ", 1, 8, f);
  std::fwrite(&fmt, 4, 1, f); std::fwrite(&pcm, 2, 1, f); std::fwrite(&ch, 2, 1, f); std::fwrite(&sr, 4, 1, f);
  std::fwrite(&br, 4, 1, f); std::fwrite(&ba, 2, 1, f); std::fwrite(&bits, 2, 1, f);
  std::fwrite("data", 1, 4, f); std::fwrite(&data, 4, 1, f);
  for (float v : x) {
    int16_t s = static_cast<int16_t>(std::lrint(std::clamp(v, -1.0f, 1.0f) * 32767.0f));
    std::fwrite(&s, 2, 1, f);
  }
  std::fclose(f);
}

// Le WAV PCM16 mono percorrendo os chunks (aceita LIST etc.); aborta se nao for esse formato.
Sig LerWav16(const std::string& path) {
  FILE* f = std::fopen(path.c_str(), "rb");
  if (!f) { std::perror(path.c_str()); std::exit(2); }
  char id[4];
  uint32_t tam = 0;
  if (std::fread(id, 1, 4, f) != 4 || std::memcmp(id, "RIFF", 4) != 0 || std::fseek(f, 4, SEEK_CUR) != 0 ||
      std::fread(id, 1, 4, f) != 4 || std::memcmp(id, "WAVE", 4) != 0) {
    std::fprintf(stderr, "%s: nao e RIFF/WAVE\n", path.c_str());
    std::exit(2);
  }
  uint16_t fmt = 0, canais = 0, bits = 0;
  while (std::fread(id, 1, 4, f) == 4 && std::fread(&tam, 4, 1, f) == 1) {
    if (std::memcmp(id, "fmt ", 4) == 0) {
      uint8_t b[16];
      if (tam < 16 || std::fread(b, 1, 16, f) != 16) break;
      fmt = b[0] | (b[1] << 8); canais = b[2] | (b[3] << 8); bits = b[14] | (b[15] << 8);
      std::fseek(f, tam - 16 + (tam & 1), SEEK_CUR);
    } else if (std::memcmp(id, "data", 4) == 0) {
      if (fmt != 1 || canais != 1 || bits != 16) break;
      Sig x(tam / 2);
      for (auto& v : x) {
        int16_t s;
        if (std::fread(&s, 2, 1, f) != 1) break;
        v = s / 32768.0f;
      }
      std::fclose(f);
      return x;
    } else {
      std::fseek(f, tam + (tam & 1), SEEK_CUR);
    }
  }
  std::fprintf(stderr, "%s: esperado WAV PCM16 mono\n", path.c_str());
  std::exit(2);
}

double Rms(const Sig& x) {
  double s = 0;
  for (float v : x) s += double(v) * v;
  return std::sqrt(s / std::max<size_t>(x.size(), 1));
}
double Peak(const Sig& x) {
  double p = 0;
  for (float v : x) p = std::max(p, double(std::fabs(v)));
  return p;
}
void ScaleToRms(Sig& x, double rms_db) {
  double k = std::pow(10.0, rms_db / 20.0) / std::max(Rms(x), 1e-12);
  for (auto& v : x) v = static_cast<float>(v * k);
}

// ---------- geradores ----------
Sig Seno(double seg, double freq, double amp) {
  Sig x(static_cast<size_t>(seg * kRate));
  for (size_t i = 0; i < x.size(); ++i) x[i] = static_cast<float>(amp * std::sin(2 * M_PI * freq * i / kRate));
  return x;
}
Sig Varredura(double seg, double f0, double f1, double amp) {
  Sig x(static_cast<size_t>(seg * kRate));
  const double k = std::log(f1 / f0) / seg;
  for (size_t i = 0; i < x.size(); ++i) {
    double t = double(i) / kRate;
    x[i] = static_cast<float>(amp * std::sin(2 * M_PI * f0 * (std::exp(k * t) - 1) / k));
  }
  return x;
}
Sig RuidoBranco(double seg, uint64_t seed) {
  Rng r(seed);
  Sig x(static_cast<size_t>(seg * kRate));
  for (auto& v : x) v = static_cast<float>(r.Gauss());
  return x;
}
Sig RuidoRosa(double seg, uint64_t seed) {  // filtro de Paul Kellet (aprox. -3 dB/oitava)
  Rng r(seed);
  Sig x(static_cast<size_t>(seg * kRate));
  double b0 = 0, b1 = 0, b2 = 0, b3 = 0, b4 = 0, b5 = 0, b6 = 0;
  for (auto& v : x) {
    double w = r.Gauss();
    b0 = 0.99886 * b0 + w * 0.0555179; b1 = 0.99332 * b1 + w * 0.0750759; b2 = 0.96900 * b2 + w * 0.1538520;
    b3 = 0.86650 * b3 + w * 0.3104856; b4 = 0.55000 * b4 + w * 0.5329522; b5 = -0.7616 * b5 - w * 0.0168980;
    v = static_cast<float>(b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362);
    b6 = w * 0.115926;
  }
  return x;
}
Sig RuidoMarrom(double seg, uint64_t seed) {  // integral do branco (-6 dB/oitava) com vazamento
  Rng r(seed);
  Sig x(static_cast<size_t>(seg * kRate));
  double y = 0;
  for (auto& v : x) { y = 0.998 * y + r.Gauss() * 0.02; v = static_cast<float>(y); }
  return x;
}
Sig Zumbido60(double seg, double amp) {  // 60 Hz + harmonicos 2..8 com amplitude decrescente
  Sig x(static_cast<size_t>(seg * kRate), 0.0f);
  for (int h = 1; h <= 8; ++h)
    for (size_t i = 0; i < x.size(); ++i)
      x[i] += static_cast<float>(amp / h * std::sin(2 * M_PI * 60.0 * h * i / kRate + 0.3 * h));
  return x;
}
Sig Ventilador(double seg, uint64_t seed) {  // ruido de banda larga modulado + tom de passagem de pa (150 Hz)
  Sig n = RuidoRosa(seg, seed);
  Sig x(n.size());
  double lp = 0;
  for (size_t i = 0; i < n.size(); ++i) {
    lp += 0.15 * (n[i] - lp);  // passa-baixas ~1,1 kHz
    double mod = 1.0 + 0.15 * std::sin(2 * M_PI * 3.0 * i / kRate);
    x[i] = static_cast<float>(lp * mod + 0.25 * std::sin(2 * M_PI * 150.0 * i / kRate));
  }
  return x;
}

// Ressonador de 2a ordem (formante): polos em f, banda bw.
struct Formante {
  double a1, a2, g, y1 = 0, y2 = 0;
  Formante(double f, double bw) {
    double r = std::exp(-M_PI * bw / kRate);
    a1 = 2 * r * std::cos(2 * M_PI * f / kRate); a2 = -r * r; g = 1 - r;
  }
  double Run(double x) { double y = g * x + a1 * y1 + a2 * y2; y2 = y1; y1 = y; return y; }
};

// Fala sintetica: fonte glotal (pulsos com F0 variavel) -> 3 formantes por vogal, envelope silabico,
// consoantes fricativas (ruido filtrado) entre silabas. Nao e fala real; e um sinal voz-like reprodutivel.
Sig FalaSintetica(double seg, uint64_t seed) {
  Rng r(seed);
  struct V { double f1, f2, f3; };
  const V vogais[] = {{730, 1090, 2440}, {530, 1840, 2480}, {270, 2290, 3010}, {570, 840, 2410}, {300, 870, 2240}};
  Sig x(static_cast<size_t>(seg * kRate), 0.0f);
  double fase = 0;
  const double silaba = 0.22;  // ~4,5 silabas/s
  Formante cf[3] = {Formante(730, 90), Formante(1090, 110), Formante(2440, 170)};
  int ultimo = -1;
  double hp = 0, lpf = 0, lpf2 = 0;
  for (size_t i = 0; i < x.size(); ++i) {
    double t = double(i) / kRate;
    int idx = static_cast<int>(t / silaba);
    const V& v = vogais[(idx * 3 + 1) % 5];
    double pos = std::fmod(t, silaba) / silaba;                                // 0..1 dentro da silaba
    double env = std::pow(std::sin(M_PI * std::min(pos / 0.85, 1.0)), 1.5);    // ataque/queda
    bool pausa = (idx % 9) == 8;                                               // uma pausa a cada 9 silabas
    double f0 = 120.0 * (1.0 + 0.08 * std::sin(2 * M_PI * 0.7 * t) + 0.03 * std::sin(2 * M_PI * 5.0 * t));
    fase += f0 / kRate;
    double pulso = 0;
    if (fase >= 1.0) { fase -= 1.0; pulso = 1.0; }
    // pulso glotal suavizado (dois passa-baixas) para espectro ~ -12 dB/oitava
    hp += 0.12 * (pulso - hp);
    lpf += 0.12 * (hp - lpf);
    lpf2 += 0.12 * (lpf - lpf2);
    if (ultimo != idx) {  // formantes estaticos dentro de cada silaba
      cf[0] = Formante(v.f1, 90); cf[1] = Formante(v.f2, 110); cf[2] = Formante(v.f3, 170);
      ultimo = idx;
    }
    double e = lpf2 * 40.0;
    double voz = cf[0].Run(e) + 0.7 * cf[1].Run(e) + 0.4 * cf[2].Run(e);
    double fric = (pos > 0.85 && !pausa) ? 0.05 * r.Gauss() * std::sin(M_PI * (pos - 0.85) / 0.15) : 0.0;  // consoante
    x[i] = static_cast<float>(pausa ? 0.0 : (voz * env * 6.0 + fric));
  }
  ScaleToRms(x, -23.0);  // ~ -23 dBFS RMS
  // Ruido de sala (-60 dBFS RMS): gravacao real nunca tem silencio digital (o detector de glitch o trata como dropout).
  Rng sala(seed + 1000);
  for (auto& v : x) v += static_cast<float>(0.001 * sala.Gauss());
  return x;
}

Sig Soma(const Sig& a, const Sig& b, double kb) {
  Sig y(a.size());
  for (size_t i = 0; i < a.size(); ++i) y[i] = static_cast<float>(a[i] + kb * b[i % b.size()]);
  return y;
}
// Mistura sinal+ruido com a relacao sinal/ruido pedida (dB), pelas potencias RMS.
Sig MisturaSnr(const Sig& sinal, const Sig& ruido, double snr_db) {
  double k = Rms(sinal) / std::max(Rms(ruido), 1e-12) / std::pow(10.0, snr_db / 20.0);
  return Soma(sinal, ruido, k);
}

// ---------- processamento e controles ----------
Sig ProcessaCadeia(Sig x, double hp_hz) {
  gravasom::dsp::Chain c(kRate);
  c.Add(gravasom::dsp::MakeHighPass(hp_hz, kRate));
  const int bloco = c.FrameSamples();
  for (size_t i = 0; i < x.size(); i += bloco) c.Process(x.data() + i, static_cast<int>(std::min<size_t>(bloco, x.size() - i)));
  return x;
}
Sig CtrlGanhoComClipping(const Sig& x) {  // normaliza em -1 dBFS e aplica +3 dB com corte duro
  double k = std::pow(10.0, -1.0 / 20.0) / std::max(Peak(x), 1e-12) * std::pow(10.0, 3.0 / 20.0);
  Sig y(x.size());
  for (size_t i = 0; i < x.size(); ++i) y[i] = std::clamp(static_cast<float>(x[i] * k), -1.0f, 1.0f);
  return y;
}
Sig CtrlPitch(const Sig& x, double cents) {  // reamostragem (interp. cubica) mantendo a duracao: pitch e andamento mudam
  const double r = std::pow(2.0, cents / 1200.0);
  Sig y(x.size(), 0.0f);
  auto at = [&](long i) { return static_cast<double>(x[static_cast<size_t>(std::clamp<long>(i, 0, static_cast<long>(x.size()) - 1))]); };
  for (size_t i = 0; i < y.size(); ++i) {
    const double p = i * r;
    const long i1 = static_cast<long>(p);
    const double t = p - i1, a = at(i1 - 1), b = at(i1), c = at(i1 + 1), d = at(i1 + 2);
    y[i] = static_cast<float>(b + 0.5 * t * (c - a + t * (2 * a - 5 * b + 4 * c - d + t * (3 * (b - c) + d - a))));
  }
  return y;
}
// Supressao agressiva: gate por quadro de 10 ms que silencia tudo abaixo de 60% do RMS global (corta consoantes e
// finais de palavra) e ainda atenua o resto em 3 dB; transicoes suavizadas para nao gerar cliques.
Sig CtrlSupressaoAgressiva(const Sig& x) {
  const size_t q = kRate / 100;
  const double limiar = 0.6 * Rms(x);
  Sig y(x.size());
  double g = 1.0;
  const double alvo_aberto = std::pow(10.0, -3.0 / 20.0);
  for (size_t i = 0; i < x.size(); i += q) {
    const size_t n = std::min(q, x.size() - i);
    double e = 0;
    for (size_t k = 0; k < n; ++k) e += double(x[i + k]) * x[i + k];
    const double alvo = std::sqrt(e / n) < limiar ? 0.0 : alvo_aberto;
    for (size_t k = 0; k < n; ++k) {
      g += 0.02 * (alvo - g);
      y[i + k] = static_cast<float>(x[i + k] * g);
    }
  }
  return y;
}
Sig CtrlCliques(const Sig& x, int n) {  // n impulsos isolados de +-0.6 em posicoes deterministicas
  Sig y = x;
  Rng r(77);
  for (int i = 0; i < n; ++i) {
    size_t pos = 2000 + static_cast<size_t>(r.Uniform() * (x.size() - 4000));
    y[pos] = (i & 1) ? 0.6f : -0.6f;
  }
  return y;
}
Sig CtrlCorte(const Sig& x, double ms) {  // remove `ms` do meio
  size_t n = static_cast<size_t>(ms * kRate / 1000), ini = x.size() / 2;
  Sig y(x.begin(), x.begin() + ini);
  y.insert(y.end(), x.begin() + ini + n, x.end());
  return y;
}

}  // namespace

int main(int argc, char** argv) {
  if (argc < 2) { std::fprintf(stderr, "uso: corpus_tool <dir>\n"); return 2; }
  const std::string d = argv[1];
  mkdir(d.c_str(), 0755);
  auto out = [&](const char* nome, const Sig& x) { WriteWav16(d + "/" + nome + ".wav", x); };

  // corpus sintetico base
  Sig seno = Seno(5.0, 1000.0, 0.5);              out("seno_1k", seno);
  Sig varr = Varredura(5.0, 20.0, 20000.0, 0.5);   out("varredura", varr);
  Sig fala = FalaSintetica(10.0, 42);              out("fala_sintetica", fala);
  Sig branco = RuidoBranco(5.0, 1);                ScaleToRms(branco, -20); out("ruido_branco", branco);
  Sig rosa = RuidoRosa(5.0, 2);                    ScaleToRms(rosa, -20);   out("ruido_rosa", rosa);
  Sig marrom = RuidoMarrom(5.0, 3);                ScaleToRms(marrom, -20); out("ruido_marrom", marrom);
  Sig zumbido = Zumbido60(5.0, 0.2);               out("zumbido_60hz", zumbido);
  Sig vent = Ventilador(10.0, 4);                  ScaleToRms(vent, -20);   out("ruido_ventilador", vent);

  // misturas fala + ruido (entrada da Fase 14) e fala + zumbido (entrada da Fase 15)
  out("fala_ventilador_snr0", MisturaSnr(fala, vent, 0.0));
  out("fala_ventilador_snr10", MisturaSnr(fala, vent, 10.0));
  out("fala_rosa_snr0", MisturaSnr(fala, rosa, 0.0));
  out("fala_zumbido_snr10", MisturaSnr(fala, zumbido, 10.0));

  // saidas "boas" e controles positivos para cada base (fala sintetica + falas reais passadas na linha de comando)
  struct Base { std::string nome; Sig x; };
  std::vector<Base> bases{{"fala_sintetica", fala}};
  for (int i = 2; i < argc; ++i) {
    std::string caminho = argv[i];
    std::string nome = caminho.substr(caminho.find_last_of('/') + 1);
    bases.push_back({nome.substr(0, nome.size() - 4), LerWav16(caminho)});
  }
  for (const auto& b : bases) {
    const std::string n = b.nome;
    if (n != "fala_sintetica") out(n.c_str(), b.x);
    out((n + "__hp80").c_str(), ProcessaCadeia(b.x, 80.0));
    out((n + "__ctrl_ganho3db_clip").c_str(), CtrlGanhoComClipping(b.x));
    out((n + "__ctrl_pitch_mais50c").c_str(), CtrlPitch(b.x, 50.0));
    out((n + "__ctrl_corte50ms").c_str(), CtrlCorte(b.x, 50.0));
    out((n + "__ctrl_cliques").c_str(), CtrlCliques(b.x, 40));
    out((n + "__ctrl_supressao_agressiva").c_str(), CtrlSupressaoAgressiva(b.x));
    out((n + "__ctrl_ruido_reinjetado").c_str(), MisturaSnr(b.x, rosa, 15.0));  // ruido de volta a 15 dB SNR
  }
  FILE* lista = std::fopen((d + "/bases_sinteticas.txt").c_str(), "w");
  for (const auto& b : bases) std::fprintf(lista, "%s\n", b.nome.c_str());
  std::fclose(lista);
  std::printf("corpus gerado em %s\n", d.c_str());
  return 0;
}
