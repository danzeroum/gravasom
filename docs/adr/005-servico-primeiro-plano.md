# ADR 005 — RecordingService em primeiro plano (type=microphone)

Status: aceito

## Contexto

Gravar com a tela bloqueada ou com o app em segundo plano exige um serviço em
primeiro plano. A partir do Android 14, o tipo `microphone` é obrigatório e o
acesso ao microfone em segundo plano é restrito.

## Decisão

`RecordingService` em primeiro plano, com `foregroundServiceType="microphone"`,
notificação persistente com pausar/retomar/parar/marcador.

## Consequências positivas

- Gravação sobrevive à tela bloqueada e ao app em segundo plano.
- Controles acessíveis pela notificação, sem abrir o app.
- Alinhado às exigências do Android 14+.

## Consequências negativas (o preço)

- Notificação permanente enquanto grava (custo de UX, mas também visibilidade).
- O serviço precisa tratar foco de áudio, ligações e morte do processo.
- Permissões `FOREGROUND_SERVICE_MICROPHONE` e `POST_NOTIFICATIONS`.

## Contexto de avaliação

Sem o serviço, a gravação morreria ao apagar a tela — inaceitável para o caso
de uso principal (aula, entrevista).
