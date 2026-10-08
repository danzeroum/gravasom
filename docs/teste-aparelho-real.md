# Roteiro de teste em aparelho real (e com pessoas)

Tudo aqui é **PENDENTE-HUMANO**: a CI verifica em emulador, mas **nada abaixo foi executado em aparelho real**.
Não marque como aprovado sem preencher a coluna "Resultado" (data, aparelho, versão do Android, observação).

**Verificação** = a CI provou em emulador (coluna "Verificação"). **Validação** = uma pessoa confirma no aparelho (este roteiro).

## Antes de começar

1. Baixe o APK: artefato `gravasom-apk` (debug) do último run verde, ou `gravasom-release-apk` (assinado) quando os secrets estiverem cadastrados (`docs/release-assinatura.md`).
2. Instale (`adb install -r app-debug.apk` ou pelo arquivo). Se já havia uma versão com outra assinatura, desinstale antes.
3. Anote: modelo, versão do Android, versão do app (`Ajustes`).

## Roteiro

| # | Passo | O que deve acontecer | Verificação (CI) | Resultado (preencher) |
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
