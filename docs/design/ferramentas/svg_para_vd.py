#!/usr/bin/env python3
"""Converte os SVGs de docs/design/icone/ em Vector Drawables do Android (só <path>, sem traço nem gradiente).

  python3 svg_para_vd.py            # escreve
  python3 svg_para_vd.py --check    # confere sem escrever (sai com 1 se diferente)

Destinos (ver docs/design/icone/README.md):
  ic_launcher_{background,foreground,monochrome} -> app/src/main/res/drawable/
  ic_stat_*, ic_tile_*                           -> core/service/src/main/res/drawable/   (arquivos novos; a ligação é da Parte B)
"""
from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[3]
ORIGEM = RAIZ / "docs/design/icone"
DESTINOS = {
    "ic_launcher_background": RAIZ / "app/src/main/res/drawable",
    "ic_launcher_foreground": RAIZ / "app/src/main/res/drawable",
    "ic_launcher_monochrome": RAIZ / "app/src/main/res/drawable",
    "ic_stat_gravasom": RAIZ / "core/service/src/main/res/drawable",
    "ic_stat_gravando": RAIZ / "core/service/src/main/res/drawable",
    "ic_stat_pausado": RAIZ / "core/service/src/main/res/drawable",
    "ic_tile_gravar": RAIZ / "core/service/src/main/res/drawable",
    "ic_tile_gravando": RAIZ / "core/service/src/main/res/drawable",
}
NS = "{http://www.w3.org/2000/svg}"


def cor(hex_: str) -> str:
    return "#FF" + hex_.lstrip("#").upper()


def dimensao(valor: str) -> str:
    return str(int(float(valor)))


def converter(svg: Path) -> str:
    raiz = ET.parse(svg).getroot()
    largura, altura = dimensao(raiz.get("width", "24")), dimensao(raiz.get("height", "24"))
    _, _, vw, vh = (dimensao(v) for v in raiz.get("viewBox", f"0 0 {largura} {altura}").split())
    titulo = (raiz.findtext(f"{NS}title") or svg.stem).strip()
    linhas = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- Gerado por docs/design/ferramentas/svg_para_vd.py a partir de docs/design/icone/{svg.name}. {titulo}. -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{largura}dp"',
        f'    android:height="{altura}dp"',
        f'    android:viewportWidth="{vw}"',
        f'    android:viewportHeight="{vh}">',
    ]
    for caminho in raiz.findall(f"{NS}path"):
        preenchimento = caminho.get("fill")
        if not preenchimento or not preenchimento.startswith("#"):
            raise ValueError(f"{svg.name}: só fill #RRGGBB é aceito")
        linhas.append("    <path")
        linhas.append(f'        android:fillColor="{cor(preenchimento)}"')
        if caminho.get("fill-rule") == "evenodd":
            linhas.append('        android:fillType="evenOdd"')
        linhas.append(f'        android:pathData="{caminho.get("d")}" />')
    outros = [e.tag for e in raiz if e.tag not in (f"{NS}path", f"{NS}title")]
    if outros:
        raise ValueError(f"{svg.name}: elementos não suportados {outros}")
    linhas.append("</vector>")
    return "\n".join(linhas) + "\n"


def main(argv: list[str]) -> int:
    saidas = {pasta / f"{nome}.xml": converter(ORIGEM / f"{nome}.svg") for nome, pasta in DESTINOS.items()}
    if "--check" in argv:
        ruins = [str(p.relative_to(RAIZ)) for p, c in saidas.items() if not p.exists() or p.read_text(encoding="utf-8") != c]
        if ruins:
            print("desatualizados:", *ruins, sep="\n  ")
            return 1
        print(f"ok: {len(saidas)} ícones iguais ao gerado")
        return 0
    for p, c in saidas.items():
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(c, encoding="utf-8", newline="\n")
        print("escrito", p.relative_to(RAIZ))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
