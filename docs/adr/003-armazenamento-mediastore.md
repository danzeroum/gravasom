# ADR 003 — Armazenamento: MediaStore público + cofre privado opcional

Status: aceito

## Contexto

Áudio gravado é dado pessoal e pode ser biométrico (LGPD). O app precisa de dois
comportamentos: gravações públicas (compartilháveis, visíveis em outros apps) e
privadas (só no app, criptografadas).

## Decisão

- Padrão: `Music/Recordings` via MediaStore (sem permissão de armazenamento na API 29+).
- Opção "Gravações privadas": `filesDir` com criptografia AES-256-GCM via Tink.

## Consequências positivas

- Sem `WRITE_EXTERNAL_STORAGE` no manifesto para o caminho público.
- O cofre privado não aparece em backups nem em outros apps.
- O usuário escolhe o nível de exposição.

## Consequências negativas (o preço)

- Dois caminhos de armazenamento e dois de leitura; mais casos de teste.
- O cofre privado exige desbloqueio/chave e não é acessível por players externos
  sem exportação explícita.

## Contexto de avaliação

Privilégio mínimo e minimização de dados pesam mais que a simplicidade de um
único caminho.
