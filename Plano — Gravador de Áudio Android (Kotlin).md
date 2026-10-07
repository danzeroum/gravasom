# Plano — Gravador de Áudio Android (Kotlin)

Oct 7, 2026 · @Daniel

## Visão geral

O agente vai entregar um app Android nativo em Kotlin que grava áudio pelo microfone e salva em **.m4a (MP4/AAC)**, **.wav (PCM)** ou **.mp3**, guiado por 16 recomendações de UX, arquitetura e qualidade. O plano é executável em 7 fases, cada uma com critério de aceite verificável.

**Escopo do MVP**

- Gravar, pausar, retomar e parar, inclusive com a tela bloqueada (serviço em primeiro plano).
- Escolher formato e qualidade antes de gravar (taxa de amostragem, bitrate, mono/estéreo).
- Listar, ouvir, renomear, compartilhar e excluir gravações.
- Medidor de nível (VU) e cronômetro em tempo real.
- Tratar interrupções: ligação, outro app tomando o microfone, pouco espaço, permissão negada.

**Fora de escopo no MVP:** edição/corte de áudio, sincronização em nuvem, transcrição, widget de tela inicial. Ficam como backlog.

**Alvo de plataforma:** minSdk 26 (Android 8.0), targetSdk 36, Kotlin 2.x, Jetpack Compose + Material 3.

## Decisões técnicas e trade-offs

Cada formato exige um caminho de captura diferente, por isso o núcleo de gravação é uma interface com três implementações. Seguindo as Leis da Arquitetura, *toda decisão tem seu preço* e *só pode ser avaliada no seu contexto* — o agente deve registrar cada uma como ADR (Architecture Decision Record) em `docs/adr/`.

| Formato | Como gravar | Ganho | Preço |
| --- | --- | --- | --- |
| .m4a (MP4 + AAC-LC) — **padrão** | `MediaRecorder` com `OutputFormat.MPEG_4` + `AudioEncoder.AAC` | Nativo, arquivo pequeno (\~1 MB/min a 128 kbps), pausa/retoma nativas (API 24+) | Compressão com perdas; menos controle sobre os buffers |
| .wav (PCM 16 bits) | `AudioRecord` lendo PCM + escrita própria do cabeçalho RIFF (44 bytes) | Sem perdas, máxima fidelidade, VU preciso a partir das amostras | \~5 MB/min a 44,1 kHz mono; precisa corrigir o cabeçalho ao final e em recuperação de falha |
| .mp3 | `AudioRecord` PCM → codificador LAME via NDK/JNI (ou biblioteca empacotada) | Compatibilidade universal | Android não codifica MP3 nativamente: dependência nativa, +2–3 MB no APK, licença LGPL, mais CPU/bateria |

**Recomendação ao agente:** entregar M4A e WAV no MVP e MP3 atrás de uma *feature flag*, implementado como um terceiro `AudioEncoder` sobre o mesmo pipeline PCM do WAV. Alternativa mais barata: gravar em WAV e converter para MP3 só ao exportar.

**Stack**

- UI: Jetpack Compose, Material 3, Navigation Compose.
- Estado: ViewModel + `StateFlow` (UDF/MVI), Kotlin Coroutines.
- Injeção de dependência: Hilt.
- Persistência: Room (metadados) + `MediaStore` (arquivos em `Music/Recordings`, sem permissão de armazenamento no API 29+).
- Serviço: `ForegroundService` com `foregroundServiceType="microphone"` e notificação com controles.
- Reprodução: Media3 ExoPlayer.
- Logs: Timber; métricas/crash: Firebase Crashlytics ou equivalente opt-in.

## UX

A tela principal faz uma coisa: um toque começa a gravar. Tudo o mais (formato, qualidade, lista) fica a um passo de distância, nunca no caminho do botão.

### Design centrado no usuário (empatizar → definir → idear → prototipar → testar)

