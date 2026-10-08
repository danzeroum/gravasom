"""Prova de ponta a ponta de que o "pular silencio" nao remove fala (ADR-014): o WER da transcricao do audio ENCURTADO pelo plano
nao pode ser pior que o do audio original. Usa o whisper.cpp (modelo base quantizado) sobre as gravacoes de sala silenciosa do
corpus do VAD, com as faixas puladas que o VadCorpusEvalTest gravou em <relatorio>.planos.

Uso: vad_asr.py <dir_corpus_vad> <relatorio.tsv.planos> <whisper-cli> <modelo.bin> [margem_wer=0.02]
Sai 1 se algum arquivo piorar mais que a margem (em fracao de WER).
"""
import os
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import jiwer
import numpy as np

sys.path.insert(0, os.path.dirname(__file__))
from asr_corpus import normalizar  # noqa: E402


def ler16k(p):
    with tempfile.TemporaryDirectory() as t:
        out = Path(t) / "x.wav"
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(p), "-ac", "1", "-ar", "16000", "-sample_fmt", "s16", str(out)], check=True)
        with wave.open(str(out), "rb") as w:
            return np.frombuffer(w.readframes(w.getnframes()), dtype="<i2")


def gravar(p, x):
    with wave.open(str(p), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(16000)
        w.writeframes(np.asarray(x, dtype="<i2").tobytes())


def transcrever(cli, modelo, wav):
    r = subprocess.run([cli, "-m", str(modelo), "-f", str(wav), "-l", "pt", "-t", "4", "-np"], capture_output=True, text=True, check=True)
    import re
    return " ".join(re.sub(r"^\[[^\]]*\]\s*", "", l) for l in r.stdout.splitlines()).strip()


def pular(x, faixas_ms):
    mantido, cursor = [], 0
    for a, b in faixas_ms:
        a, b = int(a * 16), int(b * 16)
        mantido.append(x[cursor:a])
        cursor = b
    mantido.append(x[cursor:])
    return np.concatenate(mantido)


def main():
    corpus, planos, cli, modelo = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3], sys.argv[4]
    margem = float(sys.argv[5]) if len(sys.argv) > 5 else 0.02
    ruim = False
    print("| arquivo | nivel | duracao original | duracao depois do pulo | WER original | WER depois do pulo |")
    print("| --- | --- | ---: | ---: | ---: | ---: |")
    for linha in planos.read_text(encoding="utf-8").splitlines():
        nome, modo, faixas = (linha.split("\t") + [""])[:3]
        if not nome.endswith("_limpo"):
            continue  # com ruido o plano e vazio (nada a pular); o limpo e onde o pulo age
        ref = normalizar((corpus / f"{nome}.ref.txt").read_text(encoding="utf-8"))
        x = ler16k(corpus / f"{nome}.wav")
        ranges = [tuple(map(int, f.split("-"))) for f in faixas.split(";") if f]
        y = pular(x, ranges)
        with tempfile.TemporaryDirectory() as t:
            gravar(Path(t) / "orig.wav", x)
            gravar(Path(t) / "pulado.wav", y)
            wer_o = jiwer.wer(ref, normalizar(transcrever(cli, modelo, Path(t) / "orig.wav")))
            wer_p = jiwer.wer(ref, normalizar(transcrever(cli, modelo, Path(t) / "pulado.wav")))
        print(f"| {nome} | {modo} | {len(x) / 16000:.1f} s | {len(y) / 16000:.1f} s | {wer_o:.3f} | {wer_p:.3f} |", flush=True)
        if wer_p > wer_o + margem:
            ruim = True
    sys.exit(1 if ruim else 0)


if __name__ == "__main__":
    main()
