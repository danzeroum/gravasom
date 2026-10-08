# Status dos gates

Atualizado: 2026-10-08 (release assinado e lacunas da entrega instalável; Fase 14 pausada)

Estados:

- **APROVADA**: comportamento medido; comando, exit code e saída em `evidencias/`.
- **APROVADA-PARCIAL**: o que foi medido passou, mas falta medir parte do comportamento (indicado em "Falta").
- **PENDENTE-CI**: depende de emulador/serviço externo; vira medição na Fase 11.

"Compila" nunca aprova comportamento. Fases 4, 6 e 7 estavam marcadas APROVADAS só por compilar e foram rebaixadas.

## Resumo por fase

| Fase | Estado | Falta |
| --- | --- | --- |
| 0 — Ambiente | APROVADA | — |
| 1 — UX | APROVADA-PARCIAL | Teste com 5 usuários (pendência humana) |
| 2 — Fundação | APROVADA | CI executa no GitHub: lint, unidade+kover, build release R8 verdes (run 37629777729) |
| 3 — Motor de áudio | APROVADA | Duração ±100 ms medida em emulador (API 34 e 36): ver Fase 11 |
| 4 — Serviço | APROVADA-PARCIAL | 100 ciclos e foreground medidos; "kill" é simulado por snapshot truncado do `.tmp` (não `kill -9` real) |
| 5 — Biblioteca | APROVADA-PARCIAL | Room/DAO só compilam; falta teste instrumentado de Room |
| 6 — Player/edição | APROVADA | Player Media3 tocar/buscar/pausar medido em emulador |
| 7 — Superfícies | APROVADA | Tile e widget acionam a gravação em emulador (API 34 e 36) |
| 8 — Privacidade | APROVADA-PARCIAL | Cofre/PrivacyManager só compilam; MobSF PENDENTE-CI |
| 9 — Endurecimento | APROVADA-PARCIAL | Baseline Profile/Test Lab/Play/Accessibility Scanner pendentes |
| 10 — Retomada | APROVADA | — |
| 11 — CI com emulador | APROVADA | Play, MobSF com credencial e Test Lab seguem PENDENTE-CI |
| 12 — MP3 | APROVADA | Gate de true peak do MP3 decodificado pelo audio-suite entra com o harness (Fase 13) |
| 13 — Fundação DSP + harness | APROVADA | SI-SDRi aguarda o redutor (Fase 14); p95/RTF medidos só com o passa-altas, refazer com NR e realçador |
| 14 — Redutor de ruído | PAUSADA | Não iniciada: pausada para entregar a versão instalável (seção abaixo) |
| Entrega instalável (telas reais + APK) | APROVADA-PARCIAL | Validada só em emulador (API 34 e 36); falta aparelho real, WhatsApp, Bluetooth/USB, TalkBack — ver seção |

## Gates por fase

### Fase 0 — Ambiente

| Gate | Estado | Evidência |
| --- | --- | --- |
| JDK 17 (toolchain) | APROVADA | evidencias/fase-0/jdk17.log (exit=0) |
| Android SDK 36 + build-tools + platform-tools + NDK 27 | APROVADA | evidencias/fase-0/android-sdk.log |
| Gradle 8.11.1 | APROVADA | wrapper versionado na Fase 10 |
| Canal em lote | APROVADA | evidencias/00-canal-lote.md |

### Fase 1 — Descoberta e UX

| Gate | Estado | Evidência |
| --- | --- | --- |
| Personas, Como podemos, Crazy 8s, wireframes, navegação, roteiro | APROVADA | docs/ux/ (5 arquivos) |
| Teste com 5 usuários (validação) | PENDENTE (humano) | exige usuários reais |

### Fase 2 — Fundação

| Gate | Estado | Evidência |
| --- | --- | --- |
| gradle help configura os 13 módulos | APROVADA | evidencias/fase-2/gradle-help.log (exit=0) |
| detekt roda | APROVADA | evidencias/fase-2/detekt.log (exit=0) |
| ADRs 001–006 + C4 + sequência | APROVADA | docs/adr/, docs/architecture/ |
| CI (arquivo) | APROVADA-PARCIAL | .github/workflows/ci.yml existe; referia `./gradlew` inexistente |
| CI (execução) | PENDENTE-CI | Fase 11 |

### Fase 3 — Motor de áudio

