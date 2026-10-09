"""Gera os SVGs do ícone do Gravasom (anel = botão de gravar; três barras = voz)."""
import json, sys, os

out = sys.argv[1]
icon = json.load(open(os.path.join(os.path.dirname(__file__), 'icon.json')))
BG, FG, ACC = icon['background'], icon['foreground'], icon['foregroundAccent']


def ring(cx, cy, ro, ri):
    return (f"M{cx} {cy - ro}a{ro} {ro} 0 1 0 0 {2 * ro}a{ro} {ro} 0 1 0 0 {-2 * ro}z"
            f"M{cx} {cy - ri}a{ri} {ri} 0 1 1 0 {2 * ri}a{ri} {ri} 0 1 1 0 {-2 * ri}z")


def disc(cx, cy, r):
    return f"M{cx} {cy - r}a{r} {r} 0 1 0 0 {2 * r}a{r} {r} 0 1 0 0 {-2 * r}z"


def bar(cx, cy, w, h):
    """Barra vertical com pontas redondas, centrada em (cx, cy)."""
    r = w / 2
    x, y = cx - r, cy - h / 2
    g = lambda v: f"{v:g}"
    return (f"M{g(x)} {g(y + r)}a{g(r)} {g(r)} 0 0 1 {g(w)} 0v{g(h - w)}"
            f"a{g(r)} {g(r)} 0 0 1 {-w:g} 0z")


def rrect(cx, cy, s, rad):
    x, y = cx - s / 2, cy - s / 2
    g = lambda v: f"{v:g}"
    return (f"M{g(x + rad)} {g(y)}h{g(s - 2 * rad)}a{g(rad)} {g(rad)} 0 0 1 {g(rad)} {g(rad)}v{g(s - 2 * rad)}"
            f"a{g(rad)} {g(rad)} 0 0 1 {-rad:g} {g(rad)}h{-(s - 2 * rad):g}a{g(rad)} {g(rad)} 0 0 1 {-rad:g} {-rad:g}"
            f"v{-(s - 2 * rad):g}a{g(rad)} {g(rad)} 0 0 1 {g(rad)} {-rad:g}z")


def svg(size, body, title):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 {size} {size}">\n'
            f'  <title>{title}</title>\n{body}</svg>\n')


def path(d, fill, evenodd=False):
    fr = ' fill-rule="evenodd"' if evenodd else ''
    return f'  <path fill="{fill}"{fr} d="{d}"/>\n'


# ---- Ícone adaptativo: 108 x 108 dp; zona segura = círculo de raio 33 no centro (54, 54)
C = 54
RING = ring(C, C, 30, 24)
BARS = [bar(44.5, C, 6, 14), bar(C, C, 6, 28), bar(63.5, C, 6, 18)]

files = {
    'ic_launcher_background.svg': svg(108, path('M0 0h108v108H0z', BG), 'Gravasom — fundo do ícone adaptativo'),
    'ic_launcher_foreground.svg': svg(108, path(RING, FG, True) + ''.join(path(b, ACC) for b in BARS),
                                      'Gravasom — primeiro plano do ícone adaptativo'),
    'ic_launcher_monochrome.svg': svg(108, path(RING, '#000000', True) + ''.join(path(b, '#000000') for b in BARS),
                                      'Gravasom — camada monocromática (o sistema aplica a cor)'),
}

# ---- Silhuetas de 24 x 24 dp (só branco + transparência, como exige a barra de status)
W = '#FFFFFF'
c = 12
b24 = [bar(8.75, c, 2, 5), bar(c, c, 2, 10), bar(15.25, c, 2, 6.5)]
files['ic_stat_gravasom.svg'] = svg(24, path(ring(c, c, 10, 8), W, True) + ''.join(path(b, W) for b in b24),
                                    'Notificação — marca (reprodução e avisos)')
# Gravando: disco CHEIO com as barras vazadas (silhueta "acesa", diferente do anel vazado)
files['ic_stat_gravando.svg'] = svg(24, path(disc(c, c, 10) + ''.join(b24), W, True),
                                    'Notificação — gravando (disco cheio)')
# Pausado: anel com duas barras de pausa
files['ic_stat_pausado.svg'] = svg(24, path(ring(c, c, 10, 8), W, True) + path(bar(9.5, c, 2.5, 9), W) + path(bar(14.5, c, 2.5, 9), W),
                                   'Notificação — pausado (anel + pausa)')
# Tile: parado = anel + ponto (toque para gravar); gravando = disco cheio com quadrado vazado (toque para parar)
files['ic_tile_gravar.svg'] = svg(24, path(ring(c, c, 10, 8), W, True) + path(disc(c, c, 4.5), W), 'Bloco — gravar')
files['ic_tile_gravando.svg'] = svg(24, path(disc(c, c, 10) + rrect(c, c, 8, 1.5), W, True), 'Bloco — gravando (toque para parar)')

for name, content in files.items():
    with open(os.path.join(out, name), 'w') as f:
        f.write(content)
print('\n'.join(files))
