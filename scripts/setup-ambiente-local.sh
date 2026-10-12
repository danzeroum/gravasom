#!/usr/bin/env bash
# Prepara o ambiente para COMPILAR E TESTAR o Gravasom nesta maquina, sem depender
# de SDK do sistema. Tudo vai para .tooling/ (gitignored): JDK 17, Android SDK, Gradle,
# whisper.cpp e o modelo de transcricao.
#
# Uso:  scripts/setup-ambiente-local.sh
# Depois:
#   export JAVA_HOME="$PWD/.tooling/jdk-17.0.20.1+1"
#   export ANDROID_HOME="$PWD/.tooling/android-sdk"
#   export ANDROID_SDK_ROOT="$ANDROID_HOME"
#   ./gradlew -PwhisperSrc="$PWD/.tooling/whisper.cpp" :app:assembleDebug
#
# O que este script NAO faz: instalar o emulador (precisa de AVD + KVM), instalar
# Android Studio, nem aceitar licencas do SDK alem das que ja existem em .tooling.
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
TOOL="$RAIZ/.tooling"
mkdir -p "$TOOL"
cd "$TOOL"

# --- JDK 17 (o build exige Java 17; o sistema pode ter 21) -------------------
JDK_DIR="$TOOL/jdk-17.0.20.1+1"
if [ ! -x "$JDK_DIR/bin/java" ]; then
  echo "==> Baixando JDK 17 (Temurin)" >&2
  curl -fsSL --retry 3 -o jdk17.tar.gz \
    'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jdk_x64_linux_hotspot_17.0.20.1_1.tar.gz'
  tar xzf jdk17.tar.gz
  rm -f jdk17.tar.gz
fi

# --- Android SDK (command-line tools + plataforma 36 + build-tools + NDK 27) --
SDK_DIR="$TOOL/android-sdk"
if [ ! -x "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "==> Baixando command-line tools" >&2
  mkdir -p "$SDK_DIR/cmdline-tools"
  curl -fsSL --retry 3 -o cmdtools.zip \
    'https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip'
  unzip -q -o cmdtools.zip -d cmdline-tools-tmp
  mkdir -p "$SDK_DIR/cmdline-tools/latest"
  cp -r cmdline-tools-tmp/cmdline-tools/* "$SDK_DIR/cmdline-tools/latest/"
  rm -rf cmdline-tools-tmp cmdtools.zip
fi

SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"
if [ ! -d "$SDK_DIR/platforms/android-36" ]; then
  echo "==> Aceitando licencas e instalando pacotes do SDK" >&2
  yes | "$SDKMANAGER" --sdk_root="$SDK_DIR" --licenses >/dev/null || true
  "$SDKMANAGER" --sdk_root="$SDK_DIR" \
    'platforms;android-36' 'build-tools;36.0.0' 'platform-tools' 'ndk;27.0.12077973' 'cmake;3.22.1'
fi

# --- Gradle 8.11.1 (o wrapper resolve sozinho, mas guardamos aqui por completude) ---
# (O wrapper versionado na raiz ja cuida disto; nada a fazer.)

# --- whisper.cpp + modelo base q5_1 (transcricao offline, ADR-013/016) -------
WHISPER_DIR="$TOOL/whisper.cpp"
if [ ! -f "$WHISPER_DIR/CMakeLists.txt" ]; then
  echo "==> Baixando whisper.cpp 1.8.5 por hash" >&2
  curl -fsSL --retry 3 -o src.tar.gz \
    'https://files.pythonhosted.org/packages/9f/35/610787a277e177e6b7983277a35a2969c0876b46c0d889bc6a0c4b4ac4a7/pywhispercpp-1.5.1.tar.gz'
  echo '5df897e5dd9d9f16804fc937bd85b9f5880a0b29a55f8843585e0e03d76195e5  src.tar.gz' | sha256sum -c -
  tar xzf src.tar.gz
  rm -rf "$WHISPER_DIR"
  mv pywhispercpp-1.5.1/whisper.cpp "$WHISPER_DIR"
  rm -rf pywhispercpp-1.5.1 src.tar.gz
fi

MODELOS="$TOOL/modelos"
mkdir -p "$MODELOS"
MODELO="$MODELOS/ggml-base-q5_1.bin"
if [ ! -f "$MODELO" ] || [ "$(sha256sum "$MODELO" | cut -d' ' -f1)" != "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898" ]; then
  echo "==> Baixando modelo base q5_1 (~57 MB)" >&2
  curl -fsSL --retry 3 -o "$MODELO" \
    'https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin'
  echo '422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898  '"$MODELO" | sha256sum -c -
fi

# --- local.properties (gitignored) --------------------------------------------
cat > "$RAIZ/local.properties" <<EOF
## Gerado por scripts/setup-ambiente-local.sh. Nao versionar.
sdk.dir=$SDK_DIR
EOF

echo '==> Pronto. Para buildar:' >&2
echo "    export JAVA_HOME=$JDK_DIR" >&2
echo "    export ANDROID_HOME=$SDK_DIR" >&2
echo "    export ANDROID_SDK_ROOT=\$ANDROID_HOME" >&2
echo "    ./gradlew -PwhisperSrc=$WHISPER_DIR :app:assembleDebug" >&2
