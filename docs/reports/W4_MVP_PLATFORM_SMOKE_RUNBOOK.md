# W4 MVP: платформы, тесты и GO/NO-GO (runbook)

**Версия проекта:** Alfa-0.1.1  
**Назначение:** воспроизводимые шаги для **Недели 4** плана [PHASE1_MVP_TO_100_PLAN.md](../planning/PHASE1_MVP_TO_100_PLAN.md) (этапы 1.7 + 1.10) и подготовки к решению по [RELEASE_GO_NO_GO_CHECKLIST.md](../planning/RELEASE_GO_NO_GO_CHECKLIST.md).

---

## 1. Автоматизируемый build gate

Из корня репозитория (Windows, PowerShell):

```powershell
.\scripts\w4-mvp-platform-and-gate.ps1
```

- По умолчанию: `:server:api:build` и `:android:app:compileDebugKotlin` (без `-Full`).
- Опция **`-AndroidLocalFrameAnalytics`**: те же Gradle-аргументы `-Pipcss.localFrameAnalytics=true`, что и у `android-video-background-smoke.ps1 -LocalFrameAnalytics`, применяются к **обоим** шагам — к `compileDebugKotlin` в gate и к smoke (не `-Full`).
- Полный релизный набор (как в чеклисте, раздел 1):  
  `.\scripts\w4-mvp-platform-and-gate.ps1 -Full`  
  Опции: `-IncludeWeb` (tsc в `server/web`), `-IncludeDesktop`, `-IncludeNas`.
- Профили `-RunProfile local|staging|strict` при включённых platform smoke передают в `android-video-background-smoke.ps1` флаг **`-CompileOnly`** и в `desktop-video-event-longrun-smoke.ps1` **`-CompileAndTestOnly`**, чтобы автоматический gate не уходил в `PARTIAL` без adb/долгого long-run; при **`-CompileOnly`** сбой опционального `installDebug` (если включён `-RunInstallIfDevicePresent`) также не даёт `PARTIAL`, пока зелёны compile+assemble; при **`-CompileAndTestOnly`** «мягкий» исход long-run (exit 3 у `video-runtime-longrun-smoke.ps1`) не даёт `PARTIAL`, пока зелёны compile+desktop tests. Для полной строгости по железу: `-NoPlatformSmokeCompileOnlyGate` (и ручные W4-1 / W4-2 ниже).

Результат зафиксировать (лог терминала, дата). В `w4-mvp-platform-gate-*.json|.md` сохраняются **runProfile**, **full**, **platformSmokeCompileOnlyGate**, **androidLocalFrameAnalytics** (см. `scripts/show-latest-w4-gate-status.ps1`).

---

## 2. Ручные сценарии W4 (матрица)

### W4-1 — Android: видео + фон

- Установить debug/release APK на устройство.
- Экран live view: RTSP и HLS (переключатель), воспроизведение без падений ≥ 10–15 мин.
- При необходимости: PiP / фоновое воспроизведение (MediaSession) — без критических регрессий.

### W4-2 — Desktop: long-run + события

- Собрать пакет для текущей ОС ([LOCAL_RELEASE_DESKTOP_X86_64.md](../planning/LOCAL_RELEASE_DESKTOP_X86_64.md) / ARM).
- Открыть видео/события, оставить сессию ≥ 30 мин (или согласованный порог), зафиксировать утечки/краши.

### W4-3 — Integration-тесты

- Локально: `./gradlew :server:api:test` (при необходимости с env из `server/api/build.gradle.kts`).
- Зафиксировать зелёный прогон и известные флейки.

### W4-4 — E2E smoke (минимум)

- Критичные сценарии: логин, список камер, открытие live/записи (по принятому для MVP набору).
- Зафиксировать среду (URL API, версии клиентов).

### W4-5 — Финальная приёмка и GO/NO-GO

- Пройти [RELEASE_GO_NO_GO_CHECKLIST.md](../planning/RELEASE_GO_NO_GO_CHECKLIST.md) (build, артефакты, smoke, security, документация).
- Заполнить блок «Фиксация решения» в том же файле (или копию ниже).

---

## 3. Шаблон записи решения (копия в отчёт при необходимости)

| Поле | Значение |
|------|----------|
| Дата | |
| Версия / ветка | |
| W4 gate (скрипт) | OK / FAIL |
| Ручные W4-1 … W4-4 | OK / FAIL / N/A |
| Решение | **GO** / **NO-GO** |
| Ответственный | |
| Комментарии / риски | |

---

## 4. Где хранить артефакты диагностики

Рекомендуемый каталог: `diagnostics/platform-smoke/YYYYMMDD/` (логи, скриншоты, короткий `README.txt` с командами и результатом).
