# Roteiro de teste em aparelho real (e com pessoas)

> **Mudança de regime (2026-10-08):** a CI do GitHub está desligada (limite de gasto da conta; o workflow só roda por
> `workflow_dispatch`). A **verificação** agora é local (`./gradlew detekt lintDebug testDebugUnitTest koverVerify`, saída em
> `docs/status.md`) e a **validação** é sua, instalando o APK no celular. **Nenhum teste instrumentado (emulador) rodou nesta
> entrega**: não há emulador nesta máquina. Tudo o que está marcado PENDENTE-HUMANO abaixo nunca foi executado em aparelho real.

Não marque como aprovado sem preencher a coluna "Resultado" (data, aparelho, versão do Android, observação).

## Antes de começar

1. Instale o APK recebido (`app-release.apk`): toque no arquivo, ou `adb install -r app-release.apk`. Ele é assinado com a
   chave de debug do repositório (`config/debug.keystore`, pública), a **mesma** de todas as entregas: cada versão nova instala
   **por cima** da anterior, sem perder as gravações. (Se já houver uma versão assinada com outra chave, desinstale antes.)
2. Confira o SHA-256 do arquivo com o que veio na mensagem de entrega (`sha256sum app-release.apk`).
3. Anote: modelo, versão do Android, versão do app (`Ajustes`).
4. Permita **microfone** e **notificações** quando o app pedir.

## A. Roteiro curto da Onda 1 (~15 min) — os 4 achados de severidade 3 + o núcleo da onda

> **Validado pelo dono em 08/10/2026**, no celular, com o APK `1.1.0-onda1` (versionCode 60): o dono relatou que a Onda 1 passou na validação.
> O relato foi geral; **não há observações por passo registradas**. Se algo específico falhou, anote em `docs/uso.md`.

| # | Passo | O que deve acontecer | Resultado (preencher) |
| --- | --- | --- | --- |
| A1 | **Gravar** → bloquear a tela → puxar a notificação → tocar **Marcar** | O texto da notificação muda para **"Marcador 1 às 00:07"** (número e instante) e o aparelho **vibra uma vez**, com a tela bloqueada | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A2 | Tocar **Marcar** mais 2 vezes (também pelo widget, se tiver) | "Marcador 2 às …", "Marcador 3 às …", uma vibração a cada toque | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A3 | **Parar** → abrir a gravação na lista | Os 3 marcadores aparecem na onda e na lista de marcadores, nos instantes mostrados na notificação | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A4 | No player, tocar **Marcar** → no diálogo digitar uma nota → **Cancelar** | Nenhum marcador novo aparece (antes ficava um marcador vazio) | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A5 | No player, tocar **Marcar** → digitar uma nota → **Salvar** | Exatamente 1 marcador novo, com a nota, na posição em que você tocou | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A6 | Nota com 141 caracteres | Mensagem de erro e **Salvar** desativado; com 140, salva | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A7 | Tocar num marcador → **Editar** → **Excluir marcador** | Aparece **"Marcador excluído — Desfazer"** por 5 s | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A8 | Tocar **Desfazer** dentro de 5 s | O marcador volta **com a mesma nota, cor e posição**. Excluir de novo e **esperar 6 s**: o aviso some e o marcador não volta | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A9 | Tocar a gravação para tocar → tocar o **X** do player | O painel fecha e **o áudio continua**; aparece a notificação de mídia | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A10 | Na notificação de mídia, tocar **Pausar** (e **Tocar** de novo) | O áudio pausa/retoma. Com a tela bloqueada também funciona | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A11 | Tocar uma gravação **privada** (cofre) e fechar o painel | A notificação de mídia mostra **"Gravação privada"**, nunca o nome da gravação | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A12 | Tocando em segundo plano, trocar de aba (Gravar/Ajustes) e voltar | O áudio não para ao trocar de aba | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A13 | Abrir uma gravação longa (ideal: 30–60 min) → **pinçar** a onda → tocar num marcador | Zoom suave; o toque no marcador leva ao ponto em ≤3 toques desde a lista | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A14 | Ligar **Pular silêncio** numa aula/reunião real | O silêncio é pulado e **nenhuma fala é cortada** (ouça atento o começo das frases) | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A15 | Selecionar um trecho → **Exportar** → abrir o trecho em **outro player** | O trecho toca fora do app, com a duração escolhida | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A16 | Etiquetas: pôr "aula" em 2 gravações e filtrar pela etiqueta | A lista mostra só as com a etiqueta | VALIDADO PELO DONO (08/10/2026, versionCode 60) |
| A17 | Instalar a **próxima** versão por cima | Instala sem desinstalar e as gravações continuam lá | VALIDADO PELO DONO (08/10/2026, versionCode 60) |

