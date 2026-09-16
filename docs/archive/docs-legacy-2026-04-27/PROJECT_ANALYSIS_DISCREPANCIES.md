# Анализ неточностей и расхождений проекта IP-CSS

**Дата анализа:** 26 January 2026
**Версия проекта:** Alfa-0.0.1
**Аналитик:** AI Code Review

> 📋 **Варианты решения:** Детальные варианты решения всех задокументированных проблем доступны в [docs/SOLUTIONS_FOR_DISCREPANCIES.md](SOLUTIONS_FOR_DISCREPANCIES.md)

---

## 📊 Сводная таблица всех обнаруженных проблем

| # | Категория | Проблема | Приоритет | Статус | Файлы/Местоположение |
|---|-----------|----------|-----------|--------|----------------------|
| 1 | **Архитектура** | Циклическая зависимость `:core:license` ↔ `:shared` | 🔴 КРИТИЧЕСКИЙ | ❌ Требует исправления | `core/license/build.gradle.kts:25` |
| 2 | **Код** | Отсутствует импорт `InputValidator` в `CameraRepositoryImpl` | 🔴 КРИТИЧЕСКИЙ | ❌ Требует исправления | `shared/.../CameraRepositoryImpl.kt` |
| 3 | **Зависимости** | Избыточная зависимость `:core:network` в Android app | 🟡 ВЫСОКИЙ | ❌ Требует исправления | `android/app/build.gradle.kts:41` |
| 4 | **Документация** | Расхождение в процентах прогресса (14.5% vs 20%) | 🔵 НИЗКИЙ | ✅ Исправлено | `CURRENT_STATUS.md`, `IMPLEMENTATION_STATUS.md` (TEMP_EDIT_MAP удален) |
| 5 | **Безопасность** | Небезопасные Docker привилегии (SYS_ADMIN, seccomp:unconfined) | 🔴 КРИТИЧЕСКИЙ | ❌ Требует исправления | `docker-compose.yml:31-34` |
| 6 | **Безопасность** | Небезопасные пароли по умолчанию в Docker | 🟠 ВЫСОКАЯ | ❌ Требует исправления | `docker-compose.yml:25,51,66` |
| 7 | **Безопасность** | Отсутствие аутентификации на сервере API | 🔴 КРИТИЧЕСКИЙ | ❌ Требует реализации | `server/api/` |
| 8 | **Безопасность** | Небезопасная CORS конфигурация (anyHost()) | 🔴 КРИТИЧЕСКИЙ | ❌ Требует исправления | `server/api/.../Application.kt` |
| 9 | **Безопасность** | Отсутствие валидации SSL/TLS сертификатов | 🔴 КРИТИЧЕСКИЙ | ❌ Требует реализации | `core/network/.../ApiClient.kt` |
| 10 | **Безопасность** | Хранение токенов в localStorage (XSS уязвимость) | 🟠 ВЫСОКАЯ | ❌ Требует исправления | `server/web/.../authService.ts` |
| 11 | **Код** | Неиспользуемые методы в `CameraRepositoryImpl` | 🟢 СРЕДНИЙ | ❌ Требует удаления/использования | `shared/.../CameraRepositoryImpl.kt` |
| 12 | **Конфигурация** | Docker образ не существует (`company/ip-camera-surveillance:latest`) | 🟢 СРЕДНИЙ | ❌ Требует исправления | `docker-compose.yml:8` |
| 13 | **Конфигурация** | Healthcheck endpoint не существует (`/health`) | 🟢 СРЕДНИЙ | ❌ Требует исправления | `docker-compose.yml:36` |
| 14 | **Документация** | Несоответствие описания SQL injection паттернов | 🔵 НИЗКИЙ | ✅ Исправлено | `docs/ANALYSIS_ERRORS.md` (устаревшая информация) |
| 15 | **Документация** | Множественные файлы с устаревшей информацией о прогрессе | 🔵 НИЗКИЙ | ❌ Требует обновления | Различные MD файлы |

---

## 🔴 КРИТИЧЕСКИЕ ПРОБЛЕМЫ

### 1. Циклическая зависимость между модулями

**Проблема:**
- `:core:license` зависит от `:shared` (строка 25 в `core/license/build.gradle.kts`)
- `:shared` использует `LicenseManager` из `:core:license` через `LicenseRepositoryImpl`
- Создается циклическая зависимость: `:core:license` → `:shared` → `:core:license`

**Последствия:**
- Может вызвать проблемы при сборке проекта
- Нарушает принципы модульной архитектуры
- Усложняет тестирование и поддержку

**Решение:**
1. Удалить зависимость `:core:license` от `:shared`
2. Если `:core:license` нужны типы из `:shared`, вынести их в `:core:common`
3. Или пересмотреть архитектуру: сделать `LicenseRepository` частью `:shared`, а `LicenseManager` - частью `:core:license`, но без зависимости от `:shared`

