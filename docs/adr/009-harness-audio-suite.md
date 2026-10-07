# ADR 009 — Harness de validação com audio-suite

Status: aceito

## Contexto

Testes de DSP **verificam o algoritmo**; só medição acústica independente **valida que o som melhorou**.
O `audio-suite` (https://github.com/danzeroum/audio-suite) mede loudness, true peak, clipping, glitch,
inteligibilidade, voice_artifacts e pitch (F0) e compara dois arquivos.

## Decisão

1. **Instalação isolada e pinada**: `pipx install git+https://github.com/danzeroum/audio-suite@<SHA>`,
   SHA fixo em `.github/audio-suite.sha` (hoje `1e3fa85d0497f346fa57e9de6f839bc073601664`). Nunca no mesmo
   ambiente Python de outra ferramenta (pipx cria um venv por aplicativo).
2. **Exportação**: os testes (host, JVM e instrumentados) gravam WAV PCM 16/24 bit em `tests-out/`, enviados como
   artefato do CI.
3. **Execução no CI**: `audio-suite analyze <wav> --profile perfis/<perfil>.yaml --format json` por amostra, e
   `audio-suite compare <original> <processado>`.
4. **Exit codes viram estado** (`scripts/audio-suite-gate.sh`):

   | Exit | Estado |
   | --- | --- |
   | 0 | VERDE |
   | 1 | VERMELHO (achado de nível *fail*) |
   | 2, 3, 64, ausente | INDECISO |

   Com `ESTUDIO_EXIGIR_AUDIO_SUITE=1` (CI), **ausente ou indeciso vira VERMELHO**. Localmente, indeciso não reprova
   mas é impresso.
5. **Calibração**: nenhum teto é digitado. Todo limite vem do corpus (`perfis/CALIBRACAO.md` registra
   as medidas). Cada métrica tem **controle positivo**: uma degradação deliberada que **tem** de reprovar
   (+3 dB com clipping, pitch +50 cents, corte de 50 ms, ruído reinjetado). Se o teto aceita a saída boa mas não
   pega o controle → parada `TETO_NAO_DISCRIMINA`.
6. **Descritivos não reprovam**: centroide, BPM etc. são observação; só defeitos objetivos viram falha, e só
   com calibração (princípio do próprio audio-suite).
7. **Corpus**: sinais sintéticos gerados no teste + um conjunto pequeno de fala e música com licença compatível
   registrada em `CORPUS.md`. Nada grande no git: o que for grande é baixado por hash (`corpus/corpus.sha256`).
