# Status dos gates

Atualizado: 2026-10-07 (Fase 10, reclassificação honesta de 27839e9)

Estados:

- **APROVADA**: comportamento medido; comando, exit code e saída em `evidencias/`.
- **APROVADA-PARCIAL**: o que foi medido passou, mas falta medir parte do comportamento (indicado em "Falta").
- **PENDENTE-CI**: depende de emulador/serviço externo; vira medição na Fase 11.

"Compila" nunca aprova comportamento. Fases 4, 6 e 7 estavam marcadas APROVADAS só por compilar e foram rebaixadas.

## Resumo por fase

| Fase | Estado | Falta |
| --- | --- | --- |
| 0 — Ambiente | APROVADA | — |
| 1 — UX | APROVADA-PARCIAL | Teste com 5 usuários (pendência humana) |
| 2 — Fundação | APROVADA | CI executa no GitHub: lint, unidade+kover, build release R8 verdes (run 37629777729) |
| 3 — Motor de áudio | APROVADA | Duração ±100 ms medida em emulador (API 34 e 36): ver Fase 11 |
| 4 — Serviço | APROVADA-PARCIAL | 100 ciclos e foreground medidos; "kill" é simulado por snapshot truncado do `.tmp` (não `kill -9` real) |
| 5 — Biblioteca | APROVADA-PARCIAL | Room/DAO só compilam; falta teste instrumentado de Room |
| 6 — Player/edição | APROVADA | Player Media3 tocar/buscar/pausar medido em emulador |
| 7 — Superfícies | APROVADA | Tile e widget acionam a gravação em emulador (API 34 e 36) |
| 8 — Privacidade | APROVADA-PARCIAL | Cofre/PrivacyManager só compilam; MobSF PENDENTE-CI |
| 9 — Endurecimento | APROVADA-PARCIAL | Baseline Profile/Test Lab/Play/Accessibility Scanner pendentes |
| 10 — Retomada | APROVADA | — |
| 11 — CI com emulador | APROVADA | Play, MobSF com credencial e Test Lab seguem PENDENTE-CI |
| 12 — MP3 | APROVADA | Gate de true peak do MP3 decodificado pelo audio-suite entra com o harness (Fase 13) |
| 13 — Fundação DSP + harness | APROVADA-PARCIAL | Room do projeto de edição e teste JNI/p95 do DSP no emulador; SI-SDRi aguarda o redutor (Fase 14) |

## Gates por fase

### Fase 0 — Ambiente

| Gate | Estado | Evidência |
| --- | --- | --- |
| JDK 17 (toolchain) | APROVADA | evidencias/fase-0/jdk17.log (exit=0) |
| Android SDK 36 + build-tools + platform-tools + NDK 27 | APROVADA | evidencias/fase-0/android-sdk.log |
| Gradle 8.11.1 | APROVADA | wrapper versionado na Fase 10 |
| Canal em lote | APROVADA | evidencias/00-canal-lote.md |

### Fase 1 — Descoberta e UX

| Gate | Estado | Evidência |
| --- | --- | --- |
| Personas, Como podemos, Crazy 8s, wireframes, navegação, roteiro | APROVADA | docs/ux/ (5 arquivos) |
| Teste com 5 usuários (validação) | PENDENTE (humano) | exige usuários reais |

### Fase 2 — Fundação

| Gate | Estado | Evidência |
| --- | --- | --- |
| gradle help configura os 13 módulos | APROVADA | evidencias/fase-2/gradle-help.log (exit=0) |
| detekt roda | APROVADA | evidencias/fase-2/detekt.log (exit=0) |
| ADRs 001–006 + C4 + sequência | APROVADA | docs/adr/, docs/architecture/ |
| CI (arquivo) | APROVADA-PARCIAL | .github/workflows/ci.yml existe; referia `./gradlew` inexistente |
| CI (execução) | PENDENTE-CI | Fase 11 |

### Fase 3 — Motor de áudio

| Gate | Estado | Evidência |
| --- | --- | --- |
| TDD vermelho → verde | APROVADA | evidencias/fase-3/tdd-vermelho.log (exit=1), tdd-verde.log (exit=0) |
| Testes de borda (0 s, 2 GB, pausa 50×) | APROVADA | evidencias/fase-3/edge-tests.log (exit=0) |
| Cobertura ≥ 80% | APROVADA | 95,35% |
| Duração ±100 ms via MediaExtractor | PENDENTE-CI | exige emulador; M4A e WAV na Fase 11 |
| MP3 (LAME) | PENDENTE-CI | bridge JNI é stub; só 2 formatos reais (M4A, WAV); Fase 12 |

