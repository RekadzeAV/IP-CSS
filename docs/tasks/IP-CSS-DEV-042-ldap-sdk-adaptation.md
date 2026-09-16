# Постановка задачи: Адаптация LDAP SDK под актуальную версию

## ID задачи
IP-CSS-DEV-042

## Название
Исследование и адаптация интеграции LDAP/Active Directory под UnboundID LDAP SDK v7.x

## Статус
Новая задача

## Приоритет
High

## Компонент
`server/api/src/main/kotlin/com/company/ipcamera/server/config/LdapConfig.kt`
`server/api/src/main/kotlin/com/company/ipcamera/server/config/EnterpriseAuthConfig.kt`

---

## 1. Проблема

### 1.1 Описание
Проект IP-CSS v1.0.0 использует **UnboundID LDAP SDK for Java** для интеграции с LDAP/Active Directory сервером (NAS LDAP). 

В процессе подготовки Docker-образа v1.0.0 обнаружено, что код использует **устаревший API SDK**, который несовместим с актуальной версией **7.0.0**, подключенной в проекте.

**Затронутые файлы:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/config/LdapConfig.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/config/EnterpriseAuthConfig.kt`

### 1.2 Симптомы ошибки

При попытке сборки проекта (`gradlew :server:api:installDist`) возникают ошибки компиляции:

```
e: Unresolved reference 'connect'
e: Unresolved reference 'bind' / 'simpleBind'
e: Unresolved reference 'entries'
e: Unresolved reference 'SUBTREE'
e: Unresolved reference 'allEntries'
e: Unresolved reference 'getEntry'
e: Argument type mismatch: actual type is 'kotlin.Int', but 'java.net.InetAddress!' was expected
```

### 1.3 Текущее состояние (workaround)
Временно реализованы **заглушки** в `LdapConfig.kt`:
- `connect()` — выбрасывает `UnsupportedOperationException`
- `findUserByUsername()` — всегда возвращает `null`
- `getUserGroups()` — всегда возвращает пустой список
- `validateCredentials()` — всегда возвращает `false`
- `testConnection()` — возвращает `true` без реальной проверки

**Это критично для Enterprise-режима**, где требуется аутентификация через LDAP/AD.

---

## 2. Цель исследования

Определить **актуальный API UnboundID LDAP SDK v7.x** и адаптировать существующий код интеграции LDAP/AD под новую версию API без потери функциональности.

---

## 3. Область исследования

### 3.1 Что исследовать

1. **Документация UnboundID LDAP SDK v7.x**
   - Официальная документация: https://developer.pingidentity.com/getStart/
   - Changelog от v6.x к v7.x
   - Breaking changes и migration guide

2. **API-совместимость**
   - Конструкторы `LDAPConnection` — как создаются подключения
   - Методы аутентификации — `bind()`, `simpleBind()`, `saslBind()`
   - Методы поиска — `search()` — сигнатуры, параметры
   - Работа с результатами — `SearchResult`, `SearchEntry`, коллекция записей
   - Настройки таймаутов — `setConnectionTimeout()`, `setTimeout()`
   - Scope поиска — `SearchScope.SUBTREE` и аналоги

3. **Сравнение с текущим кодом**
   - `LdapConfig.kt`:
     - Подключение: `LDAPConnection(connectionPoolSize, timeout, host, port)`
     - Таймаут: `connection.defaultTimeout = readTimeout`
     - Bind: `connection.bind(bindDn, bindPassword)`
     - Поиск: `connection.search(baseDN, scope, filter, attributes...)`
     - Результаты: `result.entries`, `result.entries[0]`
     - Scope: `com.unboundid.ldap.sdk.SearchScope.SUBTREE`
   
   - `EnterpriseAuthConfig.kt`:
     - `testLdapConnection()` — создание подключения, bind, close

4. **Альтернативные варианты**
   - Использование другого LDAP SDK (например, Apache Directory API)
   - Вынос LDAP-логики в отдельный микросервис
   - Использование встроенного HTTP-клиента для LDAP-запросов (не рекомендуется)

### 3.2 Что НЕ исследовать
- Изменение бизнес-логики аутентификации
- Изменение архитектуры интеграции
- Настройка LDAP-сервера или инфраструктуры

---

## 4. План доработки

### Этап 1: Исследование (1-2 дня)

| № | Задача | Результат |
|---|--------|-----------|
| 1.1 | Изучить официальную документацию UnboundID LDAP SDK v7.x | Документ с ключевыми изменениями API |
| 1.2 | Протестировать API в изолированном модуле | Working sample кода для каждого метода |
| 1.3 | Составить карту соответствий (old → new API) | Таблица маппинга методов |
| 1.4 | Оценить сложность адаптации | Оценка и выявление рисков |

### Этап 2: Реализация (2-3 дня)

| № | Задача | Описание |
|---|--------|----------|
| 2.1 | Обновить `LdapConfig.kt` | Заменить заглушки на рабочий код под SDK v7.x |
| 2.2 | Обновить `EnterpriseAuthConfig.kt` | Исправить `testLdapConnection()` |
| 2.3 | Добавить unit-тесты | Mock-тесты для LDAP-операций |
| 2.4 | Добавить integration-тесты | Тесты с реальным LDAP-сервером (Docker) |

### Этап 3: Тестирование (1-2 дня)

| № | Задача | Описание |
|---|--------|----------|
| 3.1 | Проверить сборку проекта | `gradlew :server:api:installDist` |
| 3.2 | Проверить работу с LDAP | Подключение, поиск пользователя, bind |
| 3.3 | Проверить Enterprise-режим | Аутентификация через LDAP/AD |
| 3.4 | Проверить graceful degradation | Поведение при недоступности LDAP |

### Этап 4: Документирование (0.5 дня)

| № | Задача | Описание |
|---|--------|----------|
| 4.1 | Обновить документацию | README, docs/phase4/ |
| 4.2 | Добавить CHANGELOG | Запись об обновлении LDAP SDK |
| 4.3 | Обновить .env.example | Переменные для LDAP-конфигурации |

---

## 5. Критерии приемки (Acceptance Criteria)

### 5.1 Обязательные
- [ ] Проект собирается без ошибок: `gradlew :server:api:installDist`
- [ ] `LdapConfig.testConnection()` — проверяет доступность LDAP-сервера
- [ ] `LdapConfig.connect()` — создает рабочее подключение к LDAP
- [ ] `LdapConfig.findUserByUsername()` — находит пользователя по username
- [ ] `LdapConfig.getUserGroups()` — возвращает группы пользователя
- [ ] `LdapConfig.validateCredentials()` — проверяет логин/пароль
- [ ] `EnterpriseAuthConfig.testLdapConnection()` — работает корректно
- [ ] Все unit-тесты проходят: `gradlew test`
- [ ] Integration-тесты с LDAP проходят

### 5.2 Дополнительные
- [ ] Graceful degradation: при недоступности LDAP сервер не падает
- [ ] Логирование ошибок LDAP с понятными сообщениями
- [ ] Таймауты настроены корректно (connection, read, search)
- [ ] Поддержка SSL/TLS (LDAPS)
- [ ] Документация обновлена

### 5.3 Не breaking changes
- [ ] Существующие API маршруты не изменены
- [ ] `.env` переменные остаются совместимыми
- [ ] Формат ответов API не изменен
- [ ] backward compatible с v1.0.0

---

## 6. Ресурсы и ссылки

### 6.1 Документация
- UnboundID LDAP SDK for Java: https://developer.pingidentity.com/software-downloads/
- API Reference (v7.x): https://developer.pingidentity.com/unboundid/
- Changelog v6 → v7: https://github.com/PingIdentity/unboundid-ldap-sdk-for-java/releases
- GitHub репозиторий: https://github.com/PingIdentity/unboundid-ldap-sdk-for-java

### 6.2 Затронутые файлы проекта
```
server/api/src/main/kotlin/com/company/ipcamera/server/config/
├── LdapConfig.kt
└── EnterpriseAuthConfig.kt

