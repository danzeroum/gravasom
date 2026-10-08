# Decisões de design — identidade visual

Cada decisão segue o formato dos ADRs do projeto: contexto, opções, decisão, trade-off. Os valores citados estão em
[`tokens.json`](tokens.json); a explicação visual está em [`identidade.md`](identidade.md) e as telas em [`telas.md`](telas.md).

Estado: **proposta**, ainda sem teste com usuários (ver "Como validar" no fim).

---

## D1 — Cor dinâmica: cores próprias por padrão, dinâmica como opção em Ajustes

**Contexto.** Hoje (`core/ui/GravasomTheme.kt`) o Android 12+ usa `dynamicLight/DarkColorScheme` e o app não tem cor
própria. Abaixo do Android 12, o primário é o vermelho `#B3261E`, o mesmo do estado "gravando". Duas consequências:

1. Se o papel de parede for vermelho ou rosa, a cor dinâmica gera um primário avermelhado e todo botão comum passa a
   parecer "gravando".
2. No Android 8–11 a interface inteira é vermelha, então o vermelho deixa de sinalizar estado.

**Opções.**

| Opção | A favor | Contra |
| --- | --- | --- |
| A. Manter só a dinâmica | Combina com o aparelho; zero esforço | Sem identidade; contraste dos papéis fica com o sistema; primário pode ficar vermelho e colidir com "gravando" |
| B. Só cores próprias | Identidade e contraste garantidos; o vermelho fica só para estado | Ignora quem gosta de Material You |
| **C. Próprias por padrão + dinâmica opcional** | Identidade e contraste garantidos de saída; quem prefere ganha a cor do sistema | Uma linha a mais em Ajustes; dois caminhos para testar |

**Decisão: C.** Ajustes ganha a escolha de **paleta**, com "Cores do sistema" como uma das opções (Android 12+, não é o padrão; ver D12). Escolhida, só
`color.scheme` é trocado pelo esquema dinâmico. `color.semantic` (gravando, pausado, pronto, cofre, VU, onda) e
`color.marker` **nunca** mudam.

**Trade-off aceito.** Com a dinâmica ligada, o contraste dos papéis comuns (primary, surface…) passa a depender do sistema e
não está na tabela. O que precisa ser reconhecível (gravando, pausado, cofre) continua fixo e medido. Custo: testar os dois
caminhos nos testes de captura (claro/escuro × própria/dinâmica).

## D2 — Primário petróleo; vermelho reservado

**Decisão.** Primário petróleo (`color.scheme.light.primary` = `#006877`, semente `#1E6A78`). Vermelho só aparece em:
`color.semantic.*.gravando` (estado) e `color.scheme.*.error` (ações destrutivas e erros). O botão Gravar parado **não é
vermelho**: usa `color.semantic.*.pronto`.

**Por quê.** Petróleo é calmo e confiável para um app que guarda voz e funciona offline. Fica longe do vermelho no círculo
de matizes e, com o botão parado claro, também na luminância. Assim o vermelho que aparece ao gravar é novidade na tela.

**Trade-off.** Perde-se a convenção "botão de gravar é vermelho". Compensação: o ícone do botão é um ponto ● e o rótulo é
"Gravar". Hipótese a testar: ninguém hesita mais de 1 s para achar o botão (tarefa 1 do roteiro).

## D3 — "Gravando" com cinco sinais redundantes

Cor nunca é o único sinal. Em toda superfície, gravando = **forma + ícone + texto + movimento + luminância**:

| Sinal | Tela cheia | Notificação | Widget | Bloco (tile) |
| --- | --- | --- | --- | --- |
| Texto | Faixa "● GRAVANDO" + cronômetro correndo | Título "Gravando" + cronômetro do sistema | "Gravando" + tempo | Subtítulo "Gravando" (API 29+) |
| Ícone | ● na faixa; ■ no botão | `ic_stat_gravando` (disco **cheio**) | ■ Parar | `ic_tile_gravando` (disco cheio com ■) |
| Forma | Botão muda de círculo para quadrado arredondado | Silhueta cheia × anel vazado | Botão quadrado | Disco cheio × anel |
| Movimento | Anel pulsando no ● | — (o sistema não anima) | — | — |
| Luminância | Faixa escura com texto branco × pausado claro × pronto claro | Fundo colorido (`setColorized`) | Fundo escuro vermelho × claro | Estado ativo do sistema |

A luminância foi medida com simulação de protanopia, deuteranopia, tritanopia e acromatopsia (tabela em
`identidade.md`, seção 13): gravando × pausado e gravando × pronto ficam ≥ 3:1 em todas.

## D4 — Pausado em âmbar e com luminância invertida

**Decisão.** Pausado usa `color.semantic.*.pausado` (âmbar) com ícone ❚❚, texto "PAUSADA" e cronômetro "respirando"
(`motion.semantic.piscarPausado`). A faixa de pausado é **clara com texto escuro**; a de gravando é **escura com texto
branco**. Quem não distingue vermelho de âmbar vê a inversão claro/escuro.

