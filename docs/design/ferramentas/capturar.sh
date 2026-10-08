#!/usr/bin/env bash
# Gera as capturas da tela Gravar (pronto, gravando, pausado; claro e escuro; Gravasom e Mixlirous) em docs/design/capturas/.
# Roda no Robolectric (sem emulador): renderiza os mesmos composables do app em 360x780 dp, em pt-BR.
set -euo pipefail
raiz="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$raiz"
GRAVASOM_CAPTURAS="$raiz/docs/design/capturas" ./gradlew :feature:record:testDebugUnitTest --tests '*CapturasTest*' --rerun-tasks
ls -1 docs/design/capturas
