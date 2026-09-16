# План работ по завершению Фазы 2 (95% → 100%)

**Дата создания:** 2026-04-28  
**Текущий статус:** 95%  
**Цель:** 100%  
**Оценка времени:** 7-10 рабочих дней

---

## 📊 Текущее состояние (Baseline)

###已完成 задачи (95%)

| Компонент | Прогресс | Статус | Документ-подтверждение |
|-----------|----------|--------|------------------------|
| 2.1 RTSP Native Integration | 90% | ✅ ГОТОВО | `PHASE2_RTSP_INTEGRATION_FIX_2026-04-27.md` |
| 2.2 HLS Runtime Stability | 88% | ✅ ГОТОВО | Long-run matrix PASS |
| 2.3 Screenshot Pipeline | 85% | ✅ ГОТОВО | `ScreenshotServiceTest` PASS |
| 2.4 Video E2E Gate | 95% | ✅ ГОТОВО | Runtime=GO, Profile-aware=GO |
| 2.5 Платформенная стабильность | 60% | 🟡 PARTIAL | Windows сборка PASS |

### Ключевые метрики

- **Runtime decision:** ✅ GO
- **Profile-aware release decision:** ✅ GO  
- **1.8.A Core network/runtime:** ✅ PASS
- **1.8.B Long-run matrix:** ✅ PASS
- **1.8.C Long-run checks:** ✅ PASS
- **1.8.D Canonical readiness:** 🟡 66% (цель: ≥70%)
- **1.8.E Recording WS lifecycle:** ⚠️ CONDITIONAL (требует сервер)
- **Weighted readiness:** 85.9% (цель: ≥90%)

---

## 🎯 Декомпозиция работ по задачам

### P0 - Критические задачи (обязательны для 100%)

#### Задача 1: Recording WS Lifecycle тестирование
**ID:** PHASE2-P0-1  
**Приоритет:** P0 (BLOCKER)  
**Оценка:** 2-3 дня  
**Зависимости:** Запущенный сервер, тестовая камера

**Подзадачи:**
1.1. Запуск сервера разработки
   - Команда: `.\gradlew.bat :server:api:run --daemon`
   - Проверка: Server health check PASS

1.2. Подготовка тестовой среды
   - Проверка доступности камеры (CameraId)
   - Настройка WebSocket подключения
   - Валидация аутентификации

1.3. Запуск acceptance тестов
   - Скрипт: `.\scripts\run-recording-ws-lifecycle-acceptance.ps1`
   - Параметры: `-CameraId <id> -AdminPassword <pwd>`
   - Ожидаемые события:
     * ✅ recording_started
     * ✅ recording_paused
     * ✅ recording_resumed
     * ✅ recording_stopped

1.4. Генерация evidence отчета
   - Автоматически через `recording-ws-lifecycle-acceptance-evidence.ps1`
   - Расположение: `diagnostics/recording-ws-acceptance/`

**Критерий завершения:**
- `recordingWsOverall = PASS`
- Все 4 события получены через WebSocket
- Evidence отчет сгенерирован

**Риски:**
- Сервер не запускается → требуется диагностика
- Камера недоступна → использовать mock/emulator
- WebSocket не подключается → проверить auth flow

---

#### Задача 2: Увеличение Canonical Readiness до 70%+
**ID:** PHASE2-P0-2  
**Приоритет:** P0  
**Оценка:** 3-5 дней  
**Зависимости:** Задача 1 (желательно, но не блокирует)

**Текущее состояние:** 66%  
**Цель:** ≥70% (+4%)

**Подзадачи:**

2.1. Анализ покрытия тестами
   - Запуск: `.\gradlew.bat test --info`
   - Генерация отчета покрытия
   - Identification gaps

2.2. Добавление интеграционных тестов RTSP
   - Файл: `core/network/src/commonTest/kotlin/.../RtspClientReconnectIntegrationTest.kt`
   - Сценарии:
     * Reconnect с exponential backoff
     * Callback обработка событий
     * Error recovery scenarios
     * Callback deregistration

2.3. Добавление интеграционных тестов HLS
   - Файл: `server/api/src/test/kotlin/.../HlsGeneratorIntegrationTest.kt`
   - Сценарии:
     * HLS генерация из RTSP
     * Cleanup старых сегментов
     * Memory management
     * Multi-camera HLS

2.4. Добавление интеграционных тестов Screenshot
   - Файл: `server/api/src/test/kotlin/.../ScreenshotServiceIntegrationTest.kt`
   - Сценарии:
     * Capture с реальным потоком
     * Качество JPEG (SSIM > 80%)
     * Разные кодеки (H.264, H.265, MJPEG)
     * Fallback без FFmpeg

2.5. Добавление E2E сценариев
   - Файл: `core/integrationTest/kotlin/.../VideoE2eIntegrationTest.kt`
   - Сценарии:
     * Camera discovery → add → stream → record → stop
     * Event generation → WebSocket notification
     * Analytics → event → notification

