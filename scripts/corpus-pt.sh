#!/usr/bin/env bash
# Fala em portugues (FLEURS pt_br, CC BY 4.0) baixada POR HASH (cache em $CORPUS_CACHE). Extrai as locucoes de
# corpus/falas_pt_{vad,asr}.txt e converte para WAV mono 16 bits 16 kHz em $1/<arquivo>.wav; copia as transcricoes para $1/*.tsv.
# Uso: corpus-pt.sh <dir_saida> [vad|asr|tudo]
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:?uso: corpus-pt.sh <dir_saida> [vad|asr|tudo]}"
QUAIS="${2:-tudo}"
CACHE="${CORPUS_CACHE:-$HOME/.cache/gravasom-corpus}"
mkdir -p "$OUT" "$CACHE"

baixar() {  # baixar <sufixo-da-url> <arquivo-local>
  local hash url
  read -r hash url < <(grep -v '^#' "$RAIZ/corpus/corpus.sha256" | grep -F "$1" | head -1 | awk '{print $1, $2}')
  if [ ! -f "$2" ] || [ "$(sha256sum "$2" | cut -d' ' -f1)" != "$hash" ]; then
    curl -fsSL --retry 3 -o "$2" "$url"
  fi
  echo "$hash  $2" | sha256sum -c - >&2   # falha se o hash nao confere
}

declare -a LISTAS=()
case "$QUAIS" in
  vad) LISTAS=(falas_pt_vad.txt) ;;
  asr) LISTAS=(falas_pt_asr.txt) ;;
  *) LISTAS=(falas_pt_vad.txt falas_pt_asr.txt) ;;
esac

for divisao in dev train; do
  arquivos=$(cat "${LISTAS[@]/#/$RAIZ/corpus/}" | grep -v '^#' | awk -F'\t' -v d="$divisao" '$1==d {print "dev/" $2}' | sed "s#^dev/#$divisao/#")
  [ -n "$arquivos" ] || continue
  TAR="$CACHE/fleurs-pt_br-$divisao.tar.gz"
  baixar "pt_br/audio/$divisao.tar.gz" "$TAR"
  baixar "pt_br/$divisao.tsv" "$CACHE/fleurs-pt_br-$divisao.tsv"
  cp "$CACHE/fleurs-pt_br-$divisao.tsv" "$OUT/fleurs-$divisao.tsv"
  TMP="$(mktemp -d)"
  # shellcheck disable=SC2086
  tar xzf "$TAR" -C "$TMP" $arquivos
  for f in $arquivos; do
    ffmpeg -v error -y -i "$TMP/$f" -ac 1 -ar 16000 -sample_fmt s16 "$OUT/$(basename "$f")"
  done
  rm -rf "$TMP"
done
echo "fala pt-BR em $OUT: $(ls "$OUT"/*.wav | wc -l) arquivos" >&2
