# Варианты решения задокументированных проблем проекта IP-CSS

**Дата создания:** 26 January 2026
**Версия проекта:** Alfa-0.0.1
**Статус:** Проработанные варианты решения

---

## 📋 Содержание

1. [Критические проблемы](#критические-проблемы)
2. [Высокие приоритеты](#высокие-приоритеты)
3. [Средние приоритеты](#средние-приоритеты)
4. [Низкие приоритеты](#низкие-приоритеты)
5. [План реализации](#план-реализации)

---

## 🔴 КРИТИЧЕСКИЕ ПРОБЛЕМЫ

### 1. Циклическая зависимость `:core:license` ↔ `:shared`

**Проблема:**
- `:core:license` зависит от `:shared` (строка 25 в `core/license/build.gradle.kts`)
- `:shared` использует `LicenseManager` из `:core:license` через `LicenseRepositoryImpl`
- Создается циклическая зависимость: `:core:license` → `:shared` → `:core:license`

**Варианты решения:**

#### Вариант 1: Удалить зависимость `:core:license` от `:shared` (РЕКОМЕНДУЕТСЯ)

**Описание:**
Удалить зависимость `:core:license` от `:shared` и вынести общие типы в `:core:common`.

**Шаги реализации:**
1. Проверить, какие типы из `:shared` используются в `:core:license`
2. Вынести необходимые типы в `:core:common` (например, `License` domain model)
3. Обновить `core/license/build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation(project(":core:common"))  // Вместо :shared
       // ... остальные зависимости
   }
   ```
4. Обновить импорты в `:core:license` модуле
5. Убедиться, что `:shared` может использовать `LicenseManager` без циклической зависимости

**Преимущества:**
- ✅ Устраняет циклическую зависимость
- ✅ Следует принципам чистой архитектуры
- ✅ Улучшает модульность проекта

**Недостатки:**
- ⚠️ Требует рефакторинга типов
- ⚠️ Может потребоваться перемещение некоторых классов

**Оценка времени:** 2-4 часа

---

#### Вариант 2: Переместить `LicenseRepository` в `:core:license`

**Описание:**
Переместить интерфейс и реализацию `LicenseRepository` из `:shared` в `:core:license`.

**Шаги реализации:**
1. Переместить `LicenseRepository` интерфейс из `:shared` в `:core:license`
2. Переместить `LicenseRepositoryImpl` из `:shared` в `:core:license`
3. Обновить зависимости:
   - `:core:license` больше не зависит от `:shared`
   - `:shared` может использовать `LicenseRepository` через интерфейс из `:core:license`
4. Обновить все импорты

**Преимущества:**
- ✅ Устраняет циклическую зависимость
- ✅ Логически группирует лицензионную функциональность

**Недостатки:**
- ⚠️ Нарушает текущую архитектуру (репозитории должны быть в `:shared`)
- ⚠️ Требует больше рефакторинга

**Оценка времени:** 4-6 часов

---

#### Вариант 3: Создать отдельный модуль `:core:license:api`

**Описание:**
Создать отдельный модуль с интерфейсами, который не зависит от `:shared`.

**Шаги реализации:**
1. Создать новый модуль `:core:license:api`
2. Переместить интерфейсы в `:core:license:api`
3. Обновить зависимости:
   - `:core:license` → `:core:license:api`
   - `:shared` → `:core:license:api`
   - `:core:license:api` не зависит ни от чего

**Преимущества:**
- ✅ Полностью устраняет циклическую зависимость
- ✅ Четкое разделение интерфейсов и реализаций

**Недостатки:**
- ⚠️ Усложняет структуру проекта
- ⚠️ Может быть избыточно для текущего проекта

**Оценка времени:** 6-8 часов

**РЕКОМЕНДАЦИЯ:** Использовать **Вариант 1** как наиболее простой и эффективный.

---

### 2. Отсутствует импорт `InputValidator` в `CameraRepositoryImpl`

**Проблема:**
- В `CameraRepositoryImpl` используется `InputValidator.validateCameraUrl()`, `validateCameraName()`, `validateUsername()`, `validatePassword()`
- Явный импорт `com.company.ipcamera.core.common.security.InputValidator` отсутствует

**Варианты решения:**

#### Вариант 1: Добавить явный импорт (РЕКОМЕНДУЕТСЯ)

**Описание:**
Добавить явный импорт в начало файла `CameraRepositoryImpl.kt`.

**Шаги реализации:**
1. Открыть файл `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/repository/CameraRepositoryImpl.kt`
2. Добавить импорт после существующих импортов:
   ```kotlin
   import com.company.ipcamera.core.common.security.InputValidator
   ```

**Преимущества:**
- ✅ Простое и быстрое решение
- ✅ Улучшает читаемость кода
- ✅ Предотвращает проблемы при сборке

**Недостатки:**
- ❌ Нет недостатков

**Оценка времени:** 1 минута

**РЕКОМЕНДАЦИЯ:** Использовать **Вариант 1** - это тривиальное исправление.

---

### 3. Небезопасные Docker привилегии

**Проблема:**
```yaml
cap_add:
  - SYS_ADMIN
security_opt:
  - seccomp:unconfined
```

**Варианты решения:**

#### Вариант 1: Удалить опасные привилегии и использовать минимальные (РЕКОМЕНДУЕТСЯ)

**Описание:**
Удалить `SYS_ADMIN` и `seccomp:unconfined`, использовать только необходимые capabilities.

**Шаги реализации:**
1. Определить, какие capabilities действительно нужны для работы приложения
2. Обновить `docker-compose.yml`:
   ```yaml
   services:
     surveillance:
       # Удалить cap_add: SYS_ADMIN
       # Удалить security_opt: seccomp:unconfined
       # Добавить только необходимые capabilities (если нужны):
       # cap_add:
       #   - NET_ADMIN  # Только если нужен доступ к сети
       #   - SYS_TIME   # Только если нужно управление временем
       read_only: true  # Сделать файловую систему только для чтения
       user: "1000:1000"  # Запускать от непривилегированного пользователя
       tmpfs:
         - /tmp
         - /var/tmp
   ```

3. Если нужен доступ к `/dev/dri` для GPU:
   ```yaml
   devices:
     - /dev/dri:/dev/dri:rw
   # Но без SYS_ADMIN
   ```

**Преимущества:**
- ✅ Значительно повышает безопасность
- ✅ Следует принципу минимальных привилегий
- ✅ Уменьшает поверхность атаки

**Недостатки:**
- ⚠️ Может потребоваться настройка прав доступа к файлам
- ⚠️ Некоторые функции могут не работать (если они действительно требули SYS_ADMIN)

**Оценка времени:** 1-2 часа (включая тестирование)

---

#### Вариант 2: Использовать профиль seccomp

**Описание:**
Вместо `seccomp:unconfined` использовать кастомный профиль seccomp.

**Шаги реализации:**
1. Создать файл `seccomp-profile.json` с минимальными разрешениями
2. Обновить `docker-compose.yml`:
   ```yaml
   security_opt:
     - seccomp=./seccomp-profile.json
   ```

**Преимущества:**
- ✅ Более безопасно, чем `unconfined`
- ✅ Позволяет контролировать системные вызовы

**Недостатки:**
- ⚠️ Требует знания системных вызовов приложения
- ⚠️ Может быть сложно настроить правильно

**Оценка времени:** 4-6 часов

**РЕКОМЕНДАЦИЯ:** Использовать **Вариант 1** с последующим добавлением профиля seccomp при необходимости.

---

### 4. Отсутствие аутентификации на сервере API

**Проблема:**
- Серверная часть (`server/api`) не содержит никакой аутентификации
- Все endpoints доступны без проверки токенов или сессий

**Варианты решения:**

#### Вариант 1: JWT-based аутентификация с Ktor (РЕКОМЕНДУЕТСЯ)

**Описание:**
Реализовать JWT аутентификацию используя `ktor-server-auth` и `ktor-server-auth-jwt`.

**Шаги реализации:**

1. **Добавить зависимости в `server/api/build.gradle.kts`:**
   ```kotlin
   dependencies {
       implementation("io.ktor:ktor-server-auth:$ktor_version")
       implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
       // ... остальные зависимости
   }
   ```

2. **Создать конфигурацию JWT в `server/api/src/main/kotlin/com/company/ipcamera/server/config/JwtConfig.kt`:**
   ```kotlin
   import com.auth0.jwt.JWT
   import com.auth0.jwt.algorithms.Algorithm
   import io.ktor.server.auth.jwt.*

   object JwtConfig {
       private const val secret = System.getenv("JWT_SECRET") ?: "your-secret-key-change-in-production"
       private const val issuer = "ip-css"
       private const val audience = "ip-css-users"
       private const val realm = "IP-CSS"

       val algorithm = Algorithm.HMAC256(secret)

       fun makeJwtVerifier(): JWTAuthenticationProvider.Config.() -> Unit = {
           verifier(
               JWT
                   .require(algorithm)
                   .withIssuer(issuer)
                   .withAudience(audience)
                   .build()
           )
           validate { credential ->
               if (credential.payload.audience.contains(audience)) {
                   JWTPrincipal(credential.payload)
               } else {
                   null
               }
           }
       }

       fun generateToken(userId: String, roles: List<String>): String {
           return JWT.create()
               .withIssuer(issuer)
               .withAudience(audience)
               .withClaim("userId", userId)
               .withClaim("roles", roles)
               .withExpiresAt(Date(System.currentTimeMillis() + 3600000)) // 1 час
               .sign(algorithm)
       }
   }
   ```

3. **Настроить аутентификацию в `Application.kt`:**
   ```kotlin
   import io.ktor.server.auth.*
   import io.ktor.server.auth.jwt.*

   fun Application.module() {
       // ... существующий код

       // JWT Authentication
       install(Authentication) {
           jwt("jwt-auth") {
               realm = JwtConfig.realm
               verifier(JwtConfig.makeJwtVerifier())
           }
       }

       // ... остальной код
   }
   ```

4. **Создать middleware для защищенных маршрутов:**
   ```kotlin
   // server/api/src/main/kotlin/com/company/ipcamera/server/routing/AuthRoutes.kt
   fun Route.authRoutes() {
       route("/auth") {
           post("/login") {
               // Логика входа
               // Генерация JWT токена
           }
           post("/refresh") {
               // Обновление токена
           }
       }
   }
   ```

5. **Защитить существующие маршруты:**
   ```kotlin
   fun Route.cameraRoutes() {
       authenticate("jwt-auth") {
           get("/cameras") {
               // Защищенный endpoint
           }
           // ... остальные endpoints
       }
   }
   ```

6. **Добавить RBAC (роли и права):**
   ```kotlin
   fun Route.requireRole(role: String) {
       authenticate("jwt-auth") {
           validate {
               val principal = it.principal<JWTPrincipal>()
               principal?.payload?.getClaim("roles")?.asList(String::class.java)?.contains(role)
                   ?: false
           }
       }
   }
   ```

**Преимущества:**
- ✅ Стандартный и безопасный подход
- ✅ Stateless (не требует хранения сессий)
- ✅ Масштабируемый
- ✅ Поддерживает refresh tokens

**Недостатки:**
- ⚠️ Требует настройки секретного ключа
- ⚠️ Токены должны быть валидными и не скомпрометированными

**Оценка времени:** 1-2 дня

---

#### Вариант 2: Session-based аутентификация

**Описание:**
Использовать сессии с cookies для аутентификации.

**Преимущества:**
- ✅ Проще в реализации
- ✅ Автоматическая защита от XSS (httpOnly cookies)

**Недостатки:**
- ⚠️ Требует хранения сессий (Redis/база данных)
- ⚠️ Менее масштабируемо
- ⚠️ Проблемы с CORS

**Оценка времени:** 1 день

**РЕКОМЕНДАЦИЯ:** Использовать **Вариант 1** (JWT) для лучшей масштабируемости.

---

### 5. Небезопасная CORS конфигурация

**Проблема:**
```kotlin
install(CORS) {
    anyHost()  // ❌ КРИТИЧНО: Разрешает любые домены
}
```

**Варианты решения:**

#### Вариант 1: Явное указание разрешенных доменов (РЕКОМЕНДУЕТСЯ)

**Описание:**
Удалить `anyHost()` и указать конкретные разрешенные домены из конфигурации.

**Шаги реализации:**

1. **Создать конфигурацию CORS в `server/api/src/main/kotlin/com/company/ipcamera/server/config/CorsConfig.kt`:**
   ```kotlin
   object CorsConfig {
       val allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS")
           ?.split(",")
           ?: listOf("http://localhost:3000", "http://localhost:8080")

       val isDevelopment = System.getenv("ENVIRONMENT") == "development"
   }
   ```

2. **Обновить `Application.kt`:**
   ```kotlin
   import io.ktor.server.plugins.cors.*
   import io.ktor.http.*

   fun Application.module() {
       // ... существующий код

       // CORS
       install(CORS) {
           // Удалить anyHost()
           allowMethod(HttpMethod.Options)
           allowMethod(HttpMethod.Get)
           allowMethod(HttpMethod.Post)
           allowMethod(HttpMethod.Put)
           allowMethod(HttpMethod.Delete)
           allowHeader(HttpHeaders.ContentType)
           allowHeader(HttpHeaders.Authorization)
           allowHeader("X-Requested-With")

           // Разрешенные домены
           CorsConfig.allowedOrigins.forEach { origin ->
               allowHost(origin, schemes = listOf("http", "https"))
           }

           // В development режиме разрешить localhost
           if (CorsConfig.isDevelopment) {
               allowHost("http://localhost:3000")
               allowHost("http://localhost:8080")
           }

           allowCredentials = true
           maxAgeInSeconds = 3600
       }

       // ... остальной код
   }
   ```

3. **Добавить переменные окружения в `docker-compose.yml`:**
   ```yaml
   environment:
     - CORS_ALLOWED_ORIGINS=https://app.company.com,https://admin.company.com
     - ENVIRONMENT=production
   ```

**Преимущества:**
- ✅ Значительно повышает безопасность
- ✅ Предотвращает CSRF атаки
- ✅ Гибкая конфигурация через переменные окружения

**Недостатки:**
- ⚠️ Требует настройки для каждого окружения
- ⚠️ Нужно обновлять при добавлении новых доменов

**Оценка времени:** 1-2 часа

---

#### Вариант 2: Динамическая проверка Origin

**Описание:**
Добавить кастомный interceptor для проверки Origin заголовка.

**Шаги реализации:**
1. Создать whitelist доменов в базе данных или конфигурации
2. Добавить interceptor для проверки Origin перед обработкой запроса
3. Логировать подозрительные запросы

**Преимущества:**
- ✅ Более гибкая система
- ✅ Можно управлять через API

**Недостатки:**
- ⚠️ Более сложная реализация
- ⚠️ Требует дополнительной инфраструктуры

**Оценка времени:** 4-6 часов

**РЕКОМЕНДАЦИЯ:** Использовать **Вариант 1** как наиболее простой и эффективный.

---

### 6. Отсутствие валидации SSL/TLS сертификатов

**Проблема:**
- `ApiClient` не имеет certificate pinning
- Нет кастомного TrustManager для валидации сертификатов

**Варианты решения:**

#### Вариант 1: Certificate Pinning для всех платформ (РЕКОМЕНДУЕТСЯ)

**Описание:**
Реализовать certificate pinning для Android, iOS и Desktop.

**Шаги реализации:**

1. **Для Android (`core/network/src/androidMain/kotlin/com/company/ipcamera/core/network/ApiClient.android.kt`):**
   ```kotlin
   import io.ktor.client.engine.android.*
   import okhttp3.CertificatePinner
   import okhttp3.OkHttpClient

   fun createHttpClientEngine(): HttpClientEngineFactory<HttpClientEngineConfig> {
       return Android.create {
           val certificatePinner = CertificatePinner.Builder()
               .add("api.company.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
               .build()

           val okHttpClient = OkHttpClient.Builder()
               .certificatePinner(certificatePinner)
               .build()

           config {
               this.engine = okHttpClient
           }
       }
   }
   ```

2. **Для iOS (`core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/ApiClient.ios.kt`):**
   ```kotlin
   import io.ktor.client.engine.darwin.*
   import platform.Foundation.*

   fun createHttpClientEngine(): HttpClientEngineFactory<HttpClientEngineConfig> {
       return Darwin.create {
           // Настройка certificate pinning через NSURLSession
           // Требует дополнительной настройки
       }
   }
   ```

3. **Для Desktop/JVM (`core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/ApiClient.jvm.kt`):**
   ```kotlin
   import io.ktor.client.engine.java.*
   import javax.net.ssl.*
   import java.security.cert.X509Certificate

   fun createHttpClientEngine(): HttpClientEngineFactory<HttpClientEngineConfig> {
       return Java.create {
           // Создать кастомный TrustManager с проверкой сертификатов
           val trustManager = object : X509TrustManager {
               override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
               override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                   // Проверка цепочки сертификатов
                   // Certificate pinning логика
               }
               override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
           }

           val sslContext = SSLContext.getInstance("TLS")
           sslContext.init(null, arrayOf(trustManager), null)

           config {
               sslContext = sslContext
           }
       }
   }
   ```

4. **Добавить конфигурацию в `ApiClient.kt`:**
   ```kotlin
   data class ApiClientConfig(
       // ... существующие поля
       val enableCertificatePinning: Boolean = true,
       val pinnedCertificates: Map<String, List<String>> = emptyMap() // host -> list of pins
   )
   ```

5. **Обновить `ApiClient` для использования pinning:**
   ```kotlin
   class ApiClient(config: ApiClientConfig) {
       private val client = HttpClient(createHttpClientEngine()) {
           // ... существующая конфигурация

           if (config.enableCertificatePinning) {
               // Настройка certificate pinning
           }

           // Принудительное использование TLS 1.2+
           engine {
               // Настройки TLS
           }
       }
   }
   ```

**Преимущества:**
- ✅ Защита от MITM атак
- ✅ Высокий уровень безопасности
- ✅ Работает на всех платформах

**Недостатки:**
- ⚠️ Требует обновления при смене сертификатов
- ⚠️ Сложнее в настройке
- ⚠️ Может вызвать проблемы при использовании CDN

**Оценка времени:** 2-3 дня

---

#### Вариант 2: Кастомный TrustManager с проверкой цепочки

**Описание:**
Создать кастомный TrustManager, который проверяет цепочку сертификатов без pinning.

**Преимущества:**
- ✅ Проще в реализации
- ✅ Меньше проблем с обновлением сертификатов

**Недостатки:**
- ⚠️ Меньше защиты от MITM
- ⚠️ Не защищает от компрометации CA

**Оценка времени:** 1 день

**РЕКОМЕНДАЦИЯ:** Использовать **Вариант 1** для максимальной безопасности, но начать с **Варианта 2** для MVP.

---

## 🟡 ВЫСОКИЕ ПРИОРИТЕТЫ

### 7. Избыточная зависимость в Android app

**Проблема:**
- Android app имеет зависимость: `implementation(project(":core:network"))`
- Но `:shared` модуль уже зависит от `:core:network`
- Это избыточная зависимость

**Варианты решения:**

#### Вариант 1: Удалить избыточную зависимость (РЕКОМЕНДУЕТСЯ)

**Описание:**
Удалить строку `implementation(project(":core:network"))` из `android/app/build.gradle.kts`.

**Шаги реализации:**
1. Открыть `android/app/build.gradle.kts`
2. Удалить строку 41: `implementation(project(":core:network"))`
3. Проверить, что код компилируется (зависимость доступна транзитивно через `:shared`)

**Преимущества:**
- ✅ Упрощает зависимости
- ✅ Предотвращает конфликты версий
- ✅ Следует принципу DRY

**Недостатки:**
- ❌ Нет недостатков

**Оценка времени:** 1 минута

**РЕКОМЕНДАЦИЯ:** Использовать **Вариант 1** - это тривиальное исправление.

---

### 8. Небезопасные пароли по умолчанию в Docker

**Проблема:**
```yaml
ADMIN_PASSWORD_HASH=${ADMIN_PASSWORD_HASH:-changeme}
POSTGRES_PASSWORD=${DB_PASSWORD:-changeme}
REDIS_PASSWORD=${REDIS_PASSWORD:-changeme}
```

**Варианты решения:**

#### Вариант 1: Удалить значения по умолчанию и требовать установку (РЕКОМЕНДУЕТСЯ)

**Описание:**
Удалить значения по умолчанию и требовать явную установку переменных окружения.

**Шаги реализации:**

1. **Обновить `docker-compose.yml`:**
   ```yaml
   environment:
     - ADMIN_PASSWORD_HASH=${ADMIN_PASSWORD_HASH}  # Без значения по умолчанию
     - POSTGRES_PASSWORD=${DB_PASSWORD}  # Без значения по умолчанию
     - REDIS_PASSWORD=${REDIS_PASSWORD}  # Без значения по умолчанию
   ```

2. **Создать файл `.env.example`:**
   ```env
   # Копируйте этот файл в .env и заполните значения
   ADMIN_PASSWORD_HASH=your-hashed-password-here
   DB_PASSWORD=your-secure-database-password
   REDIS_PASSWORD=your-secure-redis-password
   JWT_SECRET=your-jwt-secret-key
   ```

3. **Добавить проверку в скрипт запуска `scripts/start.sh`:**
   ```bash
   #!/bin/bash
   if [ -z "$ADMIN_PASSWORD_HASH" ] || [ -z "$DB_PASSWORD" ] || [ -z "$REDIS_PASSWORD" ]; then
       echo "ERROR: Required environment variables are not set!"
       echo "Please create .env file from .env.example and fill in the values."
       exit 1
   fi
   docker-compose up -d
   ```

4. **Обновить документацию в `docs/DEPLOYMENT_GUIDE.md`:**
   - Добавить инструкции по созданию `.env` файла
   - Добавить предупреждение о безопасности

**Преимущества:**
- ✅ Принуждает к использованию безопасных паролей
- ✅ Предотвращает случайное использование небезопасных значений
- ✅ Соответствует best practices

**Недостатки:**
- ⚠️ Требует дополнительной настройки при первом запуске
- ⚠️ Может усложнить разработку (нужно создавать .env)

**Оценка времени:** 1-2 часа

---

#### Вариант 2: Генерация случайных паролей при первом запуске

**Описание:**
Генерировать случайные пароли при первом запуске, если переменные не установлены.

**Шаги реализации:**
1. Создать скрипт `scripts/generate-secrets.sh`:
   ```bash
   #!/bin/bash
   if [ ! -f .env ]; then
       echo "Generating secure passwords..."
       cat > .env << EOF
   ADMIN_PASSWORD_HASH=$(openssl rand -base64 32 | sha256sum | cut -d' ' -f1)
   DB_PASSWORD=$(openssl rand -base64 32)
   REDIS_PASSWORD=$(openssl rand -base64 32)
   JWT_SECRET=$(openssl rand -base64 64)
   EOF
       echo "Generated .env file with secure passwords"
       echo "IMPORTANT: Save these passwords securely!"
   fi
   ```

2. Обновить `docker-compose.yml` для использования сгенерированных паролей

**Преимущества:**
- ✅ Автоматическая генерация безопасных паролей
- ✅ Упрощает первоначальную настройку

**Недостатки:**
- ⚠️ Пароли могут быть потеряны, если не сохранены
- ⚠️ Требует дополнительной логики

**Оценка времени:** 2-3 часа

**РЕКОМЕНДАЦИЯ:** Использовать **Вариант 1** для production, **Вариант 2** можно добавить как опцию для разработки.

---

### 9. Хранение токенов в localStorage

**Проблема:**
```typescript
localStorage.setItem('token', response.data.data.token);
localStorage.setItem('refreshToken', response.data.data.refreshToken);
```

**Варианты решения:**

#### Вариант 1: Использовать httpOnly cookies (РЕКОМЕНДУЕТСЯ)

**Описание:**
Переместить токены в httpOnly cookies на сервере.

**Шаги реализации:**

1. **Обновить серверную часть (`server/api/src/main/kotlin/com/company/ipcamera/server/routing/AuthRoutes.kt`):**
   ```kotlin
   post("/auth/login") {
       // ... проверка credentials

       val token = JwtConfig.generateToken(userId, roles)
       val refreshToken = JwtConfig.generateRefreshToken(userId)

       // Установить cookies
       call.response.cookies.append(
           Cookie(
               name = "access_token",
               value = token,
               httpOnly = true,
               secure = true,  // Только HTTPS
               sameSite = SameSite.Strict,
               maxAge = 3600  // 1 час
           )
       )
       call.response.cookies.append(
           Cookie(
               name = "refresh_token",
               value = refreshToken,
               httpOnly = true,
               secure = true,
               sameSite = SameSite.Strict,
               maxAge = 604800  // 7 дней
           )
       )

       call.respond(mapOf("success" to true))
   }
   ```

2. **Обновить клиентскую часть (`server/web/src/services/authService.ts`):**
   ```typescript
   export const authService = {
     async login(credentials: LoginRequest): Promise<LoginResponse> {
       const response = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', credentials, {
         withCredentials: true  // Важно для отправки cookies
       });

       if (response.data.success) {
         // Токены теперь в cookies, не нужно сохранять в localStorage
         return { token: '', refreshToken: '' };  // Или убрать из ответа
       }
       throw new Error(response.data.message || 'Login failed');
     },

     async logout(): Promise<void> {
       await apiClient.post('/auth/logout', {}, { withCredentials: true });
       // Cookies будут удалены сервером
     },

     isAuthenticated(): boolean {
       // Проверить наличие cookie или сделать запрос к /auth/me
       return this.getToken() !== null;
     },

     getToken(): string | null {
       // Токен теперь в httpOnly cookie, недоступен из JavaScript
       // Нужно получать через API endpoint или использовать другой подход
       return null;
     }
   };
   ```

3. **Обновить `apiClient` для автоматической отправки cookies:**
   ```typescript
   const apiClient: AxiosInstance = axios.create({
     baseURL: API_URL,
     timeout: 30000,
     withCredentials: true,  // Автоматически отправлять cookies
     headers: {
       'Content-Type': 'application/json',
     },
   });

   // Убрать interceptor для добавления токена из localStorage
   // Токен теперь автоматически отправляется в cookie
   ```

4. **Обновить сервер для чтения токена из cookie:**
   ```kotlin
   // В Application.kt или middleware
   install(Authentication) {
       jwt("jwt-auth") {
           realm = JwtConfig.realm
           verifier(JwtConfig.makeJwtVerifier())
           validate { credential ->
               // Также проверять cookie, если заголовок отсутствует
               val tokenFromCookie = call.request.cookies["access_token"]
               val token = credential.token ?: tokenFromCookie
               // ... валидация
           }
       }
   }
   ```

**Преимущества:**
- ✅ Защита от XSS атак (токены недоступны для JavaScript)
- ✅ Автоматическая отправка с каждым запросом
- ✅ Более безопасное хранение

**Недостатки:**
- ⚠️ Требует настройки CORS (allowCredentials = true)
- ⚠️ Может быть сложнее для мобильных приложений
- ⚠️ Нужно обрабатывать CSRF токены

**Оценка времени:** 1 день

---

#### Вариант 2: Использовать sessionStorage вместо localStorage

**Описание:**
Использовать `sessionStorage` вместо `localStorage` для уменьшения времени жизни токенов.

**Преимущества:**
- ✅ Проще в реализации
- ✅ Токены удаляются при закрытии браузера

**Недостатки:**
- ⚠️ Все еще уязвимо для XSS
- ⚠️ Не решает основную проблему

**Оценка времени:** 30 минут

**РЕКОМЕНДАЦИЯ:** Использовать **Вариант 1** (httpOnly cookies) для максимальной безопасности.

---

## 🟢 СРЕДНИЕ ПРИОРИТЕТЫ

### 10. Неиспользуемые методы в `CameraRepositoryImpl`

**Проблема:**
- Методы `extractIpFromUrl()` и `extractPortFromUrl()` объявлены как `private` в `CameraRepositoryImpl`
- Не используются в коде класса

**Варианты решения:**

#### Вариант 1: Удалить неиспользуемые методы (РЕКОМЕНДУЕТСЯ)

**Описание:**
Удалить методы, если они действительно не нужны.

**Шаги реализации:**
1. Проверить, что методы не используются нигде в проекте
2. Удалить методы `extractIpFromUrl()` и `extractPortFromUrl()` из `CameraRepositoryImpl.kt`

**Преимущества:**
- ✅ Упрощает код
- ✅ Уменьшает технический долг
- ✅ Улучшает читаемость

**Недостатки:**
- ⚠️ Если методы планировались для использования, их придется переписать

**Оценка времени:** 5 минут

---

#### Вариант 2: Использовать методы в `discoverCameras()` или `testConnection()`

**Описание:**
Использовать методы для извлечения IP и порта из URL обнаруженных камер.

**Шаги реализации:**
1. Обновить метод `discoverCameras()`:
   ```kotlin
   override suspend fun discoverCameras(): Result<List<Camera>> = withContext(Dispatchers.Default) {
       try {
           val discoveredCameras = onvifClient.discoverCameras()
           discoveredCameras.map { discoveredUrl ->
               val ip = extractIpFromUrl(discoveredUrl)
               val port = extractPortFromUrl(discoveredUrl) ?: 80
               // Создать Camera объект
           }
       } catch (e: Exception) {
           // ...
       }
   }
   ```

2. Обновить метод `testConnection()` для использования этих методов

**Преимущества:**
- ✅ Использует существующий код
- ✅ Улучшает функциональность

**Недостатки:**
- ⚠️ Требует реализации логики использования

**Оценка времени:** 1-2 часа

**РЕКОМЕНДАЦИЯ:** Использовать **Вариант 1**, если методы не планируются к использованию. Если планируются - использовать **Вариант 2**.

---

### 11. Docker образ не существует

**Проблема:**
```yaml
image: company/ip-camera-surveillance:latest
```

**Варианты решения:**

#### Вариант 1: Использовать build вместо image (РЕКОМЕНДУЕТСЯ)

**Описание:**
Использовать `build` для сборки образа из Dockerfile вместо использования несуществующего образа.

**Шаги реализации:**
1. Обновить `docker-compose.yml`:
   ```yaml
   services:
     surveillance:
       build:
         context: .
         dockerfile: Dockerfile
       # Удалить строку: image: company/ip-camera-surveillance:latest
       # Или оставить для тегирования после сборки:
       image: company/ip-camera-surveillance:latest
   ```

2. Убедиться, что `Dockerfile` существует и корректно настроен

**Преимущества:**
- ✅ Решает проблему отсутствующего образа
- ✅ Позволяет собирать образ локально

**Недостатки:**
- ❌ Нет недостатков

**Оценка времени:** 5 минут

---

#### Вариант 2: Пометить как заготовку

**Описание:**
Добавить комментарии, что это заготовка для будущей реализации.

**Шаги реализации:**
1. Добавить комментарии в `docker-compose.yml`:
   ```yaml
   services:
     # TODO: Серверная часть еще не реализована
     # Этот сервис будет работать после реализации server/api
     surveillance:
       # build:
       #   context: .
       #   dockerfile: Dockerfile
       image: company/ip-camera-surveillance:latest  # Пока не существует
   ```

**Преимущества:**
- ✅ Ясно указывает на статус

**Недостатки:**
- ⚠️ Не решает проблему полностью

**Оценка времени:** 2 минуты

**РЕКОМЕНДАЦИЯ:** Использовать **Вариант 1** для немедленного решения.

---

### 12. Healthcheck endpoint не существует

**Проблема:**
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
```

**Варианты решения:**

#### Вариант 1: Реализовать healthcheck endpoint (РЕКОМЕНДУЕТСЯ)

**Описание:**
Создать простой endpoint `/health` для проверки здоровья сервиса.

**Шаги реализации:**

1. **Создать `server/api/src/main/kotlin/com/company/ipcamera/server/routing/HealthRoutes.kt`:**
   ```kotlin
   package com.company.ipcamera.server.routing

   import io.ktor.server.application.*
   import io.ktor.server.response.*
   import io.ktor.server.routing.*

   fun Route.healthRoutes() {
       get("/health") {
           call.respond(mapOf(
               "status" to "ok",
               "timestamp" to System.currentTimeMillis()
           ))
       }

       get("/health/ready") {
           // Проверка готовности (база данных, внешние сервисы)
           val isReady = checkDatabaseConnection() && checkRedisConnection()
           if (isReady) {
               call.respond(mapOf("status" to "ready"))
           } else {
               call.respond(mapOf("status" to "not ready"))
           }
       }

       get("/health/live") {
           // Проверка живости (просто что сервер работает)
           call.respond(mapOf("status" to "alive"))
       }
   }

   private fun checkDatabaseConnection(): Boolean {
       // Проверка подключения к БД
       return try {
           // ... логика проверки
           true
       } catch (e: Exception) {
           false
       }
   }

   private fun checkRedisConnection(): Boolean {
       // Проверка подключения к Redis
       return try {
           // ... логика проверки
           true
       } catch (e: Exception) {
           false
       }
   }
   ```

2. **Добавить маршрут в `Routing.kt`:**
   ```kotlin
   fun Application.configureRouting() {
       routing {
           healthRoutes()
           // ... остальные маршруты
       }
   }
   ```

3. **Обновить `docker-compose.yml` для использования правильного endpoint:**
   ```yaml
   healthcheck:
     test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
     interval: 30s
     timeout: 10s
     retries: 3
     start_period: 40s
   ```

**Преимущества:**
- ✅ Решает проблему
- ✅ Полезно для мониторинга
- ✅ Стандартная практика

**Недостатки:**
- ❌ Нет недостатков

**Оценка времени:** 1-2 часа

---

#### Вариант 2: Временно отключить healthcheck

**Описание:**
Закомментировать healthcheck до реализации endpoint.

**Шаги реализации:**
1. Закомментировать секцию healthcheck в `docker-compose.yml`

**Преимущества:**
- ✅ Быстрое решение

**Недостатки:**
- ⚠️ Ухудшает мониторинг
- ⚠️ Не решает проблему полностью

**Оценка времени:** 1 минута

**РЕКОМЕНДАЦИЯ:** Использовать **Вариант 1** для полноценного решения.

---

## 🔵 НИЗКИЕ ПРИОРИТЕТЫ

### 13. Расхождение в процентах прогресса

**Проблема:** ✅ Исправлено
- `TEMP_EDIT_MAP`: **Удален** (был ~14.5%)
- `CURRENT_STATUS.md`: **~20%**
- `IMPLEMENTATION_STATUS.md`: **~20%**

**Варианты решения:**

#### Вариант 1: Синхронизировать все документы (РЕКОМЕНДУЕТСЯ)

**Описание:**
Обновить все документы с актуальным прогрессом **~20%**.

**Шаги реализации:** ✅ Выполнено
1. ✅ Определен актуальный прогресс (используется `CURRENT_STATUS.md` как источник истины)
2. ✅ `TEMP_EDIT_MAP` удален (дублировал другую документацию)
3. ✅ Проверены и обновлены другие документы
4. ⏭️ Создать скрипт или процесс для автоматической синхронизации в будущем

**Преимущества:**
- ✅ Устраняет расхождения
- ✅ Улучшает точность документации

**Недостатки:**
- ⚠️ Требует ручного обновления при изменении прогресса

**Оценка времени:** 30 минут

**РЕКОМЕНДАЦИЯ:** Использовать **Вариант 1** и установить процесс регулярного обновления.

---

### 14. Устаревшая информация в документации

**Проблема:**
- В `docs/ANALYSIS_ERRORS.md` указано, что список SQL injection паттернов пустой
- На самом деле паттерны реализованы в `InputValidator.kt`

**Варианты решения:**

#### Вариант 1: Обновить документацию (РЕКОМЕНДУЕТСЯ)

**Описание:**
Обновить `docs/ANALYSIS_ERRORS.md` с актуальной информацией.

**Шаги реализации:**
1. Открыть `docs/ANALYSIS_ERRORS.md`
2. Найти секцию о SQL injection паттернах
3. Обновить информацию:
   ```markdown
   ### SQL Injection защита
   - ✅ Реализовано в `InputValidator.kt` (строки 48-50)
   - ✅ Паттерны валидации: `sanitizeForSql()`, проверка опасных символов
   - ✅ Используется в `CameraRepositoryImpl` для валидации входных данных
   ```

**Преимущества:**
- ✅ Устраняет устаревшую информацию
- ✅ Улучшает точность документации

**Недостатки:**
- ❌ Нет недостатков

**Оценка времени:** 15 минут

**РЕКОМЕНДАЦИЯ:** Использовать **Вариант 1**.

---

## 📋 ПЛАН РЕАЛИЗАЦИИ

### Фаза 1: Критические проблемы (1-2 недели)

1. **День 1-2:** Устранить циклическую зависимость `:core:license` ↔ `:shared`
   - Использовать Вариант 1 (удалить зависимость, вынести типы в `:core:common`)
   - Оценка: 2-4 часа

2. **День 1:** Добавить импорт `InputValidator` в `CameraRepositoryImpl`
   - Использовать Вариант 1 (добавить явный импорт)
   - Оценка: 1 минута

3. **День 2-3:** Исправить Docker конфигурацию
   - Удалить `SYS_ADMIN` и `seccomp:unconfined`
   - Убрать небезопасные пароли по умолчанию
   - Оценка: 2-3 часа

4. **День 3-5:** Реализовать аутентификацию на сервере
   - Использовать Вариант 1 (JWT-based аутентификация)
   - Оценка: 1-2 дня

5. **День 5:** Исправить CORS конфигурацию
   - Использовать Вариант 1 (явное указание доменов)
   - Оценка: 1-2 часа

6. **День 6-8:** Реализовать валидацию SSL/TLS
   - Начать с Варианта 2 (кастомный TrustManager)
   - Затем добавить certificate pinning (Вариант 1)
   - Оценка: 2-3 дня

### Фаза 2: Высокие приоритеты (3-5 дней)

7. **День 1:** Удалить избыточную зависимость в Android app
   - Использовать Вариант 1
   - Оценка: 1 минута

8. **День 1-2:** Исправить хранение токенов
   - Использовать Вариант 1 (httpOnly cookies)
   - Оценка: 1 день

9. **День 2:** Исправить пароли по умолчанию в Docker
   - Использовать Вариант 1 (удалить значения по умолчанию)
   - Оценка: 1-2 часа

### Фаза 3: Средние приоритеты (1-2 дня)

10. **День 1:** Удалить неиспользуемые методы
    - Использовать Вариант 1 или 2 (в зависимости от планов)
    - Оценка: 5 минут - 2 часа

11. **День 1:** Исправить docker-compose.yml
    - Использовать Вариант 1 (использовать build)
    - Оценка: 5 минут

12. **День 1-2:** Реализовать healthcheck endpoint
    - Использовать Вариант 1
    - Оценка: 1-2 часа

### Фаза 4: Низкие приоритеты (1 день)

13. **День 1:** Синхронизировать документацию
    - Обновить прогресс во всех файлах
    - Оценка: 30 минут

14. **День 1:** Обновить устаревшую информацию
    - Обновить `ANALYSIS_ERRORS.md`
    - Оценка: 15 минут

---

## 📊 Сводная таблица решений

| # | Проблема | Рекомендуемое решение | Приоритет | Оценка времени |
|---|----------|----------------------|-----------|----------------|
| 1 | Циклическая зависимость | Вариант 1: Удалить зависимость, вынести типы | 🔴 КРИТИЧЕСКИЙ | 2-4 часа |
| 2 | Отсутствует импорт InputValidator | Вариант 1: Добавить импорт | 🔴 КРИТИЧЕСКИЙ | 1 минута |
| 3 | Небезопасные Docker привилегии | Вариант 1: Удалить опасные привилегии | 🔴 КРИТИЧЕСКИЙ | 1-2 часа |
| 4 | Отсутствие аутентификации | Вариант 1: JWT-based аутентификация | 🔴 КРИТИЧЕСКИЙ | 1-2 дня |
| 5 | Небезопасная CORS | Вариант 1: Явное указание доменов | 🔴 КРИТИЧЕСКИЙ | 1-2 часа |
| 6 | Отсутствие валидации SSL/TLS | Вариант 1: Certificate pinning | 🔴 КРИТИЧЕСКИЙ | 2-3 дня |
| 7 | Избыточная зависимость | Вариант 1: Удалить зависимость | 🟡 ВЫСОКИЙ | 1 минута |
| 8 | Небезопасные пароли | Вариант 1: Удалить значения по умолчанию | 🟡 ВЫСОКИЙ | 1-2 часа |
| 9 | Хранение токенов в localStorage | Вариант 1: httpOnly cookies | 🟡 ВЫСОКИЙ | 1 день |
| 10 | Неиспользуемые методы | Вариант 1: Удалить методы | 🟢 СРЕДНИЙ | 5 минут |
| 11 | Docker образ не существует | Вариант 1: Использовать build | 🟢 СРЕДНИЙ | 5 минут |
| 12 | Healthcheck endpoint | Вариант 1: Реализовать endpoint | 🟢 СРЕДНИЙ | 1-2 часа |
| 13 | Расхождение в прогрессе | Вариант 1: Синхронизировать | 🔵 НИЗКИЙ | 30 минут |
| 14 | Устаревшая информация | Вариант 1: Обновить документацию | 🔵 НИЗКИЙ | 15 минут |

---

## 🎯 Рекомендации по приоритизации

1. **Немедленно (критические):**
   - Проблемы #1, #2, #3, #4, #5, #6
   - Блокируют безопасность и могут вызвать проблемы при сборке

2. **В ближайшее время (высокие):**
   - Проблемы #7, #8, #9
   - Улучшают безопасность и архитектуру

3. **По возможности (средние):**
   - Проблемы #10, #11, #12
   - Улучшают качество кода

4. **Низкий приоритет (документация):**
   - Проблемы #13, #14
   - Можно исправить при обновлении документации

---

**Последнее обновление:** 26 January 2026
**Следующий пересмотр:** После реализации критических исправлений



