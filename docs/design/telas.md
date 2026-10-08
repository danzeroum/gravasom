# Telas — wireframes de baixa fidelidade com tokens

Cada elemento numerado no desenho aponta para os tokens que usa. Notação:

- `c:` cor → `color.semantic.*.…`, `color.scheme.*.…` ou `color.marker.*.…` (o `*` é o tema atual: `light`/`dark`).
- `t:` tipografia → `typography.scale.…` ou `typography.extra.…`.
- `f:` forma → `shape.scale.…` ou `shape.component.…`.
- `s:` tamanho → `size.…` · `e:` espaçamento → `spacing.…` · `m:` movimento → `motion.semantic.…`.

Regras para todas as telas:

- Fundo `c:color.scheme.*.surface`; margem lateral `e:spacing.margemTela` (16 dp); espaço entre grupos
  `e:spacing.entreGrupos` (24 dp).
- Todo alvo de toque ≥ `s:size.alvoToqueMin` (48 dp), mesmo quando o desenho do ícone é menor.
- Nenhum estado depende só de cor: cada linha da coluna "Também diz" é obrigatória.
- Ícones `Icons.Rounded.*`, `s:size.icone` (24 dp) salvo indicação.

Desenhos em 34 colunas ≈ 360 dp de largura.

---

## 0. Faixa de estado global (componente)

Aparece no topo de **todas** as abas enquanto existe gravação (gravando ou pausada). Na tela Gravar fica dentro do
conteúdo; nas outras abas, logo abaixo da barra de status. Toque leva à tela Gravar.

```
 gravando                           pausada
(● GRAVANDO           00:12  )     (❚❚ PAUSADA           00:12  )
  ▲ anel pulsando                    ▲ cronômetro respirando
```

| # | Elemento | Gravando | Pausada | Também diz |
| --- | --- | --- | --- | --- |
| 0.1 | Contêiner | `c:semantic.*.gravando.container`; `f:shape.component.faixaEstado`; altura `s:size.faixaEstadoAltura`; padding horizontal `e:spacing.lg` | `c:semantic.*.pausado.container` | Escuro × claro (luminância invertida) |
| 0.2 | Ícone | `FiberManualRecord`, `s:size.pontoGravando` (12 dp), `c:semantic.*.gravando.onContainer`; anel `m:pulsoGravando` | `Pause`, 18 dp, `c:semantic.*.pausado.onContainer` | Ponto × barras |
| 0.3 | Texto | "GRAVANDO", `t:typography.extra.faixaEstado`, `c:semantic.*.gravando.onContainer` | "PAUSADA" | Palavra |
| 0.4 | Tempo | `t:typography.extra.tempo`, mesma cor do texto | idem + `m:piscarPausado` | Tempo correndo × parado |
| 0.5 | Acessibilidade | `liveRegion = Polite`; descrição "Gravando, 12 segundos" | "Gravação pausada em 12 segundos" | — |

Entrada e saída: `m:faixaEstadoEntra` / `m:faixaEstadoSai`.

---

## 1. Gravar

### 1a. Pronto

```
┌──────────────────────────────────┐
│ [1] Entrada: Microfone do aparelho ▾│
│                                  │
│            [2] 00:00             │
│   [3] ▯▯▯▯▯▯▯▯▯▯▯▯▯▯▯▯▯▯▯▯▯▯▯▯     │
│                                  │
│ ─ ─ ─ ─ ─ (metade do polegar) ─ ─│
│                                  │
│              .----.              │
│             (  ●   ) [4]         │
│              '----'              │
│              Gravar              │
│                                  │
│   [5] (M4A) ( WAV ) ( MP3 )      │
│   [6] Fica também em Músicas/    │
│       Recordings.                │
├──────────────────────────────────┤
│ [7] (●)Gravar  Gravações  Ajustes│
└──────────────────────────────────┘
```

