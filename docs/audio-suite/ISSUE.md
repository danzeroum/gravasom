# Issue para danzeroum/audio-suite — `ref_quality`: alinhamento O(n²) e `_si_sdr` sem limite de atraso

Texto pronto para colar. Patch: `0001-ref_quality-alinhamento-por-fft.patch` (aplica limpo sobre `1e3fa85`).

## Resumo

`ref_quality._align` usa `np.correlate(ref, deg, mode="full")` (O(n²)) sobre o sinal inteiro e `_si_sdr` o chama com
`sr=0`. O resultado é correto, mas lento em arquivos longos e sem limite de atraso procurado.

## Medidas (mesma máquina, um processo por vez)

Par: locução real do LibriSpeech × versão processada, `--only ref_quality`.

| Caso | Antes (`1e3fa85`) | Com o patch |
| --- | --- | --- |
| 12,5 s a 48 kHz (real_fala_5 × hp80), `speech-full-ref` | **46,1 s** | **1,1 s** |
| 12,5 s a 48 kHz, `source-sep-ref` | **47,9 s** | **1,0 s** |
| 10 s a 48 kHz (real_fala_1 × ruído) | 3,8–4,0 s | 0,7 s |
| 4 s a 8 kHz | 0,4–0,5 s | 0,7 s (inclui a partida do processo) |
| Valores | 0,7421 / 4,0766 e 0,7258 / 14,9966 | **idênticos** nos mesmos pares |

Perfil de CPU (cProfile) sem patch: 47,6 s dos 47,7 s estão em `np.correlate` dentro de `_align`. Custo ~O(n²): 42 pares do
nosso corpus (4–12,5 s) levaram **23 min e 79 min de CPU**; com FFT cresce ~n log n.

## Sobre o `status=error`

Numa rodada concorrente (outro processo pesado ativo) vimos `status=error` no `source-sep-ref` após ~86–120 s. **Não
conseguimos reproduzir o `error` isolado**: isolado, o modo retorna valor (às vezes `warning`) sem erro. Num laço longo,
um dos pares devolveu `value=None`; não identificamos a causa e **não** a atribuímos ao audio-suite. Esta issue é de desempenho.

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
