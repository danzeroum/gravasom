// Monta docs/design/tokens.json a partir de colors.json, pairs.json, cvd.json e icon.json (gerados por gen.mjs).
import fs from 'node:fs';
const OUT = process.argv[2];
const c = JSON.parse(fs.readFileSync('colors.json'));
const pairs = JSON.parse(fs.readFileSync('pairs.json'));
const cvd = JSON.parse(fs.readFileSync('cvd.json'));
const icon = JSON.parse(fs.readFileSync('icon.json'));

const T = (font, weight, size, lineHeight, letterSpacing, extra = {}) => ({ fontFamily: font, fontWeight: weight, fontSize: size, lineHeight, letterSpacing, ...extra });
const F = 'texto';

const tokens = {
  $meta: {
    nome: 'Gravasom — tokens de design',
    versao: '1.0.0',
    data: '2026-10-08',
    alvo: 'Jetpack Compose Material3 (BOM 2025.05.01 → material3 1.3.x). Glance 1.1.1 para o widget.',
    unidades: {
      cor: 'hex sRGB #RRGGBB, opaco. Em Kotlin: Color(0xFFRRGGBB).',
      fontSize: 'sp', lineHeight: 'sp', letterSpacing: 'sp (pode ser negativo)',
      shape: 'dp (raio de canto). "50%" = CircleShape / RoundedCornerShape(percent = 50).',
      spacing: 'dp', size: 'dp', duration: 'ms',
      easing: 'cubic-bezier(x1, y1, x2, y2) → CubicBezierEasing(x1, y1, x2, y2)',
    },
    geracao: {
      'Color.kt': 'color.scheme.light → lightColorScheme(<chave> = Color(<hex>)) com TODAS as chaves (os nomes são os parâmetros do Compose). color.scheme.dark → darkColorScheme(...). color.semantic e color.marker → data class GravasomExtraColors exposta por CompositionLocal (staticCompositionLocalOf), uma instância clara e uma escura.',
      'Type.kt': 'typography.fontFamilies.texto → FontFamily(Font(R.font.<arquivo>, FontWeight(<peso>)) ...). typography.scale → Typography(<chave> = TextStyle(fontFamily, fontWeight = FontWeight(<peso>), fontSize = <n>.sp, lineHeight = <n>.sp, letterSpacing = <n>.sp)). typography.extra → objeto GravasomTextStyles com TextStyle adicionais (aplicar fontFeatureSettings quando presente).',
      'Shape.kt': 'shape.scale → Shapes(extraSmall = RoundedCornerShape(<n>.dp), ...). shape.component → objeto GravasomShapes; valor "50%" = CircleShape.',
      'outros': 'spacing, size, motion e elevation → objetos Kotlin com Dp / Int (ms) / CubicBezierEasing de mesmo nome.',
    },
    corDinamica: 'Desligada por padrão. Se o usuário ligar "Cores do sistema" em Ajustes (Android 12+), substituir SÓ color.scheme pelo dynamicLight/DarkColorScheme. color.semantic e color.marker NUNCA mudam (são fixos para o vermelho de gravando e o contraste continuarem garantidos). Ver docs/design/decisoes.md D1.',
    referencias: 'Cada chave é citada em docs/design/telas.md no formato token:<caminho.com.pontos>.',
  },

  color: {
    scheme: c.scheme,
    semantic: {
      $descricao: {
        convencao: 'base = ícone, ponto ou traço sobre surface (≥ 3:1). onBase = texto/ícone sobre base (≥ 4,5:1). container = faixa ou cartão grande. onContainer = texto/ícone sobre container (≥ 4,5:1).',
        gravando: 'Estado "gravando". Vermelho reservado (só gravando + ações destrutivas, que usam color.scheme.error). Sempre com ícone ● e a palavra GRAVANDO.',
        pausado: 'Estado "pausado". Âmbar, sempre com ícone ❚❚ e a palavra PAUSADA. Luminância oposta à de gravando (faixa clara × faixa escura).',
        pronto: 'Botão Gravar parado. Luminância oposta à de gravando para a troca ser percebida também sem cor.',
        cofre: 'Gravação privada. Alias dos papéis tertiary (tertiary é reservado ao cofre; não usar tertiary para outra coisa). Sempre com ícone de cadeado.',
        vu: 'Medidor de nível, escala linear de -60 a 0 dBFS em 24 segmentos de 2,5 dB. trilho = segmentos apagados. baixo = segmentos 1–8 (≤ -40 dBFS); ok = 9–22 (-40 a -5); clipando = 23–24 (> -5). Rótulo de texto: "Baixo" se ≤ -40, "Alto demais" se RMS > -3 (aproximação; ver P3 em decisoes.md), senão "Bom".',
        onda: 'Forma de onda do player. silencio = faixas que "Pular silêncio" pula (desenhar com hachura, não só cor).',
      },
      light: c.semantic.light,
      dark: c.semantic.dark,
    },
    marker: {
      $descricao: 'Seis cores de marcador, sem vermelho. fill = linha e bandeira na onda (≥ 3:1 sobre onda.fundo); onFill = número dentro da bandeira; container/onContainer = chip na lista. Ordem fixa abaixo; o número do marcador e o rótulo sempre acompanham a cor.',
      ordem: ['ambar', 'verde', 'azul', 'roxo', 'rosa', 'grafite'],
      mapeamentoDominio: { AMBER: 'ambar', GREEN: 'verde', BLUE: 'azul', PURPLE: 'roxo', RED: 'rosa', GRAPHITE: 'grafite' },
      mapeamentoNota: 'RED (já existe no enum MarkerColor) passa a ser desenhado como "rosa" para não confundir com gravando. GRAPHITE não existe no enum: ver pergunta P1 em decisoes.md.',
      light: c.marker.light,
      dark: c.marker.dark,
    },
    icon: {
      $descricao: 'Cores fixas da marca no ícone adaptativo (não seguem tema nem cor dinâmica). monochrome é desenhado pelo sistema.',
      ...icon,
    },
  },

  typography: {
    fontFamilies: {
      texto: {
        nome: 'Atkinson Hyperlegible Next',
        licenca: 'SIL Open Font License 1.1',
        origem: 'https://fonts.google.com/specimen/Atkinson+Hyperlegible+Next',
        distribuicao: 'Empacotar os TTF estáticos em app/src/main/res/font (o app não tem internet; não usar Downloadable Fonts). Copiar o OFL.txt para app/src/main/assets/licencas/.',
        arquivos: [
          { res: 'atkinson_next_regular', weight: 400 },
          { res: 'atkinson_next_medium', weight: 500 },
          { res: 'atkinson_next_semibold', weight: 600 },
          { res: 'atkinson_next_bold', weight: 700 },
        ],
        fallback: 'FontFamily.Default (fonte do sistema)',
      },
    },
    scale: {
      displayLarge: T(F, 400, 57, 64, -0.25),
      displayMedium: T(F, 400, 45, 52, 0),
      displaySmall: T(F, 400, 36, 44, 0),
      headlineLarge: T(F, 600, 32, 40, 0),
      headlineMedium: T(F, 600, 28, 36, 0),
      headlineSmall: T(F, 600, 24, 32, 0),
      titleLarge: T(F, 600, 22, 28, 0),
      titleMedium: T(F, 600, 16, 24, 0.15),
      titleSmall: T(F, 600, 14, 20, 0.1),
      bodyLarge: T(F, 400, 16, 24, 0.5),
      bodyMedium: T(F, 400, 14, 20, 0.25),
      bodySmall: T(F, 400, 12, 16, 0.4),
      labelLarge: T(F, 500, 14, 20, 0.1),
      labelMedium: T(F, 500, 12, 16, 0.5),
      labelSmall: T(F, 500, 12, 16, 0.5, { nota: 'M3 usa 11 sp; o Gravasom usa 12 sp como mínimo legível.' }),
    },
    extra: {
      cronometro: T(F, 500, 64, 72, 0, { fontFeatureSettings: 'tnum', uso: 'Tempo da tela Gravar. Algarismos tabulares: os dígitos não "pulam" a cada segundo.' }),
      cronometroCompacto: T(F, 600, 28, 36, 0, { fontFeatureSettings: 'tnum', uso: 'Tempo no widget 4x1 e na faixa de estado de outras telas.' }),
      tempo: T(F, 500, 14, 20, 0.1, { fontFeatureSettings: 'tnum', uso: 'Posição/duração do player, tempo dos marcadores, durações da lista.' }),
      faixaEstado: T(F, 700, 14, 20, 1.0, { textTransform: 'uppercase', uso: 'Texto da faixa de estado: GRAVANDO / PAUSADA.' }),
      numeroMarcador: T(F, 700, 12, 16, 0, { fontFeatureSettings: 'tnum', uso: 'Número dentro da bandeira do marcador.' }),
    },
  },

  shape: {
    scale: { extraSmall: 4, small: 8, medium: 12, large: 20, extraLarge: 28 },
    component: {
      botaoGravar: '50%',
      botaoParar: 28,
      botaoTransporte: '50%',
      faixaEstado: '50%',
      chip: 8,
      cartao: 16,
      folhaInferior: 28,
      dialogo: 28,
      campoBusca: '50%',
      bandeiraMarcador: 6,
      onda: 12,
      segmentoVu: 2,
      widget: 20,
      $nota: 'botaoGravar → botaoParar é uma transição de forma (círculo → quadrado arredondado) ao começar a gravar; ver motion.semantic.morfarBotao. folhaInferior/dialogo: só cantos superiores na folha.',
    },
  },

  spacing: { none: 0, xxs: 2, xs: 4, sm: 8, md: 12, lg: 16, xl: 24, xxl: 32, xxxl: 48, margemTela: 16, entreGrupos: 24, $nota: 'Grade de 4 dp. margemTela = padding horizontal de todas as telas.' },

  size: {
    alvoToqueMin: 48,
    botaoGravar: 96,
    botaoParar: 96,
    botaoTransporte: 72,
    iconeBotaoGravar: 40,
    iconeTransporte: 32,
    icone: 24,
    iconePequeno: 18,
    faixaEstadoAltura: 48,
    vuAltura: 16,
    vuSegmentos: 24,
    vuEspacoSegmento: 2,
    ondaAltura: 128,
    bandeiraMarcador: 20,
    linhaMarcador: 2,
    alvoMarcador: 48,
    alcaSelecao: 24,
    pontoGravando: 12,
    miniPlayerAltura: 64,
    itemListaMin: 72,
    widgetMinLargura: 110,
    widgetMinAltura: 40,
  },

  elevation: { level0: 0, level1: 1, level2: 3, level3: 6, level4: 8, level5: 12 },

  motion: {
    duration: {
      short1: 50, short2: 100, short3: 150, short4: 200,
      medium1: 250, medium2: 300, medium3: 350, medium4: 400,
      long1: 450, long2: 500, long3: 550, long4: 600,
      extraLong1: 700, extraLong2: 800, extraLong3: 900, extraLong4: 1000,
    },
    easing: {
      linear: [0, 0, 1, 1],
      standard: [0.2, 0, 0, 1],
      standardDecelerate: [0, 0, 0, 1],
      standardAccelerate: [0.3, 0, 1, 1],
      emphasized: [0.2, 0, 0, 1],
      emphasizedDecelerate: [0.05, 0.7, 0.1, 1],
      emphasizedAccelerate: [0.3, 0, 0.8, 0.15],
    },
    semantic: {
      morfarBotao: { duration: 300, easing: 'emphasizedDecelerate', uso: 'Círculo (Gravar) → quadrado arredondado (Parar) e cor pronto → gravando.' },
      faixaEstadoEntra: { duration: 250, easing: 'emphasizedDecelerate', uso: 'Faixa GRAVANDO desce do topo.' },
      faixaEstadoSai: { duration: 200, easing: 'emphasizedAccelerate' },
      pulsoGravando: { duration: 1600, easing: 'standard', repeticao: 'infinita, reversa', escalaDe: 1.0, escalaAte: 1.6, alfaDe: 0.6, alfaAte: 0, uso: 'Anel que sai do ponto ● da faixa. O ponto em si fica parado e opaco.' },
      piscarPausado: { duration: 1000, easing: 'linear', repeticao: 'infinita', alfaDe: 1.0, alfaAte: 0.45, uso: 'Cronômetro parado "respira" (nunca some: 1 ciclo/s, abaixo do limite de 3 flashes/s).' },
      vuSubida: { duration: 50, easing: 'linear' },
      vuQueda: { duration: 300, easing: 'standardAccelerate' },
      vuPicoRetido: { duration: 1000, uso: 'Segmento de pico fica aceso por 1 s.' },
      confirmarMarcador: { duration: 400, easing: 'emphasizedDecelerate', uso: 'Bandeira do novo marcador cresce de 0 a 100% + vibração curta (já existe no MarkerFeedback).' },
      snackbar: { duration: 4000, uso: 'Tempo mínimo na tela de "Excluída · DESFAZER".' },
      reduzirMovimento: 'Se Settings.Global.ANIMATOR_DURATION_SCALE == 0 (ou "Remover animações"), sem pulso e sem piscar: o estado continua indicado por ícone, texto e cor.',
    },
  },

  contrast: {
    $descricao: 'Razão WCAG 2.x calculada sobre os hex deste arquivo. min 4,5 = texto; 3 = ícone, traço ou borda; 1 = informativo (não exigido). Gerado por script; não editar à mão.',
    pares: pairs,
    daltonismo: {
      $descricao: 'Simulação Machado et al. (2009, severidade 1,0) e acromatopsia (luminância). Mede se os estados se distinguem por claridade mesmo sem ver cor. min 3 nos pares estado × estado; os pares "× surface" são informativos.',
      comparacoes: cvd,
    },
  },
};

