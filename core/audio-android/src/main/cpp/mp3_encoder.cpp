#include "mp3_encoder.h"

#include <atomic>
#include <cstddef>

#include "lame.h"

namespace gravasom {

namespace {
std::atomic<int> g_instancias{0};
constexpr int kTaxaMin = 8000;
constexpr int kTaxaMax = 48000;
constexpr int kBitrateMax = 320;
}  // namespace

struct Mp3Encoder::Impl {
  lame_t lame = nullptr;
  int channels = 1;
  ~Impl() {
    if (lame != nullptr) lame_close(lame);
  }
};

std::unique_ptr<Mp3Encoder> Mp3Encoder::Create(int sample_rate, int channels, int bitrate_kbps, int quality) {
  if (sample_rate < kTaxaMin || sample_rate > kTaxaMax) return nullptr;
  if (channels < 1 || channels > 2) return nullptr;
  if (bitrate_kbps < 8 || bitrate_kbps > kBitrateMax) return nullptr;
  if (quality < 0 || quality > 9) return nullptr;

  auto impl = std::make_unique<Impl>();
  impl->lame = lame_init();
  if (impl->lame == nullptr) return nullptr;
  impl->channels = channels;
  lame_set_in_samplerate(impl->lame, sample_rate);
  lame_set_num_channels(impl->lame, channels);
  lame_set_mode(impl->lame, channels == 1 ? MONO : JOINT_STEREO);
  lame_set_VBR(impl->lame, vbr_off);
  lame_set_brate(impl->lame, bitrate_kbps);
  lame_set_quality(impl->lame, quality);
  if (lame_init_params(impl->lame) < 0) return nullptr;

  std::unique_ptr<Mp3Encoder> enc(new Mp3Encoder());
  enc->impl_ = std::move(impl);
  g_instancias.fetch_add(1);
  return enc;
}

Mp3Encoder::~Mp3Encoder() {
  // impl_ so existe em instancias contadas por Create().
  if (impl_) g_instancias.fetch_sub(1);
}

int Mp3Encoder::Encode(const int16_t* pcm, int frames, uint8_t* out, int out_capacity) {
  if (!impl_ || pcm == nullptr || out == nullptr || frames < 0 || out_capacity <= 0) return -1;
  if (frames == 0) return 0;
  // LAME exige buffer >= 1.25 * amostras + 7200; se for menor, falha sem escrever.
  const long minimo = static_cast<long>(1.25 * frames) + 7200;
  if (out_capacity < minimo) return -1;
  int w = impl_->channels == 1
              ? lame_encode_buffer(impl_->lame, pcm, pcm, frames, out, out_capacity)
              : lame_encode_buffer_interleaved(impl_->lame, const_cast<short*>(pcm), frames, out, out_capacity);
  return w;
}

int Mp3Encoder::Flush(uint8_t* out, int out_capacity) {
  if (!impl_ || out == nullptr || out_capacity < 7200) return -1;
  return lame_encode_flush(impl_->lame, out, out_capacity);
}

int Mp3Encoder::GetTagFrame(uint8_t* out, int out_capacity) {
  if (!impl_ || out == nullptr || out_capacity <= 0) return -1;
  const size_t n = lame_get_lametag_frame(impl_->lame, out, static_cast<size_t>(out_capacity));
  if (n > static_cast<size_t>(out_capacity)) return -1;  // buffer insuficiente
  return static_cast<int>(n);
}

int Mp3Encoder::LiveInstances() { return g_instancias.load(); }

}  // namespace gravasom
