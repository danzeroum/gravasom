# Avaliação heurística — Onda 1 do Gravasom

Avaliador independente. Substitui a análise anterior deste arquivo (telas da fase 0; o histórico fica no git). Esta avaliação cobre só as funções novas da Onda 1.

## 1. Escopo, método e limites

**Funções avaliadas:** waveform com zoom (pinça), marcadores com nota (até 140 caracteres, criados também pelo "Marcar" da notificação, do widget e da tela Gravar), pular silêncio (Suave/Normal), etiquetas com filtro na lista, nome automático e exportar trecho.

**Método:** inspeção por especialista com as 10 heurísticas de Nielsen, severidade 0–4, e cinco dimensões de usabilidade (aprendizado, eficiência, retenção, erros, satisfação). Cada achado traz uma origem:

- **[CAPTURA]** visto em `docs/ux/capturas/*.png`. Fonte: `01` = player 60 min com marcadores, `02` = pular silêncio, `03` = seleção de trecho, `04` = calculando onda.
- **[CÓDIGO]** inferido da leitura do código e das strings, sem ver a tela renderizada. Para esses achados, o comportamento visual real é hipótese.

Severidade: 0 não é problema, 1 cosmético, 2 menor, 3 grave, 4 catastrófico.

**Limites (leia antes de confiar nos números):**

1. As capturas vêm do Robolectric, não de emulador nem de aparelho. Têm 360x780 dp, locale inglês (o app tem pt-BR e en) e dados sintéticos. O texto misto "Aula de física" / "Pergunta da prova" com a UI em inglês **não** representa o que o usuário vê em pt-BR. Animações, fontes reais, gestos, haptics e desempenho não aparecem.
2. As capturas mostram só o painel do player. **Não existem capturas** da tela Gravar, da biblioteca, dos diálogos de marcador e de etiquetas, nem da notificação e do widget. Todo achado sobre essas telas é [CÓDIGO]. Não descrevo o visual delas.
3. **Não houve avaliação com usuários** e **não houve uso no aparelho real** (nem TalkBack, nem Accessibility Scanner, nem medição de latência). Todas as severidades são julgamento de especialista, não evidência de comportamento de usuário. A seção 6 lista o que só o dono pode validar.
4. Arquivos lidos: `feature/player` (PlayerPanel.kt, PlayerDialogs.kt, WaveformView.kt, PlayerViewModel.kt), `feature/library` (LibraryScreen.kt, PlayerDock.kt), `feature/record` (RecordScreen.kt), `core/service` (RecordingNotification.kt, RecordingService.kt, RecordWidget.kt), `core/domain` (AutoTitle.kt, ClipTitle.kt, Tags.kt) e as strings pt-BR (`values/`, que é o padrão) e en (`values-en/`). Não revisei a implementação do pular silêncio (VAD) nem a qualidade do corte exportado.

## 2. As 10 heurísticas

