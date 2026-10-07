# Progresso da execução

Atualizado: 2026-10-07

## Retomada

1. Ler este arquivo.
2. Rodar `sha256sum -c evidencias/manifesto.sha256`. Falha → ARQUIVOS_SUMIRAM.
3. Continuar do "próximo lote planejado".

## Estado por fase

| Fase | Estado | Último lote verde | Commit | Próximo lote |
| --- | --- | --- | --- | --- |
| 0 (ambiente) | APROVADA | — | 877cf0f | — |
| 2 (fundação) | APROVADA | lote-02 (C4, CI, detekt) | 140171b | — |
| 1 (UX) | PENDENTE | — | — | personas + como podemos + crazy 8s |
| 3 (motor áudio) | PENDENTE | — | — | testes da máquina de estados (vermelho) |
| 4 (serviço) | PENDENTE | — | — | RecordingService |
| 5 (biblioteca) | PENDENTE | — | — | Room + Paging |
| 6 (player/edição) | PENDENTE | — | — | ExoPlayer + trim |
| 7 (superfícies) | PENDENTE | — | — | Tile + widget |
| 8 (privacidade) | PENDENTE | — | — | cofre Tink |
| 9 (endurecimento) | PENDENTE | — | — | Baseline Profile |

## Gates da Fase 2

| Gate | Estado | Log |
| --- | --- | --- |
| gradle help configura tudo | APROVADO | evidencias/fase-2/gradle-help.log (exit=0) |
| detekt roda | APROVADO | evidencias/fase-2/detekt.log (exit=0) |
| ADRs 001–006 | APROVADO | docs/adr/ (6 arquivos, no manifesto) |
| C4 + sequência | APROVADO | docs/architecture/ (5 arquivos, no manifesto) |
| CI (arquivo) | APROVADO | .github/workflows/ci.yml (no manifesto) |
| CI (execução) | PENDENTE-CI | sem runner GitHub Actions neste ambiente |

## Manifesto

`evidencias/manifesto.sha256` — atualizado a cada lote.
