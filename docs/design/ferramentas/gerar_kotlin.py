#!/usr/bin/env python3
"""Gera o código Kotlin do tema a partir de docs/design/tokens.json.

Saídas (não edite à mão; rode de novo e o resultado é idêntico byte a byte):

  core/ui/src/main/kotlin/com/gravasom/core/ui/theme/Color.kt
  core/ui/src/main/kotlin/com/gravasom/core/ui/theme/Type.kt
  core/ui/src/main/kotlin/com/gravasom/core/ui/theme/Shape.kt
  core/ui/src/main/kotlin/com/gravasom/core/ui/theme/Dimens.kt
  core/ui/src/main/kotlin/com/gravasom/core/ui/theme/Motion.kt
  core/ui/src/test/kotlin/com/gravasom/core/ui/theme/ParesContrasteGerado.kt   (pares de contraste do tokens.json)
  app/src/main/res/values/colors.xml e drawable{,-night}/gravasom_window_background.xml   (fundo da janela, claro e escuro)

Uso:
  python3 gerar_kotlin.py            # escreve os arquivos
  python3 gerar_kotlin.py --check    # não escreve; sai com 1 se algum arquivo estaria diferente

Só biblioteca padrão. Sem data, hora ou caminho absoluto nas saídas: o único "carimbo" é o SHA-256 do tokens.json,
que o teste ValidadeDosTokensTest confere contra o arquivo do repositório.
"""
from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[3]
TOKENS = RAIZ / "docs/design/tokens.json"
MAIN = RAIZ / "core/ui/src/main/kotlin/com/gravasom/core/ui/theme"
TESTE = RAIZ / "core/ui/src/test/kotlin/com/gravasom/core/ui/theme"
PACOTE = "com.gravasom.core.ui.theme"
PESOS_EMPACOTADOS = (400, 600)

MARCADORES = ("ambar", "verde", "azul", "roxo", "rosa", "grafite")
ESTADOS = ("gravando", "pausado", "pronto", "cofre")
CHAVES_ESTADO = ("base", "onBase", "container", "onContainer")
CHAVES_VU = ("trilho", "baixo", "ok", "clipando")
CHAVES_ONDA = ("fundo", "barra", "tocada", "cursor", "silencio", "selecao", "alca")
CHAVES_MARCADOR = ("fill", "onFill", "container", "onContainer")

CABECALHO = """\
// GERADO por docs/design/ferramentas/gerar_kotlin.py a partir de docs/design/tokens.json. NAO EDITE A MAO:
// mude o tokens.json (ou o script) e rode o gerador de novo.
// tokens.json sha256: {sha}
@file:Suppress("ktlint", "MaxLineLength")

package {pacote}
"""


def cabecalho(sha: str, pacote: str = PACOTE) -> str:
    return CABECALHO.format(sha=sha, pacote=pacote).rstrip("\n")


def hex_kt(valor: str) -> str:
    h = valor.lstrip("#").upper()
    if len(h) != 6:
        raise ValueError(f"cor opaca esperada (#RRGGBB): {valor}")
    return f"Color(0xFF{h})"


def num_kt(valor) -> str:
    """Número para `.sp`/`.dp`/`f` do Kotlin: inteiro sem ponto, decimal com ponto, negativo entre parênteses."""
    if isinstance(valor, bool) or not isinstance(valor, (int, float)):
        raise ValueError(f"número esperado: {valor!r}")
    texto = str(int(valor)) if float(valor).is_integer() else repr(float(valor))
    return f"({texto})" if texto.startswith("-") else texto


def float_kt(valor) -> str:
    texto = repr(float(valor))
    return texto + "f"


def camel(nome: str) -> str:
    return nome


# ------------------------------------------------------------------ Color.kt

def esquema_kt(nome: str, funcao: str, cores: dict) -> str:
    linhas = [f"val {nome}: ColorScheme = {funcao}("]
    linhas += [f"    {chave} = {hex_kt(valor)}," for chave, valor in cores.items()]
    linhas.append(")")
    return "\n".join(linhas)


def grupo_kt(tipo: str, dados: dict, chaves: tuple, recuo: str) -> str:
    faltando = set(chaves) - set(dados)
    if faltando:
        raise ValueError(f"{tipo}: faltam {sorted(faltando)}")
    campos = ", ".join(f"{c} = {hex_kt(dados[c])}" for c in chaves)
    return f"{recuo}{tipo}({campos})"


