#!/usr/bin/env python3
"""Curva de intensidade do redutor (ADR-015): para um motor ja processado a 100% (saida alinhada ao corpus), mistura seco/molhado em
k = intensidade e mede (a) a TRANSPARENCIA na fala limpa (STOI limpa->misturada, teto 0,9909) e (b) o SI-SDRi nas condicoes com
ruido estacionario (ventilador, zumbido, rosa) e no murmurio. Uso: onda2_ruido_intensidade.py <dir_corpus> <dir_saida_motor> <saida.json>"""
import json
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from medidor_ref import alinhar, ler, si_sdr  # noqa: E402
from pystoi import stoi as _stoi  # noqa: E402

KS = (0.0, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 1.0)
ESTACIONARIOS = ("ventilador", "zumbido", "rosa")


def main(corpus, motor, saida):
    corpus, motor = Path(corpus), Path(motor)
    nomes = sorted(p.name for p in corpus.glob("*.wav") if not p.name.endswith("_ref.wav"))
    acc = {k: {"limpa": [], "estac": [], "murm": []} for k in KS}
    for nome in nomes:
        partes = nome[:-4].split("_")
        taxa, ref = ler(corpus / f"{partes[0]}_ref.wav")
        _, seco = ler(corpus / nome)
        _, molhado = ler(motor / nome)
        n = min(len(seco), len(molhado))
        seco, molhado = seco[:n], molhado[:n]
        base = None
        n = min(n, len(ref))
        ref, seco, molhado = ref[:n], seco[:n], molhado[:n]
        for k in KS:
            y = seco + k * (molhado - seco)
            r, o = ref, y  # a saida dos motores ja vem alinhada a entrada (latencia compensada)
            if partes[1] == "limpo":
                acc[k]["limpa"].append(float(_stoi(r, o, taxa, extended=False)))
            else:
                if base is None:
                    base = si_sdr(ref, seco)
                grupo = "murm" if partes[1] == "murmurio" else "estac"
                acc[k][grupo].append(si_sdr(r, o) - base)
    res = {}
    print(f"{'k':>4} | STOI limpa medio (pior) | SI-SDRi estacionario | SI-SDRi murmurio")
    for k in KS:
        a = acc[k]
        res[str(k)] = {"stoi_limpa_medio": float(np.mean(a["limpa"])), "stoi_limpa_pior": float(np.min(a["limpa"])),
                       "si_sdri_estacionario": float(np.mean(a["estac"])), "si_sdri_murmurio": float(np.mean(a["murm"]))}
        r = res[str(k)]
        print(f"{k:4.1f} | {r['stoi_limpa_medio']:.4f} ({r['stoi_limpa_pior']:.4f})      | {r['si_sdri_estacionario']:+.2f} dB             | {r['si_sdri_murmurio']:+.2f} dB")
    Path(saida).write_text(json.dumps(res, indent=1))


if __name__ == "__main__":
    main(*sys.argv[1:4])
