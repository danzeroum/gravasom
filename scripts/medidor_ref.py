#!/usr/bin/env python3
"""Medidor com referencia (SI-SDR, SI-SDRi, STOI via pystoi). Roda no venv de scripts/requirements-medidas.txt,
NUNCA no ambiente do audio-suite (ADR-009).

Alinha a saida a fala limpa por correlacao cruzada por FFT (O(n log n)) e REGISTRA o atraso medido; se o
algoritmo declarar o atraso, passe `atraso_conhecido` e o medidor confere contra o medido.
"""
import json
import sys

import numpy as np
from pystoi import stoi as _stoi
from scipy.io import wavfile
from scipy.signal import fftconvolve

TETO_SI_SDR_DB = 100.0  # saida identica daria infinito; limita para o gate ser comparavel


def ler(path):
    taxa, x = wavfile.read(path)
    if x.dtype == np.int16:
        x = x.astype(np.float64) / 32768.0
    elif x.dtype == np.int32:
        x = x.astype(np.float64) / 2147483648.0
    else:
        x = x.astype(np.float64)
    return taxa, (x if x.ndim == 1 else x[:, 0])


def atraso_medido(ref, out, max_ms=100.0, taxa=48000):
    """Atraso (amostras) de `out` em relacao a `ref`: positivo = out chega depois."""
    n = min(len(ref), len(out))
    m = int(max_ms * taxa / 1000)
    c = fftconvolve(out[:n], ref[:n][::-1], mode="full")
    centro = n - 1
    janela = c[centro - m : centro + m + 1]
    return int(np.argmax(janela) - m)


def _recortar(ref, out, lag):
    if lag > 0:
        out = out[lag:]
    elif lag < 0:
        ref = ref[-lag:]
    n = min(len(ref), len(out))
    return ref[:n], out[:n]


def alinhar(ref, out, taxa, atraso_conhecido=None, refino=16):
    """Lag grosso por correlacao FFT + refino que maximiza o proprio SI-SDR (a fase de um filtro desloca o pico)."""
    grosso = atraso_medido(ref, out, taxa=taxa)
    lag = min(range(grosso - refino, grosso + refino + 1), key=lambda l: -si_sdr(*_recortar(ref, out, l)))
    if atraso_conhecido is not None and abs(lag - atraso_conhecido) > 1:
        raise ValueError(f"atraso medido {lag} difere do declarado {atraso_conhecido}")
    return (*_recortar(ref, out, lag), lag)


def si_sdr(ref, est):
    ref = ref - ref.mean()
    est = est - est.mean()
    alvo = (np.dot(est, ref) / (np.dot(ref, ref) + 1e-20)) * ref
    ruido = est - alvo
    v = 10 * np.log10((np.sum(alvo**2) + 1e-20) / (np.sum(ruido**2) + 1e-20))
    return float(min(v, TETO_SI_SDR_DB))


def medir(ref_path, out_path, entrada_path=None, atraso_conhecido=None):
    taxa, ref = ler(ref_path)
    taxa2, out = ler(out_path)
    assert taxa == taxa2, "taxas diferentes"
    r, o, lag = alinhar(ref, out, taxa, atraso_conhecido)
    res = {"atraso_amostras": lag, "si_sdr_db": si_sdr(r, o), "stoi": float(_stoi(r, o, taxa, extended=False)),
           "estoi": float(_stoi(r, o, taxa, extended=True))}
    if entrada_path:
        _, ent = ler(entrada_path)
        r2, e, _ = alinhar(ref, ent, taxa)
        res["si_sdri_db"] = res["si_sdr_db"] - si_sdr(r2, e)
        res["stoi_delta"] = res["stoi"] - float(_stoi(r2, e, taxa, extended=False))
    return res


if __name__ == "__main__":
    ref, out = sys.argv[1], sys.argv[2]
    print(json.dumps(medir(ref, out, sys.argv[3] if len(sys.argv) > 3 else None), indent=1))
