#!/usr/bin/env bash
# Gera o APK de release ASSINADO, verifica assinatura e orcamento de tamanho e registra SHA-256 e certificado.
# Entradas (GitHub Secrets na CI, nunca no repositorio):
#   GRAVASOM_KEYSTORE_BASE64, GRAVASOM_KEYSTORE_PASSWORD, GRAVASOM_KEY_ALIAS, GRAVASOM_KEY_PASSWORD
# Falha (exit 1) SEM gerar APK quando algum segredo falta, ou quando o APK passa de 25 MB (25.000.000 bytes).
set -euo pipefail

LIMITE_BYTES="${LIMITE_BYTES:-25000000}"
SAIDA="${SAIDA:-evidencias-ci}"
APK_DIR="app/build/outputs/apk/release"

faltando=()
for v in GRAVASOM_KEYSTORE_BASE64 GRAVASOM_KEYSTORE_PASSWORD GRAVASOM_KEY_ALIAS GRAVASOM_KEY_PASSWORD; do
  [ -n "${!v:-}" ] || faltando+=("$v")
done
if [ "${#faltando[@]}" -gt 0 ]; then
  msg="Segredos ausentes: ${faltando[*]}. O APK de release so e gerado assinado. Cadastre-os em Settings > Secrets and variables > Actions (veja docs/release-assinatura.md). Forks e PRs de forks nao recebem segredos: la este job falha de proposito."
  echo "::error title=Release nao assinado::${msg}"
  echo "$msg" >&2
  exit 1
fi

mkdir -p "$SAIDA"
KS="$(mktemp -d)/release.jks"
trap 'rm -f "$KS"' EXIT
printf '%s' "$GRAVASOM_KEYSTORE_BASE64" | base64 -d > "$KS"
export GRAVASOM_KEYSTORE_FILE="$KS"

rm -rf "$APK_DIR"
./gradlew :app:assembleRelease --no-daemon
APK="$APK_DIR/app-release.apk"
if [ ! -f "$APK" ]; then
  echo "::error title=Release nao assinado::o Gradle nao produziu app-release.apk (so o unsigned?)" >&2
  ls -l "$APK_DIR" >&2 || true
  exit 1
fi

APKSIGNER="$(ls -d "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/root/android-sdk}}"/build-tools/*/apksigner | sort -V | tail -1)"
"$APKSIGNER" verify --verbose --print-certs "$APK" | tee "$SAIDA/apk-release-certificado.txt"

# Segredo trocado/errado geraria um APK assinado por OUTRA chave, que nao instala por cima da versao anterior.
# Com GRAVASOM_CERT_SHA256 definido, confere o certificado do signatario antes de seguir.
if [ -n "${GRAVASOM_CERT_SHA256:-}" ]; then
  obtido="$(grep -m1 -E "certificate SHA-256 digest" "$SAIDA/apk-release-certificado.txt" | sed 's/.*: *//' | tr -d ': ' | tr 'A-F' 'a-f')"
  esperado="$(printf '%s' "$GRAVASOM_CERT_SHA256" | tr -d ': ' | tr 'A-F' 'a-f')"
  if [ "$obtido" != "$esperado" ]; then
    echo "::error title=Certificado inesperado::assinado com ${obtido:-<nenhum>}, esperado ${esperado}" >&2
    exit 1
  fi
  echo "certificado confere com o esperado"
fi

BYTES="$(stat -c %s "$APK")"
{
  echo "arquivo=$APK"
  echo "bytes=$BYTES"
  echo "limite_bytes=$LIMITE_BYTES"
  echo "sha256=$(sha256sum "$APK" | cut -d' ' -f1)"
  grep -E "certificate (DN|SHA-256 digest)" "$SAIDA/apk-release-certificado.txt" | sed 's/^Signer #1 //; s/ /_/g; s/_digest:_/=/; s/_DN:_/=/' || true
} | tee "$SAIDA/apk-release-info.txt"

if [ "$BYTES" -gt "$LIMITE_BYTES" ]; then
  echo "::error title=Orcamento de APK estourado::${BYTES} bytes > ${LIMITE_BYTES} bytes" >&2
  exit 1
fi
echo "OK: assinado, ${BYTES} bytes <= ${LIMITE_BYTES}"
