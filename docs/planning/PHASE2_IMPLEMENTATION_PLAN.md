# Детальный план реализации Этапа 2: Видео и Транспорт

**Дата:** 27 April 2026  
**Цель:** Закрытие критических компонент видео-пайплайна для MVP  
**Статус:** 🟡 В реализации

---

## 📊 Текущее состояние (Baseline)

| Компонент | Прогресс | Статус |
|-----------|----------|--------|
| **RTSP Native Integration** | ~58% | ⚠️ Критический блокер |
| **HLS Pipeline** | ~88% | 🟡 В процессе |
| **Recording Core** | ~86% | 🟡 В процессе |
| **Screenshot Pipeline** | ~54% | 🟡 В процессе |
| **Web Video Player** | ~75% | 🟡 В процессе |
| **Desktop Video Player** | ~62% | 🟡 В процессе |
| **Android Video** | ~48% | ⚠️ В процессе |

---

## 🎯 Цели Этапа 2

### P0 - Критические (2-3 недели)
1. **RTSP Native Integration** - полная интеграция с нативной C++ библиотекой
2. **HLS Runtime Stability** - long-run тесты, reconnect, cleanup
3. **Video E2E Gate** - profile-aware acceptance profile

### P1 - Высокий приоритет (1-2 недели)
4. **Screenshot Pipeline** - завершение `captureFrame()`
5. **Recording Core доводка** - WebSocket события, error handling

### P2 - Средний приоритет (1 неделя)
6. **Платформенная стабильность** - Android/Desktop smoke тесты

---

## 📋 Детальный план работ

### 2.1 RTSP Native Integration (P0)

#### 2.1.1 Проверка зависимостей и окружения
- [x] Проверка FFmpeg установки
- [x] Проверка CMake версии (≥ 3.15)
- [ ] Проверка pkg-config
- [ ] Проверка libavformat, libavcodec, libavutil

**Команды:**
```powershell
# Проверка FFmpeg
ffmpeg -version

# Проверка CMake
cmake --version

# Проверка pkg-config (Linux/macOS)
pkg-config --modversion libavformat
```

**Критерий:** Все зависимости установлены и проходят проверку

---

#### 2.1.2 Компиляция нативной библиотеки
**Файл:** `native/video-processing/CMakeLists.txt`

**Задачи:**
- [ ] Проверка CMake конфигурации
- [ ] Сборка для текущей платформы
- [ ] Проверка экспорта символов

**Команды:**
```powershell
cd native/video-processing
mkdir -p build && cd build
cmake .. -DENABLE_FFMPEG=ON -DENABLE_OPENCV=ON
cmake --build . --config Release

# Проверка символов
# Linux: nm -D libvideo_processing.so | grep rtsp_client
# macOS: nm -gU libvideo_processing.dylib | grep rtsp_client
# Windows: dumpbin /EXPORTS video_processing.dll | findstr rtsp_client
```

**Критерий:** Библиотека скомпилирована, все символы экспортируются

---

#### 2.1.3 Активация FFI биндингов
**Файл:** `core/network/src/nativeMain/kotlin/.../NativeRtspClient.native.kt`

**Задачи:**
- [ ] Раскомментировать импорт `rtsp_client.*`
- [ ] Раскомментировать использование нативных функций
- [ ] Реализовать callback'и с StableRef
- [ ] Протестировать компиляцию для всех платформ

**Критерий:** Код компилируется без ошибок на всех платформах

---

#### 2.1.4 Базовое тестирование
**Файл:** `core/network/src/commonTest/kotlin/.../RtspClientTest.kt`

**Задачи:**
- [ ] Unit тесты для create/destroy
- [ ] Unit тесты для connect/disconnect
- [ ] Интеграционные тесты с реальной камерой
- [ ] Проверка lifecycle (play/pause/stop)

**Критерий:** Все тесты проходят

---

### 2.2 HLS Runtime Stability (P0)

#### 2.2.1 Long-run тесты
**Команды:**
```powershell
.\scripts\video-e2e-go-no-go.ps1 -RunLongRunMatrix
```

**Проверяемые сценарии:**
- Длительный поток (30+ минут) без деградации
- Автоматическое переподключение при разрыве
- Очистка временных файлов после остановки
- Память и CPU не растут со временем

**Критерий:** Нет утечек памяти, нет деградации FPS

---

#### 2.2.2 Reconnect логика
**Файл:** `core/network/src/commonMain/kotlin/.../RtspClient.kt`

**Задачи:**
- [ ] Реализовать `setReconnectParams()`
- [ ] Добавить exponential backoff
- [ ] Добавить maxRetries и maxDelay
- [ ] Тестировать автоматическое переподключение

**Критерий:** Автоматическое переподключение при потере соединения

---

#### 2.2.3 Cleanup процессов и файлов
**Файл:** `server/api/src/main/kotlin/.../FfmpegService.kt`

**Задачи:**
- [ ] Добавить cleanup старых сегментов HLS
- [ ] Добавить cleanup процессов при остановке
- [ ] Добавить логику удаления при нехватке места

**Критерий:** Нет накопления старых сегментов HLS

---

### 2.3 Screenshot Pipeline (P1)

