#!/usr/bin/env bash
# Compila o nucleo MP3 + LAME no host com ASan/UBSan/LSan e roda os testes.
# Saida: 0 verde, !=0 vermelho (inclui vazamento detectado pelo LeakSanitizer).
set -euo pipefail
CPP="$(cd "$(dirname "$0")/../core/audio-android/src/main/cpp" && pwd)"
OUT="$(mktemp -d)"
LAME_SRC=$(ls "$CPP"/lame/libmp3lame/*.c)
# LAME (terceiros): UBSan sem a checagem "shift" (deslocamento de negativo em VbrTag.c, benigno); ASan/LSan completos.
gcc -c -O1 -g -fsanitize=address,undefined -fno-sanitize=shift -fno-omit-frame-pointer -DHAVE_CONFIG_H \
  -I"$CPP/lame" -I"$CPP/lame/include" -I"$CPP/lame/libmp3lame" $LAME_SRC -w 2>"$OUT/lame.err" || { cat "$OUT/lame.err"; exit 2; }
mv ./*.o "$OUT"/ 2>/dev/null || true
g++ -std=c++17 -O1 -g -fsanitize=address,undefined -fno-omit-frame-pointer -Wall -Wextra \
  -I"$CPP/lame/include" "$CPP/mp3_encoder.cpp" "$CPP/host_test/mp3_encoder_test.cpp" "$OUT"/*.o -lm -o "$OUT/mp3_encoder_test"
ASAN_OPTIONS=detect_leaks=1 UBSAN_OPTIONS=halt_on_error=1:print_stacktrace=1 "$OUT/mp3_encoder_test"