## D5 — Cofre = tertiary (ameixa) + cadeado

**Decisão.** O papel `tertiary` do esquema é reservado ao cofre ("Gravação privada"); `color.semantic.*.cofre` é alias dele.
Hoje a onda usa `tertiary` para os marcadores: isso muda (D6). O cofre sempre aparece com o ícone de cadeado e o texto "No
cofre".

**Trade-off.** Com a cor dinâmica ligada (D1), `tertiary` do esquema muda, mas `color.semantic.*.cofre` não: o código deve ler o
cofre de `semantic`, nunca de `colorScheme.tertiary`.

## D6 — Seis cores de marcador, nenhuma vermelha

**Decisão.** `color.marker` = âmbar, verde, azul, roxo, rosa, grafite. O enum atual (`MarkerColor`) tem `RED`: ele passa a
ser desenhado como **rosa**. A sexta cor entra no enum como `GRAPHITE` (resposta R1). A bandeira do marcador mostra o
**número** do marcador e a lista mostra o rótulo; cor é só agrupamento.

**Trade-off.** Rosa e âmbar ficam perto de cofre (ameixa) e pausado (âmbar). Não há confusão porque aparecem em lugares
diferentes (marcadores só no player e na lista de marcadores; pausado só na tela Gravar, notificação e widget; cofre só
como selo com cadeado).

## D7 — Medidor de nível (VU) segmentado com rótulo

**Decisão.** 24 segmentos (`size.vuSegmentos`) em três zonas: baixo (`vu.baixo`), ok (`vu.ok`), clipando (`vu.clipando`,
laranja, nunca vermelho). Ao lado, um rótulo de texto: "Baixo", "Bom", "Alto demais". O segmento de pico fica aceso 1 s.
As barras seguem o RMS; o aviso "Alto demais" segue o **pico ≥ −1 dBFS** (R3).

**Trade-off.** Laranja e âmbar (pausado) são parecidos, mas não aparecem juntos: o VU só fica ativo gravando; pausado, o VU
fica todo em `vu.trilho`.

## D8 — Fonte Atkinson Hyperlegible Next, empacotada

**Decisão.** Uma família só, Atkinson Hyperlegible Next (OFL 1.1, Google Fonts), desenhada pelo Braille Institute para
baixa visão. Os números do cronômetro usam algarismos tabulares (`fontFeatureSettings = "tnum"`). Os TTF vão em
`res/font`: o app não tem permissão de internet e não deve depender do provedor de fontes do Google Play.

**Trade-off.** Aumenta o APK (4 pesos estáticos). Alternativa sem custo: a fonte do sistema (fallback já previsto). Se o
APK passar da meta de 25 MB, usar só Regular e SemiBold (pergunta P6).

**Resolvido (R6):** o APK tem ~8 MB, mas o dono pediu só **Regular (400) e SemiBold (600)**. A escala inteira usa
apenas esses dois pesos (`typography.fontFamilies.texto.pesosDisponiveis`).

## D9 — Ícones de interface: Material Symbols arredondados; ícones da marca originais

**Decisão.** Ícones de interface continuam da biblioteca do Material que o app já usa (`material-icons-extended`,
Apache 2.0), no estilo **Rounded** (`Icons.Rounded.*`) para combinar com as formas. Ícone do app, da notificação e do
bloco são desenhos originais (`docs/design/icone/`). Nenhuma marca de terceiros (inclusive em "Compartilhar com WhatsApp":
usar a folha do sistema, que mostra os ícones dos apps, nunca ícones copiados).

**Resolvido (R4):** os Material Symbols (Apache 2.0) não contam como ícones de terceiros. Não há conjunto próprio.

## D10 — Widget e bloco mostram o estado real

**Decisão.** O widget tem três estados (pronto, gravando, pausado) e o bloco tem dois (inativo, ativo). Hoje nenhum dos
dois recebe o estado da gravação. **Resolvido (R2):** os dois passam a refletir o estado real (gravando, pausado,
parado); entra na implementação.

## D11 — Notificação de gravação colorida

**Decisão.** Gravando: `setColorized(true)` + `setColor(semantic.light.gravando.container)` + `ic_stat_gravando`. Pausado:
sem colorir, `setColor(semantic.light.pausado.base)` + `ic_stat_pausado`. O título sempre traz o estado em texto.

**Trade-off.** Alguns fabricantes ignoram `setColorized`. Por isso o texto e a silhueta do ícone (cheia × vazada)
carregam o estado sozinhos.

## D12 — Paleta opcional "Mixlirous"

**Contexto.** Pedido do dono: oferecer um tema com as cores da Mixlirous, a partir de duas fotos de estúdio (painéis
roxos e vermelho-neon; madeira, espuma preta e luz quente; cabos com brilho ciano).

**Decisão.** Ajustes › Aparência › Paleta passa a ter três opções: **Gravasom** (padrão), **Mixlirous** e **Cores do
sistema**. A Mixlirous (`color.themes.mixlirous`) foi gerada pelo mesmo método HCT a partir de amostras das fotos:
primário roxo dos painéis, neutros quentes de madeira e terciário ciano dos cabos, que nesse tema é o **cofre**. Tem
claro e escuro, e todos os pares passam no AA (linhas `mixlirous-*` da tabela em `identidade.md`).

