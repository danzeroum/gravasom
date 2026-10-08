"""Gera o corpus do harness do VAD (ADR-014) a partir da fala pt-BR de scripts/corpus-pt.sh.

Cada "gravacao" lembra uma aula ou reuniao: falas separadas por pausas de 1,2 a 15 s. A verdade-terreno (oraculo) sai do
SINAL LIMPO (so a fala, zeros nas pausas): ver oraculo_locucao (ancorado nos quadros vozeados e fortes, com margem para
consoantes surdas nas pontas). Depois o ruido entra por cima (ventilador, rosa, zumbido, murmurio de vozes) em SNR
definido contra o RMS da fala. Determinista (sementes fixas). So numpy; ffmpeg faz a reamostragem das variantes de taxa.

Saida em <dir>: <nome>.wav, <nome>.oraculo (um '0'/'1' por quadro de 20 ms) e manifest.tsv
(nome, wav, oraculo, cenario, pausa_ideal_ms).
"""
import subprocess
import sys
import wave
from pathlib import Path

import numpy as np

SR = 16000
FRAME = 320
LEVEL_DBFS = -22.0  # RMS alvo de cada locucao
PAUSAS_S = [1.2, 2.5, 4.0, 8.0, 15.0]
ROOM_DBFS = -65.0


def ler(p):
    with wave.open(str(p), "rb") as w:
        assert w.getframerate() == SR and w.getnchannels() == 1 and w.getsampwidth() == 2
        return np.frombuffer(w.readframes(w.getnframes()), dtype="<i2").astype(np.float64) / 32768.0


