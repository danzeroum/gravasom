# Status dos gates

Atualizado: 2026-10-07 (Fase 10, reclassificação honesta de 27839e9)

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
| 2 — Fundação | APROVADA-PARCIAL | CI nunca executado (PENDENTE-CI → Fase 11); `gradlew` não estava versionado, corrigido na Fase 10 |
| 3 — Motor de áudio | APROVADA-PARCIAL | Duração ±100 ms via MediaExtractor (nunca medida); MP3 é stub (Fase 12) |
| 4 — Serviço | APROVADA-PARCIAL | Só compila + testes JVM do engine; 100 ciclos de interrupção e foreground em emulador (Fase 11) |
| 5 — Biblioteca | APROVADA-PARCIAL | Room/DAO só compilam; falta teste instrumentado de Room (Fase 11) |
| 6 — Player/edição | APROVADA-PARCIAL | Trim WAV testado (TDD); player Media3 só compila, sem tocar/buscar/pausar medido (Fase 11) |
| 7 — Superfícies | APROVADA-PARCIAL | Tile e widget só compilam (Fase 11) |
| 8 — Privacidade | APROVADA-PARCIAL | Cofre/PrivacyManager só compilam; MobSF PENDENTE-CI |
| 9 — Endurecimento | APROVADA-PARCIAL | Release/detekt/kover medidos; Baseline Profile, Test Lab, Play, Accessibility Scanner pendentes |

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