**O que ficou de fora, de propósito.**
- O vermelho-neon e o rosa-avermelhado das fotos: colidem com o vermelho reservado para "gravando" (D2).
- A madeira como `secondary`: o botão Pausar usa `secondaryContainer` e ficaria parecido com o âmbar de "pausado".
- As fotos e o logotipo: o tema leva só cores. As fotos pesariam no APK e mostram equipamentos e marcas de terceiros.

**Trade-off.** Mais um esquema para manter e testar (capturas em 2 paletas × 2 temas). O primário claro da Mixlirous
(`#804792`) tem a mesma luminância do vermelho de gravando; por isso o botão Gravar parado usa o container claro
(`semantic.pronto`), e a medição sem cor continua ≥ 3,5:1 entre gravando e pronto.

**Resolvido (R9):** Mixlirous é marca do dono e aparece com esse nome em Ajustes › Aparência › Paleta.

---

## Respostas do dono (2026-10-08)

As perguntas P1–P9 foram respondidas. Cada linha diz o que muda para a implementação; os tokens já refletem as
respostas.

| # | Resposta | O que muda (para o agente de código) |
| --- | --- | --- |
| R1 | Acrescentar `GRAPHITE` ao `MarkerColor`. `RED` mantém o nome no código; na tela vira "Rosa". | Enum com 6 valores na ordem `AMBER, RED, GREEN, BLUE, PURPLE, GRAPHITE`. Cor → token por `color.marker.mapeamentoDominio`; rótulo por `color.marker.rotulos`. **`MarkerSidecar` conferido:** o arquivo `.markers` guarda só `posicaoMs<TAB>rotulo`, sem cor; os marcadores feitos durante a gravação nascem `AMBER` (padrão do domínio) ao serem ingeridos. Nada a migrar no sidecar. No Room a cor é texto lido com `runCatching { valueOf }`: valor novo não exige migração, e uma versão antiga que leia `GRAPHITE` cai em `AMBER` sem quebrar. |
| R2 | Bloco e widget refletem o estado real (gravando, pausado, parado). | Bloco: `Tile.state`, ícone, `label` e `subtitle` por estado (`telas.md` §6), atualizados a cada mudança do estado de gravação; em `onStartListening` ler o estado atual em vez de forçar "inativo". Widget: 3 estados × 2 tamanhos (`telas.md` §5), atualizado a cada mudança de estado. |
| R3 | Expor o pico no `LevelMeter`; "Alto demais" usa pico ≥ −1 dBFS. | `LevelMeter` ganha o pico em dBFS (maior \|amostra\| do bloco) ao lado do RMS, e o estado do serviço passa a publicar os dois. As barras do VU seguem o RMS; o aviso segue o pico. **Atenção ao M4A:** esse gravador lê `MediaRecorder.getMaxAmplitude()`, que já é um pico, não um RMS; ali o mesmo valor alimenta as barras e o aviso. |
| R4 | Material Symbols não contam como ícones de terceiros. | Ícones de interface continuam `Icons.Rounded.*`. |
| R5 | O seletor de cor entra no diálogo do marcador. | `telas.md` §3a. Os 6 swatches seguem `color.marker.ordem`; o padrão de um marcador novo continua `AMBER`. |
| R6 | Empacotar só Regular e SemiBold. | `res/font`: `atkinson_next_regular` (400) e `atkinson_next_semibold` (600). Pesos 500/700 removidos da escala. |
| R7 | Trocar o ícone da notificação de mídia por `ic_stat_gravasom`. | Ver `icone/README.md` (Media3 1.5.0: conferir o recurso que o `DefaultMediaNotificationProvider` usa como ícone pequeno e sobrescrevê-lo, ou definir o ícone no provider). |
| R8 | Widget 2×1 e 4×1 responsivos. | `size.widget2x1`, `size.widget4x1`, `size.widgetPontoQuebra`; layouts em `telas.md` §5. |
| R9 | "Mixlirous" é marca do dono. | Rótulo "Mixlirous" em Ajustes › Aparência › Paleta. |

## Como validar (pendente, depende do dono)

1. **Teste de 5 segundos** com 5 pessoas (inclua ao menos uma com daltonismo, ou use o filtro "Simular espaço de cor" das
   Opções do desenvolvedor do Android): mostrar por 1 s a tela Gravar, a notificação, o widget e o bloco, em ordem
   aleatória entre pronto/gravando/pausado, e perguntar "está gravando?". Meta: ≥ 95% de acerto, resposta em < 1 s.
2. **Tarefa 1 do roteiro** (`docs/ux/05-roteiro-teste.md`) com o botão Gravar em petróleo: tempo até gravar ≤ 2 s.
3. **Accessibility Scanner** nas 4 telas, claro e escuro: zero avisos de contraste e de alvo de toque.
