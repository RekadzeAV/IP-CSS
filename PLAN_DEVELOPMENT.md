# План доработок (функциональных)

**Версия проекта:** 0.5.1.1-beta
**Дата актуализации:** 04.09.2026 (первичная: 08.08.2026)
**Статус:** P0 закрыт полностью (29.08–04.09); P1 — частично (остаток = Этапы 4–5 мастер-плана `PLAN_EXECUTION_MASTER.md`)

---

## 🔴 Приоритет P0 — блокировало достоверный статус «готово» → ✅ ЗАКРЫТО

| № | Задача | Итог |
|---|--------|------|
| 1 | Реальная нативная аналитика на сервере | ✅ 29.08: конвейер RTSP→кадры→JNI OpenCV (MOG2/YOLO), MotionZoneFilter, graceful degradation; +10 тестов |
| 2 | Реальный PTZ через ONVIF | ✅ 29.08: `CameraControlService` (ContinuousMove/Stop/Zoom/GotoPreset), +12 тестов |
| 3 | Реальные ffmpeg-операции | ✅ 29.08: ProcessBuilder ffmpeg/ffprobe, encode/export/transcode/pipe, `getVideoInfo` |
| 4 | Миграция server-репозиториев на SQLDelight/PostgreSQL | ✅ Events (29.08) + Recording/Settings (04.09); in-memory удалён из DI; +23 теста |

## 🟡 Приоритет P1 — до тестовых сборок (остаток)

| № | Задача | Статус | Куда |
|---|--------|--------|------|
| 5 | Единый источник Android (дубль `android/app` vs `platforms/client-android/app`) | ✅ **Устранён (проверено 15.09)**: `platforms/client-android` = только `README.md`; CI собирает `:android:app:assembleDebug` (`ci.yml:200`) | **Этап 4 закрыт** |
| 6 | `google-services.json` (FCM) + `POST_NOTIFICATIONS` (API 33+) | ⬜ файла нет; серверный FCM HTTP v1 готов | **Этап 5** |
| 7 | Реальные уведомления (Telegram/WebPush/APNs/email) | 🟡 Web Push RFC 8291 ✅, FCM HTTP v1 ✅, LDAP ✅ (29.08–24.08); Telegram-канал доводка; APNs → iOS-трек | **Этап 5** |
| 8 | Реальные UseCase (убрать `UseCaseStubs.kt`) | ✅ Файл удалён 29.08, DI почищен | — |

## 🟢 Приоритет P2 — после тестовых сборок

| Задача | Статус | Куда |
|--------|--------|------|
| AI-агент до рабочего состояния (LLM, инструменты камер, авторизация) | 🟡 **14.09**: инфраструктура ~95% (FastAPI/WS/JWT); починены `pytest.ini` (дубль addopts+pythonpath), испорченный `memory/__init__.py`, bool-detect; авторизация вынесена в `.env`; **46/52 теста зелёные**; остаток: интеграция с Ktor backend (нет сервера — порт A) | Этап 10 |
| Согласовать expect/actual (iOS `UPnPDiscovery`) | ⬜ (SecurePasswordHasher ✅) | iOS-трек |
| Mobile security logger → сервер | ⬜ | Этап 5 |

