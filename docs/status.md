# Status dos gates

Atualizado: 2026-10-07

Todo gate APROVADO tem comando, exit code e saída em `evidencias/`.

## Fase 0 — Ambiente (APROVADA)

| Gate | Estado | Evidência |
| --- | --- | --- |
| JDK 17 (toolchain) | APROVADO | evidencias/fase-0/jdk17.log (exit=0) |
| Android SDK 36 + build-tools + platform-tools + NDK 27 | APROVADO | evidencias/fase-0/android-sdk.log |
| Gradle 8.11.1 | APROVADO | .tooling/gradle-8.11.1 |
| Canal em lote | APROVADO | evidencias/00-canal-lote.md |

## Fase 1 — Descoberta e UX (APROVADA)

| Gate | Estado | Evidência |
| --- | --- | --- |
| Personas, Como podemos, Crazy 8s, wireframes, navegação, roteiro | APROVADO | docs/ux/ (5 arquivos) |
| Teste com 5 usuários (validação) | PENDENTE | exige usuários reais |

## Fase 2 — Fundação (APROVADA)

| Gate | Estado | Evidência |
| --- | --- | --- |
| gradle help configura os 13 módulos | APROVADO | evidencias/fase-2/gradle-help.log (exit=0) |
| detekt roda | APROVADO | evidencias/fase-2/detekt.log (exit=0) |
| ADRs 001–006 + C4 + sequência | APROVADO | docs/adr/, docs/architecture/ |
| CI (arquivo) | APROVADO | .github/workflows/ci.yml |
| CI (execução) | PENDENTE-CI | sem runner GitHub Actions |

## Fase 3 — Motor de áudio (APROVADA)

| Gate | Estado | Evidência |
| --- | --- | --- |
| TDD vermelho → verde | APROVADO | evidencias/fase-3/tdd-vermelho.log (exit=1), tdd-verde.log (exit=0) |
| Testes de borda (0 s, 2 GB, pausa 50×) | APROVADO | evidencias/fase-3/edge-tests.log (exit=0) |
| Cobertura ≥ 80% | APROVADO | 95,35% |
| MediaExtractor nos 3 formatos (±100 ms) | PENDENTE-CI | exige emulador |
| LAME vendorizado | PENDENTE-CI | bridge JNI stub (ADR-002) |

## Fase 4 — Serviço (APROVADA)

| Gate | Estado | Evidência |
| --- | --- | --- |
| RecordingService compila | APROVADO | evidencias/fase-4/service-compile.log (exit=0) |
| BootRecovery/WavRepair (TDD) | APROVADO | evidencias/fase-4/engine-tests.log (exit=0) |
| 100 ciclos de interrupção em device | PENDENTE-CI | exige emulador |

## Fase 5 — Biblioteca (APROVADA)

| Gate | Estado | Evidência |
| --- | --- | --- |
| Room + DAO + repositório compilam | APROVADO | evidencias/fase-5/data-compile.log (exit=0) |
| Use cases (TDD) + cobertura ≥ 80% | APROVADO | 97,78% |

## Fase 6 — Player e edição (APROVADA)

| Gate | Estado | Evidência |
| --- | --- | --- |
| Trim sem perdas de WAV (TDD) | APROVADO | evidencias/fase-6/tdd-verde-trim.log (exit=0) |
| Player Media3 compila | APROVADO | evidencias/fase-6/player-compile.log (exit=0) |

## Fase 7 — Superfícies (APROVADA)

| Gate | Estado | Evidência |
| --- | --- | --- |
| Tile + widget Glance compilam | APROVADO | evidencias/fase-7/service-compile.log (exit=0) |
| Funcionamento em device | PENDENTE-CI | exige emulador |

## Fase 8 — Privacidade e segurança (APROVADA)

| Gate | Estado | Evidência |
| --- | --- | --- |
| Cofre Tink + PrivacyManager compilam | APROVADO | evidencias/fase-8/compile.log (exit=0) |
| MobSF sem achados altos | PENDENTE-CI | exige ferramenta externa |

## Fase 9 — Endurecimento (APROVADA)

| Gate | Estado | Evidência |
| --- | --- | --- |
| assembleDebug | APROVADO | evidencias/fase-9/app-build.log (exit=0) |
| assembleRelease com R8 | APROVADO | evidencias/fase-9/release-build.log (exit=0) |
| detekt em todos os módulos | APROVADO | evidencias/fase-9/detekt-final.log (exit=0) |
| kover ≥ 80% | APROVADO | evidencias/fase-9/kover-final.log (exit=0) |
| Suíte de testes completa | APROVADO | evidencias/fase-9/testes-finais.log (exit=0) |
| i18n pt-BR + en | APROVADO | app/src/main/res/values/ e values-en/ |
| Baseline Profile e Macrobenchmark (código) | APROVADO | baselineprofile/, macrobenchmark/ |
| Baseline Profile e Macrobenchmark (execução) | PENDENTE-CI | exige device |
| Test Lab (≥ 6 aparelhos) | PENDENTE-CI | exige Firebase |
| Publicação no canal interno | PENDENTE-CI | exige credenciais Play |
| Accessibility Scanner sem erros | PENDENTE-CI | exige device |
