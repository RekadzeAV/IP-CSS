# План завершения Этапа 2: Детальный анализ оставшихся задач

**Дата:** 2026-04-28  
**Текущий прогресс Этапа 2:** 55% → **Цель: 95%**

---

## 📊 Текущий статус (Baseline)

| Компонент | Прогресс | Статус | Остаток до 100% |
|-----------|----------|--------|-----------------|
| **2.1 RTSP Native Integration** | 90% | ✅ Готово | 10% (дополнительные тесты) |
| **2.2 HLS Runtime Stability** | 88% | ✅ Готово | 12% (long-run тесты) |
| **2.3 Screenshot Pipeline** | 85% | ✅ Готово | 15% (интеграционные тесты) |
| **2.4 Video E2E Gate** | 66% | 🟡 PARTIAL | 34% (Recording WS lifecycle) |
| **2.5 Платформенная стабильность** | 0% | ⚪ Не начато | 100% (Linux/macOS) |

**Общий прогресс Этапа 2:** 55% → Цель: 95%

---

## 🎯 Оставшиеся задачи по компонентам

### 2.1 RTSP Native Integration (90% → 100%)

**Оставшиеся задачи:**

#### P1 - Дополнительные тесты (2-3 дня)
- [ ] Добавить integration тесты с mock RTSP сервером
- [ ] Добавить тесты на reconnect логика
- [ ] Добавить тесты на callback обработку
- [ ] Добавить тесты на error handling

**Файлы:**
- `core/network/src/commonTest/kotlin/.../RtspClientTest.kt`
- `core/network/src/commonTest/kotlin/.../RtspClientReconnectTest.kt`

**Критерий готовности:** 100% coverage для основных сценариев

---

### 2.2 HLS Runtime Stability (88% → 100%)

**Оставшиеся задачи:**

#### P0 - Long-run тесты (3-5 дней)
- [x] ✅ Long-run matrix уже запущен (PASS 100%)
- [ ] Добавить тесты на memory leak (30+ минут)
- [ ] Добавить тесты на CPU usage
- [ ] Добавить тесты на cleanup старых сегментов

**Файлы:**
- `scripts/video-runtime-longrun-smoke.ps1`
- `diagnostics/video-longrun-matrix/`

**Критерий готовности:**
- Нет memory leak > 100MB за 30 минут
- CPU usage < 30%
- Cleanup сегментов работает корректно

#### P1 - Reconnect логика (2-3 дня)
- [ ] Добавить тесты на exponential backoff
- [ ] Добавить тесты на jitter
- [ ] Добавить тесты на maxRetries

**Файлы:**
- `core/network/src/commonTest/kotlin/.../RtspClientReconnectTest.kt`

---

### 2.3 Screenshot Pipeline (85% → 100%)

**Оставшиеся задачи:**

#### P0 - Интеграционные тесты (2-3 дня)
- [ ] Добавить тесты с реальным RTSP потоком
- [ ] Добавить тесты на качество JPEG
- [ ] Добавить тесты на разные кодеки (H.264, H.265, MJPEG)
- [ ] Добавить тесты на fallback без FFmpeg

**Файлы:**
- `server/api/src/test/kotlin/.../ScreenshotServiceIntegrationTest.kt`

**Критерий готовности:**
- Все тесты проходят
- Качество JPEG > 80% (SSIM)
- Fallback метод работает

#### P1 - API endpoints (1 день)
- [x] ✅ Endpoint уже реализован (`POST /api/v1/cameras/{id}/screenshot`)
- [ ] Добавить кеширование скриншотов (5 минут)
- [ ] Добавить throttling (max 10 requests/minute)

---

### 2.4 Video E2E Gate (66% → 95%)

**Оставшиеся задачи:**

#### P0 - Recording WS Lifecycle (3-5 дней)
- [ ] Запустить `run-recording-ws-lifecycle-acceptance.ps1`
- [ ] Проверить все 4 события:
  - [ ] `recording_started`
  - [ ] `recording_paused`
  - [ ] `recording_resumed`
  - [ ] `recording_stopped`
- [ ] Исправить ошибки если есть

**Файлы:**
- `scripts/run-recording-ws-lifecycle-acceptance.ps1`
- `diagnostics/recording-ws-acceptance/`

**Критерий готовности:**
- `recordingWsOverall = PASS`
- Все 4 события приходят через WebSocket

#### P1 - Canonical weighted readiness (2-3 дня)
- [ ] Увеличить покрытие тестами до 70%+
- [ ] Добавить интеграционные тесты для всех компонент
- [ ] Добавить E2E сценарии

**Критерий готовности:**
- `Canonical 1.8 readiness >= 70%`

---

### 2.5 Платформенная стабильность (0% → 80%)

**Оставшиеся задачи:**

#### P1 - Linux сборка (3-5 дней)
- [ ] Настроить CMake для Linux
- [ ] Скомпилировать нативную библиотеку для Linux
- [ ] Собрать JVM artifacts
- [ ] Протестировать на Linux

**Команды:**
```bash
cd native/video-processing
mkdir -p build-linux && cd build-linux
cmake .. -DENABLE_FFMPEG=ON -DENABLE_OPENCV=OFF
make -j$(nproc)
```

