# План рефакторинга

**Версия проекта:** 0.5.1.1-beta
**Дата актуализации:** 04.09.2026 (первичная: 08.08.2026)
**Статус:** ключевая архитектурная задача закрыта; остатки — Этапы 4, 10 мастер-плана (`PLAN_EXECUTION_MASTER.md`)

---

## 1. Архитектурный разрыв «сервер ↔ нативные либы»
✅ **Закрыто 29.08**: JNI-мост интегрирован в `server:api` (`NativeAnalyticsFactory`, конвейер RTSP→кадры→JNI OpenCV MOG2/YOLO в `MotionDetectorService`/`ObjectDetectionService`/`VideoAnalyticsService`), graceful degradation без ffmpeg/нативной библиотеки. Стаб-папки `stub/opencv`, `stub/javacv` сервером более не используются как источник логики.

## 2. Пункты из `REFACTORING_PLAN.md` (перенесён в `archive/docs-deprecated-2026-09-04/`)

| Пункт | Статус |
|-------|--------|
| `java.time.Clock` → `kotlinx.datetime` (commonMain) | ✅ CameraRepositoryImplV2; остаток — пост-релиз |
| `java.security` → expect/actual (JVM/Android/iOS) | ✅ Гейт `check-security-expect-actual-signatures` проходит |
| JNA только в `jvmMain` | ✅ Проверено — нарушений нет |
| Опциональный cinterop Live555/FFmpeg (`-Pipcss.enableNativeInterop=true`) | ⬜ Опционально, пост-релиз |

## 3. Дубликаты

| Дубль | Статус |
|-------|--------|
| Два Android-приложения (`android/app` и `platforms/client-android/app`) | ✅ **Устранён (проверено 15.09):** `platforms/client-android/` = только `README.md`; `:android:app` — единый источник в `settings.gradle.kts`; CI android-джоба → `:android:app:assembleDebug` (`ci.yml:200`) |
| Дубли Model/DTO и репозиториев Shared ↔ Server | ⬜ Пост-релиз (Этап 10), после консолидации Android |

## 4. Структура модулей
✅ Задокументирована в `PROJECT_STRUCTURE.md` (v4.0): `:android:app`, `:server:api`, `:platforms:*` в Gradle; `native/*` и `ai-agent` — вне Gradle как отдельные артефакты. Сверка с фактом — в рамках Этапа 3 (линк-чек) и Этапа 4 (после удаления дубля).

