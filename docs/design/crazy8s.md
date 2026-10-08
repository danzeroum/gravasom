# Crazy 8s — tela Gravar (rodada da identidade visual)

A primeira rodada (`docs/ux/02-crazy-8s.md`) decidiu **o que** a tela tem: botão central, chips de formato, VU. Esta
rodada decide **como o estado aparece**: oito jeitos de mostrar pronto → gravando → pausado, cada um esboçado em 1 minuto.

**Critérios** (nota de 1 a 5):

- **Estado em 1 s:** dá para dizer "está gravando?" num relance, também sem ver vermelho?
- **Uma mão:** os controles ficam ao alcance do polegar (metade de baixo)?
- **Calma:** a tela continua quieta (sem ruído visual quando nada acontece)?
- **Esforço:** quanto custa implementar em Compose (5 = barato)?

Desenhos em 34 colunas, aproximando um celular de 360 dp. Os três quadros de cada variação são pronto · gravando ·
pausado.

---

## 1. Tela inteira muda de cor

```
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│              │ │██████████████│ │▒▒▒▒▒▒▒▒▒▒▒▒▒▒│
│    00:00     │ │██  00:12  ███│ │▒▒  00:12  ▒▒▒│
│              │ │██ GRAVANDO ██│ │▒▒  PAUSADA ▒▒│
│    ( ● )     │ │██  ( ■ )  ███│ │▒▒ (▶) (■) ▒▒▒│
└──────────────┘ └──────────────┘ └──────────────┘
```

O fundo todo fica vermelho gravando e âmbar pausado. Impossível não ver. Mas grita o tempo todo de uma aula de 90 min,
gasta bateria em OLED e transforma o vermelho em papel de parede.

## 2. Faixa de estado no topo + botão que muda de forma

```
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ M4A WAV MP3  │ │(● GRAVANDO  )│ │(❚❚ PAUSADA  )│
│    00:00     │ │    00:12     │ │    00:12 ~   │
│  ▁▁▁▁▁▁▁▁▁   │ │  ▂▄▆█▆▄▂ Bom │ │  ▁▁▁▁▁▁▁▁▁   │
│              │ │              │ │              │
│    ( ● )     │ │ (❚❚) [■] (⚑) │ │ (▶)  [■] (⚑) │
│    Gravar    │ │Pausar Parar  │ │Retomar Parar │
└──────────────┘ └──────────────┘ └──────────────┘
```

Uma pílula no topo diz o estado com ponto + palavra. O círculo do Gravar morfa no quadrado do Parar. Vermelho só na
pílula e no Parar. A pílula pode virar faixa global nas outras abas.

## 3. Anel de progresso em volta do botão

```
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│    00:00     │ │    00:12     │ │    00:12     │
│   .-----.    │ │   .=====.    │ │   .=- -=.    │
│  (   ●   )   │ │  ║   ■   ║   │ │  ¦   ■   ¦   │
│   '-----'    │ │   '====='    │ │   '-- --'    │
└──────────────┘ └──────────────┘ └──────────────┘
```

Um anel grosso gira em volta do botão enquanto grava e fica tracejado pausado. Bonito e compacto. Movimento é o sinal
principal: some com "Remover animações" e não ajuda leitor de tela.

## 4. Cronômetro gigante como herói

```
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│              │ │ ● GRAVANDO   │ │ ❚❚ PAUSADA   │
│   00:00      │ │  00:12       │ │  00:12       │
│   (96 sp)    │ │  (vermelho)  │ │  (âmbar)     │
│ ( ● Gravar ) │ │ [■ Parar   ] │ │ [▶ Retomar ] │
└──────────────┘ └──────────────┘ └──────────────┘
```

O número ocupa metade da tela e muda de cor. Ótimo para ver de longe (celular na mesa). Número colorido é texto colorido:
o vermelho em 96 sp sobre fundo escuro precisa de outro tom, e "cor do número" ainda é cor como sinal principal.

## 5. Onda ao vivo como fundo

```
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│    00:00     │ │    00:12     │ │    00:12     │
│ ──────────── │ │ ▂▄▆█▆▄▂▄▆█▆▄ │ │ ▂▄▆█▆▄▂ ──── │
│ ──────────── │ │ ▂▄▆█▆▄▂▄▆█▆▄ │ │ ▂▄▆█▆▄▂ ──── │
│    ( ● )     │ │   (■)  (❚❚)  │ │   (■)  (▶)   │
└──────────────┘ └──────────────┘ └──────────────┘
```

