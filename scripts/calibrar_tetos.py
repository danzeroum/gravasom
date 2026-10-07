#!/usr/bin/env python3
"""Calibra os tetos dos gates a partir do corpus e verifica que cada um DISCRIMINA.

  calibrar_tetos.py calibrar  <perfil> <pasta_corpus> <saida_tetos.json> <saida_calibracao.md>
  calibrar_tetos.py verificar <perfil> <pasta_corpus> <tetos.json>

Regra: teto = media geometrica entre o pior caso BOM (com piso na resolucao da medida) e o controle positivo.
Se algum controle nao fica claramente acima do pior bom (fator >= 2) -> TETO_NAO_DISCRIMINA (exit 3).
Exit: 0 ok, 1 gate reprovou na verificacao, 2 ferramenta ausente/entrada invalida (indeciso), 3 TETO_NAO_DISCRIMINA.
"""
import json
import math
import sys

import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gate_lib as G  # noqa: E402

FATOR_MINIMO = 2.0


def coletar(perfil, pasta):
    nomes = [n for par in G.pares_bons(pasta) for n in par]
    for g in G.GATES:
        nomes += [n for par in G.par_controle(pasta, g) for n in par]
    return G.medir_corpus(perfil, pasta, nomes)


def valores(m, pasta):
    """bons: lista de {gate: valor}; ctrl: {gate: [valor por base]} (o PIOR controle e o de menor valor)."""
    bons = [G.grandezas(m[r], m[o]) for r, o in G.pares_bons(pasta)]
    ctrl = {g: [G.grandezas(m[r], m[o])[g] for r, o in G.par_controle(pasta, g)] for g in G.GATES}
    return bons, ctrl


def calibrar(perfil, pasta, tetos_json, md):
    m = coletar(perfil, pasta)
    bons, ctrl = valores(m, pasta)
    tetos, linhas, ruim = {}, [], []
    for g, spec in G.GATES.items():
        pior_bom = max(b[g] for b in bons)
        ref = max(pior_bom, spec["resolucao"])
        c = min(ctrl[g])  # o controle MAIS FRACO tem de passar do teto
        ativo = spec.get("ativo", True)
        if not ativo:
            c = min(c, 0.0) if c < 0 else c
        discrimina = c >= FATOR_MINIMO * ref and c > pior_bom
        teto = math.sqrt(ref * c) if ref > 0 and c > 0 else max(c, 0.0) / FATOR_MINIMO
        if g in ("duracao_amostras", "clipping_ou_true_peak"):
            teto = 0.0  # identidade exata: o bom mede 0 e o controle > 0
            discrimina = pior_bom == 0.0 and c > 0.0
        tetos[g] = {"teto": teto, "pior_bom": pior_bom, "controle": spec["controle"], "valor_controle": c,
                    "discrimina": discrimina, "ativo": ativo, "n_bons": len(bons)}
        linhas.append((g, pior_bom, spec["controle"], c, teto, discrimina, ativo))
        if not discrimina and ativo:
            ruim.append(g)
    json.dump({"versao": 1, "tetos": tetos}, open(tetos_json, "w"), indent=1, sort_keys=True)
    with open(md, "w") as f:
        f.write("# Calibracao dos tetos (gerado por scripts/calibrar_tetos.py)\n\n")
        f.write("Teto = media geometrica entre o pior caso BOM e o controle positivo (identidade exata quando o bom mede 0).\n")
        f.write(f"Bases: {', '.join(G.bases(pasta))}\nBons usados ({len(bons)}): hp80 e bypass em cada base. Controle: o mais fraco entre as bases.\n\n")
        f.write("| gate | pior bom | controle positivo | valor no controle (mais fraco) | teto | discrimina | estado |\n|---|---|---|---|---|---|---|\n")
        for g, pb, ct, cv, t, d, a in linhas:
            estado = "ativo" if a else "BLOQUEADO (TETO_NAO_DISCRIMINA): so observacao"
            f.write(f"| {g} | {pb:.4g} | {ct} | {cv:.4g} | {t:.4g} | {'sim' if d else 'NAO'} | {estado} |\n")
    print(open(md).read())
    if ruim:
        print("TETO_NAO_DISCRIMINA:", ruim)
        return 3
    return 0


def verificar(perfil, pasta, tetos_json):
    tetos = json.load(open(tetos_json))["tetos"]
    m = coletar(perfil, pasta)
    bons, ctrl = valores(m, pasta)
    pares = G.pares_bons(pasta)
    rc = 0
    for g, t in tetos.items():
        if not t.get("ativo", True):
            print(f"INATIVO  {g:24s} (TETO_NAO_DISCRIMINA registrado em CALIBRACAO.md; so observacao)")
            continue
        reprovados = [(r, o, b[g]) for (r, o), b in zip(pares, bons) if b[g] > t["teto"]]
        print(f"BOM      {g:24s} pior={max(b[g] for b in bons):.4g} teto={t['teto']:.4g}  "
              f"{'ok' if not reprovados else 'REPROVOU: ' + str(reprovados)}")
        if reprovados:
            rc = rc or 1
        pior_controle = min(ctrl[g])
        pegou = pior_controle > t["teto"]
        print(f"CONTROLE {g:24s} {t['controle']}: pior={pior_controle:.4g} > {t['teto']:.4g}  {'pegou' if pegou else 'NAO PEGOU'}")
        if not pegou:
            rc = 3  # TETO_NAO_DISCRIMINA
    if rc == 3:
        print("TETO_NAO_DISCRIMINA")
    return rc


if __name__ == "__main__":
    try:
        if sys.argv[1] == "calibrar":
            sys.exit(calibrar(*sys.argv[2:6]))
        sys.exit(verificar(*sys.argv[2:5]))
    except FileNotFoundError as e:
        print("INDECISO (ferramenta/arquivo ausente):", e)
        sys.exit(2)
