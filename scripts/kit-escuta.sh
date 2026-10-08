#!/usr/bin/env bash
# Kit de escuta A/B CEGO para as falas reais (sem referencia, validacao subjetiva).
# Uso: kit-escuta.sh <corpus> <sufixo_processado (ex.: hp80)> <dir_saida> [semente]
# Gera, por base real: <id>_A.wav e <id>_B.wav (um e o original, outro o processado, ordem sorteada),
# formulario.csv (para o ouvinte preencher) e gabarito.csv (NAO mostrar ao ouvinte).
set -euo pipefail
C="${1:?corpus}"; SUF="${2:?sufixo}"; OUT="${3:?saida}"; SEMENTE="${4:-1}"
mkdir -p "$OUT"
RANDOM=$SEMENTE
echo "id,preferencia_A_ou_B_ou_igual,qual_tem_menos_ruido,qual_soa_mais_natural,comentario" > "$OUT/formulario.csv"
echo "id,A_e" > "$OUT/gabarito.csv"
for base in $(grep '^real_fala_' "$C/bases_sinteticas.txt"); do
  id="${base#real_fala_}"
  if [ $((RANDOM % 2)) -eq 0 ]; then a="$C/$base.wav"; b="$C/${base}__$SUF.wav"; ae=original
  else a="$C/${base}__$SUF.wav"; b="$C/$base.wav"; ae=processado; fi
  cp "$a" "$OUT/${id}_A.wav"; cp "$b" "$OUT/${id}_B.wav"
  echo "$id,$ae" >> "$OUT/gabarito.csv"
  echo "$id,,,," >> "$OUT/formulario.csv"
done
cat > "$OUT/LEIA-ME.md" <<'EOT'
# Kit de escuta A/B (cego)

1. Ouça cada par `N_A.wav` / `N_B.wav` em fones, no mesmo volume, quantas vezes quiser.
2. Preencha `formulario.csv`: preferência (A, B ou igual), qual tem menos ruído, qual soa mais natural, comentário.
3. **Não abra `gabarito.csv`** antes de terminar (ele diz qual lado é o original).
4. Mínimo recomendado: 5 ouvintes. Resultado é validação subjetiva; não substitui os gates objetivos.
EOT
echo "kit em $OUT ($(grep -c . "$OUT/gabarito.csv") linhas de gabarito)"
