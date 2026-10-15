#!/usr/bin/env bash
# Roda TODOS os testes automatizados na sua maquina (sem CI, sem emulador, sem aparelho).
#   scripts/testar-local.sh           -> nativo (ASan/UBSan) + testes JVM (JUnit/Robolectric) dos modulos tocados
#   scripts/testar-local.sh nativo    -> so os testes C++ no host
#   scripts/testar-local.sh jvm       -> so o Gradle (testDebugUnitTest)
# Saida: 0 = tudo verde; !=0 = algum teste falhou (o resumo no fim diz quais).
# Requisitos: g++/gcc, JDK 17+ e acesso ao Maven (a 1a execucao do Gradle baixa dependencias).
set -uo pipefail
cd "$(dirname "$0")/.."
MODO="${1:-tudo}"
FALHAS=()

passo() {
  local nome="$1"; shift
  echo; echo "=== $nome ==="
  if "$@"; then echo "--> OK: $nome"; else echo "--> FALHOU: $nome"; FALHAS+=("$nome"); fi
}

if [[ "$MODO" == "tudo" || "$MODO" == "nativo" ]]; then
  passo "MP3/LAME no host (ASan/UBSan/LSan)" bash scripts/test-native-host.sh
  passo "Cadeia DSP no host (ASan/UBSan)"    bash scripts/test-dsp-host.sh
fi

if [[ "$MODO" == "tudo" || "$MODO" == "jvm" ]]; then
  passo "JVM: audio-engine, data, domain, service" \
    ./gradlew --console=plain \
      :core:audio-engine:test :core:data:testDebugUnitTest :core:domain:test :core:service:testDebugUnitTest
  passo "JVM: features (record, library, player, enhance, settings)" \
    ./gradlew --console=plain \
      :feature:record:testDebugUnitTest :feature:library:testDebugUnitTest :feature:player:testDebugUnitTest \
      :feature:enhance:testDebugUnitTest :feature:settings:testDebugUnitTest
fi

echo; echo "================ RESUMO ================"
if ((${#FALHAS[@]} == 0)); then echo "Tudo verde."; exit 0; fi
printf 'FALHOU: %s\n' "${FALHAS[@]}"
exit 1
