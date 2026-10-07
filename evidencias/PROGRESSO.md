# Progresso da execução

Atualizado: 2026-10-07

## Retomada

1. Ler este arquivo e `docs/status.md`.
2. `git fetch` e confirmar o último commit verde no remote (o push a cada lote é a prova de durabilidade).
3. Continuar do "próximo passo".

## Estado atual

- Branch: `claude/gracious-clarke-7q1k3x` (origem: 27839e9 em `main`).
- Fases 10, 11 (menos o widget), 12 concluídas e medidas no CI (API 34 e 36).
- **Fase 13 PARADA em `TETO_NAO_DISCRIMINA`** (gate de inteligibilidade). Nenhum teto foi afrouxado.

## Parada nomeada: TETO_NAO_DISCRIMINA

- **O que falhou**: `speech_intelligibility.stoi_proxy_no_ref` (audio-suite) não cai quando se reinjeta ruído rosa a 15 dB SNR em 3 das 7 bases
  (real_fala_1 queda +0,016; real_fala_2 −0,018; real_fala_4 −0,045; nas outras bases a queda é 0,12–0,18).
  Além disso `ref_quality` com referência leva 48–120 s por par e o modo `source-sep-ref` retorna `error`.
- **Evidência**: `evidencias/fase-13/CALIBRACAO.md`, `perfis/tetos.json`.
- **Último commit verde**: ver `git log` (o CI de lint/unidade/build e 12 de 13 testes de emulador estão verdes).
- **Próximo passo recomendado**: medir SNR/SI-SDR com referência limpa por um script numpy próprio (venv separado do audio-suite),
  mantendo `speech_intelligibility` do audio-suite só como observação. Precisa de decisão (ver mensagem final).

## Também calibrado (não são paradas)

- F0: meta inicial 5 cents **não é mensurável** em fala real (o passa-altas de 80 Hz sozinho move a mediana até 21 cents); teto calibrado 29,7 cents; controle +50 cents dá 42–77.
- Glitch: `click_sensitivity` 6,0 dá 400–2000 "cliques" na fala limpa; calibrado em 30,0 (limpo 0–14; 40 cliques injetados dão 39–41); teto 8,8 eventos novos.

## Pendências humanas

Teste com 5 usuários, Accessibility Scanner, publicação Play, MobSF com credencial, Firebase Test Lab, medidas em dispositivo real (bateria, latência de partida).
