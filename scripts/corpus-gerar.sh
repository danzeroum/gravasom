#!/usr/bin/env bash
# Gera o corpus sintetico + saida da cadeia + controles positivos em $1 (padrao: tests-out/corpus).
set -euo pipefail
D="$(cd "$(dirname "$0")/../core/dsp/src" && pwd)"
OUT="${1:-tests-out/corpus}"
BIN="$(mktemp -d)/corpus_tool"
g++ -std=c++17 -O2 -ffp-contract=off -Wall -Wextra "$D"/main/cpp/dsp_chain.cpp "$D"/hosttest/corpus_tool.cpp -lm -o "$BIN"
"$BIN" "$OUT" "${@:2}"
