#!/usr/bin/env bash
# Baixa o LibriSpeech dev-clean POR HASH (cache em $CORPUS_CACHE), extrai as locucoes de corpus/falas_reais.txt
# e converte para WAV mono 16 bits 48 kHz em $1. Licenca: CC BY 4.0 (ver CORPUS.md).
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:?uso: corpus-real.sh <dir_saida>}"
CACHE="${CORPUS_CACHE:-$HOME/.cache/gravasom-corpus}"
mkdir -p "$OUT" "$CACHE"
read -r HASH URL < <(grep -v '^#' "$RAIZ/corpus/corpus.sha256" | head -1 | awk '{print $1, $2}')
TAR="$CACHE/dev-clean.tar.gz"
if [ ! -f "$TAR" ] || [ "$(sha256sum "$TAR" | cut -d' ' -f1)" != "$HASH" ]; then
  curl -fsSL --retry 3 -o "$TAR" "$URL"
fi
echo "$HASH  $TAR" | sha256sum -c - >&2   # falha se o hash nao confere
LISTA=$(grep -v '^#' "$RAIZ/corpus/falas_reais.txt" | grep .)
TMP="$(mktemp -d)"
tar xzf "$TAR" -C "$TMP" $LISTA
i=0
: > "$OUT/bases.txt"
echo fala_sintetica >> "$OUT/bases.txt"
for f in $LISTA; do
  i=$((i+1))
  nome="real_fala_$i"
  ffmpeg -v error -y -i "$TMP/$f" -ac 1 -ar 48000 -sample_fmt s16 "$OUT/$nome.wav"
  echo "$nome" >> "$OUT/bases.txt"
  echo "$nome <- $(basename "$f") sha256(fonte)=$(sha256sum "$TMP/$f" | cut -c1-16)…" >&2
done
rm -rf "$TMP"
