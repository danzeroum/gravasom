#!/usr/bin/env python3
"""Prepara as entradas do harness de normalizacao: cada locucao da fala pt-BR (FLEURS) em tres taxas (16, 44,1 e 48 kHz),
ajustada a uma loudness-alvo BAIXA (padrao -30 LUFS) medida com pyloudnorm. Uso: onda2_preparar.py <dir_fala> <dir_saida> [lufs]"""
import subprocess
import sys
from pathlib import Path

import numpy as np
import pyloudnorm as pyln
from scipy.io import wavfile


def ler(p):
    taxa, x = wavfile.read(p)
    return taxa, x.astype(np.float64) / 32768.0


def main(dir_fala, saida, alvo=-30.0):
    saida = Path(saida)
    saida.mkdir(parents=True, exist_ok=True)
    tmp = saida / ".tmp"
    tmp.mkdir(exist_ok=True)
    arquivos = sorted(Path(dir_fala).glob("*.wav"))
    if not arquivos:
        sys.exit(f"sem WAV em {dir_fala}")
    for f in arquivos:
        for taxa in (16000, 44100, 48000):
            conv = tmp / f"{f.stem}_{taxa}.wav"
            subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(f), "-ar", str(taxa), "-ac", "1", "-sample_fmt", "s16", str(conv)], check=True)
            t, x = ler(conv)
            lufs = pyln.Meter(t).integrated_loudness(x)
            y = x * 10 ** ((alvo - lufs) / 20.0)
            wavfile.write(saida / f"{f.stem}_{taxa // 1000}k.wav", t, np.round(np.clip(y, -1, 1) * 32767).astype(np.int16))
    for p in tmp.iterdir():
        p.unlink()
    tmp.rmdir()
    print(f"{len(list(saida.glob('*.wav')))} arquivos em {saida} (alvo {alvo} LUFS)")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], float(sys.argv[3]) if len(sys.argv) > 3 else -30.0)
