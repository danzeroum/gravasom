# ADR 014 — Pular silêncio ao ouvir: WebRTC VAD, analisado ao salvar

Status: aceito (Onda 1 da v2)

## Contexto

`RecordingConfig.skipSilence` existia e não fazia nada. O plano da v2 pede "pular silêncio na reprodução", com duas condições:
**não usar só o nível do VU** (um ventilador passa por fala) e **não remover nenhuma fala**. Ordem de tentativa: WebRTC VAD primeiro;
Silero só se o WebRTC cortar fala no corpus com ruído.

## Decisão

1. **VAD de verdade, não nível.** O WebRTC VAD (C, BSD-3, do projeto WebRTC; vendorizado sem alteração a partir do sdist
   `webrtcvad 2.0.10` do PyPI, sha256 em `core/dsp/src/main/cpp/webrtc_vad/VENDOR.sha256`) vive em `libgravasom_dsp.so`.
   `VadStream` (C++, sem JNI) classifica 16 kHz mono em quadros de 20 ms; `WebRtcVad` (Kotlin) é o `SpeechClassifier`.
2. **Calculado ao salvar, não ao ouvir.** `RecordingIngestor` dispara `RecordingAnalyzer` em segundo plano: decodifica
   (MediaCodec/WAV direto), faz uma passada que produz **onda** (1 byte por 10 ms) e **fala por quadro** para os dois níveis
   de sensibilidade, e guarda no Room (`recording_analysis`, ~0,4 MB por hora). Gravações do cofre **não** persistem a análise
   (o desenho da atividade de fala seria um vazamento); ficam só em memória. O que falta (importadas, ou o app morreu antes) é
   calculado na primeira abertura.
3. **Plano de pulo (`SkipPlan`).** Qualquer quadro de fala, por isolado que seja, protege uma margem: **200 ms antes e 500 ms
   depois** (o fim das frases some devagar e o VAD o declara silêncio antes); só pula se, depois das margens, sobram **≥ 800 ms**.
   Erra para o lado de não pular. Uma lacuna longa deixa 0,7 s de pausa natural (aceite: ≤ 1 s a mais que a fala).
4. **Execução.** `PlayerController` confere a posição a cada 40 ms enquanto toca e, dentro de uma lacuna, salta para o fim dela.
5. **Dois níveis para o usuário:** Suave (modo 1 do VAD) e **Normal (modo 2, padrão)**. O modo 3 ("forte") **não é oferecido**:
   cortou fala com parte das margens testadas e rendeu ~4 pontos a mais do ideal (tabela abaixo). Desligado por padrão; a escolha persiste.
6. **Silero não entra.** A condição ("só se o WebRTC cortar fala no corpus com ruído") não ocorreu nos níveis oferecidos.

## Evidência (harness `harness-vad` da CI; reproduzível com `scripts/test-vad-host.sh` + `VadCorpusEvalTest`)

Corpus pt-BR: **FLEURS pt_br (Google, CC BY 4.0)**, baixado **por hash** (`corpus/corpus.sha256`; 8 locuções masculinas do `dev` e 8
femininas do `train`, `corpus/falas_pt_vad.txt`). Com elas, `scripts/vad_corpus.py` monta 6 gravações de ~1 min no feitio de aula
ou reunião (4 locuções com pausas de 1,2 a 15 s) e as renderiza em 6 cenários: sala silenciosa (44,1 kHz), ventilador a SNR 10 dB
e a 0 dB (48 kHz), ruído rosa a 0 dB, zumbido de 60 Hz a 10 dB e murmúrio de vozes a 5 dB (36 arquivos, 52 122 quadros de fala).
O código medido é o de produção (decodificado → reamostragem para 16 kHz → WebRTC VAD por JNI → `SkipPlan`), rodando em JVM com a
`.so` de host.

