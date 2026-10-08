"""WER dos JSON medidos no celular (spike/whisper/resultados/*.json) contra a referencia da gravacao de 10 min (ADR-013).

Uso: asr_wer.py <dir_resultados> <lista_asr.txt> [dir_pt]   (dir_pt: onde corpus-pt.sh deixou os WAV/TSV; padrao tests-out/pt)
"""
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, os.path.dirname(__file__))
import jiwer  # noqa: E402
from asr_corpus import montar, normalizar  # noqa: E402


def main():
    resultados, lista = Path(sys.argv[1]), Path(sys.argv[2])
    pt_dir = Path(sys.argv[3]) if len(sys.argv) > 3 else Path("tests-out/pt")
    _, ref, usados = montar(pt_dir, lista)
    print(f"referencia: {len(usados)} locucoes, {len(ref.split())} palavras")
    for arq in sorted(resultados.glob("*.json")):
        d = json.loads(arq.read_text(encoding="utf-8"))
        if "texto" not in d:
            continue
        wer = jiwer.wer(ref, normalizar(d["texto"]))
        print(f"{arq.stem}: WER={wer:.3f} RTF={d.get('fator_tempo_real')} gasto={d.get('gasto_uAh')} uAh temp_max={d.get('temp_c_max')} C carregando={d.get('carregando')}")


if __name__ == "__main__":
    main()
