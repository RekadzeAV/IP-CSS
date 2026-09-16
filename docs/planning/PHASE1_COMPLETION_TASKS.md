# План завершения Фазы 1 (MVP) до 100%

**Проект:** IP-CSS  
**Версия:** Alfa-0.1.1  
**Дата:** 27 April 2026  
**Цель:** Доведение Фазы 1 (MVP) до 100% готовности  

**Актуальный статус:** ~75% (согласно `docs/status/PROJECT_STATUS_PHASES.md`)  
**Оценка剩余 времени:** 3-4 недели при фокусированной разработке  

---

## 📊 Текущий статус по этапам Фазы 1

| Этап | Название | Прогресс | Статус | Критичность |
|------|----------|----------|--------|-------------|
| 1.1 | Инфраструктура и основа | 100% | ✅ Завершено | - |
| 1.2 | Доменный слой | 55% | 🟡 В процессе | Средняя |
| 1.3 | Слой данных | 98% | 🟢 Почти завершено | Низкая |
| 1.4 | Сетевой слой | 95% | 🟢 Почти завершено | Средняя |
| 1.5 | Серверная часть | 88% | 🟡 В процессе | Высокая |
| 1.6 | Веб-интерфейс | 95% | 🟢 Почти завершено | Высокая |
| 1.7 | Мобильные и десктоп | 30% (Android), 70% (Desktop) | 🟡 В процессе | Высокая |
| 1.8 | Видео и запись | 66% | 🟡 В процессе | КРИТИЧЕСКАЯ |
| 1.9 | Безопасность MVP | 80% | 🟡 В процессе | КРИТИЧЕСКАЯ |
| 1.10 | Тестирование | 28% | ⚠️ Начато | Высокая |

**Общий прогресс:** ~75%

---

## 🚨 Критические блокеры (приоритет 1)

### БЛОКЕР-1: RTSP клиент - runtime стабильность (1.8.4, 1.4.5)

**Статус:** ~58% готово  
**Влияние:** Блокирует production-использование, влияет на 1.8 Видео  
**Текущие проблемы:**
- ✅ Нативная C++ реализация (rtsp_client.cpp) - ~85% готово
- ✅ Структура Kotlin обертки (RtspClient.kt)
- ✅ cinterop + Kotlin/Native (mingwX64) компилируется
- ❌ Полная интеграция FFmpeg с Kotlin/Native
- ❌ Аудио декодирование (отключено из-за FFmpeg 8.0 API)
- ❌ Длительные интеграционные тесты с реальными RTSP камерами
- ❌ Обработка различных кодеков (H.264, H.265, MJPEG) в production

**Задачи:**
1. [ ] **RTSP-1.1** Исправить аудио декодирование или документировать as-known limitation
   - Файлы: `native/rtsp_client.cpp`, `shared/src/jvmMain/kotlin/.../NativeRtspClient.kt`
   - Оценка: 2-3 дня
   - Приоритет: Высокий (но не блокирующий MVP без аудио)

2. [ ] **RTSP-1.2** Реализовать полноценные интеграционные тесты с mock RTSP сервером
   - Файлы: `shared/src/desktopTest/kotlin/.../RtspClientIntegrationTest.kt`
   - Оценка: 3-4 дня
   - Приоритет: Критический

3. [ ] **RTSP-1.3** Длительные тесты (long-run) на стабильность (24+ часа)
   - Скрипт: `scripts/rtsp-long-run-stability-test.ps1`
   - Оценка: 2 дня (настройка) + 24+ часа прогона
   - Приоритет: Высокий

4. [ ] **RTSP-1.4** Обработка ошибок и reconnect логика
   - Файлы: `shared/src/commonMain/kotlin/.../RtspClient.kt`
   - Оценка: 2-3 дня
   - Приоритет: Критический

**DoD:**
- RTSP клиент стабильно работает 24+ часа без утечек
- Понятная обработка всех классов ошибок
- Документированные ограничения (если аудио не работает)
- Integration tests проходят в CI

---

### БЛОКЕР-2: Видео runtime путь - полная валидация (1.8.1, 1.8.2, 1.8.5)

**Статус:** ~66%  
**Влияние:** Критично для MVP (live view, recording)  
**Текущие проблемы:**
- HLS pipeline работает, но требуется validation для production
- Screenshot pipeline ~54% (требует field validation)
- VideoPlayer stability на разных платформах

**Задачи:**
1. [ ] **VIDEO-2.1** HLS long-run stability тесты
   - Скрипт: `scripts/hls-long-run-stability.ps1`
   - Оценка: 2 дня (настройка) + 24+ часа прогона
   - Приоритет: Высокий

