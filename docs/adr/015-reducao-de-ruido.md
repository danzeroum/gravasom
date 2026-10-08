# ADR 015 — Redutor de ruído pós-gravação: RNNoise (variante completa), 40% por padrão

Status: aceito (números medidos em host; tempo no celular e escuta A/B: validação do dono)

## Contexto

Onda 2 "Som melhor e proteção" pede um redutor de ruído pós-gravação, como efeito **não destrutivo** (ADR-008), com intensidade de 0 a
100% e prévia A/B. Compararam-se o RNNoise (xiph, v0.2) e o DeepFilterNet3 (Rikorose, pesos de `fal/DeepFilterNet3`) em
fala pt-BR com ruído, pelo critério do harness: **SI-SDRi** melhora e o **STOI da fala limpa** não cai abaixo de 0,9909
(`perfis/tetos_ref.json`: o pior "bom" do corpus fica em 0,9996 e o melhor controle degradado em 0,9823).

## O que foi medido (host x86-64; scripts em `scripts/onda2_ruido_*.py`, saídas em `evidencias/onda-2/`)

Corpus: 8 locuções FLEURS pt-BR (CC BY 4.0, por hash) a 48 kHz + ventilador, zumbido de 60 Hz, ruído rosa e murmúrio de 4 vozes, em SNR
0/5/10/15 dB (16 condições x 8 = 128 misturas) + 8 falas limpas. Medidor independente do app (SI-SDR e STOI contra a fala limpa, com
alinhamento por correlação que registra o atraso).

| Motor (100% de intensidade) | SI-SDRi médio | ventilador | zumbido | rosa | murmúrio | STOI da fala **limpa** (média / pior) | tempo por quadro de 10 ms (host, 1 thread) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| RNNoise v0.2 completo | +3,13 dB | +4,08 | +4,38 | +4,88 | −0,82 | 0,9546 / 0,8490 | 310 µs (média) |
| RNNoise v0.2 leve ("little") | +2,96 dB | +4,01 | +4,03 | +4,84 | −1,06 | 0,9512 / 0,8486 | 202 µs |
| DeepFilterNet3 (PyTorch CPU) | +3,94 dB | +6,08 | +7,58 | +4,84 | −2,72 | 0,9672 / 0,8916 | 523 µs (offline em lote: indicativo) |

Tamanho no APK:

| Motor | Biblioteca/modelo | Observação |
| --- | --- | --- |
| RNNoise completo | **5,34 MB** por ABI (arm64-v8a e x86_64, NDK r27, `-O3`, `-Wl,--gc-sections -Wl,-s`) | pesos float e int8 dentro do `.so` |
| RNNoise leve | 2,74 MB por ABI | |
| DeepFilterNet3 | modelo: 8,0 MB (ONNX, `DeepFilterNet3_onnx.tar.gz`, 7 983 136 bytes) ou 8,7 MB (float32); **runtime não medido** | só roda com o `libDF` em Rust + tract; não consegui compilá-lo para Android aqui (o crate publicado em crates.io é outra versão, sem inferência; o fonte oficial está no GitHub, bloqueado nesta máquina) |

## Decisão

1. **RNNoise v0.2, variante completa.** C puro, entra no NDK que já usamos, quadro de 10 ms que casa com a cadeia de 48 kHz do ADR-008.
   O DeepFilterNet3 é melhor em ruído estacionário (+2 a +3 dB sobre o RNNoise em ventilador e zumbido) e pior em murmúrio, mas exige um runtime
   em Rust que não consegui construir nem medir, e só o modelo já custaria mais que a biblioteca inteira do RNNoise. A variante leve perde
   ~0,2 dB de SI-SDRi para economizar 2,6 MB por ABI e 35% do tempo; como o redutor é pós-gravação (offline), ficou a completa.
   **Reavaliar o DeepFilterNet3** se o dono sentir falta de qualidade em ruído estacionário no uso real.