1. **Empatizar:** o agente escreve 3 personas curtas (estudante gravando aula, jornalista em entrevista, músico gravando ideia) e o contexto de uso de cada uma (uma mão, tela bloqueada, ambiente barulhento).
2. **Definir:** 1 problema-chave por persona no formato "Como podemos…", com a métrica de sucesso (ex.: tempo até começar a gravar ≤ 2 s após abrir o app).
3. **Idear:** listar soluções e escolher por impacto × esforço.
4. **Prototipar:** ver Prototipação rápida.
5. **Testar:** roteiro de 5 tarefas com 5 usuários; registrar sucesso, tempo e erros por tarefa.

### Prototipação rápida

- **Crazy 8s:** 8 variações da tela de gravação em 8 minutos (botão central vs. barra inferior, VU em barras vs. forma de onda).
- **Baixa fidelidade:** wireframes em Compose `@Preview` com placeholders cinza, usados para co-criar com o usuário, não só validar.
- **Média fidelidade:** previews com tema real em claro/escuro e fonte 200%.

### Arquitetura de informação

| Tela | Conteúdo | Rótulo na navegação |
| --- | --- | --- |
| Gravar | Botão gravar/pausar/parar, cronômetro, VU, formato atual (chip tocável) | Gravar |
| Gravações | Lista ordenada por data (mais recente primeiro), busca, duração, formato e tamanho | Gravações |
| Detalhe/Player | Player, renomear, compartilhar, excluir, metadados | — (aberto pela lista) |
| Ajustes | Formato padrão, qualidade, local de salvamento, privacidade | Ajustes |

Navegação inferior com 3 destinos. Nome padrão do arquivo legível: `Gravação 2026-10-07 14h32.m4a`.

### Heurísticas de Nielsen (aprendizado, eficiência, retenção, erros, satisfação)

- **Visibilidade do status:** cronômetro, VU, ponto vermelho e notificação persistente enquanto grava.
- **Prevenção de erros:** aviso antes de gravar se espaço < 100 MB; excluir pede confirmação e oferece "Desfazer" por 5 s.
- **Recuperação de erros:** mensagens em linguagem simples com ação ("Microfone em uso por outro app — tentar de novo").
- **Controle e liberdade:** pausar/retomar; parar nunca descarta o áudio.
- **Consistência:** Material 3, ícones padrão, mesmo botão nas mesmas posições.
- **Eficiência:** Quick Settings Tile e atalho de app para gravar direto.
- **Acessibilidade:** alvos ≥ 48 dp, `contentDescription` em todos os controles, contraste AA, TalkBack anunciando início/fim da gravação, feedback háptico ao iniciar/parar.

## Arquitetura

Clean Architecture em três camadas (UI → domínio → dados), com o motor de gravação isolado atrás de interfaces para que trocar de formato não toque na UI.

&#91;embedded content: C4 · nível Container · 3 camadas + plataforma\]

As setas apontam sempre para o domínio: trocar o formato de gravação ou o banco muda só a camada de baixo. Caixas tracejadas são a plataforma Android, fora do controle do app. O agente produz também os níveis Contexto, Componente e Código, mais diagramas de sequência dos cenários de interrupção, em `docs/architecture/`.

### Atributos de qualidade (-ilities) e metas mensuráveis

| Atributo | Meta verificável | Como o agente garante |
| --- | --- | --- |
| Confiabilidade | 0 gravações perdidas em 100 ciclos de interrupção (ligação, kill do processo) | Escrita incremental, gravação em arquivo temporário + rename atômico, reparo de cabeçalho WAV no próximo início |
| Performance | Início da gravação ≤ 300 ms após o toque; UI a 60 fps com VU ativo | Leitura de áudio em `Dispatchers.IO`/thread dedicada; VU amostrado a 20 Hz |
| Eficiência (bateria) | ≤ 5% de bateria por hora gravando em M4A, tela apagada | Sem wake lock extra; sem processamento na thread principal |
| Escalabilidade | Lista fluida com 1.000+ gravações | Room + Paging 3; duração/tamanho em cache no banco |
| Segurança | Ver seção Segurança | — |
| Manutenibilidade | Complexidade ciclomática ≤ 10 por função; módulos com dependência unidirecional | Detekt + regras de dependência entre módulos |
| Testabilidade | ≥ 80% de cobertura no domínio e dados | Interfaces + fakes; Hilt para injetar dublês |
| Disponibilidade | App funciona 100% offline | Nenhuma dependência de rede no fluxo principal |