server/api/build.gradle.kts (dependency: com.unboundid:unboundid-ldapsdk:7.0.0)
```

### 6.3 Текущий status (заглушки)
```kotlin
// LdapConfig.kt — ЗАГЛУШКИ
fun connect(): LDAPConnection {
    throw UnsupportedOperationException("LDAP connection not available - SDK API mismatch")
}

fun findUserByUsername(username: String): SearchResult? = null
fun getUserGroups(username: String): List<String> = emptyList()
fun validateCredentials(username: String, password: String): Boolean = false
```

---

## 7. Риски

| Риск | Вероятность | Влияние | Митигация |
|------|-------------|---------|-----------|
| API v7.x сильно отличается от v6.x | Высокая | Высокое | Детальное исследование перед реализацией |
| Нет тестового LDAP-сервера | Средняя | Среднее | Docker-образ OpenLDAP / Active Directory |
| Breaking changes в бизнес-логике | Низкая | Высокое | Сохранение существующих сигнатур методов |
| Увеличение времени сборки | Низкая | Низкое | Изолированное обновление только LDAP-компонентов |

---

## 8. Оценка усилий

| Этап | Оценка (дни) |
|------|-------------|
| 1. Исследование | 1-2 |
| 2. Реализация | 2-3 |
| 3. Тестирование | 1-2 |
| 4. Документирование | 0.5 |
| **Итого** | **4.5-7.5 рабочих дней** |

---

## 9. Критерии успеха

1. **Функциональность**: LDAP-аутентификация работает в Enterprise-режиме
2. **Стабильность**: Сервер не падает при недоступности LDAP
3. **Совместимость**: Все существующие API остаются рабочими
4. **Качество**: Покрытие тестами ≥ 80% для LDAP-компонентов

---

## 10. Ответственные

- **Исполнитель**: Backend Developer
- **Ревьюер**: Tech Lead / Security Team
- **Заказчик**: Product Owner

---

## 11. Дедлайн

- **Исследование**: [указать дату + 2 дня]
- **Реализация**: [указать дату + 5 дней]
- **Тестирование**: [указать дату + 7 дней]
- **Мердж в main**: [указать дату + 8 дней]

---

## Приложение A: Карта текущего API → ожидаемый новый API

### A.1 LDAPConnection

| Текущий код (v6.x) | Новый API (v7.x) | Статус |
|-------------------|------------------|--------|
| `LDAPConnection(poolSize, timeout, host, port)` | `LDAPConnection().connect(host, port, timeout)` | ⚠️ Изменен |
| `connection.defaultTimeout = ms` | `connection.setTimeout(ms)` | ⚠️ Изменен |
| `connection.bind(dn, password)` | `connection.simpleBind(dn, password)` | ⚠️ Изменен |

### A.2 Поиск

| Текущий код (v6.x) | Новый API (v7.x) | Статус |
|-------------------|------------------|--------|
| `connection.search(base, SUBTREE, filter, attrs...)` | `connection.search(base, SearchScope.SUBTREE, filter, attrs...)` | ⚠️ Изменен |
| `result.entries` | `result.allEntries` или `result.count` | ⚠️ Изменен |
| `result.entries[0]` | `result.getEntry(0)` | ⚠️ Изменен |

### A.3 Scope

| Текущий код (v6.x) | Новый API (v7.x) | Статус |
|-------------------|------------------|--------|
| `com.unboundid.ldap.sdk.SearchScope.SUBTREE` | `SearchScope.SUBTREE` (import) | ⚠️ Возможно изменен |

---

## Приложение B: Тестовые сценарии

### B.1 Успешный сценарий
1. LDAP-сервер доступен
2. Подключение установлено
3. Bind с service account успешен
4. Поиск пользователя по username возвращает запись
5. Группы пользователя получены
6. Bind с учетными данными пользователя успешен

### B.2 Отказоустойчивость
1. LDAP-сервер недоступен
2. Таймаут подключения срабатывает корректно
3. Сервер продолжает работу без LDAP (fallback на DB)
4. Логирование ошибок с понятными сообщениями

### B.3 Безопасность
1. LDAPS (LDAP over SSL) работает
2. Пароли не логируются
3. Bind DN хранится в переменных окружения
4. Таймауты настроены для защиты от DoS

---

**Дата создания**: 2024-XX-XX  
**Версия документа**: 1.0  
**Статус**: На согласовании
