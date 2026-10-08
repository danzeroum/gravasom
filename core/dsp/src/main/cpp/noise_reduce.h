// Redutor de ruido: o RNNoise (48 kHz, quadros de 10 ms) dentro da cadeia, para QUALQUER taxa de amostragem (reamostra para 48 kHz e
// volta). A saida do passo e o sinal molhado ATRASADO em exatamente LatencySamples() amostras, e o seco e atrasado o mesmo tanto:
// a intensidade (0..1) mistura os dois por amostra, sem pente. Intensidade 0 devolve o seco atrasado BIT A BIT (o RNNoise nem roda).
#pragma once
#include <memory>

#include "dsp_chain.h"

namespace gravasom::dsp {

constexpr int kNrFrame = 480;  // quadro do RNNoise: 10 ms a 48 kHz

// Um quadro de 480 amostras a 48 kHz, em [-1, 1]. O RNNoise real fica em rnnoise_denoiser.cpp; os testes injetam um falso.
class FrameDenoiser {
 public:
  virtual ~FrameDenoiser() = default;
  virtual void Reset() = 0;
  virtual void Process(float* out, const float* in) = 0;
};

std::unique_ptr<Step> MakeNoiseReductionWith(std::unique_ptr<FrameDenoiser> denoiser, float intensity, int sample_rate);

// RNNoise v0.2 embutido. nullptr se a taxa for invalida (fora de 8-96 kHz).
std::unique_ptr<Step> MakeNoiseReduction(float intensity, int sample_rate);

}  // namespace gravasom::dsp