def gravar(p, x, rate=SR):
    x = np.clip(x, -1.0, 1.0 - 1.0 / 32768)
    with wave.open(str(p), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes((np.round(x * 32768)).astype("<i2").tobytes())


def rms(x):
    return float(np.sqrt(np.mean(x * x) + 1e-20))


def db(v):
    return 20 * np.log10(v + 1e-12)


def normalizar(x):
    return x * (10 ** (LEVEL_DBFS / 20) / max(rms(x), 1e-9))


def quadros_db(x):
    n = len(x) // FRAME
    f = x[: n * FRAME].reshape(n, FRAME)
    return 10 * np.log10(np.mean(f * f, axis=1) + 1e-12)


def vozeado(frame):
    """Quadro periodico (voz sustentada)? Autocorrelacao normalizada em 70-400 Hz > 0,5."""
    f = frame - frame.mean()
    e = float(np.dot(f, f))
    if e < 1e-9:
        return False
    n = len(f)
    lags = range(SR // 400, SR // 70 + 1)
    best = max(float(np.dot(f[: n - k], f[k:])) / e for k in lags)
    return best > 0.5


def passa_faixa(x, baixo_hz=250.0, alto_hz=4000.0):
    """Banda de inteligibilidade da fala (250 Hz-4 kHz). Ronco, sopro no microfone e deriva (< 250 Hz) nao sao fala e
    enganam a autocorrelacao; tambem derrubam a comparacao de nivel com o percentil 95 da locucao."""
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    spec[(f < baixo_hz) | (f > alto_hz)] = 0
    return np.fft.irfft(spec, len(x))


def ronco(frames_originais):
    """True onde > 80% da energia do quadro esta abaixo de 150 Hz: sopro/estalo no microfone, nao fala."""
    espectro = np.abs(np.fft.rfft(frames_originais * np.hanning(FRAME), axis=1)) ** 2
    baixo = espectro[:, : int(150 / (SR / FRAME)) + 1].sum(axis=1)
    return baixo > 0.8 * (espectro.sum(axis=1) + 1e-20)


def oraculo_locucao(x):
    """Quadros de fala da locucao. Do primeiro ao ultimo quadro VOZEADO e forte (a vogal e o que ancora a fala), ampliado
    em 150 ms antes e 200 ms depois (consoantes surdas nas pontas, como o /s/ final de "anos"); dentro desse trecho,
    so quadros com energia >= p95 - 30 dB (na banda de fala) e que nao sejam ronco (> 80% da energia abaixo de 150 Hz). Respiracao e ruido de sala antes e depois ficam FORA (nao sao fala); pausas
    longas dentro do trecho tambem ficam fora pela energia. E um superconjunto da fala: errar para este lado torna o
    teste mais duro."""
    original = x
    x = passa_faixa(x)
    d = quadros_db(x)
    teto = np.percentile(d, 95)
    n = len(d)
    frames = x[: n * FRAME].reshape(n, FRAME)
    fortes = np.where(d >= teto - 30.0)[0]
    voz = [int(i) for i in fortes if vozeado(frames[i])]
    saida = np.zeros(n, dtype=bool)
    if not voz:
        return saida
    ini = max(voz[0] - 8, 0)  # 150 ms ~ 8 quadros
    fim = min(voz[-1] + 10, n - 1)  # 200 ms = 10 quadros
    saida[ini : fim + 1] = d[ini : fim + 1] >= max(teto - 30.0, -80.0)
    saida &= ~ronco(original[: n * FRAME].reshape(n, FRAME))
    return saida


def ruido_colorido(n, rng, expoente):
    """Ruido com espectro 1/f^expoente (0 = branco, 1 = rosa) normalizado a RMS 1."""
    b = rng.standard_normal(n)
    spec = np.fft.rfft(b)
    f = np.fft.rfftfreq(n, 1 / SR)
    f[0] = f[1]
    spec *= f ** (-expoente / 2)
    y = np.fft.irfft(spec, n)
    return y / rms(y)


def ventilador(n, rng):
    y = ruido_colorido(n, rng, 0.8)
    spec = np.fft.rfft(y)
    f = np.fft.rfftfreq(n, 1 / SR)
    spec *= 1 / (1 + (f / 3500.0) ** 4)  # passa-baixas suave
    y = np.fft.irfft(spec, n)
    t = np.arange(n) / SR
    y = y / rms(y) + 0.35 * np.sin(2 * np.pi * 150 * t) + 0.15 * np.sin(2 * np.pi * 300 * t)
    return y / rms(y)


def zumbido(n, rng):
    t = np.arange(n) / SR
    y = sum((0.7 ** k) * np.sin(2 * np.pi * 60 * (k + 1) * t + rng.uniform(0, 6.28)) for k in range(8))
    return y / rms(y)


def murmurio(n, rng, pool):
    soma = np.zeros(n)
    for _ in range(4):
        x = normalizar(pool[rng.integers(len(pool))])
        x = np.tile(x, n // len(x) + 2)
        ini = int(rng.integers(0, len(x) - n))
        soma += x[ini : ini + n]
    return soma / rms(soma)


def montar(rng, locucoes):
    """Devolve (sinal_limpo, oraculo_por_quadro, pausa_ideal_ms)."""
    partes, orc = [], []
    pausas = [PAUSAS_S[int(rng.integers(len(PAUSAS_S)))] for _ in range(len(locucoes) + 1)]
    for i, x in enumerate(locucoes):
        p = np.zeros(int(pausas[i] * SR) // FRAME * FRAME)
        partes.append(p)
        orc.append(np.zeros(len(p) // FRAME, dtype=bool))
        x = normalizar(x)
        x = x[: len(x) // FRAME * FRAME]
        partes.append(x)
        orc.append(oraculo_locucao(x))
    p = np.zeros(int(pausas[-1] * SR) // FRAME * FRAME)
    partes.append(p)
    orc.append(np.zeros(len(p) // FRAME, dtype=bool))
    return np.concatenate(partes), np.concatenate(orc), sum(pausas) * 1000


def resample(src, dst, rate):
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(src), "-ar", str(rate), "-sample_fmt", "s16", str(dst)], check=True)


def main():
    pt_dir, lista, saida = Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3])
    saida.mkdir(parents=True, exist_ok=True)
    itens = [l.rstrip("\n").split("\t") for l in lista.read_text(encoding="utf-8").splitlines() if l and not l.startswith("#")]
    homens = [pt_dir / i[1] for i in itens if i[2] == "MALE"]
    mulheres = [pt_dir / i[1] for i in itens if i[2] == "FEMALE"]
    pool = [ler(p) for p in homens + mulheres]
    rng = np.random.default_rng(20261008)
    # 6 gravacoes: 3 com vozes masculinas, 3 femininas, 4 locucoes cada
    grupos = []
    for g, nome in ((homens, "m"), (mulheres, "f")):
        for k in range(3):
            esc = [g[(k * 3 + j) % len(g)] for j in range(4)]
            grupos.append((f"{nome}{k + 1}", esc))
    cenarios = [  # nome, gerador(n, rng) -> ruido unitario | None, SNR dB | None (sala), taxa
        ("limpo", None, None, 44100),
        ("ventilador_snr10", ventilador, 10.0, 16000),
        ("ventilador_snr0", ventilador, 0.0, 48000),
        ("rosa_snr0", lambda n, r: ruido_colorido(n, r, 1.0), 0.0, 16000),
        ("zumbido_snr10", zumbido, 10.0, 16000),
        ("murmurio_snr5", lambda n, r: murmurio(n, r, pool), 5.0, 16000),
    ]
    linhas = []
    for gnome, arquivos in grupos:
        limpo, orc, pausa_ms = montar(rng, [ler(p) for p in arquivos])
        n = len(limpo)
        fala_rms = rms(limpo[np.repeat(orc, FRAME)[: n]]) if orc.any() else 1.0
        for cnome, gen, snr, taxa in cenarios:
            ruido = ruido_colorido(n, rng, 1.0) * 10 ** (ROOM_DBFS / 20) if gen is None else gen(n, rng) * fala_rms * 10 ** (-snr / 20)
            sinal = limpo + ruido
            nome = f"vad_{gnome}_{cnome}"
            wav16 = saida / f"{nome}.wav"
            gravar(wav16, sinal)
            if taxa != SR:
                tmp = saida / f"{nome}.tmp.wav"
                resample(wav16, tmp, taxa)
                tmp.replace(wav16)
            (saida / f"{nome}.oraculo").write_text("".join("1" if v else "0" for v in orc))
            linhas.append(f"{nome}\t{wav16.name}\t{nome}.oraculo\t{cnome}\t{int(pausa_ms)}")
    (saida / "manifest.tsv").write_text("\n".join(linhas) + "\n")
    print(f"{len(linhas)} gravacoes de teste em {saida}")


if __name__ == "__main__":
    main()
