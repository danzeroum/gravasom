# ADR 001 — Formato padrão de gravação: M4A (MP4/AAC-LC)

Status: aceito

## Contexto

O app precisa de um formato padrão para o primeiro toque. Três formatos são suportados:
M4A (MediaRecorder + AAC-LC), WAV (AudioRecord + PCM) e MP3 (PCM + LAME via NDK).

## Decisão

M4A (MP4/AAC-LC) é o padrão.

## Consequências positivas

- Codificação nativa: sem dependência externa, sem custo de licença, sem NDK.
- Arquivo pequeno (~1 MB/min a 128 kbps), bom para longas gravações.
- Pausa/retomada nativas no MediaRecorder desde a API 24.
- Compatível com MediaExtractor e Media3 sem conversão.

## Consequências negativas (o preço)

- Compressão com perdas: não serve para masterização fiel.
- Menos controle sobre os buffers do que AudioRecord; o VU preciso por amostra
  não é possível no caminho do MediaRecorder.

## Alternativas descartadas

- WAV como padrão: fidelidade máxima, mas ~5 MB/min e cabeçalho que precisa de
  correção ao final; caro para o usuário casual.
- MP3 como padrão: dependência nativa, +2–3 MB no APK e licença LGPL.

## Contexto de avaliação

Toda decisão tem seu preço: o M4A troca fidelidade por tamanho e simplicidade.
Só faz sentido porque o público-alvo (aula, entrevista, ideia musical) prioriza
compartilhar rápido e não estourar o armazenamento.
