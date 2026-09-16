# Release Handoff: Synology SPK (Alfa-0.1.1)

Краткий handoff-документ для релиза SPK-пакетов Synology.

## Готовые артефакты

- `build/ip-css-Alfa-0.1.1-synology-x86_64.spk`
  - Size: `51,787,881` bytes
  - SHA256: `613468BB35B69C1946ACBF3A1A651326FC7BA60CFA4E468066DCD3348BAB4F29`
- `build/ip-css-Alfa-0.1.1-synology-arm64.spk`
  - Size: `51,793,142` bytes
  - SHA256: `E9F3CDEE0E6BACE88DA5AF16BE53AB5F1036CB5190BA74546A69EDC2BE82C9F4`

## Что было проверено локально

- Стабилизация сборки API:
  - `./gradlew.bat :server:api:compileKotlin --no-daemon`
  - `./gradlew.bat :server:api:assemble --no-daemon`
- Сборка пакетов:
  - `powershell -ExecutionPolicy Bypass -File .\scripts\build-nas-package.ps1 -PackageType synology -Arch x86_64`
  - `powershell -ExecutionPolicy Bypass -File .\scripts\build-nas-package.ps1 -PackageType synology -Arch arm64`
- Базовая проверка структуры `.spk`:
  - внутри есть `INFO`, `package.tgz`, `icons/*`.

## Go / No-Go перед публикацией

Go, если выполнены все пункты:

- [ ] На целевом NAS (x86_64/arm64) пакет устанавливается вручную через DSM без ошибок.
- [ ] `synopkg status ip-css` показывает `running`.
- [ ] `curl -fsS http://127.0.0.1:8081/api/v1/health` возвращает успешный ответ.
- [ ] В `ip-css.log` нет фатальных ошибок старта.
- [ ] Web endpoint на `:8080` отвечает (200/302, не 5xx).

No-Go, если хотя бы один пункт не выполнен:

- [ ] Отсутствует успешный запуск сервиса после установки.
- [ ] API health-check не проходит.
- [ ] В логах есть критические ошибки инициализации.

## Связанные runbook'и

- `docs/DSM_SMOKE_AND_ROLLBACK.md` — smoke/rollback/reinstall команды для ручного DSM прогона.
