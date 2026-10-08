# ADR 007 — MP3: LAME 3.100 vendorizado via NDK, biblioteca compartilhada separada

Status: aceito (substitui a parte "implementação diferida" do ADR 002)

## Contexto

O MP3 era um stub: `nativeInit` devolvia 0 e o gravador gravava WAV com extensão `.mp3`.
Duas saídas: (a) vendorizar o LAME via NDK, (b) retirar o MP3 da UI. Decisão pela evidência.

## Evidências medidas (evidencias/fase-12/)

| Critério | Medida | Fonte |
| --- | --- | --- |
| Tamanho do APK | release 9.614.304 B contra 8.582.547 B sem LAME: **+1,03 MB** (4 ABIs). Orçamento 25 MB: uso de 38%. | apk-release.log, apk-libs.log |
| Biblioteca por ABI | libmp3lame.so 171–258 KB, libgravasom_mp3.so 7–36 KB | apk-libs.log |
| Tempo de build | `:core:audio-android:assembleDebug assembleRelease` em ~45 s (4 ABIs, build frio de CMake) | build-nativo.log |
| Licença | LAME 3.100: LGPL v2 (`COPYING` = GNU Library GPL v2), tarball sha256 `ddfe36ca…da1e` | lame/VENDOR.sha256 |
| Correção do codificador | 3 s de seno → 117 quadros ≈ 3056 ms (3000 + atraso do codificador + quadro de tag), 48.900 B a 128 kbps; saída determinística e idêntica em blocos de 10 ms | tdd-verde.log |
| Vazamento | 200 ciclos create/encode/close com ASan+LSan sem vazamento; `LiveInstances()==0` | tdd-verde.log |

## Decisão

**Vendorizar o LAME.** O custo (1 MB, 45 s de build) é pequeno; a licença é atendida pela forma de ligação.

## Como a LGPL é atendida

1. `libmp3lame.so` é uma **biblioteca compartilhada separada** (alvo CMake `mp3lame`). O código do Gravasom
   (`libgravasom_mp3.so`) a liga dinamicamente. O usuário pode substituí-la por uma versão modificada.
2. Só `libmp3lame` é usado; sem frontend e sem mpglib (GPL do frontend não entra).
3. O código do LAME fica **inalterado** em `core/audio-android/src/main/cpp/lame/` (apenas `config.h` é nosso, no lugar do gerado pelo configure).
4. Aviso e oferta de fonte: `app/src/main/assets/licencas/LAME-LGPL.txt` e `docs/licencas/LAME.md`
   (texto completo da licença em `lame/COPYING`). **Pendência de produto**: exibir o aviso numa tela
   "Sobre/Licenças" (ainda não existe tela de Ajustes; ver docs/status.md).

## Verificação

- Núcleo C++ sem JNI testado no host (TDD vermelho→verde, ASan/UBSan/LSan): `scripts/test-native-host.sh`.
- Instrumentado (CI, emulador): round-trip de duração ±100 ms, sem vazamento JNI, MP3 decodificado sem clipping
  (`Mp3Test`). O gate do audio-suite (true peak e clipping) sobre o `mp3_decodificado.wav` entra na Fase 13.
- Alinhamento de página de 16 KB (`-Wl,-z,max-page-size=16384`) nas duas bibliotecas.

## Consequências

- MP3 sempre usa `PcmMp3Recorder`; se a biblioteca não carregar, `prepare()` falha com `Mp3NotAvailable`
  (nunca mais WAV com extensão `.mp3`).
- Mais CPU/bateria na gravação MP3 (não medido no dispositivo: PENDENTE).