2. **Intensidade padrão de 40%.** A 100% o RNNoise estraga a fala limpa (STOI 0,955; pior caso 0,849, muito abaixo do teto de 0,9909). A mistura
   seco/molhado por amostra resolve: a curva medida sobre o RNNoise completo (`evidencias/onda-2/ruido-intensidade-completo.txt`):

   | Intensidade | STOI da fala limpa (média / pior) | SI-SDRi em ruído estacionário | SI-SDRi em murmúrio |
   | --- | --- | --- | --- |
   | 20% | 0,9994 / 0,9985 | +1,34 dB | +0,51 dB |
   | **40%** | **0,9976 / 0,9936** | **+2,67 dB** | **+0,75 dB** |
   | 50% | 0,9962 / 0,9896 | +3,29 dB | +0,75 dB |
   | 70% | 0,9921 / 0,9785 | +4,29 dB | +0,44 dB |
   | 100% | 0,9547 / 0,8490 | +4,39 dB | −0,83 dB |

   40% é a maior intensidade "redonda" em que **toda** locução limpa fica acima de 0,9909. O usuário pode subir até 100% sabendo do custo.
3. **Atraso de 2 quadros (960 amostras a 48 kHz), compensado.** Medido por correlação no spike. O passo declara `LatencySamples()` e o
   exportador compensa (duração idêntica em amostras). Dentro do passo, reamostrador racional de fase zero (sinc, janela de Kaiser) leva
   qualquer taxa de 8 a 96 kHz para 48 kHz e de volta; o seco é atrasado o mesmo tanto, então a mistura não faz pente.
4. **0% = cópia idêntica bit a bit.** Passo com intensidade 0 é "sem efeito" (`EditStep.isNoOp`): o exportador copia o original byte a byte.
5. **RNNoise não vai para o git.** `corpus/rnnoise.sha256` fixa por hash cada arquivo (tag v0.2) e o tarball do modelo (~29 MB de C);
   `scripts/buscar-rnnoise.sh` baixa e confere; o Gradle roda a tarefa `fetchRnnoise` antes do build nativo. O v0.2 inclui `os_support.h`
   (herdado do Opus) nos caminhos NEON e escalar sem distribuí-lo: `core/dsp/src/main/cpp/rnnoise_shim/os_support.h` cobre isso.

## Verificação com o código do app (harness `scripts/onda2-ruido.sh`)

O **mesmo C++ do celular**, via JNI no host (Kotlin + `ProjectExporter`), sobre o mesmo corpus a 48, 44,1 e 16 kHz (passando pelo reamostrador):

| Taxa | SI-SDRi médio a 40% (pior condição) | STOI da fala limpa (média / pior) | atraso mediano |
| --- | --- | --- | --- |
| 48 kHz | +2,42 dB (+0,94) | 0,9983 / 0,9977 | 0 amostras |
| 44,1 kHz | +2,41 dB (+0,93) | 0,9983 / 0,9976 | 0 amostras |
| 16 kHz | +2,37 dB (+0,92) | 0,9983 / 0,9976 | 0 amostras |

As 16 condições melhoram em todas as taxas (inclusive o murmúrio, +0,9 a +1,3 dB a 40%). A 100% os mesmos arquivos dão +3,82 dB de média, mas
STOI de fala limpa de 0,9514 (pior 0,8476): **não é o padrão**.

## Limites conhecidos

- **Murmúrio de vozes**: nenhum dos motores é feito para isso (a 100% piora −0,8 a −2,7 dB). A 40% melhora pouco (+0,75 dB). Não prometer redução de conversa de fundo.
- **Tempo no celular não medido** (só host): o RNNoise completo gasta ~3% do orçamento de 10 ms no host; em arm64 pode ser algumas vezes isso, ainda dentro do tempo real.
  Processar 1 h leva da ordem de minutos; roda como tarefa com progresso e cancelamento. Bateria/temperatura: validar no aparelho.
- **Sons de bichos e de água** são tratados como ruído: os presets Natureza e Água vêm com o redutor **desligado** (ADR de presets em `docs/status.md`).
- STOI e SI-SDRi são proxies; só a escuta A/B do dono diz se soa melhor.
