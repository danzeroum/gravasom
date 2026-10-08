// Ferramenta de linha de comando do harness: le WAV mono 16 bits 16 kHz e imprime, por modo (0..3), uma linha de
// '0'/'1' por quadro de 20 ms. Uso: vad_tool <arquivo.wav> [modo]
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <vector>

#include "../main/cpp/vad_stream.h"

int main(int argc, char** argv) {
  if (argc < 2) return 2;
  FILE* f = std::fopen(argv[1], "rb");
  if (f == nullptr) return 3;
  std::vector<uint8_t> bytes;
  uint8_t buf[65536];
  size_t n;
  while ((n = std::fread(buf, 1, sizeof buf, f)) > 0) bytes.insert(bytes.end(), buf, buf + n);
  std::fclose(f);
  size_t off = 12;  // RIFF....WAVE
  while (off + 8 <= bytes.size()) {
    const uint32_t size = bytes[off + 4] | (bytes[off + 5] << 8) | (bytes[off + 6] << 16) | (static_cast<uint32_t>(bytes[off + 7]) << 24);
    if (bytes[off] == 'd' && bytes[off + 1] == 'a' && bytes[off + 2] == 't' && bytes[off + 3] == 'a') {
      off += 8;
      std::vector<int16_t> pcm((bytes.size() - off) / 2);
      for (size_t i = 0; i < pcm.size(); ++i) pcm[i] = static_cast<int16_t>(bytes[off + 2 * i] | (bytes[off + 2 * i + 1] << 8));
      for (int mode = 0; mode <= 3; ++mode) {
        if (argc > 2 && std::atoi(argv[2]) != mode) continue;
        gravasom::dsp::VadStream vad(mode);
        std::vector<uint8_t> flags;
        vad.Process(pcm.data(), static_cast<int>(pcm.size()), &flags);
        vad.Flush(&flags);
        for (uint8_t v : flags) std::putchar('0' + v);
        std::putchar('\n');
      }
      return 0;
    }
    off += 8 + size + (size & 1);
  }
  return 4;
}