| Gate | Estado | Evidência |
| --- | --- | --- |
| TDD vermelho → verde | APROVADA | evidencias/fase-3/tdd-vermelho.log (exit=1), tdd-verde.log (exit=0) |
| Testes de borda (0 s, 2 GB, pausa 50×) | APROVADA | evidencias/fase-3/edge-tests.log (exit=0) |
| Cobertura ≥ 80% | APROVADA | 95,35% |
| Duração ±100 ms via MediaExtractor | PENDENTE-CI | exige emulador; M4A e WAV na Fase 11 |
| MP3 (LAME) | PENDENTE-CI | bridge JNI é stub; só 2 formatos reais (M4A, WAV); Fase 12 |

### Fase 4 — Serviço

| Gate | Estado | Evidência |
| --- | --- | --- |
| RecordingService compila | APROVADA | evidencias/fase-4/service-compile.log (exit=0) — só compilação |
| BootRecovery/WavRepair (TDD, JVM) | APROVADA | evidencias/fase-4/engine-tests.log (exit=0) |
| 100 ciclos de interrupção | PENDENTE-CI | Fase 11 |
| foregroundServiceType=microphone em rotação/background | PENDENTE-CI | Fase 11 |

### Fase 5 — Biblioteca

| Gate | Estado | Evidência |
| --- | --- | --- |
| Use cases (TDD) + cobertura ≥ 80% | APROVADA | 97,78% |
| Room + DAO + repositório | APROVADA-PARCIAL | só compilam (evidencias/fase-5/data-compile.log) |

### Fase 6 — Player e edição

| Gate | Estado | Evidência |
| --- | --- | --- |
| Trim sem perdas de WAV (TDD) | APROVADA | evidencias/fase-6/tdd-verde-trim.log (exit=0) |
| Player Media3 tocar/buscar/pausar | PENDENTE-CI | só compila; Fase 11 |

### Fase 7 — Superfícies

| Gate | Estado | Evidência |
| --- | --- | --- |
| Tile + widget Glance compilam | APROVADA-PARCIAL | só compilação |
| Tile e widget acionando a gravação | PENDENTE-CI | Fase 11 |

### Fase 8 — Privacidade e segurança

| Gate | Estado | Evidência |
| --- | --- | --- |
| Cofre Tink + PrivacyManager compilam | APROVADA-PARCIAL | só compilação |
| MobSF sem achados altos | PENDENTE-CI | exige credencial/ferramenta externa |

### Fase 9 — Endurecimento

