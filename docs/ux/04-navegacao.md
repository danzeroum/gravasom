# Arquitetura de informação e navegação

## Organização

Navegação inferior com 3 destinos. A tela Gravar faz uma coisa; o resto fica a um
toque de distância, nunca no caminho do botão.

| Tela | Conteúdo | Rótulo |
| --- | --- | --- |
| Gravar | Botão, cronômetro, VU, chip de formato | Gravar |
| Gravações | Lista por data, busca, duração, formato, tamanho | Gravações |
| Detalhe/Player | Player, forma de onda, marcadores, renomear, compartilhar, excluir | (aberto pela lista) |
| Ajustes | Formato padrão, qualidade, local, privacidade | Ajustes |

## Hierarquia

- Gravações → toque no item → Detalhe/Player.
- Detalhe/Player → ações (renomear, compartilhar, excluir) em bottom sheet.
- Ajustes → sub-telas só se necessário (qualidade avançada).

## Rotulação

Nomes curtos em pt-BR, consistentes com o Material 3. Nome padrão do arquivo:
`Gravação 2026-10-07 14h32.m4a`.
