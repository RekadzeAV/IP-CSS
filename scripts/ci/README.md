# 🤖 Скрипты CI/CD и Автоматизации

**Версия:** 1.0.0  
**Дата:** 16 May 2026

---

## 📋 Обзор

Этот каталог содержит все скрипты для автоматизации CI/CD, проверок и приемочного тестирования.

**Полная документация по скриптам:** [scripts/README.md](../README.md)

---

## 🔒 Обязательные KMP-проверки (CI Gates)

### `verify-kmp-phase1.py` / `verify-kmp-phase1.ps1` / `verify-kmp-phase1.sh`

**Описание:** Комплексная проверка Kotlin Multiplatform проекта

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
- Пустые `playlistUrls` в enabled local scenario = ошибка

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** перед PR

---

### `check-commonmain-forbidden-imports.py` / `check-commonmain-forbidden-imports.sh`

**Описание:** Проверка запрещенных импортов в commonMain

**Запрещенные:**
- `java.*`
- `javax.*`
- `android.*`
- Любые платформенные API

**Команда:**
```bash
python scripts/ci/check-commonmain-forbidden-imports.py .
# или
./scripts/ci/check-commonmain-forbidden-imports.sh .
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

---

### `check-security-expect-actual-signatures.py` / `check-security-expect-actual-signatures.sh`

**Описание:** Проверка сигнатур expect/actual для security модулей

**Требования:**
- Минимум 3 actual реализации (Android, JVM/Desktop, iOS)
- Сигнатуры должны совпадать

**Команда:**
```bash
python scripts/ci/check-security-expect-actual-signatures.py .
# или
./scripts/ci/check-security-expect-actual-signatures.sh .
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** для security модулей

---

### `check-no-jvm-deps-in-native-source-sets.py`

**Описание:** Проверка отсутствия JVM-зависимостей в native source sets

**Команда:**
```bash
python scripts/ci/check-no-jvm-deps-in-native-source-sets.py .
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО**

---

### `check-video-runtime-matrix-config.py`

**Описание:** Валидация конфигурации runtime matrix

**Команда:**
```bash
python scripts/ci/validate-video-runtime-matrix-config.py --root .
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении runtime matrix

---

### `validate-video-e2e-profile.py`

**Описание:** Валидация video E2E acceptance profile

**Команда:**
```bash
python scripts/ci/validate-video-e2e-profile.py --root .
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении video E2E

---

### `check-doc-links-active.py`

**Описание:** Проверка локальных markdown-ссылок в active scope

**Команда:**
```bash
python scripts/ci/check-doc-links-active.py
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении документации

---

### `validate-api-docs.py`

**Описание:** Валидация API документации (OpenAPI + WebSocket protocol)

**Команда:**
```bash
python scripts/ci/validate-api-docs.py
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении API

---

### `validate-audit-integrity-artifacts.py`

**Описание:** Валидация audit integrity artifacts (migration + hash chain)

**Команда:**
```bash
python scripts/ci/validate-audit-integrity-artifacts.py
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении миграций БД

---

## 🧪 MVP Автоматизация

### `mvp-automated-acceptance.ps1` / `mvp-automated-acceptance.sh`

**Описание:** Автоматизированное приемочное тестирование MVP Phase 1

**Команды:**
```bash
# Windows PowerShell
.\scripts\ci\mvp-automated-acceptance.ps1
.\scripts\ci\mvp-automated-acceptance.ps1 -GeneratePhase1Summary

# Linux/macOS/WSL
bash scripts/ci/mvp-automated-acceptance.sh
bash scripts/ci/mvp-automated-acceptance.sh --generate-phase1-summary
```

**Что делает:**
- Gradle тесты (`:shared:desktopTest`, `:core:network:desktopTest`, `:server:api:test`)
- Web тесты (`server/web` build + test)
- Video E2E gate (`video-e2e-go-no-go.ps1`)
- Генерация Phase1 summary (опционально)

**Переменные:**
- `MVP_VIDEO_ACCEPTANCE_PROFILE` - путь к JSON профилю video gate
- Если не задана в GitHub Actions → `config/video-e2e-acceptance-profile.mvp-ci.json`

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** перед релизом MVP

---

### `video-e2e-go-no-go.ps1`

**Описание:** Video E2E go/no-go gate

**Команда:**
```bash
.\scripts\video-e2e-go-no-go.ps1
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении video модулей

---

### `render-kmp-verify-report.py`

**Описание:** Рендеринг KMP verifier markdown summary из JSON отчета

**Команда:**
```bash
python scripts/ci/render-kmp-verify-report.py \
  --input diagnostics/kmp/verify-report.json \
  --output diagnostics/kmp/verify-report.md
