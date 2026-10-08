# ADR 008 — Cadeia de realce não destrutiva

Status: aceito

## Contexto

O Gravasom passa a oferecer redução de ruído, realce de voz e (spike) separação de instrumentos.
Voz é dado pessoal (LGPD) e a gravação original é o ativo do usuário.

## Decisão

1. **O original nunca é alterado.** Cada realce é um *passo* com parâmetros. A gravação original é aberta
   só para leitura; o export grava um arquivo **novo**. Prova automática: sha256 do original idêntico antes e
   depois de editar, exportar, desfazer e refazer.
2. **Projeto de edição na Room**: `edit_project` (id, recordingId, versão do esquema, criado/atualizado) e
   `edit_step` (projectId, posição, tipo, parâmetros em JSON, habilitado). Reabrir o projeto reproduz o mesmo
   export (mesmo hash): processamento determinístico + parâmetros persistidos.
3. **Preview A/B em tempo real**: a mesma cadeia roda em blocos de 10 ms (480 amostras a 48 kHz) sobre o
   original; A = bypass, B = cadeia. O bypass e a intensidade 0% devolvem os **bytes idênticos** (caminho sem cópia
   processada, não "quase igual").
4. **Núcleo DSP em C++ (NDK)**, sem alocação no caminho de áudio, sem estado global, determinístico
   (mesma entrada + mesmos parâmetros → mesmo hash). Interface Kotlin fina via JNI (`DspChain`).
   Teste de host (ASan/UBSan) para TDD rápido; instrumentado no emulador para tempo por quadro.
5. **Processamento sempre no dispositivo.** Nenhum áudio sai do aparelho; qualquer envio futuro exige
   consentimento explícito e base legal registrada (LGPD). Não há permissão `INTERNET` nova para o DSP.
6. **Export em streaming** (blocos), nunca carrega a gravação inteira: 2 h de áudio sem OOM; cancelamento
   no meio apaga o `.tmp` e não deixa arquivo corrompido (mesma regra `.tmp` + rename dos gravadores).

## Consequências

- Passos são funções puras de (bloco, parâmetros, estado interno reiniciável): desfazer/refazer = reaplicar a lista.
- Latência do preview = 1 bloco (10 ms) + o atraso do filtro que tiver (declarado por passo).
- Passos com atraso (look-ahead) informam `latencySamples`; o export compensa para manter a **duração idêntica em amostras**.
