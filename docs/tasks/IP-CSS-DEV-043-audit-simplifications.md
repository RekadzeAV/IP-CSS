# Постановка задачи: Аудит упрощений и восстановление полнофункциональной реализации

## ID задачи
IP-CSS-DEV-043

## Название
Аудит кодовой базы на наличие упрощений/stub'ов и восстановление полнофункциональной реализации бизнес-логики

## Статус
Новая задача

## Приоритет
High

## Компонент
`server/api/src/main/kotlin/com/company/ipcamera/server/validation/RequestValidator.kt`
`server/api/src/main/kotlin/com/company/ipcamera/server/controller/OnvifRoutes.kt`
`server/api/src/main/kotlin/com/company/ipcamera/server/config/LdapConfig.kt`
`server/api/src/main/kotlin/com/company/ipcamera/server/config/EnterpriseAuthConfig.kt`
`shared/`, `core/`, `server/api/` (общий аудит)

---

## 1. Проблема

### 1.1 Описание
В ходе устранения критических ошибок компиляции (адаптация под UnboundID LDAP SDK v7.x, исправление WebSocket-сессий, удаление дубликатов функций) ряд модулей был **временно упрощён** или приведён к **минимально рабочему состоянию**. 

Текущая сборка проходит успешно, но функциональность Enterprise-режима, валидации и ONVIF-маршрутов реализована частично или не работает с реальной БД.

**Критические зоны упрощений:**
1. `RequestValidator.kt` — удалён ~150 строк валидации (массовые операции, аналитика).
2. `OnvifRoutes.kt` — хардкод IP/учётных данных вместо запросов к БД.
3. `LdapConfig.kt` / `EnterpriseAuthConfig.kt` — базовая совместимость с SDK v7.x достигнута, но отсутствуют пулинг, LDAPS, retry-логика и маппинг групп.
4. Общий код — присутствуют `TODO`/`FIXME` метки, не обработанные в рамках горячих фиксов.

### 1.2 Симптомы
- Массовое удаление/экспорт записей и камер не валидируется.
- Правила аналитики не проверяются на корректность.
- ONVIF-запросы работают только с заглушками (`192.168.10.23`, `survival/1234567890qazxs`).
- LDAP-подключение не масштабируется (нет пула, нет graceful reconnect, нет LDAPS).

---

## 2. Цель исследования и доработки

Полностью восстановить удалённую валидацию, заменить заглушки на реальные вызовы репозиториев/БД, и перевести LDAP-интеграцию в production-ready состояние (пулинг, LDAPS, resilience).

---

## 3. Область аудита и доработки

### 3.1 `RequestValidator.kt` — Восстановление удалённых методов
При исправлении ошибки `Conflicting overloads` для `private fun error()` были удалены следующие публичные и приватные методы:
- [ ] `validateBulkDeleteRecordingsRequest` — валидация массового удаления записей
- [ ] `validateBulkExportRecordingsRequest` — валидация массового экспорта (формат, качество, таймфрейм)
- [ ] `validateBulkDeleteCamerasRequest` — валидация массового удаления камер
- [ ] `validateAnalyticsRule` + helpers:
  - `validateRuleName`
  - `validateAnalyticsType`
  - `validateConditions`
  - `validateActions`
- [ ] `validateAnalyticsConfig` + helpers:
  - `validateObjectDetection`
  - `validateZones`
  - `validateMotionDetection`
  - `validateANPR`
  - `validateFaceRecognition`

**Требуется:** Восстановить методы из Git-истории (если возможно) или реализовать заново, сверившись с DTO и бизнес-требованиями. Убедиться, что валидатор не ломает текущие маршруты.

### 3.2 `OnvifRoutes.kt` — Устранение хардкода и подключение БД
**Текущее состояние:**
```kotlin
// TODO: Get camera IP from database
val cameraIp = "192.168.10.23" // Placeholder

// TODO: Get camera credentials from database
val cameraIp = "192.168.10.23"
val username = "survival"
val password = "1234567890qazxs"
```

**Требуется:**
- [ ] Подключить `CameraRepository` / `CameraService` через Koin (`by inject()`)
- [ ] Извлекать `ip`, `username`, `password` из БД по `cameraId`
- [ ] Добавить обработку `CameraNotFoundException` с возвратом `404 Not Found`
- [ ] Реализовать безопасное хранение/использование паролей (не логировать, использовать `CharArray` где возможно)

### 3.3 `LdapConfig.kt` & `EnterpriseAuthConfig.kt` — Production Hardening
**Текущее состояние:** SDK v7.x совместимость восстановлена, базовые методы работают. Отсутствуют:
- [ ] **Connection Pooling**: `LDAPConnectionPool` с настройками `minSize`, `maxSize`, `healthCheck`
- [ ] **LDAPS Support**: Конфигурация SSL/TLS, `SSLUtil`, truststore
- [ ] **Resilience**: Retry-логика при таймаутах, graceful fallback при недоступности LDAP
- [ ] **Group Mapping**: Полноценный маппинг LDAP-групп в роли IP-CSS (`VIEWER`, `OPERATOR`, `ADMIN`)
- [ ] **Attribute Extraction**: Маппинг LDAP-атрибутов (`uid`, `mail`, `cn`, `description`) в доменную модель пользователя
- [ ] **Logging**: Структурированное логирование LDAP-операций без раскрытия_sensitive данных_

