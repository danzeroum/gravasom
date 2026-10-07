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
| 2 (fundação) | EM ANDAMENTO | lote-01 (ADRs 001–006) | pendente | C4 (Contexto, Container, Componente, Código) |
| 1 (UX) | PENDENTE | — | — | personas + crazy 8s |
| 3 (motor áudio) | PENDENTE | — | — | testes da máquina de estados (vermelho) |
| 4 (serviço) | PENDENTE | — | — | RecordingService |
| 5 (biblioteca) | PENDENTE | — | — | Room + Paging |
| 6 (player/edição) | PENDENTE | — | — | ExoPlayer + trim |
| 7 (superfícies) | PENDENTE | — | — | Tile + widget |
| 8 (privacidade) | PENDENTE | — | — | cofre Tink |
| 9 (endurecimento) | PENDENTE | — | — | Baseline Profile |

## Gates pendentes da Fase 2

- C4 (Contexto, Container, Componente, Código) — a escrever
- Pipeline GitHub Actions — a escrever (PENDENTE-CI para execução)
- Detekt/ktlint no build — config a escrever

## Manifesto

`evidencias/manifesto.sha256` — atualizado a cada lote.