| # | Elemento | Componente | Tokens | Texto / regra |
| --- | --- | --- | --- | --- |
| 1 | Entrada de áudio | `OutlinedButton` com ícone `Mic` à esquerda | `c:scheme.*.outline` (borda), `c:scheme.*.onSurface` (texto); `t:scale.labelLarge`; `f:shape.scale.small`; altura ≥ `s:size.alvoToqueMin` | "Entrada: <nome>" (string `record_input_label`) |
| 2 | Cronômetro | `Text` | `t:extra.cronometro`; `c:scheme.*.onSurfaceVariant` (pronto) | "00:00"; descrição "Tempo de gravação 00:00" |
| 3 | VU (apagado) | 24 segmentos `Box` | `s:size.vuAltura`, `s:size.vuSegmentos`, `e:size.vuEspacoSegmento`; `f:shape.component.segmentoVu`; todos `c:semantic.*.vu.trilho` | Sem rótulo no pronto |
| 4 | Botão Gravar | `FilledIconButton` grande | `s:size.botaoGravar` (96); `f:shape.component.botaoGravar` (círculo); fundo `c:semantic.*.pronto.container`; ícone `FiberManualRecord` `s:size.iconeBotaoGravar` em `c:semantic.*.pronto.onContainer`; rótulo `t:scale.labelLarge` `c:scheme.*.onSurface` | "Gravar"; centro do botão a ~30% da altura útil a partir de baixo |
| 5 | Formato | 3 × `FilterChip` | Selecionado: `c:scheme.*.secondaryContainer` + ícone `Check`; não selecionado: borda `c:scheme.*.outline`; `f:shape.component.chip`; `t:scale.labelLarge`; `e:spacing.sm` entre chips | "M4A", "WAV", "MP3" (o ✓ marca o escolhido, não só a cor) |
| 6 | Onde fica | `Text` | `t:scale.bodySmall`; `c:scheme.*.onSurfaceVariant` | `record_where`; com cofre: ícone `Lock` 18 dp `c:semantic.*.cofre.base` + `record_where_private` |
| 7 | Navegação | `NavigationBar` | Fundo `c:scheme.*.surfaceContainer`; indicador `c:scheme.*.secondaryContainer`; ícones ativos preenchidos, inativos contorno; `t:scale.labelMedium` | Gravar · Gravações · Ajustes |

### 1b. Gravando

```
┌──────────────────────────────────┐
│ [8] (● GRAVANDO           00:12 )│
│     Entrada: Microfone do aparelho│
│                                  │
│            [9] 00:12             │
│ [10] ▮▮▮▮▮▮▮▮▮▮▮▮▮▮▯▯▯▯▯▯▯▯▯▯ Bom │
│                                  │
│ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─│
│                                  │
│  .--.      .------.      .--.    │
│ ( ❚❚ ) [11]|  ■   |[12] ( ⚑ )[13]│
│  '--'      '------'      '--'    │
│ Pausar      Parar       Marcar   │
│                       2 marcadores│
│   (M4A) ( WAV ) ( MP3 ) (cinza)  │
│   O formato não muda durante a   │
│   gravação.                      │
├──────────────────────────────────┤
│  (●)Gravar  Gravações  Ajustes   │
└──────────────────────────────────┘
```

| # | Elemento | Componente | Tokens | Texto / regra |
| --- | --- | --- | --- | --- |
| 8 | Faixa de estado | Seção 0 | Seção 0 | Fica no topo, largura total menos margens |
| 9 | Cronômetro | `Text` | `t:extra.cronometro`; `c:scheme.*.onSurface` | Atualiza a cada 1 s |
| 10 | VU ativo | 24 segmentos + rótulo | Escala -60 a 0 dBFS, 2,5 dB por segmento: 1–8 (≤ -40) `c:semantic.*.vu.baixo`; 9–22 (-40 a -5) `c:semantic.*.vu.ok`; 23–24 (> -5) `c:semantic.*.vu.clipando`; apagados `c:semantic.*.vu.trilho`; `m:vuSubida`, `m:vuQueda`, `m:vuPicoRetido`; rótulo `t:scale.labelMedium` `c:scheme.*.onSurfaceVariant` | Rótulo: "Baixo" (≤ -40), "Alto demais" com ícone `Warning` (RMS > -3, ver P3), senão "Bom"; descrição de acessibilidade "Nível do microfone: bom" |
| 11 | Pausar | `FilledTonalIconButton` | `s:size.botaoTransporte` (72); `f:shape.component.botaoTransporte`; fundo `c:scheme.*.secondaryContainer`; ícone `Pause` `s:size.iconeTransporte` `c:scheme.*.onSecondaryContainer`; rótulo `t:scale.labelLarge` | "Pausar" |
| 12 | Parar | `FilledIconButton` | `s:size.botaoParar` (96); `f:shape.component.botaoParar` (canto 28); fundo `c:semantic.*.gravando.container`; ícone `Stop` 40 dp `c:semantic.*.gravando.onContainer`; morfa a partir do 4 com `m:morfarBotao` | "Parar" |
| 13 | Marcar | `FilledTonalIconButton` | Igual ao 11; ícone `Flag`; contador `t:scale.labelSmall` `c:scheme.*.onSurfaceVariant`; ao tocar: `m:confirmarMarcador` (bandeira cresce no ícone) + vibração | "Marcar"; "N marcadores" |
| — | Chips de formato | Como 5, `enabled = false` | Desabilitado: 38% de opacidade (padrão M3) | Texto `record_format_locked` em `t:scale.labelSmall` |

