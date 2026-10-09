#!/usr/bin/env python3
"""Corpus do spike/harness do redutor de ruido (ADR-015): fala pt-BR (FLEURS, scripts/corpus-pt.sh) a 48 kHz, limpa e com ruido.

Para cada locucao: <nome>_limpo.wav e, para cada ruido x SNR, <nome>_<ruido>_snr<dB>.wav, com o ruido somado a um RMS definido contra o
RMS da fala. Ruidos (todos sinteticos e deterministas): ventilador (1/f filtrado + tons de 150/300 Hz), zumbido (60 Hz + harmonicas),
rosa, murmurio (4 outras locucoes sobrepostas: o caso dificil, ruido tambem e fala). Tambem grava <nome>_ref.wav = a fala limpa
(a referencia dos medidores). Uso: onda2_ruido_corpus.py <dir_fala_16k> <dir_saida> [n_locucoes]
"""
import subprocess
import sys
import wave
from pathlib import Path

import numpy as np

SR = 48000
SNRS = (0, 5, 10, 15)
RUIDOS = ("ventilador", "zumbido", "rosa", "murmurio")
LEVEL_DBFS = -22.0


def ler(p):
    with wave.open(str(p), "rb") as w:
        assert w.getnchannels() == 1 and w.getsampwidth() == 2
        return w.getframerate(), np.frombuffer(w.readframes(w.getnframes()), dtype="<i2").astype(np.float64) / 32768.0


def gravar(p, x, rate=SR):
    with wave.open(str(p), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(np.round(np.clip(x, -1, 1 - 1 / 32768) * 32768).astype("<i2").tobytes())


def rms(x):
    return float(np.sqrt(np.mean(x * x) + 1e-20))


def para_48k(src, dst):
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(src), "-ar", str(SR), "-sample_fmt", "s16", str(dst)], check=True)


def colorido(n, rng, expoente):
    b = rng.standard_normal(n)
    spec = np.fft.rfft(b)
    f = np.fft.rfftfreq(n, 1 / SR)
    f[0] = f[1]
    spec *= f ** (-expoente / 2)
    y = np.fft.irfft(spec, n)
    return y / rms(y)


def ventilador(n, rng):
    y = colorido(n, rng, 0.8)
    spec = np.fft.rfft(y)
    f = np.fft.rfftfreq(n, 1 / SR)
    y = np.fft.irfft(spec / (1 + (f / 3500.0) ** 4), n)
    t = np.arange(n) / SR
    y = y / rms(y) + 0.35 * np.sin(2 * np.pi * 150 * t) + 0.15 * np.sin(2 * np.pi * 300 * t)
    return y / rms(y)


def zumbido(n, rng):
    t = np.arange(n) / SR
    y = sum((0.7**k) * np.sin(2 * np.pi * 60 * (k + 1) * t + rng.uniform(0, 6.28)) for k in range(8))
    return y / rms(y)


def murmurio(n, rng, pool):
    soma = np.zeros(n)
    for _ in range(4):
        x = pool[int(rng.integers(len(pool)))]
        x = np.tile(x, n // len(x) + 2)
        ini = int(rng.integers(0, len(x) - n))
        soma += x[ini : ini + n]
    return soma / rms(soma)


def main(dir_fala, saida, n_loc=8):
    saida = Path(saida)
    saida.mkdir(parents=True, exist_ok=True)
    tmp = saida / ".tmp"
    tmp.mkdir(exist_ok=True)
    arquivos = sorted(Path(dir_fala).glob("*.wav"))
    pool_all = []
    for f in arquivos:
        para_48k(f, tmp / f.name)
        _, x = ler(tmp / f.name)
        pool_all.append(x * (10 ** (LEVEL_DBFS / 20) / rms(x)))
    rng = np.random.default_rng(20261009)
    for idx, f in enumerate(arquivos[: int(n_loc)]):
        limpo = pool_all[idx]
        nome = f.stem
        gravar(saida / f"{nome}_ref.wav", limpo)
        gravar(saida / f"{nome}_limpo.wav", limpo)
        pool = [p for j, p in enumerate(pool_all) if j != idx]
        for tipo in RUIDOS:
            ruido_unit = {"ventilador": lambda n: ventilador(n, rng), "zumbido": lambda n: zumbido(n, rng),
                          "rosa": lambda n: colorido(n, rng, 1.0), "murmurio": lambda n: murmurio(n, rng, pool)}[tipo](len(limpo))
            for snr in SNRS:
                mix = limpo + ruido_unit * (rms(limpo) / 10 ** (snr / 20))
                gravar(saida / f"{nome}_{tipo}_snr{snr}.wav", mix)
    for p in tmp.iterdir():
        p.unlink()
    tmp.rmdir()
    print(f"{len(list(saida.glob('*.wav')))} arquivos em {saida}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else 8)
