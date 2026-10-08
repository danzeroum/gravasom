// Gera os papéis de cor M3 + tokens semânticos do Gravasom e calcula contraste WCAG.
import {
  Hct, TonalPalette, DynamicScheme, MaterialDynamicColors, hexFromArgb, argbFromHex,
} from '@material/material-color-utilities';
// O pacote não exporta Variant pelo índice; importa pelo caminho do arquivo.
import * as V from './node_modules/@material/material-color-utilities/dynamiccolor/variant.js'; const Variant = V.Variant;
import fs from 'node:fs';

const SEED = '#1E6A78'; // petróleo
const seedHct = Hct.fromInt(argbFromHex(SEED));
const H = seedHct.hue;

const pal = {
  primary: TonalPalette.fromHueAndChroma(H, 40),
  secondary: TonalPalette.fromHueAndChroma(H, 14),
  tertiary: TonalPalette.fromHueAndChroma(330, 30), // ameixa = cofre
  neutral: TonalPalette.fromHueAndChroma(H, 5),
  neutralVariant: TonalPalette.fromHueAndChroma(H, 9),
};

function scheme(isDark) {
  return new DynamicScheme({
    sourceColorArgb: argbFromHex(SEED), variant: Variant.TONAL_SPOT, contrastLevel: 0, isDark,
    primaryPalette: pal.primary, secondaryPalette: pal.secondary, tertiaryPalette: pal.tertiary,
    neutralPalette: pal.neutral, neutralVariantPalette: pal.neutralVariant,
  });
}

// Nomes exatamente como os parâmetros de lightColorScheme()/darkColorScheme() do Compose Material3 1.3.x
const ROLES = {
  primary: 'primary', onPrimary: 'onPrimary', primaryContainer: 'primaryContainer', onPrimaryContainer: 'onPrimaryContainer',
  inversePrimary: 'inversePrimary',
  secondary: 'secondary', onSecondary: 'onSecondary', secondaryContainer: 'secondaryContainer', onSecondaryContainer: 'onSecondaryContainer',
  tertiary: 'tertiary', onTertiary: 'onTertiary', tertiaryContainer: 'tertiaryContainer', onTertiaryContainer: 'onTertiaryContainer',
  background: 'background', onBackground: 'onBackground',
  surface: 'surface', onSurface: 'onSurface', surfaceVariant: 'surfaceVariant', onSurfaceVariant: 'onSurfaceVariant',
  surfaceTint: 'surfaceTint', inverseSurface: 'inverseSurface', inverseOnSurface: 'inverseOnSurface',
  error: 'error', onError: 'onError', errorContainer: 'errorContainer', onErrorContainer: 'onErrorContainer',
  outline: 'outline', outlineVariant: 'outlineVariant', scrim: 'scrim',
  surfaceBright: 'surfaceBright', surfaceDim: 'surfaceDim',
  surfaceContainerLowest: 'surfaceContainerLowest', surfaceContainerLow: 'surfaceContainerLow',
  surfaceContainer: 'surfaceContainer', surfaceContainerHigh: 'surfaceContainerHigh', surfaceContainerHighest: 'surfaceContainerHighest',
};

const out = { light: {}, dark: {} };
for (const mode of ['light', 'dark']) {
  const s = scheme(mode === 'dark');
  for (const [k, mdc] of Object.entries(ROLES)) {
    out[mode][k] = hexFromArgb(MaterialDynamicColors[mdc].getArgb(s)).toUpperCase();
  }
}
// error = vermelho de "destrutivo" (M3 baseline), mantido
// ---- WCAG
function lum(hex) {
  const n = parseInt(hex.slice(1), 16);
  const c = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map(v => { v /= 255; return v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4; });
  return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
}
export function ratio(a, b) { const [x, y] = [lum(a), lum(b)].sort((p, q) => q - p); return (x + 0.05) / (y + 0.05); }

const tone = (hue, chroma, t) => hexFromArgb(TonalPalette.fromHueAndChroma(hue, chroma).tone(t)).toUpperCase();

