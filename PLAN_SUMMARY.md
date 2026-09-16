# 📋 СВОДНЫЙ ПЛАН ПРОЕКТА IP-CSS

**Версия проекта:** 0.5.1.1-beta
**Дата актуализации:** 04.09.2026 (первичная: 08.08.2026)
**Ветка:** main
**Статус:** P0 закрыт (29.08–04.09); безрегрессный конвейер к 0.6.0-beta — `PLAN_EXECUTION_MASTER.md`

## 📊 Текущий статус проекта

| Показатель | Значение |
|---|---|
| Версия | 0.5.1.1-beta |
| Gradle | 8.9 |
| JDK | 17 |
| Всего файлов | ~210k |
| `docs/` .md | 1,291 |
| Модулей KMP/Gradle | 12+ |

### Модули
- `shared`, `core:common`, `core:network`, `core:security`, `core:ui-bridge`, `core:test-jvm`
- `server:api`, `server:web`
- `android:app`
- `platforms`: `client-desktop-x86_64`, `client-desktop-arm`, `client-android`, `client-ios`, `nas-x86_64`, `nas-arm`, `sbc-arm`, `server-x86_64`

### Готовность к тестовым сборкам
- ✅ `:server:api:test` — **474 passed / 0 failed** (Kover №6-8 + Web Push delivery + WebRTC gateway-интерфейс; покрытие `server:api` — **30.1% LINE**, порог `minBound(30)`).
- ✅ `:shared:desktopTest` — 245 passed
- ✅ `:core:common:desktopTest` — 23 passed
- ✅ `:core:test-jvm:test` — 19 passed
- ✅ `:core:network:desktopTest` — **544 tests, 0 failed, 38 skipped** (все 25 падений устранены)
- ✅ `:server:api:integrationTest` — подключён к Gradle; 14 tests (10 skipped без сервера, 4 passed DB)
- ✅ KMP-гейты `scripts/ci/verify-kmp-phase1.py --skip-gradle` — проходят (закрыт прогон `check-commonmain-forbidden-imports`, `check-security-expect-actual-signatures`)
- ✅ Android: `assembleDebug` собран + `testDebugUnitTest` 3/3 (APK ~33MB)
- ⚠️ iOS/NAS — не проверялись (требуют macOS/Xcode, нативное окружение)
**Статус:** Выполняется (консолидация + подготовка к тестовым сборкам)

## 📁 Файловая база и артефакты
- Всего файлов: **~210k**.
- Модульные исходы/ресурсы: `shared` ~8.6k, `core` ~6.8k, `server` ~102k, `android` ~408, `platforms` ~259, `docs` ~1.3k, `native` ~73.7k, `data` ~1.3k, `config` ~22, `credentials` ~13.
- Артефакты сборки присутствуют в `build/` (~448 файлов), корень чистится от готовых dist.

## ⚠️ Блокеры/ограничения (актуализировано 04.09)
- `:core:network:desktopTest` — ✅ **544 tests / 0 failed / 38 skipped** (все 25 падений устранены; причины и фиксы в `PLAN_TESTING.md`).
- Android: APK собирается (~33MB); NAS/iOS в локальных прогонах не проверялись (iOS — macOS-трек).
- `docs/` — 04.09: 54 устаревших файла архивированы в `archive/docs-deprecated-2026-09-04/`; линк-чек после переноса — прогнан, битые ссылки устранены.
- KMP-гейты: полный прогон с Gradle-шагами проходит (`PLAN_TESTING.md` п.5); NAS-окружение — Этап 7.

## 📈 Подробная файловая статистика
### Топ расширений (без артефактов build/.git)
- `.js` — 50,024
- `.ts` — 23,095
- `.class` — 12,587
- `.json` — 9,361
- `.map` — 8,298
- `.c` — 6,910
- `.h` — 5,469
- `.cmake` — 5,437
- `.md` — 3,481
- `.jar` — 3,397