**Limitações conhecidas para não estranhar** (descritas, não corrigidas nesta etapa):

- O **pular silêncio só atua com o painel do player aberto**: com o painel fechado (áudio em segundo plano) a reprodução segue sem pular.
- Fora do painel não há botão de pausa dentro do app: pausa-se pela notificação de mídia (ou reabrindo a gravação).
- "Marcar" no **widget** fora de uma gravação em curso é ignorado em silêncio.
- A transcrição (modelo base q5_1) **não** está nesta versão.

## A2. Roteiro curto da Onda 2 (~15 min) — som melhor e proteção

> APK `1.2.0-onda2` (versionCode 61). **Verificação ≠ validação**: o que está abaixo é a **validação de ouvido** — os números
> (loudness ±1 LU, pico ≤ −1 dBTP, STOI ≥ 0,9909, SHA-256 do backup) foram verificados por teste local e harness antes do APK.
> Use um fone para os passos B1–B4 (A/B de ouvido é o coração da onda). Preencha a coluna "Resultado".

| # | Passo | O que deve acontecer | Resultado (preencher) |
| --- | --- | --- | --- |
| B1 | Gravar ~30 s de voz num lugar com vento/ruído (preset **Voz**) → aba Gravações → menu (⋮) da gravação → **Melhorar som** | Abre a tela "Melhorar som" com "O original nunca é alterado" e o redutor/volume já ligados (padrão do preset Voz) | |
| B2 | Na tela, tocar **A · Original** e **B · Melhorado** alternadamente (mexa o "Trecho da prévia" se quiser ouvir outro pedaço) | Mesmo trecho dos dois lados, sem corte entre A e B; **B fica com menos chiado e volume parelho**; o texto diz qual lado está tocando | |
| B3 | Desligar o redutor, ouvir A/B de novo; ligar de volta e empurrar a intensidade | A diferença entre A e B acompanha o ajuste na hora (a prévia é regerada ao mexer) | |
| B4 | **Salvar cópia melhorada** → assistir a barra → **Concluir** | Nova gravação "… (melhorada)" na aba Gravações, em WAV; a original continua idêntica (ouça e compare) | |
| B5 | Repetir B1 e, no meio da barra de progresso, tocar **Cancelar** | Volta para os ajustes; **nenhuma** gravação nova aparece e a original segue intacta | |
| B6 | No player de uma gravação, arrastar a onda para um ponto e tocar **Dividir aqui** | Aviso "Duas gravações criadas: … parte 1 e … parte 2"; as duas tocam e, juntas, cobrem a original; a original não muda | |
| B7 | Na aba Gravações, **segurar** duas gravações do mesmo formato → **Juntar** (conferir a ordem no diálogo) → confirmar | Gravação nova "A e B" com os áudios na ordem mostrada; as duas originais continuam lá | |
| B8 | Ajustes → **Backup cifrado** → **Criar backup** → escolher pasta → senha (≥6) → confirmar | Aviso "Backup criado com N gravações" e o arquivo `gravasom-backup-….gravasom` onde você salvou | |
| B9 | **Excluir** (com Desfazer expirado, ou gravar 2 novas quaisquer) → **Restaurar backup** → senha **errada** | "Senha errada: nada foi restaurado" — a lista fica exatamente como estava | |
| B10 | **Restaurar backup** de novo com a senha **certa** | "N gravações restauradas. Nada foi apagado"; as restauradas tocam e os marcadores/etiquetas voltam | |
| B11 | Gravar com preset **Natureza** e **Água** ao ar livre → Melhorar som de cada | Os efeitos começam **desligados** (sons de bichos/água não passam pelo redutor) | |
| B12 | Na tela Gravar, tocar o chip **WAV** (sem preset) e gravar 5 s | A gravação sai em WAV; em Ajustes o app mostra "personalizado" (fora de preset) | |
| B13 | Ouvir uma aula longa com **Pular silêncio** ligado → tocar o **X** do player e deixar tocando em segundo plano | O silêncio **continua sendo pulado** com o painel fechado (correção da Onda 1) | |
| B14 | Tocar **Marcar** no widget **sem** gravação em curso | Aviso "Nenhuma gravação em andamento para marcar" (nada em silêncio; correção da Onda 1) | |