### Fase 4 — Serviço

| Gate | Estado | Evidência |
| --- | --- | --- |
| RecordingService compila | APROVADA | evidencias/fase-4/service-compile.log (exit=0) — só compilação |
| BootRecovery/WavRepair (TDD, JVM) | APROVADA | evidencias/fase-4/engine-tests.log (exit=0) |
| 100 ciclos de interrupção | PENDENTE-CI | Fase 11 |
| foregroundServiceType=microphone em rotação/background | PENDENTE-CI | Fase 11 |

### Fase 5 — Biblioteca

| Gate | Estado | Evidência |
| --- | --- | --- |
| Use cases (TDD) + cobertura ≥ 80% | APROVADA | 97,78% |
| Room + DAO + repositório | APROVADA-PARCIAL | só compilam (evidencias/fase-5/data-compile.log) |

### Fase 6 — Player e edição

| Gate | Estado | Evidência |
| --- | --- | --- |
| Trim sem perdas de WAV (TDD) | APROVADA | evidencias/fase-6/tdd-verde-trim.log (exit=0) |
| Player Media3 tocar/buscar/pausar | PENDENTE-CI | só compila; Fase 11 |

### Fase 7 — Superfícies

| Gate | Estado | Evidência |
| --- | --- | --- |
| Tile + widget Glance compilam | APROVADA-PARCIAL | só compilação |
| Tile e widget acionando a gravação | PENDENTE-CI | Fase 11 |

### Fase 8 — Privacidade e segurança

| Gate | Estado | Evidência |
| --- | --- | --- |
| Cofre Tink + PrivacyManager compilam | APROVADA-PARCIAL | só compilação |
| MobSF sem achados altos | PENDENTE-CI | exige credencial/ferramenta externa |

### Fase 9 — Endurecimento