Ordem dos botões: Pausar à esquerda, Parar no centro (maior), Marcar à direita. Espaço entre botões `e:spacing.xl`.

### 1c. Pausada

Igual a 1b com estas trocas:

| # | Troca | Tokens | Texto |
| --- | --- | --- | --- |
| 8 | Faixa pausada | Seção 0, coluna Pausada | "PAUSADA" |
| 9 | Cronômetro respira | `m:piscarPausado`; `c:scheme.*.onSurface` | Tempo parado |
| 10 | VU todo apagado | `c:semantic.*.vu.trilho` | Sem rótulo |
| 11 | Retomar | Ícone `PlayArrow`; fundo `c:semantic.*.pausado.container`; ícone `c:semantic.*.pausado.onContainer` | "Retomar" |
| — | Motivo (se houve ligação) | `Text` abaixo da faixa, `t:scale.bodyMedium`, `c:scheme.*.onSurfaceVariant` | "Pausada pela ligação. Toque em Retomar." |

### 1d. Salvando, salvo, erro e permissão

| Estado | Elemento | Tokens | Texto |
| --- | --- | --- | --- |
| Salvando | Botão 12 vira `CircularProgressIndicator` 48 dp dentro do mesmo quadrado | Fundo `c:scheme.*.surfaceContainerHigh`; indicador `c:scheme.*.primary` | "Salvando…" (`t:scale.labelLarge`) |
| Salvo | Botão volta ao 4 (morfa reverso); abaixo `OutlinedButton` | `c:scheme.*.primary` (texto) | "Gravação salva" + botão "Ver em Gravações" |
| Erro | `Card` acima dos botões | `c:scheme.*.errorContainer` / `c:scheme.*.onErrorContainer`; ícone `ErrorOutline`; `f:shape.component.cartao`; `t:scale.titleMedium` + `t:scale.bodyMedium` | Causa + ação (ver tom de voz em `identidade.md` §3) |
| Permissão negada | `Card` | Igual ao erro; botão `Button` com `c:scheme.*.primary` | `record_perm_*` + "Abrir configurações" |
| Pouco espaço | `AlertDialog` | `f:shape.component.dialogo`; fundo `c:scheme.*.surfaceContainerHigh`; ícone `Warning` `c:semantic.*.pausado.base` | `record_low_space_*` |

---

## 2. Gravações

```
┌──────────────────────────────────┐
│ [0] (● GRAVANDO           12:40 )│  ← só se houver gravação
│ [14] ( 🔍 Buscar por nome…    ✕ )│
│ [15] (aula ✓) (física) (banda)  →│
│ ──────────────────────────────── │
│ [16] ▶  Aula de física      ★ ⋮  │
│         00:48:03 · M4A · 46 MB   │
│         #aula #física            │
│ ──────────────────────────────── │
│      ▶  Ideia violão  🔒No cofre ⋮│
│         00:31 · WAV · 2,6 MB     │
│ ──────────────────────────────── │
│ ┌[17]─────────────────────────┐  │
│ │ ❚❚ Aula de física   12:03    │  │
│ │ ▬▬▬▬▬▬▬▬▬●────────────────  │  │
│ └──────────────────────────────┘  │
├──────────────────────────────────┤
│  Gravar  (≡)Gravações  Ajustes   │
└──────────────────────────────────┘
```

