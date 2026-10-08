# Validação dos protótipos de baixa fidelidade (antes da UI final)

**Honestidade sobre o método**: não houve sessão com usuários reais (pendência humana mantida). O que foi feito é um
**walkthrough cognitivo** dos wireframes (`03-wireframes.md`) e do Crazy 8s (`02-crazy-8s.md`) contra o **roteiro de 5 tarefas**
(`05-roteiro-teste.md`): para cada tarefa, passo a passo, "o usuário sabe o que fazer? vê o resultado?". Ele encontra problemas
de design baratos antes de codificar; **não substitui** o teste com 5 usuários.

## Crazy 8s — Gravações (faltava; só Gravar tinha)

1. Lista simples por data.
2. Lista com busca fixa no topo.
3. Lista agrupada (Hoje / Esta semana / Antes).
4. Cartões grandes com onda.
5. Lista com play embutido na linha.
6. Lista + mini-player fixo embaixo.
7. Chips de filtro (Favoritas / M4A / WAV / MP3).
8. Lista com ações por deslizar (excluir / favoritar).

**Escolha: 2 + 6 + 5** — busca fixa (achar em ≤ 3 toques), play na linha e mini-player embaixo (ouvir em 1 toque sem sair).
Deslizar (8) descartado: não é descobrível nem acessível; ações ficam num menu "⋮" com alvo de 48 dp.

## Walkthrough das 5 tarefas

| # | Tarefa | Resultado no wireframe | Problema achado | Mudança antes da UI |
| --- | --- | --- | --- | --- |
| 1 | Abrir e gravar em ≤ 2 s | Abre na tela Gravar; 1 botão | Permissão aparece no 1º toque e **o fluxo some se negada** (sem saída) | Estado "Permissão negada" com explicação e botão **Abrir configurações** |
| 2 | Pausar, atender chamada, retomar | Só há "( ● )" | **Wireframe não tem pausa/parar** durante a gravação | Estado Gravando: **Pausar** e **Parar** grandes; texto de status ("Pausada pela ligação") |
| 3 | Achar e tocar | Lista com ▶ | Sem busca no wireframe da lista, e o ▶ não diz se está tocando | Busca fixa no topo; mini-player embaixo com título e posição |
| 4 | Compartilhar | Só no Detalhe | **3 toques** (item → detalhe → compartilhar); sem "Salvar em…" | Menu "⋮" na linha: Compartilhar, Salvar em…, Renomear, Excluir (2 toques) |
| 5 | Excluir e desfazer | "Excluir" sem rede de segurança | Sem confirmação nem desfazer | Excluir vai para a lixeira + barra "Excluída · DESFAZER" |

Achados extras: (a) a entrada de áudio não aparece (Carla com fone BT) → linha "Entrada: …" na tela Gravar;
(b) Ajustes sem lugar para "Seus números" e licenças → duas linhas novas; (c) wireframe de Gravar tem chips de formato mas
sem dizer que **não trocam durante a gravação** → chips desabilitados enquanto grava.

## Wireframes revisados (delta)

```
GRAVAR (pronto)                        GRAVAR (gravando)
┌───────────────────────────┐          ┌───────────────────────────┐
│ [M4A] [WAV] [MP3]         │          │ [M4A] [WAV] [MP3] (cinza) │
│ Entrada: Microfone ▾      │          │ Entrada: Microfone        │
│        00:00              │          │        00:12              │
│     ▁▁▁▁▁▁▁ (VU)          │          │     ▂▄▆█▆▄▂ (VU)          │
│        ( ● ) Gravar       │          │  ( ⏸ Pausar )  ( ■ Parar )│
│ Gravações aparecem em     │          │ "Gravando — pode bloquear │
│ Músicas/Recordings        │          │  a tela"                  │
└───────────────────────────┘          └───────────────────────────┘
GRAVAÇÕES
┌───────────────────────────┐
│ 🔍 Buscar por nome…       │
│ ▶ Gravação hoje 14h32   ⋮ │
│   00:12 · M4A · 1,2 MB    │
│ …                         │
│ ┌ mini-player ──────────┐ │
│ │ ⏸ Gravação hoje  0:03 │ │
│ └───────────────────────┘ │
└───────────────────────────┘
```

## Decisão

Seguir para a UI com essas 8 mudanças. Validação com usuários reais continua pendente (tarefas 1–5 do roteiro).
