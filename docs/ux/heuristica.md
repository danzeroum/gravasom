# Análise heurística de Nielsen — telas novas

Telas avaliadas: **Gravar**, **Gravações** (com mini-player), **Ajustes**, **Seus números**, **notificação de gravação**, **receber áudio**.
Método: inspeção do código e dos testes de UI por um avaliador (não é teste com usuários; ver pendências). Severidade de Nielsen:
0 = não é problema · 1 = cosmético · 2 = menor · 3 = grave (prioridade alta) · 4 = catastrófico (corrigir antes de liberar).
Cada tela cita a etapa da jornada e a hipótese de `docs/ux/jornada.md`.

## Problemas, priorizados

| # | Sev. | Heurística | Tela / etapa | Problema | Estado |
| --- | --- | --- | --- | --- | --- |
| 1 | **4** | 1 Visibilidade do status · 3 Controle | Notificação / Gravar (Bruno, tela bloqueada) | Os botões Pausar/Parar/Marcador eram `PendingIntent` **vazios**; o tempo ficava fixo em `00:00`; "Marcador" não existia. | **Corrigido**: ações reais, sem botão morto, cronômetro nativo; `NotificacaoTest` |
| 2 | **3** | 3 Controle · 7 Eficiência | Gravações / Ouvir (Ana, aula de 90 min) | Mini-player sem busca nem velocidade, embora a jornada prometa 0,5–2×. | **Corrigido**: barra de posição + velocidade 1/1,5/2/0,5× |
| 3 | **3** | 2 Mundo real (honestidade) | Ajustes / Descobrir (Bruno) | O nome "Cofre privado" sugeriria criptografia que **não está ligada** (Tink existe, não integrado). | **Corrigido**: rótulo "Manter só neste app" (não afirma criptografia) |
| 4 | **3** | 5 Prevenção de erros | Gravar / Gravar (Ana) | Sem aviso de pouco espaço em disco (a string existe, não é usada). WAV mono 44,1 kHz ocupa ~5,3 MB/min (90 min ≈ 475 MB). | **Aberto** |
| 5 | **3** | 5 Prevenção · 9 Recuperação | Todas (API 26–28) | Cópia pública em Músicas/Recordings só existe a partir da API 29; abaixo disso a gravação fica só no app, sem aviso. | **Aberto** (mostrar aviso ou pedir permissão legada) |
| 6 | 2 | 1 Visibilidade | Gravar / Gravar (Bruno) | Pausa por ligação mostra "Pausada" sem dizer o motivo nem que basta Retomar. | Aberto |
| 7 | 2 | 9 Recuperação de erros | Gravar | Erro (ex.: microfone em uso) explica a causa mas não sugere a ação. | Aberto |
| 8 | 2 | 7 Eficiência | Gravar / Ouvir (Carla) | Depois de parar, ouvir exige trocar de aba. | **Corrigido**: botão "Ver em Gravações" no cartão de salva |
| 9 | 2 | 9 Recuperação | Receber áudio / Encontrar (Carla) | OGG/AAC/OPUS são recusados (só M4A, WAV, MP3). Aviso existe, conversão não. | Aberto |
| 10 | 2 | 6 Reconhecimento | Gravações | Há "Favoritar", mas não há filtro de favoritas. | Aberto |
| 11 | 1 | 8 Minimalismo | Gravar | Texto "onde ficam as gravações" + chips + entrada competem com o botão. | Aberto (medir em teste real) |
| 12 | 1 | 3 Controle | Gravações | O mini-player não tem botão de fechar. | Aberto |
| 13 | 1 | 10 Ajuda | Ajustes | Sem ajuda no app; só licenças e "não usa internet". | Aberto |
| 14 | ? | Acessibilidade | Todas | Rótulos de TalkBack e `liveRegion` no status existem, mas **TalkBack e Accessibility Scanner não foram executados em aparelho**. | Pendência humana |

## O que está bem (verificado por teste)

- **Visibilidade do status (1)**: estado, cronômetro e VU sempre visíveis; formato bloqueado durante a gravação com explicação (`FluxoUsuarioTest`).
- **Prevenção de erros (5)**: excluir vai para a lixeira com **DESFAZER** (teste `excluirOferecerDesfazerERestaura`); permissão pedida em contexto.
- **Recuperação de erros (9)**: permissão negada duas vezes mostra explicação e botão **Abrir configurações**, sem crash (`PermissaoNegadaTest`).
- **Eficiência (7)**: a gravação mais recente toca com **1 toque**; achar entre 20 pelo nome parcial exige **2 toques** (aba + campo de busca).
- **Reconhecimento (6)**: busca sempre visível; entrada de áudio ativa exibida ("Entrada: …").
- **Privacidade**: sem permissão de internet (teste); métricas só com opt-in e sem nomes/áudio (testes).

## Etapas, hipóteses e telas

| Tela | Etapa | Hipótese (jornada.md) |
| --- | --- | --- |
| Gravar | Abrir, Gravar | H1 tempo até gravar ≤ 2 s; entrada ativa exibida |
| Gravações | Encontrar, Ouvir, Compartilhar | H2 ≤ 3 toques; tocar em 1 toque; compartilhar sem caminho bruto |
| Ajustes | Gravar (formato), Descobrir (confiança) | trocar para MP3 sem ajuda |
| Seus números | Descobrir, Encontrar | H5 abrir ≥ 60% em 7 dias; 0 vazamentos |
| Receber áudio | Encontrar, Ouvir | H4 aparecer em ≤ 3 s |

## Próxima revisão

Repetir esta análise depois do teste com 5 usuários (roteiro `05-roteiro-teste.md`) e do Accessibility Scanner; reordenar por severidade com os dados.
