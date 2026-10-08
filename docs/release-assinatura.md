# Assinatura do APK de release

O artefato `gravasom-release-apk` só é gerado **assinado**. A chave nunca entra no repositório: a CI lê quatro *secrets*.

## Uma vez só, na sua máquina

```bash
keytool -genkeypair -v -keystore gravasom-release.jks -alias gravasom -keyalg RSA -keysize 4096 -validity 10000
# guarde o .jks e as senhas num cofre de senhas. Perder a chave = não poder atualizar o app já instalado.
base64 -w0 gravasom-release.jks   # (macOS: base64 -i gravasom-release.jks)
```

## Secrets (GitHub → Settings → Secrets and variables → Actions)

| Secret | Conteúdo |
| --- | --- |
| `GRAVASOM_KEYSTORE_BASE64` | saída do `base64` acima |
| `GRAVASOM_KEYSTORE_PASSWORD` | senha do keystore |
| `GRAVASOM_KEY_ALIAS` | `gravasom` (ou o alias escolhido) |
| `GRAVASOM_KEY_PASSWORD` | senha da chave |

## O que a CI faz (`.github/scripts/release-apk.sh`)

1. Falta algum secret → o job **falha** com mensagem clara e **não gera APK** (forks e PRs de forks não recebem secrets: lá o job falha de propósito).
2. Decodifica o keystore para um arquivo temporário (apagado ao final) e roda `assembleRelease` com R8.
3. `apksigner verify --print-certs`; registra bytes, SHA-256 do APK e SHA-256 do certificado em `evidencias-ci/apk-release-info.txt`.
4. Falha se o APK passar de **25 000 000 bytes** (orçamento só do release; o APK de debug está isento).

## Segurança

- Sem keystore no repositório; senhas só em variáveis de ambiente do job.
- O APK de debug continua usando a chave de debug pública (`config/debug.keystore`) e **não é um build de produção**.
- Trocar de chave exige novo `applicationId` ou desinstalar o app (o Android recusa atualização com assinatura diferente).