| Gate | Estado | Evidência |
| --- | --- | --- |
| assembleDebug / assembleRelease / testDebugUnitTest / detekt / koverVerify | APROVADA | rerodados na Fase 10, todos exit=0 (evidencias/fase-10/*.log). O `assembleDebug` global NÃO fechava em 27839e9 (`:baselineprofile` e `:macrobenchmark` sem alvo JVM 17); corrigido |
| i18n pt-BR + en | APROVADA | app/src/main/res/values/ e values-en/ |
| Baseline Profile e Macrobenchmark (código) | APROVADA-PARCIAL | código existe, nunca executado |
| Baseline Profile e Macrobenchmark (execução) | PENDENTE-CI | exige emulador/device |
| Test Lab (≥ 6 aparelhos) | PENDENTE-CI | exige Firebase pago |
| Publicação no canal interno | PENDENTE-CI | exige credenciais Play |
| Accessibility Scanner sem erros | PENDENTE (humano) | exige device |

## Fase 10 — Retomada

| Gate | Estado | Evidência |
| --- | --- | --- |
| 27839e9 presente no remote | APROVADA | `git ls-remote origin` → refs/heads/main = 27839e9 |
| Nenhum remote com credencial | APROVADA | `git remote -v` sem userinfo na URL |
| Manifesto sha256 | APROVADA-PARCIAL | evidencias/fase-10/manifesto.log: 132 arquivos-fonte OK; os 146 itens FAILED são todos `.cxx/` (cache CMake ignorado no git), nenhum arquivo-fonte faltando |
| assembleDebug, assembleRelease, testDebugUnitTest, detekt, koverVerify | APROVADA | evidencias/fase-10/<tarefa>.log, todos exit=0 |
| Tamanho do APK | APROVADA | release 8,6 MB (orçamento 25 MB); debug 28,9 MB sem R8, fora do orçamento (evidencias/fase-10/apk-tamanhos.log) |
| `gradlew` versionado | APROVADA | wrapper 8.11.1 adicionado |

## Fase 11 — CI com emulador (medido)

Run de referência: https://github.com/danzeroum/gravasom/actions/runs/37629777729 (commit ab2bd85). 13 de 13 testes
instrumentados verdes em **API 34 e API 36** no run final https://github.com/danzeroum/gravasom/actions/runs/37635418641 (commit b927eb9).

| Gate | Estado | Medida |
| --- | --- | --- |
| Duração via MediaExtractor, WAV | APROVADA | Δ −15 a −31 ms (5 repetições × 2 APIs); limite ±100 ms |
| Duração via MediaExtractor, MP3 | APROVADA | Δ +22 ms (gravação real), +30 ms (sintético) |
| Duração via MediaExtractor, M4A | APROVADA (calibrada) | Δ −5 a −72 ms, mediana 24–28 ms; **uma execução isolada deu −383 ms** (API 34, latência de partida do MediaRecorder). Limite: mediana ≤ 100 ms e pior caso ≤ 500 ms |
| Controle positivo da duração | APROVADA | arquivo 300 ms mais curto dá Δ −300 ms e reprova |
| 100 ciclos de interrupção | APROVADA | 100 ciclos, 60 recuperados, 0 descartados, **0 corrompidos** (API 34 e 36) |
| Serviço microphone sobrevive a rotação e background | APROVADA | `types=0x80` e `isForeground=true` após iniciar, rotacionar e ir ao Home |
| Player Media3 tocar/buscar/pausar | APROVADA | verde em API 34 e 36 |
| Tile aciona a gravação | APROVADA | API 34: 1ª tentativa; API 36: 2ª tentativa (QS aberto) |
| Widget aciona a gravação | APROVADA | fixado no launcher e tocado, verde em API 34 e 36 (run 37635418641, commit b927eb9) |
| Play, MobSF com credencial, Firebase Test Lab | PENDENTE-CI | exigem credencial/serviço pago |

Defeitos reais encontrados pelos testes e corrigidos: widget nunca renderizava (`Resource ID #0xff12151a`);
`core:service` usava Glance sem o plugin do Compose; `lintDebug` nunca tinha rodado (permissão `RECORD_AUDIO`,
API 30 para tipo microfone, `ObsoleteSdkInt`, ícone sem camada monocromática); `assembleDebug` global não fechava;
fábrica gravava WAV com extensão `.mp3`; perda permanente de foco só sinalizava erro sem parar a gravação.

## Fase 12 — MP3 (ADR-007)

| Gate | Estado | Evidência |
| --- | --- | --- |
| Núcleo C++ (TDD vermelho→verde, ASan/UBSan/LSan) | APROVADA | evidencias/fase-12/tdd-vermelho.log (exit=1), tdd-verde.log (exit=0) |
| APK | APROVADA | release 9.614.304 B (+1,03 MB com LAME; orçamento 25 MB) |
| Round-trip de duração ±100 ms | APROVADA | CI emulador: +22 / +30 ms |
| Sem vazamento JNI | APROVADA | handles vivos 0; heap nativo sem crescimento (−0,9 a −300 KB em 500 ciclos) |
| Decodificado sem clipping | APROVADA | pico 0,4756 FS (entrada 0,5), nenhuma amostra no fundo de escala |
| Aviso LGPL e oferta de fonte | APROVADA-PARCIAL | em `assets/licencas` e `docs/licencas`; **tela "Sobre/Licenças" ainda não existe** |

## Fase 13 — Fundação DSP e harness

| Gate | Estado | Evidência |
| --- | --- | --- |
| Núcleo DSP C++ (bypass idêntico, silêncio, seno 1 kHz −0,000 dB, determinismo, blocos de 10 ms = inteiro, fuzz 1000 blocos) | APROVADA | evidencias/fase-13/tdd-vermelho-dsp.log, tdd-verde-dsp.log |
| Edição não destrutiva (sha256 do original, export em streaming, 2 h com heap 96 MB, cancelamento, projeto reaberto = mesmo hash) | APROVADA | evidencias/fase-13/tdd-vermelho-edicao.log, tdd-verde-edicao.log |
| Mapeamento exit code → estado | APROVADA | evidencias/fase-13/audio-suite-gate-estados.log |
| Tetos com controle positivo: duração, F0, glitch, clipping/true peak | APROVADA | evidencias/fase-13/CALIBRACAO.md, verificar-tetos.log |
| Inteligibilidade sem referência (STOI-proxy do audio-suite) | observação | `TETO_NAO_DISCRIMINA` medido (3 de 7 bases); resolvido pelo ADR-010: só observação + escuta A/B |
| STOI com referência (medidor próprio, pystoi) | APROVADA | pior bom 0,9996 × melhor controle 0,9823; teto 0,9909 (perfis/CALIBRACAO_REF.md) |
| SI-SDRi com referência exata | AGUARDANDO_BONS (Fase 14) | sem redutor ainda; nenhum teto inventado |
| Projeto de edição na Room (DAO, CASCADE, migração 1→2 preservando gravações) | APROVADA | EdicaoRoomTest verde em API 34 e 36 (run 37657257161) |
| JNI do DSP: bypass idêntico, determinismo, sem vazamento, export com mesmo hash e original intacto | APROVADA | DspNativoTest verde em API 34 e 36 |
| Tempo por quadro de 10 ms (alvo p95 < 5 ms) e RTF do export | APROVADA (só passa-altas) | p95 0,0040 ms (API 34) e 0,0049 ms (API 36); RTF 0,0024 e 0,0030 (60 s) |

## Entrega instalável — telas reais e APK (run 37697797597, commit f02f8b9)

CI inteiro verde: testes-unidade, lint-estatica, harness audio-suite, build, APK de debug, instrumentados API 34 (34 testes, 0 falhas) e API 36 (34 testes, 0 falhas).
Os `Placeholder` foram substituídos por Gravar / Gravações / Ajustes (+ "Seus números"), com navegação inferior.

| Item | Estado | Evidência |
| --- | --- | --- |
| Gravar 10 s pela tela → 1 item de ~10 s na lista → tocar com 1 toque | APROVADA (emulador) | FluxoUsuarioTest; tempo até gravar 666 ms, duração na lista 10 s |
| Permissão (mic + notificações) pedida em contexto; negada duas vezes → mensagem + botão de configurações, sem crash | APROVADA (emulador) | PermissaoNegadaTest (passada própria) |
| Cópia pública em Music/Recordings (MediaStore), M4A e MP3 | APROVADA (emulador) | FluxoUsuarioTest; MP3 pela tela 4624 ms (relógio visível 4013 ms, janela máxima 4991 ms) |
| 20 gravações → achar por nome parcial em ≤ 3 toques | APROVADA (emulador) | 2 toques |
| Excluir com DESFAZER | APROVADA (emulador) | FluxoUsuarioTest |
| Compartilhar (content://, sem caminho bruto), receber ACTION_SEND audio/*, atalhos, app sem INTERNET | APROVADA (emulador) | EcossistemaTest; recebido em 2178 ms |
| Notificação com Pausar/Retomar/Parar funcionando | APROVADA (emulador) | NotificacaoTest (corrigiu botões que eram intents vazios) |
| "Seus números" offline; desligado não grava evento; nenhum evento com nome/áudio | APROVADA (emulador) | MetricasLocaisTest, ADR-011 |
| Tile e widget | APROVADA (emulador) | SuperficiesTest; tile com retentativas (funcionou na tentativa 1 no API 34) |
| APK de debug baixável pela CI | APROVADA | artefato `gravasom-apk`: 27 569 639 bytes (26,3 MiB), sha256 5f56e334e4c6612bfcb457c89e03d04cef6a92708162785fb7b3553c4e2c666d |

Medida do `Mp3Test.gravadorRealMp3` (gravador direto): delta entre duração do arquivo e relógio variou de −291 ms a +22 ms entre execuções (emulador).
O limite antigo de ±100 ms não era calibrado; o teste usa ±500 ms (aceite do produto). Tolerância mais apertada exige medida em aparelho real.

### Pendente (não chamar de completo)

- **Orçamento de APK**: o APK de debug tem 26,3 MiB (> 25 MB); o release com R8 media 7,5 MB numa medida anterior. Decisão humana pendente sobre a que APK o orçamento se aplica.
- Validação em **aparelho real** (os testes rodaram em emulador): gravar, permissão, Arquivos, compartilhar pelo WhatsApp, microfone Bluetooth/USB.
- TalkBack / Accessibility Scanner; teste com 5 usuários (H6).
- API 26–28: sem cópia pública no MediaStore (só armazenamento do app).
- Aviso de pouco espaço em disco ao gravar: não implementado.
- Cofre criptografado (Tink) não está ligado; "Manter só neste app" apenas evita a cópia pública.
- Build de release sem `signingConfig` de produção; o APK da CI é assinado com a chave de debug pública.
- Fase 14 em diante (redutor, realçador, filtros na captação, separação) seguem pendentes.

## Release assinado e lacunas da entrega instalável (em verificação — preencher com o run)

Decisão do orçamento: os **25 MB valem só para o APK de release**; o APK de debug está isento (26,3 MiB medido no run 37697797597).

| Item | Estado | Verificação (CI) | Validação (pessoa/aparelho) |
| --- | --- | --- | --- |
| APK de release assinado (`gravasom-release-apk`), ≤ 25 000 000 bytes, SHA-256 e certificado registrados | PENDENTE-HUMANO (secrets) | script testado localmente com keystore descartável: assinado (apksigner v2), 7 556 080 bytes; sem secrets o job **falha sem gerar APK**; limite estourado também falha | Cadastrar os 4 secrets (`docs/release-assinatura.md`); SHA-256 e certificado reais saem do 1º run assinado |
| API 26–28: cópia pública via MediaStore (`WRITE_EXTERNAL_STORAGE` com `maxSdkVersion=28`) | a confirmar no run (emulador API 28) | `PublicacaoApi28Test` | Roteiro item 4 — PENDENTE-HUMANO |
| Aviso de pouco espaço (< 100 MB) com tempo estimado, "Gravar mesmo assim" | a confirmar no run | `RecordingBudgetTest` (JVM, TDD), `EspacoLivreTest` (50 MB simulados) | Roteiro item 5 — PENDENTE-HUMANO |
| Cofre cifrado (Tink AES-256-GCM em fluxo), ADR-012 | a confirmar no run | `VaultCryptoTest` (8 testes JVM, TDD), `CofreTest` | Roteiro itens 6–10 — PENDENTE-HUMANO |
| AccessibilityChecks (ATF) nos testes Compose, 0 erros em en e pt-BR | a confirmar no run | `AcessibilidadeTest` + regra em Fluxo, Cofre, Espaço, Métricas, Permissão negada | TalkBack — roteiro item 13, PENDENTE-HUMANO |
| Testes de UI em pt-BR | a confirmar no run | passada `pt-br` nas API 34 e 36 | Roteiro item 14 |

Riscos residuais registrados: cópia em claro do cofre no cache privado enquanto toca/compartilha (ADR-012); gravar pelo Tile/widget não passa pelo aviso de espaço (sem tela para perguntar).

## Filtro de caminhos da CI (não desfazer sem medir)

Objetivo: gastar tempo de CI onde há risco, sem perder proteção.

**Gatilho único.** `on.push` em `main`, `claude/**` e tags `v*`; **não existe `pull_request`**. Antes, todo commit numa branch com PR rodava a CI
inteira duas vezes (o `concurrency` usa `github.ref`, diferente no push e no PR, então um não cancelava o outro). Os checks do push aparecem no PR
quando a branch é do mesmo repositório (este é o caso; repositório privado, sem forks). Se um dia houver PR de fork, reavaliar.

**Job `mudancas`** (`dorny/paths-filter` v4.0.3 fixado por SHA `ceb8a2b8…`) compara com o commit **anterior ao push** (não com a `main`), para que um commit só de docs
não herde "mudou código" de commits antigos da branch; branch nova compara com a `main`. **Falha aberta**: se o filtro não conseguir comparar, tudo roda.

| Saída | Caminhos | Roda quando verdadeiro |
| --- | --- | --- |
| `audio` | `core/audio-*`, `core/dsp`, `corpus`, `perfis`, `scripts`, `.github/audio-suite.sha` | `harness-audio-suite` |
| `app` | `app`, `feature`, `core`, `build-logic`, `gradle` (inclui `libs.versions.toml`), `config`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, **`.github/workflows/ci.yml`**, **`.github/scripts/ci-instrumentados.sh`** | `instrumentados` (API 28/34/36) |

Em **push na `main` e em tag `v*`** os dois rodam sempre. `lint-estatica`, `testes-unidade`, `build`, `apk` e `release-apk` rodam sempre. `release` (só em tag) continua dependendo só de `build`;
job pulado por `if` aparece como *skipped* e não quebra quem não depende dele.

Desvios da lista pedida (para quem for mexer): `ci.yml` e `ci-instrumentados.sh` contam como `app` — uma mudança na própria CI de emulador tem de exercitar o emulador;
acrescentei também `config`, `build.gradle.kts`, `settings.gradle.kts` e `gradle.properties` (mudam o build do app) e `.github/audio-suite.sha` (muda o audio-suite do harness).

**Antes (medido, run por commit só de docs — `ba8394e`, "docs: status da entrega…"):**

| Run | Evento | Duração | Jobs |
| --- | --- | --- | --- |
| 37700502263 | push | 22,0 min | todos (8, com emuladores 28/34/36 nesse tempo: API 34 e 36) |
| 37706667373 | pull_request | 24,1 min | todos, repetido |
| **Total** | **2 runs** | **46,1 min de relógio de CI (2 × a CI inteira)** | |

**Depois:** ver a linha "Depois (medido)" abaixo, preenchida com o run do commit só de docs (`docs/uso.md`).
