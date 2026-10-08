# Identidade visual do Gravasom

Fonte única dos valores: [`tokens.json`](tokens.json). Decisões e perguntas em aberto: [`decisoes.md`](decisoes.md).
Telas: [`telas.md`](telas.md). Ideação da tela Gravar: [`crazy8s.md`](crazy8s.md). Ícones: [`icone/`](icone/).

Nas tabelas, `token:caminho.com.pontos` aponta para uma chave de `tokens.json`. Onde aparece `*`, vale o tema atual
(`light` ou `dark`).

---

## 1. Conceito

**Caderno de campo silencioso.** O Gravasom é o caderno que vai no bolso para aula, reunião, entrevista ou ideia de
violão, e que não conversa com a internet. A interface fica quieta: petróleo e grafite, formas redondas, letras grandes
de alta legibilidade e um botão que se acha sem olhar duas vezes. Assim, o único elemento que "fala alto" é o vermelho
do **gravando**, que aparece só quando há gravação, sempre com ponto, palavra e cronômetro correndo. Privacidade tem a
sua própria cor (ameixa do cofre) e marcadores são etiquetas coloridas de caderno, numeradas. O resultado deve parecer um
instrumento confiável, não um app de rede social.

## 2. Processo (de onde vem cada decisão)

| Etapa | O que foi usado | Resultado |
| --- | --- | --- |
| Empatizar | `docs/ux/01-personas.md`, `jornada.md` (Ana, Bruno, Carla) | Uma mão, tela bloqueada, pressa, medo de perder a gravação, desconfiança da nuvem |
| Definir | Momentos da verdade da jornada + avaliação heurística (`docs/ux/heuristica.md`) | Problema de identidade: o estado "gravando" se confunde com a cor da interface (D1, D2) e o "Marcar" não confirma (heurística 1) |
| Idear | [`crazy8s.md`](crazy8s.md) | 8 variações da tela Gravar; escolhida a "faixa de estado + botão que muda de forma" |
| Prototipar | [`telas.md`](telas.md) (baixa fidelidade, com tokens) e o canvas de design | 4 telas + player, widget, bloco e notificação |
| Testar | Contraste e daltonismo calculados (seções 12 e 13); teste de 5 segundos pendente (`decisoes.md`, "Como validar") | Todos os pares passam no AA |

## 3. Tom de voz (pt-BR)

O app fala como um bom assistente de estúdio: curto, calmo e concreto. Trata por **você** sem usar o pronome (o verbo
basta).

| Princípio | Faça | Evite |
| --- | --- | --- |
| Verbo de ação nos botões | "Gravar", "Parar", "Marcar", "Salvar em…" | "OK", "Confirmar", "Prosseguir" |
| Estado em uma palavra, em maiúsculas só na faixa | "GRAVANDO", "PAUSADA" | "A gravação está em andamento no momento" |
| Erro = o que houve + o que fazer | "Sem espaço no aparelho. Apague gravações antigas ou grave em M4A." | "Erro 3", "Não foi possível exportar o trecho." (sem causa) |
| Nunca culpar a pessoa | "Nota longa demais (máximo 140)." | "Você digitou demais" |
| Tranquilizar sobre privacidade, sem jargão | "Fica só neste aparelho." | "Armazenamento local criptografado AES-256-GCM" (o detalhe fica em Ajustes › Sobre) |
| Números no padrão brasileiro | "1,5x", "08/10/2026 14:32", "1,2 MB" | "1.5x" (hoje `PlayerPanel.kt` mostra com ponto) |
| Jargão só com explicação | "M4A · leve, ideal para voz" (já existe) | "AAC-LC 128 kbps" sozinho |
| Desfazer em vez de "tem certeza?" | "Gravação excluída · DESFAZER" | Diálogo de confirmação para tudo |

Glossário fixo: **gravação** (nunca "áudio" ou "arquivo" na interface), **marcador**, **etiqueta**, **cofre**
("Gravação privada" quando o título está oculto), **trecho**, **entrada** (microfone).

## 4. Paleta

Semente petróleo `#1E6A78`, gerada com o algoritmo HCT do Material (biblioteca `material-color-utilities` 0.3.0) com
paletas fixas: primária (matiz da semente, croma 40), secundária (croma 14), terciária ameixa (matiz 330, croma 30,
reservada ao cofre), neutras (croma 5 e 9). Os 36 papéis do Material 3 estão em `token:color.scheme.light` e
`token:color.scheme.dark`.

### 4.1 Papéis principais

| Papel | Claro | Escuro | Uso no Gravasom |
| --- | --- | --- | --- |
| primary | `#006877` | `#76D4E8` | Ações principais, aba ativa, parte já tocada da onda |
| onPrimary | `#FFFFFF` | `#00363F` | Texto sobre primary |
| primaryContainer | `#A5EEFF` | `#004E5A` | Chip de formato escolhido, mini-player |
| secondary | `#4E6267` | `#B5CAD0` | Alças de seleção, ações secundárias |
| secondaryContainer | `#D1E6EC` | `#374A4F` | Seleção de trecho na onda, indicador da aba |
| tertiary (= cofre) | `#785177` | `#E7B8E4` | Só cofre |
| surface | `#F6FAFB` | `#101415` | Fundo das telas |
| surfaceContainer | `#EBEEF0` | `#1C2022` | Barra de navegação, cartões |
| onSurface | `#181C1D` | `#DFE3E4` | Texto principal |
| onSurfaceVariant | `#3E494B` | `#BDC8CB` | Texto de apoio (metadados da lista) |
| outline | `#6E797C` | `#889396` | Bordas de campo, barras da onda não tocadas |
| error (= destrutivo) | `#BA1A1A` | `#FFB4AB` | Excluir, erros |

### 4.2 Semânticos (fixos, não seguem a cor dinâmica)

| Token | Claro (base / container / texto no container) | Escuro | Sempre acompanhado de |
| --- | --- | --- | --- |
| `semantic.*.gravando` | `#BA1A1A` / `#BA1A1A` / `#FFFFFF` | `#FF897D` / `#CC2925` / `#FFFFFF` | ● + "GRAVANDO" + cronômetro correndo |
| `semantic.*.pausado` | `#926000` / `#FFCC85` / `#291800` | `#FFB94E` / `#FFD6A1` / `#291800` | ❚❚ + "PAUSADA" + cronômetro respirando |
| `semantic.*.pronto` | `#006877` / `#A5EEFF` / `#004E5A` | `#76D4E8` / `#A5EEFF` / `#001F25` | ● + "Gravar" |
| `semantic.*.cofre` | `#785177` / `#FFD6FB` / `#5E3A5E` | `#E7B8E4` / `#5E3A5E` / `#FFD6FB` | Cadeado + "No cofre" |
| `semantic.*.vu.baixo` | `#6F797B` | `#969FA2` | Rótulo "Baixo" |
| `semantic.*.vu.ok` | `#007A54` | `#60CC9C` | Rótulo "Bom" |
| `semantic.*.vu.clipando` | `#BF5A00` | `#FF9651` | Ícone de aviso + "Alto demais" |
| `semantic.*.vu.trilho` | `#DFE3E4` | `#313537` | (segmento apagado) |
| `semantic.*.onda.*` | ver `tokens.json` | ver `tokens.json` | Silêncio com hachura; cursor com 2 dp |

Repare que **gravando é escuro com texto branco** e **pausado e pronto são claros com texto escuro**, nos dois temas. É
isso que mantém os estados distintos para quem não vê vermelho (seção 13).

### 4.3 Marcadores

| Token | Claro (fill / container) | Escuro (fill / container) | Enum hoje |
| --- | --- | --- | --- |
| `marker.*.ambar` | `#8D6300` / `#FFDEAB` | `#F2AC00` / `#5F4100` | `AMBER` (padrão) |
| `marker.*.verde` | `#327932` / `#A9F59F` | `#80CA79` / `#035310` | `GREEN` |
| `marker.*.azul` | `#1A6DB5` / `#D2E4FF` | `#85BCFF` / `#00497F` | `BLUE` |
| `marker.*.roxo` | `#755BB5` / `#E9DDFF` | `#C4ACFF` / `#50358E` | `PURPLE` |
| `marker.*.rosa` | `#AF4282` / `#FFD8E8` | `#FF98CE` / `#811A5C` | `RED` (nome mantido) → rótulo "Rosa" |
| `marker.*.grafite` | `#686B6C` / `#E1E3E4` | `#B7B9BA` / `#444748` | `GRAPHITE` (novo) |