| # | Heurística | Sev. | Achados concretos |
|---|---|---|---|
| 1 | Visibilidade do status | **3** | (a) O "Marcar" da notificação não dá confirmação perceptível. O canal é `IMPORTANCE_LOW` e a notificação é `setOnlyAlertOnce(true)` (RecordingNotification.kt:21, 47). O único sinal é o texto "N marcadores", que só aparece quando N > 0 (linha 51) e não mostra o instante. Não há vibração nem som no código [CÓDIGO]. Quem marca com a tela bloqueada não tem como saber se funcionou. (b) O widget "Marcar" fora de gravação é ignorado em silêncio (RecordingService.kt:162-167) [CÓDIGO]. (c) Calcular a onda de 60 min mostra spinner e texto, sem estimativa e sem cancelar [CAPTURA 04]. Mark, Zoom e Pular silêncio ficam habilitados nesse estado sem efeito visível (PlayerPanel.kt:155-163) [CAPTURA 04 + CÓDIGO]. (d) Ao exportar, o botão só muda para "Exportando…" (PlayerPanel.kt:314). O resultado é um snackbar sem ação "Ver" (PlayerDock.kt:92) [CÓDIGO]. (e) Pular silêncio: não há sinal no momento do salto. As faixas puladas são riscos cinza com 12% de alpha (WaveformView.kt SKIP_ALPHA), quase invisíveis [CAPTURA 02]. O resumo "Skipped 02:34 so far" ajuda [CAPTURA 02]. |
| 2 | Correspondência com o mundo real | 2 | (a) O ícone de tesoura (ContentCut) tem dois significados: ligar o modo de seleção no transporte e "selecionar até o próximo marcador" em cada linha de marcador [CAPTURA 01, PlayerPanel.kt:230-235 e 354-356]. A explicação só existe como contentDescription. (b) A velocidade aparece como `speed.toString()` (PlayerPanel.kt:263), o que dá "1.5x" com ponto em pt-BR [CÓDIGO]. (c) Os chips de formato mostram "M4A/WAV/MP3" (RecordScreen.kt:255), jargão para Ana e Carla [CÓDIGO]. (d) "Sensibilidade: Suave/Normal" não diz o que muda (corta menos ou mais silêncio) [CAPTURA 02 + strings.xml:21-23]. |
| 3 | Controle e liberdade do usuário | **3** | (a) "Marcar" no player salva o marcador **antes** de abrir o diálogo (PlayerDock.kt:48). "Cancelar" no diálogo deixa um marcador vazio, e não desfaz [CÓDIGO]. (b) "Excluir marcador" apaga na hora, sem confirmar e sem DESFAZER (PlayerDialogs.kt:72-74, PlayerViewModel.kt:168-170), enquanto a exclusão de gravação tem DESFAZER (LibraryScreen.kt:148-150). Inconsistente [CÓDIGO]. (c) O X do player pausa e fecha (LibraryScreen.kt:84). Não há como minimizar e continuar ouvindo enquanto se navega na lista [CAPTURA 01 mostra o X + CÓDIGO]. (d) Em modo de seleção a pinça e o arrasto de rolagem ficam desligados (WaveformView.kt:153-165). A janela só muda pelos botões de zoom, e arrastar até a borda não rola [CÓDIGO]. |
| 4 | Consistência e padrões | 2 | (a) "Marcar aqui" (descrição) × "Marcar" (rótulo visível): o texto de ajuda manda tocar em "Marcar aqui", que não existe na tela [CAPTURA 04, strings.xml:34]. (b) O chip "Pular silêncio" (FilterChip) parece desabilitado ao lado de "Velocidade" e "Etiquetas" (AssistChip, texto escuro) [CAPTURA 01, 03, 04]. (c) Tocar na onda só posiciona; tocar no marcador posiciona e toca (WaveformView.kt:114 × PlayerPanel.kt:169) [CÓDIGO]. (d) Nome automático ("08/10/2026 14:32 · M4A 128 kbps mono", AutoTitle.kt) × áudio importado ("Gravação 2026-10-07 14h32", RecordingIngestor.kt:95-96, fixo em português e em outro formato de data) [CÓDIGO]. (e) Selecionar trecho: o primeiro arrasto cria, o seguinte move a ponta mais próxima (WaveformView.kt:168-200) [CÓDIGO]. |
| 5 | Prevenção de erros | 2 | **Pontos fortes (CÓDIGO):** a nota tem contador n/140, erro em texto e Salvar desabilitado (PlayerDialogs.kt:85-106). Exportar fica desabilitado sem trecho ou durante a exportação (PlayerPanel.kt:312). O trecho exportado tem mínimo de 200 ms (PlayerViewModel.kt:212). **Problemas:** o nome do marcador é cortado em 40 caracteres sem aviso nem contador (PlayerDialogs.kt:56, 160) [CÓDIGO]. Etiquetas são normalizadas em silêncio: minúsculas, máximo 24 caracteres, máximo 10 por gravação (Tags.kt:8-17). O que passa disso some sem aviso [CÓDIGO]. A etiqueta depende de vírgula digitada (PlayerDialogs.kt:124-128, 126). |
| 6 | Reconhecimento em vez de memorização | 2 | (a) O chip "Etiquetas" não mostra as etiquetas atuais nem quantas são [CAPTURA 01, 03, 04]. (b) O filtro de etiquetas só aparece quando já existe alguma (LibraryScreen.kt:159). A função é invisível para quem nunca etiquetou [CÓDIGO]. (c) Com zoom não há escala de tempo nem indicador do nível de zoom ou da janela visível (TimeRow só mostra posição/duração, PlayerPanel.kt:176-193) [CÓDIGO]. (d) Os botões de tesoura e lápis nas linhas de marcador não têm texto [CAPTURA 01]. (e) A busca é "por nome" (strings.xml:3). Não encontra por nota nem por etiqueta [CÓDIGO]. |
| 7 | Flexibilidade e eficiência | 2 | **Pontos fortes:** marcador → toque → toca ali, 3 toques até o trecho [CAPTURA 01 + PlayerPanel.kt:63-66]. Existe exportar "daqui até o próximo marcador" em 1 toque (PlayerViewModel.kt:200-206). Há marcar pela notificação, widget e tela Gravar. Há anterior/próximo marcador. **Problemas:** a nota só se escreve depois, no player. Os marcadores feitos na gravação nascem sem nome e sem nota (RecordingService.kt:169, RecordingIngestor.kt:86) [CÓDIGO]. Velocidade e sensibilidade só percorrem em ciclo (PlayerPanel.kt:256, 262), sem menu [CÓDIGO]. Zoom só em passos de 2x (ZOOM_STEP) [CÓDIGO]. Editar etiquetas exige abrir a gravação (só existe no player) [CÓDIGO]. |
| 8 | Design estético e minimalista | 2 | O painel concentra 11 controles em 3 linhas, mais a lista [CAPTURA 01]. Com Pular silêncio ligado, aparecem um chip a mais e uma linha de texto, e a lista de marcadores desce cerca de 40 px (layout instável) [CAPTURA 01 × 02]. A linha de chips estoura a largura de 360 dp e o chip de velocidade fica cortado em "Sp" [CAPTURA 02]. Em seleção, entram texto, 2 botões e a dica, e o painel passa de 600 dp [CAPTURA 03]. O painel ocupa até 62% da altura (PlayerDock.kt:34) e deixa pouca lista visível [CÓDIGO]. Pontos positivos: a onda está limpa e os marcadores são legíveis [CAPTURA 01]. |
| 9 | Ajudar a reconhecer, diagnosticar e corrigir erros | 2 | Pontos positivos: a nota longa dá mensagem clara com limite ("máximo 140. Encurte para salvar", strings.xml:42) [CÓDIGO]. A falha de exportação e a falha da onda têm texto (strings.xml:31-32). **Problemas:** "Não foi possível exportar o trecho." não dá causa nem ação (espaço? corte? gravação no cofre?) [CÓDIGO]. A falha da onda (`AnalysisFailed`) não oferece tentar de novo [CÓDIGO]. Marcador fora da gravação: "Esse ponto está fora da gravação." (strings.xml:47) não diz o que fazer [CÓDIGO]. |
| 10 | Ajuda e documentação | 2 | A pinça só é anunciada para leitores de tela (`player_wave_desc`, strings.xml:6). Visualmente, a única dica de zoom são os botões +/− [CAPTURA 01]. A dica de seleção ("Arraste na onda…") só aparece quando não há trecho (PlayerPanel.kt:298-299) [CÓDIGO]. Nada explica Suave × Normal, nem que "Pular silêncio" não altera o arquivo, nem que o filtro de etiquetas é "todas" (E) (LibraryScreen.kt:156 comentário) [CÓDIGO]. O texto de lista vazia de marcadores é um bom exemplo de ajuda em contexto [CAPTURA 04]. |

