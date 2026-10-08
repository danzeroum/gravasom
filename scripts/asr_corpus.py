"""Monta a gravacao longa pt-BR do spike de transcricao (ADR-013): as locucoes de corpus/falas_pt_asr.txt coladas com 0,5 s
de pausa ate completar 10 min, mais o texto de referencia. A MESMA gravacao vale no host (asr_spike.py) e no celular
(spike/whisper), para os WER serem comparaveis.

Uso como programa: asr_corpus.py <dir_pt> <lista_asr.txt> <saida.wav> <saida.txt> [segundos=600]
"""
import re
import sys
import unicodedata
import wave
from pathlib import Path

import numpy as np

SR = 16000
PAUSA = np.zeros(SR // 2, dtype="<i2")


def normalizar(t: str) -> str:
    t = unicodedata.normalize("NFC", t).lower()
    t = re.sub(r"[^\w\s]", " ", t)
    return re.sub(r"\s+", " ", t).strip()


def ler(p):
    with wave.open(str(p), "rb") as w:
        return np.frombuffer(w.readframes(w.getnframes()), dtype="<i2")


def referencias(pt_dir: Path) -> dict:
    refs = {}
    for t in ("dev", "train"):
        arq = pt_dir / f"fleurs-{t}.tsv"
        if arq.exists():
            for linha in arq.read_text(encoding="utf-8").splitlines():
                c = linha.split("\t")
                refs[c[1]] = c[3]
    return refs


def itens(lista: Path) -> list:
    return [l.split("\t") for l in lista.read_text(encoding="utf-8").splitlines() if l and not l.startswith("#")]


def montar(pt_dir: Path, lista: Path, limite_s: int = 600):
    """(pcm int16, texto de referencia normalizado, arquivos usados). Para quando a proxima locucao passaria do limite."""
    refs = referencias(pt_dir)
    partes, textos, usados, total = [], [], [], 0
    for item in itens(lista):
        x = ler(pt_dir / item[1])
        if total + len(x) + len(PAUSA) > limite_s * SR:
            break
        partes += [x, PAUSA]
        textos.append(refs[item[1]])
        usados.append(item[1])
        total += len(x) + len(PAUSA)
    return np.concatenate(partes), normalizar(" ".join(textos)), usados


def gravar(p, x):
    with wave.open(str(p), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(np.asarray(x, dtype="<i2").tobytes())


if __name__ == "__main__":
    pcm, texto, usados = montar(Path(sys.argv[1]), Path(sys.argv[2]), int(sys.argv[5]) if len(sys.argv) > 5 else 600)
    gravar(sys.argv[3], pcm)
    Path(sys.argv[4]).write_text(texto, encoding="utf-8")
    print(f"{len(usados)} locucoes, {len(pcm) / SR:.1f} s")
