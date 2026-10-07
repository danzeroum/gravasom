# ADR 011 — Métricas de uso locais, opt-in, sem identificadores

Status: aceito

## Contexto

Precisamos de dados para decidir (hipóteses de `docs/ux/jornada.md`), mas o app **não tem permissão de internet** e a decisão
de privacidade já registrada é: telemetria só por opt-in. Áudio de voz é dado pessoal (LGPD).

## Decisão

1. **Eventos ficam só no aparelho** (tabela Room `usage_events`). Não há envio. Exportação é **manual**: o usuário toca em
   "Exportar números" e escolhe onde salvar (CSV via Storage Access Framework).
2. **Opt-in, desligado por padrão** ("Registrar meus números neste aparelho"). Desligado ⇒ **nenhum evento é gravado**.
   Religado ⇒ os contadores voltam a subir. Desligar não apaga o histórico; há "Apagar meus números".
3. **Esquema fechado por construção**: `tipo` (enum), `momento` (epoch ms), `valor` (Long opcional, ex.: duração ou
   tempo em ms) e `rotulo` (enum opcional, ex.: formato). **Não há campo de texto livre**; a API só aceita enums e números, então nome
   de arquivo, caminho, áudio ou localização **não cabem** no esquema. Um teste insere todos os tipos e confere isso.
4. "Seus números" mostra (a) estatísticas **derivadas da própria biblioteca** (gravações, tempo total, formato mais usado) — valem
   mesmo sem opt-in — e (b) contadores de eventos, só quando o opt-in estiver ligado.
5. Sem SDK de terceiros.

## Eventos

`APP_ABERTO`, `GRAVACAO_INICIADA(rotulo=formato)`, `TEMPO_ATE_GRAVAR(valor=ms)`, `GRAVACAO_CONCLUIDA(valor=duração ms, rotulo=formato)`,
`PERMISSAO_NEGADA`, `BUSCA_USADA`, `REPRODUCAO_INICIADA`, `COMPARTILHADO(rotulo=canal)`, `IMPORTADO`, `FORMATO_ALTERADO(rotulo)`,
`NUMEROS_ABERTO`.

## Consequências

- Dados só existem quando alguém ativa e exporta: o aprendizado (docs/aprendizado.md) usa também **medições automatizadas no emulador**
  (tempo até gravar, toques até achar), que não dependem do opt-in de ninguém.
- Eventos ficam em claro no aparelho; não contêm dado pessoal, então não entram no cofre.