**Limitações conhecidas da Onda 2 para não estranhar**:

- Juntar exige gravações do **mesmo formato** (M4A com M4A etc.) e, no WAV, a mesma taxa/canais — o aviso explica quando não dá.
- A prévia A/B cobre uma janela de 20 s por vez (mexa o "Trecho da prévia" para ouvir outros pedaços).
- A cópia melhorada sai **em WAV** (sem compressão); aparelhos com pouco espaço podem preferir apagá-la depois de ouvir.
- O pacote de backup **não** guarda a senha (de propósito): sem ela não há como abrir.

## B. Casos que só rodam em emulador/aparelho (instrumentados) e NÃO rodaram nesta entrega

Estes testes existem em `app/src/androidTest` e precisam de emulador ou aparelho conectado
(`./gradlew :app:connectedDebugAndroidTest`, com um AVD API 28/34/36 ligado). Foram verdes na CI em API 34 e 36
(run 37779440170, commit `f26e739`, **antes** das mudanças desta etapa); depois dela **não foram reexecutados**. Os casos
impactados pelas mudanças desta etapa — rodar primeiro:

| Teste | O que cobre | Impacto desta etapa |
| --- | --- | --- |
| `OndaMarcadoresTest.nota141CaracteresRejeitadaE140Salva`, `marcarAquiPeloPlayerCriaMarcadorNaOnda` | Marcar pelo player e nota 140/141 | O marcador agora só nasce ao **Salvar** (rascunho) |
| `OndaMarcadoresTest` (marcador da notificação na onda) | Marcar pela notificação | A notificação ganhou texto "Marcador N às mm:ss" e vibração |
| `NotificacaoTest`, `SuperficiesTest` | Ações da notificação e do widget | Texto do conteúdo da notificação mudou |
| `PlayerMedia3Test` | Player com Media3 | O ExoPlayer agora é do `PlaybackHub`; `release()` não o libera |
| `EdicaoRoomTest`, `ExportarTrechoTest`, `PularSilencioTest`, `EtiquetasTest` | Migração, exportar, pular silêncio, etiquetas | Sem mudança de código nesta etapa (corrigidos no run anterior) |
| `AcessibilidadeTest`, `CofreTest`, `FluxoUsuarioTest`, `CiclosInterrupcaoTest` | Fluxos gerais | Sem mudança de código |

Também **nunca executado**: o Macrobenchmark da pinça na onda de 1 h (`macrobenchmark/`), e o job de API 28 da CI.

## C. Roteiro das fases anteriores (ainda valem)


