# План устранения уязвимостей безопасности

**Дата составления:** Декабрь 2024
**Версия:** Alfa-0.0.1
**Основан на:** SECURITY_AUDIT_REPORT.md

---

## Обзор

Этот документ содержит пошаговый план устранения всех выявленных уязвимостей безопасности в проекте IP Camera Surveillance System. План разделен на три фазы по приоритету и сложности реализации.

**Общая оценка времени:** 8-12 недель для полного устранения всех уязвимостей

---

## Фаза 1: Критические уязвимости (Недели 1-4)

**Цель:** Устранить все критические уязвимости, блокирующие продакшен-развертывание.

### Задача 1.1: Реализация аутентификации и авторизации на сервере

**Приоритет:** 🔴 КРИТИЧЕСКИЙ
**Оценка времени:** 1-2 недели
**Ответственный:** Backend разработчик

#### Шаги выполнения:

1. **Установить зависимости для аутентификации**
   ```kotlin
   // В server/api/build.gradle.kts
   implementation("io.ktor:ktor-server-auth:2.3.5")
   implementation("io.ktor:ktor-server-auth-jwt:2.3.5")
   implementation("com.auth0:java-jwt:4.4.0")
   ```

2. **Создать конфигурацию JWT**
   - Файл: `server/api/src/main/kotlin/com/company/ipcamera/server/config/JwtConfig.kt`
   - Генерировать секретный ключ из переменной окружения
   - Настроить срок жизни access token (15-30 минут)
   - Настроить срок жизни refresh token (7-30 дней)

3. **Создать middleware для аутентификации**
   - Файл: `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/AuthMiddleware.kt`
   - Установить JWT аутентификацию в Application.module()
   - Добавить проверку токенов на всех защищенных маршрутах

4. **Создать endpoints для аутентификации**
   - Файл: `server/api/src/main/kotlin/com/company/ipcamera/server/routing/AuthRoutes.kt`
   - POST /api/v1/auth/login - вход в систему
   - POST /api/v1/auth/refresh - обновление токена
   - POST /api/v1/auth/logout - выход из системы
   - POST /api/v1/auth/register - регистрация (опционально)

5. **Реализовать систему ролей и прав (RBAC)**
   - Создать enum для ролей и разрешений
   - Файл: `server/api/src/main/kotlin/com/company/ipcamera/server/security/Role.kt`
   - Создать функции проверки прав доступа
   - Интегрировать проверку прав в маршруты

6. **Обновить существующие маршруты**
   - Файл: `server/api/src/main/kotlin/com/company/ipcamera/server/routing/CameraRoutes.kt`
   - Добавить `authenticate("jwt-auth")` ко всем маршрутам
   - Добавить проверку прав для операций (например, только ADMIN может удалять)
   - Исключить только `/health` из аутентификации

7. **Создать middleware для проверки прав**
   - Файл: `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/AuthorizationMiddleware.kt`
   - Проверка ролей и разрешений пользователя

8. **Тестирование**
   - Unit тесты для JWT генерации и валидации
   - Integration тесты для endpoints аутентификации
   - Тесты проверки прав доступа

**Критерии приемки:**
- ✅ Все защищенные endpoints требуют валидный JWT токен
- ✅ Система ролей работает корректно
- ✅ Refresh token механизм функционирует
- ✅ Тесты покрывают все сценарии

**Файлы для изменения:**
- `server/api/build.gradle.kts`
- `server/api/src/main/kotlin/com/company/ipcamera/server/Application.kt`
- Создать новые файлы для auth infrastructure
- `server/api/src/main/kotlin/com/company/ipcamera/server/routing/*.kt`

---

### Задача 1.2: Исправление Docker конфигурации безопасности

**Приоритет:** 🔴 КРИТИЧЕСКИЙ
**Оценка времени:** 2-3 дня
**Ответственный:** DevOps/SRE инженер

#### Шаги выполнения:

1. **Удалить опасные привилегии**
   - Удалить `cap_add: - SYS_ADMIN`
   - Удалить `security_opt: - seccomp:unconfined`
   - Оценить, нужны ли какие-либо capabilities (скорее всего, нет)

2. **Настроить read-only файловую систему**
   ```yaml
   read_only: true
   tmpfs:
     - /tmp
     - /var/tmp
     - /app/tmp
   ```

3. **Использовать non-root пользователь**
   ```yaml
   user: "1000:1000"  # Или создать пользователя в Dockerfile
   ```

4. **Создать Dockerfile с non-root пользователем**
   ```dockerfile
   RUN groupadd -r appuser && useradd -r -g appuser appuser
   USER appuser
   ```

5. **Ограничить volumes только необходимыми**
   - Проверить все volumes на необходимость
   - Использовать именованные volumes вместо bind mounts где возможно

