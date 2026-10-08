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
ser desenhado como **rosa**. A sexta cor (grafite) não existe no enum (pergunta P1). A bandeira do marcador mostra o
**número** do marcador e a lista mostra o rótulo; cor é só agrupamento.

**Trade-off.** Rosa e âmbar ficam perto de cofre (ameixa) e pausado (âmbar). Não há confusão porque aparecem em lugares
diferentes (marcadores só no player e na lista de marcadores; pausado só na tela Gravar, notificação e widget; cofre só
como selo com cadeado).

## D7 — Medidor de nível (VU) segmentado com rótulo

**Decisão.** 24 segmentos (`size.vuSegmentos`) em três zonas: baixo (`vu.baixo`), ok (`vu.ok`), clipando (`vu.clipando`,
laranja, nunca vermelho). Ao lado, um rótulo de texto: "Baixo", "Bom", "Alto demais". O segmento de pico fica aceso 1 s.

**Trade-off.** Laranja e âmbar (pausado) são parecidos, mas não aparecem juntos: o VU só fica ativo gravando; pausado, o VU
fica todo em `vu.trilho`.

## D8 — Fonte Atkinson Hyperlegible Next, empacotada

**Decisão.** Uma família só, Atkinson Hyperlegible Next (OFL 1.1, Google Fonts), desenhada pelo Braille Institute para
baixa visão. Os números do cronômetro usam algarismos tabulares (`fontFeatureSettings = "tnum"`). Os TTF vão em
`res/font`: o app não tem permissão de internet e não deve depender do provedor de fontes do Google Play.

**Trade-off.** Aumenta o APK (4 pesos estáticos). Alternativa sem custo: a fonte do sistema (fallback já previsto). Se o
APK passar da meta de 25 MB, usar só Regular e SemiBold (pergunta P6).

## D9 — Ícones de interface: Material Symbols arredondados; ícones da marca originais

**Decisão.** Ícones de interface continuam da biblioteca do Material que o app já usa (`material-icons-extended`,
Apache 2.0), no estilo **Rounded** (`Icons.Rounded.*`) para combinar com as formas. Ícone do app, da notificação e do
bloco são desenhos originais (`docs/design/icone/`). Nenhuma marca de terceiros (inclusive em "Compartilhar com WhatsApp":
usar a folha do sistema, que mostra os ícones dos apps, nunca ícones copiados).

**Ponto aberto:** P4 confirma se a biblioteca do Material conta como "ícone de terceiros" para a regra do projeto.

## D10 — Widget e bloco mostram o estado real

**Decisão.** O widget tem três estados (pronto, gravando, pausado) e o bloco tem dois (inativo, ativo). Hoje nenhum dos
dois recebe o estado da gravação (pergunta P2). O desenho em `telas.md` assume que vai receber.

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

**Ponto aberto:** P9 — confirmar que "Mixlirous" é marca do próprio projeto e pode aparecer por nome no app.

---

## Perguntas em aberto (não inventei a resposta)

| # | Pergunta | Onde apareceu | O que muda com a resposta |
| --- | --- | --- | --- |
| P1 | Podemos acrescentar `GRAPHITE` ao enum `MarkerColor` (e tratar `RED` como rosa, ou renomeá-lo)? A cor é salva como texto no Room (`MarkerEntity.color`) e lida com `runCatching { valueOf }`, então valor novo não exige migração — mas o sidecar dos marcadores (`MarkerSidecar`) precisa ser conferido. | `core/domain/.../marker/Marker.kt` tem 5 cores; o pedido fala em 6 | Se não, o seletor mostra 5 cores e `color.marker.*.grafite` fica sem uso |
| P2 | Quem avisa o bloco e o widget de que a gravação começou/parou? Hoje o bloco volta a "inativo" em `onStartListening` e o widget é estático. | `RecordTileService.kt`, `RecordWidget.kt` | Sem isso, o estado "gravando" do bloco e do widget (aceite) não aparece |
| P3 | O VU recebe só o RMS (`LevelMeter.rmsDb`). Para "clipando" o certo é o pico (amostra ≥ -1 dBFS). Podemos expor o pico? Até lá, o token usa RMS > -3 dBFS como aproximação. | `core/audio-engine/.../LevelMeter.kt` | Precisão do aviso "Alto demais" |
| P4 | Os ícones do Material (Apache 2.0, já dependência do app) contam como "ícones de terceiros"? | Regra do pedido | Se sim, desenhar um conjunto próprio (~30 ícones) |
| P5 | Hoje não existe tela para escolher a cor do marcador (todos usam `tertiary`). O seletor proposto em `telas.md` (diálogo do marcador) entra no escopo? | `PlayerDialogs.kt` | Sem seletor, todos ficam âmbar (padrão do domínio) |
| P6 | Há folga de tamanho no APK para 4 arquivos de fonte? | Meta ≤ 25 MB do README | Quantos pesos empacotar |
| P7 | A notificação de mídia do player usa o ícone padrão do Media3. Podemos trocar pelo `ic_stat_gravasom` (provavelmente sobrescrevendo o drawable `media3_notification_small_icon` — confirmar na versão 1.5.0)? | `PlaybackService.kt` | Consistência do ícone na barra de status |
| P9 | "Mixlirous" é marca sua (pode aparecer por nome em Ajustes)? Se não, a opção se chama "Estúdio" e mantém as mesmas cores. | Pedido do tema (D12) | Só o rótulo da opção |
| P8 | Quais tamanhos de widget queremos? Hoje só existe o mínimo 110 × 40 dp. Proponho 2×1 e 4×1 responsivos. | `record_widget_info.xml` | Layout do widget |

## Como validar (pendente, depende do dono)

1. **Teste de 5 segundos** com 5 pessoas (inclua ao menos uma com daltonismo, ou use o filtro "Simular espaço de cor" das
   Opções do desenvolvedor do Android): mostrar por 1 s a tela Gravar, a notificação, o widget e o bloco, em ordem
   aleatória entre pronto/gravando/pausado, e perguntar "está gravando?". Meta: ≥ 95% de acerto, resposta em < 1 s.
2. **Tarefa 1 do roteiro** (`docs/ux/05-roteiro-teste.md`) com o botão Gravar em petróleo: tempo até gravar ≤ 2 s.
3. **Accessibility Scanner** nas 4 telas, claro e escuro: zero avisos de contraste e de alvo de toque.
