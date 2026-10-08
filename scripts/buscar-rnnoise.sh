#!/usr/bin/env bash
# Baixa o RNNoise v0.2 (xiph, BSD-3-Clause) POR HASH (corpus/rnnoise.sha256) e monta a arvore de fontes em <dir>/{include,src}.
# O modelo (rnnoise_data.c, ~29 MB de texto) NAO vai para o git: vem do tarball de media.xiph.org, conferido por hash.
# Uso: buscar-rnnoise.sh <dir_destino> [completo|leve]   (leve = modelo "little" do v0.2: pesos mais esparsos)
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
DEST="${1:?uso: buscar-rnnoise.sh <dir_destino> [completo|leve]}"
VARIANTE="${2:-completo}"
CACHE="${CORPUS_CACHE:-$HOME/.cache/gravasom-corpus}/rnnoise-v0.2"
mkdir -p "$CACHE" "$DEST"
SELO="$DEST/.montado-$VARIANTE"
if [ -f "$SELO" ] && [ "$(cat "$SELO")" = "$(sha256sum "$RAIZ/corpus/rnnoise.sha256" | cut -d' ' -f1)" ]; then exit 0; fi
while read -r hash url rel; do
  case "$hash" in \#*|"") continue ;; esac
  alvo="$CACHE/$rel"
  mkdir -p "$(dirname "$alvo")"
  if [ ! -f "$alvo" ] || [ "$(sha256sum "$alvo" | cut -d' ' -f1)" != "$hash" ]; then
    curl -fsSL --retry 3 -o "$alvo" "$url"
  fi
  echo "$hash  $alvo" | sha256sum -c - >/dev/null
done < "$RAIZ/corpus/rnnoise.sha256"
rm -rf "$DEST/src" "$DEST/include"
mkdir -p "$DEST/src" "$DEST/include"
(cd "$CACHE" && cp COPYING "$DEST/" && cp include/rnnoise.h "$DEST/include/" && cp -r src/. "$DEST/src/")
tar xzf "$CACHE/rnnoise_data.tar.gz" -C "$DEST" src/rnnoise_data.c src/rnnoise_data.h src/rnnoise_data_little.c src/rnnoise_data_little.h
if [ "$VARIANTE" = "leve" ]; then
  mv "$DEST/src/rnnoise_data_little.c" "$DEST/src/rnnoise_data.c"
  mv "$DEST/src/rnnoise_data_little.h" "$DEST/src/rnnoise_data.h"
else
  rm -f "$DEST/src/rnnoise_data_little.c" "$DEST/src/rnnoise_data_little.h"
fi
sha256sum "$RAIZ/corpus/rnnoise.sha256" | cut -d' ' -f1 > "$SELO"
