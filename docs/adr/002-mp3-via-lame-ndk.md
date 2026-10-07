# ADR 002 — MP3 via LAME no NDK, atrás de feature flag

Status: aceito (implementação diferida)

## Contexto

O Android não codifica MP3 nativamente. Os caminhos possíveis são: (a) LAME via
NDK/JNI, (b) biblioteca Java pura, (c) gravar em WAV e converter para MP3 só ao
exportar.

## Decisão

MP3 via LAME no NDK/JNI, atrás de uma feature flag. O caminho do PCM (AudioRecord)
é o mesmo do WAV, então o MP3 reaproveita o pipeline existente.

## Consequências positivas

- Um único pipeline PCM serve WAV e MP3; menos código duplicado.
- Compatibilidade universal do MP3 quando o usuário precisa.

## Consequências negativas (o preço)

- Dependência nativa: +2–3 MB no APK, build mais lento, NDK obrigatório no CI.
- Licença LGPL do LAME exige atenção jurídica.
- Mais CPU e bateria durante a gravação.

## Mitigação

- A feature flag desliga o MP3 por padrão; o usuário opta em Ajustes.
- O NDK não é obrigatório para M4A e WAV, que não usam código nativo.
- O bridge JNI mantém a superfície estável; trocar de encoder não toca a UI.

## Contexto de avaliação

A alternativa (c) — gravar em WAV e converter ao exportar — é mais barata, mas
não grava MP3 em tempo real. Fica registrada como opção de degradação caso a
licença do LAME inviabilize a distribuição.
