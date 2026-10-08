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

| # | Passo | O que deve acontecer | Resultado (preencher) |
| --- | --- | --- | --- |
| A1 | **Gravar** → bloquear a tela → puxar a notificação → tocar **Marcar** | O texto da notificação muda para **"Marcador 1 às 00:07"** (número e instante) e o aparelho **vibra uma vez**, com a tela bloqueada | PENDENTE-HUMANO |
| A2 | Tocar **Marcar** mais 2 vezes (também pelo widget, se tiver) | "Marcador 2 às …", "Marcador 3 às …", uma vibração a cada toque | PENDENTE-HUMANO |
| A3 | **Parar** → abrir a gravação na lista | Os 3 marcadores aparecem na onda e na lista de marcadores, nos instantes mostrados na notificação | PENDENTE-HUMANO |
| A4 | No player, tocar **Marcar** → no diálogo digitar uma nota → **Cancelar** | Nenhum marcador novo aparece (antes ficava um marcador vazio) | PENDENTE-HUMANO |
| A5 | No player, tocar **Marcar** → digitar uma nota → **Salvar** | Exatamente 1 marcador novo, com a nota, na posição em que você tocou | PENDENTE-HUMANO |
| A6 | Nota com 141 caracteres | Mensagem de erro e **Salvar** desativado; com 140, salva | PENDENTE-HUMANO |
| A7 | Tocar num marcador → **Editar** → **Excluir marcador** | Aparece **"Marcador excluído — Desfazer"** por 5 s | PENDENTE-HUMANO |
| A8 | Tocar **Desfazer** dentro de 5 s | O marcador volta **com a mesma nota, cor e posição**. Excluir de novo e **esperar 6 s**: o aviso some e o marcador não volta | PENDENTE-HUMANO |
| A9 | Tocar a gravação para tocar → tocar o **X** do player | O painel fecha e **o áudio continua**; aparece a notificação de mídia | PENDENTE-HUMANO |
| A10 | Na notificação de mídia, tocar **Pausar** (e **Tocar** de novo) | O áudio pausa/retoma. Com a tela bloqueada também funciona | PENDENTE-HUMANO |
| A11 | Tocar uma gravação **privada** (cofre) e fechar o painel | A notificação de mídia mostra **"Gravação privada"**, nunca o nome da gravação | PENDENTE-HUMANO |
| A12 | Tocando em segundo plano, trocar de aba (Gravar/Ajustes) e voltar | O áudio não para ao trocar de aba | PENDENTE-HUMANO |
| A13 | Abrir uma gravação longa (ideal: 30–60 min) → **pinçar** a onda → tocar num marcador | Zoom suave; o toque no marcador leva ao ponto em ≤3 toques desde a lista | PENDENTE-HUMANO |
| A14 | Ligar **Pular silêncio** numa aula/reunião real | O silêncio é pulado e **nenhuma fala é cortada** (ouça atento o começo das frases) | PENDENTE-HUMANO |
| A15 | Selecionar um trecho → **Exportar** → abrir o trecho em **outro player** | O trecho toca fora do app, com a duração escolhida | PENDENTE-HUMANO |
| A16 | Etiquetas: pôr "aula" em 2 gravações e filtrar pela etiqueta | A lista mostra só as com a etiqueta | PENDENTE-HUMANO |
| A17 | Instalar a **próxima** versão por cima | Instala sem desinstalar e as gravações continuam lá | PENDENTE-HUMANO |

**Limitações conhecidas para não estranhar** (descritas, não corrigidas nesta etapa):

- O **pular silêncio só atua com o painel do player aberto**: com o painel fechado (áudio em segundo plano) a reprodução segue sem pular.
- Fora do painel não há botão de pausa dentro do app: pausa-se pela notificação de mídia (ou reabrindo a gravação).
- "Marcar" no **widget** fora de uma gravação em curso é ignorado em silêncio.
- A transcrição (modelo base q5_1) **não** está nesta versão.

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