**Файлы:**
- `core/license/build.gradle.kts:25`
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/repository/LicenseRepositoryImpl.kt`

---

### 2. Отсутствует импорт InputValidator

**Проблема:**
- В `CameraRepositoryImpl` используется `InputValidator.validateCameraUrl()`, `validateCameraName()`, `validateUsername()`, `validatePassword()`
- Явный импорт `com.company.ipcamera.core.common.security.InputValidator` отсутствует
- Код компилируется за счет неявных импортов, но это может привести к проблемам при сборке в некоторых средах

**Решение:**
Добавить импорт в начало файла:
```kotlin
import com.company.ipcamera.core.common.security.InputValidator
```

**Файлы:**
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/repository/CameraRepositoryImpl.kt`

---

### 3. Небезопасные Docker привилегии

**Проблема:**
```yaml
cap_add:
  - SYS_ADMIN
security_opt:
  - seccomp:unconfined
```

**Риски:**
- `SYS_ADMIN` дает практически полный контроль над системой
- `seccomp:unconfined` отключает защиту ядра Linux
- При компрометации контейнера злоумышленник получает доступ к хосту

**Решение:**
- Удалить `SYS_ADMIN` - использовать только необходимые capabilities
- Удалить `seccomp:unconfined` или использовать профиль seccomp
- Добавить `read_only: true` для файловой системы контейнера где возможно
- Использовать `user: "non-root-user"` вместо root

**Файлы:**
- `docker-compose.yml:31-34`

---

### 4. Отсутствие аутентификации на сервере API

**Проблема:**
- Серверная часть (`server/api`) не содержит никакой аутентификации
- Все endpoints доступны без проверки токенов или сессий
- Нет middleware для проверки JWT токенов

**Решение:**
1. Установить и настроить `ktor-server-auth` и `ktor-server-auth-jwt`
2. Добавить middleware для проверки JWT токенов на всех защищенных маршрутах
3. Реализовать систему ролей и прав доступа (RBAC)

**Файлы:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/routing/CameraRoutes.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/Application.kt`

---

### 5. Небезопасная CORS конфигурация

**Проблема:**
```kotlin
install(CORS) {
    anyHost()  // ❌ КРИТИЧНО: Разрешает любые домены
}
```

**Риски:**
- Любой сайт может делать запросы к API от имени пользователя
- Возможность кражи токенов через XSS атаки
- Нет защиты от CSRF атак

**Решение:**
- Удалить `anyHost()` - явно указать разрешенные домены
- Добавить проверку Origin заголовка
- Ограничить разрешенные методы HTTP

**Файлы:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/Application.kt`

---

### 6. Отсутствие валидации SSL/TLS сертификатов

**Проблема:**
- `ApiClient` не имеет certificate pinning
- Нет кастомного TrustManager для валидации сертификатов
- По умолчанию используется системный trust store без дополнительных проверок

**Решение:**
1. Реализовать certificate pinning для всех платформ
2. Создать кастомный TrustManager с проверкой цепочки сертификатов
3. Использовать только TLS 1.2+ (запретить SSL и TLS 1.0/1.1)

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/ApiClient.kt`

---

## 🟡 ВЫСОКИЕ ПРИОРИТЕТЫ

### 7. Избыточная зависимость в Android app

**Проблема:**
- Android app имеет зависимость: `implementation(project(":core:network"))`
- Но `:shared` модуль уже зависит от `:core:network`
- Это избыточная зависимость

**Решение:**
Удалить строку `implementation(project(":core:network"))` из `android/app/build.gradle.kts`, так как зависимость уже транзитивно доступна через `:shared`.

**Файлы:**
- `android/app/build.gradle.kts:41`

---

### 8. Небезопасные пароли по умолчанию в Docker

**Проблема:**
```yaml
ADMIN_PASSWORD_HASH=${ADMIN_PASSWORD_HASH:-changeme}
POSTGRES_PASSWORD=${DB_PASSWORD:-changeme}
REDIS_PASSWORD=${REDIS_PASSWORD:-changeme}
```

**Риски:**
- Пароли по умолчанию легко угадать
- Если переменные окружения не установлены, используются небезопасные значения

**Решение:**
1. Удалить значения по умолчанию - требовать установку переменных
2. Использовать Docker secrets или внешний vault
3. Генерировать случайные пароли при первом запуске

**Файлы:**
- `docker-compose.yml:25,51,66`

---

### 9. Хранение токенов в localStorage

**Проблема:**
```typescript
localStorage.setItem('token', response.data.data.token);
localStorage.setItem('refreshToken', response.data.data.refreshToken);
```

**Риски:**
- localStorage доступен для JavaScript кода, включая XSS скрипты
- Токены могут быть украдены при любой XSS уязвимости

**Решение:**
1. Использовать httpOnly cookies вместо localStorage для токенов
2. Установить флаги Secure и SameSite для cookies
3. Использовать короткоживущие access tokens (15-30 минут)

**Файлы:**
- `server/web/src/services/authService.ts`

---

## 🟢 СРЕДНИЕ ПРИОРИТЕТЫ

### 10. Неиспользуемые методы

**Проблема:**
- Методы `extractIpFromUrl()` и `extractPortFromUrl()` объявлены как `private` в `CameraRepositoryImpl`
- Не используются в коде класса
- Возможно, планировались для использования в `discoverCameras()` или `testConnection()`, но забыты

**Решение:**
- Удалить неиспользуемые методы
- Или использовать их в `discoverCameras()` или `testConnection()` для извлечения IP и порта из URL обнаруженных камер

**Файлы:**
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/repository/CameraRepositoryImpl.kt`