| # | Elemento | Componente | Tokens | Texto / regra |
| --- | --- | --- | --- | --- |
| 14 | Busca | `SearchBar`/`OutlinedTextField` | Fundo `c:scheme.*.surfaceContainerHigh`; `f:shape.component.campoBusca`; ícone `Search`; placeholder `c:scheme.*.onSurfaceVariant`; `t:scale.bodyLarge`; altura 56 | `library_search_hint` |
| 15 | Filtro de etiquetas | `LazyRow` de `FilterChip` | Como 5; ✓ no selecionado; `e:spacing.sm`; padding `e:spacing.margemTela` | Sempre visível; sem etiquetas: chip desabilitado "Sem etiquetas ainda" |
| 16 | Item | `ListItem` | Altura ≥ `s:size.itemListaMin`; título `t:scale.titleMedium` `c:scheme.*.onSurface` (máx. 2 linhas); meta `t:extra.tempo` + `t:scale.bodySmall` `c:scheme.*.onSurfaceVariant`; etiquetas `t:scale.labelSmall` `c:scheme.*.onSurfaceVariant`; play `IconButton` 48 dp `c:scheme.*.primary`; favorito `Star` 18 dp `c:marker.*.ambar.fill` + descrição "Favorita"; cofre: `Lock` 18 dp `c:semantic.*.cofre.base` + texto "No cofre" `t:scale.labelSmall` `c:semantic.*.cofre.base`; divisor `c:scheme.*.outlineVariant` | Tocando: o ícone vira `Pause` e o título fica `c:scheme.*.primary` + descrição "Tocando" |
| 17 | Mini-player | `Surface` | Fundo `c:scheme.*.primaryContainer`; texto `c:scheme.*.onPrimaryContainer`; `f:shape.component.cartao`; altura `s:size.miniPlayerAltura`; tempo `t:extra.tempo`; barra `LinearProgressIndicator` `c:scheme.*.primary` sobre `c:scheme.*.surfaceContainerHighest`; elevação `elevation.level2` | Título "Gravação privada" se for do cofre (já existe `player_notification_private_title`) |
| — | Excluída | `Snackbar` | Fundo `c:scheme.*.inverseSurface`; ação `c:scheme.*.inversePrimary`; `m:snackbar` (4 s) | "Gravação excluída" · "DESFAZER" |
| — | Lista vazia | `Text` + ícone da marca 64 dp `c:scheme.*.outline` | `t:scale.bodyLarge` | `library_empty` |

---

## 3. Player (painel sobre a lista)

```
┌──────────────────────────────────┐
│ [18] Aula de física           ✕  │
│ ┌[19]──────────────────────────┐ │
│ │ ①      ②          ③          │ │
│ │ ▌▂▄▆█▆▄▂▁▁/////▁▂▄▆█▆▄▂▁▂▄▆█▌│ │
│ │ ▲tocado │cursor     ▲silêncio │ │
│ └──────────────────────────────┘ │
│ [20] 12:03 / 48:03   (−)(+) 4x   │
│ [21] (⏮)  ( ❚❚ )  (⏭)  ⚑Marcar ✂Trecho│
│ [22] (Pular silêncio ✓)(1,5x)(Etiquetas 2) → │
│ [23] Marcadores (3)              │
│  [①] 02:10  Definição de força ✎│
│  [②] 15:42  Exemplo da rampa   ✎│
│  [③] 31:05  Pergunta da prova  ✎│
└──────────────────────────────────┘
```

