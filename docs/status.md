# Status dos gates

Atualizado: 2026-10-07

Cada gate só é APROVADO com o comando, o exit code e a saída salvos em `evidencias/`.

## Fase 0 — Ambiente

| Gate | Estado | Evidência / motivo |
| --- | --- | --- |
| JDK 17 (toolchain) | APROVADO | evidencias/fase-0/jdk17.log — Temurin 17.0.20.1, exit=0 |
| Android SDK 36 | APROVADO | evidencias/fase-0/android-sdk.log |
| build-tools 36.0.0 | APROVADO | evidencias/fase-0/android-sdk.log |
| platform-tools | APROVADO | evidencias/fase-0/android-sdk.log |
| NDK 27.0.12077973 | APROVADO | evidencias/fase-0/android-sdk.log |
| Gradle 8.11.1 | APROVADO | .tooling/gradle-8.11.1 |
| Canal em lote | APROVADO | evidencias/00-canal-lote.md |

## Fase 2 — Fundação

| Gate | Estado | Evidência / motivo |
| --- | --- | --- |
| gradle help configura todos os módulos | APROVADO | evidencias/fase-2/gradle-help.log — BUILD SUCCESSFUL, exit=0 |
| detekt roda | APROVADO | evidencias/fase-2/detekt.log — exit=0 |
| build-logic com 5 convention plugins | APROVADO | compila e empacota no log |
| Catálogo de versões | APROVADO | resolvido pelo build |
| ADRs 001–006 | APROVADO | docs/adr/ |
| C4 (4 níveis) + sequência | APROVADO | docs/architecture/ |
| Pipeline CI (arquivo) | APROVADO | .github/workflows/ci.yml |
| Pipeline CI (execução) | PENDENTE-CI | sem runner GitHub Actions neste ambiente |
| Manifesto sha256 | APROVADO | 50 arquivos, sha256sum -c sem falhas |

## Fases 1 e 3–9

PENDENTE (a executar).
