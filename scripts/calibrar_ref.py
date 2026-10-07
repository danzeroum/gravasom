#!/usr/bin/env python3
"""Calibra e verifica os gates COM REFERENCIA (venv do medidor). Mesma regra do ADR-009:
teto entre o pior BOM e o melhor CONTROLE; sem separacao suficiente -> TETO_NAO_DISCRIMINA (exit 3).

  calibrar_ref.py calibrar  <corpus> <tetos_ref.json> <CALIBRACAO_REF.md>
  calibrar_ref.py verificar <corpus> <tetos_ref.json>
Gates (valor MAIOR = melhor):
  stoi_limpa   STOI(limpa, saida) nas bases de fala limpa. Bons: hp80 e bypass. Controles: ruido reinjetado e
               supressao agressiva (os DOIS tem de ficar abaixo do teto).
  si_sdri      melhora de SI-SDR sobre misturas com referencia exata. SEM BONS ate a Fase 14 (nao ha redutor):
               fica AGUARDANDO_BONS; so registra os controles. Nenhum teto e inventado.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import medidor_ref as M  # noqa: E402

# Criterio sem numero magico: gap >= FATOR x max(dispersao dos bons, resolucao do medidor).
FATOR_GAP = 5.0
RESOLUCAO = {"stoi_limpa": 0.001}
CONTROLES_STOI = ["ctrl_ruido_reinjetado", "ctrl_supressao_agressiva"]


def bases(pasta):
    return [l.strip() for l in open(os.path.join(pasta, "bases_sinteticas.txt")) if l.strip()]


def medir_stoi(pasta):
    bons, ctrl, lags = {}, {}, {}
    for b in bases(pasta):
        ref = os.path.join(pasta, b + ".wav")
        for nome, destino in (("hp80", bons), ("bypass", bons)):
            out = ref if nome == "bypass" else os.path.join(pasta, f"{b}__{nome}.wav")
            r = M.medir(ref, out)
            destino[f"{b}:{nome}"] = r["stoi"]
            lags[f"{b}:{nome}"] = r["atraso_amostras"]
        for c in CONTROLES_STOI:
            ctrl[f"{b}:{c}"] = M.medir(ref, os.path.join(pasta, f"{b}__{c}.wav"))["stoi"]
    return bons, ctrl, lags


def calibrar(pasta, tetos_json, md):
    bons, ctrl, lags = medir_stoi(pasta)
    pior_bom, melhor_ctrl = min(bons.values()), max(ctrl.values())
    gap = pior_bom - melhor_ctrl
    dispersao = max(bons.values()) - pior_bom
    gap_exigido = FATOR_GAP * max(dispersao, RESOLUCAO["stoi_limpa"])
    discrimina = gap >= gap_exigido
    teto = (pior_bom + melhor_ctrl) / 2
    json.dump({"versao": 1, "stoi_limpa": {"teto_min": teto, "pior_bom": pior_bom, "melhor_controle": melhor_ctrl,
                                          "gap": gap, "gap_exigido": gap_exigido, "dispersao_bons": dispersao, "discrimina": discrimina},
               "si_sdri": {"estado": "AGUARDANDO_BONS (Fase 14)"}}, open(tetos_json, "w"), indent=1, sort_keys=True)
    with open(md, "w") as f:
        f.write("# Calibracao dos gates COM REFERENCIA (gerado por scripts/calibrar_ref.py)\n\n")
        f.write("Medidor: scripts/medidor_ref.py (numpy/scipy/pystoi MIT, venv separado do audio-suite). "
                "Alinhamento: correlacao FFT + refino por SI-SDR; atraso medido registrado.\n\n")
        f.write(f"## stoi_limpa (maior = melhor)\n\n- pior bom: **{pior_bom:.4f}**; melhor controle: **{melhor_ctrl:.4f}**; "
                f"gap {gap:.4f} (exigido {gap_exigido:.4f} = {FATOR_GAP:g} x max(dispersao dos bons {dispersao:.4f}, resolucao {RESOLUCAO['stoi_limpa']})); teto (ponto medio): **{teto:.4f}**; "
                f"discrimina: **{'sim' if discrimina else 'NAO (TETO_NAO_DISCRIMINA)'}**\n\n")
        f.write("| par | STOI |\n|---|---|\n")
        for k, v in sorted({**{f'BOM {k}': v for k, v in bons.items()}, **{f'CONTROLE {k}': v for k, v in ctrl.items()}}.items()):
            f.write(f"| {k} | {v:.4f} |\n")
        f.write(f"\nAtrasos medidos nos bons (amostras): min {min(lags.values())}, max {max(lags.values())} "
                "(passa-altas causal: fase, nao atraso declarado).\n\n")
        f.write("## si_sdri\n\nAGUARDANDO_BONS: nao ha redutor de ruido ainda (Fase 14). Os controles "
                "(ruido reinjetado, supressao agressiva) ja existem no corpus; o teto sera calibrado com a saida do redutor.\n")
    print(open(md).read())
    return 0 if discrimina else 3


def verificar(pasta, tetos_json):
    t = json.load(open(tetos_json))["stoi_limpa"]
    bons, ctrl, _ = medir_stoi(pasta)
    rc = 0
    rep = {k: v for k, v in bons.items() if v < t["teto_min"]}
    print(f"BOM      stoi_limpa pior={min(bons.values()):.4f} teto_min={t['teto_min']:.4f} {'ok' if not rep else 'REPROVOU ' + str(rep)}")
    rc = 1 if rep else 0
    nao = {k: v for k, v in ctrl.items() if v >= t["teto_min"]}
    print(f"CONTROLE stoi_limpa melhor={max(ctrl.values()):.4f} < {t['teto_min']:.4f} {'pegou' if not nao else 'NAO PEGOU ' + str(nao)}")
    if nao:
        print("TETO_NAO_DISCRIMINA")
        rc = 3
    return rc


if __name__ == "__main__":
    try:
        sys.exit(calibrar(*sys.argv[2:5]) if sys.argv[1] == "calibrar" else verificar(*sys.argv[2:4]))
    except FileNotFoundError as e:
        print("INDECISO (arquivo ausente):", e)
        sys.exit(2)
