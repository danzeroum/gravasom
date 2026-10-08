# Corpus de validação do harness (ADR-009)

Nada grande no git: fontes externas são baixadas **por hash** (`corpus/corpus.sha256`) e o CI guarda cache.

## Sinais sintéticos (gerados por `core/dsp/src/hosttest/corpus_tool.cpp`, determinísticos)

seno 1 kHz, varredura 20 Hz–20 kHz, fala sintética (fonte glotal + formantes + ruído de sala de −60 dBFS),
ruído branco/rosa/marrom, zumbido de 60 Hz com 8 harmônicos, ruído de ventilador (banda larga + tom de 150 Hz),
misturas fala+ventilador (SNR 0 e 10 dB), fala+rosa (SNR 0 dB), fala+zumbido (SNR 10 dB).
PRNG próprio (xorshift64*) com sementes fixas: mesmo binário ⇒ mesmos bytes.

## Fala real

| Fonte | Licença | Uso |
| --- | --- | --- |
| LibriSpeech `dev-clean` (Panayotov, Chen, Povey, Khudanpur, ICASSP 2015), https://www.openslr.org/12 | **CC BY 4.0** (atribuição obrigatória; áudio derivado de LibriVox, domínio público) | 6 locuções de 6 falantes (`corpus/falas_reais.txt`), convertidas a WAV 48 kHz mono só para medição |

- Tarball: sha256 `76f87d090650617fca0cac8f88b9416e0ebf80350acb97b343a85fa903728ab3` (MD5 `42e2234b…300a1` confere com o publicado pelo OpenSLR).
- As locuções **não são redistribuídas** neste repositório; o script baixa e converte no CI (`scripts/corpus-real.sh`).

## Música

Ainda não há música real no corpus. Entra na Fase 17 (spike de separação) com mixagens sintéticas de fontes conhecidas
e, para música real, fonte com licença registrada aqui **antes** de baixar.

## Saídas "boas" e controles positivos

Para cada base (fala sintética + 6 reais): `__hp80` (passa-altas 80 Hz = saída boa), e os controles
`__ctrl_ganho3db_clip`, `__ctrl_pitch_mais50c`, `__ctrl_corte50ms`, `__ctrl_cliques`, `__ctrl_ruido_reinjetado`.
