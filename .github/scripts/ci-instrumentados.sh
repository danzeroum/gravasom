#!/usr/bin/env bash
# Roda os testes instrumentados no emulador e guarda comando + exit code em log.
#  API >= 33 (34 e 36), tres passadas, cada uma em instalacao limpa (o AGP reinstala o app entre elas):
#    1) tudo menos PermissaoNegadaTest (esses testes CONCEDEM as permissoes), no idioma do emulador (ingles);
#    2) so PermissaoNegadaTest, para a negacao real pelo dialogo;
#    3) as classes de UI de novo em pt-BR (-e gravasom.locale pt-BR), com as checagens de acessibilidade ligadas.
#  API < 33 (28): so as classes que valem para o Android 9 (cópia publica por WRITE_EXTERNAL_STORAGE, cofre, espaco, acessibilidade).
set -u
API="${1:?uso: ci-instrumentados.sh <api>}"
UI_CLASSES="com.gravasom.FluxoUsuarioTest,com.gravasom.CofreTest,com.gravasom.EspacoLivreTest,com.gravasom.AcessibilidadeTest,com.gravasom.MetricasLocaisTest,com.gravasom.NotificacaoTest,com.gravasom.EcossistemaTest,com.gravasom.TileGravacaoTest"
LEGADO_CLASSES="com.gravasom.PublicacaoApi28Test,com.gravasom.CofreTest,com.gravasom.EspacoLivreTest,com.gravasom.AcessibilidadeTest"
ARG="-Pandroid.testInstrumentationRunnerArguments"
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

RESULTADO=""
if [ "$API" -ge 33 ]; then
  passada principal "${ARG}.notClass=com.gravasom.PermissaoNegadaTest"; A=$?
  passada permissao-negada "${ARG}.class=com.gravasom.PermissaoNegadaTest"; B=$?
  passada pt-br "${ARG}.class=${UI_CLASSES}" "${ARG}.gravasom.locale=pt-BR"; C=$?
  RESULTADO="principal=${A} permissao-negada=${B} pt-br=${C}"
  [ "$A" -eq 0 ] && [ "$B" -eq 0 ] && [ "$C" -eq 0 ]; OK=$?
else
  passada legado "${ARG}.class=${LEGADO_CLASSES}"; A=$?
  RESULTADO="legado=${A}"
  [ "$A" -eq 0 ]; OK=$?
fi

kill "$LOGCAT_PID" 2>/dev/null || true
echo "${RESULTADO}" | tee "evidencias-ci/codigos-api${API}.txt"

# Resumo legivel + anotacoes de erro (aparecem na pagina do job sem baixar artefatos).
if [ -f scripts/ci-resumo.py ]; then
  python3 -I scripts/ci-resumo.py evidencias-ci > "evidencias-ci/resumo-api${API}.txt" 2>&1 || true
  [ -n "${GITHUB_STEP_SUMMARY:-}" ] && { echo "### Instrumentados API ${API}"; echo '```'; head -c 60000 "evidencias-ci/resumo-api${API}.txt"; echo '```'; } >> "$GITHUB_STEP_SUMMARY"
  grep -A1 -E '^FALHOU' "evidencias-ci/resumo-api${API}.txt" | grep -v '^--$' | cut -c1-900 | while IFS= read -r linha; do
    echo "::error title=Instrumentado API ${API}::${linha}"
  done
fi
exit "$OK"