A onda rola atrás dos controles enquanto grava e congela pausada. Dá retorno rico (Carla, música). Calcular e desenhar a
onda ao vivo custa CPU/bateria em gravações de 90 min, e em silêncio a onda fica reta e parece "parado".

## 6. Cartão de estado que se transforma

```
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│┌────────────┐│ │┌────────────┐│ │┌────────────┐│
││Pronto para ││ ││● Gravando  ││ ││❚❚ Pausada  ││
││gravar      ││ ││  00:12     ││ ││  00:12     ││
│└────────────┘│ │└────────────┘│ │└────────────┘│
│    ( ● )     │ │ (❚❚) (■) (⚑) │ │ (▶) (■) (⚑)  │
└──────────────┘ └──────────────┘ └──────────────┘
```

É a tela de hoje (`StatusCard` em `RecordScreen.kt`) com cor por estado. Barato. O cartão fica no meio da tela, longe do
polegar e longe do topo, onde o olho procura status.

## 7. Modo bolso (tela mínima)

```
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│              │ │●             │ │❚❚            │
│              │ │   00:12      │ │   00:12      │
│  toque duplo │ │              │ │              │
│  para gravar │ │ deslize ↑    │ │ deslize ↑    │
│              │ │ para parar   │ │ para retomar │
└──────────────┘ └──────────────┘ └──────────────┘
```

Tela quase preta, gesto grande para evitar toque acidental no bolso (Bruno). Interessante como modo extra, mas gesto não
é descobrível e fere a meta de "1 toque para gravar".

## 8. Metade de baixo para o polegar, metade de cima para ler

```
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ Entrada: Mic │ │ ● GRAVANDO   │ │ ❚❚ PAUSADA   │
│    00:00     │ │    00:12     │ │    00:12     │
│  ▁▁▁▁▁▁▁▁▁   │ │  ▂▄▆█▆▄▂     │ │  ▁▁▁▁▁▁▁▁▁   │
│══════════════│ │══════════════│ │══════════════│
│    ( ● )     │ │ (❚❚) [■] (⚑) │ │ (▶) [■] (⚑)  │
│ M4A WAV MP3  │ │ (bloqueado)  │ │ (bloqueado)  │
└──────────────┘ └──────────────┘ └──────────────┘
```

Divide a tela: leitura em cima, ação embaixo, chips de formato junto do polegar. Ótimo para uma mão. Sozinha não resolve
o reconhecimento do estado.

---

## Matriz

| # | Variação | Estado em 1 s | Uma mão | Calma | Esforço | Total |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Tela inteira colorida | 5 | 3 | 1 | 5 | 14 |
| 2 | **Faixa + botão que morfa** | **5** | 3 | **4** | 4 | **16** |
| 3 | Anel de progresso | 3 | 3 | 4 | 3 | 13 |
| 4 | Cronômetro herói | 4 | 3 | 3 | 5 | 15 |
| 5 | Onda ao vivo | 3 | 3 | 2 | 2 | 10 |
| 6 | Cartão de estado (atual) | 3 | 2 | 4 | 5 | 14 |
| 7 | Modo bolso | 3 | 4 | 5 | 2 | 14 |
| 8 | **Metade de cima / de baixo** | 2 | **5** | 4 | 4 | 15 |

## Escolha: 2 + 8 (+ o tamanho do 4)

- Da **2**: pílula de estado no topo com ● GRAVANDO / ❚❚ PAUSADA (escura × clara), botão que morfa de círculo para
  quadrado, vermelho só ali. A pílula vira **faixa global** nas outras abas.
- Da **8**: leitura em cima (estado, entrada, cronômetro, VU), ação na metade de baixo (botões), chips de formato junto
  do polegar.
- Da **4**: cronômetro grande (`typography.extra.cronometro`, 64 sp) com algarismos tabulares.
- Descartadas: 1 (cansa e gasta bateria), 3 e 5 (movimento ou desenho como sinal principal), 7 (gesto não descobrível;
  fica como ideia para depois).

O resultado está desenhado em [`telas.md`](telas.md), seção 1, e no canvas de design.
