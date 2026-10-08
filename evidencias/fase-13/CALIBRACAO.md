# Calibracao dos tetos (gerado por scripts/calibrar_tetos.py)

Teto = media geometrica entre o pior caso BOM e o controle positivo (identidade exata quando o bom mede 0).
Bases: fala_sintetica, real_fala_1, real_fala_2, real_fala_3, real_fala_4, real_fala_5, real_fala_6
Bons usados (14): hp80 e bypass em cada base. Controle: o mais fraco entre as bases.

| gate | pior bom | controle positivo | valor no controle (mais fraco) | teto | discrimina | estado |
|---|---|---|---|---|---|---|
| duracao_amostras | 0 | ctrl_corte50ms | 2400 | 0 | sim | ativo |
| f0_cents | 20.98 | ctrl_pitch_mais50c | 42.16 | 29.74 | sim | ativo |
| glitch_novos | 2 | ctrl_cliques | 39 | 8.832 | sim | ativo |
| stoi_queda | 0 | ctrl_ruido_reinjetado | -0.0452 | 0 | NAO | BLOQUEADO (TETO_NAO_DISCRIMINA): so observacao |
| clipping_ou_true_peak | 0 | ctrl_ganho3db_clip | 1 | 0 | sim | ativo |
