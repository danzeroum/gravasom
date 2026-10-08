#!/usr/bin/env bash
# Instala o NDK exigido pelo projeto com novas tentativas. O Gradle baixa o NDK sob demanda e, quando o download falha no
# runner ("Failed to install the following SDK components: ndk;..."), o job morre sem rodar nada. Versao: fonte unica no plugin.
set -u
NDK="$(grep -oP 'ndkVersion = "\K[^"]+' build-logic/src/main/kotlin/com/gravasom/buildlogic/AndroidLibraryConventionPlugin.kt)"
[ -n "$NDK" ] || { echo "::error::ndkVersion nao encontrado no plugin de convencao"; exit 1; }
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
SDKM="$SDK/cmdline-tools/latest/bin/sdkmanager"
if [ -d "$SDK/ndk/$NDK" ]; then echo "NDK $NDK ja instalado"; exit 0; fi
for i in 1 2 3 4; do
  if echo y | "$SDKM" --install "ndk;$NDK" > /dev/null 2>&1 && [ -d "$SDK/ndk/$NDK" ]; then
    echo "NDK $NDK instalado (tentativa $i)"; exit 0
  fi
  echo "NDK $NDK: tentativa $i falhou; nova tentativa em $((i * 15)) s"
  sleep $((i * 15))
done
echo "::error title=NDK::nao foi possivel instalar o NDK $NDK depois de 4 tentativas"
exit 1
