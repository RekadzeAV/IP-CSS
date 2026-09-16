# Локальная релизная сборка: NAS пакеты

**Тип выпуска:** NAS Release Packages  
**Gradle задачи:** `buildNasPackage*` / `buildNasPackages`

## Назначение

Собрать локальные пакеты для NAS-платформ (Synology/QNAP/Asustor/TrueNAS) на основе серверного релиза.

## Предварительные условия

- Успешная сборка `:server:api:assemble` (или `:server:api:build`)
- Наличие `tar` и инструментов упаковки для целевой платформы
- Актуальные скрипты упаковки:
  - Linux/macOS: `scripts/build-nas-package.sh`
  - Windows: `scripts/build-nas-package.ps1`

## Команды проверки

```bash
./gradlew buildNasPackageSynologyX86
./gradlew buildNasPackageSynologyArm
./gradlew buildNasPackageQnapX86
./gradlew buildNasPackageQnapArm
./gradlew buildNasPackageAsustorX86
./gradlew buildNasPackageAsustorArm
./gradlew buildNasPackageTruenas
```

Для Windows задачи автоматически используют PowerShell-скрипт упаковки.

## Контрактная валидация (обязательно перед сборкой)

- Контракт артефактов: `config/nas-artifact-contract.json`
- Контракт runtime env: `config/nas-runtime-env-contract.json`
- Локальная проверка (Windows/PowerShell):

```powershell
./scripts/ci/validate-nas-contracts.ps1 -Version Alfa-0.1.1 -Packages "synology,qnap,asustor,truenas" -Architectures "x86_64,arm64"
```

- CI проверка (Linux/GitHub Actions):

```bash
VERSION=Alfa-0.1.1 PACKAGES=synology,qnap,asustor,truenas ARCHITECTURES=x86_64,arm64 ./scripts/ci/validate-nas-contracts.sh
```

Сборка должна останавливаться до `build-nas-package.*`, если контракты нарушены.

## Критерии готовности

- Каждая задача завершается с `BUILD SUCCESSFUL`
- Формируется пакет нужного формата (`.spk`, `.qpkg`, и др.)

## Текущие блокеры

На 27.03.2026 критичных блокеров нет: задачи NAS проходят локально на Windows.

Проверенные статусы:

- `buildNasPackageSynologyX86` — GREEN (`.spk`)
- `buildNasPackageSynologyArm` — GREEN (`.spk`)
- `buildNasPackageQnapX86` — GREEN (`.qpkg`)
- `buildNasPackageQnapArm` — GREEN (`.qpkg`)
- `buildNasPackageAsustorX86` — GREEN (`.apk`)
- `buildNasPackageAsustorArm` — GREEN (`.apk`)
- `buildNasPackageTruenas` — GREEN (папка `build/truenas-<version>`)

## Минимальный план исправления

1. Поддерживать паритет `build-nas-package.sh` и `build-nas-package.ps1`
2. Периодически прогонять `buildNasPackages` как smoke-check
3. Перед релизом валидировать установку пакетов на целевых NAS
