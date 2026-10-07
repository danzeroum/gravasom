# Progresso da execução

Atualizado: 2026-10-07

## Retomada

1. Ler este arquivo.
2. Rodar `sha256sum -c evidencias/manifesto.sha256`. Falha → ARQUIVOS_SUMIRAM.
3. Continuar do "próximo lote planejado".

## Estado por fase

| Fase | Estado | Commit | Próximo lote |
| --- | --- | --- | --- |
| 0 (ambiente) | APROVADA | 877cf0f | — |
| 2 (fundação) | APROVADA | 84c3e52 | — |
| 1 (UX) | APROVADA | 0b99f2d | — |
| 3 (motor áudio) | APROVADA (núcleo) | 81b7916 | fechamento |
| 4 (serviço) | PENDENTE | — | RecordingService + foco de áudio + interrupções |
| 5 (biblioteca) | PENDENTE | — | Room + Paging + MediaStore |
| 6 (player/edição) | PENDENTE | — | ExoPlayer + trim |
| 7 (superfícies) | PENDENTE | — | Tile + widget Glance |
| 8 (privacidade) | PENDENTE | — | cofre Tink + consentimento |
| 9 (endurecimento) | PENDENTE | — | Baseline Profile + release |

## Gates PENDENTE-CI (exigem emulador/runner)

- MediaExtractor abre M4A/WAV/MP3 com duração ±100 ms
- Gradle Managed Devices (API 26/30/34/36)
- Firebase Test Lab (≥ 6 aparelhos)
- Macrobenchmark de início a frio
- Publicação no canal interno do Play

## Manifesto

`evidencias/manifesto.sha256` — atualizado a cada lote.
