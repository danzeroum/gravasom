// Tema opcional "Mixlirous": paleta tirada das fotos de estúdio de referência (painéis roxos, madeira, brilho ciano dos cabos).
// Só troca color.scheme e os semânticos de IDENTIDADE (pronto, cofre, onda). Gravando, pausado, VU e marcadores ficam fixos.
// Rodar depois de gen.mjs (lê colors.json para os semânticos fixos). Escreve mixlirous.json, pairs_mixlirous.json, cvd_mixlirous.json.
import {
  Hct, TonalPalette, DynamicScheme, MaterialDynamicColors, hexFromArgb, argbFromHex,
} from '@material/material-color-utilities';
import * as V from './node_modules/@material/material-color-utilities/dynamiccolor/variant.js'; const Variant = V.Variant;
import fs from 'node:fs';

// Amostras medianas das fotos (pixels com saturação > 0,35), em sRGB:
const AMOSTRAS = {
  roxoPainel: '#562B64',   // foto 2, painéis acústicos roxos (matiz 285–300°)
  madeira: '#C1935E',      // foto 1, difusor de madeira e luz quente (matiz 30–45°)
  cianoCabo: '#41A09A',    // fotos 1 e 2, brilho dos cabos (matiz 165–210°)
};
const hue = hex => Hct.fromInt(argbFromHex(hex)).hue;
const H = { roxo: hue(AMOSTRAS.roxoPainel), madeira: hue(AMOSTRAS.madeira), ciano: hue(AMOSTRAS.cianoCabo) };

const pal = {
  primary: TonalPalette.fromHueAndChroma(H.roxo, 48),
  secondary: TonalPalette.fromHueAndChroma(H.roxo, 16),
  tertiary: TonalPalette.fromHueAndChroma(H.ciano, 34), // ciano = cofre neste tema
  // Neutros quentes de madeira: superfícies creme no claro e carvão-marrom no escuro. Madeira NÃO vai para secondary porque
  // secondaryContainer pinta o botão Pausar e ficaria parecido com o âmbar de "pausado".
  neutral: TonalPalette.fromHueAndChroma(H.madeira, 6),
  neutralVariant: TonalPalette.fromHueAndChroma(H.madeira, 10),
};
const SEED = AMOSTRAS.roxoPainel;
const scheme = isDark => new DynamicScheme({
  sourceColorArgb: argbFromHex(SEED), variant: Variant.TONAL_SPOT, contrastLevel: 0, isDark,
  primaryPalette: pal.primary, secondaryPalette: pal.secondary, tertiaryPalette: pal.tertiary,
  neutralPalette: pal.neutral, neutralVariantPalette: pal.neutralVariant,
});

const base = JSON.parse(fs.readFileSync('colors.json'));
const ROLES = Object.keys(base.scheme.light);
const hx = a => hexFromArgb(a).toUpperCase();
const out = { light: {}, dark: {} };
for (const mode of ['light', 'dark']) {
  const s = scheme(mode === 'dark');
  for (const k of ROLES) out[mode][k] = hx(MaterialDynamicColors[k].getArgb(s));
}

// Semânticos de identidade (variam com o tema)
const semantic = { light: {}, dark: {} };
for (const mode of ['light', 'dark']) {
  const o = out[mode];
  semantic[mode].pronto = mode === 'light'
    ? { base: o.primary, onBase: o.onPrimary, container: o.primaryContainer, onContainer: o.onPrimaryContainer }
    : { base: o.primary, onBase: o.onPrimary, container: hx(pal.primary.tone(90)), onContainer: hx(pal.primary.tone(10)) };
  semantic[mode].cofre = { base: o.tertiary, onBase: o.onTertiary, container: o.tertiaryContainer, onContainer: o.onTertiaryContainer };
  semantic[mode].onda = {
    fundo: o.surfaceContainerLow, barra: o.outline, tocada: o.primary, cursor: o.onSurface,
    silencio: o.outlineVariant, selecao: o.secondaryContainer, alca: o.secondary,
  };
}