| # | Passo | O que deve acontecer | Verificação (CI, histórica) | Resultado (preencher) |
| --- | --- | --- | --- | --- |
| 1 | Abrir o app → **Gravar** → aceitar microfone (e notificações no Android 13+) | Pedido de permissão aparece **no primeiro toque em Gravar**, não antes; gravação começa | FluxoUsuarioTest, PermissaoNegadaTest | PENDENTE-HUMANO |
| 2 | Gravar 10 s → **Parar** | Item na aba Gravações com ~10 s; tocar com 1 toque | FluxoUsuarioTest | PENDENTE-HUMANO |
| 3 | Ajustes → trocar formato M4A, WAV, MP3 e gravar 5 s em cada | Três gravações; abrir o app **Arquivos** → Músicas/Recordings: os três arquivos visíveis e tocam em outro player | FluxoUsuarioTest (MediaStore) | PENDENTE-HUMANO |
| 4 | **Android 9 ou anterior** (se tiver): repetir 1–3 | O app pede também "Armazenamento"; com ele, os arquivos aparecem em Músicas/Recordings; sem ele, a gravação funciona e fica só no app | PublicacaoApi28Test (emulador API 28) | PENDENTE-HUMANO |
| 5 | Com pouco espaço (encher o armazenamento até < 100 MB livres) → **Gravar** | Aviso com MB livres e minutos estimados; "Gravar mesmo assim" grava; "Cancelar" não | EspacoLivreTest (espaço simulado) | PENDENTE-HUMANO |
| 6 | Ajustes → ligar **Cofre cifrado** → gravar 10 s | Item com selo de cofre; **não** aparece em Arquivos/Músicas; toca no app | CofreTest | PENDENTE-HUMANO |
| 7 | Plugar o aparelho no computador (ou usar "Arquivos" → Android/data) e achar o `.gvs` do cofre | O arquivo **não abre** em nenhum player | CofreTest (MediaExtractor não abre) | PENDENTE-HUMANO |
| 8 | Desligar o cofre → gravar de novo | A gravação antiga continua na lista e toca; a nova aparece em Músicas/Recordings | CofreTest | PENDENTE-HUMANO |
| 9 | Compartilhar uma gravação (do cofre e de fora do cofre) **pelo WhatsApp** | A conversa recebe o áudio, que toca; o nome é o título, nunca um caminho | EcossistemaTest/CofreTest (`content://`) | PENDENTE-HUMANO |
| 10 | "Salvar em…" para o Google Drive ou Arquivos | O arquivo salvo toca (decifrado, se vinha do cofre) | CofreTest (copyTo) | PENDENTE-HUMANO |
| 11 | Outro app (gravador, WhatsApp) → Compartilhar áudio → **Gravasom** | Entra na lista em até 3 s | EcossistemaTest | PENDENTE-HUMANO |
| 12 | Microfone Bluetooth e/ou fone USB-C: conectar e abrir **Gravar** | A entrada ativa aparece no botão "Entrada"; gravar usa esse microfone | — (sem hardware no emulador) | PENDENTE-HUMANO |
| 13 | **TalkBack** ligado: percorrer Gravar, Gravações, Ajustes, aviso de espaço, menu da gravação | Tudo é anunciado com nome e função; botões grandes o bastante; gravando/pausado é anunciado | AcessibilidadeTest (checagens ATF) cobre rótulos/área de toque, **não** a experiência | PENDENTE-HUMANO |
| 14 | Idioma do aparelho em português e em inglês | Textos corretos, sem cortes | passada pt-BR e en da CI | PENDENTE-HUMANO |
| 15 | Notificação: bloquear a tela durante a gravação e usar Pausar/Parar da notificação | Os botões funcionam | NotificacaoTest | PENDENTE-HUMANO |
| 16 | Matar o app (deslizar nos recentes) durante uma gravação e reabrir | A gravação aparece recuperada | CiclosInterrupcaoTest (simulado) | PENDENTE-HUMANO |
| 17 | Tile "Gravar" e widget | Iniciam a gravação | SuperficiesTest | PENDENTE-HUMANO |

## Com pessoas (teste de uso — H6)

5 pessoas, tarefas do `docs/ux/06-validacao-prototipos.md` (achar a última gravação, gravar, compartilhar, ligar o cofre, achar por nome). Meta: ≥ 90% de sucesso,
tempo e toques por tarefa anotados. **PENDENTE-HUMANO**.

## Como registrar

Copie a tabela para um arquivo `docs/evidencias-aparelho/AAAA-MM-DD-<modelo>.md`, preencha a coluna "Resultado" com **OK / FALHOU + observação**
e anexe capturas de tela quando FALHOU. Só então o item vira APROVADO em `docs/status.md`.