| # | Elemento | Componente | Tokens | Texto / regra |
| --- | --- | --- | --- | --- |
| 18 | Título | `Text` + `IconButton Close` | `t:scale.titleMedium`; 1 linha com reticências; fechar 48 dp | Fechar **minimiza** para o mini-player (heurística 3c) |
| 19 | Onda | `Canvas` | Altura `s:size.ondaAltura`; `f:shape.component.onda`; fundo `c:semantic.*.onda.fundo`; barras não tocadas `c:semantic.*.onda.barra`; tocadas `c:semantic.*.onda.tocada`; cursor 2 dp `c:semantic.*.onda.cursor`; faixas de silêncio: fundo `c:semantic.*.onda.silencio` **com hachura diagonal** 45° a cada 6 dp; seleção `c:semantic.*.onda.selecao` + alças `c:semantic.*.onda.alca` de `s:size.alcaSelecao` | Calculando: `CircularProgressIndicator` 20 dp `c:scheme.*.primary` + `player_wave_analyzing` |
| 19a | Marcador na onda | Linha + bandeira | Linha `s:size.linhaMarcador` em `c:marker.*.<cor>.fill`; bandeira `s:size.bandeiraMarcador` no topo, `f:shape.component.bandeiraMarcador`, fundo `c:marker.*.<cor>.fill`, número `t:extra.numeroMarcador` `c:marker.*.<cor>.onFill`; alvo invisível `s:size.alvoMarcador` | O número identifica; a cor agrupa |
| 20 | Tempo e zoom | `Text` + 2 `IconButton` | `t:extra.tempo` `c:scheme.*.onSurfaceVariant`; `ZoomOut`/`ZoomIn` 48 dp; nível `t:scale.labelMedium` | "12:03 / 48:03"; "4x" (zoom visível, heurística 6c) |
| 21 | Transporte | `IconButton` × 2, `FilledIconButton`, `TextButton` × 2 | Tocar/pausar 56 dp `c:scheme.*.primary` / `c:scheme.*.onPrimary`, `f:shape.component.botaoTransporte`; anterior/próximo marcador 48 dp; "Marcar" e "Trecho" com ícone + texto `t:scale.labelLarge` | "Trecho" escrito (tesoura deixa de ser ambígua) |
| 22 | Chips | `LazyRow` de `FilterChip`/`AssistChip` | Todos com mesmo estilo: `f:shape.component.chip`; selecionado `c:scheme.*.secondaryContainer` + ✓; rola na horizontal (não corta) | "Pular silêncio", "1,5x" (vírgula), "Etiquetas (2)" |
| 23 | Lista de marcadores | `ListItem` | Altura ≥ 48; chip do número `c:marker.*.<cor>.container` / `c:marker.*.<cor>.onContainer`, `t:extra.numeroMarcador`; tempo `t:extra.tempo`; título `t:scale.bodyMedium`; nota `t:scale.bodySmall` `c:scheme.*.onSurfaceVariant`; editar `Edit` 48 dp | Toque na linha = ir para o ponto e tocar |

### 3a. Diálogo do marcador (com cor — depende de P5)

```
┌──────────────────────────────┐
│ Marcador ② em 15:42          │
│ Nome (opcional) [__________] │
│ Nota [_____________] 12/140  │
│ Cor                          │
│ (●✓)(● )(● )(● )(● )(● )     │
│ Âmbar                        │
│ [Excluir marcador] [Cancelar] [Salvar] │
└──────────────────────────────┘
```

| Elemento | Tokens | Regra |
| --- | --- | --- |
| Amostras | 6 círculos 32 dp dentro de alvos de 48 dp, na ordem `color.marker.ordem`; fundo `c:marker.*.<cor>.fill`; selecionada com anel 2 dp `c:scheme.*.onSurface` + ícone `Check` `c:marker.*.<cor>.onFill` | Abaixo, o nome da cor selecionada em texto (`t:scale.labelLarge`); cada amostra tem descrição "Cor âmbar, selecionada" |
| Excluir marcador | `TextButton` com `c:scheme.*.error` | Excluir mostra snackbar com DESFAZER (heurística 3b) |

---

## 4. Ajustes

