// Testes do nucleo MP3 no host (sem Android). Sai com codigo != 0 se algum falhar.
#include <algorithm>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <vector>
#include "../mp3_encoder.h"

using gravasom::Mp3Encoder;

static int g_falhas = 0;
#define VERIFICA(cond, ...)                                  \
  do {                                                       \
    if (!(cond)) {                                           \
      ++g_falhas;                                            \
      std::printf("FALHOU %s:%d: ", __FILE__, __LINE__);    \
      std::printf(__VA_ARGS__);                              \
      std::printf("\n");                                     \
    }                                                        \
  } while (0)

static std::vector<int16_t> Seno(double segundos, int taxa, double freq = 1000.0, double amp = 0.5) {
  std::vector<int16_t> v(static_cast<size_t>(segundos * taxa));
  for (size_t i = 0; i < v.size(); ++i)
    v[i] = static_cast<int16_t>(std::sin(2 * M_PI * freq * i / taxa) * amp * 32767);
  return v;
}

// Codifica `pcm` em blocos de `bloco` amostras (0 = tudo de uma vez).
static std::vector<uint8_t> Codifica(const std::vector<int16_t>& pcm, int taxa, int kbps, size_t bloco) {
  auto enc = Mp3Encoder::Create(taxa, 1, kbps, 5);
  std::vector<uint8_t> saida;
  if (!enc) return saida;
  std::vector<uint8_t> buf(1 << 20);
  size_t passo = bloco == 0 ? pcm.size() : bloco;
  for (size_t i = 0; i < pcm.size(); i += passo) {
    int n = static_cast<int>(std::min(passo, pcm.size() - i));
    int w = enc->Encode(pcm.data() + i, n, buf.data(), static_cast<int>(buf.size()));
    if (w < 0) return {};
    saida.insert(saida.end(), buf.begin(), buf.begin() + w);
  }
  int w = enc->Flush(buf.data(), static_cast<int>(buf.size()));
  if (w < 0) return {};
  saida.insert(saida.end(), buf.begin(), buf.begin() + w);
  return saida;
}

// Conta quadros MPEG-1 Layer III e devolve a duracao pela soma dos quadros (1152 amostras cada).
struct Quadros { int quantos = 0; int taxa = 0; size_t bytes_validos = 0; };
static Quadros ParseQuadros(const std::vector<uint8_t>& d) {
  static const int kbps[16] = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0};
  static const int taxas[4] = {44100, 48000, 32000, 0};
  Quadros q;
  size_t pos = 0;
  // pula ID3v2, se houver
  if (d.size() > 10 && std::memcmp(d.data(), "ID3", 3) == 0)
    pos = 10 + ((d[6] & 0x7F) << 21 | (d[7] & 0x7F) << 14 | (d[8] & 0x7F) << 7 | (d[9] & 0x7F));
  while (pos + 4 <= d.size()) {
    if (d[pos] != 0xFF || (d[pos + 1] & 0xFE) != 0xFA) break;  // MPEG1 Layer3
    int br = kbps[d[pos + 2] >> 4];
    int sr = taxas[(d[pos + 2] >> 2) & 3];
    int pad = (d[pos + 2] >> 1) & 1;
    if (br == 0 || sr == 0) break;
    size_t len = 144u * br * 1000u / sr + pad;
    if (pos + len > d.size()) break;
    q.quantos++; q.taxa = sr; pos += len; q.bytes_validos = pos;
  }
  return q;
}

static void TestaParametrosInvalidos() {
  VERIFICA(Mp3Encoder::Create(44100, 0, 128, 5) == nullptr, "canais=0 deveria falhar");
  VERIFICA(Mp3Encoder::Create(44100, 3, 128, 5) == nullptr, "canais=3 deveria falhar");
  VERIFICA(Mp3Encoder::Create(44100, 1, 0, 5) == nullptr, "bitrate=0 deveria falhar");
  VERIFICA(Mp3Encoder::Create(0, 1, 128, 5) == nullptr, "taxa=0 deveria falhar");
  VERIFICA(Mp3Encoder::LiveInstances() == 0, "instancias vivas apos falhas: %d", Mp3Encoder::LiveInstances());
}

