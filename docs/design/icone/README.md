# Ícones do Gravasom

Desenho original: **anel** (o botão de gravar) com **três barras de voz** dentro. Sem marcas nem ícones de terceiros.
Regra das silhuetas: **anel vazado = parado ou pausado; disco cheio = gravando.**

| Arquivo | Tamanho | Uso | Destino no app (Vector Drawable) |
| --- | --- | --- | --- |
| `ic_launcher_background.svg` | 108 × 108 | Camada de fundo do ícone adaptativo, `color.icon.background` | `app/src/main/res/drawable/ic_launcher_background.xml` |
| `ic_launcher_foreground.svg` | 108 × 108 | Camada da frente: anel `color.icon.foreground`, barras `color.icon.foregroundAccent`. Tudo dentro da zona segura (círculo de raio 33 no centro) | `.../drawable/ic_launcher_foreground.xml` |
| `ic_launcher_monochrome.svg` | 108 × 108 | Ícone temático do Android 13+ (o sistema pinta) | `.../drawable/ic_launcher_monochrome.xml` |
| `ic_stat_gravasom.svg` | 24 × 24 | Notificação de mídia e avisos | `core/service/.../drawable/ic_stat_gravasom.xml` |
| `ic_stat_gravando.svg` | 24 × 24 | Notificação **gravando** (disco cheio, barras vazadas) | `.../drawable/ic_stat_gravando.xml` |
| `ic_stat_pausado.svg` | 24 × 24 | Notificação **pausada** (anel + pausa) | `.../drawable/ic_stat_pausado.xml` |
| `ic_tile_gravar.svg` | 24 × 24 | Bloco parado (anel + ponto) | `.../drawable/ic_tile_gravar.xml` |
| `ic_tile_gravando.svg` | 24 × 24 | Bloco gravando (disco cheio + quadrado vazado = "toque para parar") | `.../drawable/ic_tile_gravando.xml` |

## Conversão

Os SVGs usam só `<path>` com preenchimento sólido e `fill-rule="evenodd"`, sem traços, gradientes ou texto, para a
conversão ser direta: Android Studio › New › Vector Asset › Local file, mantendo 108 dp (launcher) ou 24 dp (silhuetas).
`fill-rule="evenodd"` vira `android:fillType="evenOdd"`.

As silhuetas de 24 dp são brancas sobre transparente, como a barra de status exige; o sistema aplica a cor.

## O que substitui

| Hoje | Passa a ser |
| --- | --- |
| `app/src/main/res/mipmap/ic_launcher.xml` → `@android:drawable/ic_btn_speak_now` (ícone do sistema) | `background`, `foreground` e `monochrome` acima |
| `app/src/main/res/values/colors.xml` → `ic_launcher_background` `#12151A` | Drawable `ic_launcher_background` (`#004E5A`) |
| `RecordingNotification.setSmallIcon(android.R.drawable.ic_btn_speak_now)` | `ic_stat_gravando` / `ic_stat_pausado` conforme o estado |
| `core/service/AndroidManifest.xml` → tile `android:icon="@android:drawable/ic_btn_speak_now"` | `ic_tile_gravar` (e `ic_tile_gravando` via `Tile.icon` quando ativo) |

Os arquivos são gerados por `../ferramentas/icons.py`.
