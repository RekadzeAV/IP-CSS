# Единый перечень TODO проекта IP-CSS

**Дата составления:** 1 марта 2026  
**Источники:** поиск по кодовой базе (`// TODO`, `* TODO`, `**TODO**`), `docs/TECHNICAL_DEBT.md`, документация.

---

## Сводка

| Категория | Кол-во | Приоритет |
|-----------|--------|-----------|
| RTSP / видео | 15+ | Высокий |
| Лицензирование | 10 | Высокий |
| Сервер (БД, репозитории) | 8 | Критический |
| Аналитика (правила, скриншоты) | 8 | Средний |
| Нативная аналитика (YOLO, OCR, кодеки) | 8 | Средний |
| Платформы (Android, iOS, Desktop) | 14+ | Средний |
| Безопасность и мониторинг | 4 | Высокий |
| Документация / инструкции | 5+ | Низкий |
| **Всего уникальных задач** | **~70+** | — |

---

## 1. RTSP и видео

### 1.1 RtspClient (Kotlin)

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/RtspClient.kt`

| № | Строка | Описание |
|---|--------|----------|
| 1 | 128 | Интеграция с нативной библиотекой RTSP (native/video-processing) |
| 2 | 184 | Интеграция с нативной библиотекой RTSP — вызов `rtsp_client_play()` |
| 3 | 205 | Интеграция с нативной библиотекой RTSP — вызов `rtsp_client_stop()` |
| 4 | 225 | Интеграция с нативной библиотекой RTSP — вызов `rtsp_client_pause()` |
| 5 | 241 | Интеграция с нативной библиотекой RTSP — вызов `rtsp_client_disconnect()` |
| 6 | 289 | Интеграция с нативной библиотекой RTSP для получения кадров |

### 1.2 VideoDecoder

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/video/VideoDecoder.kt`

| № | Строка | Описание |
|---|--------|----------|
| 7 | 104 | Конвертация YUV420 в RGB |

### 1.3 Нативный RTSP (RTP)

**Файл:** `native/video-processing/src/rtsp_client.cpp`

| № | Строка | Описание |
|---|--------|----------|
| 8 | 2521 | Добавить MTAP16 (26), MTAP24 (27), FU-B (29) — используются реже |
| 9 | 2596 | Добавить MTAP16 (50), MTAP24 (51) для H.265 — используются реже |

### 1.4 Тесты VideoDecoder

**Файл:** `core/network/src/jvmTest/kotlin/com/company/ipcamera/core/network/video/VideoDecoderPerformanceTest.kt`

| № | Строка | Описание |
|---|--------|----------|
| 10 | 65 | Заменить на реальные кадры из камеры или тестового файла |
| 11 | 102 | Декодирование тестовых кадров |
| 12 | 134 | Декодирование кадров |
| 13 | 159 | Тестирование с поврежденными кадрами |

---

## 2. Лицензирование

### 2.1 LicenseManager (common)

**Файл:** `core/license/src/commonMain/kotlin/com/company/ipcamera/core/license/LicenseManager.kt`

| № | Строка | Описание |
|---|--------|----------|
| 14 | 114 | Реализовать онлайн активацию согласно спецификации лицензирования |
| 15 | 122 | Реализовать офлайн активацию согласно спецификации лицензирования |
| 16 | 240 | Реализовать проверку целостности лицензии |

### 2.2 LicenseManager (Android)

**Файл:** `core/license/src/androidMain/kotlin/com/company/ipcamera/core/license/LicenseManager.android.kt`

| № | Строка | Описание |
|---|--------|----------|
| 17 | 21 | Получить Android ID через Settings.Secure.ANDROID_ID |
| 18 | 39 | Реализовать полную расшифровку через Android Keystore |
| 19 | 48 | Реализовать периодическую проверку лицензии через WorkManager |
| 20 | 104 | Реализовать дополнительное шифрование через Android Keystore |

### 2.3 LicenseManager (iOS)

**Файл:** `core/license/src/iosMain/kotlin/com/company/ipcamera/core/license/LicenseManager.ios.kt`

| № | Строка | Описание |
|---|--------|----------|
| 21 | 22 | Реализовать полную расшифровку через iOS Keychain Services |
| 22 | 31 | Реализовать периодическую проверку лицензии через iOS Background Tasks |

---

## 3. Сервер: репозитории и БД

**Статус (март 2026):**
- **23** ✅ Реализовано: при `DATABASE_URL` используется `ServerUserRepositoryPostgres` (таблицы `user`, `user_password_hash`, `refresh_token`, `user_totp` — миграция Flyway V3).
- **24–25** ✅ EventRepository и SettingsRepository берутся из shared и при `DATABASE_URL` пишут в ту же PostgreSQL через общий драйвер.
- **26** ✅ На сервере `RecordingRepository` — `ServerRecordingRepositorySqlDelight` (общий `CameraDatabase`).
- **27** ✅ Подписанные URL с истечением срока: реализованы в `SignedUrlService` и используются в `RecordingRoutes`.
- **28** ✅ Экспорт в формате/качестве: реализован в `RecordingRoutes` через `FfmpegService.exportVideo`.