// ---- Semânticos. Convenção por token: base (ícone/traço sobre surface, >=3:1), onBase (texto sobre base, >=4.5:1),
// container (faixa/cartão), onContainer (texto sobre container, >=4.5:1)
const RED = { h: 25, c: 84 };      // gravando
const AMBER = { h: 75, c: 60 };    // pausado
const sem = {
  gravando: { hue: RED.h, chroma: RED.c, light: { base: 40, onBase: 100, container: 40, onContainer: 100 }, dark: { base: 70, onBase: 10, container: 45, onContainer: 100 } },
  pausado: { hue: AMBER.h, chroma: AMBER.c, light: { base: 45, onBase: 100, container: 85, onContainer: 10 }, dark: { base: 80, onBase: 10, container: 88, onContainer: 10 } },
};
const semantic = { light: {}, dark: {} };
for (const [name, d] of Object.entries(sem)) for (const mode of ['light', 'dark']) {
  semantic[mode][name] = Object.fromEntries(Object.entries(d[mode]).map(([k, t]) => [k, tone(d.hue, d.chroma, t)]));
}
// Pronto (botão Gravar parado): luminância OPOSTA à do vermelho de gravando, para a troca de estado ser visível sem cor
for (const mode of ['light', 'dark']) {
  const o = out[mode];
  semantic[mode].pronto = mode === 'light'
    ? { base: o.primary, onBase: o.onPrimary, container: o.primaryContainer, onContainer: o.onPrimaryContainer }
    : { base: o.primary, onBase: o.onPrimary, container: hexFromArgb(pal.primary.tone(90)).toUpperCase(), onContainer: hexFromArgb(pal.primary.tone(10)).toUpperCase() };
}
// Cofre = papéis tertiary (aliases)
for (const mode of ['light', 'dark']) {
  const o = out[mode];
  semantic[mode].cofre = { base: o.tertiary, onBase: o.onTertiary, container: o.tertiaryContainer, onContainer: o.onTertiaryContainer };
}
// VU: baixo (neutro), ok (petróleo/verde-azulado), clipando (laranja); "trilho" = fundo vazio do medidor
const vuDef = {
  baixo: { h: H, c: 8, light: 50, dark: 65 },
  ok: { h: 165, c: 48, light: 45, dark: 75 },
  clipando: { h: 50, c: 80, light: 50, dark: 72 },
};
for (const mode of ['light', 'dark']) {
  semantic[mode].vu = { trilho: out[mode].surfaceContainerHighest };
  for (const [k, d] of Object.entries(vuDef)) semantic[mode].vu[k] = tone(d.h, d.c, d[mode]);
}
// Marcadores: 6 matizes, sem vermelho. fill = traço/bandeira na onda; container/onContainer = chip numerado
const mk = {
  ambar: { h: 80, c: 60 }, verde: { h: 145, c: 50 }, azul: { h: 255, c: 50 },
  roxo: { h: 300, c: 50 }, rosa: { h: 350, c: 60 }, grafite: { h: H, c: 4 },
};
const marker = { light: {}, dark: {} };
for (const [k, d] of Object.entries(mk)) {
  marker.light[k] = { fill: tone(d.h, d.c, 45), onFill: tone(d.h, d.c, 100), container: tone(d.h, d.c, 90), onContainer: tone(d.h, d.c, 10) };
  marker.dark[k] = { fill: tone(d.h, d.c, 75), onFill: tone(d.h, d.c, 10), container: tone(d.h, d.c, 30), onContainer: tone(d.h, d.c, 90) };
}
// Onda
for (const mode of ['light', 'dark']) {
  const o = out[mode];
  semantic[mode].onda = {
    fundo: o.surfaceContainerLow, barra: o.outline, tocada: o.primary, cursor: o.onSurface,
    silencio: o.outlineVariant, selecao: o.secondaryContainer, alca: o.secondary,
  };
}