### Исходники проекта
- Kotlin (`.kt`) — 2,126
- Kotlin DSL (`.kts`) — 24
- Java (`.java`) — 17
- C/C++ (`.c`/`.cpp`) — 7,038
- Headers (`.h`) — 5,469
- Swift/ObjC (`.swift`/`.m`) — 21
- JS/TS — 73,119
- XML — 1,383
- Markdown — 3,481
- Scripts (`.ps1`/`.py`/`.sh`) — 546
- Build configs (`.gradle`/`.properties`/`.yml`/`.yaml`) — 327
- Protobuf (`.proto`) — 26
- KMP def (`.def`) — 61

### Модули по файлам (исходы без build/артефактов, факт 09 Aug 2026)
- Всего файлов (без `build/`, `node_modules/`, `.git/`) — **93,098**
- Kotlin (`src/**`) по модулям:
  - `shared` — 257
  - `core/common` — 25
  - `core/network` — 189
  - `core/security` — 25
  - `core/ui-bridge` — 13
  - `core/test-jvm` — 18
  - `server/api` — 510
  - `server/web` (без `node_modules`) — 339
  - `android/app` — 46
- Прочие основные группы: `native/` (С/C++ + vcpkg), `docs/`, `scripts/`, `config/`, `data/` (~101 console-wide counts по расширениям).
- Исходники Kotlin (`.kt`, без артефактов) — **2,109**; Python (`*.py`) — **320**.

### Документация
- `docs/` — 1,180 `.md` (после архивации 57 дублей)
- Корневые `.md` — 23 файла (README, PLAN_*, ARCHIVE_MANIFEST и др.)

## 🧪 Тестовая готовность (факт на 08 August 2026)
| Модуль | Задача | Tests | Failures | Skipped | Статус |
|---|---|---|---|---|---|
| `server:api` | `test` | 328 | 0 | 0 | ✅ (добавлены Kover-тесты; ранее 253) |
| `shared` | `desktopTest` | 245 | 0 | 1 | ✅ |
| `core:common` | `desktopTest` | 23 | 0 | 5 | ✅ |
| `core:test-jvm` | `test` | 19 | 0 | 0 | ✅ |
| `core:network` | `desktopTest` | 544 | 0 | 38 | ✅ **все 25 падений устранены** |
| `server:api` | `integrationTest` | 14 | 0 | 10 | ✅ запускается; 10 skipped (нет сервера), 4 passed (DB) |
| `android:app` | `assembleDebug` ✅ + `testDebugUnitTest` ✅ (3 tests, 0 failures) | — | — | — | ✅ APK собран (33MB) |

Итого стабильных модулей: **540 (без core:network) + 506 = 1046 tests, 0 failures**; core:network 38 skipped — платформенные/интеграционные; `server:api` — 328 тестов, 0 failures.

## ⚠️ Риски и ограничения
- `:core:network:desktopTest` — ✅ все 25 падений устранены (отчёт `docs/analysis-core-network-25-failures-2026-08-09.md`), 38 skipped — интеграционные/платформенные сценарии.
- Интеграционные тесты `server:api` подключены и запускаются (`ApiIntegrationTest` пропускается при отсутствии сервера, `DatabaseIntegrationTest` проходит).
- Stub-аудит активного кода: `docs/stub-audit-active-2026-08-09.md` (52 файла).
- Kover `server:api`: INSTRUCTION 22.2% / **LINE 23.1%** / BRANCH 17.4% / METHOD 21.8% / CLASS 23.8% (порог `minBound(10)` пройден с запасом; цель — ≥30%).
- **Уязвимости:** GitHub завил 121 vulnerability (61 high / 55 moderate / 5 low) на `main`; `gh` не авторизован → точный список требует `gh auth login`. Кандидаты по версиям: BouncyCastle 1.70, Ktor 2.3.5 (отчёт `docs/dependency-security-audit-2026-08-10.md`).
- Нет данных по iOS/NAS тестам в этом прогоне. Android — собран (см. выше).
- **`core:ui-bridge`** — ✅ **восстановлен (2026-08-24)**: синхронизирован expect/actual `UiBridge`, импорты актуализированы (`DiscoveredCamera` из `shared.domain.repository`, битый импорт `RtspClient` удалён), локально объявлен отсутствовавший `LicensePlateResult`, удалён дублирующий `interface WebSocketClient`; конструкторы бриджей приведены к nullable-контракту тестов. Результат: компиляция commonMain/desktopMain/androidMain + **desktopTest 46 / 0 failed**.

