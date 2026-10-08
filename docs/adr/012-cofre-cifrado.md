# ADR 012 — Cofre cifrado (Tink, AES-256-GCM em fluxo)

Status: aceito

## Contexto

O relatório da entrega instalável listou como lacuna: "Cofre criptografado (Tink) não está ligado; 'Manter só neste app' apenas evita a cópia
pública". A opção estava com o rótulo certo para o que fazia (não publicar), mas o arquivo continuava em claro. Voz é dado pessoal (LGPD):
dado sensível pede cifra em repouso (Segurança por Design).

## Decisão

1. A opção **"Cofre cifrado"** (Ajustes) passa a **cifrar de verdade**. Gravação nova com o cofre ligado:
   - é cifrada com **Tink `StreamingAead` AES256_GCM_HKDF_4KB** (por arquivo, em fluxo: não carrega o áudio inteiro na memória; funciona para gravações de horas);
   - fica em `files/vault/<nome>.<ext>.gvs`; o arquivo **não abre como áudio** (teste: `MediaExtractor` não abre e o byte 4 não é `ftyp`);
   - **não** recebe cópia pública (sem MediaStore) e o arquivo em claro é apagado depois da cifra;
   - aparece na lista com o selo "No cofre".
2. A chave (keyset Tink) é gerada na primeira vez que o cofre é usado e guardada cifrada por uma **chave-mestra do Android Keystore**
   (`android-keystore://gravasom_vault_master`). Nada sai do aparelho e a chave não tem cópia (`allowBackup=false`).
3. **Tocar**: o arquivo é decifrado para `cache/vault-play/` (cache **privado** do app, nenhum outro app lê) e tocado pelo Media3.
   **Compartilhar**: decifra para `cache/vault-share/<título>.<ext>` e entrega por `content://` (FileProvider), nunca pelo caminho bruto.
   **Salvar em…**: decifra direto para o destino escolhido (sem arquivo temporário em claro).
   As cópias em claro do cache são apagadas ao abrir o app e ao excluir a gravação.
4. **Desligar o cofre não perde nada**: gravações que já estão no cofre continuam cifradas e tocáveis (o "estar no cofre" é do arquivo, não da preferência);
   as novas voltam a ser normais (com cópia pública).
5. **Falha de cifra nunca vaza**: se a cifra falhar (ex.: Keystore indisponível), o arquivo em claro permanece **só no app, sem cópia pública**.
6. Autenticação: cada segmento de 4 KB é autenticado (GCM); bytes alterados fazem a leitura falhar com `IOException` e não entregam áudio parcial.

## Alternativas descartadas

- *DataSource do Media3 que decifra sob demanda (seekable)*: evitaria o claro no cache, mas é bem mais código e risco; fica como evolução. O claro
  no cache privado é um **risco residual aceito e documentado** (aparelho com root ou backup do cache).
- *`EncryptedFile` (Jetpack Security)*: biblioteca em descontinuação; Tink é a base dela e já estava no projeto.
- *Cifra por gravação com senha do usuário*: pede senha para tocar; fora do escopo e piora o tempo até ouvir (H2).

## Consequências

- Importar áudio de outro app também vai para o cofre quando ele está ligado.
- Edição/exportação (`audio-engine`) ainda não é exposta na UI; quando for, deverá ler via `VaultStore.playable`.
- Perder o Keystore (restaurar o aparelho, trocar a tela de bloqueio em alguns fabricantes) torna as gravações do cofre **ilegíveis por desenho**.
  A tela de Ajustes avisa na descrição; recuperação não existe.
- Verificação: `VaultCryptoTest` (JVM, 8 testes), `CofreTest` (emulador). Validação com o Keystore em hardware de um aparelho real: `docs/teste-aparelho-real.md`.
