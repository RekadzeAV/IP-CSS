# Отчёт: проверка переменных и риски утечки данных / сбоев

**Дата:** 2026-03-01  
**Цель:** Выявить совпадения имён переменных, неверное использование общего состояния и конфигурации, способные вызвать утечку данных или сбои.

---

## 1. Критические несоответствия переменных окружения

### 1.1 ADMIN_PASSWORD vs ADMIN_PASSWORD_HASH

| Место | Переменная | Описание |
|-------|------------|----------|
| **Код** `ServerUserRepository.kt` | `ADMIN_PASSWORD` | Пароль администратора в открытом виде; хешируется при старте |
| **Docker** `docker-compose.yml` | `ADMIN_PASSWORD_HASH` | Ожидается готовый BCrypt-хеш |
| **Документация** `ENVIRONMENT_VARIABLES.md` | `ADMIN_PASSWORD_HASH` | Описан хеш, не пароль |

**Риск:** При развёртывании через текущий docker-compose в контейнер передаётся `ADMIN_PASSWORD_HASH`, а код читает `ADMIN_PASSWORD`. В итоге используется значение по умолчанию `"admin"` — небезопасно и не соответствует намерению админа задать свой пароль через хеш.

**Рекомендация:** В коде поддерживать обе переменные: если задан `ADMIN_PASSWORD_HASH` — использовать его как готовый хеш; если задан `ADMIN_PASSWORD` — хешировать и использовать (для обратной совместимости и локальной разработки).

---

### 1.2 Пароль БД: DATABASE_PASSWORD vs DB_PASSWORD

| Место | Переменная | Использование |
|-------|------------|----------------|
| **Код** `DatabaseConfig.kt` | `DATABASE_PASSWORD` | Пароль для подключения приложения к БД |
| **Docker** `docker-compose.yml` | `DB_PASSWORD` | Пароль контейнера PostgreSQL (`POSTGRES_PASSWORD`) |
| **Документация** | `DB_PASSWORD` | В примерах и гайдах |

**Риск:** В docker-compose для сервиса `surveillance` не пробрасываются `DATABASE_URL`/`DATABASE_PASSWORD` (используется SQLite). При переходе на PostgreSQL нужно задать и `DB_PASSWORD` (для контейнера postgres), и `DATABASE_PASSWORD` (и при необходимости `DATABASE_URL`) для приложения. Разные имена ведут к путанице: можно выставить только `DB_PASSWORD` и получить падение или дефолтный пароль в приложении.

**Рекомендация:** В коде дополнительно читать `DB_PASSWORD`, если `DATABASE_PASSWORD` не задан (например: `System.getenv("DATABASE_PASSWORD") ?: System.getenv("DB_PASSWORD")`). В документации и примерах явно указать оба имени и то, что для приложения при использовании PostgreSQL нужны `DATABASE_URL` и пароль (через `DATABASE_PASSWORD` или `DB_PASSWORD`).

---

### 1.3 ENVIRONMENT vs NODE_ENV

| Место | Переменная | Использование |
|-------|------------|---------------|
| `Application.kt`, `AuthRoutes.kt`, `CsrfMiddleware.kt`, `ExceptionHandlerMiddleware.kt` | `ENVIRONMENT` | production/prod для режима |
| `ServerConfig.kt`, `SecurityHeadersMiddleware.kt` | `NODE_ENV` | Аналогичная проверка (production) |

**Риск:** Разные имена при одной и той же роли. Если выставить только `NODE_ENV=production` или только `ENVIRONMENT=production`, часть кода может считать окружение production, а часть — нет. Возможны расхождения в безопасности (cookies, CORS, логирование ошибок).

**Рекомендация:** Унифицировать на одну переменную (например, `ENVIRONMENT`) и во всех местах читать только её; в документации и docker указать именно её.

---

## 2. Кэш и общее состояние

### 2.1 ApiClient — ключ кэша без учёта авторизации

**Файл:** `core/network/.../ApiClient.kt`

Ключ кэша строится только из `path` и `queryParameters`:

```kotlin
private fun buildCacheKey(path: String, queryParameters: Map<String, String>): String {
    // path + queryString, без authToken / apiKey
}
```

**Риск:** Один экземпляр `ApiClient` (например, в мобильном/десктопном приложении) используется с разными пользователями (разные `authToken`). Ответы, закэшированные для пользователя A по пути вида `/api/v1/cameras`, могут отдаваться пользователю B при том же пути — утечка данных между пользователями.

**Рекомендация:** Включать в ключ кэша контекст авторизации, когда он есть (например, хеш `authToken` или `apiKey`), чтобы кэш был разным для разных пользователей. Либо отключать кэш для запросов с авторизацией.

---

### 2.2 OnvifClient — кэш Digest по URL без учёта пользователя