const json = { scheme: out, semantic, marker };
fs.writeFileSync('colors.json', JSON.stringify(json, null, 2));

// ---- Pares a verificar
const pairs = [];
const P = (mode, name, fg, bg, min) => pairs.push({ mode, par: name, fg, bg, ratio: +ratio(fg, bg).toFixed(2), min, pass: ratio(fg, bg) >= min });
for (const mode of ['light', 'dark']) {
  const o = out[mode], s = semantic[mode];
  const T = [['onPrimary', 'primary'], ['onPrimaryContainer', 'primaryContainer'], ['onSecondary', 'secondary'], ['onSecondaryContainer', 'secondaryContainer'],
    ['onTertiary', 'tertiary'], ['onTertiaryContainer', 'tertiaryContainer'], ['onBackground', 'background'], ['onSurface', 'surface'],
    ['onSurfaceVariant', 'surfaceVariant'], ['onSurfaceVariant', 'surface'], ['onError', 'error'], ['onErrorContainer', 'errorContainer'],
    ['inverseOnSurface', 'inverseSurface'], ['inversePrimary', 'inverseSurface'], ['primary', 'surface'], ['error', 'surface'], ['tertiary', 'surface'], ['secondary', 'surface']];
  for (const sc of ['surfaceContainerLowest', 'surfaceContainerLow', 'surfaceContainer', 'surfaceContainerHigh', 'surfaceContainerHighest', 'surfaceDim', 'surfaceBright']) {
    T.push(['onSurface', sc]); T.push(['onSurfaceVariant', sc]); T.push(['primary', sc]);
  }
  for (const [f, b] of T) P(mode, `${f} / ${b}`, o[f], o[b], 4.5);
  for (const [f, b] of [['outline', 'surface'], ['outline', 'surfaceContainerHighest']]) P(mode, `${f} / ${b} (borda)`, o[f], o[b], 3);
  for (const n of ['gravando', 'pausado', 'cofre', 'pronto']) {
    P(mode, `semantic.${n}.onBase / base`, s[n].onBase, s[n].base, 4.5);
    P(mode, `semantic.${n}.onContainer / container`, s[n].onContainer, s[n].container, 4.5);
    P(mode, `semantic.${n}.base / surface (ícone)`, s[n].base, o.surface, 3);
    P(mode, `semantic.${n}.container / surface (faixa)`, s[n].container, o.surface, n === 'gravando' ? 3 : 1);
  }
  for (const k of ['baixo', 'ok', 'clipando']) { P(mode, `semantic.vu.${k} / vu.trilho (segmento)`, s.vu[k], s.vu.trilho, 3); P(mode, `semantic.vu.${k} / surface (segmento)`, s.vu[k], o.surface, 3); }
  P(mode, 'semantic.onda.barra / onda.fundo', s.onda.barra, s.onda.fundo, 3);
  P(mode, 'semantic.onda.tocada / onda.fundo', s.onda.tocada, s.onda.fundo, 3);
  P(mode, 'semantic.onda.cursor / onda.fundo', s.onda.cursor, s.onda.fundo, 3);
  P(mode, 'semantic.onda.alca / onda.fundo', s.onda.alca, s.onda.fundo, 3);
  for (const [k, m] of Object.entries(marker[mode])) {
    P(mode, `marker.${k}.fill / onda.fundo`, m.fill, s.onda.fundo, 3);
    P(mode, `marker.${k}.onFill / fill`, m.onFill, m.fill, 4.5);
    P(mode, `marker.${k}.onContainer / container`, m.onContainer, m.container, 4.5);
  }
}
fs.writeFileSync('pairs.json', JSON.stringify(pairs, null, 2));
const fails = pairs.filter(p => !p.pass);
console.log('pares:', pairs.length, 'falhas:', fails.length);
for (const f of fails) console.log(f.mode, f.par, f.fg, f.bg, f.ratio, '<', f.min);
console.log(JSON.stringify(out.light.primary), out.dark.primary, out.light.surface, out.dark.surface);
console.log(JSON.stringify(semantic.light.gravando), JSON.stringify(semantic.dark.gravando), JSON.stringify(semantic.light.pausado), JSON.stringify(semantic.dark.pausado));

