# Spike de transcrição (whisper.cpp) — ADR-013

Prova de conceito **sem tela** e **sem permissão de internet**. O modelo vai **dentro do APK** (decisão do dono, 2026-10-07).
Nada daqui entra no app: o módulo só existe no build com `-Pspike` e nada do whisper.cpp nem dos modelos fica no git (tudo por hash).

## O que mede

| Medida | Onde | Como |
| --- | --- | --- |
| WER pt-BR (60 locuções FLEURS, 10,9 min) e WER da gravação longa de 10 min | host (CI: job `spike-transcricao`, sob demanda) | `scripts/asr_spike.py` |
| Tamanho do APK com o modelo dentro | host | `assembleRelease` do módulo, um por modelo |
| Tempo (fator de tempo real), **bateria**, **temperatura**, estado térmico e memória para 10 min | **o seu celular** | `MedicaoTranscricaoTest` via `scripts/medir-no-celular.sh` |
| APK sem `INTERNET` | CI/celular | `MedicaoTranscricaoTest.apkNaoPedeInternet` e `aapt2 dump permissions` |

Bateria e temperatura **não existem no emulador** nem no host: o harness está pronto, mas a medida é do dono (validação), em aparelho real, fora da tomada.

## Rodar no celular

```bash
# 1) celular com depuração por wifi (cabo carrega e invalida a medida de bateria), brilho mínimo, modo avião, bateria > 50 %
adb tcpip 5555 && adb connect IP_DO_CELULAR:5555
# 2) tudo (baixa fonte e modelos por hash, gera o clip de 10 min, mede os 4 modelos, espera esfriar entre eles)
spike/whisper/scripts/medir-no-celular.sh
# 3) os JSON ficam em spike/whisper/resultados/ e o script imprime o WER de cada modelo
```

Os quatro modelos: `tiny` e `base`, cada um em ponto flutuante de 16 bits (`ggml-*.bin`) e quantizado `q5_1`.

## Compilar à mão

```bash
spike/whisper/scripts/buscar-whisper.sh spike/whisper/third_party/whisper.cpp
spike/whisper/scripts/buscar-modelos.sh ~/.cache/gravasom-corpus/modelos
./gradlew -Pspike :spike:whisper:assembleRelease -PmodeloWhisper=$HOME/.cache/gravasom-corpus/modelos/ggml-tiny-q5_1.bin
```
