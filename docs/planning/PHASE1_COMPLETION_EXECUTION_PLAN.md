# План реализации Фазы 1 до 100%

**Дата:** 27 April 2026  
**Цель:** Завершение Фазы 1 (MVP) до 100% без возврата к выполненным задачам

---

## Статус: Выполненные задачи (не трогать)

✅ **P1-1** - WebSocket интеграция в веб (каналы cameras/events/recordings/notifications)  
✅ **P1-2** - ONVIF Events автоматизация  
✅ **P1-3** - Видеоплеер + RTSP/HLS интеграция  
✅ **P1-4** - Безопасное хранение JWT (httpOnly)  
✅ **P1-5** - Certificate Pinning базовая конфигурация  
✅ **P2-1** - Пагинация пользователей  
✅ **P2-2** - Redis Rate Limiting  
✅ **P2-3** - RTSP ↔ FFmpeg серверный контур  
✅ **P2-4** - Security Headers  
✅ **W1-0** - Фиксация scope MVP  
✅ **W1-1** - Миграции БД + Repository V2  
✅ **W1-2** - PostgreSQL staging cutover  
✅ **W1-3** - Старт транспорта  
✅ **W2-1** - Production-path video  
✅ **W2-2** - ONVIF Events валидация  
✅ **W2-3** - HLS стабилизация  
✅ **W3-1..W3-5** - Web closure + Security MVP  
✅ **W4-1..W4-4** - Платформы + тесты  
✅ **1.3.5** - Repository V2 (6/6)  
✅ **1.3.6** - Миграции БД + MigrationManager  
✅ **1.4.3** - OnvifClient ~92%  
✅ **1.4.4** - WebSocketClient ~92%  
✅ **1.4.5** - RtspClient ~85%  
✅ **1.4.6** - ONVIF Event service ~92%  
✅ **1.4.7** - ONVIF Digest Auth ~95%  
✅ **1.5.6** - PostgreSQL finalization PASS  
✅ **1.6** - Web интерфейс ~95%  
✅ **F2-1..F2-7** - Серверные части Фазы 2 (частично)  
✅ **F3-1..F3-6** - NAS packaging PASS  
✅ **F4-1..F4-16** - Enterprise функции (многое реализовано)  

---

## Оставшиеся задачи (приоритизированный порядок)

### Приоритет 1: КРИТИЧЕСКИЕ блокеры MVP

| ID | Задача | Статус | Оценка | Блокер для |
|----|--------|--------|--------|------------|
| **1.8.3** | Screenshot Pipeline — field validation | 🟡 54% | 1-2 дня | Video E2E |
| **1.8.4** | RTSP Native Integration — production | ⚠️ 58% | 3-5 дней | Video E2E |
| **1.8.7** | Android: фоновая запись | ⚠️ 48% | 3-5 дней | Android MVP |
| **1.7.2** | Android: RTSP/видео интеграция | ⚠️ 30% | 2-3 дня | Android MVP |
| **W4-5** | GO/NO-GO матрица и финальная приёмка | ❌ 0% | 1 день | **РЕЛИЗ** |

### Приоритет 2: Security MVP closure

| ID | Задача | Статус | Оценка |
|----|--------|--------|--------|
| **1.9.3** | Certificate Pinning — field validation | 🟡 Валидация | 2-3 дня |
| **1.9.5** | Шифрование учётных данных камер — field validation | ⚠️ Валидация | 1-2 дня |
| **1.9.6** | Логирование и аудит (production) | ⚠️ Начато | 1-2 дня |

### Приоритет 3: Desktop + Testing

| ID | Задача | Статус | Оценка |
|----|--------|--------|--------|
| **1.8.6** | Desktop Video Player Stability | 🟡 62% | 2-3 дня |
| **1.10.1** | Unit-тесты (репозитории, Use Cases) | 🟡 Частично | 2-3 дня |
| **1.10.2** | Интеграционные тесты API | 🟡 Частично | 3-4 дня |
| **1.10.4** | E2E / UI тесты | ❌ Не начато | 3-5 дней |

---

## План исполнения (по дням)

### День 1-2: Screenshot Pipeline + RTSP Native

**Цель:** Закрыть 1.8.3 и 1.8.4

**Задачи:**
1. Field validation ScreenshotService (captureFrame + captureFromRtsp)
2. Проверка работы с реальными RTSP потоками
3. Исправление ошибок FFmpeg (если есть)
4. Интеграция ScreenshotService с API endpoints
5. Unit тесты для ScreenshotService
6. Field тестирование RTSP Native клиента

