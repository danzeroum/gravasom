# Comparativo de ASR on-device — Gravasom (Onda 3, transcrição offline)

Atualizado: 2026-10-12

Este documento é o **comparativo de tecnologia** que a Fase 1 do plano de transcrição pede. Ele **não escolhe sozinho**: a escolha está no [ADR-013](adr/013-transcricao-whisper-cpp-spike.md) (spike medido) e no [ADR-016](adr/016-transcricao-offline-producao.md) (produção). Aqui estão os números e o que cada opção custaria neste repositório.

Regra do repositório que restringe o espaço de busca: **o app não tem permissão de `INTERNET`** (`README.md`, `docs/privacy.md`). Qualquer ASR que dependa de Play Services para baixar modelo ou de rede para transcrever está fora, salvo se o dono mudar essa decisão.

## Critérios

| Critério | Por que importa aqui |
| --- | --- |
| **Idiomas** | pt-BR é o caso principal (personas, corpus FLEURS pt-BR). en-US/es-ES/fr-FR são desejáveis, não obrigatórios. |
| **Offline de verdade** | Sem `INTERNET`; sem depender de Play Services baixar modelo em runtime. |
| **Tamanho no APK** | O teto de release é **25 MB** (`docs/status.md`). O APK de release atual mede ~7,9 MB. |
| **CPU / RAM** | A máquina de referência de teste tem 4 GB; aparelho alvo é mid-range. Nada pode estourar 200 MB de pico. |
| **Latência / RTF** | O worker precisa terminar 10 min de áudio em tempo razoável, sem travar o app. |
| **Integração** | Tem de caber em Kotlin/Hilt/Coroutines + `feature/*` sem inventar arquitetura nova. |
| **Licença** | Sem copyleft forte que contamine o app. |
| **Qualidade pt-BR** | WER é o número que decide se a feature é útil. |

## Opções avaliadas

### 1. whisper.cpp (base q5_1) — já medido no spike, ADR-013

**Como funciona:** biblioteca C++ (MIT) compilada via NDK + modelo `.bin` em assets. O app já tem esse pipeline montado em `spike/whisper` (JNI `nativeInit/nativeTranscribe`, `WhisperBridge`, `Transcricao.kt`).

Números medidos no spike (host x86, corpus FLEURS pt-BR de 60 locuções + gravação longa de 10 min; ver ADR-013 para metodologia):

| Modelo | Arquivo | WER locução | WER 10 min | RTF host | Pico RAM | APK extra |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| tiny f16 | 74,1 MiB | 21,2% | 24,8% | 0,107 | 320 MiB | 83,7 MB |
| tiny q5_1 | 30,7 MiB | 23,7% | 26,3% | 0,089 | 274 MiB | 38,1 MB |
| base f16 | 141,1 MiB | 10,5% | 12,0% | 0,170 | 428 MiB | 153,9 MB |
| **base q5_1** | **56,9 MiB** | **9,9%** | **12,4%** | **0,150** | **341 MiB** | **65,7 MB** |

- **Prós:** offline de verdade; cobre pt-BR com WER ≈ 10% (base); roda hoje no spike; licença MIT; sem Play Services; sem `INTERNET`.
- **Contras:** **56,9 MiB** do modelo + ~5,9 MB de libs (2 ABIs) **não cabem** no APK de 25 MB junto do app (~8 MB). Exige uma decisão sobre como distribuir o modelo (ver ADR-016). Pico de RAM (341 MiB) está **acima** do orçamento de 200 MB do prompt — precisa de chunking e/ou medir no aparelho. CPU do host é x86, aparelho é ARM: **o RTF no celular ainda não foi medido** (`spike/whisper/scripts/medir-no-celular.sh`).
- **Bateria / temperatura:** pendente de medição no aparelho (ADR-013, tabela em branco de propósito).

### 2. ML Kit Speech-to-Text on-device (`com.google.mlkit:speech-to-text`)

**Como funciona:** biblioteca do Google que reconhece fala no aparelho via Play Services. Alguns idiomas são suportados on-device; os outros caem na nuvem.

- **Prós:** API Kotlin simples; o modelo não vai no APK (fica sob o Play Services); tamanho de dependência pequeno; latência baixa por ser streaming.
- **Contras bloqueantes para este projeto:**
  - **pt-BR não está na lista de idiomas on-device do ML Kit Speech-to-Text.** O conjunto on-device, por documentação pública do Google, cobre um número restrito de idiomas (majoritariamente inglês e alguns outros), e pt-BR depende do **serviço em nuvem** — o que **contradiz** a regra do repositório de não ter `INTERNET` e a promessa de "tudo local" (`README.md`, `docs/privacy.md`).
  - Depende de **Play Services** com o módulo de reconhecimento baixado; em aparelho sem Play Services ou sem o download prévio, a transcrição simplesmente não funciona. O usuário não controla isso.
  - O modelo é gerenciado pelo Google (fora do controle do app): não dá para fixar versão, nem embutir, nem auditar exatamente o que roda.