// ---- Simulação de daltonismo (Machado et al. 2009, severidade 1.0) + acromatopsia; compara estados por luminância
const M = {
  protanopia: [[0.152286, 1.052583, -0.204868], [0.114503, 0.786281, 0.099216], [-0.003882, -0.048116, 1.051998]],
  deuteranopia: [[0.367322, 0.860646, -0.227968], [0.280085, 0.672501, 0.047413], [-0.011820, 0.042940, 0.968881]],
  tritanopia: [[1.255528, -0.076749, -0.178779], [-0.078411, 0.930809, 0.147602], [0.004733, 0.691367, 0.303900]],
};
const toLin = v => { v /= 255; return v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4; };
const toSrgb = v => { v = Math.min(1, Math.max(0, v)); v = v <= 0.0031308 ? v * 12.92 : 1.055 * v ** (1 / 2.4) - 0.055; return Math.round(v * 255); };
function sim(hex, kind) {
  const n = parseInt(hex.slice(1), 16); const rgb = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map(toLin);
  if (kind === 'acromatopsia') { const y = 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]; const g = toSrgb(y); return '#' + [g, g, g].map(x => x.toString(16).padStart(2, '0')).join('').toUpperCase(); }
  const m = M[kind]; const o = m.map(r => r[0] * rgb[0] + r[1] * rgb[1] + r[2] * rgb[2]).map(toSrgb);
  return '#' + o.map(x => x.toString(16).padStart(2, '0')).join('').toUpperCase();
}
const cvd = [];
for (const mode of ['light', 'dark']) {
  const s = semantic[mode], o = out[mode];
  const comps = [
    ['gravando.container × pausado.container', s.gravando.container, s.pausado.container],
    ['gravando.container × surface', s.gravando.container, o.surface],
    ['gravando.container × pronto.container (botão Gravar parado)', s.gravando.container, s.pronto.container],
  ];
  for (const kind of ['normal', 'protanopia', 'deuteranopia', 'tritanopia', 'acromatopsia']) for (const [name, a, b] of comps) {
    const A = kind === 'normal' ? a : sim(a, kind), B = kind === 'normal' ? b : sim(b, kind);
    { const r = +ratio(A, B).toFixed(2); const min = name.includes('surface') ? null : 3; cvd.push({ mode, visao: kind, comparacao: name, a: A, b: B, ratio: r, min, pass: min === null ? null : r >= min }); }
  }
}
fs.writeFileSync('cvd.json', JSON.stringify(cvd, null, 2));
for (const c of cvd) console.log(c.mode, c.visao.padEnd(13), c.comparacao.padEnd(48), c.ratio);

// ---- Ícone (cores da marca, fora do esquema dinâmico)
const icon = {
  background: hexFromArgb(pal.primary.tone(30)).toUpperCase(),
  foreground: hexFromArgb(pal.primary.tone(95)).toUpperCase(),
  foregroundAccent: hexFromArgb(pal.primary.tone(80)).toUpperCase(),
};
pairs.push({ mode: 'icone', par: 'icon.foreground / icon.background', fg: icon.foreground, bg: icon.background, ratio: +ratio(icon.foreground, icon.background).toFixed(2), min: 3, pass: ratio(icon.foreground, icon.background) >= 3 });
pairs.push({ mode: 'icone', par: 'icon.foregroundAccent / icon.background', fg: icon.foregroundAccent, bg: icon.background, ratio: +ratio(icon.foregroundAccent, icon.background).toFixed(2), min: 3, pass: ratio(icon.foregroundAccent, icon.background) >= 3 });
fs.writeFileSync('icon.json', JSON.stringify(icon));
fs.writeFileSync('pairs.json', JSON.stringify(pairs, null, 2));
console.log('icone', icon, pairs.slice(-2).map(p => p.ratio));
