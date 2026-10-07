# C4 — Nível 1: Contexto

O Gravasom é um app Android que grava áudio pelo microfone e o mantém no aparelho.
Nada sai sem ação explícita do usuário.

```mermaid
C4Context
    title Contexto do Gravasom
    Person(usuario, "Usuário", "Grava aula, entrevista ou ideia musical")
    System(app, "Gravasom", "Grava, organiza e reproduz áudio no aparelho")
    System_Ext(mic, "Microfone / Bluetooth", "Fonte de áudio (plataforma Android)")
    System_Ext(players, "Players externos", "Recebem arquivos compartilhados")
    System_Ext(play, "Google Play", "Distribuição do app")
    Rel(usuario, app, "Grava, ouve, organiza")
    Rel(app, mic, "Captura áudio", "AudioRecord/MediaRecorder")
    Rel(usuario, players, "Compartilha gravação", "FileProvider + Intent")
    Rel(app, play, "Publica release", "Gradle Play Publisher")
```

## Fronteiras

- O app não acessa a rede no fluxo principal (disponibilidade 100% offline).
- Telemetria é opt-in e não contém áudio, nome de arquivo nem localização.
