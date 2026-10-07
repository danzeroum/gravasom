# Jornada do usuário (journey map) — 3 personas

Etapas: **Descobrir → Abrir → Gravar → Encontrar → Ouvir → Compartilhar**. Personas em `01-personas.md`.
Legenda: 😀 positivo · 😐 neutro · 😟 dor · ⭐ momento da verdade (a experiência é decidida aqui).

Premissas de privacidade (já decididas): o app **não tem permissão de internet**; telemetria só por opt-in, **só neste aparelho**,
sem áudio, nomes de arquivo ou localização (ADR-011).

## Jornada 1 — Ana (estudante, aulas de 50–90 min)

| Etapa | O que faz | Emoção | Dor / oportunidade | Resposta do produto |
| --- | --- | --- | --- | --- |
| Descobrir | Colega indica; instala | 😐 | Medo de app que "come" bateria e dados | Sem internet; consumo de bateria é meta medida (Fase 9) |
| Abrir | Chega na sala atrasada, abre o app | 😟 | Pressa: cada segundo de aula perdido dói | Abre direto na tela **Gravar**, sem login nem tutorial |
| Gravar | ⭐ 1º toque em Gravar, aceita a permissão | 😟→😀 | Dúvida "vai gravar mesmo?" | Permissão pedida **no 1º toque**, em contexto; estado "Gravando 00:12" e VU visíveis |
| Encontrar | Depois da aula procura a gravação | 😐 | Lista cheia, nomes iguais | Mais recente no topo, busca sempre visível, nome com data/hora |
| Ouvir | Revisa na hora de estudar | 😀 | Quer pular e acelerar | Toque no item já toca; velocidade 0,5–2× |
| Compartilhar | Manda para o grupo de estudo | 😟 | Arquivo grande, formato que o colega não abre | Folha de compartilhamento do sistema; M4A leve por padrão |

## Jornada 2 — Bruno (jornalista, entrevista com tela bloqueada)

| Etapa | O que faz | Emoção | Dor / oportunidade | Resposta do produto |
| --- | --- | --- | --- | --- |
| Descobrir | Procura gravador confiável | 😐 | Desconfia de app que sobe áudio para a nuvem | "Sem internet" comprovável no manifesto; gravação fica no aparelho |
| Abrir | Abre o app e coloca o celular no bolso | 😐 | Medo de a gravação parar | Serviço em primeiro plano + notificação com Parar |
| Gravar | ⭐ Toca uma ligação no meio da entrevista | 😟 | Perder a entrevista | Foco perdido **pausa e preserva**; 0 corrompidos em 100 ciclos (medido) |
| Encontrar | Volta à redação | 😐 | Precisa achar entre dezenas | Busca por nome; favoritos |
| Ouvir | Confere trechos | 😐 | Precisa ir a um ponto | Barra de posição; (marcadores: futuro) |
| Compartilhar | ⭐ Envia à redação (WhatsApp/e-mail/Drive) | 😟→😀 | Caminho do arquivo exposto; arquivo não abre | Compartilhar via `content://` (FileProvider), **Salvar em…** (Drive/Arquivos) |

## Jornada 3 — Carla (música, ideia de violão de 30 s)

| Etapa | O que faz | Emoção | Dor / oportunidade | Resposta do produto |
| --- | --- | --- | --- | --- |
| Descobrir | Vê no app de ideias da banda | 😐 | Formato que o grupo não abre | M4A/WAV/MP3 selecionáveis |
| Abrir | Ideia chegando, abre rápido | 😟 | Latência para começar | Atalho do launcher "Gravar agora" e Tile |
| Gravar | ⭐ Grava a ideia | 😀 | Entrada de áudio errada (fone BT no ouvido) | Mostra a **entrada ativa** e deixa escolher (celular, USB, Bluetooth) |
| Encontrar | Procura "ideia violão" | 😐 | — | Busca por nome parcial |
| Ouvir | ⭐ Para e quer ouvir já | 😟→😀 | Sair da tela atrapalha | Toque no item toca na própria lista (mini-player) |
| Compartilhar | Manda para a banda; recebe áudio deles | 😐 | Importar áudio de outro app | **Receber áudio** (Compartilhar → Gravasom) entra na lista |