Decisões devem minimizar o custo acumulado ao longo da vida do app: preferir APIs da plataforma a bibliotecas, e isolar o que for nativo (LAME).

### Módulos Gradle

- `:app` — Application, Hilt, navegação.
- `:feature:record`, `:feature:library`, `:feature:settings` — telas Compose + ViewModels.
- `:core:domain` — casos de uso e modelos puros em Kotlin (sem Android): `StartRecording`, `PauseRecording`, `StopRecording`, `ListRecordings`, `DeleteRecording`, `RenameRecording`.
- `:core:audio` — `AudioRecorder` (interface) com `MediaRecorderM4aRecorder`, `PcmWavRecorder`, `PcmMp3Recorder`; `WavHeaderWriter`; `LevelMeter`.
- `:core:data` — `RecordingRepository`, Room, `MediaStore`, DataStore de preferências.
- `:core:service` — `RecordingService` (foreground) e máquina de estados da gravação.
- `:core:ui` — tema, componentes, previews.

Regra: `feature` → `domain` ← `data/audio`. O domínio não conhece Android.

### Máquina de estados da gravação

`Idle → Preparing → Recording ⇄ Paused → Stopping → Saved`, com `Error(causa)` alcançável de qualquer estado e retorno a `Idle`. Toda transição é uma função pura testável.

### Padrões aplicados

- **Strategy** para o codificador por formato; **Factory** escolhe a estratégia a partir da preferência.
- **Repository** para esconder Room + MediaStore.
- **Observer** via `StateFlow` do serviço para a UI.

### Refatoração contínua

A cada fase, o agente revisa: acoplamento entre módulos (nenhuma dependência circular), duplicação entre os gravadores PCM (extrair `PcmCapture` comum), padrões emergentes e se a estrutura aguenta os itens do backlog (edição, nuvem) sem reescrita.

## Segurança, privacidade e observabilidade

Voz gravada é dado pessoal (e pode ser biométrico, portanto sensível na LGPD): o app guarda tudo no aparelho e nada sai sem ação explícita do usuário.

### Segurança por design

- **Privilégio mínimo:** só `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE` e `POST_NOTIFICATIONS`. Nenhuma permissão de internet no MVP, nem de armazenamento (MediaStore no API 29+).
- **Permissão em contexto:** pedida no primeiro toque em Gravar, com tela explicando o porquê; recusa permanente leva às configurações do sistema.
- **Armazenamento:** opção "Gravações privadas" salva em `filesDir` com Jetpack Security / Tink (AES-256-GCM); padrão público em `Music/Recordings` explicado na primeira execução.
- **Compartilhamento:** apenas via `FileProvider` com URI temporária; nunca caminho de arquivo bruto.
- **Backup:** `android:allowBackup` controlado por `data_extraction_rules.xml`, excluindo gravações privadas.
- **Build:** R8 ativado, assinatura com chave fora do repositório, sem segredos no código.

### LGPD / GDPR

- Base legal: execução de serviço pedido pelo titular; telemetria só com consentimento opt-in e revogável em Ajustes.
- Minimização: telemetria nunca contém áudio, nome de arquivo ou localização.
- Direitos do titular: "Excluir todas as gravações" e "Excluir dados de diagnóstico" em Ajustes.
- Aviso de que gravar terceiros pode exigir consentimento deles.

### Observabilidade

- **Logs estruturados (Timber):** eventos `record_start`, `record_pause`, `record_stop`, `record_error` com formato, duração, bitrate e causa — sem dados pessoais. Logs de debug removidos no release.
- **Tracing:** `androidx.tracing` em `startRecording`, abertura do codificador e escrita em disco, para medir os 300 ms de início no Perfetto.
- **Monitoramento:** crash e ANR (opt-in); métricas de buffer *overrun* do `AudioRecord` e falhas de `MediaRecorder.prepare()`.
- **Desempenho:** Macrobenchmark de inicialização a frio e Baseline Profile.

