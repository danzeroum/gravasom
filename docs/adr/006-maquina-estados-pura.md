# ADR 006 — Máquina de estados pura e testável

Status: aceito

## Contexto

A gravação passa por estados bem definidos e as transições inválidas (ex.: pausar
quando ocioso) precisam ser tratadas sem corromper o arquivo.

## Decisão

A máquina de estados `Idle → Preparing → Recording ⇄ Paused → Stopping → Saved`,
com `Error(causa)` alcançável de qualquer estado, vive em `:core:audio-engine`
(Kotlin/JVM puro), sem dependência de Android. Cada transição é uma função pura.

## Consequências positivas

- Testável só com o JDK, sem emulador (rápido e determinístico).
- Transições inválidas retornam erro sem mudar o estado.
- Reutilizável pelo serviço e pela UI.

## Consequências negativas (o preço)

- A lógica pura precisa ser espelhada no serviço (que é quem executa os efeitos).
- Exige disciplina para não vazar Android para dentro do módulo puro.

## Contexto de avaliação

Colocar a máquina no módulo Android a tornaria testável só em device, atrasando
cada mudança. O isolamento em JVM puro é o que permite TDD de verdade.
