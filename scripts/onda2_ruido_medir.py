#!/usr/bin/env python3
"""Medidor do redutor de ruido (ADR-015), INDEPENDENTE do codigo do app: SI-SDR/SI-SDRi e STOI contra a fala limpa de referencia, com
o alinhamento por correlacao de scripts/medidor_ref.py (que registra o atraso medido). Roda no venv de scripts/requirements-medidas.txt.

Uso: onda2_ruido_medir.py <dir_corpus> <saida.json> <motor>=<dir_saida_do_motor> [...]
Para cada motor: por ruido x SNR, SI-SDRi medio e variacao de STOI; e a TRANSPARENCIA na fala limpa (STOI limpa->processada,
que tem de ficar >= 0,9909, o teto do harness: perfis/tetos_ref.json).
"""
import json
import sys
from collections import defaultdict
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from medidor_ref import alinhar, ler, si_sdr  # noqa: E402
from pystoi import stoi as _stoi  # noqa: E402

TETO_STOI_LIMPA = 0.9909345196910356


def stoi(ref, est, taxa):
    return float(_stoi(ref, est, taxa, extended=False))


def medir_arquivo(corpus, nome, saida_motor, cache):
    base, _, resto = nome.partition("_")
    # nome = <locucao>_<ruido|limpo>[_snr<dB>].wav
    locucao = nome.split("_")[0]
    ref_taxa, ref = ler(corpus / f"{locucao}_ref.wav")
    chave = nome
    if chave not in cache:
        _, entrada = ler(corpus / nome)
        r, e, _ = alinhar(ref, entrada, ref_taxa)
        cache[chave] = (si_sdr(r, e), stoi(r, e, ref_taxa))
    sdr_in, stoi_in = cache[chave]
    _, out = ler(saida_motor / nome)
    r, o, lag = alinhar(ref, out, ref_taxa)
    return {"si_sdr_in": sdr_in, "si_sdr_out": si_sdr(r, o), "stoi_in": stoi_in, "stoi_out": stoi(r, o, ref_taxa), "lag": lag}


def main(corpus, saida, motores):
    corpus = Path(corpus)
    cache = {}
    resultado = {}
    nomes = sorted(p.name for p in corpus.glob("*.wav") if not p.name.endswith("_ref.wav"))
    for motor, pasta in motores.items():
        pasta = Path(pasta)
        grupos = defaultdict(list)
        limpa = []
        for nome in nomes:
            m = medir_arquivo(corpus, nome, pasta, cache)
            partes = nome[:-4].split("_")
            if partes[1] == "limpo":
                limpa.append(m)
            else:
                grupos[f"{partes[1]}_{partes[2]}"].append(m)
        linhas = {}
        for g, itens in sorted(grupos.items()):
            linhas[g] = {
                "n": len(itens),
                "si_sdri_db": float(np.mean([i["si_sdr_out"] - i["si_sdr_in"] for i in itens])),
                "stoi_in": float(np.mean([i["stoi_in"] for i in itens])),
                "stoi_out": float(np.mean([i["stoi_out"] for i in itens])),
            }
        todos = [i for itens in grupos.values() for i in itens]
        resultado[motor] = {
            "por_condicao": linhas,
            "si_sdri_db_medio": float(np.mean([i["si_sdr_out"] - i["si_sdr_in"] for i in todos])),
            "si_sdri_db_pior_condicao": min(v["si_sdri_db"] for v in linhas.values()),
            "stoi_ganho_medio": float(np.mean([i["stoi_out"] - i["stoi_in"] for i in todos])),
            "limpa_stoi_medio": float(np.mean([i["stoi_out"] for i in limpa])),
            "limpa_stoi_pior": float(np.min([i["stoi_out"] for i in limpa])),
            "limpa_si_sdr_medio": float(np.mean([i["si_sdr_out"] for i in limpa])),
            "atraso_amostras_mediano": int(np.median([i["lag"] for i in todos + limpa])),
            "transparente_na_fala_limpa": bool(np.min([i["stoi_out"] for i in limpa]) >= TETO_STOI_LIMPA),
        }
    Path(saida).write_text(json.dumps(resultado, indent=1, ensure_ascii=False))
    for motor, r in resultado.items():
        print(f"\n== {motor}: SI-SDRi medio {r['si_sdri_db_medio']:+.2f} dB (pior condicao {r['si_sdri_db_pior_condicao']:+.2f}), "
              f"ganho de STOI {r['stoi_ganho_medio']:+.4f}; fala LIMPA: STOI medio {r['limpa_stoi_medio']:.4f} (pior {r['limpa_stoi_pior']:.4f}, "
              f"teto {TETO_STOI_LIMPA:.4f}: {'OK' if r['transparente_na_fala_limpa'] else 'ABAIXO'}); atraso mediano {r['atraso_amostras_mediano']} amostras")
        for g, v in r["por_condicao"].items():
            print(f"   {g:18s} SI-SDRi {v['si_sdri_db']:+6.2f} dB  STOI {v['stoi_in']:.3f} -> {v['stoi_out']:.3f}")


if __name__ == "__main__":
    pares = dict(a.split("=", 1) for a in sys.argv[3:])
    main(sys.argv[1], sys.argv[2], pares)