### 4.4 Paleta opcional "Mixlirous"

Escolhida em Ajustes › Aparência › Paleta (`token:color.themes.mixlirous`). Cores tiradas das fotos de estúdio de
referência (mediana dos pixels saturados de cada matiz), passadas pelo mesmo gerador HCT:

| Origem na foto | Amostra | Vira | Claro | Escuro |
| --- | --- | --- | --- | --- |
| Painéis acústicos roxos | `#562B64` | primary | `#804792` | `#EFB0FF` |
| | | primaryContainer | `#FAD7FF` | `#662F79` |
| Roxo acinzentado (mesmo matiz, pouco croma) | — | secondary | `#6A596C` | `#D5C0D7` |
| Brilho ciano dos cabos | `#41A09A` | tertiary = **cofre** neste tema | `#0C6A65` | `#87D4CE` |
| Difusor de madeira e luz quente | `#C1935E` | neutros (surface…) | `#FFF8F4` | `#18120C` |
| | | surfaceContainer | `#FAEBE0` | `#251E17` |
| | | onSurface | `#211A13` | `#EEE0D5` |

O que muda e o que **não** muda:

- Mudam: os 36 papéis do esquema e os semânticos de identidade `pronto` (botão Gravar: `#FAD7FF` / `#FAD7FF`), `cofre` (ciano + cadeado) e `onda`.
- **Não mudam:** gravando, pausado, VU e marcadores. O vermelho neon das fotos **não** entrou: ele colidiria com o
  vermelho reservado para "gravando". O brilho rosa-avermelhado das fotos aparece só como o roxo-rosado claro do primário
  escuro (`#EFB0FF`).
- A madeira foi para os neutros e não para `secondary`, porque `secondaryContainer` pinta o botão Pausar e ficaria
  parecido com o âmbar de "pausado".
- As fotos em si não entram no app (pesariam no APK e mostram equipamentos e marcas de terceiros). O tema leva só as
  cores.

## 5. Tipografia

**Atkinson Hyperlegible Next** (OFL 1.1), uma família só, só os pesos **400 (Regular) e 600 (SemiBold)** empacotados. Foi desenhada para
leitores com baixa visão: letras de forma bem distinta (I/l/1, O/0), boa para ler de relance com o celular apoiado na mesa.

- Escala completa do M3 em `token:typography.scale`. Diferenças em relação ao padrão: títulos, headlines e rótulos em
  600 (o M3 usa 400/500; aqui só existem 400 e 600) e `labelSmall` com 12 sp (o M3 usa 11).
- Estilos extras em `token:typography.extra`:
  - `cronometro` 64/72 sp, peso 400, algarismos tabulares (`tnum`): os dígitos não "dançam".
  - `cronometroCompacto` 28/36 sp, para widget.
  - `tempo` 14/20 sp tabular, para posição, duração e tempo de marcador.
  - `faixaEstado` 14/20 sp, 600, caixa-alta, espaçamento 1,0: "GRAVANDO", "PAUSADA".
  - `numeroMarcador` 12/16 sp, 600, dentro da bandeira.
- Respeitar o tamanho de fonte do sistema (só `sp`); testar com escala 200%: o cronômetro pode quebrar para 2 linhas, os
  botões não.

## 6. Formas

Redondas e macias, coerentes com o ícone (anel). Escala em `token:shape.scale` (4 / 8 / 12 / 20 / 28 dp).

| Componente | Token | Forma |
| --- | --- | --- |
| Botão Gravar (parado) | `shape.component.botaoGravar` | Círculo 96 dp |
| Botão Parar (gravando) | `shape.component.botaoParar` | Quadrado 96 dp com canto 28 — o círculo **morfa** para ele |
| Pausar / Marcar | `shape.component.botaoTransporte` | Círculo 72 dp |
| Faixa de estado | `shape.component.faixaEstado` | Pílula |
| Bandeira do marcador | `shape.component.bandeiraMarcador` | Retângulo canto 6 com número |
| Cartões, mini-player | `shape.component.cartao` | Canto 16 |
| Folha inferior, diálogos | `shape.component.folhaInferior` | Canto 28 (topo) |
| Widget | `shape.component.widget` | Canto 20 (ou o raio do sistema no Android 12+) |

## 7. Ícones

- **Marca:** anel (o botão de gravar) com três barras de voz dentro. Desenhado do zero; arquivos em `icone/`. O anel
  repete nas silhuetas: **anel vazado = parado/pausado; disco cheio = gravando**.
- **Interface:** Material Symbols estilo **Rounded** (`Icons.Rounded.*`), 24 dp, preenchido para o item ativo e
  contorno para o inativo na navegação. Ícones de estado: `FiberManualRecord` (●) gravando, `Pause` pausado,
  `Stop` parar, `Flag` marcador, `Lock` cofre, `Warning` VU alto demais.
- Ícone sozinho nunca é rótulo: todo botão de ícone tem texto visível embaixo (tela Gravar) ou `contentDescription`
  (barras compactas). A tesoura com dois sentidos (heurística 2a) ganha texto: "Trecho".

## 8. Movimento

Durações e curvas do M3 em `token:motion.duration` e `token:motion.easing`. Movimentos com sentido em
`token:motion.semantic`:

| Momento | Token | Comportamento |
| --- | --- | --- |
| Começar a gravar | `morfarBotao` (300 ms, emphasizedDecelerate) | Círculo petróleo vira quadrado vermelho; ícone ● vira ■ |
| Faixa de estado | `faixaEstadoEntra` / `faixaEstadoSai` | Desce do topo; sobe ao parar |
| Gravando | `pulsoGravando` (1,6 s) | Anel sai do ● e some; o ● fica parado (o pulso é enfeite, não o sinal) |
| Pausado | `piscarPausado` (1 s) | Cronômetro vai de 100% a 45% de opacidade e volta; nunca some |
| VU | `vuSubida` / `vuQueda` / `vuPicoRetido` | Sobe rápido, cai devagar, pico fica 1 s |
| Marcar | `confirmarMarcador` (400 ms) | Bandeira numerada cresce + vibração curta |

Com "Remover animações" ligado no sistema, nada pulsa nem pisca; o estado continua dito por texto, ícone e cor.

## 9. Estados — matriz rápida

| Estado | Faixa | Botão central | Cronômetro | VU | Notificação | Widget | Bloco |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Pronto | — | ● Gravar, círculo, `pronto.container` | 00:00, `onSurfaceVariant` | Trilho | — | ● Gravar | Inativo, `ic_tile_gravar` |
| Gravando | ● GRAVANDO, `gravando.container` | ■ Parar, quadrado, `gravando.container` | Correndo, `onSurface` | 3 zonas | Colorida, `ic_stat_gravando` | Fundo `gravando.container`, ■ Parar, tempo | Ativo, `ic_tile_gravando`, "Gravando" |
| Pausado | ❚❚ PAUSADA, `pausado.container` | ■ Parar + ▶ Retomar | Respirando | Trilho | `ic_stat_pausado`, "Gravação pausada" | Fundo `pausado.container`, ▶ Retomar | Ativo, "Pausada" |
| Salvando | "Salvando…" em `surfaceContainerHigh` | Desabilitado com indicador | Congelado | Trilho | — | — | — |
| Erro | Cartão `errorContainer` com causa + ação | ● Gravar | — | — | — | ● Gravar | Inativo |

## 10. Avaliação heurística da identidade (Nielsen)

Inspeção da proposta, não das telas atuais. Severidade 0–4. Julgamento de especialista, sem usuários.

| Heurística | Como a identidade responde | Risco restante | Sev. |
| --- | --- | --- | --- |
| 1 Visibilidade do status | Faixa de estado em toda tela enquanto grava; 5 sinais redundantes (D3); marcador confirma com bandeira numerada + vibração + texto na notificação | Fabricantes que ignoram `setColorized` | 1 |
| 2 Mundo real | Anel = botão de gravar; ■ = parar; ❚❚ = pausa; cadeado = cofre | Petróleo no botão Gravar quebra a convenção do vermelho (D2) | 2 |
| 3 Controle e liberdade | Parar e Pausar sempre visíveis e grandes; excluir com DESFAZER | — | 0 |
| 4 Consistência | Um token por significado: vermelho = gravando/destrutivo; ameixa = cofre; âmbar = pausado | Com cor dinâmica ligada, o primário pode ficar parecido com um semântico | 1 |
| 5 Prevenção de erros | Formato bloqueado durante a gravação (chips desabilitados + texto); VU "Alto demais" antes de estragar o áudio | — (aviso pelo pico ≥ −1 dBFS) | 1 |
| 6 Reconhecer, não lembrar | Rótulos de texto sob todos os botões da tela Gravar; chip "Etiquetas (n)" | — | 1 |
| 7 Eficiência | Uma mão: botões na metade de baixo; widget e bloco com estado | — (bloco e widget com estado real) | 1 |
| 8 Estético e minimalista | Tela Gravar com 1 decisão por estado; o resto em Ajustes | — | 0 |
| 9 Recuperar de erros | Cartão de erro com causa + ação (tom de voz) | Textos atuais precisam ser reescritos | 2 |
| 10 Ajuda | Texto curto de onde a gravação fica (público × cofre) | — | 1 |

