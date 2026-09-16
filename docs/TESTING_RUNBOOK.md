# Руководство по запуску тестирования Фазы 1 (MVP)

**Дата:** 27 April 2026  
**Версия:** 1.0

---

## 📋 Содержание

1. [Быстрый старт](#быстрый-старт)
2. [Подготовка инфраструктуры](#подготовка-инфраструктуры)
3. [RTSP Long-Run тестирование](#rtsp-long-run-тестирование)
4. [MVP Automated Acceptance](#mvp-automated-acceptance)
5. [Расширенные сценарии](#расширенные-сценарии)
6. [Устранение проблем](#устранение-проблем)

---

## Быстрый старт

### Минимальный тест (10 минут)

```powershell
# Быстрый тест RTSP стабильности
.\scripts\run-rtsp-test-with-checks.ps1 -RtspUrl "rtsp://user:pass@192.168.1.100:554/stream" -FastMode
```

### Полная проверка MVP

```powershell
# Запуск всех MVP acceptance тестов
.\scripts\run-mvp-acceptance-with-infra.ps1
```

---

## Подготовка инфраструктуры

### 1. Настройка переменных окружения

Скопируйте и настройте файл `.env`:

```powershell
# Копирование примера
Copy-Item .env.example .env

# Редактирование .env
notepad .env
```

**Обязательные переменные:**

```bash
# Секреты (сгенерируйте сильные пароли!)
JWT_SECRET=<минимум 32 случайных символа>
DB_PASSWORD=<минимум 16 символов>
REDIS_PASSWORD=<минимум 16 символов>

# PostgreSQL
DATABASE_URL=jdbc:postgresql://localhost:5432/surveillance
DATABASE_USER=surveillance
DATABASE_PASSWORD=<ваш пароль из DB_PASSWORD>
```

**Генерация секретов (PowerShell):**

```powershell
# JWT_SECRET (32 байта = 64 hex символа)
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 64 | ForEach-Object {[char]$_})

# DB_PASSWORD (24 байта = 48 hex символа)
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 48 | ForEach-Object {[char]$_})
```

### 2. Запуск Docker контейнеров

```powershell
# Проверка Docker
docker --version
docker compose version

# Запуск всех сервисов
cd <project-root>
docker compose up -d

# Проверка статуса
docker compose ps

# Просмотр логов (при проблемах)
docker compose logs -f surveillance
```

**Ожидаемый вывод:**

```
✓ ip-camera-surveillance  - Основной сервис
✓ surveillance-postgres   - PostgreSQL база данных
✓ surveillance-redis      - Redis кэш
```

### 3. Проверка доступности сервисов

```powershell
# Health check основного сервиса
curl http://localhost:8080/api/v1/health

# PostgreSQL
docker exec surveillance-postgres pg_isready -U surveillance

# Redis
docker exec surveillance-redis redis-cli ping
```

---

## RTSP Long-Run тестирование

### Цели тестирования

- Проверка стабильности RTSP подключения
- Мониторинг использования памяти
- Проверка обработки ошибок и reconnect
- Сбор метрик производительности (FPS)

### Сценарий 1: Быстрая проверка (10 минут)

```powershell
.\scripts\run-rtsp-test-with-checks.ps1 `
  -RtspUrl "rtsp://admin:password@192.168.1.100:554/stream" `
  -FastMode
```

**Что делает:**
- Автоматически запускает Docker контейнеры если они остановлены
- Запускает короткий тест на 10 минут
- Генерирует отчёт в `docs/reports/rtsp-stability-tests/`

### Сценарий 2: Стандартный тест (1-2 часа)

```powershell
.\scripts\run-rtsp-test-with-checks.ps1 `
  -RtspUrl "rtsp://admin:password@192.168.1.100:554/stream" `
  -DurationMinutes 120
```

**Важно:**
- Если сегодня НЕ выходной день, скрипт запросит подтверждение
- Используйте `-WeekendMode` если запускаете в выходной

### Сценарий 3: Длительный тест (24 часа)

```powershell
# ВАЖНО: Запускайте только в выходные дни!
.\scripts\run-rtsp-test-with-checks.ps1 `
  -RtspUrl "rtsp://admin:password@192.168.1.100:554/stream" `
  -DurationMinutes 1440 `
  -WeekendMode
```

**Рекомендации:**
- ✅ Запускайте в субботу/воскресенье
- ✅ Убедитесь в стабильности сети
- ✅ Проверьте свободное место на диске (>10 ГБ для записей)
- ✅ Настройте уведомление по завершении

### Параметры скрипта

| Параметр | Описание | По умолчанию |
|----------|----------|--------------|
| `-RtspUrl` | RTSP URL тестовой камеры | **обязательно** |
| `-DurationMinutes` | Длительность в минутах | 60 |
| `-FastMode` | Короткий тест (10 минут) | false |
| `-WeekendMode` | Явно указать выходной день | false |
| `-ShowHelp` | Показать справку | - |

### Результаты тестирования

Отчёты сохраняются в: `docs/reports/rtsp-stability-tests/`

**Структура отчёта:**

```markdown
# RTSP Long-Run Stability Test Report

## Summary
- Status: ✅ PASSED / ❌ FAILED
- Duration: 3600s
- Total Frames: 108000
- Average FPS: 30.0
- Reconnects: 0
- Memory Leak: NO

## Connection Statistics
- Total Connections: 1
- Successful: 1
- Failed: 0
- Reconnections: 0

## Memory Statistics
- Initial: 256.50 MB
- Peak: 289.30 MB
- Final: 261.20 MB
- Memory Leak Suspected: NO
```

### Критерии прохождения теста

**✅ PASSED:**
- Failed connections ≤ 3
- Reconnections ≤ 5
- No memory leak detected
- Total frames received > 0

**❌ FAILED:**
- Больше 3 неудачных подключений
- Больше 5 reconnections
- Подозрение на утечку памяти (>20% рост)
- Не получено ни одного кадра

---

## MVP Automated Acceptance

### Цели тестирования

- Проверка всех компонентов MVP
- Unit и integration тесты
- Web build и tests
- Video E2E gate (опционально)

### Сценарий 1: Полный прогон

```powershell
.\scripts\run-mvp-acceptance-with-infra.ps1
```

**Что проверяет:**
1. ✅ `:shared:desktopTest` - Shared module tests
2. ✅ `:core:network:desktopTest` - Network layer tests
3. ✅ `:server:api:test` - API integration tests
4. ✅ `server/web: npm build + test` - Web interface
5. ✅ `video-e2e-go-no-go.ps1` - Video E2E gate

### Сценарий 2: Без видео gate

```powershell
.\scripts\run-mvp-acceptance-with-infra.ps1 -SkipVideoGate
```

**Используйте если:**
- Нет доступа к реальным камерам
- Video gate не требуется для текущего сценария

### Сценарий 3: С генерацией Phase 1 summary

```powershell
.\scripts\run-mvp-acceptance-with-infra.ps1 -GeneratePhase1Summary
```

**Генерирует:**
- `docs/reports/PHASE1_GO_NO_GO_SUMMARY_*.md`
- Итоговый отчёт по всем критериям приёмки

### Параметры скрипта

| Параметр | Описание | По умолчанию |
|----------|----------|--------------|
| `-SkipVideoGate` | Пропустить video E2E gate | false |
| `-GeneratePhase1Summary` | Сгенерировать summary | false |
| `-Phase1SummaryProfile` | Strict \| MvpCi | MvpCi |
| `-SkipInfrastructureCheck` | Пропустить проверку infra | false |
| `-ShowHelp` | Показать справку | - |

### Результаты тестирования

**Успешный завершение (exit 0):**
```
✅ MVP Automated Acceptance успешно завершен!
```

**NO-GO (exit 2):**
```
❌ MVP Automated Acceptance завершился с ошибками (exit code: 2)
   Status: NO-GO (критичные проблемы)
```

**CONDITIONAL GO (exit 3):**
```
❌ MVP Automated Acceptance завершился с ошибками (exit code: 3)
   Status: CONDITIONAL GO (есть замечания)
```

**Логи:**
- Gradle тесты: консольный вывод
- Web тесты: `server/web/coverage/`
- MVP summary: `docs/reports/PHASE1_GO_NO_GO_SUMMARY_*.md`

---

## Расширенные сценарии

### Запуск только определённых тестов

```powershell
# Только shared module tests
.\gradlew.bat :shared:desktopTest

# Только API tests
.\gradlew.bat :server:api:test

# Только web tests
cd server\web
npm test
```

### Проверка миграций БД

```powershell
.\gradlew.bat :shared:desktopTest --tests "*MigrationManagerIntegrationTest*"
```

### Проверка RTSP отдельно

```powershell
.\gradlew.bat :shared:desktopTest --tests "*RtspClient*"
```

### Docker управление

```powershell
# Остановка всех контейнеров
docker compose down

# Перезапуск с очисткой томов
docker compose down -v

# Просмотр логов в реальном времени
docker compose logs -f surveillance

# Остановка конкретного сервиса
docker compose stop surveillance

# Перезапуск конкретного сервиса
docker compose restart surveillance
```

---

## Устранение проблем

### Проблемы с Docker

**Проблема:** Контейнеры не запускаются

```powershell
# Проверка логов
docker compose logs surveillance

# Проверка портов (8080 занят?)
netstat -ano | findstr :8080

# Освободить порт или изменить PORT в .env
```

**Проблема:** PostgreSQL не готов

```powershell
# Ожидание 30 секунд и повторная проверка
Start-Sleep -Seconds 30
docker exec surveillance-postgres pg_isready -U surveillance

# Проверка миграций
docker compose logs surveillance | Select-String "Flyway"
```

### Проблемы с тестами

**Проблема:** Gradle тесты падают

```powershell
# Очистка кэша
.\gradlew.bat clean

# Повторный запуск
.\gradlew.bat :shared:desktopTest

# Проверка Java версии
java -version  # Должна быть Java 17+
```

**Проблема:** Web тесты падают

```powershell
cd server\web

# Очистка node_modules
Remove-Item -Recurse -Force node_modules
npm cache clean --force

# Переустановка зависимостей
npm ci

# Запуск тестов
npm test
```

### Проблемы с RTSP

**Проблема:** Камера не отвечает

```powershell
# Проверка доступности камеры
ping 192.168.1.100

# Проверка порта RTSP
Test-NetConnection 192.168.1.100 -Port 554

# Проверка правильности URL
# Формат: rtsp://username:password@ip:554/path
```

**Проблема:** Аутентификация не работает

```powershell
# Проверка учетных данных
# Убедитесь что username и password правильные

# Проверка поддержки аутентификации камерой
# Некоторые камеры требуют DIGEST_AUTH
```

### Проблемы с памятью

**Проблема:** Утечка памяти в тестах

```powershell
# Проверка использования памяти
Get-Process | Where-Object {$_.ProcessName -like "*java*"} | Sort-Object WorkingSet -Descending

# Увеличение heap size (если нужно)
# Добавьте в gradlew.bat:
# set JAVA_OPTS=-Xmx4g -Xms1g
```

---

## Чеклист перед запуском длительных тестов

### Для тестов > 2 часов

- [ ] Сегодня выходной день (или я использую `-WeekendMode`)
- [ ] Свободно >10 ГБ на диске
- [ ] Стабильное интернет-соединение
- [ ] Камера включена и доступна
- [ ] Docker контейнеры работают стабильно
- [ ] Нет запланированных перезагрузок системы
- [ ] Уведомление настроено (скрипт завершится с сообщением)

### Для MVP Acceptance

- [ ] Все переменные в `.env` настроены
- [ ] Docker контейнеры запущены
- [ ] Health check проходит: `http://localhost:8080/api/v1/health`
- [ ] PostgreSQL готов: `pg_isready`
- [ ] Redis готов: `redis-cli ping` → PONG
- [ ] Нет других запущенных инстансов на порту 8080

---

## Полезные команды

### Мониторинг

```powershell
# Статус контейнеров
docker compose ps

# Использование ресурсов
docker stats

# Проверка портов
netstat -ano | findstr :8080

# Логи в реальном времени
docker compose logs -f surveillance
```

### Очистка

```powershell
# Очистка ненужных образов
docker image prune -f

# Очистка ненужных томов
docker volume prune -f

# Полная очистка (осторожно!)
docker system prune -a -f
```

### Резервное копирование

```powershell
# Бэкап PostgreSQL
docker exec surveillance-postgres pg_dump -U surveillance surveillance > backup.sql

# Восстановление PostgreSQL
docker exec -i surveillance-postgres psql -U surveillance < backup.sql
```

---

## Связанные документы

- [PHASE1_COMPLETION_TASKS.md](../docs/planning/PHASE1_COMPLETION_TASKS.md) - План задач
- [PHASE1_COMPLETION_STATUS_2026-04-27.md](../docs/reports/PHASE1_COMPLETION_STATUS_2026-04-27.md) - Статус
- [RTSP_LONG_RUN_STABILITY_TEST.md](../docs/testing/RTSP_LONG_RUN_STABILITY_TEST.md) - Документация RTSP тестов
- [MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](../docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md) - Автоматизация

---

**Последнее обновление:** 27 April 2026  
**Версия документа:** 1.0
