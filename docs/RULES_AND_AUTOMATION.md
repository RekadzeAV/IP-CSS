# Правила и Автоматизация Проекта IP-CSS

**Версия документа:** 1.0.0  
**Дата создания:** 16 May 2026  
**Обязательность:** ✅ **ОБЯЗАТЕЛЬНОЕ СОБЛЮДЕНИЕ** для всех контрибьюторов

---

## 📋 Содержание

1. [Обзор](#-обзор)
2. [Код-стили и Форматирование](#-код-стили-и-форматирование)
3. [CI/CD Автоматизация](#-cicd-автоматизация)
4. [KMP (Kotlin Multiplatform) Правила](#-kmp-kotlin-multiplatform-правила)
5. [Правила Безопасности](#-правила-безопасности)
6. [Управление Документацией](#-управление-документацией)
7. [Git Workflow и Ветвление](#-git-workflow-и-ветвление)
8. [Labels (метки issues/PR)](#-labels-метки-issuespr)
9. [Тестирование](#-тестирование)
10. [Управление Зависимостями](#-управление-зависимостями)
11. [Обязательные Чеклисты](#-обязательные-чеклисты)
12. [Локальная Проверка](#-локальная-проверка)
13. [Наказания за Нарушения](#-наказания-за-нарушения)
14. [Ссылки на Исходники](#-ссылки-на-исходники)
15. [Правила Установки Зависимостей и Проверки Окружения](#-правила-установки-зависимостей-и-проверки-окружения)

---

## 🎯 Обзор

Этот документ содержит **полный список всех правил и автоматизаций**, применяемых в проекте IP-CSS. Все правила **обязательны к исполнению** для всех контрибьюторов, включая разработчиков, тестировщиков и техническую документацию.

### Цель Документа

- Единственный источник истины по всем правилам проекта
- Быстрая проверка соответствия перед коммитом/PR
- Локальная валидация до отправки в CI
- Понимание автоматизации и её требований

### Где Находятся Правила

| Категория | Файл/Директория | Описание |
|-----------|----------------|----------|
| Код-стили | `.editorconfig`, `.clang-format`, `.ktlint.yml` | Форматирование кода |
| CI/CD | `.github/workflows/` | Автоматизация сборки и тестов |
| KMP-проверки | `scripts/ci/` | Скрипты валидации KMP |
| Безопасность | `core/*/src/*/security/` | Certificate pinning, expect/actual |
| Документация | `scripts/manage-documentation.*` | Управление версиями документов |
| Git hooks | `scripts/setup-git-hooks.sh` | Pre-commit проверки |
| Тестирование | `scripts/ci/mvp-automated-acceptance.*` | Автотесты MVP |

---

## 🎨 Код-стили и Форматирование

### 1. Kotlin

**Файлы конфигурации:**
- `.editorconfig` - общие правила
- `.ktlint.yml` - ktlint правила
- `.clang-format` - для C++ (не Kotlin)

**Правила:**
```yaml
# editorconfig для Kotlin
[*.{kt,kts}]
indent_style = space
indent_size = 4
max_line_length = 120
charset = utf-8
end_of_line = lf
trim_trailing_whitespace = true
insert_final_newline = true
```

**Правила ktlint:**
```yaml
indent_size: 4
indent_style: space
end_of_line: lf
charset: utf-8
trim_trailing_whitespace: true
insert_final_newline: true
max_line_length: 120
disabled_rules:
  - no-wildcard-imports  # Разрешены wildcard-imports
```

**Инструменты:**
- ✅ **ktlint** - форматтер
- ✅ **Detekt** - статический анализ

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

**Проверка:**
```bash
# Локально
./gradlew ktlintFormat  # Автоматическое форматирование
./gradlew detekt        # Статический анализ

# CI: task `:core:common:detekt`, `:core:network:detekt`, `:shared:detekt`
```

---

### 2. C/C++

**Файл конфигурации:**
- `.clang-format` - clang-format правила

**Правила:**
```yaml
Language: Cpp
BasedOnStyle: LLVM
IndentWidth: 4
TabWidth: 4
UseTab: Never
ColumnLimit: 120
Standard: Cpp17
```

**Инструменты:**
- ✅ **clang-format** - форматтер

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

**Проверка:**
```bash
# Локально (Linux/macOS)
clang-format -i native/video-processing/src/*.cpp

# Windows (через PowerShell)
clang-format -i native/video-processing/src/*.cpp
```

---

### 3. JavaScript/TypeScript

**Файлы конфигурации:**
- `.editorconfig` - общие правила
- `.prettierrc.json` - Prettier правила
- `server/web/.eslintrc.*` - ESLint правила

**Правила:**
```yaml
# editorconfig для JS/TS
[*.{js,jsx,ts,tsx,json}]
indent_style = space
indent_size = 2
max_line_length = 120
```

**Инструменты:**
- ✅ **ESLint** - статический анализ
- ✅ **Prettier** - форматтер

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

**Проверка:**
```bash
cd server/web
npm run lint      # ESLint
npm run format    # Prettier
```

---

## 🤖 CI/CD Автоматизация

### GitHub Actions Workflows

**Директория:** `.github/workflows/`

#### 1. **ci.yml** - Основной CI пайплайн

**Триггеры:**
- `push` в `develop`, `feature/*`, `release/*`
- `pull_request` в `main`, `develop`

**Jobs (обязательные проверки):**

| Job | Описание | Обязательность |
|-----|----------|----------------|
| `docs-link-check-active-scope` | Проверка ссылок в Markdown | 🔴 **Блокирует merge** |
| `code-quality` | Detekt, ESLint, KMP gates | 🔴 **Блокирует merge** |
| `build-and-test` | Сборка и тесты | 🔴 **Блокирует merge** |
| `kmp-cross-compile` | Кросс-компиляция KMP | 🔴 **Блокирует merge** |
| `server-api-compose-integration` | Интеграционные тесты API | 🔴 **Блокирует merge** |
| `mvp-automated-acceptance` | Приемочное тестирование MVP | 🟡 **Рекомендуется** |

**Правила:**
- ✅ Все обязательные jobs должны быть `green`
- ✅ При смене документации — `docs-link-check-active-scope` должен пройти
- ✅ KMP-гаты должны пройти перед merge

---

#### 2. **build-native-libraries.yml**

**Описание:** Сборка нативных C++ библиотек для всех платформ

**Платформы:**
- Windows (x64, x86)
- Linux (x64, ARM64)
- macOS (x64, ARM64)
- Android (armeabi-v7a, arm64-v8a, x86, x86_64)
- iOS (arm64, x86_64)

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении `native/`

---

#### 3. **build-nas-packages.yml**

**Описание:** Сборка пакетов для NAS (Synology, QNAP)

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении NAS-специфичного кода

---

#### 4. **cd.yml**

**Описание:** Continuous Deployment

**Правила:**
- ✅ Деплой только из ветки `main`
- ✅ Требуется approval от maintainers
- ✅ Автоматический rollback при ошибках

---

#### 5. **codeql.yml**

**Описание:** Security анализ кода (CodeQL)

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

---

#### 6. **secret-scan.yml**

**Описание:** Сканирование на секреты (тесты, ключи, токены)

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

**Запрещено коммитить:**
- `*.pem`, `*.key`, `*.crt`, `*.p12`, `*.keystore`
- `secrets/`, `.env`, `.env.local`, `*.secret`
- `activation_codes.txt`, `license.xml`

---

#### 7. **dependency-verification.yml**

**Описание:** Проверка зависимостей на уязвимости

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

---

#### 8. **ffmpeg-tests.yml**

**Описание:** Тесты FFmpeg интеграции

**Обязательность:** 🟡 **Рекомендуется** при изменении `native/video-processing/`

---

#### 9. **nightly-live-integration.yml**

**Описание:** Ночная интеграция с реальными камерами

**Обязательность:** 🟡 **Опционально** (для maintainers)

---

#### 10. **postgres-finalization-gate.yml**

**Описание:** Gate для PostgreSQL finalization

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении миграций БД

---

### Custom GitHub Actions

**Директория:** `.github/actions/`

#### **setup-jdk-gradle/action.yml**

**Описание:** Установка JDK 17 + кэширование Gradle

**Использование:**
```yaml
- uses: ./.github/actions/setup-jdk-gradle
  with:
    gradle-cache-key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
    gradle-restore-keys: ${{ runner.os }}-gradle-
```

---

## 🧩 KMP (Kotlin Multiplatform) Правила

### 1. expect/actual

**Правила:**
- ✅ В `commonMain` **ЗАПРЕЩЕНЫ** платформенные API: `java.*`, `javax.*`, `android.*`
- ✅ Любая платформенная логика выносится через `expect/actual`
- ✅ Минимум **3 actual реализации** (Android, JVM/Desktop, iOS) для новых expect
- ✅ Не добавлять JVM/Android-only зависимости в `iosMain`/`nativeMain`

**Где зафиксировано:**
- `core/common/src/commonMain/` - expect объявления
- `core/common/src/{android,jvm,ios,native}Main/` - actual реализации
- `scripts/ci/check-security-expect-actual-signatures.py` - проверка

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

---

### 2. KMP-проверки (CI Gates)

**Директория:** `scripts/ci/`

#### **verify-kmp-phase1.py** (и `.ps1`, `.sh`)

**Описание:** Комплексная проверка KMP

**Режимы:**
```bash
# Полный режим (как в CI)
python scripts/ci/verify-kmp-phase1.py --ci-profile

# Строгий режим runtime matrix
python scripts/ci/verify-kmp-phase1.py --strict-runtime-matrix

# Быстрый режим без Gradle
python scripts/ci/verify-kmp-phase1.py --skip-gradle

# Windows PowerShell
.\scripts\ci\verify-kmp-phase1.ps1 -CiProfile
.\scripts\ci\verify-kmp-phase1.ps1 -StrictRuntimeMatrix
.\scripts\ci\verify-kmp-phase1.ps1 -SkipGradle

# Linux/macOS/WSL
./scripts/ci/verify-kmp-phase1.sh --ci-profile
./scripts/ci/verify-kmp-phase1.sh --strict-runtime-matrix
./scripts/ci/verify-kmp-phase1.sh --skip-gradle
```

**Проверки:**
- expect/actual сигнатуры
- Запрещенные импорты в commonMain
- Runtime matrix конфигурация
- Пустые `playlistUrls` в enabled local scenario = **ошибка**

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** перед PR

---

#### **check-commonmain-forbidden-imports.py**

**Описание:** Проверка запрещенных импортов в commonMain

**Запрещенные:**
- `java.*`
- `javax.*`
- `android.*`
- Любые платформенные API

**Проверка:**
```bash
python scripts/ci/check-commonmain-forbidden-imports.py .
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

---

#### **check-security-expect-actual-signatures.py**

**Описание:** Проверка сигнатур expect/actual для security модулей

**Требования:**
- Минимум 3 actual реализации (Android, JVM/Desktop, iOS)
- Сигнатуры должны совпадать

**Проверка:**
```bash
python scripts/ci/check-security-expect-actual-signatures.py .
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** для security модулей

---

#### **check-no-jvm-deps-in-native-source-sets.py**

**Описание:** Проверка отсутствия JVM-зависимостей в native source sets

**Проверка:**
```bash
python scripts/ci/check-no-jvm-deps-in-native-source-sets.py .
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

---

#### **validate-video-e2e-profile.py**

**Описание:** Валидация video E2E acceptance profile

**Проверка:**
```bash
python scripts/ci/validate-video-e2e-profile.py --root .
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении video E2E

---

#### **validate-video-runtime-matrix-config.py**

**Описание:** Валидация конфигурации runtime matrix

**Проверка:**
```bash
python scripts/ci/validate-video-runtime-matrix-config.py --root .
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении runtime matrix

---

### 3. Обязательные Gradle Задачи

**Где зафиксировано:** `build.gradle.kts` модулей

**Задачи:**
```bash
# Compile metadata (все платформы)
./gradlew :core:common:compileKotlinMetadata
./gradlew :core:network:compileKotlinMetadata
./gradlew :shared:compileKotlinMetadata

# Desktop тесты
./gradlew :core:common:desktopTest

# KMP sanity checks
./gradlew ciKmpLinuxSanity
./gradlew ciKmpMacosIosCompile
./gradlew ciKmpWindowsSanity
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** перед PR

---

## 🔒 Правила Безопасности

### 1. Certificate Pinning

**Где зафиксировано:**
- `core/network/src/commonMain/security/` - CertificatePinningConfig
- `core/network/src/{android,jvm,ios,native}Main/security/` - actual реализации
- `scripts/ci/check-security-expect-actual-signatures.py` - проверка

**Правила:**
- ✅ Принудительный HTTPS
- ✅ Certificate pinning из конфига
- ✅ CertificatePinningConfigLoader для всех платформ

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

---

### 2. Secret Management

**Где зафиксировано:**
- `.gitignore` - блокировка секретов
- `.github/workflows/secret-scan.yml` - сканирование

**Запрещено коммитить:**
```
*.pem, *.key, *.crt, *.p12, *.keystore
secrets/, .env, .env.local, *.secret
credentials.json, activation_codes.txt
license.xml
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

---

### 3. HTTPS Enforcement

**Где зафиксировано:**
- `server/api/` - редирект HTTP → HTTPS
- `core/*/src/*/networking/` - клиенты с HTTPS

**Правила:**
- ✅ Принудительный HTTPS на сервере
- ✅ HTTPS в клиентах (опция в конфигах)

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** для MVP

---

## 📚 Управление Документацией

### 1. Версионирование Документов

**Где зафиксировано:**
- `scripts/manage-documentation.ps1` (PowerShell)
- `scripts/manage-documentation.sh` (Linux/macOS)
- `scripts/manage-documentation.py` (Python)

**Правила:**
- ✅ Версия в формате `Alfa-0.0.x`
- ✅ Автоматическое увеличение версии при обновлении
- ✅ Архивация старых версий в `docs/archive/`
- ✅ Версия указана в header документа

**Команды:**
```bash
# Создание нового документа
.\scripts\manage-documentation.ps1 -Action create -Document "docs/NEW_FEATURE.md"

# Обновление документа
.\scripts\manage-documentation.ps1 -Action update -Document "docs/ARCHITECTURE.md"

# Слияние документов
.\scripts\manage-documentation.ps1 -Action merge -SourceDocuments @("docs/DOC1.md", "docs/DOC2.md") -OutputDocument "docs/MERGED.md"
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при работе с документацией

---

### 2. Обязательная Актуализация TIMELINE.md

**Где зафиксировано:**
- `docs/TIMELINE.md` - временная шкала проекта
- `CONTRIBUTING.md` - требования к TIMELINE.md

**Правила:**
- ✅ Обновление при каждом релизе
- ✅ Обновление при завершении задач
- ✅ Обновление при изменении планов
- ✅ Обновление при добавлении блокеров

**Чеклист:**
- [ ] Обновлена соответствующая секция TIMELINE.md
- [ ] Использованы правильные статусы (✅/🟡/⚠️/❌)
- [ ] Обновлена дата "Последнее обновление"
- [ ] Обновлен процент прогресса (если применимо)
- [ ] Информация согласована с CHANGELOG.md
- [ ] Информация согласована с CURRENT_STATUS.md

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** для значимых изменений

**Наказание:** PR может быть отклонен до обновления TIMELINE.md

---

### 3. PROJECT_STRUCTURE_AUTO.md

**Где зафиксировано:**
- `scripts/generate-project-structure.py` - генерация
- `.git/hooks/pre-commit` - проверка
- `CONTRIBUTING.md` - требования

**Правила:**
- ✅ Автоматическая генерация структуры проекта
- ✅ Pre-commit hook проверяет актуальность
- ✅ При изменении структуры — добавить в коммит

**Команда:**
```bash
python scripts/generate-project-structure.py
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении структуры проекта

---

### 4. Чеклист Документации

**Перед коммитом:**
- [ ] Использован скрипт управления документацией
- [ ] Версия проекта указана корректно (из `gradle.properties`)
- [ ] Документ размещен в правильном каталоге (`docs/` или корень)
- [ ] Обновлен `DOCUMENTATION_INDEX.md` (если добавлен новый документ)
- [ ] Обновлен `docs/README.md` (если изменена структура)

---

## 🔄 Git Workflow и Ветвление

### 1. Структура Веток

**Где зафиксировано:**
- `CONTRIBUTING.md` - workflow
- `.git/hooks/pre-commit` - проверки

**Ветки:**
```
main                    # единственная долгоживущая ветка (точка истины)
feature/*               # короткоживущие feature-ветки от main
chore/*, refactor/*     # служебные ветки от main
dependabot/*            # автоматические обновления зависимостей
```

> Обновлено 15.09.2026: платформенные ветки (`dev/*`, `test/*`, `develop/platform-*`, `test/platform-*`) упразднены и удалены (бэкап: `archive/git-branches-backup-2026-09-15/`).

**Правила:**
- ✅ Feature branch от ветки разработки (`dev/*`)
- ✅ Pull Request в ветку разработки (`dev/*`)
- ✅ Code review обязателен
- ✅ CI должен быть green перед merge

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

---

### 2. Git Hooks

**Где зафиксировано:**
- `scripts/setup-git-hooks.sh` - установка hooks
- `.git/hooks/pre-commit` - pre-commit проверка

**Установка:**
```bash
./scripts/setup-git-hooks.sh
```

**Проверки:**
- ✅ Обновление PROJECT_STRUCTURE_AUTO.md
- ✅ Запрет на секреты в staged files

**Обязательность:** ✅ **РЕКОМЕНДУЕТСЯ** (но CI проверит в любом случае)

---

### 3. Сообщения Коммитов

**Где зафиксировано:**
- `.gitmessage.txt` - шаблон

**Формат:**
```
тип(область): краткое описание

Детальное описание (если нужно)

Closes #123
```

**Типы:**
- `feat` - новая функция
- `fix` - исправление бага
- `docs` - документация
- `style` - форматирование
- `refactor` - рефакторинг
- `test` - тесты
- `chore` - инфраструктура

**Обязательность:** 🟡 **РЕКОМЕНДУЕТСЯ**

---

## 🏷️ Labels (метки issues/PR)

> **Актуализация 15.09.2026:** таксономия 50 меток применена через REST API. Авто-метки Dependabot (`dependencies`, `github_actions`, `java`, `javascript`, `python`) и шаблонов (`bug`, `enhancement`) **не переименовывать** — сломается авторазметка.

### Приоритеты (синхронизированы с `REMAINING_TASKS.md`)

| Метка | Значение |
|---|---|
| `priority/P0` | блокирует релиз 0.6.0-beta |
| `priority/P1` | до GO/NO-GO, программно доступно |
| `priority/P2` | внешние ресурсы (порты A1–A4) |
| `priority/P3` | пост-релиз (Этап 10) |

### Статусы

| Метка | Значение |
|---|---|
| `status/in-progress` | в работе |
| `status/blocked` | заблокировано внутренней задачей/зависимостью |
| `status/blocked-external` | заблокировано внешним ресурсом (камеры/macOS/Firebase/Linux) |
| `status/needs-review` | ожидает ревью |

### Типы / контуры

`security`, `tech-debt`, `refactoring`, `tests`, `ci-build`, `performance`, `docs`

### Модули

`server`, `core`, `shared`, `android`, `desktop`, `ios`, `nas`, `ai-agent`, `web-dashboard`

### Dependabot-волны (Этап V4, `PLAN_VULNERABILITIES.md`)

| Метка | Волна |
|---|---|
| `deps-gradle` / `deps-npm` / `deps-pip` | контур обновлений |
| `W1`…`W9` | волна разбора (W1 — закрыта; W2/W3/W4/W6/W7 — P1; W5/W8 — P3; W9 — после pip-lock) |

### Правила применения

- PR Dependabot: авто-метки ecosystem + метка волны + контур + приоритет (применено 15.09 к #11–#28).
- Приоритет — ровно одна метка `priority/*`; статус и контур — по необходимости.
- При мёрже волны обновлять статус в `PLAN_VULNERABILITIES.md` (этап V4) и снимать `status/*` с закрытых PR.

---

## 🧪 Тестирование

### 1. Обязательные Тесты

**Правила:**
- ✅ Unit тесты для бизнес-логики
- ✅ Integration тесты для API
- ✅ E2E тесты для критических путей
- ✅ KMP-тесты для всех платформ
- ✅ Покрытие тестами > 80% для критических модулей

**Где зафиксировано:**
- `scripts/ci/mvp-automated-acceptance.*` - автотесты MVP
- `core/*/src/*/test/` - тесты модулей

---

### 2. Автотесты MVP

**Где зафиксировано:**
- `scripts/ci/mvp-automated-acceptance.ps1` (PowerShell)
- `scripts/ci/mvp-automated-acceptance.sh` (Linux/macOS)

**Команды:**
```bash
# Полный режим
.\scripts\ci\mvp-automated-acceptance.ps1
bash scripts/ci/mvp-automated-acceptance.sh --generate-phase1-summary

# С генерацией summary
.\scripts\ci\mvp-automated-acceptance.ps1 -GeneratePhase1Summary
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** перед релизом MVP

---

### 3. Video E2E Gate

**Где зафиксировано:**
- `scripts/video-e2e-go-no-go.ps1`
- `scripts/ci/validate-video-e2e-profile.py`

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении video модулей

---

### 4. ONVIF Acceptance

**Где зафиксировано:**
- `scripts/run-phase1-onvif-acceptance.ps1`
- `scripts/run-phase1-onvif-gate.ps1`

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении ONVIF модулей

---

## 📦 Управление Зависимостями

### 1. Gradle

**Правила:**
- ✅ Gradle wrapper обязателен (без кастомных установок)
- ✅ `gradle-wrapper-validation.yml` - валидация wrapper
- ✅ Dependency review action - проверка уязвимостей

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

---

## 🛡️ Правила Установки Зависимостей и Проверки Окружения

### 1. Проверка Перед Установкой Модулей/Компонентов/Библиотек

**Где зафиксировано:**
- `scripts/check-dependency-conflicts.py` - проверка конфликтов
- `scripts/check-environment-requirements.py` - проверка окружения
- `CONTRIBUTING.md` - требования перед разработкой

**Правила:**

#### 🔴 **ПРАВИЛО #1: Проверка Установленных Версий Перед Установкой**

**Перед установкой любого модуля, компонента, библиотеки или инструмента:**

1. **Проверить наличие уже установленной версии в локальной среде:**
   ```bash
   # Python пакеты
   pip list | grep <package-name>
   # или
   pip show <package-name>

   # Node.js пакеты
   npm list -g <package-name>
   # или
   npm list <package-name>

   # Gradle зависимости
   ./gradlew dependencies --configuration <configuration-name>

   # Системные инструменты (Linux/macOS)
   which <tool-name>
   <tool-name> --version

   # Системные инструменты (Windows PowerShell)
   Get-Command <tool-name>
   <tool-name> --version
   ```

2. **Проверить глобально установленные версии:**
   ```bash
   # Python
   pip list --format=columns

   # Node.js
   npm list -g --depth=0

   # Rust
   cargo install --list

   # Go
   go list -m all
   ```

3. **Проверить виртуальные окружения:**
   ```bash
   # Python virtualenv
   source venv/bin/activate  # если активировано
   pip list

   # Python conda
   conda env list
   conda activate <env-name>
   ```

4. **Использовать скрипт автоматической проверки:**
   ```bash
   # Linux/macOS
   ./scripts/check-dependency-conflicts.py --package <package-name>

   # Windows PowerShell
   .\scripts\check-dependency-conflicts.ps1 -Package <package-name>
   ```

**Результаты проверки:**
- ✅ Если версия **не найдена** → можно устанавливать
- ⚠️ Если версия **найдена** → проверить совместимость версий
- ❌ Если версия **несовместима** → обновить/удалить старую версию перед установкой

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** перед любой установкой зависимостей

---

#### 🔴 **ПРАВИЛО #2: Проверка Окружения Перед Запуском Сборок/Тестов/Компиляции**

**Перед запуском любых сборок, тестов, компиляции или CI-задач:**

1. **Проверить наличие всех необходимых компонентов:**
   ```bash
   # Проверка скриптом
   # Linux/macOS
   ./scripts/check-environment-requirements.py

   # Windows PowerShell
   .\scripts\check-environment-requirements.ps1
   ```

2. **Проверить версии критических инструментов:**
   ```bash
   # Java
   java -version  # Требуется: JDK 17+

   # Gradle
   ./gradlew --version  # Требуется: Gradle 8.0+

   # Node.js
   node --version  # Требуется: Node.js 18+
   npm --version   # Требуется: npm 9+

   # Python
   python --version  # Требуется: Python 3.10+
   pip --version

   # C++ компилятор
   g++ --version    # Требуется: GCC 11+ (Linux)
   clang --version  # Требуется: Clang 13+ (macOS)
   MSVC version     # Требуется: Visual Studio 2022 (Windows)

   # Docker
   docker --version  # Требуется: Docker 20.10+
   docker-compose --version

   # Git
   git --version  # Требуется: Git 2.30+
   ```

3. **Проверить системные требования:**
   ```bash
   # RAM
   free -h  # Требуется: минимум 8GB RAM

   # Disk Space
   df -h  # Требуется: минимум 50GB свободного места

   # CPU Cores
   nproc  # Требуется: минимум 4 cores для параллельной сборки
   ```

4. **Проверить наличие системных библиотек:**
   ```bash
   # Linux
   ldconfig -p | grep <library-name>
   rpm -q <library-name>      # RHEL/CentOS
   dpkg -l | grep <library-name>  # Debian/Ubuntu

   # macOS
   brew list | grep <library-name>
   ls /usr/local/lib | grep <library-name>

   # Windows
   Get-ChildItem C:\Windows\System32 | Select-String <library-name>
   ```

5. **Проверить переменные окружения:**
   ```bash
   # Linux/macOS
   env | grep <VAR_NAME>

   # Windows PowerShell
   Get-ChildItem Env: | Where-Object {$_.Name -like "*<VAR_NAME>*"}
   ```

**Обязательный чеклист перед сборкой/тестами:**

- [ ] JDK 17+ установлен и доступен в PATH
- [ ] Gradle 8.0+ установлен (или используется wrapper)
- [ ] Node.js 18+ установлен (для веб-модулей)
- [ ] Python 3.10+ установлен (для скриптов CI)
- [ ] Docker 20.10+ запущен (для интеграционных тестов)
- [ ] Минимум 8GB RAM доступно
- [ ] Минимум 50GB свободного места на диске
- [ ] Все системные библиотеки установлены (FFmpeg, OpenCV и т.д.)
- [ ] Переменные окружения настроены (ANDROID_HOME, JAVA_HOME и т.д.)
- [ ] Git hooks установлены (`./scripts/setup-git-hooks.sh`)

**Команды для быстрой проверки:**

```bash
# Полный набор проверок (Linux/macOS)
./scripts/check-environment-requirements.py --verbose

# Полный набор проверок (Windows)
.\scripts\check-environment-requirements.ps1 -Verbose

# Быстрая проверка (только критические компоненты)
./scripts/check-environment-requirements.py --quick

# Проверка с генерацией отчёта
./scripts/check-environment-requirements.py --report diagnostics/environment-report.json
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** перед запуском `./gradlew build`, `./gradlew test`, `./gradlew assemble` и т.п.

---

### 2. Управление Конфликтами Зависимостей

**Где зафиксировано:**
- `scripts/check-dependency-conflicts.py` - проверка конфликтов
- `build.gradle.kts` - центральное управление версиями

**Правила:**

1. **Централизованное управление версиями:**
   - Все версии зависимостей в `libs.versions.toml` (Gradle Version Catalog)
   - Не указывать версии напрямую в модулях

2. **Проверка конфликтов перед добавлением:**
   ```bash
   # Linux/macOS
   ./scripts/check-dependency-conflicts.py --add <dependency>

   # Windows PowerShell
   .\scripts\check-dependency-conflicts.ps1 -Dependency <dependency>
   ```

3. **Разрешение конфликтов:**
   - Использовать наиболее совместимую версию
   - Обновить все зависимости до совместимых версий
   - Зафиксировать решение в `libs.versions.toml`

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

---

### 3. Docker Зависимости

**Где зафиксировано:**
- `docker-compose.yml` - окружение разработки
- `Dockerfile` - продакшен
- `scripts/check-container-health.py` - проверка контейнеров

**Правила:**

1. **Проверить запущенные контейнеры:**
   ```bash
   docker ps -a
   ```

2. **Проверить доступность сервисов:**
   ```bash
   # Linux/macOS
   ./scripts/check-container-health.py

   # Windows PowerShell
   .\scripts\check-container-health.ps1
   ```

3. **Перед запуском тестов, требующих Docker:**
   - ✅ Все контейнеры должны быть `healthy`
   - ✅ Порты должны быть свободны
   - ✅ Volume'ы должны быть подключены

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** для интеграционных тестов

---

## 📦 Управление Зависимостями (Дополнительно)

### 1. Gradle

**Правила:**
- ✅ Gradle wrapper обязателен (без кастомных установок)
- ✅ `gradle-wrapper-validation.yml` - валидация wrapper
- ✅ Dependency review action - проверка уязвимостей
- ✅ **Проверка перед установкой:** см. раздел [Правила Установки Зависимостей](#-правила-установки-зависимостей-и-проверки-окружения)
- ✅ **Проверка окружения перед сборкой:** см. раздел [Правила Установки Зависимостей](#-правила-установки-зависимостей-и-проверки-окружения)

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

---

### 2. Docker

**Где зафиксировано:**
- `docker-compose.yml` - окружение разработки
- `Dockerfile` - продакшен
- `container-scan.yml` - сканирование

**Правила:**
- ✅ Использовать official образы
- ✅ Сканирование на уязвимости
- ✅ `.dockerignore` - исключение лишнего

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** для серверных модулей

---

## ✅ Обязательные Чеклисты

### Перед Коммитом

**Код:**
- [ ] Пройден ktlint/Detekt (`./gradlew ktlintFormat detekt`)
- [ ] Пройден clang-format для C++ (`clang-format -i ...`)
- [ ] Пройден ESLint/TypeScript (`npm run lint`)
- [ ] Все тесты проходят (`./gradlew test`)
- [ ] Покрытие тестами > 80% (для критических модулей)

**KMP:**
- [ ] Нет запрещенных импортов в commonMain
- [ ] expect/actual для всех платформенных API
- [ ] Минимум 3 actual реализации
- [ ] Пройдены KMP-гаты (`verify-kmp-phase1.py --ci-profile`)

**Зависимости и Окружение:**
- [ ] Проверено отсутствие конфликтов зависимостей
- [ ] Проверено наличие всех необходимых компонентов для сборки
- [ ] Проверены версии инструментов (JDK, Gradle, Node.js, Python, Docker)
- [ ] Проверено наличие системных библиотек (FFmpeg, OpenCV и т.д.)
- [ ] Docker контейнеры запущены и healthy (если тесты требуют)
- [ ] Достаточно системных ресурсов (RAM >= 8GB, Disk >= 50GB)

**Документация:**
- [ ] Обновлен TIMELINE.md (если значимые изменения)
- [ ] Обновлена PROJECT_STRUCTURE_AUTO.md (если изменилась структура)
- [ ] Пройдена проверка ссылок (`python scripts/ci/check-doc-links-active.py`)
- [ ] Обновлен CHANGELOG.md (если релиз)

**Безопасность:**
- [ ] Нет секретов в коде
- [ ] Certificate pinning настроен
- [ ] HTTPS принудительный
- [ ] Нет JVM-зависимостей в native source sets

---

### Перед Pull Request

- [ ] Все локальные тесты проходят
- [ ] Пройдены все KMP-гаты
- [ ] Документация обновлена (если нужно)
- [ ] TIMELINE.md обновлен (если значимые изменения)
- [ ] PROJECT_STRUCTURE_AUTO.md обновлен (если изменилась структура)
- [ ] Нет секретов в staged files
- [ ] Code review от 1+ maintainers

---

### Перед Merge в main

- [ ] Все CI jobs green
- [ ] Code review от 2+ maintainers
- [ ] Sign-off от release manager
- [ ] CHANGELOG.md обновлен
- [ ] TIMELINE.md обновлен
- [ ] Версия проекта увеличена

---

## 🔍 Локальная Проверка

### Полный Набор Проверок (как в CI)

```bash
# 1. Код-стили
./gradlew ktlintFormat detekt
clang-format -i native/video-processing/src/*.cpp
cd server/web && npm run lint && npm run format && cd ../..

# 2. KMP-гаты
python scripts/ci/verify-kmp-phase1.py --ci-profile
python scripts/ci/check-commonmain-forbidden-imports.py .
python scripts/ci/check-no-jvm-deps-in-native-source-sets.py .

# 3. Сборка и тесты
./gradlew :core:common:compileKotlinMetadata
./gradlew :core:network:compileKotlinMetadata
./gradlew :shared:compileKotlinMetadata
./gradlew :core:common:desktopTest

# 4. Документация
python scripts/ci/check-doc-links-active.py

# 5. PROJECT_STRUCTURE_AUTO.md
python scripts/generate-project-structure.py
```

### Windows PowerShell

```powershell
# Код-стили
./gradlew ktlintFormat detekt
clang-format -i native/video-processing/src/*.cpp
cd server/web; npm run lint; npm run format; cd ..

# KMP-гаты
.\scripts\ci\verify-kmp-phase1.ps1 -CiProfile
.\scripts\ci\check-commonmain-forbidden-imports.ps1 .
.\scripts\ci\check-no-jvm-deps-in-native-source-sets.ps1 .

# Сборка и тесты
./gradlew :core:common:compileKotlinMetadata
./gradlew :core:network:compileKotlinMetadata
./gradlew :shared:compileKotlinMetadata
./gradlew :core:common:desktopTest

# Документация
python scripts/ci/check-doc-links-active.py

# PROJECT_STRUCTURE_AUTO.md
python scripts/generate-project-structure.py
```

### Linux/macOS/WSL

```bash
# Код-стили
./gradlew ktlintFormat detekt
clang-format -i native/video-processing/src/*.cpp
cd server/web && npm run lint && npm run format && cd ../..

# KMP-гаты
./scripts/ci/verify-kmp-phase1.sh --ci-profile
./scripts/ci/check-commonmain-forbidden-imports.sh .
./scripts/ci/check-no-jvm-deps-in-native-source-sets.sh .

# Сборка и тесты
./gradlew :core:common:compileKotlinMetadata
./gradlew :core:network:compileKotlinMetadata
./gradlew :shared:compileKotlinMetadata
./gradlew :core:common:desktopTest

# Документация
python scripts/ci/check-doc-links-active.py

# PROJECT_STRUCTURE_AUTO.md
python scripts/generate-project-structure.py
```

---

## ⚠️ Наказания за Нарушения

### Уровень 1: Предупреждение
- **Нарушения:** Небольшие отклонения от стилей кода
- **Действие:** PR помечается как "Changes requested", требуется исправление

### Уровень 2: Блокировка Merge
- **Нарушения:** Провал CI jobs, запрещенные импорты в commonMain, секреты в коде
- **Действие:** PR заблокирован до исправления

### Уровень 3: Отклонение PR
- **Нарушения:** Не обновлен TIMELINE.md при значимых изменениях, не обновлена PROJECT_STRUCTURE_AUTO.md, пропущены обязательные тесты
- **Действие:** PR отклонен до выполнения требований

### Уровень 4: Ban от контрибуций
- **Нарушения:** Повторные нарушения после предупреждений, умышленное игнорирование правил
- **Действие:** Временный или постоянный ban от контрибуций

---

## 📎 Ссылки на Исходники

### Конфигурационные Файлы
- `.editorconfig` - общие правила форматирования
- `.clang-format` - правила для C++
- `.ktlint.yml` - правила для Kotlin
- `.prettierrc.json` - правила для JS/TS
- `.gitignore` - игнорируемые файлы
- `.gitmessage.txt` - шаблон сообщений коммитов

### CI/CD
- `.github/workflows/ci.yml` - основной CI пайплайн
- `.github/workflows/build-native-libraries.yml` - сборка нативных библиотек
- `.github/workflows/codeql.yml` - security анализ
- `.github/workflows/secret-scan.yml` - сканирование секретов
- `.github/actions/setup-jdk-gradle/action.yml` - custom action

### Скрипты Проверок
- `scripts/ci/verify-kmp-phase1.py` (`.ps1`, `.sh`) - KMP проверки
- `scripts/ci/check-commonmain-forbidden-imports.py` (`.sh`) - запрещенные импорты
- `scripts/ci/check-security-expect-actual-signatures.py` (`.sh`) - security expect/actual
- `scripts/ci/check-no-jvm-deps-in-native-source-sets.py` - JVM-зависимости
- `scripts/ci/validate-video-e2e-profile.py` - video E2E профиль
- `scripts/ci/validate-video-runtime-matrix-config.py` - runtime matrix
- `scripts/ci/check-doc-links-active.py` - проверка ссылок

### Управление Документацией
- `scripts/manage-documentation.ps1` (`.sh`, `.py`) - управление документами
- `scripts/generate-project-structure.py` - генерация структуры

### Git Hooks
- `scripts/setup-git-hooks.sh` - установка git hooks

### Тестирование
- `scripts/ci/mvp-automated-acceptance.ps1` (`.sh`) - автотесты MVP
- `scripts/run-phase1-onvif-acceptance.ps1` - ONVIF acceptance
- `scripts/video-e2e-go-no-go.ps1` - video E2E gate

---

## 📞 Вопросы и Поддержка

**Вопросы по правилам:**
- Создать Issue с меткой `question`
- Обратиться к maintainers в Discord/Slack

**Нарушения:**
- Сообщить через Issue с меткой `violation`
- Либо напрямую maintainers

**Исключения:**
- Обсудить с maintainers
- Зафиксировать в `docs/EXCEPTIONS.md`

---

## 📝 История Изменений

| Версия | Дата | Изменения | Автор |
|--------|------|-----------|-------|
| 1.0.0 | 16 May 2026 | Начальная версия | AI Assistant |

---

**© 2026 IP-CSS Project. Все права защищены.**

**Этот документ является обязательным для всех контрибьюторов проекта.**