2. [ ] **VIDEO-2.2** Screenshot pipeline integration test с реальным FFmpeg
   - Файлы: `server/api/src/test/kotlin/.../ScreenshotServiceIntegrationTest.kt`
   - Оценка: 2-3 дня
   - Приоритет: Высокий

3. [ ] **VIDEO-2.3** VideoPlayer stability на Desktop (long-run)
   - Оценка: 2-3 дня
   - Приоритет: Высокий

4. [ ] **VIDEO-2.4** VideoPlayer адаптивность и performance на Web
   - Файлы: `server/web/src/.../VideoPlayer.tsx`
   - Оценка: 3-4 дня
   - Приоритет: Средний

5. [ ] **VIDEO-2.5** Android video + background recording instrumented tests
   - Оценка: 3-5 дней
   - Приоритет: Высокий

**DoD:**
- HLS pipeline проходит 24+ часа без деградации
- Screenshot работает с реальными RTSP потоками
- VideoPlayer стабильно работает на всех поддерживаемых платформах
- Известные performance метрики (FPS, memory usage)

---

### БЛОКЕР-3: AI аналитика - сквозная интеграция (2.1.7)

**Статус:** ~45% (но это Фаза 2, не блокирует Фазу 1)  
**Влияние:** Не блокирует MVP, но является подфокусом текущего исполнения  
**Текущие проблемы:**
- HLS-only без server frame source для аналитики
- Сквозная стабильность требует доработки

**Задачи (если требуется в рамках Фазы 1):**
1. [ ] **ANALYTICS-3.1** Закрыть сценарий S-RTSP-1 из `PHASE2_1_7_ACCEPTANCE_MATRIX.md`
   - Файлы: `server/api/src/main/kotlin/.../VideoAnalyticsService.kt`
   - Оценка: 2-3 дня
   - Приоритет: Средный (если требуется в MVP)

2. [ ] **ANALYTICS-3.2** Метрики и monitoring для production
   - Файлы: `server/api/src/main/kotlin/.../AnalyticsRoutes.kt`
   - Оценка: 1-2 дня
   - Приоритет: Средный

**DoD:** (если включено в MVP)
- RTSP-decoded frames успешно обрабатываются всеми детекторами
- WebSocket события отправляются корректно
- Метрики доступны через `/analytics/metrics`

---

## 🔒 Приоритет 2: Security closure (1.9)

### Задачи безопасности MVP

1. [ ] **SEC-4.1** HTTPS enforcement финальная валидация
   - Файлы: `server/api/src/main/kotlin/.../HttpsRedirectAndHstsMiddleware.kt`
   - Тесты: `HttpsRedirectAndHstsMiddlewareTest`
   - Оценка: 1 день
   - Приоритет: Высокий

2. [ ] **SEC-4.2** Certificate Pinning field validation (Android/Desktop)
   - Скрипт: `scripts/security-mvp-readiness-check.ps1`
   - Оценка: 2-3 дня
   - Приоритет: Высокий

3. [ ] **SEC-4.3** Шифрование учётных данных камер - валидация на staging
   - Файлы: `shared/src/commonMain/kotlin/.../PasswordEncryption.kt`
   - Оценка: 2 дня
   - Приоритет: Высокий

4. [ ] **SEC-4.4** Security logging и аудит - production readiness
   - Файлы: `server/api/src/main/kotlin/.../SecurityLogger.kt`, `AuditRoutes.kt`
   - Оценка: 1-2 дня
   - Приоритет: Средний

**DoD:**
- Security MVP checklist (`SECURITY_MVP_READINESS.md`) в зеленом статусе
- Нет открытых high-severity проблем
- Field validation пройдена

---

## 🗄️ Приоритет 3: PostgreSQL finalization (1.5.6)

**Статус:** В работе  
**Влияние:** Production readiness

**Задачи:**
1. [ ] **DB-5.1** PostgreSQL staging cutover по runbook
   - Документ: `POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md`
   - Оценка: 2-3 дня
   - Приоритет: Высокий

2. [ ] **DB-5.2** Rollback rehearsal (тестовый прогон)
   - Оценка: 1 день
   - Приоритет: Высокий

3. [ ] **DB-5.3** Production smoke тесты после cutover
   - Оценка: 1-2 дня
   - Приоритет: Критический

**DoD:**
- Staging cutover пройден успешно
- Rollback rehearsal успешен
- Production smoke тесты green

---

## 🌐 Приоритет 4: Web closure (1.6)

**Статус:** ~95%  
**Остаток:** 5%

**Задачи:**
1. [ ] **WEB-6.1** WebSocket полная интеграция - тестирование
   - Файлы: `server/web/src/.../WebSocketProvider.tsx`, `useWebSocket.ts`
   - Оценка: 1-2 дня
   - Приоритет: Высокий

