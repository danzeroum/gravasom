#!/usr/bin/env bash
# Roda o audio-suite sobre um WAV e converte o exit code em ESTADO (ADR-009):
#   0 -> VERDE | 1 -> VERMELHO | 2,3,64 ou binario ausente -> INDECISO
# ESTUDIO_EXIGIR_AUDIO_SUITE=1 (CI): INDECISO vira VERMELHO.
# Saida do script: 0 VERDE, 1 VERMELHO, 3 INDECISO.
# Uso: audio-suite-gate.sh <perfil.yaml> <arquivo.wav> [referencia.wav]
set -u
PERFIL="${1:?perfil}"; WAV="${2:?wav}"; REF="${3:-}"
EXE="${AUDIO_SUITE:-audio-suite}"
estado() {
  echo "ESTADO=$1 ($2) wav=$(basename "$WAV")"
  case "$1" in
    VERDE) exit 0 ;;
    VERMELHO) exit 1 ;;
    *) if [ "${ESTUDIO_EXIGIR_AUDIO_SUITE:-0}" = "1" ]; then echo "ESTADO=VERMELHO (indeciso nao e aceito com ESTUDIO_EXIGIR_AUDIO_SUITE=1)"; exit 1; fi; exit 3 ;;
  esac
}
command -v "$EXE" >/dev/null 2>&1 || estado INDECISO "audio-suite ausente"
"$EXE" analyze "$WAV" --profile "$PERFIL" --format json --output /dev/null >/dev/null 2>"${TMPDIR:-/tmp}/as.err"
CODE=$?
case "$CODE" in
  0) ;;
  1) estado VERMELHO "analyze exit=1 (achado de nivel fail)" ;;
  2|3|64) estado INDECISO "analyze exit=$CODE: $(tail -c 120 "${TMPDIR:-/tmp}/as.err" | tr '\n' ' ')" ;;
  *) estado INDECISO "analyze exit=$CODE" ;;
esac
if [ -n "$REF" ]; then
  "$EXE" compare "$REF" "$WAV" --profile "$PERFIL" --fail-on-regression >/dev/null 2>&1
  CODE=$?
  case "$CODE" in
    0) ;;
    1) estado VERMELHO "compare exit=1 (regressao objetiva frente a referencia)" ;;
    *) estado INDECISO "compare exit=$CODE" ;;
  esac
fi
estado VERDE "analyze exit=0${REF:+, compare exit=0}"