2.6. Пересчет weighted readiness
   - Скрипт: `.\scripts\calculate-canonical-readiness.ps1`
   - Обновление метрик в документе

**Критерий завершения:**
- Canonical readiness ≥ 70%
- Coverage report показывает рост на +10%
- Все новые тесты PASS

---

### P1 - Высокий приоритет (опционально для GO)

#### Задача 3: Linux сборка
**ID:** PHASE2-P1-3  
**Приоритет:** P1  
**Оценка:** 3-5 дней  
**Зависимости:** None

**Подзадачи:**

3.1. Создание build скрипта
   - Файл: `native/video-processing/build-linux.sh`
   - Команды:
     ```bash
     #!/bin/bash
     set -e
     cd "$(dirname "$0")"
     mkdir -p build-linux && cd build-linux
     cmake .. -DENABLE_FFMPEG=ON -DENABLE_OPENCV=OFF
     make -j$(nproc)
     ```

3.2. Настройка CMake для Linux
   - Файл: `native/video-processing/CMakeLists.txt`
   - Проверка платформенных зависимостей
   - FFmpeg для Linux (apt/yum)

3.3. Компиляция библиотеки
   - Ожидаемый артефакт: `libvideo_processing.so`
   - Проверка символов экспорта
   - Тестирование cinterop

3.4. Тестирование на Linux
   - Запуск unit тестов
   - Проверка native binding
   - Performance baseline

**Критерий завершения:**
- `libvideo_processing.so` скомпилирована
- Сборка PASS без ошибок
- Все тесты PASS на Linux

---

#### Задача 4: macOS сборка
**ID:** PHASE2-P1-4  
**Приоритет:** P1  
**Оценка:** 3-5 дней  
**Зависимости:** None

**Подзадачи:**

4.1. Создание build скрипта
   - Файл: `native/video-processing/build-macos.sh`
   - Команды:
     ```bash
     #!/bin/bash
     set -e
     cd "$(dirname "$0")"
     mkdir -p build-macos && cd build-macos
     cmake .. -DENABLE_FFMPEG=ON -DENABLE_OPENCV=OFF
     make -j$(sysctl -n hw.ncpu)
     ```

4.2. Настройка CMake для macOS
   - Файл: `native/video-processing/CMakeLists.txt`
   - Cross-compilation настройки
   - Framework dependencies

4.3. Компиляция библиотеки
   - Ожидаемый артефакт: `libvideo_processing.dylib`
   - Проверка fat binary (x86_64 + arm64)
   - Code signing (опционально)

4.4. Тестирование на macOS
   - Запуск unit тестов
   - Проверка native binding
   - Performance baseline

**Критерий завершения:**
- `libvideo_processing.dylib` скомпилирована
- Сборка PASS без ошибок
- Все тесты PASS на macOS

---

#### Задача 5: Memory leak тестирование
**ID:** PHASE2-P1-5  
**Приоритет:** P1  
**Оценка:** 2-3 дня  
**Зависимости:** Задача 1 (сервер)

**Подзадачи:**

5.1. Настройка профилирования
   - Инструменты: VisualVM / JProfiler / Async Profiler
   - Метрики: heap usage, GC activity, native memory

5.2. Long-run тесты (30+ минут)
   - Скрипт: `.\scripts\video-runtime-longrun-smoke.ps1`
   - Параметры: `-DurationMinutes 30 -IntervalSec 60`
   - Мониторинг памяти в реальном времени

5.3. Анализ результатов
   - Максимальный рост памяти: <100MB
   - GC frequency: нормальный
   - Native memory leaks: none

5.4. CPU profiling
   - Максимальная загрузка CPU: <30%
   - Hot spots identification
   - Optimization при необходимости

**Критерий завершения:**
- Нет memory leak >100MB за 30 минут
- CPU usage <30% под нагрузкой
- Профилирование отчет сгенерирован

---

## 📅 Таймлайн выполнения

### Неделя 1: P0 задачи (критические)

| День | Задачи | Ожидаемый результат |
|------|--------|-------------------|
| **День 1** | Задача 1.1-1.2: Подготовка сервера и тестовой среды | Server running, camera ready |
| **День 2** | Задача 1.3-1.4: Recording WS Lifecycle тесты | recordingWsOverall = PASS |
| **День 3** | Задача 2.1-2.2: Анализ покрытия + RTSP тесты | RTSP integration tests added |
| **День 4** | Задача 2.3: HLS интеграционные тесты | HLS integration tests PASS |
| **День 5** | Задача 2.4-2.6: Screenshot + E2E + пересчет | Canonical readiness ≥ 70% |

**Итог Недели 1:** P0 задачи завершены, 100% готовности достигнута

### Неделя 2: P1 задачи (опциональные улучшения)

