# Identidade visual — Parte B (o que falta, arquivo por arquivo)

A Parte A entregou o tema, a tipografia, as formas, os ícones e a tela Gravar. A Parte B **não pôde** ser feita junto porque mexe
em arquivos que a Onda 2 altera em paralelo (`core/service`, `core/audio-engine`, `core/dsp`, `core/data`, `core/domain`,
`feature/settings`, `feature/player`). Fazer **depois do merge da Onda 2**, em uma branch nova a partir da main.

## O que a Parte A já deixou pronto (para a Parte B só usar)

| Peça | Onde | Como usar |
| --- | --- | --- |
| Tema com paleta | `core/ui/.../GravasomTheme.kt` | `GravasomTheme(darkTheme, paleta = Paleta.GRAVASOM)`; `GravasomTheme.extra` dá `gravando`, `pausado`, `pronto`, `cofre`, `vu`, `onda`, `marker` |
| Paletas | `core/ui/.../Paleta.kt` | `Paleta.fromId(id)`, `Paleta.disponiveis()` (esconde "Cores do sistema" antes do Android 12), `efetiva()` |
| Tokens em Kotlin | `core/ui/.../theme/{Color,Type,Shape,Dimens,Motion}.kt` | **Gerados** por `docs/design/ferramentas/gerar_kotlin.py`; nunca editar à mão |
| Faixa de estado | `core/ui/.../components/FaixaDeEstado.kt` | `FaixaDeEstado(EstadoFaixa.Gravando/Pausada, elapsedMillis, onClick)` |
| Medidor de nível | `core/ui/.../components/MedidorDeNivel.kt` | `MedidorDeNivel(nivelDb, ativo, picoDb)` |
| Ícones | `app/.../drawable/ic_launcher_*.xml`, `core/service/.../drawable/ic_stat_*`, `ic_tile_*` | Gerados por `svg_para_vd.py`; os de `core/service` ainda **não estão ligados** a nada |
| Fontes e licença | `core/ui/src/main/res/font`, `core/ui/src/main/assets/licencas/OFL-atkinson-hyperlegible-next.txt` | Só Regular 400 e SemiBold 600 |

## Por arquivo

### app
- `app/src/main/kotlin/com/gravasom/MainActivity.kt`: ler a paleta e o tema (Sistema/Claro/Escuro) das preferências e passar a
  `GravasomTheme(darkTheme = …, paleta = …)`. Hoje usa o padrão (Gravasom, tema do sistema). Troca de paleta com cross-fade de
  `GravasomDuration.medium2` (300 ms), sem reiniciar a Activity.
- `app/src/main/kotlin/com/gravasom/GravasomApp.kt`: `NavigationBar` conforme `telas.md` §1a item 7 (fundo `surfaceContainer`,
  indicador `secondaryContainer`, ícones `Rounded` cheios no ativo e contornados nos outros); **faixa de estado global**
  (`FaixaDeEstado`) logo abaixo da barra de status em todas as abas enquanto houver gravação, com toque levando à aba Gravar
  (`telas.md` §0). Na aba Gravar a faixa já está dentro da tela; esconder a global ali.
- `app/src/main/res/xml/shortcuts.xml` e `app/src/main/AndroidManifest.xml`: conferir `android:roundIcon` (não há `ic_launcher_round`;
  o adaptativo serve) e o ícone dos atalhos.
- Licenças (`docs/licencas/` e a tela "Licenças de código aberto"): acrescentar Atkinson Hyperlegible Next (SIL OFL 1.1, arquivo em
  `core/ui/src/main/assets/licencas/`).

### core/data
- `PreferencesRepository`: `paleta` (id texto, padrão `gravasom`) e `tema` (`system|light|dark`), com `Paleta.fromId`.

### core/domain
- `MarkerColor`: acrescentar `GRAPHITE` (ordem `AMBER, RED, GREEN, BLUE, PURPLE, GRAPHITE`, R1). `RED` continua `RED` no código e no
  banco; na tela é Rosa. A cor sai de `GravasomTheme.extra.marker.forDomainName(cor.name)`; o rótulo, de `color.marker.rotulos`
  (strings novas: Âmbar, Verde, Azul, Roxo, Rosa, Grafite).

### core/service
- `RecordingService`/`LevelMeter` (R3): publicar o **pico** em dBFS (maior |amostra| do bloco) além do RMS, em
  `RecordingServiceState`. No M4A, `MediaRecorder.getMaxAmplitude()` já é pico: o mesmo valor serve às barras e ao aviso.
- `RecordingNotification.kt`: `setSmallIcon(ic_stat_gravando / ic_stat_pausado)`, gravando com `setColorized(true)` e
  `setColor(#BA1A1A)`, pausado sem colorir e `setColor(#926000)`, `setUsesChronometer(true)` só gravando, texto "12:40 gravados" pausado,
  ações na ordem Pausar/Retomar · Marcar · Parar (`telas.md` §7a).