## 📄 Отчёты и артефакты анализа (09-10 August 2026)
- `docs/analysis-core-network-25-failures-2026-08-09.md` — детализация 25 падений `core:network:desktopTest` (✅ устранены: 544 tests / 0 failed / 38 skipped).
- `docs/stub-audit-active-2026-08-09.md` — аудит заглушек в активном коде.
- `docs/dependency-security-audit-2026-08-10.md` + `docs/dependency-manifest-2026-08-10.csv` — манифест зависимостей и аудит уязвимостей (121 advisory; кандидаты: BouncyCastle 1.70, Ktor 2.3.5).
- `scripts/ci/export-dependency-manifest.py` — offline-экспорт манифеста зависимостей.
- `server/api/build/reports/kover/html/index.html` — покрытие `server:api` (Kover).
- `docs/archive/core-network-discovery-duplicates-2026-08-09/` — заархивированные JVM-дубли WS/UPnP Discovery.

## 📋 Вывод
Проект имеет большую кодовую базу (~210k файлов) с акцентом на кроссплатформенность (KMP + нативные модули). Основные модули собираются, юнит-тесты стабильны. Основной риск — интеграционные тесты `core:network` и отсутствие подключённых интеграционных сценариев для сервера.
## 📌 Вывод по готовности
- Unit/JVM тесты модулей `server`, `shared`, `core:common`, `core:test-jvm` зелёные.
- Основная блокирующая проблема для полноценной тестовой сборки устранена (`:core:network:desktopTest` снова собирается).
- Документация консолидируется, часть дублей уже перемещена в архив.
**Статус:** Выполняется (консолидация + подготовка к тестовым сборкам)

---

## 📑 Все планы (сводка)

| # | Документ | Назначение | Приоритет | Статус |
|---|----------|------------|-----------|--------|
| 1 | [PLAN_DEVELOPMENT.md](PLAN_DEVELOPMENT.md) | Функциональные доработки (аналитика, PTZ, ffmpeg, репозитории) | 🔴 P0 | ⬜ Ждёт среды сборки |
| 2 | [PLAN_FIXES.md](PLAN_FIXES.md) | Исправления и риски (секреты, D8 OOM, LICENSE, dependabot) | 🔴 P0 | 🟡 Частично |
| 3 | [PLAN_VULNERABILITIES.md](PLAN_VULNERABILITIES.md) | Устранение уязвимостей зависимостей (реестр VULN, CI-гейты, dependabot, процесс новых) | 🔴 P0 | 🟡 В работе (npm-контур закрыт 14.09) |
| 4 | [PLAN_TECH_DEBT_TODO.md](PLAN_TECH_DEBT_TODO.md) | Техдолг и консолидация ToDo (repo-гигиена, TODO-кода) | 🔴 P0 | 🟡 Частично |
| 5 | [PLAN_REFACTORING.md](PLAN_REFACTORING.md) | Рефакторинг (native↔сервер, дубли, expect/actual) | 🟠 P1 | ⬜ |
| 6 | [PLAN_TESTING.md](PLAN_TESTING.md) | Тестирование (baseline, интеграционные, native smoke, KMP-гейты) | 🟠 P1 | ⬜ |
| 7 | [PLAN_TEST_BUILDS.md](PLAN_TEST_BUILDS.md) | Выпуск тестовых сборок (установка/настройка/запуск) | 🟠 P1 | ⬜ |
| 8 | [PLAN_RELEASE.md](PLAN_RELEASE.md) | План релиза (GO/NO-GO, артефакты, публикация) | 🟠 P1 | ⬜ |
| 9 | [PLAN_DOCUMENTATION.md](PLAN_DOCUMENTATION.md) | Консолидация документации (индекс, дубли, ссылки, архив) | 🟢 P2 | 🟡 Частично |
| 10 | [PLAN_RECOMMENDATIONS.md](PLAN_RECOMMENDATIONS.md) | Рекомендации по тестированию/релизу/улучшениям | 🟢 P2 | ⬜ |
| 11 | [PLAN_ROADMAP.md](PLAN_ROADMAP.md) | План развития (фазы к v1.0.0) | 🟢 P2 | ⬜ |

---

## ✅ Выполнено (когорта 0.5.1.1-beta)

