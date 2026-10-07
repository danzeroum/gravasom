# ADR 004 — Estado com StateFlow e UDF (MVI simplificado)

Status: aceito

## Contexto

A gravação tem estado que a UI precisa refletir em tempo real (cronômetro, VU,
botões) e que sobrevive à tela bloqueada e ao serviço em primeiro plano.

## Decisão

O serviço é a fonte única de verdade do estado da gravação e o expõe por
`StateFlow`. A UI coleta o fluxo e envia intenções (comandos) de volta. Padrão
UDF (unidirectional data flow), na prática um MVI simplificado.

## Consequências positivas

- Um único dono do estado, evitando divergência entre UI e serviço.
- Testável: a máquina de estados é pura; o fluxo é observável com Turbine.
- Funciona com a tela bloqueada, porque o serviço continua vivo.

## Consequências negativas (o preço)

- Acoplamento da UI ao contrato do serviço; mudar o contrato toca os dois lados.
- Estado exposto como fluxo exige cuidado com re-emissão e com o ciclo de vida.

## Contexto de avaliação

A alternativa (cada tela guardar seu estado) quebraria na tela bloqueada e
duplicaria o cronômetro.