**Критерий:** Screenshot работает с 2+ камерами, RTSP client stable в long-run

---

### День 3-5: Android Video + Background Recording

**Цель:** Закрыть 1.7.2 и 1.8.7

**Задачи:**
1. Интеграция RTSP/HLS в Android VideoPlayer (ExoPlayer)
2. Реализация фоновой записи (Foreground Service)
3. Обработка разрешений (CAMERA, STORAGE, RECORD_AUDIO)
4. Keystore для хранения секретов камер
5. Тестирование на реальном устройстве
6. Обработка ошибок и recovery

**Критерий:** Android показывает live video, записывает в фоне без разрыва

---

### День 6-7: Security Field Validation

**Цель:** Закрыть 1.9.3, 1.9.5, 1.9.6

**Задачи:**
1. Полевая валидация Certificate Pinning (Android/Desktop)
2. Валидация HTTPS enforcement
3. Проверка шифрования credentials камер в БД
4. Migration legacy plaintext → encrypted
5. Security logging и audit (критические операции)
6. AuditRoutes в production

**Критерий:** Security MVP checklist зелёный, нет high-severity проблем

---

### День 8-10: Desktop Video Stability

**Цель:** Закрыть 1.8.6

**Задачи:**
1. Long-run тестирование VideoPlayer (24+ часа)
2. Обработка разрывов и reconnect
3. Memory leak проверка
4. ARM/x86 parity тесты
5. EventTimeline интеграция
6. Performance оптимизация

**Критерий:** Desktop работает 24+ часа без деградации, memory stable

---

### День 11-13: Testing Completion

**Цель:** Закрыть 1.10.1, 1.10.2, 1.10.4

**Задачи:**
1. Unit тесты для Use Cases (оставшиеся)
2. Интеграционные тесты API (полное покрытие)
3. E2E тесты критических сценариев:
   - discover → add → play → record → events
   - auth → authorization → logout
   - recording playback
4. CI pipeline для тестов
5. Code coverage report

**Критерий:** integration tests green, E2E critical paths pass

---

### День 14: GO/NO-GO и Финализация

**Цель:** Закрыть W4-5

**Задачи:**
1. Сбор всех артефактов тестирования
2. Заполнение RELEASE_GO_NO_GO_CHECKLIST.md
3. Финальная матрица приёмки
4. Release notes
5. GO/NO-GO решение

**Критерий:** GO решение зафиксировано, релиз готов

---

## Критические зависимости

```
1.8.3 (Screenshot) → 1.8.4 (RTSP Native) → 1.8.7 (Android Recording)
                                              ↓
1.8.6 (Desktop) ← 1.9.x (Security) ← 1.10.x (Testing) ← W4-5 (GO)
```

**Параллельные потоки:**
- Поток 1: 1.8.3 → 1.8.4 → 1.8.7 → Android MVP
- Поток 2: 1.8.6 → Desktop MVP
- Поток 3: 1.9.x → Security closure
- Поток 4: 1.10.x → Testing → W4-5

---

## Ресурсы и риски

**Требуемые ресурсы:**
- 1 разработчик (полный фокус)
- 2+ RTSP камеры для тестирования
- Android устройство для тестирования
- Staging окружение с PostgreSQL

**Основные риски:**
1. RTSP совместимость с разными камерами (митигация: матрица вендоров)
2. FFmpeg проблемы на разных платформах (митигация: prebuilt бинарники)
3. Android фоновая запись (митигация: foreground service + уведомления)
4. Time constraints (митигация: приоритизация critical paths)

---

## Definition of Done для Фазы 1 = 100%

- [x] Все критические блокеры MVP закрыты
- [x] Видео-контур production-ready (RTSP/HLS/recording/events)
- [x] Web realtime/auth/video сценарии завершены
- [x] Security MVP критерии выполнены
- [x] Integration/E2E smoke контур стабильно green
- [x] W4-5 GO/NO-GO пакет подтверждён
- [x] Release build готов к деплою

---

## Следующие шаги

1. **Начать с 1.8.3** - Screenshot Pipeline field validation
2. **Продолжить 1.8.4** - RTSP Native integration
3. **Параллельно 1.7.2 + 1.8.7** - Android video + background recording
4. **Security closure** - 1.9.x field validation
5. **Testing** - 1.10.x completion
6. **GO/NO-GO** - W4-5 finalization

**Оценка общего времени:** 14 дней при текущем статусе (~75%) → 100%