| № | Файл | Описание |
|---|------|----------|
| 23 | `ServerUserRepositoryPostgres.kt` + Flyway V3 | Миграция на PostgreSQL выполнена |
| 24 | shared + DATABASE_URL | EventRepository уже использует общую БД |
| 25 | shared + DATABASE_URL | SettingsRepository уже использует общую БД |
| 26 | `ServerRecordingRepositorySqlDelight` | Используется как `RecordingRepository` на сервере |
| 27 | `SignedUrlService` + `RecordingRoutes` | Подписанные URL с истечением срока действия |
| 28 | `RecordingRoutes` POST `/export` + `FfmpegService` | Экспорт в формате и качестве |

---

## 4. Аналитика (сервер и правила)

### 4.1 AnalyticsRuleService

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/service/AnalyticsRuleService.kt`

| № | Строка | Описание |
|---|--------|----------|
| 29 | 263 | Проверка временного окна и дней недели |
| 30 | 264 | Проверка зон |
| 31 | 318 | Запуск записи |
| 32 | 319 | Отправка webhook |

### 4.2 ScreenshotService

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/service/ScreenshotService.kt`

| № | Строка | Описание |
|---|--------|----------|
| 33 | 44 | Реализовать декодирование H.264 кадра в BufferedImage |

---

## 5. Нативная аналитика (C++)

### 5.1 Object Detector (YOLO)

**Файл:** `native/analytics/src/object_detector.cpp`

| № | Строка | Описание |
|---|--------|----------|
| 34 | 154 | Реализовать полный парсинг выходов YOLO модели |

### 5.2 ANPR (Tesseract OCR)

**Файл:** `native/analytics/src/anpr_engine.cpp`

| № | Строка | Описание |
|---|--------|----------|
| 35 | 55 | Инициализация Tesseract OCR библиотеки |
| 36 | 109 | Распознавание текста номера через Tesseract OCR |

### 5.3 Codec Manager

**Файл:** `native/codecs/src/codec_manager.cpp`

| № | Строка | Описание |
|---|--------|----------|
| 37 | 21 | Реализовать проверку аппаратного ускорения через FFmpeg |
| 38 | 35 | Реализовать проверку аппаратного ускорения через FFmpeg |
| 39 | 118 | Реализовать проверку аппаратного ускорения для различных платформ |

---

## 6. Платформы: Android

### 6.1 RecordingService

**Файл:** `android/app/src/main/java/com/company/ipcamera/android/service/RecordingService.kt`

| № | Строка | Описание |
|---|--------|----------|
| 40 | 267 | Реализовать паузу записи (требует поддержки в VideoRecordingService) |
| 41 | 286 | Реализовать возобновление записи |
| 42 | 314 | Интегрировать с VideoRecordingService из shared модуля |

### 6.2 ServiceManager

**Файл:** `android/app/src/main/java/com/company/ipcamera/android/service/ServiceManager.kt`

| № | Строка | Описание |
|---|--------|----------|
| 43 | 69 | Использовать callback или Flow для ожидания подключения |

### 6.3 CameraMonitoringService

**Файл:** `android/app/src/main/java/com/company/ipcamera/android/service/CameraMonitoringService.kt`

| № | Строка | Описание |
|---|--------|----------|
| 44 | 222 | Реализовать проверку доступности через RTSP или HTTP |

### 6.4 ExoVideoPlayer

**Файл:** `android/app/src/main/java/com/company/ipcamera/android/ui/components/ExoVideoPlayer.kt`

| № | Строка | Описание |
|---|--------|----------|
| 45 | 123 | Получать serverBaseUrl из конфигурации (сейчас захардкожен localhost:8080) |

### 6.5 CameraDetailScreen

**Файл:** `android/app/src/main/java/com/company/ipcamera/android/ui/screens/camera/CameraDetailScreen.kt`

| № | Строка | Описание |
|---|--------|----------|
| 46 | 141 | Показать результат (Show result) |

### 6.6 AppNavigation

**Файл:** `android/app/src/main/java/com/company/ipcamera/android/ui/navigation/AppNavigation.kt`

| № | Строка | Описание |
|---|--------|----------|
| 47 | 92 | Реализовать RecordingPlaybackScreen |

### 6.7 NotificationManager (Android)

**Файл:** `shared/src/androidMain/kotlin/com/company/ipcamera/shared/common/NotificationManager.android.kt`

| № | Строка | Описание |
|---|--------|----------|
| 48 | 199 | Реализовать загрузку кастомных иконок из ресурсов Android |
| 49 | 205 | Реализовать загрузку иконок действий из ресурсов Android |
| 50 | 225 | Реализовать навигацию к экрану уведомлений через Navigation Component |
| 51 | 235 | Реализовать обработку действия из уведомления |

---

## 7. Платформы: iOS

### 7.1 BackgroundWorker (iOS)

