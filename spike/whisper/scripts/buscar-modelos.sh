#!/usr/bin/env bash
# Baixa os modelos do whisper.cpp POR HASH (corpus/modelos.sha256) para $1. Falha se algum hash nao confere.
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/../../.." && pwd)"
DEST="${1:?uso: buscar-modelos.sh <dir_destino>}"
mkdir -p "$DEST"
while read -r hash url; do
  arq="$DEST/$(basename "$url")"
  if [ ! -f "$arq" ] || [ "$(sha256sum "$arq" | cut -d' ' -f1)" != "$hash" ]; then
    curl -fsSL --retry 3 -o "$arq" "$url"
  fi
  echo "$hash  $arq" | sha256sum -c - >&2
done < <(grep -v '^#' "$RAIZ/corpus/modelos.sha256" | grep .)
echo "$DEST"
