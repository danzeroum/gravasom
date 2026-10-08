#!/usr/bin/env python3
"""Spike (ADR-015): DeepFilterNet3 (PyTorch, CPU) sobre os WAV de 48 kHz de uma pasta. Roda no venv do DeepFilterNet
(python3.11, torch 2.1.2, deepfilternet 0.5.6) com os pesos Apache-2.0 de huggingface.co/fal/DeepFilterNet3 (config.ini + model.safetensors).
Uso: dfn_wav.py <dir_modelo> <dir_entrada> <dir_saida> [intensidade 0..1]
Mede o tempo de inferencia por quadro de 10 ms (offline em lote: indicativo, nao e streaming em tempo real no celular)."""
import sys
import time
import wave
from pathlib import Path

import numpy as np
import torch
from df.enhance import enhance, init_df
from df.model import ModelParams  # noqa: F401


def ler(p):
    with wave.open(str(p), "rb") as w:
        return np.frombuffer(w.readframes(w.getnframes()), dtype="<i2").astype(np.float32) / 32768.0


def gravar(p, x):
    with wave.open(str(p), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(48000)
        w.writeframes(np.round(np.clip(x, -1, 1 - 1 / 32768) * 32768).astype("<i2").tobytes())


def main(modelo, entrada, saida, intensidade=1.0):
    torch.set_num_threads(1)
    model, df_state, _ = init_df(model_base_dir=modelo, log_level="WARNING")
    assert df_state.sr() == 48000
    Path(saida).mkdir(parents=True, exist_ok=True)
    total_s, quadros = 0.0, 0
    for f in sorted(Path(entrada).glob("*.wav")):
        x = ler(f)
        t0 = time.perf_counter()
        y = enhance(model, df_state, torch.from_numpy(x)[None, :], pad=True).squeeze(0).numpy()
        total_s += time.perf_counter() - t0
        quadros += len(x) // 480
        y = y[: len(x)]
        if len(y) < len(x):
            y = np.pad(y, (0, len(x) - len(y)))
        gravar(Path(saida) / f.name, x + intensidade * (y - x) if intensidade < 1.0 else y)
    print(f"dfn3: {quadros} quadros, {1e6 * total_s / quadros:.1f} us por quadro de 10 ms (1 thread, host)")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3], float(sys.argv[4]) if len(sys.argv) > 4 else 1.0)
