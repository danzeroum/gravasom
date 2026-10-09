#!/usr/bin/env python3
"""Medidor INDEPENDENTE da Onda 2 (nao usa nada do codigo Kotlin/C++ do app). Roda no venv de scripts/requirements-medidas.txt.

  onda2_medir.py normalizacao <dir_entrada> <dir_saida> [alvo_lufs]
      Loudness integrada por DOIS medidores (pyloudnorm e ffmpeg ebur128) e pico verdadeiro por DOIS (ffmpeg ebur128 peak=true e
      sobre-amostragem 8x com scipy). Falha (exit 1) se a loudness de saida nao estiver a +-1 LU do alvo ou se o pico verdadeiro
      passar de -1 dBTP. Escreve um relatorio TSV em <dir_saida>/medido.tsv.
"""
import re
import subprocess
import sys
from pathlib import Path

import numpy as np
import pyloudnorm as pyln
from scipy.io import wavfile
from scipy.signal import resample_poly

TOLERANCIA_LU = 1.0
TETO_DBTP = -1.0


def ler(p):
    taxa, x = wavfile.read(p)
    x = x.astype(np.float64) / 32768.0
    return taxa, (x if x.ndim == 1 else x)


def loudness_pyln(taxa, x):
    return float(pyln.Meter(taxa).integrated_loudness(x))


def ffmpeg_ebur128(path):
    """(LUFS integrada, pico verdadeiro dBTP) segundo o filtro ebur128 do ffmpeg (libebur128)."""
    r = subprocess.run(["ffmpeg", "-nostats", "-i", str(path), "-af", "ebur128=peak=true", "-f", "null", "-"], capture_output=True, text=True)
    resumo = r.stderr.split("Summary:")[-1]
    lufs = float(re.search(r"I:\s+(-?[\d.]+) LUFS", resumo).group(1))
    pico = float(re.search(r"True peak:.*?Peak:\s+(-?[\d.inf]+) dBFS", resumo, re.S).group(1))
    return lufs, pico


def pico_verdadeiro_8x(x):
    y = resample_poly(x, 8, 1, axis=0)
    p = np.max(np.abs(y))
    return 20 * np.log10(max(p, 1e-12))


def normalizacao(entrada, saida, alvo=-16.0):
    entrada, saida = Path(entrada), Path(saida)
    linhas = ["arquivo\tlufs_in\tlufs_out_pyln\tlufs_out_ffmpeg\tpico_ffmpeg\tpico_8x\tgasto_db"]
    falhas = []
    for f_out in sorted(saida.glob("*.wav")):
        f_in = entrada / f_out.name
        t_in, x_in = ler(f_in)
        t_out, x_out = ler(f_out)
        l_in = loudness_pyln(t_in, x_in)
        l_py = loudness_pyln(t_out, x_out)
        l_ff, p_ff = ffmpeg_ebur128(f_out)
        p_8 = pico_verdadeiro_8x(x_out)
        pico = max(p_ff, p_8)
        ganho = l_py - l_in
        linhas.append(f"{f_out.name}\t{l_in:.2f}\t{l_py:.2f}\t{l_ff:.2f}\t{p_ff:.2f}\t{p_8:.2f}\t{ganho:.2f}")
        if abs(l_py - alvo) > TOLERANCIA_LU or abs(l_ff - alvo) > TOLERANCIA_LU:
            falhas.append(f"{f_out.name}: loudness {l_py:.2f} (pyloudnorm) / {l_ff:.2f} (ffmpeg), alvo {alvo}")
        if pico > TETO_DBTP:
            falhas.append(f"{f_out.name}: pico verdadeiro {pico:.2f} dBTP > {TETO_DBTP}")
        if len(x_in) != len(x_out):
            falhas.append(f"{f_out.name}: duracao em amostras mudou ({len(x_in)} -> {len(x_out)})")
    (saida / "medido.tsv").write_text("\n".join(linhas) + "\n")
    print("\n".join(linhas))
    dados = [l.split("\t") for l in linhas[1:]]
    if dados:
        py = [float(d[2]) for d in dados]
        ff = [float(d[3]) for d in dados]
        pk = [max(float(d[4]), float(d[5])) for d in dados]
        print(f"\n{len(dados)} arquivos | loudness pyloudnorm min/med/max = {min(py):.2f}/{np.mean(py):.2f}/{max(py):.2f} | "
              f"ffmpeg {min(ff):.2f}/{np.mean(ff):.2f}/{max(ff):.2f} | pior pico verdadeiro {max(pk):.2f} dBTP")
    if falhas:
        print("\nFALHOU:\n" + "\n".join(falhas))
        sys.exit(1)
    print("\nOK: dentro de +-1 LU do alvo e pico verdadeiro <= -1 dBTP em todos os arquivos.")


if __name__ == "__main__":
    if len(sys.argv) >= 4 and sys.argv[1] == "normalizacao":
        normalizacao(sys.argv[2], sys.argv[3], float(sys.argv[4]) if len(sys.argv) > 4 else -16.0)
    else:
        sys.exit(__doc__)