**Acessibilidade:** não é heurística de Nielsen, mas é transversal. A onda tem semântica (ações de zoom e de marcador anterior/próximo, progresso, WaveformView.kt:126-137) e alvos de 48 dp nos marcadores (linhas 259-280) [CÓDIGO]. Isso **não foi testado com TalkBack**. Na tela, o alvo do marcador na onda é pequeno para um polegar (cabeça de 5 dp) e a faixa de pular silêncio (12% de alpha) tem contraste muito baixo [CAPTURA 01, 02].

## 3. As cinco dimensões de usabilidade

### 3.1 Aprendizado (learnability) — severidade 3

**Pontos fortes**
- O fluxo principal é descobrível: tocar numa linha de marcador já toca ali [CAPTURA 01].
- Botões de zoom visíveis, sem depender só da pinça [CAPTURA 01].
- Estados vazios com instrução: "Nenhum marcador. Use…" [CAPTURA 04].
- Dica de arrastar ao entrar em seleção [CÓDIGO].

**Problemas**
- A tesoura tem dois significados e nenhum rótulo visível [CAPTURA 01, 03].
- O texto de ajuda cita "Marcar aqui", mas o botão diz "Marcar" [CAPTURA 04].
- O filtro de etiquetas não existe até haver etiquetas (LibraryScreen.kt:159) [CÓDIGO].
- O diálogo de etiquetas pede vírgula, sem chips (PlayerDialogs.kt:124-128). Em muitos teclados de celular a vírgula fica na segunda página [CÓDIGO].
- "Sensibilidade" é um ciclo que mostra o estado atual, não a ação (PlayerPanel.kt:256-257) [CÓDIGO].