6. **Добавить network policies**
   - Настроить изоляцию сетей
   - Ограничить доступ между сервисами

7. **Тестирование**
   - Проверить, что приложение работает с новыми ограничениями
   - Проверить, что нет необходимости в SYS_ADMIN

**Критерии приемки:**
- ✅ Контейнер работает без SYS_ADMIN
- ✅ Контейнер работает без seccomp:unconfined
- ✅ Контейнер работает от non-root пользователя
- ✅ Все функции приложения работают корректно

**Файлы для изменения:**
- `docker-compose.yml`
- `Dockerfile` (если есть, или создать)

---

### Задача 1.3: Реализация certificate pinning и валидации TLS

**Приоритет:** 🔴 КРИТИЧЕСКИЙ
**Оценка времени:** 1-2 недели
**Ответственный:** Mobile/Network разработчик

#### Шаги выполнения:

1. **Создать общий интерфейс для certificate pinning**
   - Файл: `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.kt`
   - Определить expect/actual механизм

2. **Реализация для Android**
   - Файл: `core/network/src/androidMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.android.kt`
   - Использовать OkHttp CertificatePinner или создать кастомный TrustManager
   - Интегрировать с Ktor HttpClient через OkHttp engine

3. **Реализация для iOS**
   - Файл: `core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.ios.kt`
   - Использовать NSURLSession certificate pinning
   - Интегрировать с Ktor HttpClient через Darwin engine

4. **Реализация для Desktop/JVM**
   - Файл: `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.jvm.kt`
   - Создать кастомный TrustManager
   - Проверять fingerprint сертификатов

5. **Интеграция в ApiClient**
   - Файл: `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/ApiClient.kt`
   - Добавить certificate pinner в конфигурацию
   - Применить к HttpClient при создании

6. **Настройка сертификатов**
   - Добавить SHA-256 fingerprints сертификатов в конфигурацию
   - Создать механизм обновления сертификатов
   - Документировать процесс добавления новых доменов

7. **Настроить минимальную версию TLS**
   - Использовать только TLS 1.2+
   - Запретить SSL и TLS 1.0/1.1

8. **Тестирование**
   - Unit тесты для certificate pinner
   - Integration тесты с реальными сертификатами
   - Тесты на различных платформах

**Критерии приемки:**
- ✅ Certificate pinning работает на всех платформах
- ✅ Соединения отклоняются при несовпадении сертификата
- ✅ Используется только TLS 1.2+
- ✅ Есть механизм обновления сертификатов

**Файлы для изменения:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/ApiClient.kt`
- Создать новые файлы для certificate pinning на каждой платформе
- `core/network/build.gradle.kts` (зависимости)

---

### Задача 1.4: Исправление шифрования паролей камер

**Приоритет:** 🔴 КРИТИЧЕСКИЙ
**Оценка времени:** 1-2 недели
**Ответственный:** Mobile/Platform разработчик

#### Шаги выполнения:

#### Для Android:

1. **Использовать Android Keystore для генерации ключа**
   - Файл: `core/common/src/androidMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.android.kt`
   - Создать ключ в Android Keystore при первом запуске
   - Использовать KeyGenParameterSpec для настройки ключа
   - Ключ должен быть недоступен вне приложения

2. **Реализовать миграцию существующих зашифрованных паролей**
   - Если ключ уже существует в старом формате, мигрировать данные
   - Перешифровать все пароли новым ключом

3. **Добавить обработку ошибок**
   - Обрабатывать случаи, когда Keystore недоступен
   - Логировать ошибки (без чувствительных данных)

#### Для iOS:

1. **Полная реализация Keychain Services**
   - Файл: `core/common/src/iosMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.ios.kt`
   - Использовать SecKey API для генерации ключа
   - Хранить ключ в Keychain с kSecAttrAccessibleWhenUnlockedThisDeviceOnly
   - Использовать Touch ID/Face ID для защиты ключа (опционально)

2. **Реализовать миграцию данных**
   - Перешифровать существующие пароли

#### Для Desktop/JVM:

1. **Использовать Java KeyStore**
   - Файл: `core/common/src/desktopMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.jvm.kt`
   - Создать JKS файл для хранения ключа
   - Защитить KeyStore паролем
   - Хранить KeyStore в безопасном месте (Application Data)

2. **Альтернатива: файл ключей с защитой**
   - Если KeyStore не подходит, использовать зашифрованный файл
   - Шифровать ключ с помощью пользовательского пароля
   - Использовать PBKDF2 для получения ключа шифрования

3. **Миграция данных**
   - Перешифровать существующие пароли

4. **Тестирование на всех платформах**
   - Unit тесты для генерации и использования ключей
   - Тесты миграции данных
   - Тесты обработки ошибок

**Критерии приемки:**
- ✅ Ключи генерируются и хранятся в Keystore/Keychain/JKS
- ✅ Ключи уникальны для каждого устройства
- ✅ Существующие данные мигрированы
- ✅ Тесты покрывают все сценарии
- ✅ Удалены все TODO комментарии

**Файлы для изменения:**
- `core/common/src/androidMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.android.kt`
- `core/common/src/iosMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.ios.kt`
- `core/common/src/desktopMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.jvm.kt`

---

### Задача 1.5: Исправление CORS конфигурации

**Приоритет:** 🔴 КРИТИЧЕСКИЙ
**Оценка времени:** 1 день
**Ответственный:** Backend разработчик

#### Шаги выполнения:

1. **Удалить anyHost() и указать конкретные домены**
   - Файл: `server/api/src/main/kotlin/com/company/ipcamera/server/Application.kt`
   - Получить список разрешенных доменов из конфигурации
   - Добавить production и development домены

2. **Настроить правильные заголовки**
   - Разрешить только необходимые заголовки
   - Установить allowCredentials = true только если нужно

3. **Ограничить HTTP методы**
   - Разрешить только GET, POST, PUT, DELETE
   - Не разрешать OPTIONS для всех (использовать preflight)

4. **Добавить валидацию Origin**
   - Проверять Origin заголовок перед обработкой запроса
   - Логировать подозрительные запросы

5. **Настроить для разных окружений**
   - Разные настройки для dev/staging/production
   - Использовать переменные окружения

6. **Тестирование**
   - Тесты CORS headers
   - Проверить с разных доменов

**Критерии приемки:**
- ✅ anyHost() удален
- ✅ Указаны только разрешенные домены
- ✅ CORS работает корректно для разрешенных доменов
- ✅ Запросы с неразрешенных доменов отклоняются

**Файлы для изменения:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/Application.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/config/Config.kt` (добавить CORS config)

