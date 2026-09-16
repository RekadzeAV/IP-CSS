# Консолидированный отчет о статусе сборки проекта IP-CSS

**Версия проекта:** Alfa-0.1.1
**Дата последнего обновления:** 27 April 2026
**Статус:** 📦 Исторический baseline (January 2026), не release gate source of truth

> **📚 Связанные документы:**
> - [PROJECT_STATUS.md](../status/PROJECT_STATUS.md) - Общий статус проекта (актуальный source of truth)
> - [PROJECT_STATUS_PHASES.md](../status/PROJECT_STATUS_PHASES.md) - Детализация фаз/этапов
> - [MODULE_STATUS_BASELINE_2026-04-23.md](../status/MODULE_STATUS_BASELINE_2026-04-23.md) - Единый baseline модулей
> - [VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md](VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md) - Runtime/release policy для video e2e
> - [DEPLOYMENT_GUIDE.md](../../archive/docs/deployment/DEPLOYMENT_GUIDE.md) - Руководство по развертыванию
> - [BUILD_ORGANIZATION.md](../../archive/docs/guides/BUILD_ORGANIZATION.md) - Организация сборки
>
> **⚠️ Контекст:** этот документ фиксирует состояние контура сборки на январь 2026 и используется как исторический/диагностический отчет.
> Актуальный релизный статус берите из `docs/reports/PHASE1_AUTO_EXECUTION_STATUS_2026-04-26.md`,
> `docs/reports/NAS_GO_NO_GO_AGGREGATOR_2026-04-26.md` и `docs/planning/RELEASE_GO_NO_GO_CHECKLIST.md`.
>
> **Синхронизация (2026-04-27):** не использовать этот отчет как release gate source of truth; актуальное решение фиксируется в `docs/reports/PHASE1_AUTO_EXECUTION_STATUS_2026-04-26.md`, `docs/reports/NAS_GO_NO_GO_AGGREGATOR_2026-04-26.md`, `docs/planning/RELEASE_GO_NO_GO_CHECKLIST.md`.

---

## 📊 Общий статус сборки

| Компонент | Статус | Прогресс | Примечания |
|-----------|--------|----------|------------|
| Инфраструктура | ✅ Готово | 100% | Gradle, структура проекта |
| Конфигурация | ✅ Готово | 100% | Все конфигурационные файлы готовы |
| Компиляция | ⚠️ Частично | 60% | Основные модули компилируются |
| API Сервер | ⚠️ Требуется сборка | 0% | Зависит от исправления ошибок |
| Веб-интерфейс | ⚠️ Требуется проверка | 0% | Требуется сборка |
| SPK пакет | ⚠️ Базовая структура | 30% | Структура создана, требуется артефакты |

**Общая готовность к сборке:** ~50%

---

## ✅ Выполненные работы

### 1. Инфраструктура и конфигурация

- ✅ Структура проекта Synology создана
- ✅ Конфигурационные файлы INFO готовы
- ✅ Скрипты сборки обновлены (bash и PowerShell)
- ✅ Иконки пакета созданы
- ✅ Gradle Wrapper настроен
- ✅ Версия SQLDelight обновлена до 2.1.0

### 2. Исправления компиляции

#### core:common - Ошибки expected/actual функций ✅

**Проблема:** Использование `actual override` для функций, реализующих интерфейсы

**Решение:** Убран `actual`, оставлен только `override`

**Результат:** ✅ `core:common:compileKotlinDesktop` компилируется успешно

**Исправленные файлы:**
- `PasswordEncryption.jvm.kt` - 3 функции
- `LocalDataEncryption.jvm.kt` - 5 функций
- `MobileSecurityLogger.jvm.kt` - 1 функция

#### shared - SQLDelight синтаксические ошибки ✅

**Проблемы:**
- Отсутствовали двоеточия после имен запросов
- Дублирование `OR REPLACE OR REPLACE`
- Неправильный формат SQL запросов

**Решение:**
- ✅ Добавлены двоеточия после всех имен запросов
- ✅ Убрано дублирование
- ✅ Исправлен формат SQL запросов
- ✅ Заменены `ON CONFLICT DO UPDATE` на `INSERT OR REPLACE` для совместимости с SQLite 3.18

**Исправленные запросы:**
- ✅ `insertCamera`
- ✅ `insertRecording`
- ✅ `insertEvent`
- ✅ `insertUser`
- ✅ `insertSetting`
- ✅ `insertNotification`

**Результат:** ✅ SQLDelight генерация работает

### 3. Исправления зависимостей

- ✅ Обновлена версия SQLDelight с 2.0.3 на 2.1.0 (версия 2.0.3 недоступна в Maven Central)
- ✅ Исправлены зависимости Ktor (удален недоступный `ktor-client-http-cookies`)
- ✅ Добавлена зависимость `kotlin-logging` для desktopMain в core/common
- ✅ Проверена доступность репозиториев Maven Central и Gradle Plugin Portal

### 4. Подготовка SPK пакета

- ✅ Создана базовая структура SPK пакета
- ✅ Создан INFO файл
- ✅ Созданы директории (bin, lib, web, conf)
- ✅ Созданы заглушки для скриптов (start.sh, stop.sh)

---

## ⚠️ Текущие проблемы