// ---- Contraste
function lum(hex) {
  const n = parseInt(hex.slice(1), 16);
  const c = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map(v => { v /= 255; return v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4; });
  return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
}
const ratio = (a, b) => { const [x, y] = [lum(a), lum(b)].sort((p, q) => q - p); return (x + 0.05) / (y + 0.05); };
const pairs = [];
const P = (mode, par, fg, bg, min) => { const r = ratio(fg, bg); pairs.push({ mode: 'mixlirous-' + mode, par, fg, bg, ratio: +r.toFixed(2), min, pass: r >= min }); };
for (const mode of ['light', 'dark']) {
  const o = out[mode], s = semantic[mode], fx = base.semantic[mode], mk = base.marker[mode];
  const T = [['onPrimary', 'primary'], ['onPrimaryContainer', 'primaryContainer'], ['onSecondary', 'secondary'], ['onSecondaryContainer', 'secondaryContainer'],
    ['onTertiary', 'tertiary'], ['onTertiaryContainer', 'tertiaryContainer'], ['onBackground', 'background'], ['onSurface', 'surface'],
    ['onSurfaceVariant', 'surfaceVariant'], ['onSurfaceVariant', 'surface'], ['onError', 'error'], ['onErrorContainer', 'errorContainer'],
    ['inverseOnSurface', 'inverseSurface'], ['inversePrimary', 'inverseSurface'], ['primary', 'surface'], ['error', 'surface'], ['tertiary', 'surface'], ['secondary', 'surface']];
  for (const sc of ['surfaceContainerLowest', 'surfaceContainerLow', 'surfaceContainer', 'surfaceContainerHigh', 'surfaceContainerHighest', 'surfaceDim', 'surfaceBright']) {
    T.push(['onSurface', sc]); T.push(['onSurfaceVariant', sc]); T.push(['primary', sc]);
  }
  for (const [f, b] of T) P(mode, `${f} / ${b}`, o[f], o[b], 4.5);
  P(mode, 'outline / surface (borda)', o.outline, o.surface, 3);
  P(mode, 'outline / surfaceContainerHighest (borda)', o.outline, o.surfaceContainerHighest, 3);
  for (const n of ['pronto', 'cofre']) {
    P(mode, `semantic.${n}.onBase / base`, s[n].onBase, s[n].base, 4.5);
    P(mode, `semantic.${n}.onContainer / container`, s[n].onContainer, s[n].container, 4.5);
    P(mode, `semantic.${n}.base / surface (ícone)`, s[n].base, o.surface, 3);
  }
  // Semânticos FIXOS sobre as superfícies deste tema
  P(mode, 'semantic.gravando.base (fixo) / surface (ícone)', fx.gravando.base, o.surface, 3);
  P(mode, 'semantic.gravando.container (fixo) / surface (faixa)', fx.gravando.container, o.surface, 3);
  P(mode, 'semantic.pausado.base (fixo) / surface (ícone)', fx.pausado.base, o.surface, 3);
  for (const k of ['baixo', 'ok', 'clipando']) P(mode, `semantic.vu.${k} (fixo) / surfaceContainerHighest (trilho do tema)`, fx.vu[k], o.surfaceContainerHighest, 3);
  for (const k of ['baixo', 'ok', 'clipando']) P(mode, `semantic.vu.${k} (fixo) / surface`, fx.vu[k], o.surface, 3);
  for (const k of ['barra', 'tocada', 'cursor', 'alca']) P(mode, `semantic.onda.${k} / onda.fundo`, s.onda[k], s.onda.fundo, 3);
  for (const [k, m] of Object.entries(mk)) P(mode, `marker.${k}.fill (fixo) / onda.fundo`, m.fill, s.onda.fundo, 3);
}