---

### Задача 1.6: Принудительное использование HTTPS

**Приоритет:** 🔴 КРИТИЧЕСКИЙ
**Оценка времени:** 3-5 дней
**Ответственный:** DevOps/Backend разработчик

#### Шаги выполнения:

1. **Android: Удалить cleartext traffic**
   - Файл: `android/app/src/main/AndroidManifest.xml`
   - Установить `android:usesCleartextTraffic="false"`
   - Создать network security config для исключений (если нужно для debug)

2. **Создать network security config для Android**
   - Файл: `android/app/src/main/res/xml/network_security_config.xml`
   - Разрешить cleartext только для localhost в debug режиме
   - В production запретить весь cleartext

3. **Настроить HTTPS на сервере**
   - Использовать reverse proxy (nginx) для HTTPS терминирования
   - Или настроить встроенный HTTPS в Ktor
   - Использовать валидные сертификаты (Let's Encrypt)

4. **Настроить автоматическое перенаправление HTTP -> HTTPS**
   - Middleware для редиректа
   - Настроить в nginx или в приложении

5. **Добавить HSTS заголовок**
   - Strict-Transport-Security: max-age=63072000; includeSubDomains; preload
   - Настроить в nginx или в приложении

6. **Обновить Docker compose для HTTPS**
   - Добавить nginx контейнер с SSL сертификатами
   - Или настроить Ktor с SSL

7. **Обновить Next.js для использования HTTPS**
   - В production использовать HTTPS
   - Для localhost можно оставить HTTP в development

8. **Тестирование**
   - Проверить, что HTTP запросы перенаправляются на HTTPS
   - Проверить работу на всех платформах
   - Проверить сертификаты

**Критерии приемки:**
- ✅ Cleartext traffic отключен в Android
- ✅ Сервер работает на HTTPS
- ✅ HTTP автоматически перенаправляется на HTTPS
- ✅ HSTS заголовок установлен
- ✅ Все соединения используют TLS 1.2+

**Файлы для изменения:**
- `android/app/src/main/AndroidManifest.xml`
- Создать `android/app/src/main/res/xml/network_security_config.xml`
- `server/api/src/main/kotlin/com/company/ipcamera/server/Application.kt`
- `docker-compose.yml`
- `nginx.conf` (если используется nginx)

---

## Фаза 2: Высокие уязвимости (Недели 5-8)

### Задача 2.1: Перемещение токенов в httpOnly cookies

**Приоритет:** 🟠 ВЫСОКИЙ
**Оценка времени:** 3-5 дней
**Ответственный:** Full-stack разработчик

#### Шаги выполнения:

1. **Обновить сервер для установки cookies**
   - Модифицировать login endpoint для установки httpOnly cookies
   - Установить флаги Secure, HttpOnly, SameSite=Strict
   - Установить expires для refresh token

2. **Обновить клиент (Next.js)**
   - Удалить использование localStorage для токенов
   - Обновить axios interceptor для работы с cookies
   - Обновить authService.ts

3. **Реализовать refresh token rotation**
   - При обновлении токена выдавать новый refresh token
   - Инвалидировать старый refresh token
   - Обработать случаи reuse refresh token

4. **Обновить logout**
   - Очищать cookies при выходе
   - Инвалидировать refresh token на сервере

5. **Тестирование**
   - Проверить, что токены недоступны через JavaScript
   - Проверить работу refresh token
   - Проверить logout

**Критерии приемки:**
- ✅ Токены хранятся в httpOnly cookies
- ✅ Токены недоступны через JavaScript
- ✅ Refresh token rotation работает
- ✅ Logout корректно очищает cookies

**Файлы для изменения:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/routing/AuthRoutes.kt`
- `server/web/src/services/authService.ts`
- `server/web/src/utils/api.ts`

---

### Задача 2.2: Добавление security headers в Next.js

**Приоритет:** 🟠 ВЫСОКИЙ
**Оценка времени:** 2-3 дня
**Ответственный:** Frontend разработчик

#### Шаги выполнения:

1. **Создать middleware для security headers**
   - Файл: `server/web/src/middleware.ts` (обновить существующий)
   - Или создать отдельный middleware для headers

2. **Добавить все необходимые headers**
   - Content-Security-Policy
   - X-Frame-Options: DENY
   - X-Content-Type-Options: nosniff
   - Referrer-Policy: strict-origin-when-cross-origin
   - Permissions-Policy
   - Strict-Transport-Security (если не на уровне nginx)

3. **Настроить CSP для приложения**
   - Определить все источники скриптов, стилей, изображений
   - Настроить правила CSP
   - Протестировать, что ничего не сломано

4. **Добавить в next.config.js**
   - Или использовать headers в next.config.js
   - Или middleware (предпочтительно)

5. **Тестирование**
   - Проверить все headers через dev tools
   - Проверить, что CSP не блокирует легитимный контент
   - Использовать CSP evaluator

**Критерии приемки:**
- ✅ Все security headers установлены
- ✅ CSP настроен правильно
- ✅ Приложение работает без ошибок
- ✅ Headers проверены через security scanner

**Статус:** ✅ **ЗАВЕРШЕНО** (26 January 2026)

**Реализовано:**
- ✅ Security Headers middleware для Ktor API
- ✅ Security Headers конфигурация для Next.js
- ✅ Все основные headers настроены (CSP, X-Frame-Options, HSTS, и др.)
- ✅ Разделение настроек для development и production

**Документация:**
- См. [SECURITY_HEADERS.md](../docs/SECURITY_HEADERS.md) - Полная документация по Security Headers

**Файлы для изменения:**
- `server/web/src/middleware.ts`
- `server/web/next.config.js`

---

### Задача 2.3: Реализация rate limiting

**Приоритет:** 🟠 ВЫСОКИЙ
**Оценка времени:** 1 неделя
**Ответственный:** Backend разработчик

#### Шаги выполнения:

1. **Выбрать библиотеку для rate limiting**
   - Варианты: Redis-based, in-memory, или кастомная реализация
   - Рекомендуется Redis для распределенных систем

2. **Установить зависимости**
   - Добавить Redis клиент
   - Или использовать библиотеку rate limiting для Ktor

3. **Создать rate limiter middleware**
   - Файл: `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/RateLimitMiddleware.kt`
   - Реализовать sliding window или token bucket алгоритм

4. **Настроить разные лимиты для разных endpoints**
   - Общий лимит: 100 запросов в минуту
   - Login endpoint: 5 попыток в минуту
   - Registration: 3 попытки в час
   - Другие endpoints по необходимости

5. **Добавить идентификацию клиентов**
   - По IP адресу
   - По user ID (если аутентифицирован)
   - Обработать прокси и load balancers (X-Forwarded-For)

6. **Добавить заголовки ответа**
   - X-RateLimit-Limit
   - X-RateLimit-Remaining
   - X-RateLimit-Reset

7. **Интегрировать в Application**
   - Применить middleware к нужным маршрутам
   - Исключить public endpoints (health check)

8. **Реализовать circuit breaker в клиенте**
   - Файл: `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/CircuitBreaker.kt`
   - Добавить в ApiClient

9. **Тестирование**
   - Unit тесты для rate limiter
   - Integration тесты с реальными запросами
   - Нагрузочное тестирование

**Критерии приемки:**
- ✅ Rate limiting работает на всех защищенных endpoints
- ✅ Разные лимиты для разных типов запросов
   - ✅ Circuit breaker работает в клиенте
   - ✅ Правильные заголовки ответа
   - ✅ Тесты покрывают все сценарии

**Файлы для изменения:**
- `server/api/build.gradle.kts`
- Создать `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/RateLimitMiddleware.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/Application.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/ApiClient.kt`

---

### Задача 2.4: Удаление небезопасных паролей по умолчанию

**Приоритет:** 🟠 ВЫСОКИЙ
**Оценка времени:** 1-2 дня
**Ответственный:** DevOps инженер

#### Шаги выполнения:

1. **Удалить значения по умолчанию из docker-compose.yml**
   - Заменить `:-changeme` на требование установки переменной
   - Использовать `:?` синтаксис для обязательных переменных

2. **Использовать Docker secrets**
   - Или внешний vault (HashiCorp Vault, AWS Secrets Manager)
   - Или .env файл (не коммитить в git)

3. **Создать скрипт генерации паролей**
   - Скрипт для генерации случайных паролей
   - Документировать процесс

4. **Обновить документацию**
   - Добавить инструкции по установке переменных окружения
   - Добавить в README.md или DEPLOYMENT_GUIDE.md

5. **Создать .env.example**
   - Файл с примером переменных (без реальных значений)
   - Добавить комментарии

6. **Обновить .gitignore**
   - Убедиться, что .env файлы игнорируются

7. **Тестирование**
   - Проверить, что приложение не запускается без переменных
   - Проверить работу с правильно установленными переменными

**Критерии приемки:**
- ✅ Нет паролей по умолчанию
- ✅ Приложение требует установки всех переменных
- ✅ Документация обновлена
- ✅ .env файлы в .gitignore

**Файлы для изменения:**
- `docker-compose.yml`
- Создать `.env.example`
- Обновить `.gitignore`
- Обновить `docs/DEPLOYMENT_GUIDE.md`

---

### Задача 2.5: Добавление валидации входных данных на сервере

**Приоритет:** 🟠 ВЫСОКИЙ
**Оценка времени:** 1 неделя
**Ответственный:** Backend разработчик

#### Шаги выполнения:

1. **Использовать существующий InputValidator на сервере**
   - Импортировать из core/common модуля
   - Или создать серверную версию

2. **Добавить валидацию в каждый endpoint**
   - POST /api/v1/cameras - валидировать CreateCameraRequest
   - PUT /api/v1/cameras/{id} - валидировать UpdateCameraRequest
   - POST /api/v1/auth/login - валидировать LoginRequest
   - И т.д.

3. **Создать DTO валидаторы**
   - Использовать kotlinx.serialization validation
   - Или кастомную валидацию

4. **Добавить санитизацию данных**
   - Использовать функции sanitize из InputValidator
   - Очистить данные перед сохранением в БД

5. **Валидация типов данных**
   - Проверка форматов (email, URL, и т.д.)
   - Проверка длин строк
   - Проверка числовых диапазонов

6. **Возвращать понятные ошибки валидации**
   - Структурированные сообщения об ошибках
   - HTTP 400 Bad Request с деталями

7. **Тестирование**
   - Unit тесты для валидаторов
   - Integration тесты для endpoints
   - Тесты на SQL инъекции, XSS

**Критерии приемки:**
- ✅ Все входные данные валидируются
- ✅ Некорректные данные отклоняются с понятными ошибками
- ✅ Данные санитизируются перед сохранением
- ✅ Тесты покрывают все сценарии

**Файлы для изменения:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/routing/*.kt`
- Создать валидаторы для DTO
- `server/api/src/main/kotlin/com/company/ipcamera/server/dto/*.kt`

---

### Задача 2.6: Реализация логирования безопасности

**Приоритет:** 🟠 ВЫСОКИЙ
**Оценка времени:** 1 неделя
**Ответственный:** Backend разработчик + Security Engineer

#### Шаги выполнения:

1. **Определить события для логирования**
   - Попытки входа (успешные и неуспешные)
   - Операции с камерами (создание, обновление, удаление)
   - Ошибки авторизации и доступа
   - Подозрительная активность (множественные неудачные попытки)
   - Изменения настроек безопасности

2. **Создать структурированное логирование**
   - Использовать JSON формат
   - Стандартизированные поля (timestamp, user, action, result, IP, и т.д.)
   - Файл: `server/api/src/main/kotlin/com/company/ipcamera/server/logging/SecurityLogger.kt`

3. **Интегрировать логирование в endpoints**
   - Добавить логирование в AuthRoutes
   - Добавить логирование в CameraRoutes
   - Добавить логирование в другие критичные endpoints

4. **Настроить уровни логирования**
   - ERROR для критичных событий
   - WARN для подозрительной активности
   - INFO для нормальных операций

5. **Обеспечить безопасность логов**
   - НЕ логировать пароли, токены, ключи
   - Маскировать чувствительные данные
   - Хранить логи в безопасном месте

6. **Настроить централизованный сбор логов**
   - ELK stack, Splunk, или облачный сервис
   - Настроить ротацию логов
   - Настроить retention policy

7. **Создать dashboard для мониторинга**
   - Отслеживание попыток входа
   - Обнаружение подозрительной активности
   - Алерты при аномалиях

8. **Тестирование**
   - Проверить, что все события логируются
   - Проверить, что чувствительные данные не логируются
   - Проверить формат логов

**Критерии приемки:**
- ✅ Все критичные события логируются
- ✅ Логи структурированы (JSON)
- ✅ Чувствительные данные не логируются
- ✅ Есть механизм централизованного сбора
- ✅ Есть dashboard для мониторинга

**Файлы для изменения:**
- Создать `server/api/src/main/kotlin/com/company/ipcamera/server/logging/SecurityLogger.kt`
- Обновить все routing файлы
- Создать конфигурацию для логирования

---

## Фаза 3: Средние уязвимости (Недели 9-12)

### Задача 3.1: Исправление Android allowBackup

**Приоритет:** 🟡 СРЕДНЯЯ
**Оценка времени:** 1 день
**Ответственный:** Android разработчик

#### Шаги выполнения:

1. **Определить необходимость резервного копирования**
   - Оценить, нужен ли backup для приложения
   - Если нет - отключить полностью

2. **Если backup не нужен:**
   - Установить `android:allowBackup="false"` в AndroidManifest.xml

3. **Если backup нужен:**
   - Создать `android/app/src/main/res/xml/backup_rules.xml`
   - Исключить чувствительные данные (пароли, токены, база данных)
   - Указать в AndroidManifest.xml: `android:fullBackupContent="@xml/backup_rules"`

4. **Тестирование**
   - Проверить backup через adb
   - Убедиться, что чувствительные данные не включены

**Критерии приемки:**
- ✅ Backup отключен или настроен правильно
- ✅ Чувствительные данные не включены в backup
- ✅ Тесты пройдены

**Файлы для изменения:**
- `android/app/src/main/AndroidManifest.xml`
- Создать `android/app/src/main/res/xml/backup_rules.xml` (если нужно)

---

### Задача 3.2: Завершение реализации шифрования для iOS и Desktop

**Приоритет:** 🟡 СРЕДНЯЯ
**Оценка времени:** 1-2 недели
**Ответственный:** iOS/Desktop разработчик

#### Шаги выполнения:

1. **iOS: Завершить Keychain реализацию**
   - Реализовать все TODO в PasswordEncryption.ios.kt
   - Использовать SecKey API для генерации ключей
   - Настроить правильные атрибуты Keychain

2. **Desktop: Завершить KeyStore реализацию**
   - Реализовать Java KeyStore
   - Или альтернативный механизм с защитой

3. **Удалить все TODO комментарии**
   - Завершить все незавершенные части
   - Добавить документацию

4. **Добавить тесты**
   - Unit тесты для всех платформ
   - Integration тесты

5. **Тестирование**
   - Протестировать на реальных устройствах
   - Проверить миграцию данных

**Критерии приемки:**
- ✅ Все TODO завершены
- ✅ Keychain/KeyStore используются правильно
- ✅ Тесты покрывают все сценарии
- ✅ Работает на всех платформах

**Файлы для изменения:**
- `core/common/src/iosMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.ios.kt`
- `core/common/src/desktopMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.jvm.kt`

---

### Задача 3.3: Реализация проверки целостности лицензий

**Приоритет:** 🟡 СРЕДНЯЯ
**Оценка времени:** 1-2 недели
**Ответственный:** Backend/Mobile разработчик

#### Шаги выполнения:

1. **Определить алгоритм цифровой подписи**
   - RSA-2048 или ECC (рекомендуется ECC P-256)
   - Выбрать формат подписи (PKCS#1, PSS, и т.д.)

2. **Создать инфраструктуру для подписи на сервере**
   - Генерация ключевой пары
   - Подпись лицензий при выдаче
   - Хранение приватного ключа в безопасности

3. **Реализовать проверку подписи на клиенте**
   - Файл: `core/license/src/commonMain/kotlin/com/company/ipcamera/core/license/LicenseVerifier.kt`
   - Проверка подписи при загрузке лицензии
   - Проверка срока действия

4. **Реализовать проверку привязки к устройству**
   - Генерировать уникальный идентификатор устройства
   - Включать в лицензию
   - Проверять при валидации

5. **Реализовать проверку хеша**
   - Вычислять SHA-256 хеш лицензии
   - Включать в подпись
   - Проверять при валидации

6. **Обновить LicenseManager**
   - Реализовать checkLicenseIntegrity()
   - Интегрировать все проверки

7. **Тестирование**
   - Unit тесты для проверки подписи
   - Тесты на поддельные лицензии
   - Тесты на истекшие лицензии

**Критерии приемки:**
- ✅ Лицензии подписываются при выдаче
- ✅ Подпись проверяется на клиенте
- ✅ Проверяется привязка к устройству
- ✅ Проверяется хеш и срок действия
- ✅ Поддельные лицензии отклоняются

**Файлы для изменения:**
- `core/license/src/commonMain/kotlin/com/company/ipcamera/core/license/LicenseManager.kt`
- Создать файлы для подписи и проверки
- Серверная часть для генерации лицензий

---

### Задача 3.4: Защита от path traversal в FileSystem

**Приоритет:** 🟡 СРЕДНЯЯ
**Оценка времени:** 3-5 дней
**Ответственный:** Platform разработчик

#### Шаги выполнения:

1. **Добавить валидацию путей во все FileSystem операции**
   - Файлы: `shared/src/*/kotlin/com/company/ipcamera/shared/common/FileSystem.*.kt`
   - Проверка на наличие `..` в пути
   - Проверка на абсолютные пути

2. **Реализовать нормализацию путей**
   - Использовать `Path.normalize()` или эквивалент
   - Проверять, что нормализованный путь в пределах разрешенной директории

3. **Определить базовую директорию**
   - Использовать только поддиректории базовой директории
   - Проверять каждый путь относительно базы

4. **Добавить санитизацию путей**
   - Удалять опасные символы
   - Обрабатывать edge cases

5. **Тестирование**
   - Unit тесты для path traversal атак
   - Тесты на различные варианты `../`
   - Тесты на абсолютные пути

**Критерии приемки:**
- ✅ Все пути валидируются
- ✅ Path traversal атаки блокируются
- ✅ Тесты покрывают все сценарии
- ✅ Работает на всех платформах

**Файлы для изменения:**
- `shared/src/androidMain/kotlin/com/company/ipcamera/shared/common/FileSystem.android.kt`
- `shared/src/iosMain/kotlin/com/company/ipcamera/shared/common/FileSystem.ios.kt`
- `shared/src/desktopMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.jvm.kt`

---

### Задача 3.5: Валидация размера файлов при загрузке

**Приоритет:** 🟡 СРЕДНЯЯ
**Оценка времени:** 2-3 дня
**Ответственный:** Backend разработчик

#### Шаги выполнения:

1. **Добавить проверку размера в ApiClient.upload()**
   - Файл: `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/ApiClient.kt`
   - Проверять размер перед началом загрузки
   - Установить максимальный размер (например, 100MB)

2. **Добавить проверку на сервере**
   - Middleware для проверки Content-Length
   - Проверка размера при получении данных
   - Отклонение слишком больших файлов

3. **Валидация типа файла**
   - Проверка MIME type
   - Проверка расширения файла
   - Whitelist разрешенных типов

4. **Добавить конфигурацию**
   - Максимальный размер из конфигурации
   - Разрешенные типы файлов
   - Разные лимиты для разных типов

5. **Тестирование**
   - Тесты на слишком большие файлы
   - Тесты на неправильные типы файлов
   - Нагрузочное тестирование

**Критерии приемки:**
- ✅ Размер файла проверяется до загрузки
- ✅ Сервер отклоняет слишком большие файлы
- ✅ Тип файла валидируется
- ✅ Тесты покрывают все сценарии

**Файлы для изменения:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/ApiClient.kt`
- Серверная часть для валидации загрузок

---

### Задача 3.6: Защита от перечисления пользователей

**Приоритет:** 🟡 СРЕДНЯЯ
**Оценка времени:** 2-3 дня
**Ответственный:** Backend разработчик

#### Шаги выполнения:

1. **Унифицировать сообщения об ошибках**
   - Файл: `server/api/src/main/kotlin/com/company/ipcamera/server/routing/AuthRoutes.kt`
   - Одинаковое сообщение для несуществующего пользователя и неправильного пароля
   - Общее сообщение: "Неверное имя пользователя или пароль"

2. **Добавить задержку при неуспешных попытках**
   - Exponential backoff
   - Минимальная задержка для всех неуспешных попыток
   - Защита от timing attacks

3. **Удалить информацию о пользователях из API ответов**
   - Не возвращать список всех пользователей без авторизации
   - Не возвращать информацию о существовании пользователя

4. **Добавить капчу после нескольких неудачных попыток**
   - CAPTCHA после 3-5 неудачных попыток
   - Защита от автоматизированных атак

5. **Тестирование**
   - Тесты на перечисление пользователей
   - Проверить, что сообщения одинаковые
   - Проверить задержки

**Критерии приемки:**
- ✅ Сообщения об ошибках унифицированы
- ✅ Есть задержка при неуспешных попытках
- ✅ Невозможно определить существование пользователя
- ✅ Капча работает после нескольких попыток

**Файлы для изменения:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/routing/AuthRoutes.kt`

---

## Общие рекомендации

### Процесс разработки

1. **Code Review**
   - Все изменения безопасности должны проходить строгий code review
   - Особое внимание к аутентификации, авторизации, шифрованию

2. **Тестирование**
   - Обязательные security тесты для каждой задачи
   - Penetration testing перед релизом
   - Автоматические security сканеры в CI/CD

3. **Документация**
   - Обновлять документацию при каждом изменении
   - Документировать security best practices
   - Создать security guidelines для разработчиков

4. **Мониторинг**
   - Настроить алерты на подозрительную активность
   - Регулярный review логов безопасности
   - Incident response plan

### Инструменты

1. **Security сканеры**
   - OWASP ZAP для веб-приложения
   - MobSF для мобильных приложений
   - Snyk для зависимостей
   - SonarQube для кода

2. **Зависимости**
   - Регулярно обновлять зависимости
   - Проверять на известные уязвимости
   - Использовать только проверенные библиотеки

3. **CI/CD**
   - Автоматические security тесты
   - Dependency scanning
   - SAST (Static Application Security Testing)

---

## Временная шкала

```
Неделя 1-2:  Задача 1.1 - Аутентификация (часть 1)
Неделя 2-3:  Задача 1.1 - Аутентификация (часть 2) + Задача 1.2 - Docker
Неделя 3-4:  Задача 1.3 - Certificate Pinning + Задача 1.4 - Шифрование
Неделя 4:    Задача 1.5 - CORS + Задача 1.6 - HTTPS

Неделя 5:    Задача 2.1 - httpOnly Cookies + Задача 2.2 - Security Headers
Неделя 6:    Задача 2.3 - Rate Limiting
Неделя 7:    Задача 2.4 - Пароли + Задача 2.5 - Валидация
Неделя 8:    Задача 2.6 - Логирование

Неделя 9:    Задача 3.1 - Backup + Задача 3.2 - Шифрование (iOS/Desktop)
Неделя 10:   Задача 3.2 - Продолжение + Задача 3.3 - Лицензии (часть 1)
Неделя 11:   Задача 3.3 - Лицензии (часть 2) + Задача 3.4 - Path Traversal
Неделя 12:   Задача 3.5 - Валидация файлов + Задача 3.6 - Перечисление пользователей

Неделя 13:   Финальное тестирование, документация, deployment
```

---

## Метрики успеха

1. **Критические уязвимости:** 0
2. **Высокие уязвимости:** 0
3. **Средние уязвимости:** ≤ 5 (приемлемый уровень для продакшена)
4. **Покрытие тестами безопасности:** ≥ 80%
5. **Соответствие OWASP Top 10:** Все пункты закрыты
6. **Security headers score:** A (Mozilla Observatory)

---

## Ответственность

- **Security Lead:** Общий надзор, архитектурные решения
- **Backend Team:** Серверная часть, API, аутентификация
- **Mobile Team:** Мобильные приложения, шифрование, certificate pinning
- **Frontend Team:** Веб-приложение, security headers, cookies
- **DevOps Team:** Docker, инфраструктура, deployment
- **QA Team:** Тестирование безопасности, penetration testing

---

**Составитель:** Security Team
**Дата:** Декабрь 2024
**Последнее обновление:** 27 January 2026
**Версия плана:** Alfa-0.0.1
**Следующий пересмотр:** После завершения Фазы 1

---

## 🔗 Связанные документы

- [ЭТАП_10_БЕЗОПАСНОСТЬ_ДЕТАЛИЗАЦИЯ.md](status/ЭТАП_10_БЕЗОПАСНОСТЬ_ДЕТАЛИЗАЦИЯ.md) - Детализация этапа 10: Безопасность
- [SECURITY_AUDIT_REPORT.md](SECURITY_AUDIT_REPORT.md) - Полный аудит безопасности
- [ПЛАН_БЕЗОПАСНОСТЬ_2026.md](status/ПЛАН_БЕЗОПАСНОСТЬ_2026.md) - Подробный план доработки
- [БЕЗОПАСНОСТЬ_РЕАЛИЗАЦИЯ_ОТЧЕТ.md](status/БЕЗОПАСНОСТЬ_РЕАЛИЗАЦИЯ_ОТЧЕТ.md) - Отчет о реализации
- [SECURITY_IMPLEMENTATION_PROGRESS.md](status/SECURITY_IMPLEMENTATION_PROGRESS.md) - Прогресс реализации



