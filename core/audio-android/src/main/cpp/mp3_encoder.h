// Nucleo C++ do codificador MP3 (LAME), sem JNI: testavel no host e no device.
#pragma once
#include <cstdint>
#include <memory>

namespace gravasom {

class Mp3Encoder {
 public:
  // Retorna nullptr para parametros invalidos ou falha do LAME.
  static std::unique_ptr<Mp3Encoder> Create(int sample_rate, int channels, int bitrate_kbps, int quality);
  ~Mp3Encoder();

  // `frames` = amostras por canal; estereo entra intercalado. Retorna bytes escritos em `out`, ou <0 em erro.
  int Encode(const int16_t* pcm, int frames, uint8_t* out, int out_capacity);
  // Descarrega o que sobrou (e escreve o rodape Xing/Info). Retorna bytes escritos, ou <0 em erro.
  int Flush(uint8_t* out, int out_capacity);

  // Quadro Xing/Info final (valido so apos Flush): deve sobrescrever o primeiro quadro do arquivo.
  // Retorna bytes escritos, 0 se nao houver, ou <0 em erro.
  int GetTagFrame(uint8_t* out, int out_capacity);

  // Canais de entrada (1 ou 2): o chamador usa para validar o tamanho do buffer PCM (frames * canais).
  int Channels() const;

  // Instancias vivas: contador para provar ausencia de vazamento.
  static int LiveInstances();

 private:
  Mp3Encoder() = default;
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

}  // namespace gravasom
