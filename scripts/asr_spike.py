"""Spike de transcricao (ADR-013): WER pt-BR, tempo e memoria de whisper.cpp para tiny/base com e sem quantizacao.

Mede no HOST (x86-64): isto vale para WER e para memoria, mas o TEMPO do host nao e o do celular. Tempo, bateria e
temperatura no aparelho vem do harness instrumentado (spike/whisper, rodado no celular do dono).

Dois testes por modelo:
  A) por locucao (60 locucoes FLEURS pt_br de corpus/falas_pt_asr.txt): WER agregado;
  B) gravacao longa (as mesmas 60 locucoes coladas com 0,5 s de pausa, ~11 min): WER, tempo e pico de memoria, que e
     o uso real (aula/reuniao de 10 min com a decodificacao longa do whisper).

Uso: asr_spike.py <dir_pt> <lista_asr.txt> <whisper-cli> <saida.json> <modelo.bin>...
Com ASR_SO_LONGO=1 pula o teste A (por locucao) e roda so a gravacao longa.
"""
import json
import os
import re
import resource
import subprocess
import sys
import time
import unicodedata
import wave
from pathlib import Path

import jiwer
import numpy as np

sys.path.insert(0, os.path.dirname(__file__))
from asr_corpus import montar  # noqa: E402

SR = 16000


def normalizar(t: str) -> str:
    t = unicodedata.normalize("NFC", t).lower()
    t = re.sub(r"[^\w\s]", " ", t)
    return re.sub(r"\s+", " ", t).strip()


def ler(p):
    with wave.open(str(p), "rb") as w:
        return np.frombuffer(w.readframes(w.getnframes()), dtype="<i2")


def gravar(p, x):
    with wave.open(str(p), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(x.astype("<i2").tobytes())


def transcrever(cli, modelo, wav, threads=4, extra=()):
    """Roda o whisper-cli; devolve (texto, segundos de parede, pico de memoria em MiB do processo filho)."""
    antes = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss
    t0 = time.perf_counter()
    r = subprocess.run(
        [cli, "-m", str(modelo), "-f", str(wav), "-l", "pt", "-t", str(threads), "-np", *extra],
        capture_output=True, text=True, check=True,
    )
    dt = time.perf_counter() - t0
    pico = resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss
    # Sem "-nt": desligar os tokens de tempo faz o whisper andar a janela de 30 s as cegas e perder palavras nas emendas.
    texto = " ".join(re.sub(r"^\[[^\]]*\]\s*", "", linha) for linha in r.stdout.splitlines())
    return texto.strip(), dt, pico / 1024.0 if pico > antes else None


def main():
    pt_dir, lista, cli, saida = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3], Path(sys.argv[4])
    modelos = [Path(m) for m in sys.argv[5:]]
    refs = {}
    for t in ("dev", "train"):
        for linha in (pt_dir / f"fleurs-{t}.tsv").read_text(encoding="utf-8").splitlines():
            c = linha.split("\t")
            refs[c[1]] = c[3]
    itens = [l.split("\t") for l in lista.read_text(encoding="utf-8").splitlines() if l and not l.startswith("#")]
    arquivos = [pt_dir / i[1] for i in itens]
    referencias = [refs[a.name] for a in arquivos]
    duracao_total = sum(len(ler(a)) for a in arquivos) / SR
    longo = pt_dir / "_longo.wav"
    pcm_longo, ref_longo, usadas = montar(pt_dir, lista, 600)  # a MESMA gravacao de 10 min do celular
    gravar(longo, pcm_longo)
    duracao_longo = len(pcm_longo) / SR
    out = {"locucoes": len(arquivos), "duracao_s": round(duracao_total, 1), "modelos": {}}
    so_longo = os.environ.get("ASR_SO_LONGO") == "1"
    for m in modelos:
        hip = []
        t_loc = 0.0
        if not so_longo:
            for a in arquivos:
                txt, dt, _ = transcrever(cli, m, a)
                hip.append(normalizar(txt))
                t_loc += dt
        wer_loc = None if so_longo else jiwer.wer([normalizar(r) for r in referencias], hip)
        txt, dt, pico = transcrever(cli, m, longo)
        wer_longo = jiwer.wer(normalizar(ref_longo), normalizar(txt))
        out["modelos"][m.name] = {
            "tamanho_mib": round(m.stat().st_size / 2**20, 1),
            "wer_por_locucao": None if wer_loc is None else round(wer_loc, 4),
            "wer_gravacao_longa": round(wer_longo, 4),
            "tempo_longo_s": round(dt, 1),
            "locucoes_na_gravacao_longa": len(usadas),
            "rtf_host_longo": round(dt / duracao_longo, 3),
            "pico_memoria_mib": round(pico, 0) if pico else None,
        }
        print(m.name, out["modelos"][m.name], flush=True)
        saida.write_text(json.dumps(out, indent=2, ensure_ascii=False), encoding="utf-8")


if __name__ == "__main__":
    main()
