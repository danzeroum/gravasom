# Ferramentas que geram os tokens e os ícones

Os arquivos de `docs/design/` que têm números (`tokens.json`, as tabelas de contraste em `identidade.md` e os SVGs de
`icone/`) saem destes scripts. Para mudar uma cor, mude o script e gere de novo; não edite o JSON à mão.

```bash
cd docs/design/ferramentas
npm i --no-save @material/material-color-utilities@0.3.0
node gen.mjs                          # cores M3 + semânticos + contraste + daltonismo (colors.json, pairs.json, cvd.json, icon.json)
node tema_mixlirous.mjs               # paleta opcional Mixlirous (mixlirous.json, pairs_mixlirous.json, cvd_mixlirous.json)
node build.mjs ../tokens.json         # monta tokens.json; falha se algum par não passar no AA
python3 icons.py ../icone             # SVGs do ícone (lê icon.json)
```

`build.mjs` também escreve `contraste.md` e `daltonismo.md` aqui; as duas tabelas são coladas nas seções 12 e 13 de
`identidade.md`. Os `.json` intermediários e `node_modules/` não vão para o git (ver `.gitignore` desta pasta).