def extras_kt(nome: str, semantico: dict, marcador: dict, identidade: dict) -> str:
    """`identidade` traz de onde vêm pronto/cofre/onda; gravando, pausado e vu vêm sempre de `semantico`."""
    partes = [f"val {nome}: GravasomExtraColors = GravasomExtraColors("]
    for estado in ESTADOS:
        origem = identidade if estado in ("pronto", "cofre") else semantico
        partes.append(f"    {estado} = StateColors(")
        partes.append("        " + ", ".join(f"{c} = {hex_kt(origem[estado][c])}" for c in CHAVES_ESTADO))
        partes.append("    ),")
    partes.append("    vu = VuColors(" + ", ".join(f"{c} = {hex_kt(semantico['vu'][c])}" for c in CHAVES_VU) + "),")
    partes.append("    onda = WaveColors(")
    partes.append("        " + ", ".join(f"{c} = {hex_kt(identidade['onda'][c])}" for c in CHAVES_ONDA))
    partes.append("    ),")
    partes.append("    marker = MarkerSwatches(")
    for cor in MARCADORES:
        partes.append(
            f"        {cor} = MarkerSwatch("
            + ", ".join(f"{c} = {hex_kt(marcador[cor][c])}" for c in CHAVES_MARCADOR)
            + "),"
        )
    partes.append("    ),")
    partes.append(")")
    return "\n".join(partes)


def gerar_color(t: dict, sha: str) -> str:
    cor = t["color"]
    mix = cor["themes"]["mixlirous"]
    for modo in ("light", "dark"):
        if set(cor["scheme"][modo]) != set(mix["scheme"][modo]):
            raise ValueError(f"mixlirous.scheme.{modo} tem chaves diferentes de scheme.{modo}")
    mapa = cor["marker"]["mapeamentoDominio"]
    ordem = cor["marker"]["ordem"]
    if tuple(ordem) != MARCADORES:
        raise ValueError(f"ordem de marcadores mudou: {ordem}")
    out = [cabecalho(sha)]
    out.append(
        """\
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

const val TOKENS_SHA256 = "%s"

/** Quatro papéis de um estado (gravando, pausado, pronto, cofre): cor sólida, texto sobre ela, faixa e texto sobre a faixa. */
@Immutable
data class StateColors(val base: Color, val onBase: Color, val container: Color, val onContainer: Color)

/** Medidor de nível: trilho apagado e as três zonas (clipando é laranja, nunca vermelho). */
@Immutable
data class VuColors(val trilho: Color, val baixo: Color, val ok: Color, val clipando: Color)

/** Forma de onda do player. */
@Immutable
data class WaveColors(
    val fundo: Color,
    val barra: Color,
    val tocada: Color,
    val cursor: Color,
    val silencio: Color,
    val selecao: Color,
    val alca: Color,
)

/** Uma cor de marcador: linha e bandeira (fill), número sobre a bandeira (onFill) e chip da lista. */
@Immutable
data class MarkerSwatch(val fill: Color, val onFill: Color, val container: Color, val onContainer: Color)

/** As seis cores de marcador, na ordem fixa de `color.marker.ordem`. */
@Immutable
data class MarkerSwatches(
    val ambar: MarkerSwatch,
    val verde: MarkerSwatch,
    val azul: MarkerSwatch,
    val roxo: MarkerSwatch,
    val rosa: MarkerSwatch,
    val grafite: MarkerSwatch,
) {
    val ordem: List<MarkerSwatch> get() = listOf(ambar, verde, azul, roxo, rosa, grafite)

    /** Cor do marcador pelo nome do enum do domínio (`MarkerColor.name`); nome desconhecido cai em âmbar. */
    fun forDomainName(nome: String): MarkerSwatch = when (nome) {
%s
        else -> ambar
    }
}

/**
 * Cores que não pertencem ao esquema do Material: estados da gravação, medidor, onda e marcadores. Nunca mudam com a
 * cor dinâmica; só pronto, cofre e onda mudam entre as paletas Gravasom e Mixlirous.
 */
@Immutable
data class GravasomExtraColors(
    val gravando: StateColors,
    val pausado: StateColors,
    val pronto: StateColors,
    val cofre: StateColors,
    val vu: VuColors,
    val onda: WaveColors,
    val marker: MarkerSwatches,
)
"""
        % (
            sha,
            "\n".join(f'        "{dominio}" -> {cor}' for dominio, cor in mapa.items()),
        )
    )
    out.append("// ---- Paleta Gravasom (padrão): color.scheme + color.semantic")
    out.append(esquema_kt("GravasomLightColors", "lightColorScheme", cor["scheme"]["light"]))
    out.append(esquema_kt("GravasomDarkColors", "darkColorScheme", cor["scheme"]["dark"]))
    out.append(extras_kt("GravasomExtraLight", cor["semantic"]["light"], cor["marker"]["light"], cor["semantic"]["light"]))
    out.append(extras_kt("GravasomExtraDark", cor["semantic"]["dark"], cor["marker"]["dark"], cor["semantic"]["dark"]))
    out.append("// ---- Paleta Mixlirous: color.themes.mixlirous (pronto, cofre e onda); gravando, pausado, VU e marcadores não mudam")
    out.append(esquema_kt("MixlirousLightColors", "lightColorScheme", mix["scheme"]["light"]))
    out.append(esquema_kt("MixlirousDarkColors", "darkColorScheme", mix["scheme"]["dark"]))
    out.append(extras_kt("MixlirousExtraLight", cor["semantic"]["light"], cor["marker"]["light"], mix["semantic"]["light"]))
    out.append(extras_kt("MixlirousExtraDark", cor["semantic"]["dark"], cor["marker"]["dark"], mix["semantic"]["dark"]))
    out.append(
        "/** Cores extras do tema atual (definidas por [com.gravasom.core.ui.GravasomTheme]); o padrão serve a pré-visualizações. */\n"
        "val LocalGravasomExtraColors = staticCompositionLocalOf { GravasomExtraLight }"
    )
    return "\n\n".join(out) + "\n"


