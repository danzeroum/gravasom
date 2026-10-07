#!/usr/bin/env bash
# Roda os testes instrumentados no emulador em DUAS passadas e guarda comando + exit code em log:
#  1) tudo menos PermissaoNegadaTest (esses testes CONCEDEM as permissoes);
#  2) so PermissaoNegadaTest, em instalacao limpa (o AGP reinstala o app entre as passadas), para a negacao real pelo dialogo.
set -u
API="${1:?uso: ci-instrumentados.sh <api>}"
mkdir -p evidencias-ci
adb logcat -c
adb logcat -v time > "evidencias-ci/logcat-api${API}.txt" &
LOGCAT_PID=$!

passada() {
  local nome="$1"; shift
  local log="evidencias-ci/connected-${nome}-api${API}.log"
  echo "\$ ./gradlew :app:connectedDebugAndroidTest --no-daemon --stacktrace $*" > "$log"
  ./gradlew :app:connectedDebugAndroidTest --no-daemon --stacktrace "$@" >> "$log" 2>&1
  local code=$?
  echo "exit=${code}" >> "$log"
  mkdir -p "evidencias-ci/${nome}"
  cp -r app/build/outputs/androidTest-results "evidencias-ci/${nome}/" 2>/dev/null || true
  cp -r app/build/outputs/connected_android_test_additional_output "evidencias-ci/${nome}/" 2>/dev/null || true
  return $code
}

passada principal -Pandroid.testInstrumentationRunnerArguments.notClass=com.gravasom.PermissaoNegadaTest
A=$?
passada permissao-negada -Pandroid.testInstrumentationRunnerArguments.class=com.gravasom.PermissaoNegadaTest
B=$?

kill "$LOGCAT_PID" 2>/dev/null || true
echo "principal=${A} permissao-negada=${B}" | tee "evidencias-ci/codigos-api${API}.txt"
[ "$A" -eq 0 ] && [ "$B" -eq 0 ]
