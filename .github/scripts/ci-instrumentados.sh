#!/usr/bin/env bash
# Roda os testes instrumentados dentro do emulador e guarda comando + exit code em log.
set -u
API="${1:?uso: ci-instrumentados.sh <api>}"
mkdir -p evidencias-ci
adb logcat -c
adb logcat -v time > "evidencias-ci/logcat-api${API}.txt" &
LOGCAT_PID=$!

LOG="evidencias-ci/connected-api${API}.log"
echo "\$ ./gradlew :app:connectedDebugAndroidTest --no-daemon --stacktrace" > "$LOG"
./gradlew :app:connectedDebugAndroidTest --no-daemon --stacktrace >> "$LOG" 2>&1
CODE=$?
echo "exit=${CODE}" >> "$LOG"

kill "$LOGCAT_PID" 2>/dev/null || true
exit "$CODE"