## Qualidade e testes

Código novo nasce por TDD (teste vermelho → verde → refatorar), e cada nível de teste separa **verificação** (o código está correto?) de **validação** (faz o que o usuário precisa?).

### Níveis de teste com V&V

| Nível | Ferramentas | Verificação (está correto?) | Validação (é o que deveria fazer?) | Foco em limites e riscos |
| --- | --- | --- | --- | --- |
| Unidade | JUnit 5, MockK, Turbine, kotlinx-coroutines-test | Máquina de estados, `WavHeaderWriter` (bytes exatos do RIFF), cálculo de RMS/dB do VU, nome de arquivo | Regras de negócio batem com as histórias (ex.: parar nunca descarta) | Arquivo de 0 s, > 2 GB (limite do WAV 32 bits), taxas 8–48 kHz, pausa repetida |
| Integração | Robolectric, Room in-memory, testes instrumentados com `AudioRecord` real | Repositório + Room + MediaStore; gravador gera arquivo que `MediaExtractor` abre com a duração esperada (±100 ms) | Arquivo abre em players externos | Espaço em disco acabando, microfone ocupado, processo morto no meio |
| Sistema | Compose UI Test, UI Automator, Firebase Test Lab (≥ 6 aparelhos, API 26–36) | Fluxo completo gravar → listar → tocar → excluir | Tela bloqueada, ligação recebida, rotação, modo escuro, TalkBack | Fabricantes com gestão agressiva de bateria (Xiaomi, Samsung) |
| Aceitação | Cenários BDD + teste com 5 usuários | Critérios de aceite das fases | Tarefas do roteiro de UX com taxa de sucesso ≥ 90% | Ambiente barulhento, uso com uma mão |

### BDD (Given/When/Then) — exemplos que o agente deve transformar em testes

```gherkin
Funcionalidade: Gravar áudio
  Cenário: Gravação sobrevive a uma ligação
    Dado que estou gravando em M4A há 30 segundos
    Quando recebo uma ligação e atendo
    Então a gravação é pausada automaticamente
    E ao encerrar a ligação vejo "Gravação pausada — tocar para retomar"

  Cenário: Pouco espaço em disco
    Dado que restam 50 MB livres
    Quando toco em Gravar
    Então vejo o aviso de espaço com tempo estimado de gravação
    E posso gravar mesmo assim

  Cenário: Processo encerrado durante gravação WAV
    Dado que o app foi encerrado pelo sistema gravando WAV
    Quando abro o app novamente
    Então a gravação aparece na lista com cabeçalho reparado e duração correta
```

### Análise estática e SAST

- **Detekt** (complexidade ciclomática ≤ 10, código morto, duplicação) e **ktlint** — quebram o build.
- **Android Lint** com `warningsAsErrors` para segurança e acessibilidade.
- **SAST:** MobSF ou Semgrep (regras OWASP MASVS: armazenamento inseguro, `exported` indevido, logs com dados pessoais).
- **Dependências:** OWASP Dependency-Check ou Dependabot.
- **Cobertura:** Kover, mínimo 80% em `:core:domain`, `:core:audio` e `:core:data`.

## DevOps e CI/CD

Nenhum código entra na `main` sem passar pela pipeline inteira; a iteração contínua aparece em PRs pequenos e releases automáticos.

Pipeline em GitHub Actions, executada em container (`ghcr.io/cirruslabs/android-sdk` ou imagem própria com JDK 17 + SDK fixos):

1. **Lint e estática:** ktlint, Detekt, Android Lint, Semgrep.
2. **Testes de unidade + cobertura:** `./gradlew testDebugUnitTest koverVerify`.
3. **Build:** `assembleRelease` + `bundleRelease` com R8.
4. **Testes instrumentados:** Gradle Managed Devices (API 26, 30, 34, 36) e, no merge, Firebase Test Lab.
5. **Segurança:** varredura de dependências e MobSF no APK.
6. **Benchmark:** Macrobenchmark de início a frio; falha se piorar > 10%.
7. **Release:** a cada tag `v*`, upload no canal interno do Google Play (Gradle Play Publisher), changelog gerado dos commits.

