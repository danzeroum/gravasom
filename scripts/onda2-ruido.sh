#!/usr/bin/env bash
# Harness do redutor de ruido (Onda 2): fala pt-BR (FLEURS) + ruido (ventilador, zumbido, rosa, murmurio) em SNR 0/5/10/15 dB a 48, 44,1 e
# 16 kHz -> redutor NATIVO do app (C++ via JNI no host, intensidade padrao 0,4 e 1,0) -> medicao independente (SI-SDRi e STOI contra a fala
# limpa). Falha se, na intensidade padrao, alguma fala LIMPA ficar abaixo do teto de STOI (0,9909) ou o SI-SDRi do ruido estacionario nao
# melhorar. Uso: scripts/onda2-ruido.sh [venv_python] [n_locucoes]
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
PY="${1:-python3}"
N="${2:-4}"
BASE="$RAIZ/tests-out/onda2/ruido"
rm -rf "$BASE"; mkdir -p "$BASE"
"$RAIZ/scripts/corpus-pt.sh" "$RAIZ/tests-out/pt" vad
"$PY" -I "$RAIZ/scripts/onda2_ruido_corpus.py" "$RAIZ/tests-out/pt" "$BASE/corpus-48k" "$N"
for taxa in 44100 16000; do
  mkdir -p "$BASE/corpus-$taxa"
  for f in "$BASE"/corpus-48k/*.wav; do
    ffmpeg -v error -y -i "$f" -ar "$taxa" -ac 1 -sample_fmt s16 "$BASE/corpus-$taxa/$(basename "$f")"
  done
done
LIB="$("$RAIZ/scripts/build-dsp-host.sh" "$BASE/dsp-host" | tail -1)"
export GRAVASOM_DSP_HOST_LIB="$LIB"
for k in 0.4 1.0; do
  for taxa in 48000 44100 16000; do
    sufixo=$([ "$taxa" = 48000 ] && echo 48k || echo "$taxa")
    export GRAVASOM_ONDA2_RUIDO_IN="$BASE/corpus-$sufixo" GRAVASOM_ONDA2_RUIDO_OUT="$BASE/saida-k$k-$sufixo" GRAVASOM_ONDA2_INTENSIDADE="$k"
    (cd "$RAIZ" && ./gradlew --no-daemon -q :core:dsp:testDebugUnitTest --tests '*NoiseHarnessTest*' --rerun-tasks)
  done
done
# um medidor por taxa (a referencia tem de estar na mesma taxa)
for s in 48k 44100 16000; do
  pares=(); for k in 0.4 1.0; do pares+=("k${k}=$BASE/saida-k$k-$s"); done
  "$PY" -I "$RAIZ/scripts/onda2_ruido_medir.py" "$BASE/corpus-$s" "$BASE/medido-$s.json" "${pares[@]}" | tee "$BASE/resumo-$s.txt"
done
"$PY" -I - "$BASE" <<'PY'
import json, sys
base = sys.argv[1]
falhas = []
for s in ("48k", "44100", "16000"):
    r = json.load(open(f"{base}/medido-{s}.json"))["k0.4"]
    est = [v["si_sdri_db"] for g, v in r["por_condicao"].items() if g.split("_")[0] in ("ventilador", "zumbido", "rosa")]
    if sum(est) / len(est) <= 0: falhas.append(f"{s}: SI-SDRi estacionario medio {sum(est)/len(est):+.2f} dB (nao melhorou)")
    if not r["transparente_na_fala_limpa"]: falhas.append(f"{s}: fala limpa com STOI {r['limpa_stoi_pior']:.4f} < 0,9909 na intensidade padrao")
    if abs(r["atraso_amostras_mediano"]) > 1: falhas.append(f"{s}: saida desalinhada ({r['atraso_amostras_mediano']} amostras)")
print("\nFALHOU:\n" + "\n".join(falhas) if falhas else "\nOK: intensidade padrao (40%) melhora o SI-SDRi do ruido estacionario, a fala limpa fica >= 0,9909 e a saida esta alinhada.")
sys.exit(1 if falhas else 0)
PY