```
┌──────────────────────────────────┐
│ [24] Gravação                    │
│  Formato de gravação             │
│   ◉ M4A · leve, ideal para voz   │
│   ○ WAV · sem compressão         │
│   ○ MP3 · abre em qualquer lugar │
│  Qualidade (kbps) (64)(128✓)(192)│
│ [25] Aparência                   │
│  Tema   (Sistema✓)(Claro)(Escuro)│
│  Cores do sistema          [  ○] │
│  Usa as cores do papel de parede.│
│  Gravando, pausado e cofre       │
│  mantêm as cores do Gravasom.    │
│ [26] Privacidade                 │
│  🔒 Cofre cifrado          [● ] │
│  Registrar meus números    [  ○] │
│  Seus números                  › │
│ Sobre                            │
│  Este app não usa a internet.    │
│  Licenças de código aberto     › │
│  Excluir todas as gravações      │
├──────────────────────────────────┤
│  Gravar  Gravações  (⚙)Ajustes   │
└──────────────────────────────────┘
```

| # | Elemento | Componente | Tokens | Texto / regra |
| --- | --- | --- | --- | --- |
| 24 | Título de seção | `Text` | `t:scale.titleSmall`; `c:scheme.*.primary`; margem superior `e:spacing.xl` | "Gravação", "Aparência", "Privacidade", "Sobre" |
| — | Opção | `ListItem` com `RadioButton`/`Switch` | Título `t:scale.bodyLarge` `c:scheme.*.onSurface`; descrição `t:scale.bodySmall` `c:scheme.*.onSurfaceVariant`; linha inteira clicável, ≥ 56 dp | Switch ligado mostra ícone `Check` no polegar (não só cor) |
| 25 | Aparência (nova) | `SegmentedButton` (tema) + `Switch` (cores) | `c:scheme.*.secondaryContainer` no segmento ativo + ✓; o switch só aparece no Android 12+ | "Tema" (Sistema / Claro / Escuro) — a parte "Tema" é sugestão nova, não é exigida pela identidade; "Cores do sistema" é a decisão D1 |
| 26 | Cofre | `ListItem` + `Switch` | Ícone `Lock` `c:semantic.*.cofre.base`; switch ligado `c:semantic.*.cofre.base` (trilho) e `c:semantic.*.cofre.onBase` (polegar) | `settings_private` / `settings_private_desc` |
| — | Excluir tudo | `ListItem` | Texto `c:scheme.*.error`; ícone `DeleteForever` `c:scheme.*.error` | Confirmação em `AlertDialog` (ação irreversível) |

---

## 5. Widget (Glance)

Três estados × dois tamanhos (P8). Fundo e textos usam os papéis do esquema **fixo** (não o dinâmico), para o vermelho
nunca vir do papel de parede. Canto `f:shape.component.widget` (no Android 12+ usar `system_app_widget_background_radius`).

```
 2×1 pronto            2×1 gravando           2×1 pausado
┌──────────────┐      ┌──────────────┐       ┌──────────────┐
│ (●) Gravar   │      │ ■ Parar 12:40│       │ ▶ Retomar    │
│              │      │ ⚑ Marcar     │       │ ❚❚ 12:40     │
└──────────────┘      └──────────────┘       └──────────────┘

 4×1 pronto                         4×1 gravando
┌──────────────────────────────┐   ┌──────────────────────────────┐
│ (●) Gravar      ⚑ Marcar(off)│   │ ● GRAVANDO 12:40 ■Parar ⚑Marcar│
└──────────────────────────────┘   └──────────────────────────────┘
```

| Estado | Fundo | Texto e ícones | Botões | Também diz |
| --- | --- | --- | --- | --- |
| Pronto | `c:scheme.light.surfaceContainer` / `c:scheme.dark.surfaceContainer` (segue o tema do sistema) | `c:scheme.*.onSurface`, `t:scale.labelLarge` | Gravar: círculo 48 dp `c:semantic.*.pronto.container` + ícone ● `c:semantic.*.pronto.onContainer`. Marcar fica oculto no 2×1 e desabilitado (38%) no 4×1, com texto "Marcar" | Anel vazado + "Gravar" |
| Gravando | `c:semantic.*.gravando.container` | `c:semantic.*.gravando.onContainer`; tempo `t:extra.cronometroCompacto` (4×1) ou `t:extra.tempo` (2×1) | Parar: quadrado 48 dp com ■; Marcar: ícone `Flag` + texto | "GRAVANDO"/"Parar" + ■ + fundo escuro |
| Pausado | `c:semantic.*.pausado.container` | `c:semantic.*.pausado.onContainer` | Retomar ▶ + Parar ■ | "Pausada" + ❚❚ + fundo claro |
| Marcar sem gravação | — | Toast/texto curto "Nada sendo gravado" por 2 s | — | Resolve heurística 1b |