Cache do Gradle e build reproduzível (versões em `libs.versions.toml`). Branches curtas, PR com template exigindo testes e captura de tela.

## Roteiro de execução para o agente

Sete fases em sequência; o agente só avança quando o critério de aceite da fase passa na CI.

1. **Descoberta e UX** — personas, problemas "Como podemos…", Crazy 8s, wireframes em `@Preview`, mapa de navegação.
   - Aceite: protótipo de baixa fidelidade das 4 telas + roteiro de teste com 5 tarefas.
2. **Fundação** — projeto multimódulo, Hilt, tema, CI com lint/Detekt/testes, ADRs 001–003 (formato padrão, MP3 via LAME, armazenamento).
   - Aceite: pipeline verde com um teste de exemplo; diagramas C4 (Contexto, Container, Componente) em `docs/architecture/`.
3. **Motor de áudio (TDD)** — interface `AudioRecorder`, máquina de estados, `MediaRecorderM4aRecorder`, `PcmWavRecorder` + `WavHeaderWriter`, `LevelMeter`.
   - Aceite: testes de unidade e instrumentados geram M4A e WAV que `MediaExtractor` lê com duração ±100 ms; cobertura ≥ 80%.
4. **Serviço e interrupções** — `RecordingService` em primeiro plano, notificação com pausar/parar, foco de áudio, ligação, falta de espaço, recuperação após kill.
   - Aceite: cenários BDD de interrupção passando; 100 ciclos sem perda.
5. **Telas** — Gravar, Gravações (Paging), Detalhe com ExoPlayer, Ajustes; permissões em contexto; acessibilidade.
   - Aceite: testes de UI Compose; Accessibility Scanner sem erros; análise heurística de Nielsen documentada.
6. **MP3 e privacidade** — `PcmMp3Recorder` com LAME via NDK atrás de feature flag; armazenamento criptografado; consentimento de telemetria; exclusão total.
   - Aceite: MP3 abre em 3 players externos; MobSF sem achados altos; `FileProvider` testado.
7. **Endurecimento e release** — Baseline Profile, Macrobenchmark, Test Lab em ≥ 6 aparelhos, teste com 5 usuários, revisão de refatoração.
   - Aceite: início ≤ 300 ms; taxa de sucesso das tarefas ≥ 90%; release no canal interno do Play.

Em cada fase o agente entrega: código, testes, ADR quando houver decisão nova, e um parágrafo de V&V (o que foi verificado e o que foi validado com o usuário).

## Prompt pronto para o agente

Copie o bloco abaixo inteiro para o agente de código; ele resume este plano e traz as 16 recomendações das quatro skills.

