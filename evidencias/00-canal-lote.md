# Prova do canal de gravação em lote (bash + heredoc)

Data: 2026-10-07

## Resultado: APROVADO

O canal `bash` + heredoc com delimitador único entre aspas simples persiste em lote,
nunca paralelo. Uma chamada gravou 5 arquivos; a seguinte os verificou por sha256.

## Lote pequeno (5 arquivos, 1 chamada)

```
$ ls -la tmp/lote/
-rw-r--r--. 18 a.txt
-rw-r--r--. 59 b.kt
-rw-r--r--. 29 c.txt
-rw-r--r--. 11 d.txt
-rw-r--r--. 11 e.txt
```

### Caso `$` e `${}` (b.kt)

Conteúdo gravado (literal, sem expansão):

```
val preco = "R$ 42,00"
val total = "${preco} com desconto"
```

Hash conferido:

```
656f8b44ff82f6e4b137501546929ddee822e3b2a3b93beb7919b458de243dce  b.kt
```

### Caso linha "EOF" no conteúdo (c.txt)

Conteúdo gravado:

```
linha antes
EOF
linha depois
```

Hash conferido:

```
2b70ad3eb8cdfc03d97518c9d82cf2b622c52d2bfd884d96c5c2326b5963420c  c.txt
```

Ambos batem com o esperado. O delimitador único `FIM_<NOME>` protege contra
linhas iguais a "EOF" dentro do conteúdo.

## Teto de tamanho (1 arquivo de 72 KB)

```
$ wc -c tmp/lote/grande.txt
72000 tmp/lote/grande.txt
$ sha256sum tmp/lote/grande.txt
2cca59f3bcff85f47fd8d3bc77d35fe5fd6c19674c2fe6e5710febb25f92b371
```

72 KB gravados numa chamada. **Teto seguro adotado: 50 KB por lote** (70% do
maior tamanho que passou). Lotes acima disso são divididos em dois, nunca truncados.

## Regras operacionais

1. Gravação **sequencial**, nunca paralela (o diagnóstico anterior mostrou perda em
   gravações paralelas no mesmo turno).
2. Um lote = 5 a 20 arquivos coesos, até 50 KB.
3. Delimitador único entre aspas simples por arquivo (`<<'FIM_<NOME>'`).
4. Ao fim do lote: `test -s` de cada arquivo e `sha256sum >> evidencias/manifesto.sha256`.
5. A chamada seguinte: `sha256sum -c evidencias/manifesto.sha256` e o gate leve do lote.
6. Commit por lote verde.