**Recomendação:** rotular a tesoura de linha ("Trecho") e usar ícone diferente no modo seleção; trocar o texto de ajuda para "Marcar"; mostrar o filtro com uma ação "Etiquetas" mesmo vazio; entrada de etiquetas por chips.

### 3.2 Eficiência — severidade 2

**Pontos fortes**
- Três toques da lista até o trecho marcado (PlayerPanel.kt:63-66) [CAPTURA 01 + CÓDIGO].
- Exportar do marcador ao próximo marcador em 1 toque (PlayerViewModel.kt:200).
- Marcar em 3 lugares (notificação, widget, tela Gravar), sem abrir o app.
- Nome automático evita digitar.

**Problemas**
- A nota só pode ser escrita no player, depois da gravação. Para quem marca 10 pontos numa aula (Ana, persona estudante), são 10 diálogos depois [CÓDIGO].
- Marcador criado no player interrompe a audição com um diálogo modal (PlayerDock.kt:48) [CÓDIGO].
- Fechar o player para a reprodução (LibraryScreen.kt:84) [CÓDIGO].
- A lista não é pesquisável por nota nem por etiqueta [CÓDIGO].
- Zoom por passos fixos de 2x, sem "ajustar ao trecho" [CÓDIGO].
- O nome automático técnico ("M4A 128 kbps mono") não ajuda a achar nada e pode gerar títulos de várias linhas na lista (LibraryScreen.kt:262 sem `maxLines`) [CÓDIGO].

**Recomendação:** abrir o diálogo de marcador sem pausar e com Cancelar que desfaz; busca que inclua notas e etiquetas; mini-player que não pare ao navegar; nome automático com data e hora legíveis e, no futuro, contexto (Onda 3).

### 3.3 Retenção (memorability) — severidade 2

**Pontos fortes**
- A linha do marcador mostra hora, nome e nota num formato constante [CAPTURA 01].
- Etiquetas aparecem como `#tag` na lista (LibraryScreen.kt:276-281) [CÓDIGO].
- O trecho "Trecho 45:10 – 46:10 (01:00)" repete o intervalo em texto [CAPTURA 03].

**Problemas**
- O uso eventual (uma aula por semana) esquece que a pinça existe e que "Etiquetas" abre um editor. O chip não mostra o que já tem [CAPTURA 01, 03].
- O filtro só aparece quando há etiquetas, então some e reaparece sem explicação [CÓDIGO].
- Sem escala de tempo, o usuário perde a noção do zoom [CÓDIGO].
- Marcadores criados pela notificação ficam sem nome. Uma lista de vários "Marcador" difere só pela hora [CAPTURA 01, linha "30:00 Marker"].

**Recomendação:** mostrar as etiquetas atuais no chip ("Etiquetas (2)") ou no cabeçalho; régua de tempo com zoom; nomear marcadores por padrão ("Marcador 1, 2, 3").

### 3.4 Erros — severidade 3

**Pontos fortes**
- Nota: contador n/140 e erro em texto, não só em cor (PlayerDialogs.kt:83-106) [CÓDIGO].
- Exportar desabilitado sem trecho (PlayerPanel.kt:312) [CÓDIGO].
- Exclusão de gravação com DESFAZER (LibraryScreen.kt:148) [CÓDIGO].
- Aviso de pouco espaço antes de gravar (RecordScreen.kt:175-197) [CÓDIGO].