2. [ ] **WEB-6.2** VideoPlayer HLS integration testing
   - Файлы: `server/web/src/.../VideoPlayer.tsx`
   - Оценка: 2-3 дня
   - Приоритет: Высокий

3. [ ] **WEB-6.3** Performance optimization (Lighthouse metrics)
   - Оценка: 2-3 дня
   - Приоритет: Средний

4. [ ] **WEB-6.4** Автоматизация web testing в CI
   - Файлы: `scripts/ci/mvp-automated-acceptance.ps1`
   - Оценка: 1-2 дня
   - Приоритет: Высокий

**DoD:**
- Все web P1 задачи закрыты
- Web тесты проходят в CI
- Lighthouse metrics в допустимых пределах

---

## 📱 Приоритет 5: Platform closure (1.7)

### Android (30% → target 60% для MVP)

1. [ ] **AND-7.1** Android video integration (ExoPlayer/RTSP)
   - Оценка: 3-5 дней
   - Приоритет: Высокий

2. [ ] **AND-7.2** Background recording service
   - Оценка: 2-3 дня
   - Приоритет: Высокий

3. [ ] **AND-7.3** Android instrumented tests (критические сценарии)
   - Оценка: 3-4 дня
   - Приоритет: Высокий

### Desktop (70% → target 85% для MVP)

1. [ ] **DESK-7.4** Desktop long-run video stability
   - Оценка: 2-3 дня
   - Приоритет: Высокий

2. [ ] **DESK-7.5** System tray и автозапуск
   - Файлы: `shared/src/desktopMain/kotlin/.../SystemIntegration.kt`
   - Оценка: 1-2 дня
   - Приоритет: Средний

**DoD:**
- Android и Desktop проходят MVP smoke тесты
- Критические сценарии работают стабильно

---

## ✅ Приоритет 6: Testing and acceptance gate (1.10)

**Статус:** ~28%  
**Цель:** Минимальный обязательный контур

**Задачи:**
1. [ ] **TEST-8.1** Unit тесты - критические Use Cases
   - Приоритетные: CameraRepository, RecordingRepository, AnalyticsUseCases
   - Оценка: 3-4 дня
   - Приоритет: Высокий

2. [ ] **TEST-8.2** Интеграционные тесты API - расширение покрытия
   - Файлы: `server/api/src/test/kotlin/.../*IntegrationTest.kt`
   - Оценка: 3-5 дней
   - Приоритет: Высокий

3. [ ] **TEST-8.3** Migration integration tests - расширенный контур
   - Уже есть: `MigrationManagerIntegrationTest`
   - Оценка: 1-2 дня (дополнительные сценарии)
   - Приоритет: Средний

4. [ ] **TEST-8.4** E2E тесты - минимальный критический сценарий
   - Сценарий: login -> discover camera -> add camera -> start stream -> record -> stop -> playback
   - Оценка: 5-7 дней
   - Приоритет: Средний

5. [ ] **TEST-8.5** Go/No-Go матрица и автоматизация
   - Файлы: `scripts/w4-mvp-platform-and-gate.ps1`, `RELEASE_GO_NO_GO_CHECKLIST.md`
   - Оценка: 2-3 дня
   - Приоритет: Критический

**DoD:**
- Обязательный тестовый контур стабильно green
- `./gradlew mvpAutomatedAcceptance` проходит
- Go/No-Go документ заполнен

---

## 📅 Рекомендуемый календарный план (4 недели)

### Неделя 1: Foundation lock + Video core

**Цель:** Закрыть критические блокеры видео

| День | Задачи |
|------|--------|
| 1-2 | **БЛОКЕР-1**: RTSP интеграция (RTSP-1.1, RTSP-1.2) |
| 3-4 | **БЛОКЕР-1**: RTSP-1.3 (long-run тесты запуск) + RTSP-1.4 |
| 5-7 | **БЛОКЕР-2**: VIDEO-2.1 (HLS long-run), VIDEO-2.2, VIDEO-2.3 |

**KPI недели:**
- RTSP long-run тесты запущены
- HLS long-run тесты запущены
- Базовая RTSP стабильность подтверждена

---

### Неделя 2: Video completion + Security start

**Цель:** Завершить видео, начать security

| День | Задачи |
|------|--------|
| 8-9 | **БЛОКЕР-2**: VIDEO-2.4, VIDEO-2.5 |
| 10-11 | **БЛОКЕР-1**: RTSP long-run анализ + исправления |
| 12-14 | **SEC-4.1**, **SEC-4.2**, **SEC-4.3** |

**KPI недели:**
- Все video блокеры закрыты
- Security readiness check пройден
- Long-run тесты успешны

---

### Неделя 3: PostgreSQL + Web + Platform

**Цель:** Production backend, web closure, platform stability