```text
Você é um engenheiro Android sênior. Desenvolva um app gravador de áudio em Kotlin
(minSdk 26, targetSdk 36, Jetpack Compose + Material 3, Hilt, Coroutines/Flow, Room,
MediaStore, Media3) que grava pelo microfone do celular nos formatos .m4a (MP4/AAC via
MediaRecorder — padrão), .wav (PCM via AudioRecord + cabeçalho RIFF) e .mp3 (PCM + LAME
via NDK, atrás de feature flag). Siga Clean Architecture multimódulo
(:app, :feature:record, :feature:library, :feature:settings, :core:domain, :core:audio,
:core:data, :core:service, :core:ui), Strategy por formato, máquina de estados
Idle→Preparing→Recording⇄Paused→Stopping→Saved/Error e RecordingService em primeiro
plano (foregroundServiceType=microphone). Execute as 7 fases do plano, uma por vez,
só avançando quando o critério de aceite passar na CI. Use TDD em todo código novo.

[RECOMENDAÇÃO] Design Centrado no Usuário: Solicite que a LLM considere o processo completo de design: empatizar, definir, idear, prototipar e testar — não apenas o código.
[RECOMENDAÇÃO] Prototipação Rápida: Sempre inclua no prompt a solicitação de protótipos como ferramenta de co-criação, não apenas validação. Peça Crazy 8s ou protótipos de baixa fidelidade.
[RECOMENDAÇÃO] Arquitetura de Informação: Para revisar interfaces, solicite avaliação da arquitetura de informação: organização, navegação, rotulação e hierarquia dos conteúdos.
[RECOMENDAÇÃO] Usabilidade Heurística: Para qualquer revisão de interface, peça análise heurística (Nielsen) cobrindo: facilidade de aprender, eficiência, retenção, erros e satisfação.
[RECOMENDAÇÃO] Atributos de Qualidade: Solicite que a LLM avalie se o código atende aos atributos de qualidade arquiteturais (escalabilidade, performance, segurança, manutenibilidade) e se as decisões arquiteturais minimizam o custo cumulativo ao longo da vida útil do sistema.
[RECOMENDAÇÃO] DevOps e Automação: Quando revisar infraestrutura e deploy, exija que a LLM verifique a presença de práticas CI/CD, automação de testes, containers e se a cultura de iteração contínua está refletida na pipeline de entrega do software.
[RECOMENDAÇÃO] Observabilidade: Inclua verificação de observabilidade: solicite que a LLM analise se existem mecanismos de logging, tracing e monitoramento que permitam compreender o comportamento interno do sistema e identificar gargalos de performance.
[RECOMENDAÇÃO] Segurança por Design: Exija que a LLM avalie se a segurança está integrada desde as fases iniciais do projeto (Security by Design), verificando criptografia de dados sensíveis, controles de acesso baseados em privilégios mínimos e conformidade com LGPD e GDPR.
[RECOMENDAÇÃO] Atributos de Qualidade (-ilities): Solicite que a LLM identifique e documente os atributos de qualidade relevantes: escalabilidade, performance, segurança, disponibilidade, manutenibilidade, testabilidade.
[RECOMENDAÇÃO] Leis da Arquitetura: Sempre inclua: 'Toda decisão tem seu preço' e 'Uma decisão só pode ser avaliada em relação ao seu contexto'. Peça trade-offs explícitos.
[RECOMENDAÇÃO] Documentação C4/4+1: Para qualquer análise, solicite documentação nos níveis C4: Contexto, Container, Componente e Código. Para revisões, inclua diagramas de sequência (Cenários).
[RECOMENDAÇÃO] Refatoração: Solicite análise de oportunidades de refatoração com: redução de acoplamento, automação de testes, identificação de padrões emergentes e planejamento escalável.
[RECOMENDAÇÃO] Verificação e Validação: Sempre solicite que a LLM separe V&V: verificação (está correto?) e validação (é o que deveria fazer?). Ambos devem ser aplicados em cada nível.
[RECOMENDAÇÃO] Níveis de Teste: Exija cobertura em todos os níveis: unidade, integração, sistema e aceitação. Para cada nível, peça foco em limites, riscos e áreas de maior complexidade.
[RECOMENDAÇÃO] TDD/BDD: Para código novo, solicite que a LLM gere ou valide usando TDD (Test-Driven Development) ou BDD (Behavior-Driven Development) com Given/When/Then.
[RECOMENDAÇÃO] Análise Estática e SAST: Solicite análise estática automatizada para detectar: código morto, duplicado, complexidade ciclomática alta, vulnerabilidades OWASP e violações de padrões de codificação.
```

## Fontes

- Recomendações de UX: e-book "UX no Desenvolvimento de Software", Paula Azevedo Macedo.
- Recomendações de arquitetura e fundamentos: e-book "Arquitetura de Software", Adriano Carezzato.
- Recomendações de qualidade e testes: e-book "Garantia e Controle de Qualidade", Jeniffer Deus.
- APIs Android citadas (MediaRecorder, AudioRecord, MediaStore, foreground service de microfone) conforme a documentação oficial em developer.android.com; versões de bibliotecas devem ser conferidas pelo agente no início da Fase 2.