Todos os alvos ≥ 48 dp. O tempo do widget atualiza a cada mudança de estado; o cronômetro usa `Chronometer` do
RemoteViews se o Glance permitir (pergunta para o agente: se não, mostrar só "Gravando" sem segundos).

---

## 6. Bloco de Configurações Rápidas (tile)

| Estado | `Tile.state` | Ícone | `label` | `subtitle` (API 29+) | `contentDescription` |
| --- | --- | --- | --- | --- | --- |
| Pronto | `STATE_INACTIVE` | `ic_tile_gravar` (anel + ponto) | "Gravar" | "Gravasom" | "Gravar com o Gravasom" |
| Gravando | `STATE_ACTIVE` | `ic_tile_gravando` (disco cheio + ■) | "Gravando" | "Toque para parar" | "Gravando. Toque para parar" |
| Pausado | `STATE_ACTIVE` | `ic_stat_pausado` | "Pausada" | "Toque para parar" | "Gravação pausada. Toque para parar" |

O sistema pinta o bloco ativo/inativo com as cores dele; o estado é dito pelo ícone (silhueta cheia × vazada) e pelo
texto. Depende de P2 para saber o estado.

---

## 7. Notificações

### 7a. Gravação (canal `gravasom_recording`)

```
 gravando (colorida)                         pausada
┌──────────────────────────────────────┐    ┌──────────────────────────────────────┐
│ ◉ Gravasom · 12:40                   │    │ ◎ Gravasom                           │
│ Gravando                             │    │ Gravação pausada                     │
│ Marcador 3 às 12:03                  │    │ 12:40 gravados                       │
│ [Pausar]   [Marcar]   [Parar]        │    │ [Retomar]  [Marcar]   [Parar]        │
└──────────────────────────────────────┘    └──────────────────────────────────────┘
```

| Elemento | Gravando | Pausada |
| --- | --- | --- |
| `setSmallIcon` | `ic_stat_gravando` (disco cheio) | `ic_stat_pausado` (anel + ❚❚) |
| `setColor` | `color.semantic.light.gravando.container` (`#BA1A1A`) | `color.semantic.light.pausado.base` (`#926000`) |
| `setColorized` | `true` | `false` |
| Título | "Gravando" (`notif_title_recording`) | "Gravação pausada" (`notif_title_paused`) |
| Tempo | `setUsesChronometer(true)` | Texto "12:40 gravados" (novo) |
| Texto | "Marcador N às mm:ss" após marcar (já existe) | idem |
| Ações (ordem fixa) | Pausar · Marcar · Parar | Retomar · Marcar · Parar |

### 7b. Mídia (player)

| Elemento | Valor |
| --- | --- |
| Ícone pequeno | `ic_stat_gravasom` (P7) |
| Título | Nome da gravação; do cofre: "Gravação privada" (já existe) |
| Cor | A do sistema para mídia (não colorir: tocar não é gravar) |

---

## 8. Checklist para o agente de código

1. Gerar `Color.kt`, `Type.kt`, `Shape.kt` e `GravasomExtraColors` a partir de `tokens.json` (`$meta.geracao`).
2. Trocar todos os `MaterialTheme.colorScheme.primary` que hoje significam "gravando" por `color.semantic.*.gravando`.
3. Na onda, trocar `marker = colors.tertiary` por `color.marker.*.<cor do marcador>` e `bar = primary` / `played = onSurface`
   por `onda.barra` / `onda.tocada`.
4. Ler o cofre sempre de `color.semantic.*.cofre` (nunca de `colorScheme.tertiary`).
5. Converter os SVGs de `docs/design/icone/` em Vector Drawables (ver `icone/README.md`).
6. Perguntas P1–P8 em `decisoes.md` antes de implementar seletor de cor, widget com estado e bloco com estado.
