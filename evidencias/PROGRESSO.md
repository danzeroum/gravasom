# Progresso da execução

Atualizado: 2026-10-07

## Retomada

1. Ler este arquivo e `docs/status.md`.
2. `git fetch` e confirmar o último commit verde no remote (o push a cada lote é a prova de durabilidade).
3. Continuar do "próximo passo".

## Estado atual

- Branch: `claude/gracious-clarke-7q1k3x` (origem: 27839e9 em `main`).
- Fases 10, 11, 12 e 13 concluídas e medidas no CI (API 34 e 36): 20/20 testes instrumentados, harness, lint, unidade e build verdes (run 37657257161, commit dec64ae).
- A parada `TETO_NAO_DISCRIMINA` da Fase 13 foi resolvida pelo ADR-010. Nenhum teto foi afrouxado.
- **Próximo**: Fase 14 (spike RNNoise × DeepFilterNet, redutor com intensidade 0–100% e A/B), depois 15, 16 e 17.

## Parada nomeada TETO_NAO_DISCRIMINA (RESOLVIDA por decisão do responsável)

- **O que falhou**: `speech_intelligibility.stoi_proxy_no_ref` (audio-suite) não cai com ruído rosa a 15 dB reinjetado em 3 das 7 bases
  (real_fala_1 +0,016; real_fala_2 −0,018; real_fala_4 −0,045; nas outras a queda é 0,12–0,18). Nenhum teto foi afrouxado.
- **Decisão**: ADR-010. Medidor próprio com referência (SI-SDR/SI-SDRi/STOI via pystoi) em venv separado; audio-suite fica com clipping,
  true peak, glitch, loudness, F0 e duração; STOI-proxy e descritores só observação nas bases reais + kit de escuta A/B.
- **Registro corrigido duas vezes**: (1) a lentidão do `ref_quality` do audio-suite é real e quadrática (12,5 s a 48 kHz: 46–48 s sem patch,
  1,1 s com patch, valores idênticos); (2) o `status=error` visto numa rodada concorrente **não** se reproduz isolado. Detalhes em `docs/audio-suite/ISSUE.md`.
  A segunda medida (full-ref do audio-suite) fica opt-in até o patch ser aplicado e o pin atualizado.
- **Próximo passo**: Fase 14 (redutor de ruído), calibrando o SI-SDRi com a saída dele (gate hoje AGUARDANDO_BONS).

## Também calibrado (não são paradas)

- F0: meta inicial 5 cents **não é mensurável** em fala real (o passa-altas de 80 Hz sozinho move a mediana até 21 cents); teto calibrado 29,7 cents; controle +50 cents dá 42–77.
- Glitch: `click_sensitivity` 6,0 dá 400–2000 "cliques" na fala limpa; calibrado em 30,0 (limpo 0–14; 40 cliques injetados dão 39–41); teto 8,8 eventos novos.

## Pendências humanas

Teste com 5 usuários, Accessibility Scanner, publicação Play, MobSF com credencial, Firebase Test Lab, medidas em dispositivo real (bateria, latência de partida).
