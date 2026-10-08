// Nucleo DSP do Gravasom: cadeia de passos em blocos, deterministica, sem JNI.
// Quadro padrao: 10 ms a 48 kHz = 480 amostras. Processamento em float [-1, 1], mono.
#pragma once
#include <cstdint>
#include <memory>
#include <vector>

namespace gravasom::dsp {

constexpr int kSampleRate = 48000;
constexpr int kFrameSamples = kSampleRate / 100;  // 10 ms

// Um passo da cadeia: estado interno proprio, sem alocacao em Process().
class Step {
 public:
  virtual ~Step() = default;
  virtual void Reset() = 0;
  virtual int LatencySamples() const { return 0; }
  virtual void Process(float* x, int n) = 0;
};

// Filtro passa-altas Butterworth de 2a ordem (fc em Hz). Estado em double.
std::unique_ptr<Step> MakeHighPass(double cutoff_hz, int sample_rate = kSampleRate);

class Chain {
 public:
  explicit Chain(int sample_rate = kSampleRate);
  ~Chain();
  void Add(std::unique_ptr<Step> step);
  void Reset();
  // bypass=true: nao toca nos dados (bytes identicos, mesmo NaN). false: roda os passos e sanitiza a saida
  // (NaN/Inf -> 0, limita a [-1, 1]).
  void SetBypass(bool bypass);
  // Intensidade 0..1: mistura seca/molhada por amostra. 0 => caminho identico ao bypass.
  void SetIntensity(float intensity);
  void Process(float* x, int n);
  int LatencySamples() const;
  int SampleRate() const;
  // Amostras de um quadro de 10 ms na taxa da cadeia.
  int FrameSamples() const { return SampleRate() / 100; }

 private:
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

}  // namespace gravasom::dsp
