# Validação de UX — Transcrição offline (Fase 1)

Atualizado: 2026-10-12

Este documento é o **plano de validação** dos protótipos de `spike/transcricao-ux`. Ele **não substitui** a validação com usuários reais (que exige pessoas) — é o roteiro para ela. O estado atual está registrado de forma honesta ao fim.

## Objetivo

Verificar que as quatro superfícies novas (toggle, aba de transcrição, gerenciar modelos, consentimento) fazem sentido para as personas de `docs/ux/01-personas.md` antes de a feature virar código de produção.

## Como rodar o protótipo

```
./gradlew -Pspike :spike:transcricao-ux:installDebug
```

Abrir o app `Gravasom spike UX transcrição` no emulador API 33+ ou aparelho mid-range. Sem `-Pspike` o módulo não entra no build.

## Roteiro de teste (5 tarefas, ~25 min por pessoa)

Cada tarefa é observada com o protótipo em mãos. Registrar: **sucesso**, **tempo**, **erros**, **fala espontânea**.

| # | Tarefa | O que se observa | Critério de sucesso |
| --- | --- | --- | --- |
| T1 | "Você quer transcrever automaticamente tudo o que gravar. Ache onde liga isso." | Encontra o toggle na tela Gravar sem procurar em Ajustes | Acha em ≤ 30 s sem pedir ajuda |
| T2 | "O texto transcrito de uma reunião já está pronto. Copie tudo." | Encontra a aba Transcrição e o botão Copiar tudo | Acha em ≤ 20 s |
| T3 | "Você fala inglês às vezes. Transcreva uma gravação em inglês." | Vai em Gerenciar modelos, escolhe en-US, volta e troca o idioma | Acha em ≤ 60 s |
| T4 | "O modelo ainda não baixou e você tentou ligar a transcrição. O que acontece?" | Lê o aviso e entende por que não ligou | Descreve o motivo sem ler o texto em voz alta |
| T5 | "A transcrição de uma gravação falhou. O que você faz?" | Acha "Tentar novamente" e entende o motivo exibido | Aponta o botão sem hesitar |

### Perguntas abertas depois das tarefas

1. Antes de ligar, o que você acha que vai acontecer com seu áudio?
2. Se a transcrição demorar 2 minutos para uma reunião de 10 minutos, isso te incomoda?
3. O aviso "~57 MB por idioma" mudaria sua decisão de baixar? Por quê?
4. Você esperaria achar a aba "Transcrição" em algum outro lugar?

### Personas-alvo (de `docs/ux/01-personas.md`)

- **Estudante gravando aula** — o caso em que a transcrição ganha valor de estudo (busca por palavra).
- **Jornalista em entrevista** — o caso em que a precisão importa.
- Terceira persona, se disponível para o teste.

## Estado da validação

| Item | Estado | Observação |
| --- | --- | --- |
| Protótipos Compose escritos | **feito** | `spike/transcricao-ux` (4 blocos, gated por `-Pspike`) |
| Roteiro de teste escrito | **feito** | este arquivo |
| Execução com 2+ usuários reais | **PENDENTE (humano)** | depende de pessoas; igual à pendência da Fase 1 original (`docs/status.md`) |
| Registro de sucesso/tempo/erros | **PENDENTE** | preencher a tabela abaixo após os testes |

## Resultados

_(a preencher após a validação; não há número inventado aqui — este arquivo é o plano, não o resultado)_

| Pessoa | T1 | T2 | T3 | T4 | T5 | Comentário |
| --- | --- | --- | --- | --- | --- | --- |
| — | — | — | — | — | — | — |

## Decisões pendentes que a validação precisa responder

1. **Toggle:** o rótulo "Transcrever ao gravar" é claro, ou precisa dizer "sem internet" no próprio rótulo?
2. **Aba Transcrição:** deve ficar como aba (na mesma tela) ou como seção abaixo do player (a tela já tem muita coisa)?
3. **Modelo por idioma:** o usuário entende que precisa baixar **por idioma** (57 MB cada) ou acha que uma vez só resolve?
4. **Consentimento:** aparece uma vez por instalação ou uma vez por idioma baixado?

## O que este documento **não** é

- Não é medição de qualidade de transcrição (isso é `docs/transcription-quality-report.md`, Fase 5).
- Não é acessibilidade formal (isso é `AcessibilidadeTest` + TalkBack, Fase 4).
- Não é aprovação de arquitetura (isso é `docs/adr/016-transcricao-offline-producao.md`).
