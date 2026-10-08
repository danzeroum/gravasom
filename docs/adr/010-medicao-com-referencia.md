# ADR 010 — Medição com referência (SI-SDR, SI-SDRi, STOI) e papel do audio-suite

Status: aceito (decisão do responsável após a parada `TETO_NAO_DISCRIMINA` da Fase 13)

## Contexto

O gate de inteligibilidade do plano usava o `speech_intelligibility` do audio-suite, que é um proxy **sem referência**.
Medido no corpus (7 bases, ruído rosa reinjetado a 15 dB): a queda do proxy foi +0,016, −0,018 e −0,045 em 3 das 7 bases
(o ruído não derrubou, ou até subiu, o proxy). Um teto assim aceita a saída boa mas não pega o controle: `TETO_NAO_DISCRIMINA`.

## Decisão

1. **Gate objetivo com referência, em medidor próprio** (`scripts/medidor_ref.py`), em **venv separado** do audio-suite
   (`scripts/requirements-medidas.txt`: numpy 2.5.3, scipy 1.18.1, pystoi 0.4.1, MIT). Nunca no ambiente do pipx.
2. **No corpus sintético (referência exata conhecida)**: SI-SDRi (melhora) e STOI com referência.
3. **Nas 7 bases reais (sem referência exata)**: STOI-proxy e descritores do audio-suite são **só observação**, mais o kit de
   escuta A/B cego (`scripts/kit-escuta.sh`).
4. O **audio-suite segue como gate** de clipping, true peak, glitch, loudness, F0 e duração (ADR-009), e o `ref_quality`
   dele entra como **segunda medida**, sem trocar a primeira.

## Cálculo

- **Alinhamento**: atraso grosso por correlação cruzada FFT (`fftconvolve`, O(n log n)) numa janela de ±100 ms; refino em ±16
  amostras escolhendo o lag que **maximiza o próprio SI-SDR** (a fase de um filtro desloca o pico da correlação). O atraso medido é
  registrado; se o algoritmo declara o seu atraso, o medidor confere (±1 amostra) ou falha.
- **SI-SDR** (Le Roux et al.): remover a média; `alvo = (<est,ref>/<ref,ref>)·ref`; `SI-SDR = 10·log10(‖alvo‖² / ‖est − alvo‖²)`,
  limitado a 100 dB (saída idêntica daria infinito). **SI-SDRi** = SI-SDR(limpa, saída) − SI-SDR(limpa, entrada ruidosa).
- **STOI** com referência: `pystoi.stoi(ref, saída, fs)`; ESTOI também registrado.
- **Teto** (`scripts/calibrar_ref.py`): `teto = (pior_bom + melhor_controle) / 2`. **Discrimina** se
  `pior_bom − melhor_controle ≥ 5 × max(dispersão dos bons, resolução do medidor)`; a resolução do STOI é 0,001. Sem número mágico.
- **Controles positivos que têm de REPROVAR**: ruído reinjetado (15 dB), supressão agressiva (gate por quadro que corta fala) e,
  nos gates de preservação, a fala limpa de entrada não pode piorar além do teto. Todos os controles de todas as bases têm de ficar
  abaixo do teto.

## Números medidos hoje (STOI com referência, 7 bases)

pior bom (hp80 e bypass) **0,9996**; melhor controle **0,9823**; gap 0,0173 (exigido 0,0050); teto **0,9909**; discrimina. Detalhe:
`perfis/CALIBRACAO_REF.md`. SI-SDRi: **AGUARDANDO_BONS** (não há redutor ainda; o teto será calibrado com a saída dele na Fase 14).
Se a saída do redutor ficar abaixo do melhor controle, será nova parada `TETO_NAO_DISCRIMINA`, não um teto afrouxado.

## Consequências

- O SI-SDR **não** serve para medir preservação por filtros que removem o grave que a fala real "limpa" contém (passa-altas 80 Hz:
  STOI 0,9997, SI-SDR ~7 dB). Por isso a preservação usa STOI e o SI-SDRi só vale com referência exata (misturas sintéticas).
- Patch e issue para o audio-suite (alinhamento O(n log n)): `docs/audio-suite/`. Medido: 12,5 s a 48 kHz leva 46–48 s sem o patch e 1,1 s com ele, valores idênticos.
  A segunda medida (`PERFIL_AS=<perfil> calibrar_ref.py calibrar`) é opt-in: só é prática com o audio-suite corrigido (o pin atual leva
  ~23 min para o corpus). O `status=error` isolado não foi reproduzido (ver issue).
