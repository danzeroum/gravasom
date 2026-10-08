#!/usr/bin/env bash
# Compila o VAD (WebRTC vendorizado + VadStream) no host com ASan/UBSan e roda os testes. 0 verde, !=0 vermelho.
set -euo pipefail
# O C do WebRTC desloca inteiros negativos (UB formal, definido no gcc/clang/NDK): so essa checagem fica fora do codigo vendorizado.
D="$(cd "$(dirname "$0")/../core/dsp/src" && pwd)"
V="$D/main/cpp/webrtc_vad"
OUT="$(mktemp -d)"
FONTES_C=$(ls "$V"/webrtc/common_audio/signal_processing/*.c "$V"/webrtc/common_audio/vad/*.c)
mkdir -p "$OUT/c"
for f in $FONTES_C; do
  gcc -O1 -g -w -DWEBRTC_POSIX -fsanitize=address,undefined -fno-sanitize=shift -fno-omit-frame-pointer -I"$V" -c "$f" -o "$OUT/c/$(basename "$f").o"
done
g++ -std=c++17 -O1 -g -Wall -Wextra -fsanitize=address,undefined -fno-omit-frame-pointer -I"$V" \
  "$D/main/cpp/vad_stream.cpp" "$D/hosttest/vad_stream_test.cpp" "$OUT"/c/*.o -lm -o "$OUT/vad_stream_test"
ASAN_OPTIONS=detect_leaks=1 UBSAN_OPTIONS=halt_on_error=1:print_stacktrace=1 "$OUT/vad_stream_test"