# ------------------------------------------------------------------ Type.kt

def estilo_kt(estilo: dict, recuo: str, familia: str = "GravasomFontFamily") -> str:
    peso = estilo["fontWeight"]
    if peso not in PESOS_EMPACOTADOS:
        raise ValueError(f"peso {peso} não está empacotado (só {PESOS_EMPACOTADOS})")
    linhas = [
        f"{recuo}fontFamily = {familia},",
        f"{recuo}fontWeight = FontWeight({peso}),",
        f"{recuo}fontSize = {num_kt(estilo['fontSize'])}.sp,",
        f"{recuo}lineHeight = {num_kt(estilo['lineHeight'])}.sp,",
        f"{recuo}letterSpacing = {num_kt(estilo['letterSpacing'])}.sp,",
    ]
    if "fontFeatureSettings" in estilo:
        linhas.append(f'{recuo}fontFeatureSettings = "{estilo["fontFeatureSettings"]}",')
    return "\n".join(linhas)


def gerar_type(t: dict, sha: str) -> str:
    tipo = t["typography"]
    familia = tipo["fontFamilies"]["texto"]
    arquivos = familia["arquivos"]
    if {a["weight"] for a in arquivos} != set(PESOS_EMPACOTADOS):
        raise ValueError("fontFamilies.texto.arquivos deve ter só os pesos 400 e 600")
    fontes = ",\n".join(f"    Font(R.font.{a['res']}, FontWeight({a['weight']}))" for a in arquivos)
    out = [cabecalho(sha)]
    out.append(
        f"""\
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gravasom.core.ui.R

/** {familia['nome']} ({familia['licenca']}), empacotada em res/font: o app não usa a internet nem o provedor de fontes. */
val GravasomFontFamily = FontFamily(
{fontes},
)

/** Pesos que existem de verdade no APK. Pedir outro faria o Android sintetizar (negrito falso). */
val GravasomPesosEmpacotados: Set<Int> = setOf({", ".join(str(p) for p in PESOS_EMPACOTADOS)})"""
    )
    escala = ["val GravasomTypography = Typography("]
    for nome, estilo in tipo["scale"].items():
        escala.append(f"    {nome} = TextStyle(")
        escala.append(estilo_kt(estilo, "        "))
        escala.append("    ),")
    escala.append(")")
    out.append("\n".join(escala))
    extra = [
        "/**",
        " * Estilos fora da escala do Material. `faixaEstado` pede texto em MAIÚSCULAS (textTransform = uppercase no token): o",
        " * Compose não tem essa propriedade, então quem usa chama `.uppercase()` no texto.",
        " */",
        "object GravasomTextStyles {",
    ]
    for nome, estilo in tipo["extra"].items():
        extra.append(f"    val {nome} = TextStyle(")
        extra.append(estilo_kt(estilo, "        "))
        extra.append("    )")
        extra.append("")
    extra.pop()
    extra.append("")
    extra.append("    val todos: List<TextStyle> get() = listOf(" + ", ".join(tipo["extra"].keys()) + ")")
    extra.append("}")
    out.append("\n".join(extra))
    return "\n\n".join(out) + "\n"


