# Roteiro de teste com 5 usuários

5 tarefas. Registrar sucesso, tempo e erros por tarefa.

| # | Tarefa | Sucesso esperado | Métrica |
| --- | --- | --- | --- |
| 1 | Abrir o app e começar a gravar | grava em ≤ 2 s | tempo |
| 2 | Pausar, atender uma chamada simulada e retomar | gravação preservada | sucesso |
| 3 | Encontrar a gravação na lista e reproduzi-la | 1 toque até tocar | toques |
| 4 | Compartilhar a gravação com outro app | arquivo abre no destino | sucesso |
| 5 | Excluir uma gravação e desfazer | exclusão e restauração | sucesso |

## Meta

Taxa de sucesso ≥ 90% nas 5 tarefas. Tempo da tarefa 1 ≤ 2 s.

## Heurísticas de Nielsen a observar

- Visibilidade do status (cronômetro, VU, notificação).
- Prevenção de erros (aviso de espaço, confirmação de exclusão com desfazer).
- Recuperação de erros (mensagem em linguagem simples com ação).
- Controle e liberdade (pausar/retomar; parar nunca descarta).
- Consistência e acessibilidade (Material 3, alvos ≥ 48 dp, TalkBack).