```

**Обязательность:** 🟡 **Рекомендуется** для CI

---

## 📡 ONVIF Автоматизация

### `run-phase1-onvif-acceptance.ps1`

**Описание:** Orchestration ONVIF acceptance тестов

**Команда:**
```bash
.\scripts\run-phase1-onvif-acceptance.ps1
```

**Что делает:**
- 1.4.1 unit-тесты
- UI regression
- 1.4.3 resilience (в т.ч. optional WS/latency checks)
- Автоматическое обновление unified evidence report

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** при изменении ONVIF модулей

---

### `run-phase1-onvif-acceptance-secure.ps1`

**Описание:** Secure wrapper для `run-phase1-onvif-acceptance.ps1`

**Команда:**
```bash
.\scripts\run-phase1-onvif-acceptance-secure.ps1
```

**Опции:**
- `-NoPrompt` - не запрашивать пароль (требуется `IPCSS_ADMIN_PASSWORD`)
- `-RequireOnvifEvidence` - `1.4.3 != PASS` = NO_GO

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** для production CI

---

### `run-phase1-onvif-gate.ps1`

**Описание:** One-command wrapper для ONVIF gate

**Команда:**
```bash
.\scripts\run-phase1-onvif-gate.ps1
```

**Опции:**
- `-ShowLatestOnRunError`
- `-AsJson` - единый JSON результат
- `-SkipArtifactsValidation` - отключить валидацию артефактов

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** для ONVIF gate

---

### `show-latest-phase1-onvif-decision.ps1`

**Описание:** Вывод последнего gate-решения ONVIF

**Команда:**
```bash
.\scripts\show-latest-phase1-onvif-decision.ps1
```

**Вывод:** GO/CONDITIONAL/NO_GO + пути к артефактам

---

### `validate-phase1-onvif-artifacts.ps1`

**Описание:** Проверка целостности ONVIF артефактов

**Команда:**
```bash
.\scripts\validate-phase1-onvif-artifacts.ps1
```

**Exit codes:**
- `0` = PASS
- `1` = WARN
- `2` = FAIL

---

### `onvif-events-api-verification.ps1`

**Описание:** HTTP-сценарий ONVIF Events при наличии `config/test-cameras.local.json`

**Опции:**
- `-EnableWebSocketCheck` - optional real-time WS-проверка
- `latencyMs` vs `latencyThresholdMs` в JSON-артефакте

---

### `onvif-events-resilience-verification.ps1`

**Описание:** Двухфазная проверка устойчивости ONVIF Events

**Что делает:**
- baseline add/subscribe/events
- (optional restart)
- повторный add/delete цикл
- прокидывает WS/latency параметры
- сохраняет markdown/json артефакты в `diagnostics/onvif-events`

---

### `validate-onvif-test-camera-config.ps1`

**Описание:** Preflight-проверка `config/test-cameras.local.json`

**Проверки:**
- Обязательные поля
- Структура
- Предупреждения

---

### `generate-phase1-onvif-evidence-report.ps1`

**Описание:** Формирование единого evidence-отчета по 1.4.3/1.4.1/UI regression

**Вывод:** `docs/reports`

---

## 🌐 Network Layer

### `run-network-layer-1-4-local-smoke.ps1`

**Описание:** One-command локальный smoke-контур 1.4

**Команда:**
```bash
.\scripts\run-network-layer-1-4-local-smoke.ps1
```

**Опции:**
- `-LocalSmoke`
- `-AsJson`
- `-SkipStatusSync`

**Что делает:**
- preflight
- strict status-sync audit
- aggregate decision report

---

## 🔧 Утилиты

### `run-server-api-integration-compose.ps1` / `run-server-api-integration-compose.sh`

**Описание:** API integration smoke via Docker Compose (PostgreSQL + Redis)

**Команда:**
```bash
.\scripts\ci\run-server-api-integration-compose.ps1
# или
bash scripts/ci/run-server-api-integration-compose.sh
```

**Обязательность:** ✅ **ОБЯЗАТЕЛЬНО** в CI

---

### `check-postgres-finalization.sh`

**Описание:** Проверка PostgreSQL finalization

**Команда:**
```bash
./scripts/ci/check-postgres-finalization.sh
```

---

## 📚 Документация

**Полный список скриптов:** [scripts/README.md](../README.md)  
**Правила проекта:** [docs/RULES_AND_AUTOMATION.md](../../docs/RULES_AND_AUTOMATION.md)  
**MVP Automation:** [docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](../../docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md)

---

**© 2026 IP-CSS Project. Все права защищены.**
