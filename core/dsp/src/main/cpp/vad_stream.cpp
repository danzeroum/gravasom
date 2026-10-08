#include "vad_stream.h"

#include <algorithm>

extern "C" {
#include "webrtc/common_audio/vad/include/webrtc_vad.h"
}

namespace gravasom::dsp {

VadStream::VadStream(int mode) {
  if (mode < 0 || mode > 3) return;
  WebRtcVadInst* inst = nullptr;
  if (WebRtcVad_Create(&inst) != 0 || inst == nullptr) return;
  if (WebRtcVad_Init(inst) != 0 || WebRtcVad_set_mode(inst, mode) != 0) {
    WebRtcVad_Free(inst);
    return;
  }
  inst_ = inst;
}

VadStream::~VadStream() {
  if (inst_ != nullptr) WebRtcVad_Free(inst_);
}

int VadStream::Process(const int16_t* samples, int n, std::vector<uint8_t>* out) {
  if (inst_ == nullptr || samples == nullptr || n <= 0) return 0;
  int frames = 0;
  int i = 0;
  while (i < n) {
    const int take = std::min(kFrameSamples - pending_len_, n - i);
    for (int k = 0; k < take; ++k) pending_[pending_len_ + k] = samples[i + k];
    pending_len_ += take;
    i += take;
    if (pending_len_ == kFrameSamples) {
      const int r = WebRtcVad_Process(inst_, kSampleRate, pending_, kFrameSamples);
      out->push_back(r == 1 ? 1 : 0);  // r < 0 (erro) conta como nao-fala; nunca acontece com 16 kHz/320
      pending_len_ = 0;
      ++frames;
    }
  }
  return frames;
}

int VadStream::Flush(std::vector<uint8_t>* out) {
  if (inst_ == nullptr || pending_len_ == 0) return 0;
  for (int k = pending_len_; k < kFrameSamples; ++k) pending_[k] = 0;
  pending_len_ = kFrameSamples;
  const int r = WebRtcVad_Process(inst_, kSampleRate, pending_, kFrameSamples);
  out->push_back(r == 1 ? 1 : 0);
  pending_len_ = 0;
  return 1;
}

}  // namespace gravasom::dsp