const fails = pairs.filter(p => p.pass === false).length + cvd.filter(x => x.pass === false).length;
if (fails) { console.error('FALHAS', fails); process.exit(1); }
fs.writeFileSync(OUT, JSON.stringify(tokens, null, 2) + '\n');

// Tabela markdown para identidade.md
const fmt = n => n.toFixed(2).replace('.', ',');
let md = '| Tema | Par (frente / fundo) | Frente | Fundo | Razão | Mínimo | OK |\n| --- | --- | --- | --- | --- | --- | --- |\n';
for (const p of pairs) md += `| ${p.mode} | \`${p.par}\` | ${p.fg} | ${p.bg} | ${fmt(p.ratio)}:1 | ${p.min === 1 ? 'informativo' : fmt(p.min) + ':1'} | ${p.pass ? 'sim' : 'NÃO'} |\n`;
let md2 = '| Tema | Visão | Comparação | A | B | Razão | Mínimo | OK |\n| --- | --- | --- | --- | --- | --- | --- | --- |\n';
for (const x of cvd) md2 += `| ${x.mode} | ${x.visao} | ${x.comparacao} | ${x.a} | ${x.b} | ${fmt(x.ratio)}:1 | ${x.min ? fmt(x.min) + ':1' : 'informativo'} | ${x.pass === null ? '—' : x.pass ? 'sim' : 'NÃO'} |\n`;
fs.writeFileSync('contraste.md', md); fs.writeFileSync('daltonismo.md', md2);
console.log('ok', pairs.length, 'pares,', cvd.length, 'comparações');