### 1. core:network - Ошибки компиляции

**Проблемы:**
- `VideoDecoderImpl.kt`: `Unresolved reference 'sws_freeContext'`
- `VideoDecoderImpl.kt`: `Cannot infer type for this parameter`
- `NativeRtspClient.jvm.kt`: Проблемы с `actual typealias`
- `CertificatePinner.jvm.kt`: Ошибка `Declaration must be marked with 'actual'`

**Статус:** ⚠️ Модуль можно исключить из сборки для тестового пакета

**Временное решение:** Используется флаг `-x :core:network:compileKotlinDesktop` для пропуска проблемного модуля

**Требуется:** Исправление зависимостей или структуры кода

### 2. API сервер не собран

**Причина:** Зависит от core:network, который не компилируется

**Статус:** Ожидает исправления ошибок компиляции

**Требуется:**
1. Исправить ошибки в core:network
2. Собрать API сервер: `.\gradlew.bat :server:api:build`

### 3. Веб-интерфейс

**Статус:** Требуется проверка расположения и сборка

**Требуется:**
1. Проверить расположение веб-интерфейса
2. Выполнить `npm install` и `npm run build`

### 4. PowerShell скрипт сборки нативных библиотек

**Проблема:** Синтаксическая ошибка в `build-video-processing-lib.ps1`

**Статус:** Задача отключена для тестовой сборки (`-x :core:network:buildNativeVideoProcessingForCurrentPlatform`)

---

## 📋 Следующие шаги

### Приоритет 1: Исправить ошибки компиляции в core:network

1. Проверить зависимости для `sws_freeContext` (FFmpeg)
2. Исправить type inference проблемы
3. Исправить структуру expect/actual для NativeRtspClient
4. Исправить проблему с CertificatePinner

### Приоритет 2: Собрать API сервер

```powershell
# После исправления ошибок компиляции
.\gradlew.bat :server:api:build --no-daemon
```

### Приоритет 3: Собрать веб-интерфейс

```powershell
cd server\web
npm install
npm run build
```

### Приоритет 4: Создать финальный SPK пакет

```powershell
.\scripts\build-nas-package.ps1 -PackageType synology -Arch x86_64 -Version Alfa-0.1.1
```

---

## 📊 Детальная статистика

### Модули компиляции

| Модуль | Статус | Примечания |
|--------|--------|------------|
| core:common | ✅ Компилируется | Все ошибки исправлены |
| shared | ✅ Компилируется | SQLDelight синтаксис исправлен |
| core:network | ⚠️ Не компилируется | Требуется исправление |
| server:api | ⚠️ Не собран | Зависит от core:network |

### Зависимости

| Зависимость | Статус | Версия |
|-------------|--------|--------|
| SQLDelight | ✅ Обновлено | 2.1.0 |
| Ktor | ✅ Исправлено | 2.3.12 |
| Kotlin | ✅ Работает | 2.0.21 |
| Gradle | ✅ Установлено | 8.9 |

### Инфраструктура сборки

| Компонент | Статус |
|-----------|--------|
| Gradle Wrapper | ✅ Готов |
| Скрипты сборки | ✅ Готовы |
| Структура SPK | ✅ Создана |
| Конфигурация | ✅ Готова |

---

## 🚀 Альтернативные варианты сборки

### Вариант 1: Использовать WSL или Git Bash

```bash
# В WSL или Git Bash
./scripts/build-nas-package.sh synology x86_64 Alfa-0.1.1
```

### Вариант 2: Собрать вручную без API сервера

Если нужно создать пакет для тестирования структуры (без функциональности):

1. Создать пустой JAR файл (только для тестирования структуры пакета)
2. Запустить сборку пакета (веб-интерфейс можно пропустить)

### Вариант 3: Использовать Docker

```bash
# Собрать в Docker контейнере с Gradle
docker run --rm -v ${PWD}:/project -w /project gradle:8.4-jdk17 gradle :server:api:build
```

---

## 📝 Созданные файлы и отчеты

1. **DEPENDENCY_UPDATE_REPORT.md** - Детальный отчет об обновлении зависимостей
2. **BUILD_STATUS_CONSOLIDATED.md** - Этот файл (консолидированный отчет)
3. **Структура SPK пакета** - `build/synology-x86_64/`

---

## ✅ Выводы

1. ✅ **Основные ошибки компиляции исправлены** - core:common и shared компилируются успешно
2. ✅ **Зависимости обновлены** - SQLDelight 2.1.0, Ktor исправлен
3. ✅ **Инфраструктура готова** - Gradle, скрипты, структура SPK
4. ⚠️ **Требуется исправление core:network** - для завершения сборки API сервера
5. ⚠️ **Требуется сборка веб-интерфейса** - для полного SPK пакета

**Проект готов к сборке после исправления ошибок в core:network.**

---

## 📚 История изменений

- **28 января 2026:** Консолидированы все отчеты о сборке в один документ
- **28 января 2026:** Исправлены ошибки компиляции в core:common и shared
- **28 января 2026:** Обновлены зависимости (SQLDelight 2.1.0)
- **28 января 2026:** Создана базовая структура SPK пакета

---

**Последнее обновление:** 27 April 2026
**Версия документа:** 1.0
