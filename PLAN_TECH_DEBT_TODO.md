# План работ по техдолгу и ToDo

**Версия проекта:** 0.5.1.1-beta
**Дата актуализации:** 14.09.2026 (первичная: 08.08.2026)
**Статус:** ~95% закрыто; остатки — пост-релиз (`PLAN_COMPLETION_2026_Q4.md` Фаза 6)

---

## 1. Repo-гигиена (техдолг №1)

| Задача | Статус |
|--------|--------|
| `git rm -r --cached data/postgres/` (1266 файлов PostgreSQL-данных) | ✅ Выполнено (файлы сохранены на диске) |
| Игнорировать нативные артефакты: `*.a`, `*.dll`, `CMakeCache.txt`, `build.ninja`, `*.obj`, `*.vcxproj`, `CMakeFiles/` | ✅ Выполнено (29.08 + Этап 3, 04.09; перепроверено 14.09 — в git нет трекаемых нативных артефактов) |
| Убрать из git build-интермедиаты (`**/build/**`), `.tmphome/`, `diagnostics/`, `release-build/`, `logs/` | ✅ Выполнено (Этап 3, 04.09) |
| Удалить `.bak`, `--help` | ✅ Выполнено 29.08 |
| Сверить `.gitignore` с фактически трекаемыми файлами | ✅ Выполнено **14.09**: выведены IDE-кэши `.vs/` (23 файла: `.vsidx/.wsuo/*.bin`), защищены `.vsidx/.wsuo` и `build-logs/` (коммит `bbc64756`); нативные артефакты не трекаются |

## 2. TODO-деконсолидация
- ✅ Выполнено 04.09: в `archive/docs-deprecated-2026-09-04/` перенесены устаревшие планы/отчёты (53 файла, включая корневые WORK_PLAN/REFACTORING_*).
- ✅ Продолжено 14.09: проверен остаток `docs/planning/` — 24 файла PHASE1_*/SPRINT_*/TASK_* — **осознанно отложены на пост-релиз (Этап 10)** согласно `PLAN_EXECUTION_MASTER.md` (этап 3: «массовая архивация docs/planning → Этап 10»), ссылки на них есть в активных индексах. `REMAINING_TASKS.md` — единственный трекер.

## 3. Внутренние TODO в коде

| Область | TODO | Статус |
|---------|------|--------|
| `core/ui-bridge` | Интеграция с Login/Logout/GetCameras/AddCamera/Discover/ControlPtz UseCase | ✅ Desktop (29.08) + androidMain (05.09, `:core:ui-bridge:assemble` зелёная) |
| `core/security` | Реальный `SecurePasswordHasher` | ✅ PBKDF2WithHmacSHA256 (29.08; `core:security:desktopTest` 37/0) |
| `core/network` | iOS `UPnPDiscovery` (NSURLSession/CFNetwork) | ⬜ iOS-трек (после появления macOS-окружения) — порт A2 плана |
| `core/network` | `ScannerUtils` расчёт ETA сканирования | ✅ Реализовано 29.08 (+7 тестов `ScanProgressEtaTest`) |
| `server` | Миграция репозиториев на SQLDelight/PostgreSQL | ✅ Завершено 04.09 (Events 29.08 + Recording/Settings 04.09, in-memory удалён из DI) |
| `core/common` | Mobile security logger → отправка критических событий на сервер | ✅ Выполнено 05.09: `SecureMobileSecurityLogger.remoteSink` + `SecurityEventUploader` (POST /api/v1/audit/client-events) |
| `server` | `OfferSdpAnswerFactory.buildAnswer` — длинный/сложный (WebRTC SDP-стаб) | 🟡 Остаток → Фаза 2.2 плана `PLAN_COMPLETION_2026_Q4.md` |

## 4. Итог
Прогресс фиксируется в `REMAINING_TASKS.md`. Оперативный план — `PLAN_COMPLETION_2026_Q4.md` (последовательный, без возвратов). Остаток программный: ~2–3 дня (Фазы 1 завершена, 2-3) + пост-релиз (Фаза 6).

