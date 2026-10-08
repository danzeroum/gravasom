#!/usr/bin/env bash
# Mede os 4 modelos NO CELULAR (adb): 10 min de fala pt-BR, tempo, bateria, temperatura, memoria e tamanho do APK.
# Pre-requisitos: celular com depuracao USB, FORA da tomada (o cabo USB carrega: use adb por wifi: `adb tcpip 5555` e `adb connect IP`),
# tela no brilho minimo, bateria > 50%, modo aviao. Roda um modelo por vez e espera o aparelho esfriar (<= 33 C) entre eles.
# Saida: spike/whisper/resultados/<modelo>.json e tamanhos.txt
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/../../.." && pwd)"
SPIKE="$RAIZ/spike/whisper"
MODELOS="${MODELOS:-$HOME/.cache/gravasom-corpus/modelos}"
SAIDA="$SPIKE/resultados"
mkdir -p "$SAIDA"
"$SPIKE/scripts/buscar-whisper.sh" "$SPIKE/third_party/whisper.cpp" >/dev/null
"$SPIKE/scripts/buscar-modelos.sh" "$MODELOS" >/dev/null
"$SPIKE/scripts/gerar-clip.sh" "$SPIKE/src/androidTest/assets/clip10min.wav"
: > "$SAIDA/tamanhos.txt"
for m in ggml-tiny-q5_1 ggml-tiny ggml-base-q5_1 ggml-base; do
  echo "== $m"
  (cd "$RAIZ" && ./gradlew -Pspike :spike:whisper:assembleRelease :spike:whisper:assembleDebugAndroidTest "-PmodeloWhisper=$MODELOS/$m.bin" --no-daemon -q)
  echo "$m $(stat -c %s "$SPIKE/build/outputs/apk/release/whisper-release.apk") bytes (APK release, modelo dentro)" >> "$SAIDA/tamanhos.txt"
  (cd "$RAIZ" && ./gradlew -Pspike :spike:whisper:connectedDebugAndroidTest "-PmodeloWhisper=$MODELOS/$m.bin" \
      "-Pandroid.testInstrumentationRunnerArguments.class=com.gravasom.spike.whisper.MedicaoTranscricaoTest" --no-daemon -q)
  adb exec-out run-as com.gravasom.spike.whisper cat files/tests-out/spike_ultimo.json > "$SAIDA/$m.json" 2>/dev/null \
    || adb pull /sdcard/Android/data/com.gravasom.spike.whisper/files/tests-out/spike_ultimo.json "$SAIDA/$m.json"
  echo "   esfriando..."
  until [ "$(adb shell dumpsys battery | awk '/temperature/ {print $2}')" -le 330 ]; do sleep 20; done
done
python3 -I "$RAIZ/scripts/asr_wer.py" "$SAIDA" "$RAIZ/corpus/falas_pt_asr.txt" || true
echo "Resultados em $SAIDA"
