// Reamostrador racional em fluxo, de FASE ZERO: a saida ja sai alinhada no tempo com a entrada (o atraso do filtro e compensado
// esperando amostras futuras; o custo e LookaheadIn()). Sinc com janela de Kaiser, banda a 95% de Nyquist da menor taxa.
// Determinista: o resultado nao depende de como a entrada foi dividida em blocos.
#pragma once
#include <cstddef>
#include <vector>

namespace gravasom::dsp {

class RationalResampler {
 public:
  RationalResampler(int in_rate, int out_rate);
  // Consome n amostras e ACRESCENTA a *out as saidas que ja podem ser calculadas.
  void Push(const float* in, size_t n, std::vector<float>* out);
  // Amostras de entrada que precisam chegar alem da posicao de uma saida para ela ser fechada (0 se as taxas sao iguais).
  int LookaheadIn() const { return passthrough_ ? 0 : half_; }
  bool Passthrough() const { return passthrough_; }
  void Reset();

 private:
  bool passthrough_;
  int up_ = 1;
  int down_ = 1;
  int half_ = 0;                         // largura do filtro em amostras de entrada, de cada lado
  std::vector<std::vector<float>> coef_;  // [fase][2*half_]
  std::vector<float> buf_;                // entrada de indice absoluto lo_ ... lo_ + buf_.size() - 1
  long long lo_ = 0;
  long long total_in_ = 0;                // amostras de entrada recebidas
  long long next_out_ = 0;                // indice da proxima saida
};

}  // namespace gravasom::dsp