# ------------------------------------------------------------------ Shape.kt

def raio_kt(valor) -> str:
    return "CircleShape" if valor == "50%" else f"RoundedCornerShape({num_kt(valor)}.dp)"


def gerar_shape(t: dict, sha: str) -> str:
    forma = t["shape"]
    escala = forma["scale"]
    comp = {k: v for k, v in forma["component"].items() if not k.startswith("$")}
    out = [cabecalho(sha)]
    out.append(
        """\
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp"""
    )
    out.append(
        "val GravasomShapeScale = Shapes(\n"
        + "\n".join(f"    {k} = RoundedCornerShape({num_kt(v)}.dp)," for k, v in escala.items())
        + "\n)"
    )
    linhas = [
        "/** Formas por componente. `botaoGravar` e `botaoParar` são a mesma peça em dois instantes (ver [GravasomShapeRadii]). */",
        "object GravasomShapes {",
    ]
    for nome, valor in comp.items():
        if nome == "folhaInferior":
            linhas.append(
                f"    val {nome}: Shape = RoundedCornerShape(topStart = {num_kt(valor)}.dp, topEnd = {num_kt(valor)}.dp)"
            )
        else:
            linhas.append(f"    val {nome}: Shape = {raio_kt(valor)}")
    linhas.append("}")
    out.append("\n".join(linhas))
    raios = ["/** Raios em dp (os \"50%\" do tokens.json ficam de fora: são metade do lado), para animar a transição de forma. */",
             "object GravasomShapeRadii {"]
    raios += [f"    val {k} = {num_kt(v)}.dp" for k, v in comp.items() if isinstance(v, (int, float))]
    raios.append("}")
    out.append("\n".join(raios))
    return "\n\n".join(out) + "\n"


# ------------------------------------------------------------------ Dimens.kt

def gerar_dimens(t: dict, sha: str) -> str:
    out = [cabecalho(sha)]
    out.append("import androidx.compose.ui.unit.Dp\nimport androidx.compose.ui.unit.DpSize\nimport androidx.compose.ui.unit.dp")
    sp = [(k, v) for k, v in t["spacing"].items() if not k.startswith("$")]
    out.append("object GravasomSpacing {\n" + "\n".join(f"    val {k}: Dp = {num_kt(v)}.dp" for k, v in sp) + "\n}")
    linhas = ["object GravasomSizes {"]
    for k, v in t["size"].items():
        if k.startswith("$"):
            continue
        if isinstance(v, list):
            linhas.append(f"    val {k} = DpSize({num_kt(v[0])}.dp, {num_kt(v[1])}.dp)")
        elif k == "vuSegmentos":
            linhas.append(f"    const val {k}: Int = {num_kt(v)}")
        else:
            linhas.append(f"    val {k}: Dp = {num_kt(v)}.dp")
    linhas.append("}")
    out.append("\n".join(linhas))
    out.append("object GravasomElevation {\n" + "\n".join(f"    val {k}: Dp = {num_kt(v)}.dp" for k, v in t["elevation"].items()) + "\n}")
    return "\n\n".join(out) + "\n"


# ------------------------------------------------------------------ Motion.kt