| День | Задачи |
|------|--------|
| 15-17 | **DB-5.1**, **DB-5.2**, **DB-5.3** (PostgreSQL cutover) |
| 18-19 | **WEB-6.1**, **WEB-6.2** |
| 20-21 | **AND-7.1**, **DESK-7.4** |

**KPI недели:**
- PostgreSQL production path готов
- Web функциональность завершена
- Platform stability подтверждена

---

### Неделя 4: Testing + Go/No-Go

**Цель:** Тестирование, acceptance, релиз

| День | Задачи |
|------|--------|
| 22-24 | **TEST-8.1**, **TEST-8.2** (unit + integration) |
| 25-26 | **TEST-8.4** (E2E), **TEST-8.5** (Go/No-Go) |
| 27-28 | Финальный прогон `mvpAutomatedAcceptance`, релизный отчет |

**KPI недели:**
- Все тесты green
- Go/No-Go: GO
- Фаза 1 = 100%

---

## 🎯 Definition of Done для Фазы 1 = 100%

Фаза 1 считается завершённой при **одновременном** выполнении:

### Критические критерии (Must have)

- [ ] ✅ Все критические блокеры закрыты (RTSP, Video runtime)
- [ ] ✅ Video контур production-ready (RTSP/HLS/recording/events)
- [ ] ✅ Web realtime/auth/video сценарии завершены
- [ ] ✅ Security MVP критерии выполнены (`SECURITY_MVP_READINESS.md` green)
- [ ] ✅ PostgreSQL production path готов и протестирован
- [ ] ✅ Android/Desktop MVP stability подтверждена
- [ ] ✅ `./gradlew mvpAutomatedAcceptance` проходит успешно
- [ ] ✅ Go/No-Go документ заполнен и статус = GO

### Дополнительные критерии (Nice to have)

- [ ] 🟡 Performance benchmarks документированы
- [ ] 🟡 Production runbook завершён
- [ ] 🟡 Known issues список актуален и минимален
- [ ] 🟡 User documentation обновлена

---

## 📈 Метрики прогресса

**Текущий прогресс:** 75%

**Разбивка по критичности:**

| Критичность | Прогресс | Остаток |
|-------------|----------|---------|
| Критическая (блокеры) | 40% | 60% |
| Высокая | 70% | 30% |
| Средняя | 85% | 15% |
| Низкая | 95% | 5% |

**Оценка остаточных человеко-дней:** 50-60 дней (при 1 разработчике = 3-4 недели)

---

## 🚦 Блокирующие зависимости

1. **RTSP клиент** → Блокирует: Video playback, Recording, Analytics
2. **Video runtime** → Блокирует: MVP acceptance
3. **PostgreSQL** → Блокирует: Production deployment
4. **Security** → Блокирует: Production deployment
5. **Web/Platform** → Блокирует: User acceptance

---

## 📋 Чеклист для еженедельных проверок

### Еженедельный статус (шаблон)

```
Неделя: ___
Дата: ___

Прогресс:
- RTSP стабильность: [ ] [ ] [ ] (0-100%)
- Video runtime: [ ] [ ] [ ] (0-100%)
- Security: [ ] [ ] [ ] (0-100%)
- PostgreSQL: [ ] [ ] [ ] (0-100%)
- Web: [ ] [ ] [ ] (0-100%)
- Platform: [ ] [ ] [ ] (0-100%)
- Testing: [ ] [ ] [ ] (0-100%)

Блокеры:
- [ ] Описание блокера 1
- [ ] Описание блокера 2

План на следующую неделю:
- Задача 1
- Задача 2

Риски:
- Риск 1 (вероятность, влияние)
- Риск 2
```

---

## 🔗 Связанные документы

- [PROJECT_STATUS_PHASES.md](../status/PROJECT_STATUS_PHASES.md) - Детальный статус по фазам
- [PHASE1_MVP_TO_100_PLAN.md](PHASE1_MVP_TO_100_PLAN.md) - Базовый план
- [CRITICAL_BLOCKERS_REMEDIATION_PLAN.md](../planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md) - План по блокерам
- [MVP_PHASE1_SCOPE_BOUNDARY.md](../planning/MVP_PHASE1_SCOPE_BOUNDARY.md) - Границы MVP
- [SECURITY_MVP_READINESS.md](../status/SECURITY_MVP_READINESS.md) - Security checklist
- [RELEASE_GO_NO_GO_CHECKLIST.md](RELEASE_GO_NO_GO_CHECKLIST.md) - Релизный gate
- [MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](../automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md) - Автоматизация

---

## 📝 История изменений

| Дата | Автор | Изменение |
|------|-------|-----------|
| 2026-04-27 | NLP-Core-Team | Initial version |

---

**Примечание:** Этот план является living document и должен обновляться по мере выполнения задач и изменения приоритетов.
