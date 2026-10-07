# Issue para danzeroum/audio-suite — `ref_quality`: alinhamento O(n²) e `_si_sdr` sem limite de atraso

Texto pronto para colar. Patch: `0001-ref_quality-alinhamento-por-fft.patch` (aplica limpo sobre `1e3fa85`).

## Resumo

`ref_quality._align` usa `np.correlate(ref, deg, mode="full")` (O(n²)) sobre o sinal inteiro e `_si_sdr` o chama com
`sr=0`. O resultado é correto, mas lento em arquivos longos e sem limite de atraso procurado.

## Medidas (CI do Gravasom, mesma máquina, sem outro processo concorrente)

Par: locução real do LibriSpeech (84-121123-0001) × a mesma com ruído rosa a 15 dB SNR.

| Caso | Antes (`1e3fa85`) | Com o patch |
| --- | --- | --- |
| 10 s a 48 kHz, `speech-full-ref` | 4,0 s | 0,7 s |
| 10 s a 48 kHz, `source-sep-ref` | 3,8 s | 0,7 s |
| 4 s a 8 kHz, ambos os modos | 0,4–0,5 s | 0,7 s (inclui a partida do processo) |
| Valores | SI-SDR 14,9966 dB; STOI-proxy 0,7258 | **idênticos** |

O custo cresce com o quadrado da duração (arquivos de minutos ficam impraticáveis); com FFT cresce ~n log n.

## Correção de uma observação anterior (nossa)

Numa primeira rodada reportamos 48–120 s por par e `status=error` no modo `source-sep-ref`. **Isso não se reproduz**:
foi medido com outro processo pesado ainda rodando em segundo plano. Isolado, não há erro. Esta issue é de **desempenho**,
não de defeito funcional.

## Mudança proposta

1. `_align`: correlação cruzada por `scipy.signal.fftconvolve` (scipy já é dependência), atraso limitado a
   `max_lag_s=1.0` s quando há taxa de amostragem, mesma semântica de sinal do lag.
2. `_si_sdr(ref, deg, sr=0)`: recebe a taxa real (o chamador passa `audio.sample_rate`); `sr<=0` mantém o comportamento sem limite.

## Testes incluídos (`tests/unit/analyzers/test_ref_quality_align_fft.py`)

- equivalência com o método direto em atrasos −37, 0, 12 e 250 amostras;
- SI-SDR + STOI-proxy de 4 s a 8 kHz em < 2 s;
- o atraso escolhido respeita o limite de 1 s.

Os testes existentes de `ref_quality` (`test_T41_T55_phase2.py -k ref_quality`) continuam verdes.

## Pedido relacionado (separado)

O pacote instalado via `pipx install git+https://github.com/danzeroum/audio-suite@<sha>` não inclui
`audio_suite/default_profile.yaml` (dado de pacote ausente): `audio-suite analyze` sem `--profile` falha com
`profile not found: .../site-packages/audio_suite/default_profile.yaml`. Sugestão: declarar o arquivo em `package-data`.