static void TestaDuracaoETamanho() {
  auto mp3 = Codifica(Seno(3.0, 44100), 44100, 128, 0);
  VERIFICA(!mp3.empty(), "saida vazia");
  Quadros q = ParseQuadros(mp3);
  double dur_ms = q.quantos * 1152.0 * 1000.0 / (q.taxa ? q.taxa : 1);
  std::printf("medida: quadros=%d duracao=%.1f ms bytes=%zu validos=%zu\n", q.quantos, dur_ms, mp3.size(), q.bytes_validos);
  VERIFICA(q.quantos > 0, "nenhum quadro MPEG valido");
  VERIFICA(std::fabs(dur_ms - 3000.0) <= 100.0, "duracao %.1f ms fora de 3000 +-100", dur_ms);
  double esperado = 128000.0 / 8 * 3.0;
  VERIFICA(mp3.size() > esperado * 0.85 && mp3.size() < esperado * 1.15, "tamanho %zu x esperado %.0f", mp3.size(), esperado);
  VERIFICA(q.bytes_validos <= mp3.size(), "bytes validos acima do total");
}

static void TestaSilencio() {
  auto mp3 = Codifica(std::vector<int16_t>(44100, 0), 44100, 128, 0);
  Quadros q = ParseQuadros(mp3);
  VERIFICA(q.quantos > 0, "silencio nao gerou quadros");
}

static void TestaDeterminismoEBlocos() {
  auto pcm = Seno(2.0, 44100);
  auto a = Codifica(pcm, 44100, 128, 0);
  auto b = Codifica(pcm, 44100, 128, 0);
  auto c = Codifica(pcm, 44100, 128, 441);  // blocos de 10 ms
  VERIFICA(!a.empty() && a == b, "mesma entrada deu saidas diferentes");
  VERIFICA(a == c, "blocos de 10 ms divergem do processamento inteiro (%zu x %zu bytes)", a.size(), c.size());
}

static void TestaSemVazamento() {
  auto pcm = Seno(0.5, 44100);
  for (int i = 0; i < 200; ++i) {
    auto enc = Mp3Encoder::Create(44100, 1, 128, 5);
    VERIFICA(enc != nullptr, "create falhou na iteracao %d", i);
    if (!enc) return;
    std::vector<uint8_t> buf(1 << 18);
    enc->Encode(pcm.data(), static_cast<int>(pcm.size()), buf.data(), static_cast<int>(buf.size()));
    // destruir sem Flush tambem nao pode vazar
  }
  VERIFICA(Mp3Encoder::LiveInstances() == 0, "vazou %d instancias", Mp3Encoder::LiveInstances());
}

static void TestaBufferPequeno() {
  auto enc = Mp3Encoder::Create(44100, 1, 128, 5);
  VERIFICA(enc != nullptr, "create");
  if (!enc) return;
  auto pcm = Seno(1.0, 44100);
  uint8_t pequeno[16];
  int w = enc->Encode(pcm.data(), static_cast<int>(pcm.size()), pequeno, sizeof pequeno);
  VERIFICA(w < 0, "buffer pequeno deveria retornar erro, retornou %d", w);
}

static void TestaQuadroDeTag() {
  auto enc = Mp3Encoder::Create(44100, 1, 128, 5);
  VERIFICA(enc != nullptr, "create");
  if (!enc) return;
  auto pcm = Seno(1.0, 44100);
  std::vector<uint8_t> buf(1 << 18), tag(4096);
  VERIFICA(enc->Encode(pcm.data(), static_cast<int>(pcm.size()), buf.data(), static_cast<int>(buf.size())) >= 0, "encode");
  VERIFICA(enc->Flush(buf.data(), static_cast<int>(buf.size())) >= 0, "flush");
  int n = enc->GetTagFrame(tag.data(), static_cast<int>(tag.size()));
  VERIFICA(n > 0, "quadro de tag vazio (%d)", n);
  if (n > 0) {
    VERIFICA(tag[0] == 0xFF && (tag[1] & 0xFE) == 0xFA, "quadro de tag sem sincronismo MPEG");
    bool achou = false;
    for (int i = 0; i + 4 <= n && i < 64; ++i)
      if (std::memcmp(&tag[i], "Info", 4) == 0 || std::memcmp(&tag[i], "Xing", 4) == 0) achou = true;
    VERIFICA(achou, "tag sem Info/Xing");
  }
}

