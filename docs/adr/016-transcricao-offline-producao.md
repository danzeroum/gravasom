# ADR-016 — Transcrição offline em produção: entrega do modelo e integração

Status: proposta (depende de medição no aparelho para virar aceito)

## Contexto

O [ADR-013](013-transcricao-whisper-cpp-spike.md) provou a viabilidade do **whisper.cpp base q5_1** para pt-BR offline (WER ≈ 10%), mas parou onde o spike acaba: sem tela, sem módulo de produção, com o modelo **embutido no APK do spike** (65,7 MB) e com as medições de **bateria, temperatura e RTF no aparelho pendentes**.

Agora a feature vira produto: o usuário precisa (1) ligar a transcrição, (2) ver o texto, (3) escolher idioma, (4) cancelar, (5) tudo sem rede. Este ADR fixa **as decisões que o comparativo (`docs/asr-comparison.md`) deixou abertas**.

## Decisões

### 1. Motor: whisper.cpp, wrapper isolado

O motor é o **whisper.cpp base q5_1** (ADR-013). O código de produção **não reusa o módulo do spike** — o spike fica como está, `-Pspike`, fora do build. O que entra é um módulo `feature/transcribe` que expõe a interface `Transcriber` (domínio) e uma implementação `WhisperCppTranscriber`. Motivo: o spike tem dependências e suposições (`lerWav16k` só, sem streaming, sem chunking, sem cancelamento) que não servem em produção. Isolar também permite trocar por **Vosk** sem tocar na UI nem no worker, se a medição no aparelho reprovar o whisper.

### 2. Entrega do modelo: download opcional pós-instalação, nunca no APK

O modelo base q5_1 pesa 56,9 MiB. O teto do APK de release é **25 MB** (`docs/status.md`) e o app já ocupa ~8 MB. Embutir o modelo **não cabe**. Decisão:

- O APK **não** carrega nenhum modelo.
- O app oferece, em **Ajustes → Transcrição → Gerenciar modelos**, o download do modelo por idioma, **por Wi-Fi**, com consentimento explícito.
- O arquivo é verificado por **SHA-256** e guardado em `context.filesDir/modelos/<lang>/` (fora do APK, dentro do sandbox do app).
- Sem o modelo baixado, a feature fica **desligada** com um aviso claro ("Baixe o modelo de português para transcrever"), nunca com uma transcrição silenciosamente vazia.

> **Nota sobre a regra "sem INTERNET".** O app continua **sem** permissão `INTERNET` no manifesto. O download do modelo usa a **API do sistema** (`DownloadManager`) por `content://` ou um `Intent` de `ACTION_VIEW` para o navegador do usuário baixar e devolver o arquivo pelo seletor de arquivos (SAF). Assim a feature continua offline **no momento de transcrever**, e a única etapa com rede é um ato explícito do usuário fora do processo do app. Se o dono preferir, a alternativa é empacotar o modelo em um **split de ABI** separado na Play Store — só faz sentido quando houver publicação. Registrado como plano B.

### 3. Chunking e streaming

Áudio de entrada é **PCM 16 kHz mono 16 bits**, obtido pelo `AudioConverter` (item 6). Para arquivos longos:

- Janela de **30 s** com **1 s de sobreposição** (evita cortar palavra na fronteira).
- Cada janela é transcrita em `Dispatchers.IO`, com no máximo `n = min(2, núcleos−1)` paralelo (limita pico de RAM).
- Segmentos são costurados pela sobreposição; frases do mesmo locutor no limite são fundidas se o texto termina/começa sem pontuação.
- **Cancelamento cooperativo** via `CoroutineScope.cancel()`; nada é gravado em Room antes do flush.

Isso é o que endereça o pico de 341 MiB do ADR-013 e o orçamento de 200 MB.

### 4. Worker

`TranscriptionWorker : CoroutineWorker` (WorkManager) com:

- **Constraints:** `requiresCharging` opcional (padrão: não), **sem** `NetworkType` (é offline).
- **Foreground** quando o app está em uso (`setForeground`), com tipo `dataSync` e notificação "Transcrevendo…".
- **Cache transitório em DataStore proto** (`TranscriptionProgress`): a cada **10 frases** o worker faz flush para Room dentro de uma transação e limpa o cache. Se o processo morrer, o worker reinicia do último flush, não do zero.
- **Cancelamento** apaga o cache transitório e deixa o registro da gravação intacto (o áudio nunca é tocado — ADR-008).

### 5. Modelo de dados (Room v5 → v6)

Nova tabela `transcripts` com FK `ON DELETE CASCADE` para `recordings`:

```
id INTEGER PK AUTOINCREMENT
recordingId INTEGER NOT NULL  (FK recordings(id) CASCADE)
language TEXT NOT NULL          -- BCP-47: pt-BR, en-US…
format TEXT NOT NULL            -- PLAIN | VTT | SRT
status TEXT NOT NULL            -- PENDING | RUNNING | DONE | FAILED | CANCELLED
text TEXT                       -- preenchido quando DONE
errorMessage TEXT               -- preenchido quando FAILED
createdAtMillis INTEGER NOT NULL
updatedAtMillis INTEGER NOT NULL
UNIQUE(recordingId, language)
```

Migration escrita à mão (padrão do repo), **sem tocar** em `recordings` nem em `edit_projects`/`markers`/`recording_analysis`. Índice único em `(recordingId, language)` para permitir retomar de onde parou sem duplicar.