**Критерий готовности:**
- `libvideo_processing.so` скомпилирована
- Сборка проходит без ошибок

#### P1 - macOS сборка (3-5 дней)
- [ ] Настроить CMake для macOS
- [ ] Скомпилировать нативную библиотеку для macOS
- [ ] Собрать JVM artifacts
- [ ] Протестировать на macOS

**Команды:**
```bash
cd native/video-processing
mkdir -p build-macos && cd build-macos
cmake .. -DENABLE_FFMPEG=ON -DENABLE_OPENCV=OFF
make -j$(sysctl -n hw.ncpu)
```

**Критерий готовности:**
- `libvideo_processing.dylib` скомпилирована
- Сборка проходит без ошибок

#### P2 - Desktop smoke тесты (2-3 дня)
- [x] ✅ Desktop compile + tests уже проходят
- [ ] Добавить runtime long-run тесты для Desktop
- [ ] Добавить тесты на UI стабильность

---

## 🗓️ Таймлайн выполнения

| Неделя | Задачи | Ожидаемый прогресс |
|--------|--------|-------------------|
| **Неделя 1** | RTSP тесты, HLS long-run тесты | 55% → 70% |
| **Неделя 2** | Screenshot интеграционные тесты, Recording WS | 70% → 85% |
| **Неделя 3** | Linux/macOS сборка | 85% → 95% |
| **Неделя 4** | Финальные тесты, документация | 95% → 100% |

**Итого:** 3-4 недели для полного закрытия Этапа 2

---

## 🚨 Критические зависимости

1. **Доступ к тестовой среде** для Recording WS Lifecycle
2. **Linux/macOS окружение** для кроссплатформенной сборки
3. **FFmpeg для всех платформ** (Windows, Linux, macOS)

---

## ✅ Метрики успеха для Этапа 2

### Критерии готовности 100%

- [x] RTSP Native Integration: библиотека компилируется ✅
- [x] RTSP Native Integration: Kotlin обертка работает ✅
- [x] HLS Runtime Stability: long-run тесты проходят ✅
- [x] Screenshot Pipeline: captureFrame() работает ✅
- [ ] Recording WS Lifecycle: все 4 события PASS
- [ ] Canonical readiness >= 70%
- [ ] Linux сборка PASS
- [ ] macOS сборка PASS
- [ ] Desktop runtime long-run PASS

### Приоритетные критерии для GO-решения (P0)

- [x] Runtime decision = GO ✅
- [x] Profile-aware release decision = GO ✅
- [x] 1.8.A (Core network/runtime) = PASS ✅
- [x] 1.8.B (Long-run matrix) = PASS ✅
- [x] 1.8.C (Long-run checks) = PASS ✅
- [ ] 1.8.E (Recording WS lifecycle) = PASS
- [ ] 1.8.D (Canonical readiness >= 70%)

---

## 📋 План автоматической реализации

### День 1-2: RTSP дополнительные тесты
```powershell
# Запуск существующих тестов
.\gradlew.bat :core:network:test

# Создание новых тестов
# - RtspClientReconnectTest.kt
# - RtspClientCallbackTest.kt
```

### День 3-5: HLS long-run тесты
```powershell
# Запуск long-run smoke
.\scripts\video-runtime-longrun-smoke.ps1 `
  -BaseUrl "http://localhost:8080" `
  -DurationMinutes 30 `
  -IntervalSec 60
```

### День 6-8: Screenshot интеграционные тесты
```powershell
# Запуск существующих тестов
.\gradlew.bat :server:api:test --tests "*ScreenshotServiceTest*"

# Создание integration тестов
# - ScreenshotServiceIntegrationTest.kt
```

### День 9-12: Recording WS Lifecycle
```powershell
# Запуск acceptance тестов
.\scripts\run-recording-ws-lifecycle-acceptance.ps1 `
  -ApiBase "http://localhost:8080" `
  -AdminUser "admin" `
  -AdminPassword "<pwd>" `
  -CameraId "cam-1"
```

### День 13-17: Linux/macOS сборка
```bash
# Linux
cd native/video-processing
./build-linux.sh

# macOS
cd native/video-processing
./build-macos.sh
```

### День 18-20: Финальные тесты и документация
```powershell
# Запуск полного Video E2E Gate
.\scripts\video-e2e-go-no-go.ps1 -RunNetworkSmoke -RunLongRunMatrix

# Обновление документации
```

---

## 🔗 Связанные документы

- **[PHASE2_IMPLEMENTATION_STATUS_2026-04-27_FINAL.md](PHASE2_IMPLEMENTATION_STATUS_2026-04-27_FINAL.md)** - Текущий статус
- **[PHASE2_COMPLETION_REPORT_2026-04-27.md](PHASE2_COMPLETION_REPORT_2026-04-27.md)** - Отчет о завершении
- **[video-e2e-go-no-go-report.md](../../release-build/test/video-e2e-go-no-go-report.md)** - Текущий E2E отчет

---

**Начало реализации:** 28 April 2026  
**Ожидаемое завершение:** 28 May 2026  
**Текущий статус:** 🟡 В процессе
