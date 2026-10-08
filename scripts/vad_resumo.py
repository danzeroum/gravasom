"""Resumo em Markdown do relatorio do harness do VAD (VadCorpusEvalTest). So stdlib.

Uso: vad_resumo.py <relatorio.tsv> [margem_padrao=250/700]
Colunas do relatorio: arquivo, cenario, modo, pad_ms (cabeca/cauda), trechos, trechos_cortados, quadros_fala,
quadros_cortados, economia_ms, economia_ideal_ms, duracao_ms.
"""
import csv
import sys
from collections import defaultdict

NOMES = {"1": "suave (1)", "2": "normal (2)", "3": "forte (3, nao oferecido)"}


def main():
    linhas = list(csv.DictReader(open(sys.argv[1], encoding="utf-8"), delimiter="\t"))
    padrao = sys.argv[2] if len(sys.argv) > 2 else "200/500"
    geral = defaultdict(lambda: [0] * 7)
    for r in linhas:
        a = geral[(r["modo"], r["pad_ms"])]
        for i, k in enumerate(("trechos", "trechos_cortados", "quadros_fala", "quadros_cortados", "economia_ms", "economia_ideal_ms", "duracao_ms")):
            a[i] += int(r[k])
    print("### Fala cortada por modo e margens (cabeca/cauda em ms), todos os cenarios\n")
    print("| modo | margens | trechos de fala | trechos cortados | quadros cortados / fala | economia (% da duracao) | % do ideal |")
    print("| --- | --- | ---: | ---: | ---: | ---: | ---: |")
    for (modo, pad), a in sorted(geral.items()):
        print(f"| {NOMES.get(modo, modo)} | {pad} | {a[0]} | {a[1]} | {a[3]}/{a[2]} | {100 * a[4] / a[6]:.1f}% | {100 * a[4] / max(a[5], 1):.0f}% |")
    cen = defaultdict(lambda: [0] * 5)
    for r in linhas:
        if r["pad_ms"] != padrao:
            continue
        a = cen[(r["modo"], r["cenario"])]
        for i, k in enumerate(("trechos", "trechos_cortados", "economia_ms", "economia_ideal_ms", "duracao_ms")):
            a[i] += int(r[k])
    print(f"\n### Por cenario, margens {padrao}\n")
    print("| modo | cenario | trechos | cortados | economia (% da duracao) | % do ideal |")
    print("| --- | --- | ---: | ---: | ---: | ---: |")
    for (modo, c), a in sorted(cen.items()):
        print(f"| {NOMES.get(modo, modo)} | {c} | {a[0]} | {a[1]} | {100 * a[2] / a[4]:.1f}% | {100 * a[2] / max(a[3], 1):.0f}% |")


if __name__ == "__main__":
    main()