#### 2.3.1 Завершение `captureFrame()`
**Файл:** `server/api/src/main/kotlin/.../ScreenshotService.kt`

**Задачи:**
- [ ] Реализовать захват кадра из RTSP потока
- [ ] Интегрировать с FFmpeg для качественного захвата
- [ ] Добавить fallback метод без FFmpeg
- [ ] Протестировать с реальными камерами

**Критерий:** Кадр захватывается и сохраняется как валидный JPEG

---

#### 2.3.2 Интеграция с API
**Файл:** `server/api/src/main/kotlin/.../CameraRoutes.kt`

**Задачи:**
- [ ] Добавить endpoint `GET /api/v1/cameras/{id}/screenshot`
- [ ] Добавить обработку ошибок
- [ ] Добавить кеширование скриншотов

**Критерий:** API endpoint возвращает скриншот

---

### 2.4 Video E2E Gate (P0)

#### 2.4.1 Запуск video-e2e-go-no-go.ps1
**Команды:**
```powershell
.\scripts\video-e2e-go-no-go.ps1 -RunNetworkSmoke -RunLongRunMatrix
```

**Проверяемые контроли:**
- **1.8.A** Core network/runtime baseline
- **1.8.B** Long-run matrix scenario pass rate
- **1.8.C** Long-run checks pass rate
- **1.8.A1** PullPoint compatibility (опционально)
- **1.8.D** Canonical readiness (опционально)
- **1.8.E** Recording WS lifecycle (опционально)

**Критерий:** `Runtime decision = GO`, `Profile-aware = GO`

---

#### 2.4.2 Recording WS Lifecycle Acceptance
**Команды:**
```powershell
.\scripts\run-recording-ws-lifecycle-acceptance.ps1 `
  -ApiBase "http://localhost:8080" `
  -AdminUser "admin" `
  -AdminPassword "<pwd>" `
  -CameraId "cam-1"
```

**Проверяемые события:**
- `recording_started`
- `recording_paused`
- `recording_resumed`
- `recording_stopped`

**Критерий:** Все 4 события приходят через WebSocket

---

#### 2.4.3 ONVIF Events Verification
**Команды:**
```powershell
.\scripts\onvif-events-api-verification.ps1 `
  -ApiBase "http://localhost:8080" `
  -AdminUser "admin" `
  -AdminPassword "<pwd>"
```

**Критерий:** ONVIF события маппятся в события IP-CSS

---

### 2.5 Платформенная стабильность (P2)

#### 2.5.1 Android Video Stability
**Команды:**
```powershell
.\scripts\w4-mvp-platform-and-gate.ps1 -Platform android
```

**Проверяемые сценарии:**
- Live HLS через ExoPlayer с OkHttp
- Синхронизация cookies с Ktor
- Абсолютный URL плейлиста
- Переключатель RTSP/HLS

**Критерий:** Android проходит MVP smoke

---

#### 2.5.2 Desktop Video Stability
**Команды:**
```powershell
.\scripts\w4-mvp-platform-and-gate.ps1 -Platform desktop
```

**Проверяемые сценарии:**
- Long-run видео (30+ минут)
- Экраны событий (EventTimeline)
- Multi-camera optimization

**Критерий:** Desktop проходит MVP smoke

---

## 🗓️ Таймлайн

| Этап | Длительность | Приоритет |
|------|--------------|-----------|
| 2.1 RTSP Native Integration | 2-3 недели | P0 |
| 2.2 HLS Runtime Stability | 3-5 дней | P0 |
| 2.3 Screenshot Pipeline | 2-3 дня | P1 |
| 2.4 Video E2E Gate | 2-3 дня | P0 |
| 2.5 Платформенная стабильность | 3-5 дней | P2 |

**Итого:** 4-6 недель для полного закрытия

---

## 🚨 Критические зависимости

1. **Доступ к реальным IP-камерам** для тестирования
2. **Стабильная версия FFmpeg** для каждой платформы
3. **CMake/Native build tools** на всех платформах
4. **Тестовый RTSP сервер** (ffserver, vlc или оборудование)

---

## ✅ Метрики успеха

### Критерии готовности RTSP/runtime stability

- [ ] RTSP клиент подключается к камере
- [ ] Видео воспроизводится через плеер
- [ ] Задержка < 3 секунд (UDP транспорт)
- [ ] Поддержка H.264 кодека
- [ ] Автоматическое переподключение при разрыве
- [ ] Работает на Desktop
- [ ] Long-run тесты без деградации (30+ мин)
- [ ] Нет утечек памяти
- [ ] Cleanup процессов и файлов работает

### Критерии готовности Video E2E Gate

- [ ] `Runtime decision = GO`
- [ ] `Profile-aware release decision = GO`
- [ ] Контроль 1.8.A (Core network/runtime) = PASS
- [ ] Контроль 1.8.B (Long-run matrix) = PASS
- [ ] Контроль 1.8.C (Long-run checks) = PASS
- [ ] Recording WS lifecycle = PASS
- [ ] ONVIF Events verification = PASS
- [ ] Все платформы (Android/Desktop) = CONDITIONAL GO

---

**Начало реализации:** 27 April 2026  
**Ожидаемое завершение:** 27 June 2026  
**Текущий статус:** 🟡 В процессе