def gerar_motion(t: dict, sha: str) -> str:
    mov = t["motion"]
    out = [cabecalho(sha)]
    out.append("import androidx.compose.animation.core.CubicBezierEasing\nimport androidx.compose.animation.core.Easing")
    out.append("object GravasomDuration {\n" + "\n".join(f"    const val {k}: Int = {v}" for k, v in mov["duration"].items()) + "\n}")
    curvas = []
    for k, v in mov["easing"].items():
        curvas.append(f"    val {k}: Easing = CubicBezierEasing({', '.join(float_kt(x) for x in v)})")
    out.append("object GravasomEasing {\n" + "\n".join(curvas) + "\n}")
    out.append("/** Duração e curva de uma animação nomeada do tokens.json. */\ndata class MotionSpec(val durationMillis: Int, val easing: Easing)")
    sem = [
        "/** Animações nomeadas. Com \"Remover animações\" ligado (escala 0) a tela não usa pulso nem piscar: o estado segue dito por texto e ícone. */",
        "object GravasomMotion {",
    ]
    for nome, dados in mov["semantic"].items():
        if not isinstance(dados, dict):
            continue
        easing = dados.get("easing", "linear")
        sem.append(f"    val {nome} = MotionSpec({dados['duration']}, GravasomEasing.{easing})")
    sem.append("")
    sem.append("    // Parâmetros numéricos de algumas animações")
    for nome, dados in mov["semantic"].items():
        if not isinstance(dados, dict):
            continue
        for chave, valor in dados.items():
            if isinstance(valor, (int, float)) and chave not in ("duration",):
                sem.append(f"    const val {nome}{chave[0].upper()}{chave[1:]}: Float = {float_kt(valor)}")
    sem.append("}")
    out.append("\n".join(sem))
    return "\n\n".join(out) + "\n"


# ------------------------------------------------------------------ ParesContrasteGerado.kt (teste)

def acessor(caminho: tuple[str, ...]) -> str:
    if caminho[0] == "scheme":
        return "s." + ".".join(caminho[1:])
    return "e." + ".".join(caminho)


def resolver(nome: str, esquema: dict, semantico: dict, marcador: dict) -> tuple[tuple[str, ...], tuple[str, ...]]:
    """'onPrimary / primary', 'semantic.gravando.onBase / base', 'marker.ambar.fill / onda.fundo'... -> caminhos."""
    sem_nota = re.sub(r"\s*\([^)]*\)", "", nome)
    esq, dir_ = (p.strip() for p in sem_nota.split("/"))

    def caminho(lado: str, contexto: tuple[str, ...] | None) -> tuple[str, ...]:
        partes = lado.split(".")
        if partes[0] == "semantic":
            return tuple(partes[1:])
        if partes[0] == "marker":
            return ("marker", *partes[1:])
        if len(partes) == 2 and partes[0] in ("vu", "onda"):
            return tuple(partes)
        if len(partes) == 1 and contexto is not None and partes[0] in (CHAVES_ESTADO + CHAVES_MARCADOR):
            return (*contexto, partes[0])
        if len(partes) == 1 and partes[0] in esquema:
            return ("scheme", partes[0])
        raise ValueError(f"não sei resolver '{lado}' em '{nome}'")

    a = caminho(esq, None)
    contexto = a[:-1] if a[0] != "scheme" and len(a) >= 2 else None
    b = caminho(dir_, contexto)
    return a, b


