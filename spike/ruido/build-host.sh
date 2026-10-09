#!/usr/bin/env bash
# Compila o RNNoise v0.2 + rnnoise_wav.c no HOST (gcc). Uso: build-host.sh <variante: completo|leve> <dir_saida>
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
V="${1:?variante}"; OUT="${2:?dir}"
SRC="$OUT/fontes-$V"
"$RAIZ/scripts/buscar-rnnoise.sh" "$SRC" "$V"
mkdir -p "$OUT"
S="$SRC/src"
gcc -O3 -DNDEBUG -I"$SRC/include" -I"$S" -o "$OUT/rnnoise_wav_$V" "$RAIZ/spike/ruido/rnnoise_wav.c" \
  "$S/denoise.c" "$S/rnn.c" "$S/pitch.c" "$S/kiss_fft.c" "$S/celt_lpc.c" "$S/nnet.c" "$S/nnet_default.c" \
  "$S/parse_lpcnet_weights.c" "$S/rnnoise_data.c" "$S/rnnoise_tables.c" -lm
ls -l "$OUT/rnnoise_wav_$V"
