#!/usr/bin/env bash
# Compila libgravasom_dsp.so para o HOST (x86-64 Linux) com JNI, para rodar o codigo Kotlin+JNI do VAD em testes JVM
# (harness do ADR-014). Uso: build-dsp-host.sh <dir_saida>. Imprime o caminho do diretorio com a lib.
set -euo pipefail
D="$(cd "$(dirname "$0")/../core/dsp/src/main/cpp" && pwd)"
OUT="${1:?uso: build-dsp-host.sh <dir_saida>}"
JH="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac || command -v java)")")")}"
mkdir -p "$OUT/obj"
for f in "$D"/webrtc_vad/webrtc/common_audio/signal_processing/*.c "$D"/webrtc_vad/webrtc/common_audio/vad/*.c; do
  gcc -O2 -w -fPIC -DWEBRTC_POSIX -I"$D/webrtc_vad" -c "$f" -o "$OUT/obj/$(basename "$f").o"
done
g++ -std=c++17 -O2 -ffp-contract=off -fPIC -shared -Wall -Wextra -I"$D/webrtc_vad" -I"$JH/include" -I"$JH/include/linux" \
  "$D"/dsp_chain.cpp "$D"/dsp_jni.cpp "$D"/vad_stream.cpp "$D"/vad_jni.cpp "$OUT"/obj/*.o -o "$OUT/libgravasom_dsp.so"
echo "$OUT"
