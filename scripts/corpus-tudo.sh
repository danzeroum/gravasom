#!/usr/bin/env bash
# Corpus completo em $1: falas reais (baixadas por hash) + sinteticos + saidas boas + controles positivos.
set -euo pipefail
AQUI="$(cd "$(dirname "$0")" && pwd)"
OUT="${1:?uso: corpus-tudo.sh <dir_saida>}"
mkdir -p "$OUT/reais"
"$AQUI/corpus-real.sh" "$OUT/reais"
"$AQUI/corpus-gerar.sh" "$OUT" "$OUT"/reais/real_fala_*.wav
# as falas reais ficam na raiz tambem (mesmos nomes que as bases)
cp "$OUT"/reais/real_fala_*.wav "$OUT"/ 2>/dev/null || true