| День | Задачи | Ожидаемый результат |
|------|--------|-------------------|
| **День 6** | Задача 3.1-3.2: Linux build скрипт и CMake | build-linux.sh ready |
| **День 7** | Задача 3.3-3.4: Linux компиляция и тестирование | libvideo_processing.so PASS |
| **День 8** | Задача 4.1-4.2: macOS build скрипт и CMake | build-macos.sh ready |
| **День 9** | Задача 4.3-4.4: macOS компиляция и тестирование | libvideo_processing.dylib PASS |
| **День 10** | Задача 5: Memory leak тестирование | No leaks, CPU <30% |

**Итог Недели 2:** Все P1 задачи завершены, кроссплатформенная поддержка полная

---

## 🚨 Критические зависимости и риски

### Зависимости

1. **Тестовая инфраструктура**
   - Доступ к серверу разработки
   - Тестовая камера (ONVIF совместимая)
   - WebSocket connectivity

2. **Окружение сборки**
   - Windows: Visual Studio 2022, CMake 3.20+
   - Linux: gcc/g++, cmake, ffmpeg-dev
   - macOS: Xcode, cmake, ffmpeg

3. **Инструменты тестирования**
   - PowerShell 7+
   - Gradle 8.x
   - Profiling tools (опционально)

### Риски и mitigation

| Риск | Вероятность | Влияние | Mitigation |
|------|-------------|---------|-----------|
| Сервер не запускается | Средняя | BLOCKER | Использовать Docker контейнер или mock |
| Камера недоступна | Низкая | BLOCKER | ONVIF emulator / test double |
| WebSocket не работает | Средняя | HIGH | Проверка auth flow, network logs |
| Memory leak обнаружен | Средняя | MEDIUM | Profiling + optimization sprint |
| Платформенные баги | Низкая | MEDIUM | Изоляция + platform-specific fix |

---

## ✅ Критерии завершения Фазы 2

### Обязательные (P0)

- [x] Runtime decision = GO
- [x] Profile-aware release decision = GO
- [x] 1.8.A Core network/runtime = PASS
- [x] 1.8.B Long-run matrix = PASS
- [x] 1.8.C Long-run checks = PASS
- [ ] **1.8.E Recording WS lifecycle = PASS** (Задача 1)
- [ ] **Canonical readiness ≥ 70%** (Задача 2)
- [ ] **Weighted readiness ≥ 90%** (результат задач 1-2)

### Опциональные (P1)

- [ ] Linux сборка PASS (Задача 3)
- [ ] macOS сборка PASS (Задача 4)
- [ ] Memory leak тесты PASS (Задача 5)

---

## 📈 Метрики успеха

### Технических метрик

| Метрика | Текущее | Цель | Задача |
|---------|---------|------|--------|
| Canonical readiness | 66% | ≥70% | Задача 2 |
| Weighted readiness | 85.9% | ≥90% | Задачи 1-2 |
| Test coverage | ~55% | ~65% | Задача 2 |
| Recording WS events | 0/4 | 4/4 PASS | Задача 1 |
| Платформенная поддержка | 1 (Windows) | 3 (Win+Linux+macOS) | Задачи 3-4 |

### Бизнес метрик

- **Время до релиза:** -7 дней (после завершения P0)
- **Стабильность runtime:** 91.4% → 95%+
- **Кроссплатформенность:** 33% → 100% (после P1)

---

## 🔄 Процесс выполнения

### Ежедневный workflow

1. **Утренний стендап (09:00)**
   - Обзор задач на день
   - Identification blockers
   - Распределение ресурсов

2. **Выполнение задач (09:30-18:00)**
   - Фокус на одну задачу за раз
   - Регулярные коммиты (каждые 1-2 часа)
   - Documentation параллельно

3. **Вечерний отчет (18:00)**
   - Прогресс за день
   - Blockers и риски
   - План на завтра

### Контрольные точки

- **Checkpoint 1 (День 2):** Recording WS Lifecycle PASS
- **Checkpoint 2 (День 5):** Canonical readiness ≥ 70%, P0 завершено
- **Checkpoint 3 (День 7):** Linux сборка PASS
- **Checkpoint 4 (День 10):** macOS сборка PASS, все P1 завершено

---

## 📚 Связанные документы

- [PHASE2_100_COMPLETION_CHECKLIST_2026-04-28.md](PHASE2_100_COMPLETION_CHECKLIST_2026-04-28.md)
- [PHASE2_REMAINING_TASKS_PLAN_2026-04-28.md](PHASE2_REMAINING_TASKS_PLAN_2026-04-28.md)
- [PROJECT_STATUS_PHASES.md](../status/PROJECT_STATUS_PHASES.md)
- [VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md](../reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md)

---

## 📝 История изменений

| Дата | Версия | Изменения | Автор |
|------|--------|-----------|-------|
| 2026-04-28 | 1.0 | Initial creation | AI Assistant |

---

**Создан:** 2026-04-28  
**Статус:** 🟡 В ожидании выполнения  
**Ответственный:** AI Assistant  
**Следующее действие:** Начать Задачу 1 (Recording WS Lifecycle)