// ---- Daltonismo: gravando (fixo) × pronto (deste tema) e × pausado (fixo)
const M = {
  protanopia: [[0.152286, 1.052583, -0.204868], [0.114503, 0.786281, 0.099216], [-0.003882, -0.048116, 1.051998]],
  deuteranopia: [[0.367322, 0.860646, -0.227968], [0.280085, 0.672501, 0.047413], [-0.011820, 0.042940, 0.968881]],
  tritanopia: [[1.255528, -0.076749, -0.178779], [-0.078411, 0.930809, 0.147602], [0.004733, 0.691367, 0.303900]],
};
const toLin = v => { v /= 255; return v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4; };
const toSrgb = v => { v = Math.min(1, Math.max(0, v)); v = v <= 0.0031308 ? v * 12.92 : 1.055 * v ** (1 / 2.4) - 0.055; return Math.round(v * 255); };
const sim = (hex, kind) => {
  const n = parseInt(hex.slice(1), 16); const rgb = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map(toLin);
  const o = kind === 'acromatopsia'
    ? Array(3).fill(toSrgb(0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]))
    : M[kind].map(r => r[0] * rgb[0] + r[1] * rgb[1] + r[2] * rgb[2]).map(toSrgb);
  return '#' + o.map(x => x.toString(16).padStart(2, '0')).join('').toUpperCase();
};
const cvd = [];
for (const mode of ['light', 'dark']) for (const kind of ['normal', 'protanopia', 'deuteranopia', 'tritanopia', 'acromatopsia']) {
  const g = base.semantic[mode].gravando.container;
  for (const [name, b] of [['gravando.container × pronto.container (Mixlirous)', semantic[mode].pronto.container], ['gravando.container × primary (Mixlirous)', out[mode].primary]]) {
    const A = kind === 'normal' ? g : sim(g, kind), B = kind === 'normal' ? b : sim(b, kind);
    const r = +ratio(A, B).toFixed(2);
    const min = name.includes('pronto') ? 3 : null;
    cvd.push({ mode: 'mixlirous-' + mode, visao: kind, comparacao: name, a: A, b: B, ratio: r, min, pass: min === null ? null : r >= min });
  }
}

const theme = {
  $descricao: 'Tema opcional "Mixlirous" (Ajustes › Aparência › Paleta). Substitui color.scheme e os semânticos de identidade abaixo; gravando, pausado, vu e marker continuam os de color.semantic/color.marker.',
  origem: { amostrasDasFotos: AMOSTRAS, matizesHct: Object.fromEntries(Object.entries(H).map(([k, v]) => [k, +v.toFixed(1)])),
    papeis: { primary: 'roxo dos painéis (croma 48)', secondary: 'roxo acinzentado (croma 16)', tertiary: 'ciano dos cabos (croma 34) = cofre neste tema', neutral: 'madeira (croma 6/10): superfícies quentes' } },
  scheme: out,
  semantic: semantic,
};
fs.writeFileSync('mixlirous.json', JSON.stringify(theme, null, 2));
fs.writeFileSync('pairs_mixlirous.json', JSON.stringify(pairs, null, 2));
fs.writeFileSync('cvd_mixlirous.json', JSON.stringify(cvd, null, 2));
const f = pairs.filter(p => !p.pass), fc = cvd.filter(c => c.pass === false);
console.log('matizes', theme.origem.matizesHct, 'pares', pairs.length, 'falhas', f.length, 'cvd falhas', fc.length);
for (const x of [...f, ...fc]) console.log(' ', x.mode, x.par || x.visao + ' ' + x.comparacao, x.ratio, '<', x.min);
for (const m of ['light', 'dark']) console.log(m, ['primary', 'primaryContainer', 'secondary', 'tertiary', 'surface', 'surfaceContainer', 'onSurface'].map(k => k + ' ' + out[m][k]).join(' | '));
console.log('cvd min pronto', Math.min(...cvd.filter(c => c.min).map(c => c.ratio)), 'cvd gravando×primary', cvd.filter(c => !c.min).map(c => c.ratio).join(' '));
