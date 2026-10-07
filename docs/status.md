# Status dos gates

Atualizado: 2026-10-07

Cada gate só é APROVADO com o comando, o exit code e a saída salvos em `evidencias/`.

## Fase 0 — Ambiente (APROVADA)

| Gate | Estado | Evidência / motivo |
| --- | --- | --- |
| JDK 17 (toolchain) | APROVADO | evidencias/fase-0/jdk17.log — Temurin 17.0.20.1, exit=0 |
| Android SDK 36 + build-tools + platform-tools + NDK 27 | APROVADO | evidencias/fase-0/android-sdk.log |
| Gradle 8.11.1 | APROVADO | .tooling/gradle-8.11.1 |
| Canal em lote | APROVADO | evidencias/00-canal-lote.md |

## Fase 2 — Fundação (APROVADA)

| Gate | Estado | Evidência / motivo |
| --- | --- | --- |
| gradle help configura todos os módulos | APROVADO | evidencias/fase-2/gradle-help.log — exit=0 |
| detekt roda | APROVADO | evidencias/fase-2/detekt.log — exit=0 |
| ADRs 001–006 | APROVADO | docs/adr/ |
| C4 (4 níveis) + sequência | APROVADO | docs/architecture/ |
| Pipeline CI (arquivo) | APROVADO | .github/workflows/ci.yml |
| Pipeline CI (execução) | PENDENTE-CI | sem runner GitHub Actions |

## Fase 1 — Descoberta e UX (APROVADA)

| Gate | Estado | Evidência / motivo |
| --- | --- | --- |
| Personas, Como podemos, Crazy 8s, wireframes, navegação, roteiro | APROVADO | docs/ux/ (5 arquivos) |
| Teste com 5 usuários | PENDENTE (validação humana) | exige usuários reais |

## Fase 3 — Motor de áudio (APROVADA no núcleo)

| Gate | Estado | Evidência / motivo |
| --- | --- | --- |
| TDD vermelho | APROVADO | evidencias/fase-3/tdd-vermelho.log — exit=1 |
| TDD verde | APROVADO | evidencias/fase-3/tdd-verde.log — exit=0 |
| Testes de borda (0 s, 2 GB, pausa 50×) | APROVADO | evidencias/fase-3/edge-tests.log — exit=0 |
| Cobertura ≥ 80% em audio-engine | APROVADO | evidencias/fase-3/kover.log — 95,35%, exit=0 |
| :core:domain compila | APROVADO | evidencias/fase-3/domain-compile.log — exit=0 |
| :core:audio-android compila | APROVADO | evidencias/fase-3/audio-android-compile.log — exit=0 |
| M4A/WAV/MP3 lidos por MediaExtractor (±100 ms) | PENDENTE-CI | exige emulador/dispositivo |
| MP3 nativo (LAME vendorizado) | PENDENTE-CI | bridge JNI stub; LAME não vendorizado (ADR-002) |

## Fases 4–9

PENDENTE (a executar).