### 6. Conversão de áudio

O app já tem decodificação em `core/audio-android` (`MediaExtractor`/`MediaCodec` via `PcmCapture`, `DefaultAudioRecorderFactory`). O `AudioConverter` de produção **reusa esse caminho**, não reimplementa:

- Entrada: M4A, WAV, MP3 (os três formatos do app).
- Saída: **PCM 16 kHz mono 16 bits em streaming** para arquivo temporário (`cacheDir/transcrever/<id>.pcm`), nunca carregando o áudio inteiro em memória.
- Cancelável: interrompe `MediaCodec` a qualquer momento e apaga o `.pcm` parcial.
- Reamostragem para 16 kHz: usar o resampler do `AudioConverter`; se não existir, usar `android.media.AudioTrack`/`MediaCodec` com `MediaFormat.KEY_SAMPLE_RATE = 16000`; se nada disso estiver no repo, escrever um resampler linear pequeno **com teste próprio**.

### 7. UI

- **Toggle "Transcrever ao gravar"** na tela de gravação (não é tela nova): entra como campo no estado MVI do `RecordViewModel` (ADR-006), persistido nas preferências junto dos outros ajustes de gravação.
- **Aba "Transcrição"** na tela de detalhe/player: texto (PLAIN) ou lista com timestamps (VTT/SRT), botões "Copiar tudo", "Exportar .txt / .vtt", "Tentar novamente" se falhou, progresso enquanto roda.
- **Tela "Gerenciar modelos de idioma"** em Ajustes: lista de idiomas (pt-BR, en-US, es-ES, fr-FR), botão "Baixar" com progresso/cancelar, tamanho estimado, verificação de bateria (≤ 20% avisa) e Wi-Fi (recomendado, não obrigatório).
- **Consentimento de primeira vez:** diálogo explicando que a transcrição é **100% local** e que o áudio não sai do aparelho.

### 8. Idiomas

- **pt-BR** é o caso principal e o único **obrigatório** para o aceite inicial.
- en-US, es-ES, fr-FR entram como **opcionais** na tela de modelos; cada um exige seu próprio download.
- O idioma fica gravado por transcrição (`transcripts.language`), para o usuário poder transcrever a mesma gravação em dois idiomas sem perder a primeira.

### 9. Privacidade

- Nenhum áudio, nenhum texto e nenhum evento sai do dispositivo. Sem `INTERNET`.
- A transcrição não é logada em `Timber` em nível de texto (só metadados: id, idioma, tempo, status).
- `docs/privacy.md` ganha seção "Transcrição offline" quando a Fase 6 do plano chegar.

## Análise de risco

| Risco | Probabilidade | Impacto | Mitigação |
| --- | --- | --- | --- |
| RTF no aparelho muito pior que no host | média | alto (feature lenta) | Chunking + foreground + medir no aparelho antes de liberar; se reprovar, cair para Vosk ou tiny q5_1 |
| Pico de RAM > 200 MB | média | médio (OOM em mid-range) | Janelas de 30 s + paralelismo ≤ 2 + medir no `Memory Monitor` |
| Bateria/temperatura ruins | média | médio (usuário desliga) | `requiresCharging` opcional; aviso em ≤ 20% de bateria; medir antes de liberar |
| Modelo baixado corrompido | baixa | alto (erro silencioso) | Verificação SHA-256 obrigatória |
| Usuário sem espaço | média | médio | Checar espaço livre (56,9 MiB + margem) antes do download e da transcrição |
| Migração Room corromper DB | baixa | alto | Migration escrita à mão + `MigracaoRoomTest` estendido |
| Fila de transcrições em tablet | baixa | baixo | WorkManager com `UniqueWork` por `recordingId` |

## Consequências

- **Positivo:** feature fica offline de verdade, cobrindo pt-BR; arquitetura permite trocar o motor (whisper ↔ Vosk) sem tocar na UI; migração isolada e reversível (basta desligar a feature e a coluna nova não afeta as antigas).
- **Negativo / dívida:** o usuário precisa baixar 56,9 MiB por idioma — isso é um atrito real e precisa ser dito com honestidade na UI. O download sai do processo do app (navegador + SAF), o que é menos bonito que um download integrado, mas é o preço de manter a regra "sem `INTERNET`".
- **Reavaliar:** se o app for publicado na Play Store, considerar split de ABI para o modelo; se o Vosk medir melhor em pt-BR no aparelho, trocar o motor; se a Apple/Google abrirem ASR pt-BR on-device decente, reconsiderar.

## Verificação × validação

| Item | Verificação (máquina) | Validação (dono no celular) |
| --- | --- | --- |
| Migração 5→6 preserva dados | `MigracaoRoomTest` estendido (Robolectric, DDL exato) | Abrir o app em build antigo, instalar o novo por cima e ver as gravações |
| Conversão M4A/WAV/MP3 → PCM 16 kHz | teste instrumentado com os três formatos | — |
| Cancelamento não deixa resíduo | teste de cancelamento em loop | Cancelar no meio e conferir que a gravação está intacta |
| RTF, RAM, bateria no aparelho | `medir-no-celular.sh` (spike/whisper) | Rodar 10 min de transcrição e conferir temperatura/bateria |
| Sem `INTERNET` | `grep -i internet` no manifest final retorna vazio | — |