**Файлы:** `OnvifClient.kt`, `OnvifEventServiceImpl.kt`, `OnvifImagingServiceImpl.kt`, `OnvifAnalyticsServiceImpl.kt`

Кэш Digest-авторизации ключится только по `url`:

```kotlin
digestAuthCache[url] = parsedParams  // url без username
```

Capabilities/DeviceInfo/Profiles кэшируются по `"$deviceUrl:${username ?: ""}"` — корректно.

**Риск:** Два разных пользователя (разные учётные записи) на одном и том же хосте (один URL) разделяют один и тот же кэш nonce/realm. Для Digest это не даёт утечки пароля (ответ считается с текущим username/password), но возможны сбои аутентификации, если сервер привязывает nonce к пользователю или сессии.

**Рекомендация:** Ключить `digestAuthCache` по `"$url:$username"` (или по паре url + username), чтобы кэш был отдельным для каждой пары (хост, пользователь).

---

### 2.3 Синглтон OnvifClient на сервере

**Файл:** `server/.../di/AppModule.kt`

Используется один общий экземпляр `OnvifClient` для всех камер. Кэши (Digest, capabilities, profiles) разделяются между всеми камерами.

**Риск:** Уже отражён в п. 2.2 (ключ только по URL). Плюс при большом числе камер один общий кэш может содержать устаревшие данные, если камеры по-разному настроены. Исправление ключа кэша (url+username) снижает риск перепутывания данных между камерами с разными учётными записями на одном хосте.

---

## 3. Безопасность и конфигурация по умолчанию

### 3.1 Секреты по умолчанию в коде

| Файл | Переменная | Дефолт | Риск |
|------|------------|--------|------|
| `SignedUrlService.kt` | `SIGNED_URL_SECRET` | `"default-secret-key-change-in-production"` | Подпись URL с предсказуемым ключом |
| `JwtConfig.kt` | `JWT_SECRET` | `"your-secret-key-change-in-production-min-32-chars"` | Подделка JWT |
| `CaptchaValidator.kt` | `CAPTCHA_SECRET_KEY` | `generateDefaultSecret()` | Слабый/предсказуемый ключ |
| `DatabaseConfig.kt` | `DATABASE_PASSWORD` | `"postgres"` | Слабый пароль БД в production |
| `ServerUserRepository.kt` | `ADMIN_PASSWORD` | `"admin"` | Слабый пароль админа |

**Рекомендация:** В production не использовать дефолты для секретов: при отсутствии переменной окружения логировать предупреждение и отказывать в старте или явно документировать, что дефолты только для разработки.

### 3.2 Конфигурация тестовых камер

**Файлы:** `config/test-cameras.example.json`, `config/test-cameras.local.json`

Поля `username`, `password` в JSON. Файл `test-cameras.local.json` в `.gitignore` — хорошо. В примере и документации стоит явно предупреждать не коммитить реальные учётные данные и не использовать production-пароли в тестовых конфигах.

---

## 4. Итоговая таблица действий

| Приоритет | Проблема | Действие |
|-----------|----------|----------|
| Критический | ADMIN_PASSWORD vs ADMIN_PASSWORD_HASH | Поддержать в коде `ADMIN_PASSWORD_HASH`; в docker/docs оставить один согласованный способ (например, хеш в production) |
| Высокий | Кэш ApiClient без учёта auth | Включить в ключ кэша контекст авторизации или отключить кэш для авторизованных запросов |
| Высокий | DATABASE_PASSWORD vs DB_PASSWORD | В коде при отсутствии `DATABASE_PASSWORD` читать `DB_PASSWORD`; в документации описать оба варианта |
| Средний | digestAuthCache по URL | Ключить кэш по `url + username` в OnvifClient и связанных сервисах |
| Средний | ENVIRONMENT vs NODE_ENV | Унифицировать на одну переменную (например, `ENVIRONMENT`) во всём коде и в конфигах |
| Низкий | Дефолтные секреты | В production требовать явную установку секретов или падать/предупреждать при дефолтах |

---

## 5. Проверенные и приемлемые моменты

- **LoginAttemptTracker:** ключ в Redis — `login_attempts:$identifier` (identifier = IP или IP+username), утечки паролей нет.
- **ApiClient.updateAuthToken:** создаётся новый экземпляр клиента с новой конфигурацией, общее изменяемое состояние не передаётся между пользователями через один экземпляр.
- **OnvifClient:** capabilities/deviceInfo/profiles кэшируются по `deviceUrl + username` — разделение по пользователям соблюдено.
- **ResponseCache в ApiClient:** размер и TTL ограничены; основная проблема — ключ кэша без учёта авторизации (см. п. 2.1).

Документ можно использовать для пошагового устранения рисков и согласования переменных окружения между кодом, Docker и документацией.