// Contrato de `frames`: amostras POR CANAL. Em estereo intercalado, N frames consomem 2N shorts.
// Regressao do bug do PcmMp3Recorder, que passava o total de shorts lidos (L+R) como `frames`.
static void TestaEstereoContratoDeFrames() {
  const int taxa = 44100;
  auto mono = Seno(2.0, taxa);                       // 2 s = 88200 frames
  std::vector<int16_t> estereo(mono.size() * 2);     // L = R intercalado: 176400 shorts
  for (size_t i = 0; i < mono.size(); ++i) { estereo[2 * i] = mono[i]; estereo[2 * i + 1] = mono[i]; }

  auto codifica = [&](int frames) {
    auto enc = Mp3Encoder::Create(taxa, 2, 128, 5);
    std::vector<uint8_t> saida, buf(1 << 20);
    if (!enc) return saida;
    int w = enc->Encode(estereo.data(), frames, buf.data(), static_cast<int>(buf.size()));
    if (w < 0) return std::vector<uint8_t>{};
    saida.insert(saida.end(), buf.begin(), buf.begin() + w);
    w = enc->Flush(buf.data(), static_cast<int>(buf.size()));
    if (w > 0) saida.insert(saida.end(), buf.begin(), buf.begin() + w);
    return saida;
  };

  Quadros certo = ParseQuadros(codifica(static_cast<int>(mono.size())));
  double dur = certo.quantos * 1152.0 * 1000.0 / (certo.taxa ? certo.taxa : 1);
  std::printf("medida: estereo frames corretos -> %.1f ms\n", dur);
  VERIFICA(std::fabs(dur - 2000.0) <= 100.0, "duracao estereo %.1f ms fora de 2000 +-100", dur);
  // Passar o total de shorts como frames faz o LAME ler alem do buffer: o ASan acusa; nao executamos isso aqui.
  // A protecao esperada fica no JNI (GetArrayLength(pcm) >= frames * channels) e e verificada no teste instrumentado.
}

// Encode com `frames` negativo ou ponteiro nulo nunca pode escrever nem travar.
static void TestaEntradasInvalidasNaoEscrevem() {
  auto enc = Mp3Encoder::Create(44100, 1, 128, 5);
  VERIFICA(enc != nullptr, "create");
  if (!enc) return;
  uint8_t out[64];
  std::memset(out, 0xAB, sizeof out);
  int16_t pcm[4] = {0, 0, 0, 0};
  int w = enc->Encode(pcm, -1, out, sizeof out);
  VERIFICA(w <= 0, "frames negativo deveria falhar ou nao escrever, retornou %d", w);
  for (size_t i = 0; i < sizeof out; ++i) VERIFICA(out[i] == 0xAB, "escreveu fora do esperado em out[%zu]", i);
  w = enc->Encode(pcm, 0, out, sizeof out);
  VERIFICA(w >= 0, "frames=0 deveria ser no-op, retornou %d", w);
}

int main() {
  TestaEstereoContratoDeFrames();
  TestaEntradasInvalidasNaoEscrevem();
  TestaQuadroDeTag();
  TestaParametrosInvalidos();
  TestaDuracaoETamanho();
  TestaSilencio();
  TestaDeterminismoEBlocos();
  TestaSemVazamento();
  TestaBufferPequeno();
  std::printf(g_falhas == 0 ? "OK: todos os testes passaram\n" : "FALHAS: %d\n", g_falhas);
  return g_falhas == 0 ? 0 : 1;
}
