# План реализации критических задач

**Дата:** 2026-05-24  
**Статус:** ✅ **RTSP СБОРКА ЗАВЕРШЕНА**  
**Приоритет:** 🔴 КРИТИЧЕСКИЙ (MVP Blockers)

---

## 📊 Текущий статус проекта

| Компонент | Прогресс | Статус | Блокер? |
|-----------|----------|--------|---------|
| PostgreSQL миграция | 100% | ✅ Завершено | ❌ НЕТ |
| RTSP клиент - нативная библиотека | 100% | ✅ Завершено | ❌ НЕТ |
| RTSP клиент - FFI биндинги | 0% | ⏳ Ожидает | ✅ ДА |
| Тестирование - покрытие | 25% | 🟡 В процессе | ⚠️ Частично |
| Видеоплеер - оптимизация | 75% | 🟡 В процессе | ⚠️ Частично |
| ONVIF Event Service | 95% | 🟡 В процессе | ❌ НЕТ |

---

## ✅ Выполненные компоненты

### 1. PostgreSQL миграция (100%)

**Реализовано:**
- ✅ PostgreSQL + HikariCP connection pooling
- ✅ Flyway миграции (V1-V5):
  - V1: Initial schema
  - V2: Performance indexes
  - V3: Server auth tables
  - V4: Audit log table
  - V5: Audit integrity chain
- ✅ DatabaseMonitoringService
- ✅ DatabaseBackupService
- ✅ BackupSchedulerService
- ✅ DatabaseRoutes
- ✅ Read replica поддержка
- ✅ JMX мониторинг
- ✅ Cluster heartbeat (Redis)

### 2. RTSP клиент - нативная библиотека (100%) ✅ НОВОЕ!

**Установленные зависимости:**
- ✅ FFmpeg 7.x (через vcpkg)
- ✅ CMake 3.x
- ✅ MinGW (GCC 15.2.0)

**Результаты сборки:**
- ✅ CMake конфигурация успешна
- ✅ Компиляция успешна
- ✅ DLL созданы: `video_processing.dll` + 7 FFmpeg DLL
- ✅ Символы экспортируются корректно (15+ функций)

**Собранные библиотеки:**
```
lib/windows/x64/
├── video_processing.dll      # Наша библиотека
├── avcodec-62.dll            # FFmpeg кодирование/декодирование
├── avformat-62.dll           # FFmpeg форматы
├── avutil-60.dll             # FFmpeg утилиты
├── swscale-9.dll             # FFmpeg масштабирование
├── swresample-6.dll          # FFmpeg ресемплинг аудио
├── avfilter-11.dll           # FFmpeg фильтры
└── avdevice-62.dll           # FFmpeg устройства
```

**Экспортируемые функции:**
- rtsp_client_create
- rtsp_client_destroy
- rtsp_client_connect
- rtsp_client_disconnect
- rtsp_client_play
- rtsp_client_stop
- rtsp_client_pause
- rtsp_client_set_frame_callback
- rtsp_client_set_status_callback
- rtsp_client_set_reconnect_params
- rtsp_client_get_stream_count
- rtsp_client_get_stream_info
- rtsp_client_get_stream_type
- rtsp_client_get_status

**Документация:**
- `docs/reports/RTSP_BUILD_SUCCESS_2026-05-24.md`
- `docs/reports/RTSP_INTEGRATION_FINAL_REPORT_2026-05-24.md`
- `docs/reports/FINAL_IMPLEMENTATION_REPORT_2026-05-24.md`
- ✅ Fail-fast валидация для production

