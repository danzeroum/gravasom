# Spike de UX — Transcrição offline (Fase 1)

Protótipos Compose das telas que a transcrição offline precisa, para revisão visual antes de virar produção. **Fora do build normal**, igual ao `spike/whisper`: só entra com `-Pspike` (`settings.gradle.kts`).

## Para que serve

Revisar desenho de UX com o dono e com usuários (ver `docs/ux-validation.md`) **antes** de mexer no app principal. Nada aqui é código de produto: não há ViewModel, não há Hilt, não há Room, não há permissão alguma.

## O que mostra

`PreviewActivity` empilha quatro blocos:

1. **Toggle "Transcrever ao gravar"** — onde ele mora na tela Gravar, e o que acontece quando o modelo de idioma não está baixado (nunca liga mudo).
2. **Aba "Transcrição"** no detalhe da gravação — três estados: rodando, concluída, falha com "Tentar novamente".
3. **Gerenciar modelos de idioma** — pt-BR, en-US, es-ES, fr-FR; baixar / cancelar / baixado.
4. **Diálogo de consentimento de primeira vez** — "o áudio e o texto não saem do aparelho".

## Como rodar

```
./gradlew -Pspike :spike:transcricao-ux:installDebug
```

Sem `-Pspike` o módulo não é incluído e o build normal (14 módulos + este spike) não muda de tamanho nem de tempo.

## Regras de copy que os protótipos fixam

- **Offline é promessa dita na cara.** Toda superfície que liga a transcrição diz "sem internet" ou "no aparelho".
- **Modelo ausente bloqueia com aviso claro**, nunca com transcrição vazia silenciosa.
- **Falha é recuperável:** o texto de erro diz o que houve (espaço, modelo) e oferece "Tentar novamente".
- **Idioma é escolha explícita** e o usuário vê o tamanho do download antes de autorizar.

## O que este spike **não** é

- Não mede WER (isso é o `spike/whisper`).
- Não é a UI final: é desenho para revisão. A versão de produção entra em `feature/transcribe` (ADR-016) respeitando MVI/Hilt/Compose M3 do repo.