---

### 11. Docker образ не существует

**Проблема:**
```yaml
image: company/ip-camera-surveillance:latest
```

**Проблема:**
- Образ не существует, так как серверная часть не реализована
- Healthcheck использует `/health` endpoint, который не существует

**Решение:**
- Либо удалить docker-compose.yml до реализации сервера
- Либо обновить с корректными образами и конфигурацией
- Либо пометить как пример/заготовку в комментариях

**Файлы:**
- `docker-compose.yml:8,36`

---

## 🔵 НИЗКИЕ ПРИОРИТЕТЫ (Документация)

### 12. Расхождение в процентах прогресса

**Проблема:** ✅ Исправлено
- `TEMP_EDIT_MAP`: **Удален** (был ~14.5%)
- `CURRENT_STATUS.md`: **~20%**
- `IMPLEMENTATION_STATUS.md`: **~20%**
- `PROJECT_FULL_ANALYSIS.md`: **~20%**

**Решение:**
Синхронизировать все документы с актуальным прогрессом **~20%** (согласно `IMPLEMENTATION_STATUS.md`).

**Файлы:**
- ~~`TEMP_EDIT_MAP`~~ (удален)
- `CURRENT_STATUS.md`
- `docs/IMPLEMENTATION_STATUS.md`
- `docs/PROJECT_FULL_ANALYSIS.md`

---

### 13. Устаревшая информация в документации

**Проблема:**
- В `docs/ANALYSIS_ERRORS.md` указано, что список SQL injection паттернов пустой
- На самом деле паттерны реализованы в `InputValidator.kt` (строки 48-50)

**Решение:**
Обновить `docs/ANALYSIS_ERRORS.md` с актуальной информацией.

**Файлы:**
- `docs/ANALYSIS_ERRORS.md:320-344`

---

## 📋 Приоритизированный план исправлений

### Немедленно (критические - блокируют сборку/безопасность):

1. ✅ **Устранить циклическую зависимость `:core:license` ↔ `:shared`**
   - Удалить зависимость `:core:license` от `:shared`
   - Пересмотреть архитектуру модулей

2. ✅ **Добавить импорт `InputValidator`**
   - Добавить `import com.company.ipcamera.core.common.security.InputValidator` в `CameraRepositoryImpl.kt`

3. ✅ **Исправить Docker конфигурацию**
   - Удалить `SYS_ADMIN` и `seccomp:unconfined`
   - Убрать небезопасные пароли по умолчанию

4. ✅ **Реализовать аутентификацию на сервере**
   - Добавить JWT-based аутентификацию
   - Настроить middleware для проверки токенов

5. ✅ **Исправить CORS конфигурацию**
   - Удалить `anyHost()`
   - Указать явные разрешенные домены

6. ✅ **Реализовать валидацию SSL/TLS**
   - Добавить certificate pinning
   - Создать кастомный TrustManager

### В ближайшее время (высокий приоритет):

7. ✅ **Удалить избыточную зависимость в Android app**
   - Удалить `implementation(project(":core:network"))` из `android/app/build.gradle.kts`

8. ✅ **Исправить хранение токенов**
   - Переместить токены в httpOnly cookies

### По возможности (средний приоритет):

9. ✅ **Удалить неиспользуемые методы**
   - Удалить `extractIpFromUrl()` и `extractPortFromUrl()` или использовать их

10. ✅ **Исправить docker-compose.yml**
    - Обновить образы или пометить как заготовку

### Низкий приоритет (документация):

11. ✅ **Синхронизировать документацию**
    - Обновить все файлы с актуальным прогрессом ~20%
    - Исправить устаревшую информацию в `ANALYSIS_ERRORS.md`

---

## 📊 Статистика проблем

| Приоритет | Количество | Статус |
|-----------|------------|--------|
| 🔴 Критический | 6 | ❌ Все требуют исправления |
| 🟡 Высокий | 3 | ❌ Все требуют исправления |
| 🟢 Средний | 2 | ❌ Все требуют исправления |
| 🔵 Низкий | 2 | ❌ Все требуют обновления |
| **ИТОГО** | **13** | **❌ 13 требуют исправления** |

---

## 🎯 Рекомендации

1. **Немедленно начать с критических проблем** - они блокируют безопасность и могут вызвать проблемы при сборке
2. **Создать задачи в issue tracker** для каждой проблемы
3. **Приоритизировать исправления** согласно таблице выше
4. **Регулярно обновлять документацию** для предотвращения расхождений
5. **Настроить автоматические проверки** (detekt, ktlint) для предотвращения подобных проблем в будущем

---

**Последнее обновление:** 26 January 2026