- **Veredito:** **inviável** para o requisito central (pt-BR offline).

### 3. `android.speech.SpeechRecognizer` (framework)

**Como funciona:** API do próprio Android, mas a implementação é do **fabricante/OEM** ou dos Google app services. Em muitos aparelhos exige rede; quando há modo offline, o pacote de idioma é baixado pelo usuário em Configurações do sistema.

- **Prós:** zero dependência externa; API estável.
- **Contras:** comportamento **varia por aparelho** (não é testável de forma reprodutível); controle de idiomas é do sistema, não do app; pode exigir `RECORD_AUDIO` em primeiro plano; em vários OEMs não funciona com a tela bloqueada nem em serviço; não dá timestamps por palavra de forma confiável.
- **Veredito:** **inviável** como base da feature. Aceitável apenas como atalho opcional em aparelhos que suportem, e isso não é o que o plano pede.

### 4. Vosk (Kaldi, Apache 2.0) — alternativa considerada

**Como funciona:** biblioteca C++ (Kaldi) + modelos por idioma em `.zip`, tamanhos variados (pt-BR em torno de 50 MB nos modelos "small").

- **Prós:** offline de verdade; licença Apache 2.0; modelos pt-BR existem; streaming com resultados parciais.
- **Contras:** WER em pt-BR nos modelos small tende a ser **pior** que whisper base (o Vosk é competitivo em modelos maiores, que pesam mais que os 56,9 MiB do q5_1); integração via NDK parecida com a do whisper (mesmo custo de setup); a comunidade e a documentação para pt-BR são menores que as do whisper.cpp.
- **Veredito:** **segunda opção**. Se o whisper.cpp falhar em bateria/temperatura no aparelho, o Vosk entra como plano B. Nada no repositório precisa mudar para testá-lo, porque o ponto de integração é a interface `Transcriber` (ver ADR-016).

## Tabela-resumo

| Critério | whisper.cpp base q5_1 | ML Kit on-device | `SpeechRecognizer` | Vosk small pt-BR |
| --- | --- | --- | --- | --- |
| Offline real (sem `INTERNET`) | **Sim** | Não (pt-BR via nuvem) | Depende do OEM | **Sim** |
| pt-BR suportado | **Sim (WER ~10%)** | Não (on-device) | Depende do OEM | Sim (WER pior que whisper base) |
| Independente de Play Services | **Sim** | Não | Não | **Sim** |
| Modelo embutível no app | Sim (56,9 MiB) | Não | Não | Sim (~50 MB) |
| Cabe no APK de 25 MB hoje | **Não sozinho** | Sim (sem modelo) | Sim | **Não sozinho** |
| Pico de RAM | 341 MiB (host) | baixo | baixo | ~300 MB (host, estimativa) |
| Licença | MIT | proprietária Google | framework | Apache 2.0 |
| Já integrado no repo | **Sim (spike)** | Não | Não | Não |

## Recomendação

**Seguir o whisper.cpp (base q5_1), confirmando o ADR-013.** É a única opção que cumpre, ao mesmo tempo, os requisitos não negociáveis do repositório: **pt-BR offline de verdade**, **sem Play Services**, **sem `INTERNET`** e **WER útil** (~10%). ML Kit e `SpeechRecognizer` ficam descartados por não cobrirem pt-BR on-device de forma confiável. Vosk é o plano B registrado.

O que **ainda não está resolvido** e precisa de decisão (não é um "sim" do comparativo, é uma pendência aberta):

1. **Como o modelo de 56,9 MiB chega ao aparelho** sem furar o teto de 25 MB do APK de release. As opções estão no ADR-016.
2. **Bateria, temperatura e RTF no aparelho**, que o ADR-013 deixou em branco de propósito e o `medir-no-celular.sh` mede.
3. **Pico de RAM de 341 MiB no host** está acima do orçamento de 200 MB do prompt; precisa de chunking e/ou medição no aparelho antes de prometer o número.

## Fontes

- ADR-013 (medições do spike, este repositório).
- ADR-016 (decisão de produção, este repositório).
- Documentação pública do Google sobre idiomas on-device do ML Kit Speech-to-Text.
- Repositório oficial do Vosk e catálogo de modelos.
- `spike/whisper/scripts/buscar-whisper.sh` e `buscar-modelos.sh` (hashes das fontes usadas no spike).