**Файлы:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/config/DatabaseConfig.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/config/DatabaseMigrationConfig.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/service/DatabaseMonitoringService.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/service/DatabaseBackupService.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/service/BackupSchedulerService.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/routing/DatabaseRoutes.kt`
- `server/api/src/main/resources/db/migration/V1__Initial_schema.sql`
- `server/api/src/main/resources/db/migration/V2__Add_performance_indexes.sql`
- `server/api/src/main/resources/db/migration/V3__Add_server_auth_tables.sql`
- `server/api/src/main/resources/db/migration/V4__Add_audit_log_table.sql`
- `server/api/src/main/resources/db/migration/V5__Add_audit_log_integrity_chain.sql`

**Дополнительные возможности:**
- ✅ Аутентификация сервера (password hash, refresh tokens)
- ✅ TOTP 2FA поддержка (user_totp таблица)
- ✅ Security audit logging (audit_log с tamper-evident hash chaining)

---

### 2. KMP проверки (100%)

**Результаты:**
- ✅ Forbidden imports check - PASSED
- ✅ Security expect/actual signatures - PASSED (23/23 files)
- ✅ No JVM deps in native source sets - PASSED
- ✅ Video runtime matrix config - PASSED
- ✅ Video E2E profile validation - PASSED

---

## ⚠️ Критические задачи (в процессе)

### 1. RTSP клиент - полная интеграция

**Текущий прогресс:** ~85%

**Реализовано:**
- ✅ Нативная C++ реализация (`rtsp_client.cpp` - ~1500 строк)
- ✅ RTSP протокол (OPTIONS, DESCRIBE, SETUP, PLAY, PAUSE, TEARDOWN)
- ✅ RTP/RTCP обработка
- ✅ H.264/H.265 декодирование (FFmpeg)
- ✅ Аудио декодирование (AAC, PCMU, PCMA)
- ✅ Digest Authentication
- ✅ Автоматическое переподключение
- ✅ Поддержка множественных потоков
- ✅ TCP/UDP транспорт
- ✅ Kotlin обертка (`RtspClient.kt`)
- ✅ CMake конфигурация
- ✅ Скрипты сборки

**Осталось:**
- [ ] **КРИТИЧЕСКИЙ БЛОКЕР:** Тестирование с реальными RTSP камерами
- [ ] **КРИТИЧЕСКИЙ БЛОКЕР:** Интеграционные тесты
- [ ] **КРИТИЧЕСКИЙ БЛОКЕР:** Аудио декодирование (FFmpeg 8.0 API проблемы)
- [ ] Оптимизация производительности
- [ ] Обработка различных кодеков (H.264, H.265, MJPEG)
- [ ] FFI биндинги для всех платформ

**Зависимости:**
- FFmpeg 4.x/5.x/6.x (libavformat, libavcodec, libavutil, libswscale, libswresample)
- CMake 3.15+
- pkg-config (Linux/macOS)

---

### 2. Видеоплеер - оптимизация

**Текущий прогресс:** ~75%

**Реализовано:**
- ✅ HlsGeneratorService
- ✅ VideoStreamService
- ✅ VideoPlayer компонент (HLS.js)
- ✅ 4 уровня качества
- ✅ PTZ управление
- ✅ ScreenshotService

**Осталось:**
- [ ] Оптимизация буферизации для низкой задержки
- [ ] WebRTC поддержка (ultra-low latency)
- [ ] ABR (Automatic Bitrate Adjustment)
- [ ] Обработка потерь пакетов

---

### 3. Тестирование - увеличение покрытия

**Текущий прогресс:** ~25%

**Реализовано:**
- ✅ Unit тесты для Use Cases
- ✅ Базовые тесты для репозиториев
- ✅ KMP Phase 1 проверки
- ✅ RTSP клиент тесты (модульные)

**Осталось:**
- [ ] Integration тесты для API endpoints
- [ ] E2E тесты для основных сценариев
- [ ] RTSP интеграционные тесты
- [ ] PostgreSQL интеграционные тесты
- [ ] Long-run stability тесты
- [ ] Покрытие 50%+

---

## 📋 План реализации

### Этап 1: RTSP клиент (2-3 недели)

**Цель:** Базовая рабочая интеграция RTSP клиента

**Задачи:**
1. **Сборка нативной библиотеки** (3-5 дней)
   - [ ] Проверить установку FFmpeg зависимостей
   - [ ] Скомпилировать библиотеку для Desktop (Windows/Linux/macOS)
   - [ ] Проверить экспорт символов
   - [ ] Скопировать библиотеки в правильные директории

2. **Интеграция FFI биндингов** (3-5 дней)
   - [ ] Проверить cinterop конфигурацию
   - [ ] Сгенерировать биндинги (`compileKotlinNative`)
   - [ ] Исправить проблемы с типами
   - [ ] Протестировать на Desktop платформе

3. **Аудио декодирование** (3-5 дней)
   - [ ] Исправить FFmpeg 8.0 API проблемы
   - [ ] Реализовать AAC декодирование
   - [ ] Реализовать G.711 (PCMU/PCMA) декодирование
   - [ ] Протестировать аудио потоки

4. **Интеграционные тесты** (5-7 дней)
   - [ ] Настроить тестовую среду с RTSP сервером
   - [ ] Создать тесты с реальными камерами
   - [ ] Проверить различные кодеки
   - [ ] Протестировать переподключение
   - [ ] Проверить обработку ошибок

**Критерии успеха:**
- ✅ RTSP клиент подключается к реальным камерам
- ✅ Видео поток воспроизводится стабильно
- ✅ Аудио поток декодируется (если доступно)
- ✅ Автоматическое переподключение работает
- ✅ Обработка ошибок корректна

---

### Этап 2: Видеоплеер - оптимизация (1-2 недели)

**Цель:** Низкая задержка и стабильность

**Задачи:**
1. **Оптимизация буферизации** (3-5 дней)
   - [ ] Уменьшить размер буфера
   - [ ] Реализовать adaptive buffering
   - [ ] Настроить HLS сегменты (targetDuration)
   - [ ] Тестирование задержки

2. **Обработка ошибок** (3-5 дней)
   - [ ] Реализовать retry logic
   - [ ] Обработка потери пакетов
   - [ ] Автоматическое восстановление
   - [ ] Индикаторы состояния

**Критерии успеха:**
- ✅ Задержка < 3 секунд
- ✅ Стабильное воспроизведение
- ✅ Автоматическое восстановление

---

### Этап 3: Тестирование (постоянно)

**Цель:** Покрытие 50%+

**Задачи:**
1. **Integration тесты** (2-3 недели)
   - [ ] API endpoints
   - [ ] PostgreSQL CRUD
   - [ ] WebSocket события
   - [ ] RTSP интеграция

2. **E2E тесты** (2-3 недели)
   - [ ] Discovery → Connect → Playback
   - [ ] Recording workflow
   - [ ] Event detection
   - [ ] User authentication

**Критерии успеха:**
- ✅ Покрытие 50%+
- ✅ Все критические сценарии покрыты
- ✅ CI pipeline настроен

---

## 🔧 Зависимости и требования

### FFmpeg установка

**Windows:**
```powershell
# Используем vcpkg
cd native/vcpkg
.\vcpkg install ffmpeg:x64-windows
.\vcpkg integrate install
```

**Linux:**
```bash
sudo apt-get install cmake build-essential pkg-config \
    libavformat-dev libavcodec-dev libavutil-dev \
    libswscale-dev libswresample-dev