def gerar_pares(t: dict, sha: str) -> str:
    cor = t["color"]
    esquema = cor["scheme"]["light"]
    por_modo: dict[str, list] = {}
    for p in t["contrast"]["pares"]:
        por_modo.setdefault(p["mode"], []).append(p)
    out = [cabecalho(sha, PACOTE)]
    out.append("import androidx.compose.material3.ColorScheme\nimport androidx.compose.ui.graphics.Color")
    out.append(
        "/** Um par texto/fundo (ou ícone/fundo) do tokens.json: [minimo] é 4,5 para texto e 3 para ícone, traço e borda. */\n"
        "class ParDeContraste(\n"
        "    val nome: String,\n"
        "    val minimo: Double,\n"
        "    val primeiroPlano: (ColorScheme, GravasomExtraColors) -> Color,\n"
        "    val fundo: (ColorScheme, GravasomExtraColors) -> Color,\n"
        ")"
    )
    corpo = ["/** Pares de contraste por modo do tokens.json (`light`, `dark`, `mixlirous-light`, `mixlirous-dark`, `icone`). */",
             "object ParesContrasteGerado {",
             "    val porModo: Map<String, List<ParDeContraste>> = mapOf("]
    for modo, pares in por_modo.items():
        corpo.append(f'        "{modo}" to listOf(')
        for p in pares:
            if modo == "icone":
                corpo.append(
                    f'            ParDeContraste("{p["par"]}", {float(p["min"])!r}, '
                    f'{{ _, _ -> {hex_kt(p["fg"])} }}, {{ _, _ -> {hex_kt(p["bg"])} }}),'
                )
                continue
            a, b = resolver(p["par"], esquema, cor["semantic"]["light"], cor["marker"]["light"])
            corpo.append(
                f'            ParDeContraste("{p["par"]}", {float(p["min"])!r}, '
                f'{{ s, e -> {acessor(a)} }}, {{ s, e -> {acessor(b)} }}),'
            )
        corpo.append("        ),")
    corpo.append("    )")
    corpo.append("}")
    out.append("\n".join(corpo))
    texto = "\n\n".join(out) + "\n"
    # Lambdas que só usam um dos parâmetros: troca o não usado por _ (exigência do compilador com warningsAsErrors).
    def limpar(m: re.Match) -> str:
        corpo_l = m.group(1)
        s = "s" if re.search(r"\bs\.", corpo_l) else "_"
        e = "e" if re.search(r"\be\.", corpo_l) else "_"
        return f"{{ {s}, {e} -> {corpo_l} }}"

    return re.sub(r"\{ s, e -> ([^{}]+?) \}", limpar, texto)


RES_APP = RAIZ / "app/src/main/res"


def gerar_recursos_app(t: dict, sha: str) -> dict[Path, str]:
    """Cor de fundo da janela (evita o clarão branco/preto antes do Compose desenhar) em claro e escuro."""
    claro = t["color"]["scheme"]["light"]["surface"].upper()
    escuro = t["color"]["scheme"]["dark"]["surface"].upper()
    nota = f"<!-- Gerado por docs/design/ferramentas/gerar_kotlin.py (tokens.json sha256 {sha}). Nao edite a mao. -->"
    colors = (
        '<?xml version="1.0" encoding="utf-8"?>\n' + nota + "\n<resources>\n"
        f'    <color name="gravasom_surface_light">{claro}</color>\n'
        f'    <color name="gravasom_surface_dark">{escuro}</color>\n'
        "</resources>\n"
    )

    def janela(cor_nome: str) -> str:
        return (
            '<?xml version="1.0" encoding="utf-8"?>\n' + nota + "\n"
            '<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n'
            f'    <solid android:color="@color/{cor_nome}" />\n</shape>\n'
        )

    return {
        RES_APP / "values/colors.xml": colors,
        RES_APP / "drawable/gravasom_window_background.xml": janela("gravasom_surface_light"),
        RES_APP / "drawable-night/gravasom_window_background.xml": janela("gravasom_surface_dark"),
    }


def principais() -> dict[Path, str]:
    bruto = TOKENS.read_bytes()
    sha = hashlib.sha256(bruto).hexdigest()
    t = json.loads(bruto.decode("utf-8"))
    return {
        **gerar_recursos_app(t, sha),
        MAIN / "Color.kt": gerar_color(t, sha),
        MAIN / "Type.kt": gerar_type(t, sha),
        MAIN / "Shape.kt": gerar_shape(t, sha),
        MAIN / "Dimens.kt": gerar_dimens(t, sha),
        MAIN / "Motion.kt": gerar_motion(t, sha),
        TESTE / "ParesContrasteGerado.kt": gerar_pares(t, sha),
    }


def main(argv: list[str]) -> int:
    saidas = principais()
    if "--check" in argv:
        diferentes = [str(p.relative_to(RAIZ)) for p, c in saidas.items() if not p.exists() or p.read_text(encoding="utf-8") != c]
        if diferentes:
            print("desatualizados:", *diferentes, sep="\n  ")
            return 1
        print(f"ok: {len(saidas)} arquivos iguais ao gerado")
        return 0
    for caminho, conteudo in saidas.items():
        caminho.parent.mkdir(parents=True, exist_ok=True)
        caminho.write_text(conteudo, encoding="utf-8", newline="\n")
        print("escrito", caminho.relative_to(RAIZ))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
