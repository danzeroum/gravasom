# Calibracao dos gates COM REFERENCIA (gerado por scripts/calibrar_ref.py)

Medidor: scripts/medidor_ref.py (numpy/scipy/pystoi MIT, venv separado do audio-suite). Alinhamento: correlacao FFT + refino por SI-SDR; atraso medido registrado.

## stoi_limpa (maior = melhor)

- pior bom: **0.9996**; melhor controle: **0.9823**; gap 0.0173 (exigido 0.0050 = 5 x max(dispersao dos bons 0.0004, resolucao 0.001)); teto (ponto medio): **0.9909**; discrimina: **sim**

| par | STOI |
|---|---|
| BOM fala_sintetica:bypass | 1.0000 |
| BOM fala_sintetica:hp80 | 0.9997 |
| BOM real_fala_1:bypass | 1.0000 |
| BOM real_fala_1:hp80 | 0.9997 |
| BOM real_fala_2:bypass | 1.0000 |
| BOM real_fala_2:hp80 | 0.9998 |
| BOM real_fala_3:bypass | 1.0000 |
| BOM real_fala_3:hp80 | 0.9998 |
| BOM real_fala_4:bypass | 1.0000 |
| BOM real_fala_4:hp80 | 0.9996 |
| BOM real_fala_5:bypass | 1.0000 |
| BOM real_fala_5:hp80 | 0.9997 |
| BOM real_fala_6:bypass | 1.0000 |
| BOM real_fala_6:hp80 | 0.9997 |
| CONTROLE fala_sintetica:ctrl_ruido_reinjetado | 0.7236 |
| CONTROLE fala_sintetica:ctrl_supressao_agressiva | 0.8679 |
| CONTROLE real_fala_1:ctrl_ruido_reinjetado | 0.9292 |
| CONTROLE real_fala_1:ctrl_supressao_agressiva | 0.8566 |
| CONTROLE real_fala_2:ctrl_ruido_reinjetado | 0.9823 |
| CONTROLE real_fala_2:ctrl_supressao_agressiva | 0.9309 |
| CONTROLE real_fala_3:ctrl_ruido_reinjetado | 0.9404 |
| CONTROLE real_fala_3:ctrl_supressao_agressiva | 0.8432 |
| CONTROLE real_fala_4:ctrl_ruido_reinjetado | 0.9552 |
| CONTROLE real_fala_4:ctrl_supressao_agressiva | 0.8330 |
| CONTROLE real_fala_5:ctrl_ruido_reinjetado | 0.9527 |
| CONTROLE real_fala_5:ctrl_supressao_agressiva | 0.8641 |
| CONTROLE real_fala_6:ctrl_ruido_reinjetado | 0.9763 |
| CONTROLE real_fala_6:ctrl_supressao_agressiva | 0.9313 |

Atrasos medidos nos bons (amostras): min -8, max 0 (passa-altas causal: fase, nao atraso declarado).

## Segunda medida: ref_quality do audio-suite (informativa; nao substitui a primeira)

| medida | pior bom (hp80) | melhor controle | separa? |
|---|---|---|---|
