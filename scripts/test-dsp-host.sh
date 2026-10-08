#!/usr/bin/env bash
# Compila o nucleo DSP no host com ASan/UBSan e roda os testes. 0 verde, !=0 vermelho.
set -euo pipefail
D="$(cd "$(dirname "$0")/../core/dsp/src" && pwd)"
OUT="$(mktemp -d)"
g++ -std=c++17 -O1 -g -ffp-contract=off -fsanitize=address,undefined -fno-omit-frame-pointer -Wall -Wextra \
  "$D"/main/cpp/dsp_chain.cpp "$D"/hosttest/dsp_chain_test.cpp -lm -o "$OUT/dsp_chain_test"
ASAN_OPTIONS=detect_leaks=1 UBSAN_OPTIONS=halt_on_error=1:print_stacktrace=1 "$OUT/dsp_chain_test"