### 3.4 Общий аудит кодовой базы
- [ ] Поиск и обработка оставшихся `TODO`, `FIXME`, `HACK`, `stub`, `placeholder`
- [ ] Проверка `WebSocketServer.kt` на предмет backpressure и лимитов размера фреймов
- [ ] Проверка `SecurityLogger` и `AuditService` на отсутствие утечек чувствительных данных

---

## 4. План работ

### Этап 1: Аудит и восстановление `RequestValidator` (1.5 дня)
| № | Задача | Результат |
|---|--------|-----------|
| 1.1 | Восстановить удалённые методы валидации (bulk delete/export, analytics) | Методы возвращают `ValidationResult` |
| 1.2 | Протестировать валидатор на корректных/некорректных DTO | Unit-тесты green |
| 1.3 | Интегрировать валидатор в маршруты (CameraRoutes, RecordingRoutes, AnalyticsRoutes) | Маршруты используют валидатор |

### Этап 2: Реализация ONVIF-маршрутов (1 день)
| № | Задача | Результат |
|---|--------|----------|
| 2.1 | Подключить `CameraRepository` к `OnvifRoutes` | Внедрение зависимостей через Koin |
| 2.2 | Реализовать извлечение IP/учётных данных из БД | Запросы к БД, обработка ошибок |
| 2.3 | Заменить хардкод на реальные значения, добавить валидацию | `TODO` удалены, код чистый |

### Этап 3: LDAP Production Hardening (2-3 дня)
| № | Задача | Результат |
|---|--------|----------|
| 3.1 | Внедрить `LDAPConnectionPool` | Подключение, переиспользование, закрытие |
| 3.2 | Добавить поддержку LDAPS (`SSLUtil`, `TrustStoreTrustManager`) | Конфигурация из `.env` |
| 3.3 | Реализовать retry-логика и graceful degradation | Fallback на DB-аутентификацию |
| 3.4 | Реализовать маппинг групп и атрибутов | Пользователь получает роли из LDAP |

### Этап 4: Финальная проверка и документация (0.5 дня)
| № | Задача | Результат |
|---|--------|----------|
| 4.1 | Полная сборка `installDist` | Без ошибок |
| 4.2 | Integration-тесты ONVIF + LDAP | Green |
| 4.3 | Обновление `docs/phase4/` и `README.md` | Актуальные инструкции |

---

## 5. Критерии приемки (Acceptance Criteria)

### 5.1 Обязательные
- [ ] `RequestValidator` содержит все удалённые методы, покрытие тестами ≥ 80%
- [ ] `OnvifRoutes` извлекает данные камер из БД, хардкод удалён
- [ ] LDAP-подключение использует пул, поддерживает LDAPS
- [ ] При недоступности LDAP сервер продолжает работу (fallback на JWT/DB)
- [ ] В коде отсутствуют `TODO`, `FIXME`, `throw UnsupportedOperationException`, `return null` (кроме случаев явного business logic)
- [ ] Сборка `gradlew :server:api:installDist` проходит успешно

### 5.2 Не breaking changes
- [ ] Существующие API маршруты и ответы не изменены
- [ ] `.env` переменные остаются совместимыми
- [ ] Формат DTO не нарушен

---

## 6. Ресурсы и ссылки

### 6.1 Затронутые файлы
```
server/api/src/main/kotlin/com/company/ipcamera/server/validation/RequestValidator.kt
server/api/src/main/kotlin/com/company/ipcamera/server/controller/OnvifRoutes.kt
server/api/src/main/kotlin/com/company/ipcamera/server/config/LdapConfig.kt
server/api/src/main/kotlin/com/company/ipcamera/server/config/EnterpriseAuthConfig.kt
server/api/src/main/kotlin/com/company/ipcamera/server/websocket/WebSocketServer.kt
```

### 6.2 Зависимости
- UnboundID LDAP SDK for Java v7.0.0 (уже подключен)
- Koin (DI)
- Kotlinx Coroutines (async/await)

### 6.3 Предыдущие задачи
- `IP-CSS-DEV-042` (LDAP SDK v7.x адаптация) — выполнена частично, требуется hardening

---

## 7. Риски

| Риск | Вероятность | Влияние | Митигация |
|------|-------------|---------|-----------|
| Утеряна логика удалённых методов валидации | Средняя | Высокое | Восстановление из Git history v1.0.0 или реверс-инжиниринг из DTO |
| LDAP пул усложнит конфигурацию | Низкая | Среднее | Изолированный конфиг-класс, дефолтные safe-значения |
| ONVIF-маршруты зависят от структуры БД камер | Средняя | Среднее | Провести реверс-инжиниринг схемы БД, добавить migration если нужно |

---

## 8. Оценка усилий

| Этап | Оценка (дни) |
|------|-------------|
| 1. Аудит и `RequestValidator` | 1.5 |
| 2. `OnvifRoutes` | 1 |
| 3. LDAP Hardening | 2-3 |
| 4. Финальная проверка | 0.5 |
| **Итого** | **5-6 рабочих дней** |

---

## 9. Ответственные

- **Исполнитель**: Backend Developer
- **Ревьюер**: Tech Lead / Security Team
- **Заказчик**: Product Owner

---

## 10. Дедлайн

- **Аудит и валидация**: [указать дату + 2 дня]
- **ONVIF и LDAP Hardening**: [указать дату + 4 дня]
- **Финальная проверка**: [указать дату + 6 дней]

---

**Дата создания**: 2024-XX-XX  
**Версия документа**: 1.0  
**Статус**: На согласовании
