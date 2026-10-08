#!/usr/bin/env bash
# Gera a gravacao de 10 min (WAV 16 kHz mono) do spike a partir da fala pt-BR baixada por hash. Uso: gerar-clip.sh <saida.wav>
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/../../.." && pwd)"
OUT="${1:?uso: gerar-clip.sh <saida.wav>}"
PT="${PT_DIR:-$RAIZ/tests-out/pt}"
[ -f "$PT/fleurs-dev.tsv" ] || "$RAIZ/scripts/corpus-pt.sh" "$PT" asr
mkdir -p "$(dirname "$OUT")"
python3 -I "$RAIZ/scripts/asr_corpus.py" "$PT" "$RAIZ/corpus/falas_pt_asr.txt" "$OUT" "${OUT%.wav}.ref.txt" 600
