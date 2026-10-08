#!/usr/bin/env python3
"""Roda `audio-suite analyze` num WAV e devolve metricas planas (so stdlib; o audio-suite fica isolado no pipx).

Uso: audio_suite_medir.py <perfil.yaml> <wav>...   -> imprime JSON {arquivo: {metrica: valor}}
Estados de saida do audio-suite: 0 verde, 1 vermelho (achado), 2/3/64/ausente indeciso (ver audio-suite-gate.sh).
"""
import json
import os
import shutil
import subprocess
import sys
import tempfile


def binario() -> str:
    exe = os.environ.get("AUDIO_SUITE", "audio-suite")
    if shutil.which(exe) is None:
        raise FileNotFoundError(f"audio-suite ausente ({exe})")
    return exe


def medir(perfil: str, wav: str) -> dict:
    with tempfile.NamedTemporaryFile(suffix=".json", delete=False) as t:
        saida = t.name
    try:
        p = subprocess.run(
            [binario(), "analyze", wav, "--profile", perfil, "--format", "json", "--output", saida],
            capture_output=True, text=True, check=False,
        )
        if p.returncode not in (0, 1):
            raise RuntimeError(f"audio-suite exit={p.returncode}: {p.stderr[-300:]}")
        d = json.load(open(saida))
    finally:
        os.unlink(saida)
    m = {"_exit": p.returncode, "_status": d.get("aggregate_status")}
    glitch = 0
    for f in d["findings"]:
        a, metric, v = f["analyzer"], f.get("metric"), f.get("value")
        if a == "glitch":
            glitch += len(f.get("evidence", {}).get("events", []))
        if isinstance(v, (int, float)):
            m[f"{a}.{metric}"] = v
        m[f"{a}.status"] = f["status"] if f.get("check_id", "").startswith(a) and f"{a}.status" not in m else m.get(f"{a}.status", f["status"])
        for k, ev in f.get("evidence", {}).items():
            if isinstance(ev, (int, float)) and not isinstance(ev, bool):
                m[f"{a}.ev.{k}"] = ev
    m["glitch.eventos"] = glitch
    return m


if __name__ == "__main__":
    perfil, arquivos = sys.argv[1], sys.argv[2:]
    print(json.dumps({os.path.basename(a): medir(perfil, a) for a in arquivos}, indent=1, sort_keys=True))
