# Status dos gates

Atualizado: 2026-10-07

Cada gate só é APROVADO com o comando, o exit code e a saída salvos em `evidencias/`.

## Fase 0 — Ambiente

| Gate | Estado | Evidência / motivo |
| --- | --- | --- |
| JDK 17 (toolchain) | APROVADO | `evidencias/fase-0/jdk17.log` — Temurin 17.0.20.1, exit=0 |
| Android SDK 36 | APROVADO | `evidencias/fase-0/android-sdk.log` — platforms/android-36 presente |
| build-tools 36.0.0 | APROVADO | `evidencias/fase-0/android-sdk.log` |
| platform-tools | APROVADO | `evidencias/fase-0/android-sdk.log` |
| NDK 27.0.12077973 | APROVADO | `evidencias/fase-0/android-sdk.log` |
| Gradle 8.11.1 | APROVADO | instalado em `.tooling/gradle-8.11.1` |
| Diagnóstico do canal de gravação | APROVADO | `evidencias/00-diagnostico-gravacao.md` — causa WRITE_QUEBRADO em lote |

## Fase 2 — Fundação

| Gate | Estado | Evidência / motivo |
| --- | --- | --- |
| `gradle help` configura todos os módulos | APROVADO | `evidencias/fase-2/gradle-help.log` — BUILD SUCCESSFUL, exit=0 |
| build-logic com convention plugins | APROVADO | 5 plugins compilam; `build-logic:jar` no log |
| Catálogo de versões (libs.versions.toml) | APROVADO | resolvido pelo build |
| Manifesto sha256 | APROVADO | 37 arquivos, `sha256sum -c` sem falhas |
| ADRs 001–006 | PENDENTE | a escrever |
| C4 (Contexto, Container, Componente, Código) | PENDENTE | a escrever |
| Pipeline GitHub Actions | PENDENTE-CI | sem runner neste ambiente; arquivo a escrever |
| Detekt/ktlint no build | PENDENTE | config a escrever |

## Paradas nomeadas

Nenhuma até agora.