**Verdade-terreno (oráculo):** vem do sinal limpo, não do VAD. Âncora nos quadros vozeados e fortes de cada locução (autocorrelação,
na banda de 250 Hz a 4 kHz), ampliada em 150 ms antes e 200 ms depois (consoantes surdas nas pontas), mantendo só quadros a
até 30 dB do percentil 95 e que não sejam ronco (> 80 % da energia abaixo de 150 Hz). Respiração e ruído de sala nas pontas das
locuções FLEURS **não** contam como fala. É um proxy (não há alinhamento por palavra); por isso, além dele, a Onda 3 comparará o
WER da transcrição antes e depois do pulo.

Resultado, todos os cenários juntos (`n` = trechos de fala do oráculo; "cortados" = trechos com algum quadro dentro de uma lacuna pulada):

| modo | margens | trechos de fala | trechos cortados | quadros cortados / fala | economia (% da duracao) | % do ideal |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| suave (1) | 150/400 | 600 | 2 | 2/52122 | 7.6% | 14% |
| suave (1) | 200/500 | 600 | 0 | 0/52122 | 7.5% | 14% |
| suave (1) | 250/700 | 600 | 0 | 0/52122 | 7.2% | 14% |
| suave (1) | 300/300 | 600 | 2 | 4/52122 | 7.6% | 14% |
| normal (2) | 150/400 | 600 | 2 | 2/52122 | 7.9% | 15% |
| normal (2) | 200/500 | 600 | 0 | 0/52122 | 7.8% | 15% |
| normal (2) | 250/700 | 600 | 0 | 0/52122 | 7.5% | 15% |
| normal (2) | 300/300 | 600 | 2 | 4/52122 | 7.9% | 15% |
| forte (3, nao oferecido) | 150/400 | 600 | 2 | 6/52122 | 10.0% | 19% |
| forte (3, nao oferecido) | 200/500 | 600 | 2 | 3/52122 | 9.8% | 19% |
| forte (3, nao oferecido) | 250/700 | 600 | 0 | 0/52122 | 9.5% | 19% |
| forte (3, nao oferecido) | 300/300 | 600 | 2 | 6/52122 | 9.9% | 19% |

Leituras:

- **Fala cortada: 0** para Suave e Normal com as margens padrão (200/500) em todos os cenários. Margens simétricas de 300 ms
  cortaram 2 trechos (4 quadros = 80 ms): a cauda precisa ser maior que a cabeça. O modo 3 cortou 2 trechos com 200/500.
- **Tempo economizado (medido):** em sala silenciosa, o nível Normal pula **46,5 % da duração** do corpus (89 % do ideal teórico). O
  corpus tem pausas longas de propósito (média de 6 s por locução de 10 s); numa gravação real a fração segue o seu ritmo de fala.
- **Limite honesto:** com ruído de fundo contínuo (ventilador, zumbido, murmúrio, ruído rosa), o WebRTC VAD declara tudo como fala:
  **a economia cai a 0 %**. É o lado seguro do erro (não corta nada), mas o recurso só rende em sala razoavelmente silenciosa. O
  redutor de ruído da Onda 2, aplicado antes do VAD, é o caminho para melhorar isso; fica registrado, não prometido.

## Licenças

WebRTC (BSD-3, Copyright The WebRTC project authors) e o empacotamento `py-webrtcvad` (MIT, John Wiseman): `docs/licencas/WebRTC-VAD.md`.
FLEURS: CC BY 4.0, atribuição em `CORPUS.md`. Nada do corpus é redistribuído pelo repositório.

## Verificação × validação

| Item | Verificação (CI) | Validação (o dono no celular) |
| --- | --- | --- |
| Não remove fala | `VadCorpusEvalTest` (0 trechos cortados, margens padrão) | ouvir 3–4 gravações suas com o pulo ligado e anotar cortes em `docs/uso.md` |
| Tempo economizado | tabela acima; `PularSilencioTest` no emulador (silêncio de 10 s) | "Pula X de silêncio" no player confere com o que você sente |
| 10 s de silêncio → ≤ 1 s a mais que a fala | `SkipPlanTest` (plano) e `PularSilencioTest` (relógio no emulador) | uma reunião real com pausas |