**Problemas**
- Cancelar o diálogo de "Marcar" mantém o marcador vazio (PlayerDock.kt:48) [CÓDIGO]. Sev 3.
- Excluir marcador sem confirmação nem DESFAZER, com o botão destrutivo ao lado de Cancelar (PlayerDialogs.kt:70-78) [CÓDIGO]. Sev 3. Em 360 dp são três botões de texto (Excluir marcador, Cancelar, Salvar) disputando a linha do diálogo. O risco de toque errado é hipótese, não visto.
- Cortes silenciosos: nome do marcador em 40 caracteres, etiquetas em 24 caracteres e 10 por gravação (PlayerDialogs.kt:56, Tags.kt:8-17) [CÓDIGO]. Sev 2.
- Mensagens de erro de exportação sem causa e sem ação (strings.xml:31-32) [CÓDIGO]. Sev 2.
- Marcar pela notificação sem confirmação (RecordingNotification.kt:21, 47). Se falhar, ninguém sabe (sev 3 para o caso de uso de reunião com tela bloqueada) [CÓDIGO].

**Recomendação:** Cancelar remove o marcador novo; DESFAZER para excluir marcador; contador também no nome do marcador; mostrar quando uma etiqueta foi ajustada; vibração curta no "Marcar" da notificação.

### 3.5 Satisfação — severidade 2 (baixa confiança)

Esta é a dimensão mais frágil: **não há medição de satisfação nem teste com usuário.** O que se pode afirmar é visual e de código.

**Pontos fortes**
- A onda com marcadores é um "mapa" legível do áudio e dá sensação de controle [CAPTURA 01].
- O resumo de silêncio ("Skipped 02:34 so far") dá retorno concreto do ganho [CAPTURA 02].
- Marcar sem tirar o celular do bolso (notificação) atende ao caso de uso reunião/aula [CÓDIGO].

**Problemas**
- O painel parece carregado e instável: chips cortados e conteúdo que se desloca ao ligar o pular silêncio [CAPTURA 02].
- O chip "Pular silêncio" desligado parece desabilitado, sinal visual pouco convidativo [CAPTURA 01].
- Em seleção a ponta final some sob o marcador de 45:10, e a seleção de 1 min em 60 min fica fina, difícil de agarrar com o dedo (alça de 8 dp, alcance 28 dp) [CAPTURA 03 + CÓDIGO]. Só o dono pode dizer se na prática irrita.
- Sem animação nem haptics verificáveis aqui; "satisfação" sensorial não foi avaliada.

**Recomendação:** estabilizar o layout do painel; alças de seleção maiores, com auto-zoom ao selecionar; medir satisfação no teste com usuários (SUS ou pergunta única).

## 4. Achados mais importantes por origem

- **Visto na captura:** tesoura com dois significados [01, 03]; chip "Pular silêncio" com aspecto de desabilitado [01]; chips cortados e layout que se desloca [02]; faixas de silêncio quase invisíveis [02]; alças de seleção sobrepostas ao marcador e trecho fino [03]; texto de ajuda cita "Marcar aqui" mas o botão diz "Marcar" [04]; sem estimativa nem cancelar no cálculo da onda [04]; chip "Etiquetas" sem estado [01, 03, 04].
- **Inferido do código (não visto):** Cancelar mantém marcador vazio; excluir marcador sem DESFAZER; confirmação ausente do "Marcar" da notificação e do widget; fechar o player para a reprodução; busca só por nome; cortes silenciosos de nome/etiquetas; formato de velocidade com ponto em pt-BR; nome de importado em formato e idioma diferentes do automático; falha de exportação sem causa; nome automático técnico.

## 5. Recomendações priorizadas (máx. 10)

Esforço: P = pequeno (horas), M = médio (1–3 dias), G = grande (semana+).

