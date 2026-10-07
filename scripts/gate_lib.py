"""Gates objetivos do harness audio-suite (ADR-009). So stdlib; o audio-suite roda isolado (pipx).

Cada gate mede uma grandeza do par (referencia, saida) e tem um CONTROLE POSITIVO designado: uma
degradacao deliberada que o gate tem de reprovar. Os tetos NAO sao digitados aqui: vem de
perfis/tetos.json, gerado por scripts/calibrar_tetos.py a partir do corpus.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from audio_suite_medir import medir  # noqa: E402


def cents(f_out, f_ref):
    if not f_out or not f_ref:
        return float("inf")  # sem F0 na saida (ou na referencia): nao ha como provar que o pitch ficou igual
    return 1200.0 * math.log2(f_out / f_ref)


def grandezas(ref: dict, out: dict) -> dict:
    """Grandezas por gate. Todas sao 'quanto piorou' (>= 0 e bom), exceto onde indicado."""
    return {
        # duracao: diferenca absoluta em amostras (o plano exige identica)
        "duracao_amostras": abs(out["inspect.ev.frames"] - ref["inspect.ev.frames"]),
        # pitch: |delta| em cents da F0 mediana
        "f0_cents": abs(cents(out.get("pitch_f0.f0_median_hz"), ref.get("pitch_f0.f0_median_hz"))),
        # glitch: eventos NOVOS (nunca negativos)
        "glitch_novos": max(0.0, out.get("glitch.glitch_event_count", 0.0) - ref.get("glitch.glitch_event_count", 0.0)),
        # inteligibilidade (proxy sem referencia): QUEDA do stoi_proxy (positivo = piorou)
        "stoi_queda": ref["speech_intelligibility.stoi_proxy_no_ref"] - out["speech_intelligibility.stoi_proxy_no_ref"],
        # defeitos objetivos do proprio audio-suite (clipping / true peak): 1 se o perfil reprova a saida
        "clipping_ou_true_peak": float(out.get("clipping.status") == "fail" or out.get("true_peak.status") == "fail"),
    }


# gate -> sufixo do controle positivo designado e resolucao de medida (piso do bom)
GATES = {
    "duracao_amostras": {"controle": "ctrl_corte50ms", "resolucao": 0.0},
    "f0_cents": {"controle": "ctrl_pitch_mais50c", "resolucao": 1.0},
    "glitch_novos": {"controle": "ctrl_cliques", "resolucao": 1.0},
    # BLOQUEADO (TETO_NAO_DISCRIMINA): o stoi_proxy_no_ref NAO cai com ruido reinjetado a 15 dB em 3 de 7 bases
    # (real_fala_1 +0,016, real_fala_2 -0,018, real_fala_4 -0,045). Fica como observacao ate decisao (ver CALIBRACAO.md).
    "stoi_queda": {"controle": "ctrl_ruido_reinjetado", "resolucao": 0.01, "ativo": False},
    "clipping_ou_true_peak": {"controle": "ctrl_ganho3db_clip", "resolucao": 0.0},
}


def bases(pasta: str):
    """Bases = fala sintetica + falas reais; cada uma tem a saida boa (__hp80) e os controles (__ctrl_*)."""
    return [l.strip() for l in open(os.path.join(pasta, "bases_sinteticas.txt")) if l.strip()]


def pares_bons(pasta: str):
    """(referencia, saida) que um bom algoritmo produz: passa-altas 80 Hz sobre fala limpa + bypass (identidade)."""
    out = []
    for b in bases(pasta):
        out += [(b, b + "__hp80"), (b, b)]
    return out


def par_controle(pasta: str, gate: str):
    return [(b, f"{b}__{GATES[gate]['controle']}") for b in bases(pasta)]


def medir_corpus(perfil: str, pasta: str, nomes) -> dict:
    return {n: medir(perfil, os.path.join(pasta, n + ".wav")) for n in sorted(set(nomes))}