- [x] Все изменения сохранены и влиты в `main` (`a3ce215` → `4bb836e`), запушено в `github/main`.
- [x] Версия зафиксирована: `0.5.1.1-beta` (`gradle.properties`, README, CHANGELOG, индексы, планы).
- [x] Созданы 10 плановых документов + настоящий сводный документ.
- [x] Корневой `DOCUMENTATION_INDEX.md` обновлён (v3.1), битые ссылки устранены (PLAN_* вместо несуществующих анализаторов).
- [x] `CONTRIBUTING.md`: ссылки приведены к реальным скриптам (`update-documentation.*`, `archive-docs.ps1`, `check-docs-links.*`).
- [x] `data/postgres/` (1186 файлов) снято с git-трекинга (файлы сохранены на диске).
- [x] Проверено окружение: JDK 17 (`17.0.17`), Gradle wrapper, Android SDK.
- [x] Компиляция `:server:api` — **BUILD SUCCESSFUL** (24s).
- [x] Тесты `:server:api:test` — **328 теста, 0 failures / 0 errors / 0 skipped** (добавлены Kover-тесты; ранее 253).
- [x] Тесты `:shared:desktopTest` — **245 тестов, 1 skipped, 0 failures / 0 errors**.
- [x] Восстановлена собираемость `:core:network:desktopTest` (компиляционная ошибка в `OnvifClientIntegrationTest.kt`). Результат: **544 теста, 25 failed, 38 skipped**.
- [x] Исправлен флаки-тест `CameraRepositoryImplTest > test update camera success` (гонка на миллисекундном разрешении `updatedAt`, строгое `>`). Правка: явное прошлое `updatedAt` исходной камеры.
- [x] Kover для `server:api` — добавлены unit-тесты для ранее непокрытых классов (`JwtService`, `ServerSettingsRepository`, `ServerUserRepositoryInMemory`, частично `RequestValidator`, `AnalyticsConfigValidator`). Покрытие LINE **19.9% → 23.1%**, тесты `server:api:test` **328 / 0 failures**, CI-порог `koverVerify minBound(10)` пройден с запасом (commit `3d0bb6294`).
- [x] `LICENSE` заполнен (заглушка GPLv3 + SPDX).
- [x] Убран плейсхолдер пароля админа (dev-only случайный пароль, хешируется именно он; production блокируется без env-пароля).

---

## ⏭ В работе / следующие шаги (актуализировано 04.09 → Этапы мастер-плана)

| Этап | Задача | Статус |
|------|--------|--------|
| 0 | Закоммитить когорту 04.09 (Recording/Settings SQLDelight, удаление стабов, DI) и push (`main` опережает `github/main` на 18 коммитов) | ⬜ |
| 2 | Строгий `integrationTest` в CI (без `\|\| true`); android-джоба на `:android:app`; Ratchet detekt | ⬜ |
| 3 | Гигиена: gitignore нативных артефактов; деконсолидация TODO; линк-чек после архивации 04.09 | 🟡 |
| 4 | Консолидация Android (`platforms/client-android` → `:android:app`; CI сейчас собирает несуществующий проект) | ⬜ |
| 5 | FCM (`google-services.json`), `POST_NOTIFICATIONS`, ui-bridge androidMain, Telegram/email-каналы | ⬜ |
| 6 | Dependabot: 112 уязвимостей (61 high), итерации по 5–10 обновлений | ⬜ |
| 7–9 | Тестовые сборки → полевая валидация (GO/NO-GO) → релиз 0.6.0-beta | ⬜ |

Детали, гейты и матрица рисков: `PLAN_EXECUTION_MASTER.md` (этапы 0–10).

---

## 📚 Сопутствующие корневые документы

- **PLAN_EXECUTION_MASTER.md** — мастер-план выполнения (этапы 0–10, гейты)
- **README.md** — обзор и быстрый старт
- **CHANGELOG.md** — история изменений (запись 0.5.1.1-beta добавлена)
- **REMAINING_TASKS.md** — список незавершённых задач (единый трекер)
- **PROJECT_STRUCTURE.md** — структура модулей
- **DOCUMENTATION_INDEX.md** — полный индекс документации (единая точка входа)

---

*Сводный документ обновляется по мере выполнения плана.*