**Файл:** `shared/src/iosMain/kotlin/com/company/ipcamera/shared/common/BackgroundWorker.ios.kt`

| № | Строка | Описание |
|---|--------|----------|
| 52 | 133 | Реализовать проверку ограничений через iOS системные API |
| 53 | 141 | Реализовать регистрацию фоновых задач через BGTaskScheduler |

---

## 8. Платформы: Desktop

### 8.1 BackgroundWorker (Desktop)

**Файл:** `shared/src/desktopMain/kotlin/com/company/ipcamera/shared/common/BackgroundWorker.desktop.kt`

| № | Строка | Описание |
|---|--------|----------|
| 54 | 155 | Реализовать проверку сетевого подключения для Desktop |
| 55 | 161 | Реализовать проверку свободного места на диске для Desktop |

### 8.2 NotificationManager (Desktop)

**Файл:** `shared/src/desktopMain/kotlin/com/company/ipcamera/shared/common/NotificationManager.desktop.kt`

| № | Строка | Описание |
|---|--------|----------|
| 56 | 93 | Реализовать поддержку действий для Desktop уведомлений |
| 57 | 146 | Реализовать загрузку кастомных иконок для уведомлений |

---

## 9. UPnP / обнаружение устройств

| № | Файл | Строка | Описание |
|---|------|--------|----------|
| 58 | `core/network/src/iosMain/.../UPnPDiscovery.ios.kt` | 11 | Реализовать через NSURLSession или CFNetwork для iOS |
| 59 | `core/network/src/nativeMain/.../UPnPDiscovery.native.kt` | 11 | Реализовать через нативные сокеты для Linux/macOS/Windows |

---

## 10. ONVIF

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifClient.kt`

| № | Строка | Описание |
|---|--------|----------|
| 60 | 619 | Определить поддержку audio из ONVIF профилей |

---

## 11. Безопасность и мониторинг

### 11.1 Отправка критических событий на сервер

| № | Файл | Строка | Описание |
|---|------|--------|----------|
| 61 | `core/common/src/androidMain/.../MobileSecurityLogger.android.kt` | 29 | Отправить критическое событие на сервер для мониторинга |
| 62 | `core/common/src/iosMain/.../MobileSecurityLogger.ios.kt` | 29 | Отправить критическое событие на сервер для мониторинга |

*Дублируется в `docs/MOBILE_SECURITY.md`: реализовать отправку критических событий на сервер для централизованного мониторинга.*

---

## 12. Видеозапись и файлы (документация)

**Файл:** `docs/VIDEO_RECORDING_IMPLEMENTATION.md`

| № | Описание |
|---|----------|
| 63 | Удаление файлов записи и thumbnail'ов при удалении записи |
| 64 | Интегрировать FFmpeg для кодирования видео или использовать native/video-processing |
| 65 | Реализовать правильную запись в выбранный формат |
| 66 | Реализовать сохранение записей в БД, индексы, миграции |
| 67 | Проверка доступного места, поддержка S3/NFS, квоты на запись |

---

## 13. Документация и инструкции (ссылки на TODO)

- **docs/RTSP_QUICK_START.md** — раскомментировать реализацию методов (`// TODO: После компиляции cinterop...`).
- **docs/RTSP_CLIENT.md** — то же для RTSP клиента.
- **docs/rtsp/ACTIVATION.md** — раскомментировать реализацию по `// TODO: После компиляции cinterop...`.
- **docs/SOLUTIONS_FOR_DISCREPANCIES.md** — серверная часть ещё не реализована (контекст расхождений).
- **docs/MISSING_FUNCTIONALITY.md** — описание отсутствующей функциональности (online/offline активация лицензий и т.д.).

---

## 14. Связь с TECHNICAL_DEBT.md

Расширенный технический долг (безопасность, CORS, HTTPS, certificate pinning, Docker, покрытие тестами и т.д.) описан в:

- **docs/TECHNICAL_DEBT.md** — 142+ задач, включая упомянутые выше TODO.

Данный файл фокусируется на **явных TODO в коде и в ключевой документации**; общая стратегия и приоритеты — в `TECHNICAL_DEBT.md`.

---

## Рекомендуемый порядок работ

1. **Критично:** миграция серверных репозиториев на SQLDelight/PostgreSQL (п. 3).
2. **Высоко:** интеграция RTSP с нативной библиотекой (п. 1.1), лицензирование — Keystore/Keychain и активация (п. 2).
3. **Средне:** аналитика — правила (время, зоны, запись, webhook), скриншоты H.264 (п. 4); нативная аналитика — YOLO, OCR, аппаратное ускорение (п. 5).
4. **Средне:** платформенные задачи — запись на Android, уведомления, BackgroundWorker, ONVIF audio (п. 6–8, 10).
5. **Низко:** UPnP (п. 9), тесты VideoDecoder (п. 1.4), документация (п. 13).

Если нужно, могу сгенерировать список в формате задач (например, для GitHub Issues) или разбить по спринтам.
