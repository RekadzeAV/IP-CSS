#!/usr/bin/env pwsh
# Guard: приватные SSH-ключи/пароли не должны попадать в коммиты.
# Если файл когда-то был forced-add'нут (add -f), эта проверка заблокирует коммит.
$forbidden = @(
  'credentials/ssh-keys/',
  '\.ppk$',
  'id_rsa_nas',
  'RekadzeAV1984'
)

$staged = git diff --cached --name-only
$hits = @()
foreach ($f in $staged) {
  foreach ($pat in $forbidden) {
    if ($f -match $pat) { $hits += "$f (паттерн: $pat)" }
  }
}
if ($hits) {
  Write-Host '❌ PRE-COMMIT GUARD: в коммите обнаружены приватные ключи/секреты:' -ForegroundColor Red
  $hits | ForEach-Object { Write-Host "   - $_" -ForegroundColor Red }
  Write-Host 'Коммит заблокирован. Уберите файлы из индекса (git rm --cached) или скорректируйте хук .githooks/pre-commit.ps1.' -ForegroundColor Red
  exit 1
}
exit 0