**Cinco dimensões.** Aprendizado: botão único e rótulos visíveis. Eficiência: gravar em 1 toque do widget, do bloco ou do
atalho. Retenção: mesmos ícones e cores em todas as superfícies. Erros: estados impossíveis de confundir, desfazer.
Satisfação: visual calmo de instrumento, sem ruído; a ser medido no teste de 5 segundos.

## 11. Arquitetura de informação

Mantida a navegação de 3 destinos (`docs/ux/04-navegacao.md`), com rótulos já testados: **Gravar · Gravações · Ajustes**.
Mudanças propostas:

- **Faixa de estado global:** enquanto há gravação, a faixa aparece também em Gravações e Ajustes (toque leva à tela
  Gravar). Hoje quem sai da tela Gravar perde o status.
- **Player:** continua como painel na lista (mini-player → painel). A hierarquia do painel vira 3 níveis: (1) onda e
  transporte, (2) chips de ajuste numa linha que rola, (3) marcadores. Isso resolve a linha de chips cortada (heurística
  8).
- **Ajustes:** quatro seções com rótulos fixos: Gravação · Aparência (nova: "Tema" e "Paleta": Gravasom, Mixlirous, Cores do sistema) · Privacidade ·
  Sobre.

## 12. Tabela de contraste (todos os pares de tokens)

Calculada pelo script que gerou `tokens.json` (mesmos valores de `contrast.pares`). "informativo" = não exigido pelo
WCAG (container grande cujo texto interno já passa). Linhas `mixlirous-*` = paleta opcional Mixlirous.

