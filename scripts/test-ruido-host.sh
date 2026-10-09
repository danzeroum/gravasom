#!/usr/bin/env bash
# Testes do redutor de ruido no host (ASan/UBSan no nosso codigo). Parte 1: pipeline com denoiser injetado (sem RNNoise).
# Parte 2 (com RNNoise v0.2 baixado por hash, compilado sem sanitizers: codigo de terceiros): comportamento do modelo.
# 0 verde, !=0 vermelho. Uso: test-ruido-host.sh [completo|leve]
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
D="$RAIZ/core/dsp/src"
V="${1:-completo}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
SAN="-fsanitize=address,undefined -fno-omit-frame-pointer"
echo "== parte 1: pipeline (reamostrador, quadros, latencia, mistura) =="
g++ -std=c++17 -O1 -g -ffp-contract=off $SAN -Wall -Wextra \
  "$D/main/cpp/dsp_chain.cpp" "$D/main/cpp/resampler.cpp" "$D/main/cpp/noise_reduce.cpp" "$D/hosttest/noise_reduce_test.cpp" -lm -o "$OUT/nr_test"
ASAN_OPTIONS=detect_leaks=1 UBSAN_OPTIONS=halt_on_error=1:print_stacktrace=1 "$OUT/nr_test"
echo "== parte 2: RNNoise ($V) =="
"$RAIZ/scripts/buscar-rnnoise.sh" "$OUT/rnnoise" "$V"
R="$OUT/rnnoise/src"
mkdir -p "$OUT/obj"
for f in denoise rnn pitch kiss_fft celt_lpc nnet nnet_default parse_lpcnet_weights rnnoise_data rnnoise_tables; do
  gcc -O2 -DNDEBUG -w -I"$D/main/cpp/rnnoise_shim" -I"$OUT/rnnoise/include" -I"$R" -c "$R/$f.c" -o "$OUT/obj/$f.o"
done
g++ -std=c++17 -O1 -g -ffp-contract=off $SAN -Wall -Wextra -I"$OUT/rnnoise/include" \
  "$D/main/cpp/dsp_chain.cpp" "$D/main/cpp/resampler.cpp" "$D/main/cpp/noise_reduce.cpp" "$D/main/cpp/rnnoise_denoiser.cpp" \
  "$D/hosttest/rnnoise_denoiser_test.cpp" "$OUT"/obj/*.o -lm -o "$OUT/rn_test"
ASAN_OPTIONS=detect_leaks=1 UBSAN_OPTIONS=halt_on_error=1:print_stacktrace=1 "$OUT/rn_test"
