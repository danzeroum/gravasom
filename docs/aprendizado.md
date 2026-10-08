# Plano de aprendizado (substitui o "plano fechado")

Em vez de prometer um escopo fechado, trabalhamos em **ciclos curtos**: hipótese → experimento → resultado **medido** → decisão
(manter, ajustar ou descartar). Mudança de produto gera ADR. Hipóteses completas: `docs/ux/jornada.md`.

Regras: (1) toda decisão cita um número e de onde ele veio; (2) nenhuma métrica é inventada; sem dado = "não medido";
(3) o teste com usuários reais continua pendente e **não** é substituído por medição automática.

## Backlog de hipóteses

| ID | Hipótese (resumo) | Métrica | Alvo | Estado |
| --- | --- | --- | --- | --- |
| H1 | Gravar com 1 toque e permissão em contexto | tempo do toque ao estado Gravando | ≤ 2 s (mediana) | ciclo 1 |
| H2 | Busca fixa + play na linha | toques da tela inicial até a gravação certa tocando | ≤ 3 toques; play em 1 | ciclo 1 |
| H3 | Compartilhar sem expor caminho | URI `content://` sem caminho bruto | 100% | ciclo 2 |
| H4 | Receber áudio de outro app | tempo até aparecer na lista | ≤ 3 s | ciclo 2 |
| H5 | "Seus números" gera valor sem ceder dados | % que abre em 7 dias; 0 vazamentos | ≥ 60%; 0 | aguarda usuários |
| H6 | Tarefas 1–5 do roteiro com usuários reais | sucesso e tempo | ≥ 90% | aguarda usuários |

## Ciclo 1 — "Começar a gravar rápido e achar sem esforço" (H1 e H2)

- **Hipótese**: ver H1 e H2.
- **Experimento**: teste de UI no emulador (API 34 e 36) que mede (a) o intervalo entre o toque em Gravar e o estado Gravando e
  (b) o número de toques para tocar a gravação mais recente entre 20, buscando pelo nome parcial.
- **Resultado medido** (run 37697797597, commit f02f8b9, emulador API 34; API 36 também verde, artefatos `instrumentados-api34/36`):
  - H1: toque em Gravar → estado Gravando = **666 ms** (alvo ≤ 2000 ms; em runs anteriores 486 e 530 ms).
  - H2: 20 gravações, achar "violao" por nome parcial = **2 toques** (aba Gravações + campo de busca; alvo ≤ 3); tocar a gravação = 1 toque.
  - H4 (ciclo 2, já medido de passagem): áudio recebido de outro app aparece em **2178 ms** (alvo ≤ 3000 ms).
  - Limite da medida: emulador, uma execução por API; não é usuário real nem aparelho real. Variação entre runs: 486–666 ms (H1).
- **Decisão**: **manter** o fluxo de gravar (1 toque + permissão em contexto) e a busca fixa no topo; sem mudança de produto, logo sem ADR.
  Reabrir a hipótese só se a medida em aparelho real passar de 2 s ou se o teste com 5 usuários (H6) mostrar > 3 toques.
