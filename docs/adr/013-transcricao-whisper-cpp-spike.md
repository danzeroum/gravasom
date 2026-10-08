# ADR-013 — Transcrição local com whisper.cpp (prova de conceito, sem tela)

Status: proposta (números de host medidos; bateria e temperatura pendentes do dono)

## Contexto
Onda 1 pede uma prova de conceito de transcrição offline. Decisão do dono: o modelo vai **embutido no APK**
(sem permissão de internet). Escopo: sem tela; módulo `:spike:whisper`, incluído só com `-Pspike`.

## O que foi comparado
whisper.cpp 1.8.5 (sdist pywhispercpp 1.5.1, sha256 em `spike/whisper/scripts/buscar-whisper.sh`);
modelos ggml por hash (`corpus/modelos.sha256`): tiny e base, f16 e quantizado q5_1.
Corpus: 60 locuções FLEURS pt-BR (CC BY 4.0, 10,9 min) e uma gravação longa de 10 min montada com elas.
Medição no **host** (CPU x86 do runner/contêiner), não no celular.

| Modelo | Arquivo | WER por locução | WER gravação 10 min | Tempo 10 min (host) | RTF host | Pico de memória | APK do spike |
|---|---|---|---|---|---|---|---|
| tiny f16 | 74,1 MiB | 21,2% | 24,8% | 63,8 s | 0,107 | 320 MiB | 79,8 MB |
| tiny q5_1 | 30,7 MiB | 23,7% | 26,3% | 53,2 s | 0,089 | 274 MiB | 38,1 MB |
| base f16 | 141,1 MiB | 10,5% | 12,0% | 101,5 s | 0,170 | 428 MiB | 153,9 MB |
| base q5_1 | 56,9 MiB | 9,9% | 12,4% | 89,8 s | 0,150 | 341 MiB | 65,7 MB |

(Tamanho do APK do spike = bytes em `spike-sizes`: tiny 83,65 MB, tiny-q5_1 38,11 MB, base 153,91 MB, base-q5_1 65,67 MB.)
Observação metodológica: a primeira rodada usou `-nt` e distorceu o WER da gravação longa; os números acima são da rodada corrigida.

## Pendente (só o aparelho do dono mede)
Bateria e temperatura para 10 min de transcrição: `spike/whisper/scripts/medir-no-celular.sh`
(harness `MedicaoTranscricaoTest`). **Não há números inventados aqui**; preencher a tabela abaixo após a medição.

| Modelo | Δ bateria (10 min) | Temp. máx. | Tempo no aparelho |
|---|---|---|---|
| tiny q5_1 | PENDENTE | PENDENTE | PENDENTE |
| base q5_1 | PENDENTE | PENDENTE | PENDENTE |

## Recomendação
**base q5_1.** WER pt-BR cai de ~24% para ~10–12% frente ao tiny, e a quantização q5_1 praticamente não
perde qualidade (9,9% vs 10,5% por locução; 12,4% vs 12,0% na longa) enquanto corta 60% do tamanho.
O base f16 não se paga (+84 MiB sem ganho de WER). O tiny q5_1 é a alternativa se a medição de
bateria/temperatura no aparelho reprovar o base.

Custo: APK final ≈ APK do app + 56,9 MiB do modelo + ~5,9 MB de bibliotecas whisper (2 ABIs).
Isso **ultrapassa o teto de 25 MB do APK release da Onda 1**; por isso a transcrição fica fora do APK
da Onda 1 e o modelo embutido entra com um teto próprio (ou como split por ABI) quando a feature virar tela.

## Consequências
- Sem permissão de internet: nada a mudar no manifesto.
- Transcrever 10 min leva ~1,5 min em CPU de host; no celular espera-se várias vezes mais — executar em serviço de primeiro plano.
- Reavaliar se o dono aceitar download opcional do modelo (contraria a decisão atual).
