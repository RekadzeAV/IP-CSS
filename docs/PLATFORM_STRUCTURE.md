# Структура платформ проекта IP-CSS

**Дата создания:** 26 January 2026
**Версия:** Alfa-0.0.1

## Фактический статус платформ (source of truth)

Статус ниже синхронизирован с `settings.gradle.kts`, Gradle-модулями и текущими CI workflow.

### Поддерживаются и имеют кодовые модули

1. **Android client**
- **Директория:** `android/`
- **Gradle-модуль:** `:android:app`
- **Статус:** implemented

2. **Desktop client x86_64**
- **Директория:** `platforms/client-desktop-x86_64/`
- **Gradle-модуль:** `:platforms:client-desktop-x86_64:app`
- **Статус:** implemented

3. **Desktop client ARM**
- **Директория:** `platforms/client-desktop-arm/`
- **Gradle-модуль:** `:platforms:client-desktop-arm:app`
- **Статус:** implemented

4. **Server API (backend)**
- **Директория:** `server/api/`
- **Gradle-модуль:** `:server:api`
- **Статус:** implemented

5. **Server Web UI (frontend)**
- **Директория:** `server/web/`
- **Тип:** Node.js/Next.js проект (не Gradle-модуль)
- **Статус:** implemented

6. **NAS packaging**
- **Директории:** `platforms/nas-x86_64/nas-build/`, `platforms/nas-arm/nas-build/`
- **Gradle-модули:** `:platforms:nas-x86_64:build`, `:platforms:nas-arm:build`
- **Статус:** implemented

### Частично реализованы / плановые

1. **iOS/macOS client**
- **Директория:** `platforms/client-ios/`
- **Статус:** planned (только документация, без app-модуля в репозитории)

2. **SBC ARM runtime platform**
- **Директория:** `platforms/sbc-arm/`
- **Статус:** planned (нет отдельного runtime-модуля, используется общий `server/api` + `server/web`)

3. **Server x86_64 runtime platform**
- **Директория:** `platforms/server-x86_64/`
- **Статус:** planned (нет отдельного runtime-модуля, используется общий `server/api` + `server/web`)

## Готовность target-ов (machine-checkable критерии)

Target считается **поддерживаемым**, если выполнены все условия:
- директория платформы присутствует в репозитории;
- есть подключенный модуль в `settings.gradle.kts` **или** явный workflow/скрипт сборки для платформы;
- есть CI-задача, которая проверяет сборку для target;
- платформа отражена в этом документе со статусом `implemented`.

Если хотя бы одно условие не выполняется, target помечается как `planned`.

## Структура веток Git (рекомендация)

### Основные ветки:
- `main` / `master` - основная стабильная ветка

> **⚠️ Обновлено 15.09.2026:** платформо-ориентированная модель Git-веток (`develop/platform-*`, `test/platform-*`, `dev/*`, `test/<платформа>`) упразднена — ветки удалены из репозитория (бэкап: `archive/git-branches-backup-2026-09-15/`). Действует trunk-based модель: `main` + короткоживущие `feature/*`/`chore/*`/`refactor/*` (см. [CONTRIBUTING.md](../CONTRIBUTING.md)). Раздел ниже — историческая справка.

### Ветки для каждой платформы (историческая модель, упразднена 15.09.2026):
- `develop/platform-sbc-arm` → `test/platform-sbc-arm`
- `develop/platform-server-x86_64` → `test/platform-server-x86_64`
- `develop/platform-nas-arm` → `test/platform-nas-arm`
- `develop/platform-nas-x86_64` → `test/platform-nas-x86_64`
- `develop/platform-client-desktop-x86_64` → `test/platform-client-desktop-x86_64`
- `develop/platform-client-desktop-arm` → `test/platform-client-desktop-arm`
- `develop/platform-client-android` → `test/platform-client-android`
- `develop/platform-client-ios` → `test/platform-client-ios`

## Общие модули

Следующие модули используются активными платформами:
- `shared/` - Kotlin Multiplatform модуль с общей бизнес-логикой
- `core/common`, `core/network` - базовые и сетевые абстракции
- `native/` - нативные C++ библиотеки

Примечание: `core/license` присутствует в репозитории, но находится в режиме ограниченной интеграции и не является обязательным dependency для всех сборочных контуров.