| Tema | Par (frente / fundo) | Frente | Fundo | Razão | Mínimo | OK |
| --- | --- | --- | --- | --- | --- | --- |
| light | `onPrimary / primary` | #FFFFFF | #006877 | 6,47:1 | 4,50:1 | sim |
| light | `onPrimaryContainer / primaryContainer` | #004E5A | #A5EEFF | 7,27:1 | 4,50:1 | sim |
| light | `onSecondary / secondary` | #FFFFFF | #4E6267 | 6,43:1 | 4,50:1 | sim |
| light | `onSecondaryContainer / secondaryContainer` | #374A4F | #D1E6EC | 7,20:1 | 4,50:1 | sim |
| light | `onTertiary / tertiary` | #FFFFFF | #785177 | 6,48:1 | 4,50:1 | sim |
| light | `onTertiaryContainer / tertiaryContainer` | #5E3A5E | #FFD6FB | 7,25:1 | 4,50:1 | sim |
| light | `onBackground / background` | #181C1D | #F6FAFB | 16,35:1 | 4,50:1 | sim |
| light | `onSurface / surface` | #181C1D | #F6FAFB | 16,35:1 | 4,50:1 | sim |
| light | `onSurfaceVariant / surfaceVariant` | #3E494B | #D9E4E8 | 7,18:1 | 4,50:1 | sim |
| light | `onSurfaceVariant / surface` | #3E494B | #F6FAFB | 8,85:1 | 4,50:1 | sim |
| light | `onError / error` | #FFFFFF | #BA1A1A | 6,46:1 | 4,50:1 | sim |
| light | `onErrorContainer / errorContainer` | #93000A | #FFDAD6 | 7,24:1 | 4,50:1 | sim |
| light | `inverseOnSurface / inverseSurface` | #EEF1F3 | #2D3132 | 11,59:1 | 4,50:1 | sim |
| light | `inversePrimary / inverseSurface` | #76D4E8 | #2D3132 | 7,74:1 | 4,50:1 | sim |
| light | `primary / surface` | #006877 | #F6FAFB | 6,16:1 | 4,50:1 | sim |
| light | `error / surface` | #BA1A1A | #F6FAFB | 6,15:1 | 4,50:1 | sim |
| light | `tertiary / surface` | #785177 | #F6FAFB | 6,16:1 | 4,50:1 | sim |
| light | `secondary / surface` | #4E6267 | #F6FAFB | 6,12:1 | 4,50:1 | sim |
| light | `onSurface / surfaceContainerLowest` | #181C1D | #FFFFFF | 17,18:1 | 4,50:1 | sim |
| light | `onSurfaceVariant / surfaceContainerLowest` | #3E494B | #FFFFFF | 9,29:1 | 4,50:1 | sim |
| light | `primary / surfaceContainerLowest` | #006877 | #FFFFFF | 6,47:1 | 4,50:1 | sim |
| light | `onSurface / surfaceContainerLow` | #181C1D | #F0F4F5 | 15,51:1 | 4,50:1 | sim |
| light | `onSurfaceVariant / surfaceContainerLow` | #3E494B | #F0F4F5 | 8,39:1 | 4,50:1 | sim |
| light | `primary / surfaceContainerLow` | #006877 | #F0F4F5 | 5,84:1 | 4,50:1 | sim |
| light | `onSurface / surfaceContainer` | #181C1D | #EBEEF0 | 14,74:1 | 4,50:1 | sim |
| light | `onSurfaceVariant / surfaceContainer` | #3E494B | #EBEEF0 | 7,98:1 | 4,50:1 | sim |
| light | `primary / surfaceContainer` | #006877 | #EBEEF0 | 5,55:1 | 4,50:1 | sim |
| light | `onSurface / surfaceContainerHigh` | #181C1D | #E5E9EA | 14,05:1 | 4,50:1 | sim |
| light | `onSurfaceVariant / surfaceContainerHigh` | #3E494B | #E5E9EA | 7,60:1 | 4,50:1 | sim |
| light | `primary / surfaceContainerHigh` | #006877 | #E5E9EA | 5,29:1 | 4,50:1 | sim |
| light | `onSurface / surfaceContainerHighest` | #181C1D | #DFE3E4 | 13,29:1 | 4,50:1 | sim |
| light | `onSurfaceVariant / surfaceContainerHighest` | #3E494B | #DFE3E4 | 7,19:1 | 4,50:1 | sim |
| light | `primary / surfaceContainerHighest` | #006877 | #DFE3E4 | 5,00:1 | 4,50:1 | sim |
| light | `onSurface / surfaceDim` | #181C1D | #D7DBDC | 12,31:1 | 4,50:1 | sim |
| light | `onSurfaceVariant / surfaceDim` | #3E494B | #D7DBDC | 6,66:1 | 4,50:1 | sim |
| light | `primary / surfaceDim` | #006877 | #D7DBDC | 4,64:1 | 4,50:1 | sim |
| light | `onSurface / surfaceBright` | #181C1D | #F6FAFB | 16,35:1 | 4,50:1 | sim |
| light | `onSurfaceVariant / surfaceBright` | #3E494B | #F6FAFB | 8,85:1 | 4,50:1 | sim |
| light | `primary / surfaceBright` | #006877 | #F6FAFB | 6,16:1 | 4,50:1 | sim |
| light | `outline / surface (borda)` | #6E797C | #F6FAFB | 4,26:1 | 3,00:1 | sim |
| light | `outline / surfaceContainerHighest (borda)` | #6E797C | #DFE3E4 | 3,46:1 | 3,00:1 | sim |
| light | `semantic.gravando.onBase / base` | #FFFFFF | #BA1A1A | 6,46:1 | 4,50:1 | sim |
| light | `semantic.gravando.onContainer / container` | #FFFFFF | #BA1A1A | 6,46:1 | 4,50:1 | sim |
| light | `semantic.gravando.base / surface (ícone)` | #BA1A1A | #F6FAFB | 6,15:1 | 3,00:1 | sim |
| light | `semantic.gravando.container / surface (faixa)` | #BA1A1A | #F6FAFB | 6,15:1 | 3,00:1 | sim |
| light | `semantic.pausado.onBase / base` | #FFFFFF | #926000 | 5,39:1 | 4,50:1 | sim |
| light | `semantic.pausado.onContainer / container` | #291800 | #FFCC85 | 11,62:1 | 4,50:1 | sim |
| light | `semantic.pausado.base / surface (ícone)` | #926000 | #F6FAFB | 5,13:1 | 3,00:1 | sim |
| light | `semantic.pausado.container / surface (faixa)` | #FFCC85 | #F6FAFB | 1,40:1 | informativo | sim |
| light | `semantic.cofre.onBase / base` | #FFFFFF | #785177 | 6,48:1 | 4,50:1 | sim |
| light | `semantic.cofre.onContainer / container` | #5E3A5E | #FFD6FB | 7,25:1 | 4,50:1 | sim |
| light | `semantic.cofre.base / surface (ícone)` | #785177 | #F6FAFB | 6,16:1 | 3,00:1 | sim |
| light | `semantic.cofre.container / surface (faixa)` | #FFD6FB | #F6FAFB | 1,23:1 | informativo | sim |
| light | `semantic.pronto.onBase / base` | #FFFFFF | #006877 | 6,47:1 | 4,50:1 | sim |
| light | `semantic.pronto.onContainer / container` | #004E5A | #A5EEFF | 7,27:1 | 4,50:1 | sim |
| light | `semantic.pronto.base / surface (ícone)` | #006877 | #F6FAFB | 6,16:1 | 3,00:1 | sim |
| light | `semantic.pronto.container / surface (faixa)` | #A5EEFF | #F6FAFB | 1,23:1 | informativo | sim |
| light | `semantic.vu.baixo / vu.trilho (segmento)` | #6F797B | #DFE3E4 | 3,46:1 | 3,00:1 | sim |
| light | `semantic.vu.baixo / surface (segmento)` | #6F797B | #F6FAFB | 4,26:1 | 3,00:1 | sim |
| light | `semantic.vu.ok / vu.trilho (segmento)` | #007A54 | #DFE3E4 | 4,15:1 | 3,00:1 | sim |
| light | `semantic.vu.ok / surface (segmento)` | #007A54 | #F6FAFB | 5,11:1 | 3,00:1 | sim |
| light | `semantic.vu.clipando / vu.trilho (segmento)` | #BF5A00 | #DFE3E4 | 3,47:1 | 3,00:1 | sim |
| light | `semantic.vu.clipando / surface (segmento)` | #BF5A00 | #F6FAFB | 4,27:1 | 3,00:1 | sim |
| light | `semantic.onda.barra / onda.fundo` | #6E797C | #F0F4F5 | 4,04:1 | 3,00:1 | sim |
| light | `semantic.onda.tocada / onda.fundo` | #006877 | #F0F4F5 | 5,84:1 | 3,00:1 | sim |
| light | `semantic.onda.cursor / onda.fundo` | #181C1D | #F0F4F5 | 15,51:1 | 3,00:1 | sim |
| light | `semantic.onda.alca / onda.fundo` | #4E6267 | #F0F4F5 | 5,80:1 | 3,00:1 | sim |
| light | `marker.ambar.fill / onda.fundo` | #8D6300 | #F0F4F5 | 4,84:1 | 3,00:1 | sim |
| light | `marker.ambar.onFill / fill` | #FFFFFF | #8D6300 | 5,36:1 | 4,50:1 | sim |
| light | `marker.ambar.onContainer / container` | #271900 | #FFDEAB | 13,29:1 | 4,50:1 | sim |
| light | `marker.verde.fill / onda.fundo` | #327932 | #F0F4F5 | 4,84:1 | 3,00:1 | sim |
| light | `marker.verde.onFill / fill` | #FFFFFF | #327932 | 5,36:1 | 4,50:1 | sim |
| light | `marker.verde.onContainer / container` | #002203 | #A9F59F | 13,21:1 | 4,50:1 | sim |
| light | `marker.azul.fill / onda.fundo` | #1A6DB5 | #F0F4F5 | 4,86:1 | 3,00:1 | sim |
| light | `marker.azul.onFill / fill` | #FFFFFF | #1A6DB5 | 5,39:1 | 4,50:1 | sim |
| light | `marker.azul.onContainer / container` | #001C37 | #D2E4FF | 13,33:1 | 4,50:1 | sim |
| light | `marker.roxo.fill / onda.fundo` | #755BB5 | #F0F4F5 | 4,84:1 | 3,00:1 | sim |
| light | `marker.roxo.onFill / fill` | #FFFFFF | #755BB5 | 5,36:1 | 4,50:1 | sim |
| light | `marker.roxo.onContainer / container` | #23005C | #E9DDFF | 13,26:1 | 4,50:1 | sim |
| light | `marker.rosa.fill / onda.fundo` | #AF4282 | #F0F4F5 | 4,83:1 | 3,00:1 | sim |
| light | `marker.rosa.onFill / fill` | #FFFFFF | #AF4282 | 5,35:1 | 4,50:1 | sim |
| light | `marker.rosa.onContainer / container` | #3C0028 | #FFD8E8 | 13,28:1 | 4,50:1 | sim |
| light | `marker.grafite.fill / onda.fundo` | #686B6C | #F0F4F5 | 4,85:1 | 3,00:1 | sim |
| light | `marker.grafite.onFill / fill` | #FFFFFF | #686B6C | 5,37:1 | 4,50:1 | sim |
| light | `marker.grafite.onContainer / container` | #191C1D | #E1E3E4 | 13,31:1 | 4,50:1 | sim |
| dark | `onPrimary / primary` | #00363F | #76D4E8 | 7,72:1 | 4,50:1 | sim |
| dark | `onPrimaryContainer / primaryContainer` | #A5EEFF | #004E5A | 7,27:1 | 4,50:1 | sim |
| dark | `onSecondary / secondary` | #203338 | #B5CAD0 | 7,74:1 | 4,50:1 | sim |
| dark | `onSecondaryContainer / secondaryContainer` | #D1E6EC | #374A4F | 7,20:1 | 4,50:1 | sim |
| dark | `onTertiary / tertiary` | #462447 | #E7B8E4 | 7,72:1 | 4,50:1 | sim |
| dark | `onTertiaryContainer / tertiaryContainer` | #FFD6FB | #5E3A5E | 7,25:1 | 4,50:1 | sim |
| dark | `onBackground / background` | #DFE3E4 | #101415 | 14,34:1 | 4,50:1 | sim |
| dark | `onSurface / surface` | #DFE3E4 | #101415 | 14,34:1 | 4,50:1 | sim |
| dark | `onSurfaceVariant / surfaceVariant` | #BDC8CB | #3E494B | 5,44:1 | 4,50:1 | sim |
| dark | `onSurfaceVariant / surface` | #BDC8CB | #101415 | 10,85:1 | 4,50:1 | sim |
| dark | `onError / error` | #690005 | #FFB4AB | 7,72:1 | 4,50:1 | sim |
| dark | `onErrorContainer / errorContainer` | #FFDAD6 | #93000A | 7,24:1 | 4,50:1 | sim |
| dark | `inverseOnSurface / inverseSurface` | #2D3132 | #DFE3E4 | 10,17:1 | 4,50:1 | sim |
| dark | `inversePrimary / inverseSurface` | #006877 | #DFE3E4 | 5,00:1 | 4,50:1 | sim |
| dark | `primary / surface` | #76D4E8 | #101415 | 10,90:1 | 4,50:1 | sim |
| dark | `error / surface` | #FFB4AB | #101415 | 10,92:1 | 4,50:1 | sim |
| dark | `tertiary / surface` | #E7B8E4 | #101415 | 10,92:1 | 4,50:1 | sim |
| dark | `secondary / surface` | #B5CAD0 | #101415 | 10,88:1 | 4,50:1 | sim |
| dark | `onSurface / surfaceContainerLowest` | #DFE3E4 | #0A0F10 | 14,92:1 | 4,50:1 | sim |
| dark | `onSurfaceVariant / surfaceContainerLowest` | #BDC8CB | #0A0F10 | 11,29:1 | 4,50:1 | sim |
| dark | `primary / surfaceContainerLowest` | #76D4E8 | #0A0F10 | 11,35:1 | 4,50:1 | sim |
| dark | `onSurface / surfaceContainerLow` | #DFE3E4 | #181C1D | 13,29:1 | 4,50:1 | sim |
| dark | `onSurfaceVariant / surfaceContainerLow` | #BDC8CB | #181C1D | 10,05:1 | 4,50:1 | sim |
| dark | `primary / surfaceContainerLow` | #76D4E8 | #181C1D | 10,10:1 | 4,50:1 | sim |
| dark | `onSurface / surfaceContainer` | #DFE3E4 | #1C2022 | 12,70:1 | 4,50:1 | sim |
| dark | `onSurfaceVariant / surfaceContainer` | #BDC8CB | #1C2022 | 9,61:1 | 4,50:1 | sim |
| dark | `primary / surfaceContainer` | #76D4E8 | #1C2022 | 9,66:1 | 4,50:1 | sim |
| dark | `onSurface / surfaceContainerHigh` | #DFE3E4 | #262B2C | 11,09:1 | 4,50:1 | sim |
| dark | `onSurfaceVariant / surfaceContainerHigh` | #BDC8CB | #262B2C | 8,39:1 | 4,50:1 | sim |
| dark | `primary / surfaceContainerHigh` | #76D4E8 | #262B2C | 8,44:1 | 4,50:1 | sim |
| dark | `onSurface / surfaceContainerHighest` | #DFE3E4 | #313537 | 9,58:1 | 4,50:1 | sim |
| dark | `onSurfaceVariant / surfaceContainerHighest` | #BDC8CB | #313537 | 7,25:1 | 4,50:1 | sim |
| dark | `primary / surfaceContainerHighest` | #76D4E8 | #313537 | 7,29:1 | 4,50:1 | sim |
| dark | `onSurface / surfaceDim` | #DFE3E4 | #101415 | 14,34:1 | 4,50:1 | sim |
| dark | `onSurfaceVariant / surfaceDim` | #BDC8CB | #101415 | 10,85:1 | 4,50:1 | sim |
| dark | `primary / surfaceDim` | #76D4E8 | #101415 | 10,90:1 | 4,50:1 | sim |
| dark | `onSurface / surfaceBright` | #DFE3E4 | #353A3B | 8,93:1 | 4,50:1 | sim |
| dark | `onSurfaceVariant / surfaceBright` | #BDC8CB | #353A3B | 6,75:1 | 4,50:1 | sim |
| dark | `primary / surfaceBright` | #76D4E8 | #353A3B | 6,79:1 | 4,50:1 | sim |
| dark | `outline / surface (borda)` | #889396 | #101415 | 5,88:1 | 3,00:1 | sim |
| dark | `outline / surfaceContainerHighest (borda)` | #889396 | #313537 | 3,93:1 | 3,00:1 | sim |
| dark | `semantic.gravando.onBase / base` | #410002 | #FF897D | 7,45:1 | 4,50:1 | sim |
| dark | `semantic.gravando.onContainer / container` | #FFFFFF | #CC2925 | 5,37:1 | 4,50:1 | sim |
| dark | `semantic.gravando.base / surface (ícone)` | #FF897D | #101415 | 8,06:1 | 3,00:1 | sim |
| dark | `semantic.gravando.container / surface (faixa)` | #CC2925 | #101415 | 3,45:1 | 3,00:1 | sim |
| dark | `semantic.pausado.onBase / base` | #291800 | #FFB94E | 10,04:1 | 4,50:1 | sim |
| dark | `semantic.pausado.onContainer / container` | #291800 | #FFD6A1 | 12,56:1 | 4,50:1 | sim |
| dark | `semantic.pausado.base / surface (ícone)` | #FFB94E | #101415 | 10,86:1 | 3,00:1 | sim |
| dark | `semantic.pausado.container / surface (faixa)` | #FFD6A1 | #101415 | 13,58:1 | informativo | sim |
| dark | `semantic.cofre.onBase / base` | #462447 | #E7B8E4 | 7,72:1 | 4,50:1 | sim |
| dark | `semantic.cofre.onContainer / container` | #FFD6FB | #5E3A5E | 7,25:1 | 4,50:1 | sim |
| dark | `semantic.cofre.base / surface (ícone)` | #E7B8E4 | #101415 | 10,92:1 | 3,00:1 | sim |
| dark | `semantic.cofre.container / surface (faixa)` | #5E3A5E | #101415 | 1,98:1 | informativo | sim |
| dark | `semantic.pronto.onBase / base` | #00363F | #76D4E8 | 7,72:1 | 4,50:1 | sim |
| dark | `semantic.pronto.onContainer / container` | #001F25 | #A5EEFF | 13,31:1 | 4,50:1 | sim |
| dark | `semantic.pronto.base / surface (ícone)` | #76D4E8 | #101415 | 10,90:1 | 3,00:1 | sim |
| dark | `semantic.pronto.container / surface (faixa)` | #A5EEFF | #101415 | 14,36:1 | informativo | sim |
| dark | `semantic.vu.baixo / vu.trilho (segmento)` | #969FA2 | #313537 | 4,59:1 | 3,00:1 | sim |
| dark | `semantic.vu.baixo / surface (segmento)` | #969FA2 | #101415 | 6,87:1 | 3,00:1 | sim |
| dark | `semantic.vu.ok / vu.trilho (segmento)` | #60CC9C | #313537 | 6,26:1 | 3,00:1 | sim |
| dark | `semantic.vu.ok / surface (segmento)` | #60CC9C | #101415 | 9,37:1 | 3,00:1 | sim |
| dark | `semantic.vu.clipando / vu.trilho (segmento)` | #FF9651 | #313537 | 5,74:1 | 3,00:1 | sim |
| dark | `semantic.vu.clipando / surface (segmento)` | #FF9651 | #101415 | 8,59:1 | 3,00:1 | sim |
| dark | `semantic.onda.barra / onda.fundo` | #889396 | #181C1D | 5,45:1 | 3,00:1 | sim |
| dark | `semantic.onda.tocada / onda.fundo` | #76D4E8 | #181C1D | 10,10:1 | 3,00:1 | sim |
| dark | `semantic.onda.cursor / onda.fundo` | #DFE3E4 | #181C1D | 13,29:1 | 3,00:1 | sim |
| dark | `semantic.onda.alca / onda.fundo` | #B5CAD0 | #181C1D | 10,08:1 | 3,00:1 | sim |
| dark | `marker.ambar.fill / onda.fundo` | #F2AC00 | #181C1D | 8,73:1 | 3,00:1 | sim |
| dark | `marker.ambar.onFill / fill` | #271900 | #F2AC00 | 8,71:1 | 4,50:1 | sim |
| dark | `marker.ambar.onContainer / container` | #FFDEAB | #5F4100 | 7,26:1 | 4,50:1 | sim |
| dark | `marker.verde.fill / onda.fundo` | #80CA79 | #181C1D | 8,70:1 | 3,00:1 | sim |
| dark | `marker.verde.onFill / fill` | #002203 | #80CA79 | 8,65:1 | 4,50:1 | sim |
| dark | `marker.verde.onContainer / container` | #A9F59F | #035310 | 7,23:1 | 4,50:1 | sim |
| dark | `marker.azul.fill / onda.fundo` | #85BCFF | #181C1D | 8,70:1 | 3,00:1 | sim |
| dark | `marker.azul.onFill / fill` | #001C37 | #85BCFF | 8,71:1 | 4,50:1 | sim |
| dark | `marker.azul.onContainer / container` | #D2E4FF | #00497F | 7,21:1 | 4,50:1 | sim |
| dark | `marker.roxo.fill / onda.fundo` | #C4ACFF | #181C1D | 8,74:1 | 3,00:1 | sim |
| dark | `marker.roxo.onFill / fill` | #23005C | #C4ACFF | 8,72:1 | 4,50:1 | sim |
| dark | `marker.roxo.onContainer / container` | #E9DDFF | #50358E | 7,25:1 | 4,50:1 | sim |
| dark | `marker.rosa.fill / onda.fundo` | #FF98CE | #181C1D | 8,70:1 | 3,00:1 | sim |
| dark | `marker.rosa.onFill / fill` | #3C0028 | #FF98CE | 8,70:1 | 4,50:1 | sim |
| dark | `marker.rosa.onContainer / container` | #FFD8E8 | #811A5C | 7,26:1 | 4,50:1 | sim |
| dark | `marker.grafite.fill / onda.fundo` | #B7B9BA | #181C1D | 8,72:1 | 3,00:1 | sim |
| dark | `marker.grafite.onFill / fill` | #191C1D | #B7B9BA | 8,70:1 | 4,50:1 | sim |
| dark | `marker.grafite.onContainer / container` | #E1E3E4 | #444748 | 7,28:1 | 4,50:1 | sim |
| icone | `icon.foreground / icon.background` | #D5F6FF | #004E5A | 8,25:1 | 3,00:1 | sim |
| icone | `icon.foregroundAccent / icon.background` | #76D4E8 | #004E5A | 5,52:1 | 3,00:1 | sim |
| mixlirous-light | `onPrimary / primary` | #FFFFFF | #804792 | 6,49:1 | 4,50:1 | sim |
| mixlirous-light | `onPrimaryContainer / primaryContainer` | #662F79 | #FAD7FF | 7,22:1 | 4,50:1 | sim |
| mixlirous-light | `onSecondary / secondary` | #FFFFFF | #6A596C | 6,45:1 | 4,50:1 | sim |
| mixlirous-light | `onSecondaryContainer / secondaryContainer` | #514254 | #F2DCF3 | 7,22:1 | 4,50:1 | sim |
| mixlirous-light | `onTertiary / tertiary` | #FFFFFF | #0C6A65 | 6,43:1 | 4,50:1 | sim |
| mixlirous-light | `onTertiaryContainer / tertiaryContainer` | #00504C | #A2F1EA | 7,24:1 | 4,50:1 | sim |
| mixlirous-light | `onBackground / background` | #211A13 | #FFF8F4 | 16,36:1 | 4,50:1 | sim |
| mixlirous-light | `onSurface / surface` | #211A13 | #FFF8F4 | 16,36:1 | 4,50:1 | sim |
| mixlirous-light | `onSurfaceVariant / surfaceVariant` | #524436 | #F4DFCB | 7,26:1 | 4,50:1 | sim |
| mixlirous-light | `onSurfaceVariant / surface` | #524436 | #FFF8F4 | 8,93:1 | 4,50:1 | sim |
| mixlirous-light | `onError / error` | #FFFFFF | #BA1A1A | 6,46:1 | 4,50:1 | sim |
| mixlirous-light | `onErrorContainer / errorContainer` | #93000A | #FFDAD6 | 7,24:1 | 4,50:1 | sim |
| mixlirous-light | `inverseOnSurface / inverseSurface` | #FCEEE2 | #372F27 | 11,55:1 | 4,50:1 | sim |
| mixlirous-light | `inversePrimary / inverseSurface` | #EFB0FF | #372F27 | 7,71:1 | 4,50:1 | sim |
| mixlirous-light | `primary / surface` | #804792 | #FFF8F4 | 6,18:1 | 4,50:1 | sim |
| mixlirous-light | `error / surface` | #BA1A1A | #FFF8F4 | 6,15:1 | 4,50:1 | sim |
| mixlirous-light | `tertiary / surface` | #0C6A65 | #FFF8F4 | 6,12:1 | 4,50:1 | sim |
| mixlirous-light | `secondary / surface` | #6A596C | #FFF8F4 | 6,13:1 | 4,50:1 | sim |
| mixlirous-light | `onSurface / surfaceContainerLowest` | #211A13 | #FFFFFF | 17,19:1 | 4,50:1 | sim |
| mixlirous-light | `onSurfaceVariant / surfaceContainerLowest` | #524436 | #FFFFFF | 9,38:1 | 4,50:1 | sim |
| mixlirous-light | `primary / surfaceContainerLowest` | #804792 | #FFFFFF | 6,49:1 | 4,50:1 | sim |
| mixlirous-light | `onSurface / surfaceContainerLow` | #211A13 | #FFF1E6 | 15,53:1 | 4,50:1 | sim |
| mixlirous-light | `onSurfaceVariant / surfaceContainerLow` | #524436 | #FFF1E6 | 8,48:1 | 4,50:1 | sim |
| mixlirous-light | `primary / surfaceContainerLow` | #804792 | #FFF1E6 | 5,87:1 | 4,50:1 | sim |
| mixlirous-light | `onSurface / surfaceContainer` | #211A13 | #FAEBE0 | 14,75:1 | 4,50:1 | sim |
| mixlirous-light | `onSurfaceVariant / surfaceContainer` | #524436 | #FAEBE0 | 8,05:1 | 4,50:1 | sim |
| mixlirous-light | `primary / surfaceContainer` | #804792 | #FAEBE0 | 5,57:1 | 4,50:1 | sim |
| mixlirous-light | `onSurface / surfaceContainerHigh` | #211A13 | #F4E6DA | 14,06:1 | 4,50:1 | sim |
| mixlirous-light | `onSurfaceVariant / surfaceContainerHigh` | #524436 | #F4E6DA | 7,67:1 | 4,50:1 | sim |
| mixlirous-light | `primary / surfaceContainerHigh` | #804792 | #F4E6DA | 5,31:1 | 4,50:1 | sim |
| mixlirous-light | `onSurface / surfaceContainerHighest` | #211A13 | #EEE0D5 | 13,31:1 | 4,50:1 | sim |
| mixlirous-light | `onSurfaceVariant / surfaceContainerHighest` | #524436 | #EEE0D5 | 7,26:1 | 4,50:1 | sim |
| mixlirous-light | `primary / surfaceContainerHighest` | #804792 | #EEE0D5 | 5,03:1 | 4,50:1 | sim |
| mixlirous-light | `onSurface / surfaceDim` | #211A13 | #E5D8CC | 12,30:1 | 4,50:1 | sim |
| mixlirous-light | `onSurfaceVariant / surfaceDim` | #524436 | #E5D8CC | 6,71:1 | 4,50:1 | sim |
| mixlirous-light | `primary / surfaceDim` | #804792 | #E5D8CC | 4,65:1 | 4,50:1 | sim |
| mixlirous-light | `onSurface / surfaceBright` | #211A13 | #FFF8F4 | 16,36:1 | 4,50:1 | sim |
| mixlirous-light | `onSurfaceVariant / surfaceBright` | #524436 | #FFF8F4 | 8,93:1 | 4,50:1 | sim |
| mixlirous-light | `primary / surfaceBright` | #804792 | #FFF8F4 | 6,18:1 | 4,50:1 | sim |
| mixlirous-light | `outline / surface (borda)` | #857464 | #FFF8F4 | 4,27:1 | 3,00:1 | sim |
| mixlirous-light | `outline / surfaceContainerHighest (borda)` | #857464 | #EEE0D5 | 3,47:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.pronto.onBase / base` | #FFFFFF | #804792 | 6,49:1 | 4,50:1 | sim |
| mixlirous-light | `semantic.pronto.onContainer / container` | #662F79 | #FAD7FF | 7,22:1 | 4,50:1 | sim |
| mixlirous-light | `semantic.pronto.base / surface (ícone)` | #804792 | #FFF8F4 | 6,18:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.cofre.onBase / base` | #FFFFFF | #0C6A65 | 6,43:1 | 4,50:1 | sim |
| mixlirous-light | `semantic.cofre.onContainer / container` | #00504C | #A2F1EA | 7,24:1 | 4,50:1 | sim |
| mixlirous-light | `semantic.cofre.base / surface (ícone)` | #0C6A65 | #FFF8F4 | 6,12:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.gravando.base (fixo) / surface (ícone)` | #BA1A1A | #FFF8F4 | 6,15:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.gravando.container (fixo) / surface (faixa)` | #BA1A1A | #FFF8F4 | 6,15:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.pausado.base (fixo) / surface (ícone)` | #926000 | #FFF8F4 | 5,13:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.vu.baixo (fixo) / surfaceContainerHighest (trilho do tema)` | #6F797B | #EEE0D5 | 3,46:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.vu.ok (fixo) / surfaceContainerHighest (trilho do tema)` | #007A54 | #EEE0D5 | 4,16:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.vu.clipando (fixo) / surfaceContainerHighest (trilho do tema)` | #BF5A00 | #EEE0D5 | 3,48:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.vu.baixo (fixo) / surface` | #6F797B | #FFF8F4 | 4,26:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.vu.ok (fixo) / surface` | #007A54 | #FFF8F4 | 5,11:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.vu.clipando (fixo) / surface` | #BF5A00 | #FFF8F4 | 4,27:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.onda.barra / onda.fundo` | #857464 | #FFF1E6 | 4,06:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.onda.tocada / onda.fundo` | #804792 | #FFF1E6 | 5,87:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.onda.cursor / onda.fundo` | #211A13 | #FFF1E6 | 15,53:1 | 3,00:1 | sim |
| mixlirous-light | `semantic.onda.alca / onda.fundo` | #6A596C | #FFF1E6 | 5,82:1 | 3,00:1 | sim |
| mixlirous-light | `marker.ambar.fill (fixo) / onda.fundo` | #8D6300 | #FFF1E6 | 4,84:1 | 3,00:1 | sim |
| mixlirous-light | `marker.verde.fill (fixo) / onda.fundo` | #327932 | #FFF1E6 | 4,85:1 | 3,00:1 | sim |
| mixlirous-light | `marker.azul.fill (fixo) / onda.fundo` | #1A6DB5 | #FFF1E6 | 4,87:1 | 3,00:1 | sim |
| mixlirous-light | `marker.roxo.fill (fixo) / onda.fundo` | #755BB5 | #FFF1E6 | 4,84:1 | 3,00:1 | sim |
| mixlirous-light | `marker.rosa.fill (fixo) / onda.fundo` | #AF4282 | #FFF1E6 | 4,84:1 | 3,00:1 | sim |
| mixlirous-light | `marker.grafite.fill (fixo) / onda.fundo` | #686B6C | #FFF1E6 | 4,86:1 | 3,00:1 | sim |
| mixlirous-dark | `onPrimary / primary` | #4D1660 | #EFB0FF | 7,71:1 | 4,50:1 | sim |
| mixlirous-dark | `onPrimaryContainer / primaryContainer` | #FAD7FF | #662F79 | 7,22:1 | 4,50:1 | sim |
| mixlirous-dark | `onSecondary / secondary` | #3A2B3D | #D5C0D7 | 7,75:1 | 4,50:1 | sim |
| mixlirous-dark | `onSecondaryContainer / secondaryContainer` | #F2DCF3 | #514254 | 7,22:1 | 4,50:1 | sim |
| mixlirous-dark | `onTertiary / tertiary` | #003734 | #87D4CE | 7,73:1 | 4,50:1 | sim |
| mixlirous-dark | `onTertiaryContainer / tertiaryContainer` | #A2F1EA | #00504C | 7,24:1 | 4,50:1 | sim |
| mixlirous-dark | `onBackground / background` | #EEE0D5 | #18120C | 14,38:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurface / surface` | #EEE0D5 | #18120C | 14,38:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurfaceVariant / surfaceVariant` | #D7C3B0 | #524436 | 5,50:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurfaceVariant / surface` | #D7C3B0 | #18120C | 10,90:1 | 4,50:1 | sim |
| mixlirous-dark | `onError / error` | #690005 | #FFB4AB | 7,72:1 | 4,50:1 | sim |
| mixlirous-dark | `onErrorContainer / errorContainer` | #FFDAD6 | #93000A | 7,24:1 | 4,50:1 | sim |
| mixlirous-dark | `inverseOnSurface / inverseSurface` | #372F27 | #EEE0D5 | 10,17:1 | 4,50:1 | sim |
| mixlirous-dark | `inversePrimary / inverseSurface` | #804792 | #EEE0D5 | 5,03:1 | 4,50:1 | sim |
| mixlirous-dark | `primary / surface` | #EFB0FF | #18120C | 10,90:1 | 4,50:1 | sim |
| mixlirous-dark | `error / surface` | #FFB4AB | #18120C | 10,94:1 | 4,50:1 | sim |
| mixlirous-dark | `tertiary / surface` | #87D4CE | #18120C | 10,91:1 | 4,50:1 | sim |
| mixlirous-dark | `secondary / surface` | #D5C0D7 | #18120C | 10,92:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurface / surfaceContainerLowest` | #EEE0D5 | #130D07 | 14,94:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurfaceVariant / surfaceContainerLowest` | #D7C3B0 | #130D07 | 11,32:1 | 4,50:1 | sim |
| mixlirous-dark | `primary / surfaceContainerLowest` | #EFB0FF | #130D07 | 11,32:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurface / surfaceContainerLow` | #EEE0D5 | #211A13 | 13,31:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurfaceVariant / surfaceContainerLow` | #D7C3B0 | #211A13 | 10,09:1 | 4,50:1 | sim |
| mixlirous-dark | `primary / surfaceContainerLow` | #EFB0FF | #211A13 | 10,09:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurface / surfaceContainer` | #EEE0D5 | #251E17 | 12,73:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurfaceVariant / surfaceContainer` | #D7C3B0 | #251E17 | 9,65:1 | 4,50:1 | sim |
| mixlirous-dark | `primary / surfaceContainer` | #EFB0FF | #251E17 | 9,65:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurface / surfaceContainerHigh` | #EEE0D5 | #302921 | 11,10:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurfaceVariant / surfaceContainerHigh` | #D7C3B0 | #302921 | 8,41:1 | 4,50:1 | sim |
| mixlirous-dark | `primary / surfaceContainerHigh` | #EFB0FF | #302921 | 8,41:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurface / surfaceContainerHighest` | #EEE0D5 | #3B332C | 9,59:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurfaceVariant / surfaceContainerHighest` | #D7C3B0 | #3B332C | 7,27:1 | 4,50:1 | sim |
| mixlirous-dark | `primary / surfaceContainerHighest` | #EFB0FF | #3B332C | 7,27:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurface / surfaceDim` | #EEE0D5 | #18120C | 14,38:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurfaceVariant / surfaceDim` | #D7C3B0 | #18120C | 10,90:1 | 4,50:1 | sim |
| mixlirous-dark | `primary / surfaceDim` | #EFB0FF | #18120C | 10,90:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurface / surfaceBright` | #EEE0D5 | #403830 | 8,90:1 | 4,50:1 | sim |
| mixlirous-dark | `onSurfaceVariant / surfaceBright` | #D7C3B0 | #403830 | 6,75:1 | 4,50:1 | sim |
| mixlirous-dark | `primary / surfaceBright` | #EFB0FF | #403830 | 6,75:1 | 4,50:1 | sim |
| mixlirous-dark | `outline / surface (borda)` | #9F8E7D | #18120C | 5,87:1 | 3,00:1 | sim |
| mixlirous-dark | `outline / surfaceContainerHighest (borda)` | #9F8E7D | #3B332C | 3,92:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.pronto.onBase / base` | #4D1660 | #EFB0FF | 7,71:1 | 4,50:1 | sim |
| mixlirous-dark | `semantic.pronto.onContainer / container` | #330045 | #FAD7FF | 13,23:1 | 4,50:1 | sim |
| mixlirous-dark | `semantic.pronto.base / surface (ícone)` | #EFB0FF | #18120C | 10,90:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.cofre.onBase / base` | #003734 | #87D4CE | 7,73:1 | 4,50:1 | sim |
| mixlirous-dark | `semantic.cofre.onContainer / container` | #A2F1EA | #00504C | 7,24:1 | 4,50:1 | sim |
| mixlirous-dark | `semantic.cofre.base / surface (ícone)` | #87D4CE | #18120C | 10,91:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.gravando.base (fixo) / surface (ícone)` | #FF897D | #18120C | 8,07:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.gravando.container (fixo) / surface (faixa)` | #CC2925 | #18120C | 3,46:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.pausado.base (fixo) / surface (ícone)` | #FFB94E | #18120C | 10,88:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.vu.baixo (fixo) / surfaceContainerHighest (trilho do tema)` | #969FA2 | #3B332C | 4,59:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.vu.ok (fixo) / surfaceContainerHighest (trilho do tema)` | #60CC9C | #3B332C | 6,26:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.vu.clipando (fixo) / surfaceContainerHighest (trilho do tema)` | #FF9651 | #3B332C | 5,74:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.vu.baixo (fixo) / surface` | #969FA2 | #18120C | 6,88:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.vu.ok (fixo) / surface` | #60CC9C | #18120C | 9,39:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.vu.clipando (fixo) / surface` | #FF9651 | #18120C | 8,61:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.onda.barra / onda.fundo` | #9F8E7D | #211A13 | 5,43:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.onda.tocada / onda.fundo` | #EFB0FF | #211A13 | 10,09:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.onda.cursor / onda.fundo` | #EEE0D5 | #211A13 | 13,31:1 | 3,00:1 | sim |
| mixlirous-dark | `semantic.onda.alca / onda.fundo` | #D5C0D7 | #211A13 | 10,11:1 | 3,00:1 | sim |
| mixlirous-dark | `marker.ambar.fill (fixo) / onda.fundo` | #F2AC00 | #211A13 | 8,74:1 | 3,00:1 | sim |
| mixlirous-dark | `marker.verde.fill (fixo) / onda.fundo` | #80CA79 | #211A13 | 8,71:1 | 3,00:1 | sim |
| mixlirous-dark | `marker.azul.fill (fixo) / onda.fundo` | #85BCFF | #211A13 | 8,70:1 | 3,00:1 | sim |
| mixlirous-dark | `marker.roxo.fill (fixo) / onda.fundo` | #C4ACFF | #211A13 | 8,75:1 | 3,00:1 | sim |
| mixlirous-dark | `marker.rosa.fill (fixo) / onda.fundo` | #FF98CE | #211A13 | 8,70:1 | 3,00:1 | sim |
| mixlirous-dark | `marker.grafite.fill (fixo) / onda.fundo` | #B7B9BA | #211A13 | 8,73:1 | 3,00:1 | sim |