## Momentos da verdade (consolidado)

1. **1º toque em Gravar** (Ana, Bruno, Carla): permissão em contexto + gravação começa visivelmente.
2. **Ligação durante a gravação** (Bruno): nada se perde.
3. **Parar → ouvir** (Carla): ouvir sem sair da lista.
4. **Compartilhar** (Bruno): o destino recebe um arquivo que abre.

## Telas, etapas e hipóteses mensuráveis

Formato: *Acreditamos que [mudança] fará [persona] [resultado]; saberemos quando [métrica] atingir [alvo]*.
Eventos locais citados estão em `docs/adr/011-metricas-locais.md`.

| Tela | Etapa(s) que resolve | Hipótese | Métrica (como se mede) | Alvo |
| --- | --- | --- | --- | --- |
| **Gravar** | Abrir, Gravar | Acreditamos que abrir direto na tela Gravar, com 1 botão e permissão no 1º toque, fará **Ana** começar a aula sem esforço; saberemos quando **tempo do toque em Gravar até o estado Gravando** atingir ≤ 2 s | `TEMPO_ATE_GRAVAR` (ms) — teste de UI no emulador e evento local | mediana ≤ 2000 ms |
| **Gravar** (entrada) | Gravar | Acreditamos que mostrar a entrada ativa fará **Carla** não gravar pelo microfone errado; saberemos quando **gravações iniciadas com a entrada exibida** forem 100% | verificação de UI (texto "Entrada: …" presente em estado pronto/gravando) | 100% |
| **Gravações** | Encontrar, Ouvir | Acreditamos que busca sempre visível e item que toca com 1 toque fará **Bruno e Carla** achar e ouvir sem navegar; saberemos quando **≥ 90% acharem a última gravação em ≤ 3 toques** e a reprodução ocorrer em ≤ 1 toque | contagem de toques no teste de UI; roteiro 5 tarefas (tarefas 3) | ≥ 90%; ≤ 3 toques; 1 toque |
| **Gravações** (compartilhar) | Compartilhar | Acreditamos que compartilhar pelo sistema e "Salvar em…" fará **Bruno** entregar a entrevista sem expor caminhos; saberemos quando **o arquivo chegar e abrir no destino** em 100% das tentativas do roteiro | tarefa 4 do roteiro; teste verifica `content://` sem caminho bruto | 100% |
| **Ajustes** | Gravar (formato), Descobrir (confiança) | Acreditamos que formato e privacidade explícitos fará **Bruno** confiar; saberemos quando **≥ 90% concluam "trocar o formato para MP3" sem ajuda** | tarefa extra do roteiro; evento `FORMATO_ALTERADO` | ≥ 90% |
| **Seus números** | Descobrir, Encontrar | Acreditamos que mostrar números do próprio uso, só no aparelho, fará **Ana** perceber valor sem ceder dados; saberemos quando **≥ 60% dos que ativarem os números os abrirem em 7 dias** (e 0 eventos com nome/áudio) | evento `NUMEROS_ABERTO`; teste de conteúdo dos eventos | ≥ 60%; 0 vazamentos |
| **Receber áudio** | Encontrar, Ouvir | Acreditamos que receber áudio de outros apps fará **Carla** juntar ideias da banda no mesmo lugar; saberemos quando **a importação aparecer na lista em ≤ 3 s** | teste instrumentado + evento `IMPORTADO` | ≤ 3 s |

Cada tela do app cita a etapa e a hipótese em `docs/ux/heuristica.md` e nos comentários de KDoc do código.
