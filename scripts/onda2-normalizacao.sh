#!/usr/bin/env bash
# Harness de "Melhorar volume" (Onda 2): fala pt-BR (FLEURS, por hash) a -30 LUFS em 16/44,1/48 kHz -> normalizacao do app (Kotlin)
# -> medicao independente (pyloudnorm + ffmpeg ebur128 + pico verdadeiro 8x). Exit 1 se sair de +-1 LU do alvo ou passar de -1 dBTP.
# Uso: scripts/onda2-normalizacao.sh [venv_python]   (venv com scripts/requirements-medidas.txt; padrao: python3)
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
PY="${1:-python3}"
BASE="$RAIZ/tests-out/onda2/normalizacao"
rm -rf "$BASE"; mkdir -p "$BASE"
"$RAIZ/scripts/corpus-pt.sh" "$RAIZ/tests-out/pt" vad
"$PY" -I "$RAIZ/scripts/onda2_preparar.py" "$RAIZ/tests-out/pt" "$BASE/in" -30
export GRAVASOM_ONDA2_IN="$BASE/in" GRAVASOM_ONDA2_OUT="$BASE/out"
(cd "$RAIZ" && ./gradlew --no-daemon -q :core:audio-engine:test --tests '*NormalizeHarnessTest*' --rerun-tasks)
"$PY" -I "$RAIZ/scripts/onda2_medir.py" normalizacao "$BASE/in" "$BASE/out" -16 | tee "$BASE/resumo.txt"