- Bloco de Configurações Rápidas (tile): `Tile.state`, ícone (`ic_tile_gravar`/`ic_tile_gravando`/`ic_stat_pausado`), `label` e `subtitle` por
  estado, lendo o estado atual em `onStartListening` (R2, `telas.md` §6); trocar o `android:icon` do `AndroidManifest.xml` do módulo
  (hoje `@android:drawable/ic_btn_speak_now`).
- Widget (Glance, `RecordWidget.kt`): 3 estados × 2 tamanhos (`SizeMode.Responsive`, quebra em 250 dp), cores de `Paleta`
  Gravasom/Mixlirous (com "Cores do sistema" usa Gravasom), "Marcar" desabilitado com texto fora de gravação (`telas.md` §5).
  Depende de a Onda 2 ter mexido nesse arquivo (aviso do "Marcar").
- Ícone da notificação de mídia (R7): `ic_stat_gravasom` no provider do Media3 (ver `docs/design/icone/README.md`). Fica em
  `feature/player/PlaybackService.kt`.

### feature/record
- `RecordViewModel.kt`/`RecordUiState`: acrescentar `peakDb` e passar a `MedidorDeNivel(nivelDb, ativo, picoDb)` (hoje o próprio nível decide o
  "Alto demais"); segmento 24 aceso por 1 s (`GravasomMotion.vuPicoRetido`); motivo da pausa ("Pausada pela ligação. Toque em Retomar.",
  `telas.md` §1c). As cadeias de texto novas já existem em `core/ui` (`ui_level_*`, `ui_strip_*`).
- Seletor de formato: texto "O formato não muda durante a gravação" já aparece; falta o ícone de cofre quando `privateMode` com a
  string `record_where_private` curta (hoje reaproveita a atual).

### feature/player
- `WaveformView`/`PlayerPanel`: cores de `GravasomTheme.extra.onda` (fundo, barra, tocada, cursor, silêncio com **hachura 45° a cada 6 dp**,
  seleção, alça) e de `marker.*` (linha `fill`, bandeira com o **número**, chip `container/onContainer`); trocar `tertiary`/`primary`
  atuais (`telas.md` §3 e §8 itens 3 e 4).
- Diálogo do marcador com seletor de 6 cores (círculo de 32 dp em alvo de 48 dp, anel e `Check`, nome da cor em texto), `telas.md` §3a.
- Chips (pular silêncio, 1,5x, Etiquetas) todos no mesmo estilo e rolando na horizontal; "Trecho" com texto ao lado da tesoura.

### feature/library (aba Gravações)
- Item, busca, filtro de etiquetas, mini-player e "excluída · DESFAZER" conforme `telas.md` §2; cofre sempre de
  `GravasomTheme.extra.cofre` (nunca `colorScheme.tertiary`) com cadeado e o texto "No cofre"; favorita com `marker.ambar.fill`.

### feature/settings
- Seção **Aparência**: `SegmentedButton` Tema (Sistema, Claro, Escuro) e 3 `RadioButton` de Paleta (Gravasom, Mixlirous, Cores do
  sistema só com `Paleta.disponiveis()`), cada um com 3 amostras de 16 dp e o texto de ajuda "Gravando, pausado e marcadores
  mantêm as mesmas cores." (`telas.md` §4, itens 24–26, D1 e D12). Cofre com `cofre.base`; "Excluir todas as gravações" em `error`.
  Tem de vir **depois** do merge da Onda 2, que mexe na seção de presets desse módulo.

### Testes e verificação da Parte B
- Capturas das outras telas (Gravações, Player, Ajustes) com o mesmo `CapturasTest` (Robolectric), claro/escuro × Gravasom/Mixlirous.
- `Accessibility Scanner` nas 4 telas (zero avisos de contraste e de alvo) e o teste de 5 segundos de `decisoes.md` ("Como validar"): é
  **validação com pessoas**, não substitui nada do que a Parte A verificou por teste automático.
- Instrumentados que tocam `botao_gravar`, `botao_parar`, `botao_pausar`, `botao_marcar`, `cronometro` e `chip_*` seguem valendo (as tags foram mantidas;
  o botão ganhou `botao_aguarde` durante Preparando/Salvando). **Não foram executados aqui: não há emulador.**

## Limites conhecidos da Parte A

- A faixa "Cores do sistema" (Android 12+) pega o esquema do sistema e só corrige a borda (`outline`) para 3:1; os demais pares passam no AA
  porque o Material os define assim, e isso foi medido com as cores padrão do Robolectric (não com todos os papéis de parede possíveis).
- Animações do anel e do piscar não rodam nos testes do Compose (a política de animação infinita do teste as desliga); foram conferidas
  por leitura do código, não por captura.
- Fonte: o zero da Atkinson Hyperlegible é riscado (desenho da fonte, para não confundir com "O").