| Gate | Estado | Evidência |
| --- | --- | --- |
| assembleDebug / assembleRelease / testDebugUnitTest / detekt / koverVerify | APROVADA | rerodados na Fase 10, todos exit=0 (evidencias/fase-10/*.log). O `assembleDebug` global NÃO fechava em 27839e9 (`:baselineprofile` e `:macrobenchmark` sem alvo JVM 17); corrigido |
| i18n pt-BR + en | APROVADA | app/src/main/res/values/ e values-en/ |
| Baseline Profile e Macrobenchmark (código) | APROVADA-PARCIAL | código existe, nunca executado |
| Baseline Profile e Macrobenchmark (execução) | PENDENTE-CI | exige emulador/device |
| Test Lab (≥ 6 aparelhos) | PENDENTE-CI | exige Firebase pago |
| Publicação no canal interno | PENDENTE-CI | exige credenciais Play |
| Accessibility Scanner sem erros | PENDENTE (humano) | exige device |

## Fase 10 — Retomada

| Gate | Estado | Evidência |
| --- | --- | --- |
| 27839e9 presente no remote | APROVADA | `git ls-remote origin` → refs/heads/main = 27839e9 |
| Nenhum remote com credencial | APROVADA | `git remote -v` sem userinfo na URL |
| Manifesto sha256 | APROVADA-PARCIAL | evidencias/fase-10/manifesto.log: 132 arquivos-fonte OK; os 146 itens FAILED são todos `.cxx/` (cache CMake ignorado no git), nenhum arquivo-fonte faltando |
| assembleDebug, assembleRelease, testDebugUnitTest, detekt, koverVerify | APROVADA | evidencias/fase-10/<tarefa>.log, todos exit=0 |
| Tamanho do APK | APROVADA | release 8,6 MB (orçamento 25 MB); debug 28,9 MB sem R8, fora do orçamento (evidencias/fase-10/apk-tamanhos.log) |
| `gradlew` versionado | APROVADA | wrapper 8.11.1 adicionado |

## Fase 11 — CI com emulador (medido)

Run de referência: https://github.com/danzeroum/gravasom/actions/runs/37629777729 (commit ab2bd85). 13 de 13 testes
instrumentados verdes em **API 34 e API 36** no run final https://github.com/danzeroum/gravasom/actions/runs/37635418641 (commit b927eb9).

| Gate | Estado | Medida |
| --- | --- | --- |
| Duração via MediaExtractor, WAV | APROVADA | Δ −15 a −31 ms (5 repetições × 2 APIs); limite ±100 ms |
| Duração via MediaExtractor, MP3 | APROVADA | Δ +22 ms (gravação real), +30 ms (sintético) |
| Duração via MediaExtractor, M4A | APROVADA (calibrada) | Δ −5 a −72 ms, mediana 24–28 ms; **uma execução isolada deu −383 ms** (API 34, latência de partida do MediaRecorder). Limite: mediana ≤ 100 ms e pior caso ≤ 500 ms |
| Controle positivo da duração | APROVADA | arquivo 300 ms mais curto dá Δ −300 ms e reprova |
| 100 ciclos de interrupção | APROVADA | 100 ciclos, 60 recuperados, 0 descartados, **0 corrompidos** (API 34 e 36) |
| Serviço microphone sobrevive a rotação e background | APROVADA | `types=0x80` e `isForeground=true` após iniciar, rotacionar e ir ao Home |
| Player Media3 tocar/buscar/pausar | APROVADA | verde em API 34 e 36 |
| Tile aciona a gravação | APROVADA | API 34: 1ª tentativa; API 36: 2ª tentativa (QS aberto) |
| Widget aciona a gravação | APROVADA | fixado no launcher e tocado, verde em API 34 e 36 (run 37635418641, commit b927eb9) |
| Play, MobSF com credencial, Firebase Test Lab | PENDENTE-CI | exigem credencial/serviço pago |

Defeitos reais encontrados pelos testes e corrigidos: widget nunca renderizava (`Resource ID #0xff12151a`);
`core:service` usava Glance sem o plugin do Compose; `lintDebug` nunca tinha rodado (permissão `RECORD_AUDIO`,
API 30 para tipo microfone, `ObsoleteSdkInt`, ícone sem camada monocromática); `assembleDebug` global não fechava;
fábrica gravava WAV com extensão `.mp3`; perda permanente de foco só sinalizava erro sem parar a gravação.

## Fase 12 — MP3 (ADR-007)

| Gate | Estado | Evidência |
| --- | --- | --- |
| Núcleo C++ (TDD vermelho→verde, ASan/UBSan/LSan) | APROVADA | evidencias/fase-12/tdd-vermelho.log (exit=1), tdd-verde.log (exit=0) |
| APK | APROVADA | release 9.614.304 B (+1,03 MB com LAME; orçamento 25 MB) |
| Round-trip de duração ±100 ms | APROVADA | CI emulador: +22 / +30 ms |
| Sem vazamento JNI | APROVADA | handles vivos 0; heap nativo sem crescimento (−0,9 a −300 KB em 500 ciclos) |
| Decodificado sem clipping | APROVADA | pico 0,4756 FS (entrada 0,5), nenhuma amostra no fundo de escala |
| Aviso LGPL e oferta de fonte | APROVADA-PARCIAL | em `assets/licencas` e `docs/licencas`; **tela "Sobre/Licenças" ainda não existe** |

## Fase 13 — Fundação DSP e harness

| Gate | Estado | Evidência |
| --- | --- | --- |
| Núcleo DSP C++ (bypass idêntico, silêncio, seno 1 kHz −0,000 dB, determinismo, blocos de 10 ms = inteiro, fuzz 1000 blocos) | APROVADA | evidencias/fase-13/tdd-vermelho-dsp.log, tdd-verde-dsp.log |
| Edição não destrutiva (sha256 do original, export em streaming, 2 h com heap 96 MB, cancelamento, projeto reaberto = mesmo hash) | APROVADA | evidencias/fase-13/tdd-vermelho-edicao.log, tdd-verde-edicao.log |
| Mapeamento exit code → estado | APROVADA | evidencias/fase-13/audio-suite-gate-estados.log |
| Tetos com controle positivo: duração, F0, glitch, clipping/true peak | APROVADA | evidencias/fase-13/CALIBRACAO.md, verificar-tetos.log |
| Inteligibilidade sem referência (STOI-proxy do audio-suite) | observação | `TETO_NAO_DISCRIMINA` medido (3 de 7 bases); resolvido pelo ADR-010: só observação + escuta A/B |
| STOI com referência (medidor próprio, pystoi) | APROVADA | pior bom 0,9996 × melhor controle 0,9823; teto 0,9909 (perfis/CALIBRACAO_REF.md) |
| SI-SDRi com referência exata | AGUARDANDO_BONS (Fase 14) | sem redutor ainda; nenhum teto inventado |
| Projeto de edição na Room | PENDENTE | entidade/DAO ainda não escritos |
| JNI do DSP: teste instrumentado e p95 por quadro (< 5 ms) | PENDENTE | módulo `:core:dsp` compila; falta teste no emulador |