## 13. Estados sem cor (simulação de daltonismo)

Os estados precisam se distinguir pela **claridade**, não só pelo matiz. Simulação de Machado et al. (2009) e acromatopsia
sobre os containers. Mínimo 3:1 nos pares estado × estado.

| Tema | Visão | Comparação | A | B | Razão | Mínimo | OK |
| --- | --- | --- | --- | --- | --- | --- | --- |
| light | normal | gravando.container × pausado.container | #BA1A1A | #FFCC85 | 4,38:1 | 3,00:1 | sim |
| light | normal | gravando.container × surface | #BA1A1A | #F6FAFB | 6,15:1 | informativo | — |
| light | normal | gravando.container × pronto.container (botão Gravar parado) | #BA1A1A | #A5EEFF | 5,01:1 | 3,00:1 | sim |
| light | protanopia | gravando.container × pausado.container | #524817 | #DFCD7F | 5,73:1 | 3,00:1 | sim |
| light | protanopia | gravando.container × surface | #524817 | #F9FAFB | 8,74:1 | informativo | — |
| light | protanopia | gravando.container × pronto.container (botão Gravar parado) | #524817 | #E1E9FF | 7,53:1 | 3,00:1 | sim |
| light | deuteranopia | gravando.container × pausado.container | #786A0F | #EBD987 | 3,83:1 | 3,00:1 | sim |
| light | deuteranopia | gravando.container × surface | #786A0F | #F8F9FB | 5,15:1 | informativo | — |
| light | deuteranopia | gravando.container × pronto.container (botão Gravar parado) | #786A0F | #D2DEFF | 4,04:1 | 3,00:1 | sim |
| light | tritanopia | gravando.container × pausado.container | #CE001D | #FFBFBA | 3,68:1 | 3,00:1 | sim |
| light | tritanopia | gravando.container × surface | #CE001D | #F5FAFA | 5,47:1 | informativo | — |
| light | tritanopia | gravando.container × pronto.container (botão Gravar parado) | #CE001D | #83F5F3 | 4,48:1 | 3,00:1 | sim |
| light | acromatopsia | gravando.container × pausado.container | #5E5E5E | #D4D4D4 | 4,37:1 | 3,00:1 | sim |
| light | acromatopsia | gravando.container × surface | #5E5E5E | #F9F9F9 | 6,16:1 | informativo | — |
| light | acromatopsia | gravando.container × pronto.container (botão Gravar parado) | #5E5E5E | #E2E2E2 | 5,01:1 | 3,00:1 | sim |
| dark | normal | gravando.container × pausado.container | #CC2925 | #FFD6A1 | 3,93:1 | 3,00:1 | sim |
| dark | normal | gravando.container × surface | #CC2925 | #101415 | 3,45:1 | informativo | — |
| dark | normal | gravando.container × pronto.container (botão Gravar parado) | #CC2925 | #A5EEFF | 4,16:1 | 3,00:1 | sim |
| dark | protanopia | gravando.container × pausado.container | #5E5422 | #E5D79D | 5,25:1 | 3,00:1 | sim |
| dark | protanopia | gravando.container × surface | #5E5422 | #131415 | 2,43:1 | informativo | — |
| dark | protanopia | gravando.container × pronto.container (botão Gravar parado) | #5E5422 | #E1E9FF | 6,25:1 | 3,00:1 | sim |
| dark | deuteranopia | gravando.container × pausado.container | #86771C | #EFE1A2 | 3,42:1 | 3,00:1 | sim |
| dark | deuteranopia | gravando.container × surface | #86771C | #121315 | 4,13:1 | informativo | — |
| dark | deuteranopia | gravando.container × pronto.container (botão Gravar parado) | #86771C | #D2DEFF | 3,35:1 | 3,00:1 | sim |
| dark | tritanopia | gravando.container × pausado.container | #E1002B | #FFCBC8 | 3,45:1 | 3,00:1 | sim |
| dark | tritanopia | gravando.container × surface | #E1002B | #0F1414 | 3,75:1 | informativo | — |
| dark | tritanopia | gravando.container × pronto.container (botão Gravar parado) | #E1002B | #83F5F3 | 3,85:1 | 3,00:1 | sim |
| dark | acromatopsia | gravando.container × pausado.container | #6A6A6A | #DCDCDC | 3,94:1 | 3,00:1 | sim |
| dark | acromatopsia | gravando.container × surface | #6A6A6A | #131313 | 3,44:1 | informativo | — |
| dark | acromatopsia | gravando.container × pronto.container (botão Gravar parado) | #6A6A6A | #E2E2E2 | 4,18:1 | 3,00:1 | sim |
| mixlirous-light | normal | gravando.container × pronto.container (Mixlirous) | #BA1A1A | #FAD7FF | 4,99:1 | 3,00:1 | sim |
| mixlirous-light | normal | gravando.container × primary (Mixlirous) | #BA1A1A | #804792 | 1,01:1 | informativo | — |
| mixlirous-light | protanopia | gravando.container × pronto.container (Mixlirous) | #524817 | #D4E0FF | 6,92:1 | 3,00:1 | sim |
| mixlirous-light | protanopia | gravando.container × primary (Mixlirous) | #524817 | #395A95 | 1,34:1 | informativo | — |
| mixlirous-light | deuteranopia | gravando.container × pronto.container (Mixlirous) | #786A0F | #DBE4FE | 4,28:1 | 3,00:1 | sim |
| mixlirous-light | deuteranopia | gravando.container × primary (Mixlirous) | #786A0F | #4A6090 | 1,15:1 | informativo | — |
| mixlirous-light | tritanopia | gravando.container × pronto.container (Mixlirous) | #CE001D | #FCDBE4 | 4,50:1 | 3,00:1 | sim |
| mixlirous-light | tritanopia | gravando.container × primary (Mixlirous) | #CE001D | #805266 | 1,10:1 | informativo | — |
| mixlirous-light | acromatopsia | gravando.container × pronto.container (Mixlirous) | #5E5E5E | #E2E2E2 | 5,01:1 | 3,00:1 | sim |
| mixlirous-light | acromatopsia | gravando.container × primary (Mixlirous) | #5E5E5E | #5E5E5E | 1,00:1 | informativo | — |
| mixlirous-dark | normal | gravando.container × pronto.container (Mixlirous) | #CC2925 | #FAD7FF | 4,15:1 | 3,00:1 | sim |
| mixlirous-dark | normal | gravando.container × primary (Mixlirous) | #CC2925 | #EFB0FF | 3,15:1 | informativo | — |
| mixlirous-dark | protanopia | gravando.container × pronto.container (Mixlirous) | #5E5422 | #D4E0FF | 5,75:1 | 3,00:1 | sim |
| mixlirous-dark | protanopia | gravando.container × primary (Mixlirous) | #5E5422 | #A6C2FF | 4,26:1 | informativo | — |
| mixlirous-dark | deuteranopia | gravando.container × pronto.container (Mixlirous) | #86771C | #DBE4FE | 3,54:1 | 3,00:1 | sim |
| mixlirous-dark | deuteranopia | gravando.container × primary (Mixlirous) | #86771C | #B5C9FC | 2,73:1 | informativo | — |
| mixlirous-dark | tritanopia | gravando.container × pronto.container (Mixlirous) | #E1002B | #FCDBE4 | 3,87:1 | 3,00:1 | sim |
| mixlirous-dark | tritanopia | gravando.container × primary (Mixlirous) | #E1002B | #F0B9CD | 2,96:1 | informativo | — |
| mixlirous-dark | acromatopsia | gravando.container × pronto.container (Mixlirous) | #6A6A6A | #E2E2E2 | 4,18:1 | 3,00:1 | sim |
| mixlirous-dark | acromatopsia | gravando.container × primary (Mixlirous) | #6A6A6A | #C6C6C6 | 3,17:1 | informativo | — |
