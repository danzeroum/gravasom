# Diagnóstico do canal de gravação

Data: 2026-10-07

## Sintoma

A ferramenta `write` responde "Wrote file successfully" mas, quando vários `write`
são enviados no mesmo turno, parte dos arquivos não aparece no disco.

## Evidência coletada

### a. Diretório raiz real

```
$ pwd
/home/danzeroum/work/products/audio/gravasom
```

Repo git existe, sem commits (`git log` → "does not have any commits yet").

### b. Sentinelas

- `SENTINELA_ABS.txt` — caminho absoluto, conteúdo `ABS-2026-10-07T00:50:00Z-abc123`
- `SENTINELA_REL.txt` — caminho relativo, conteúdo `REL-2026-10-07T00:50:00Z-xyz789`

Ambos escritos isoladamente (um por turno).

### c. Resultado

```
$ ls -la SENTINELA_*
-rw-r--r--. 1 danzeroum danzeroum 32 out  7 00:52 SENTINELA_ABS.txt
-rw-r--r--. 1 danzeroum danzeroum 32 out  7 00:52 SENTINELA_REL.txt

$ cat SENTINELA_ABS.txt
ABS-2026-10-07T00:50:00Z-abc123
$ cat SENTINELA_REL.txt
REL-2026-10-07T00:50:00Z-xyz789
```

Os dois sentinelas persistiriam e com o conteúdo exato. `write` acerta a raiz tanto
por caminho absoluto quanto relativo.

### d. Histórico suspeito

```
$ history | grep -E 'rm |git clean|git checkout|mv '
(nenhuma saída)
```

Nenhum comando apagou ou recriou diretórios. A perda não veio do shell.

### e. Conclusão

**Causa: WRITE_QUEBRADO em modo de lote.**

`write` funciona quando é o único bloco do turno. Quando há vários `write` no mesmo
turno, apenas alguns persistem; os demais são perdidos em silêncio, ainda assim
reportando sucesso.

Prova direta: no mesmo turno em que escrevi `gradle/libs.versions.toml`,
`build.gradle.kts` raiz, `build-logic/`, `core/`, `feature/record`, `feature/library`,
`feature/player` e `config/`, todos reportaram sucesso, mas nenhum existe:

```
$ ls gradle/ build-logic/ core/ config/ app/src/
ls: não foi possível acessar 'gradle/': Arquivo ou diretório inexistente
ls: não foi possível acessar 'build-logic/': Arquivo ou diretório inexistente
ls: não foi possível acessar 'core/': Arquivo ou diretório inexistente
ls: não foi possível acessar 'config/': Arquivo ou diretório inexistente
ls: não foi possível acessar 'app/src/': Arquivo ou diretório inexistente
```

## Canal adotado

1. **Um arquivo por turno**, com `write` (isolado), seguido imediatamente de
   verificação por `bash`: `test -s && wc -c && sha256sum`.
2. Cada arquivo verificado entra em `evidencias/manifesto.sha256`.
3. Ao iniciar cada fase, `sha256sum -c evidencias/manifesto.sha256`. Falha → parada
   nomeada `ARQUIVOS_SUMIRAM`.
4. Se o canal isolado também perder arquivo → parada nomeada `CANAL_INSTAVEL` e
   migração para `bash` + heredoc com delimitador único.
5. Commit ao fim de cada fase (Conventional Commits) para dar caminho de volta.