| # | Recomendação | Sev. tratada | Esforço |
|---|---|---|---|
| 1 | Dar confirmação ao "Marcar" da notificação e do widget: vibração curta, texto "Marcador 3 · 12:03" na notificação e, no widget, aviso quando não há gravação. | 3 | P |
| 2 | Cancelar no diálogo de "Marcar" remove o marcador recém-criado; excluir marcador passa a ter DESFAZER. | 3 | P |
| 3 | O player passa a ser minimizável: fechar não pára a reprodução; X separado de "Parar". | 3 | M |
| 4 | Corrigir ambiguidade de ícones e rótulos: tesoura de linha com texto ("Trecho"); ícone distinto no modo seleção; trocar "Marcar aqui" no texto de ajuda por "Marcar"; estilizar "Pular silêncio" como os demais chips. | 2–3 | P |
| 5 | Pular silêncio: faixas visíveis (contraste), explicação curta de Suave × Normal, e layout estável (reservar a linha de resumo). | 2 | P |
| 6 | Seleção de trecho: alças maiores, auto-zoom ao selecionar, rolagem ao arrastar até a borda, e snackbar "Trecho salvo" com ação "Ver". | 2–3 | M |
| 7 | Etiquetas: entrada por chips (sem depender da vírgula), chip "Etiquetas (n)" mostrando o estado, avisar cortes e limites, e filtro visível mesmo vazio. | 2 | M |
| 8 | Busca por nota e por etiqueta, não só por nome. | 2 | M |
| 9 | Nome automático legível (data/hora sem "M4A 128 kbps mono"); limitar `maxLines` na lista; alinhar o nome de importado (idioma e formato). | 2 | P |
| 10 | Locale e acessibilidade: formatar velocidade com vírgula em pt-BR; régua de tempo e indicador de zoom; testar TalkBack. | 1–2 | M |

## 6. O que só o dono pode validar no celular

### 6.1 Verificação (o produto faz o que foi especificado?)

Podem ser checadas em aparelho real, com lista e critério objetivo. Não exigem opinião de usuário:
- A pinça dá zoom ancorado no ponto entre os dedos e o arrasto rola a janela, sem travar numa gravação de 60–90 min.
- O "Marcar" da notificação, do widget e da tela Gravar cria o marcador no instante certo (tolerância de 1–2 s), inclusive com tela bloqueada, e com a gravação pausada.
- O widget "Marcar" sem gravação não quebra nem deixa o serviço preso.
- Pular silêncio: sensibilidade Suave e Normal realmente mudam a quantidade pulada, e a reprodução não corta o início das palavras.
- Exportar trecho: o arquivo novo começa e termina onde o intervalo mostra, nos 3 formatos, e aparece na biblioteca com o nome esperado.
- Nota de 140 caracteres: aceita acentos e emoji e conta como o diálogo mostra.
- Etiquetas: filtrar com várias etiquetas retorna só as gravações que têm todas; renomear e etiquetar atualizam a lista.
- Strings em pt-BR e en nas telas que não têm captura (Gravar, biblioteca, diálogos, notificação).
- TalkBack: ações de zoom, marcador anterior/próximo e progresso funcionam.
- Desempenho: a janela de zoom segue o dedo sem engasgos e o cálculo da onda de 60 min termina em tempo razoável (as capturas só provam o estado "calculando").

### 6.2 Validação (o produto resolve o problema do usuário?)

Exigem o dono (ou outros usuários) usando o app em situação real. Nada aqui foi feito:
- Marcar durante uma aula ou reunião real é fácil e confiável? O "silêncio" da confirmação (sev 3 acima) atrapalha na prática ou o texto da notificação basta?
- Escrever a nota depois, no player, é aceitável, ou o dono quer ditar ou escrever na hora?
- Achar o trecho marcado uma semana depois leva de fato 3 toques? O dono encontra as marcas sem nome?
- Pular silêncio economiza tempo ou soa "cortado" e irrita? Qual sensibilidade ele prefere?
- Vale etiquetar? As etiquetas são usadas, ou a busca por nome basta? O filtro "todas" é o que ele espera?
- O nome automático ajuda a reconhecer a gravação ou ele renomeia tudo?
- Exportar trecho: o uso real é compartilhar, e o fluxo "criar nova gravação" é o esperado?
- Satisfação e carga cognitiva do painel do player em uso com uma mão, em pé, com fone.
- As três personas de `docs/ux/01-personas.md` (Ana, Bruno, Carla) conseguem completar a jornada (`docs/ux/jornada.md`) sem ajuda.

Sugestão: 5 pessoas × 3 tarefas (marcar durante um áudio ao vivo, achar um trecho marcado, exportar um trecho), em pt-BR, medindo sucesso, tempo e erros, antes de decidir a Onda 2.