```

**macOS:**
```bash
brew install cmake ffmpeg pkg-config
```

### Тестовые камеры

Для тестирования RTSP клиента требуются:
- [ ] RTSP сервер (FFmpeg, GStreamer или реальная IP-камера)
- [ ] Тестовые видеопотоки (H.264, H.265, MJPEG)
- [ ] Аудио потоки (AAC, PCMU, PCMA)

---

## 📅 Ожидаемые сроки

| Этап | Длительность | Статус |
|------|--------------|--------|
| Этап 1: RTSP клиент | 2-3 недели | В процессе |
| Этап 2: Видеоплеер | 1-2 недели | Не начато |
| Этап 3: Тестирование | 4-6 недель | Не начато |
| **Итого** | **7-11 недель** | |

---

## 🎯 Критерии MVP готовности

- [x] PostgreSQL миграция завершена
- [ ] RTSP клиент работает с реальными камерами
- [ ] Видеоплеер с низкой задержкой (< 3 сек)
- [ ] Покрытие тестами 50%+
- [ ] ONVIF Event Service протестирован
- [ ] Базовые сценарии E2E работают
- [ ] Production smoke тесты пройдены

---

**Следующий шаг:** Сборка нативной библиотеки и тестирование FFI биндингов

**Дата последнего обновления:** 2026-04-27
