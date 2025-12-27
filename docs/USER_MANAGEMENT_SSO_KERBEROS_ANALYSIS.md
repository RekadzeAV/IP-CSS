# Углубленный анализ: Управление пользователями и интеграция с SSO/Kerberos

**Дата анализа:** Декабрь 2025
**Версия проекта:** Alfa-0.0.1
**Статус:** 🔴 КРИТИЧЕСКИЙ - Отсутствует критически важная функциональность

---

## Исполнительное резюме

Проект IP-CSS **не имеет** реализованной системы управления пользователями для работы в корпоративной сетевой инфраструктуре с доменом Active Directory и SSO/Kerberos. Текущая реализация содержит только базовую модель пользователя с ролями и клиентскую аутентификацию через JWT токены, но **полностью отсутствует**:

1. ❌ Серверная аутентификация и авторизация
2. ❌ Интеграция с Active Directory / LDAP
3. ❌ Поддержка SSO (Single Sign-On)
4. ❌ Поддержка Kerberos аутентификации
5. ❌ Синхронизация пользователей с доменом
6. ❌ Распределение прав доступа на основе групп домена

**Критичность:** 🔴 **КРИТИЧЕСКАЯ** - Проект не может быть использован в корпоративной среде без реализации данной функциональности.

---

## 1. Текущее состояние системы управления пользователями

### 1.1. Что реализовано

#### ✅ Модель пользователя (Domain Layer)

**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/model/User.kt`

```kotlin
data class User(
    val id: String,
    val username: String,
    val email: String? = null,
    val fullName: String? = null,
    val role: UserRole,
    val permissions: List<String> = emptyList(),
    val createdAt: Long,
    val lastLoginAt: Long? = null,
    val isActive: Boolean = true
)

enum class UserRole {
    ADMIN,
    OPERATOR,
    VIEWER,
    GUEST
}
```

**Анализ:**
- ✅ Базовая модель пользователя существует
- ✅ Система ролей (4 роли: ADMIN, OPERATOR, VIEWER, GUEST)
- ✅ Система разрешений (список строковых permissions)
- ✅ Методы проверки прав (`isAdmin()`, `hasPermission()`)
- ⚠️ **Ограничение:** Нет связи с доменными группами или SID

#### ✅ Репозиторий пользователей (Domain Layer)

**Файл:** `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/repository/UserRepository.kt`

**Реализованные методы:**
- `login(username, password)` - вход в систему
- `logout()` - выход из системы
- `register(username, email, password, fullName)` - регистрация
- `getCurrentUser()` - получение текущего пользователя
- `updateCurrentUser(user)` - обновление профиля
- `getUsers(page, limit, role)` - список пользователей с пагинацией
- `getUserById(id)` - получение пользователя по ID
- `updateUser(user)` - обновление пользователя
- `deleteUser(id)` - удаление пользователя
- `refreshToken(refreshToken)` - обновление токена

**Анализ:**
- ✅ Интерфейс репозитория определен
- ✅ Реализация через API (`UserRepositoryImpl`)
- ⚠️ **Ограничение:** Работает только с локальной БД, нет синхронизации с доменом

#### ✅ API клиент для пользователей

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/api/UserApiService.kt`

**Endpoints:**
- `POST /api/v1/auth/login` - вход
- `POST /api/v1/auth/register` - регистрация
- `POST /api/v1/auth/logout` - выход
- `POST /api/v1/auth/refresh` - обновление токена
- `GET /api/v1/users/me` - текущий пользователь
- `PUT /api/v1/users/me` - обновление профиля
- `GET /api/v1/users` - список пользователей
- `GET /api/v1/users/{id}` - пользователь по ID
- `PUT /api/v1/users/{id}` - обновление пользователя
- `DELETE /api/v1/users/{id}` - удаление пользователя

**Анализ:**
- ✅ API интерфейс определен
- ⚠️ **Критично:** Серверные endpoints **НЕ РЕАЛИЗОВАНЫ** (см. раздел 1.2)

#### ✅ Клиентская аутентификация (Web UI)

**Файл:** `server/web/src/services/authService.ts`

**Реализовано:**
- Хранение токенов в `localStorage` (⚠️ небезопасно, см. SECURITY_AUDIT_REPORT.md)
- Redux store для управления состоянием аутентификации
- Защищенные маршруты (`ProtectedRoute`)

**Анализ:**
- ✅ Клиентская часть работает
- ⚠️ **Критично:** Токены хранятся в localStorage (уязвимость XSS)
- ⚠️ **Критично:** Нет проверки токенов на сервере

### 1.2. Что отсутствует (критические пробелы)

#### ❌ Серверная аутентификация и авторизация

**Проблема:** Серверная часть (`server/api`) **полностью не имеет** аутентификации.

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/Application.kt`

```kotlin
fun Application.module() {
    // ❌ НЕТ установки Authentication плагина
    // ❌ НЕТ JWT верификации
    // ❌ НЕТ проверки токенов

    install(CORS) {
        anyHost()  // ❌ КРИТИЧНО: Разрешает любые домены
    }

    configureRouting()  // ❌ Все маршруты доступны без аутентификации
}
```

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/routing/CameraRoutes.kt`

```kotlin
// ❌ НЕТ authenticate() блоков
// ❌ НЕТ проверки ролей
// ❌ Все endpoints доступны без авторизации
```

**Последствия:**
- 🔴 Любой может получить доступ к API без аутентификации
- 🔴 Нет контроля доступа к данным
- 🔴 Невозможно отследить действия пользователей
- 🔴 Нет аудита доступа

**Ссылка на уязвимость:** `docs/SECURITY_AUDIT_REPORT.md` - Уязвимость #1

#### ❌ Интеграция с Active Directory / LDAP

**Отсутствует:**
- Нет библиотек для работы с LDAP (например, `unboundid-ldapsdk`)
- Нет конфигурации подключения к домену
- Нет синхронизации пользователей из AD
- Нет маппинга групп домена на роли системы
- Нет проверки членства в группах

**Требуется:**
```kotlin
// Пример требуемой структуры
data class DomainConfig(
    val domainController: String,  // dc.example.com
    val baseDN: String,              // DC=example,DC=com
    val bindDN: String,              // CN=ServiceAccount,OU=ServiceAccounts,DC=example,DC=com
    val bindPassword: String,
    val userSearchBase: String,     // OU=Users,DC=example,DC=com
    val groupSearchBase: String,    // OU=Groups,DC=example,DC=com
    val groupMapping: Map<String, UserRole>  // "Domain Admins" -> ADMIN
)
```

#### ❌ Поддержка SSO (Single Sign-On)

**Отсутствует:**
- Нет поддержки SAML 2.0
- Нет поддержки OAuth 2.0 / OpenID Connect
- Нет поддержки WS-Federation
- Нет интеграции с Identity Provider (IdP)
- Нет механизма единого входа

**Требуется:**
- SAML 2.0 провайдер (например, `spring-security-saml2-core` для JVM)
- OAuth 2.0 / OIDC клиент
- Интеграция с корпоративным IdP (Azure AD, Okta, Keycloak)

#### ❌ Поддержка Kerberos

**Отсутствует:**
- Нет библиотек для Kerberos (например, `org.ietf:gssapi`)
- Нет конфигурации Kerberos realm
- Нет обработки SPNEGO токенов
- Нет интеграции с Key Distribution Center (KDC)
- Нет поддержки делегирования билетов

**Требуется:**
```kotlin
// Пример требуемой структуры
data class KerberosConfig(
    val realm: String,                    // EXAMPLE.COM
    val kdcHost: String,                  // kdc.example.com
    val servicePrincipal: String,        // HTTP/server.example.com@EXAMPLE.COM
    val keytabPath: String,               // /etc/krb5.keytab
    val enableDelegation: Boolean = false
)
```

#### ❌ Синхронизация пользователей с доменом

**Отсутствует:**
- Нет фоновой задачи синхронизации
- Нет механизма периодического обновления пользователей
- Нет обработки изменений в AD (создание, удаление, изменение групп)
- Нет кэширования информации о пользователях
- Нет механизма разрешения конфликтов (локальный vs доменный пользователь)

#### ❌ Распределение прав на основе групп домена

**Отсутствует:**
- Нет маппинга групп AD на роли системы
- Нет проверки членства в группах при авторизации
- Нет динамического обновления прав при изменении групп
- Нет поддержки вложенных групп
- Нет поддержки универсальных групп

---

## 2. Требования для работы в домене с SSO + Kerberos

### 2.1. Архитектура аутентификации

```
┌─────────────────────────────────────────────────────────────┐
│                    Корпоративная сеть                       │
│                                                              │
│  ┌──────────────┐      ┌──────────────┐                    │
│  │ Active       │      │ Key          │                    │
│  │ Directory    │◄────►│ Distribution │                    │
│  │ (Domain      │      │ Center (KDC)  │                    │
│  │ Controller)  │      │              │                    │
│  └──────┬───────┘      └──────┬───────┘                    │
│         │                      │                            │
│         │ LDAP/                │ Kerberos                   │
│         │ Kerberos             │ Protocol                   │
│         │                      │                            │
│         ▼                      ▼                            │
│  ┌──────────────────────────────────────────┐               │
│  │     IP-CSS Server                       │               │
│  │                                          │               │
│  │  ┌────────────────────────────────────┐ │               │
│  │  │ Authentication Layer               │ │               │
│  │  │  - LDAP Client                     │ │               │
│  │  │  - Kerberos Client                 │ │               │
│  │  │  - SSO Provider (SAML/OIDC)        │ │               │
│  │  └────────────────────────────────────┘ │               │
│  │                                          │               │
│  │  ┌────────────────────────────────────┐ │               │
│  │  │ Authorization Layer                │ │               │
│  │  │  - Role Mapping                    │ │               │
│  │  │  - Permission Engine               │ │               │
│  │  │  - Group Membership Check          │ │               │
│  │  └────────────────────────────────────┘ │               │
│  │                                          │               │
│  │  ┌────────────────────────────────────┐ │               │
│  │  │ User Sync Service                   │ │               │
│  │  │  - Periodic Sync                    │ │               │
│  │  │  - Event-driven Updates             │ │               │
│  │  │  - Conflict Resolution              │ │               │
│  │  └────────────────────────────────────┘ │               │
│  └──────────────────────────────────────────┘               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 2.2. Сценарии использования

#### Сценарий 1: Вход через Kerberos (Windows)

1. Пользователь открывает веб-интерфейс IP-CSS
2. Браузер автоматически получает Kerberos билет от KDC
3. Браузер отправляет SPNEGO токен на сервер
4. Сервер проверяет токен через KDC
5. Сервер получает информацию о пользователе из AD
6. Сервер определяет роли на основе групп AD
7. Сервер создает сессию и возвращает JWT токен

#### Сценарий 2: Вход через SSO (SAML/OIDC)

1. Пользователь открывает веб-интерфейс IP-CSS
2. Сервер перенаправляет на Identity Provider (IdP)
3. Пользователь аутентифицируется в IdP
4. IdP возвращает SAML assertion или OIDC token
5. Сервер проверяет assertion/token
6. Сервер получает информацию о пользователе из токена
7. Сервер синхронизирует пользователя с AD (опционально)
8. Сервер определяет роли на основе групп AD
9. Сервер создает сессию и возвращает JWT токен

#### Сценарий 3: Синхронизация пользователей

1. Фоновая задача запускается по расписанию (например, каждые 5 минут)
2. Задача подключается к AD через LDAP
3. Задача получает список пользователей из указанных OU
4. Задача получает информацию о группах для каждого пользователя
5. Задача обновляет локальную БД:
   - Создает новых пользователей
   - Обновляет существующих пользователей
   - Деактивирует удаленных пользователей
   - Обновляет роли на основе групп

#### Сценарий 4: Авторизация на основе групп

1. Пользователь делает запрос к API
2. Middleware проверяет JWT токен
3. Middleware получает информацию о пользователе из токена
4. Middleware проверяет актуальность групп в AD (кэш или прямой запрос)
5. Middleware определяет роль пользователя на основе групп
6. Middleware проверяет разрешения для запрошенного ресурса
7. Middleware разрешает или запрещает доступ

---

## 3. Детальный анализ компонентов

### 3.1. Модель пользователя (расширенная)

**Текущая модель:**
```kotlin
data class User(
    val id: String,
    val username: String,
    val email: String? = null,
    val fullName: String? = null,
    val role: UserRole,
    val permissions: List<String> = emptyList(),
    val createdAt: Long,
    val lastLoginAt: Long? = null,
    val isActive: Boolean = true
)
```

**Требуемая модель для домена:**
```kotlin
data class User(
    val id: String,
    val username: String,                    // sAMAccountName
    val email: String? = null,
    val fullName: String? = null,
    val displayName: String? = null,
    val role: UserRole,
    val permissions: List<String> = emptyList(),

    // Доменная информация
    val domainSID: String? = null,            // Security Identifier из AD
    val domainDN: String? = null,             // Distinguished Name
    val domainGroups: List<String> = emptyList(), // Группы в AD
    val domainGroupsDN: List<String> = emptyList(), // DN групп

    // Источник пользователя
    val source: UserSource,                   // LOCAL, DOMAIN, SSO

    // Синхронизация
    val lastSyncAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,

    // Метаданные
    val createdAt: Long,
    val lastLoginAt: Long? = null,
    val isActive: Boolean = true
)

enum class UserSource {
    LOCAL,      // Локальный пользователь (создан в системе)
    DOMAIN,     // Пользователь из Active Directory
    SSO,        // Пользователь через SSO (SAML/OIDC)
    HYBRID      // Комбинация (например, локальный с привязкой к домену)
}

enum class SyncStatus {
    PENDING,    // Ожидает синхронизации
    SYNCED,     // Синхронизирован
    FAILED,     // Ошибка синхронизации
    OUTDATED    // Устаревшие данные
}
```

### 3.2. Конфигурация домена

**Требуется создать:**

```kotlin
// core/common/src/commonMain/kotlin/com/company/ipcamera/core/common/config/DomainConfig.kt

data class DomainConfig(
    // LDAP настройки
    val ldap: LdapConfig,

    // Kerberos настройки
    val kerberos: KerberosConfig? = null,

    // SSO настройки
    val sso: SsoConfig? = null,

    // Маппинг групп на роли
    val groupMapping: Map<String, UserRole>,

    // Настройки синхронизации
    val sync: SyncConfig
)

data class LdapConfig(
    val enabled: Boolean = true,
    val host: String,                        // dc.example.com
    val port: Int = 389,                      // 636 для LDAPS
    val useSSL: Boolean = true,
    val baseDN: String,                      // DC=example,DC=com
    val bindDN: String,                      // CN=ServiceAccount,OU=ServiceAccounts,DC=example,DC=com
    val bindPassword: String,
    val userSearchBase: String,              // OU=Users,DC=example,DC=com
    val userSearchFilter: String = "(objectClass=user)",
    val groupSearchBase: String,             // OU=Groups,DC=example,DC=com
    val groupSearchFilter: String = "(objectClass=group)",
    val connectionTimeout: Long = 5000,
    val readTimeout: Long = 5000
)

data class KerberosConfig(
    val enabled: Boolean = true,
    val realm: String,                       // EXAMPLE.COM
    val kdcHost: String,                     // kdc.example.com
    val kdcPort: Int = 88,
    val servicePrincipal: String,            // HTTP/server.example.com@EXAMPLE.COM
    val keytabPath: String,                  // /etc/krb5.keytab
    val enableDelegation: Boolean = false,
    val ticketCachePath: String? = null
)

data class SsoConfig(
    val enabled: Boolean = false,
    val provider: SsoProvider,              // SAML, OIDC, WS_FED
    val saml: SamlConfig? = null,
    val oidc: OidcConfig? = null
)

enum class SsoProvider {
    SAML,
    OIDC,
    WS_FED
}

data class SamlConfig(
    val idpMetadataUrl: String,
    val spEntityId: String,
    val spAssertionConsumerServiceUrl: String,
    val spSingleLogoutServiceUrl: String? = null,
    val certificatePath: String,
    val privateKeyPath: String,
    val nameIdFormat: String = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified"
)

data class OidcConfig(
    val issuer: String,                     // https://login.microsoftonline.com/{tenant-id}/v2.0
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val scopes: List<String> = listOf("openid", "profile", "email")
)

data class SyncConfig(
    val enabled: Boolean = true,
    val interval: Long = 300000,             // 5 минут
    val fullSyncInterval: Long = 86400000,   // 24 часа
    val syncOnStartup: Boolean = true,
    val syncOnLogin: Boolean = true,
    val createUsers: Boolean = true,        // Создавать новых пользователей
    val updateUsers: Boolean = true,         // Обновлять существующих
    val deactivateUsers: Boolean = true,    // Деактивировать удаленных
    val syncGroups: Boolean = true          // Синхронизировать группы
)
```

### 3.3. LDAP клиент

**Требуется создать:**

```kotlin
// core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/ldap/LdapClient.kt

interface LdapClient {
    /**
     * Подключиться к LDAP серверу
     */
    suspend fun connect(): Result<Unit>

    /**
     * Отключиться от LDAP сервера
     */
    suspend fun disconnect()

    /**
     * Аутентифицировать пользователя
     */
    suspend fun authenticate(username: String, password: String): Result<LdapUser>

    /**
     * Найти пользователя по имени
     */
    suspend fun findUser(username: String): LdapUser?

    /**
     * Найти пользователя по DN
     */
    suspend fun findUserByDN(dn: String): LdapUser?

    /**
     * Получить группы пользователя
     */
    suspend fun getUserGroups(userDN: String): List<LdapGroup>

    /**
     * Получить всех пользователей из указанной OU
     */
    suspend fun getAllUsers(ou: String? = null): List<LdapUser>

    /**
     * Получить все группы
     */
    suspend fun getAllGroups(ou: String? = null): List<LdapGroup>

    /**
     * Проверить членство пользователя в группе
     */
    suspend fun isMemberOfGroup(userDN: String, groupDN: String): Boolean
}

data class LdapUser(
    val dn: String,
    val sAMAccountName: String,
    val userPrincipalName: String?,
    val email: String?,
    val displayName: String?,
    val givenName: String?,
    val sn: String?,
    val memberOf: List<String>,              // DN групп
    val sID: String?,
    val enabled: Boolean,
    val lastLogon: Long?
)

data class LdapGroup(
    val dn: String,
    val name: String,
    val sAMAccountName: String,
    val description: String?,
    val member: List<String>,               // DN членов
    val sID: String?
)
```

**Зависимости:**
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.unboundid:unboundid-ldapsdk:6.0.8")
}
```

### 3.4. Kerberos клиент

**Требуется создать:**

```kotlin
// core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/kerberos/KerberosClient.kt

interface KerberosClient {
    /**
     * Инициализировать Kerberos контекст
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Аутентифицировать пользователя по SPNEGO токену
     */
    suspend fun authenticateSpnegoToken(token: ByteArray): Result<KerberosPrincipal>

    /**
     * Получить информацию о пользователе из Kerberos principal
     */
    suspend fun getUserInfo(principal: KerberosPrincipal): Result<LdapUser>

    /**
     * Проверить валидность билета
     */
    suspend fun validateTicket(ticket: ByteArray): Boolean
}

data class KerberosPrincipal(
    val name: String,
    val realm: String,
    val sAMAccountName: String
)
```

**Зависимости:**
```kotlin
// build.gradle.kts
dependencies {
    // Kerberos встроен в JVM, но может потребоваться:
    implementation("org.ietf:gssapi:1.0.0")  // Если нужна дополнительная функциональность
}
```

### 3.5. SSO провайдер

**Требуется создать:**

```kotlin
// core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/sso/SsoProvider.kt

interface SsoProvider {
    /**
     * Получить URL для редиректа на IdP
     */
    suspend fun getLoginUrl(returnUrl: String): String

    /**
     * Обработать ответ от IdP
     */
    suspend fun processResponse(response: SsoResponse): Result<SsoUser>

    /**
     * Выйти из SSO сессии
     */
    suspend fun logout(returnUrl: String): String
}

data class SsoResponse(
    val samlAssertion: String? = null,
    val oidcToken: String? = null,
    val relayState: String? = null
)

data class SsoUser(
    val nameId: String,
    val email: String?,
    val displayName: String?,
    val groups: List<String>,
    val attributes: Map<String, String>
)
```

**Зависимости для SAML:**
```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.security:spring-security-saml2-service-provider:6.2.0")
    // Или альтернатива:
    implementation("com.onelogin:java-saml:3.0.0")
}
```

**Зависимости для OIDC:**
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.nimbusds:oauth2-oidc-sdk:10.0.2")
}
```

### 3.6. Сервис синхронизации пользователей

**Требуется создать:**

```kotlin
// shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/service/UserSyncService.kt

interface UserSyncService {
    /**
     * Запустить синхронизацию пользователей
     */
    suspend fun syncUsers(): Result<SyncResult>

    /**
     * Синхронизировать конкретного пользователя
     */
    suspend fun syncUser(username: String): Result<User>

    /**
     * Получить статус последней синхронизации
     */
    suspend fun getSyncStatus(): SyncStatus
}

data class SyncResult(
    val created: Int,
    val updated: Int,
    val deactivated: Int,
    val errors: List<SyncError>,
    val duration: Long
)

data class SyncError(
    val username: String,
    val error: String,
    val timestamp: Long
)

data class SyncStatus(
    val lastSyncAt: Long?,
    val nextSyncAt: Long,
    val isRunning: Boolean,
    val lastResult: SyncResult?
)
```

### 3.7. Middleware для аутентификации и авторизации

**Требуется создать:**

```kotlin
// server/api/src/main/kotlin/com/company/ipcamera/server/auth/AuthMiddleware.kt

fun Application.configureAuthentication() {
    install(Authentication) {
        // JWT аутентификация
        jwt("jwt-auth") {
            realm = "ip-camera-system"
            verifier = jwtVerifier
            validate { credential ->
                val principal = credential.payload
                val userId = principal.getClaim("userId")?.asString()
                val username = principal.getClaim("username")?.asString()
                val roles = principal.getClaim("roles")?.asList(String::class.java) ?: emptyList()

                if (userId != null && username != null) {
                    JWTPrincipal(principal)
                } else {
                    null
                }
            }
        }

        // Kerberos аутентификация
        kerberos("kerberos-auth") {
            realm = kerberosConfig.realm
            // Конфигурация SPNEGO
        }

        // SSO аутентификация
        sso("sso-auth") {
            provider = ssoProvider
        }
    }
}

fun Route.requireAuth(block: Route.() -> Unit) {
    authenticate("jwt-auth", "kerberos-auth", "sso-auth") {
        block()
    }
}

fun Route.requireRole(role: UserRole, block: Route.() -> Unit) {
    requireAuth {
        authorize { principal ->
            val userRoles = principal.getClaim("roles")?.asList(String::class.java) ?: emptyList()
            userRoles.contains(role.name)
        }
        block()
    }
}

fun Route.requirePermission(permission: String, block: Route.() -> Unit) {
    requireAuth {
        authorize { principal ->
            val permissions = principal.getClaim("permissions")?.asList(String::class.java) ?: emptyList()
            permissions.contains(permission)
        }
        block()
    }
}
```

---

## 4. План реализации

### 4.1. Этап 1: Базовая серверная аутентификация (Критично)

**Приоритет:** 🔴 **КРИТИЧЕСКИЙ**
**Срок:** 1-2 недели

**Задачи:**
1. ✅ Установить `ktor-server-auth` и `ktor-server-auth-jwt`
2. ✅ Настроить JWT верификацию
3. ✅ Добавить middleware для проверки токенов
4. ✅ Защитить все API endpoints
5. ✅ Реализовать endpoints для аутентификации (`/api/v1/auth/login`, `/api/v1/auth/logout`, `/api/v1/auth/refresh`)
6. ✅ Добавить проверку ролей и разрешений
7. ✅ Исправить CORS конфигурацию (удалить `anyHost()`)

**Файлы для изменения:**
- `server/api/build.gradle.kts` - добавить зависимости
- `server/api/src/main/kotlin/com/company/ipcamera/server/Application.kt` - добавить Authentication
- `server/api/src/main/kotlin/com/company/ipcamera/server/routing/*.kt` - защитить маршруты
- `server/api/src/main/kotlin/com/company/ipcamera/server/auth/*.kt` - создать новые файлы

### 4.2. Этап 2: Интеграция с LDAP (Высокий приоритет)

**Приоритет:** 🟠 **ВЫСОКИЙ**
**Срок:** 2-3 недели

**Задачи:**
1. ✅ Добавить зависимость `unboundid-ldapsdk`
2. ✅ Создать `LdapClient` интерфейс и реализацию
3. ✅ Создать конфигурацию `DomainConfig`
4. ✅ Реализовать методы поиска пользователей и групп
5. ✅ Реализовать аутентификацию через LDAP
6. ✅ Интегрировать LDAP аутентификацию в API endpoints
7. ✅ Добавить тесты

**Файлы для создания:**
- `core/common/src/commonMain/kotlin/com/company/ipcamera/core/common/config/DomainConfig.kt`
- `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/ldap/LdapClient.kt`
- `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/ldap/LdapClientImpl.kt`

### 4.3. Этап 3: Синхронизация пользователей (Высокий приоритет)

**Приоритет:** 🟠 **ВЫСОКИЙ**
**Срок:** 2-3 недели

**Задачи:**
1. ✅ Расширить модель `User` для поддержки домена
2. ✅ Создать `UserSyncService`
3. ✅ Реализовать фоновую задачу синхронизации
4. ✅ Реализовать маппинг групп AD на роли системы
5. ✅ Добавить обработку конфликтов (локальный vs доменный пользователь)
6. ✅ Добавить API endpoints для управления синхронизацией
7. ✅ Добавить логирование и мониторинг

**Файлы для создания:**
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/service/UserSyncService.kt`
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/service/UserSyncServiceImpl.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/job/UserSyncJob.kt`

### 4.4. Этап 4: Поддержка Kerberos (Средний приоритет)

**Приоритет:** 🟡 **СРЕДНИЙ**
**Срок:** 3-4 недели

**Задачи:**
1. ✅ Создать `KerberosClient` интерфейс и реализацию
2. ✅ Настроить Kerberos конфигурацию
3. ✅ Реализовать обработку SPNEGO токенов
4. ✅ Интегрировать Kerberos в Ktor Authentication
5. ✅ Добавить поддержку делегирования билетов (опционально)
6. ✅ Добавить тесты и документацию

**Файлы для создания:**
- `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/kerberos/KerberosClient.kt`
- `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/kerberos/KerberosClientImpl.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/auth/KerberosAuth.kt`

### 4.5. Этап 5: Поддержка SSO (Средний приоритет)

**Приоритет:** 🟡 **СРЕДНИЙ**
**Срок:** 4-5 недель

**Задачи:**
1. ✅ Выбрать SSO провайдер (SAML, OIDC, или оба)
2. ✅ Добавить зависимости для выбранного провайдера
3. ✅ Создать `SsoProvider` интерфейс и реализацию
4. ✅ Настроить конфигурацию SSO
5. ✅ Реализовать endpoints для SSO (login, callback, logout)
6. ✅ Интегрировать SSO в Ktor Authentication
7. ✅ Добавить тесты и документацию

**Файлы для создания:**
- `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/sso/SsoProvider.kt`
- `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/sso/SamlProvider.kt`
- `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/sso/OidcProvider.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/routing/SsoRoutes.kt`

### 4.6. Этап 6: Авторизация на основе групп (Средний приоритет)

**Приоритет:** 🟡 **СРЕДНИЙ**
**Срок:** 2-3 недели

**Задачи:**
1. ✅ Реализовать проверку членства в группах при авторизации
2. ✅ Реализовать кэширование информации о группах
3. ✅ Реализовать динамическое обновление прав при изменении групп
4. ✅ Добавить поддержку вложенных групп
5. ✅ Добавить поддержку универсальных групп
6. ✅ Оптимизировать производительность

**Файлы для изменения:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/auth/AuthMiddleware.kt`
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/service/GroupMembershipService.kt`

---

## 5. Рекомендации по безопасности

### 5.1. Хранение учетных данных

**Проблема:** Конфигурация домена содержит чувствительные данные (пароли, ключи).

**Рекомендации:**
1. ✅ Использовать переменные окружения для паролей
2. ✅ Использовать Docker secrets или внешний vault (HashiCorp Vault, AWS Secrets Manager)
3. ✅ Не хранить пароли в коде или конфигурационных файлах
4. ✅ Использовать service account с минимальными правами для подключения к AD

### 5.2. Шифрование соединений

**Рекомендации:**
1. ✅ Использовать LDAPS (LDAP over SSL/TLS) вместо LDAP
2. ✅ Использовать валидные сертификаты для LDAPS
3. ✅ Настроить certificate pinning для защиты от MITM
4. ✅ Использовать TLS 1.2+ для всех соединений

### 5.3. Управление сессиями

**Рекомендации:**
1. ✅ Использовать короткоживущие JWT токены (15-30 минут)
2. ✅ Реализовать refresh token rotation
3. ✅ Хранить refresh tokens в httpOnly cookies
4. ✅ Реализовать механизм отзыва токенов
5. ✅ Логировать все попытки входа (успешные и неуспешные)

### 5.4. Аудит доступа

**Рекомендации:**
1. ✅ Логировать все операции с пользователями
2. ✅ Логировать все изменения прав доступа
3. ✅ Логировать все попытки доступа к защищенным ресурсам
4. ✅ Хранить логи в централизованной системе (ELK, Splunk)
5. ✅ Реализовать алерты на подозрительную активность

---

## 6. Тестирование

### 6.1. Unit тесты

**Требуется покрыть:**
- ✅ `LdapClient` - все методы
- ✅ `KerberosClient` - все методы
- ✅ `SsoProvider` - все методы
- ✅ `UserSyncService` - логика синхронизации
- ✅ Маппинг групп на роли
- ✅ Проверка разрешений

### 6.2. Integration тесты

**Требуется покрыть:**
- ✅ Аутентификация через LDAP
- ✅ Аутентификация через Kerberos
- ✅ Аутентификация через SSO
- ✅ Синхронизация пользователей
- ✅ Авторизация на основе групп

**Инфраструктура:**
- Использовать тестовый LDAP сервер (например, Apache Directory Server)
- Использовать тестовый KDC (например, MIT Kerberos)
- Использовать тестовый IdP (например, Keycloak)

### 6.3. E2E тесты

**Требуется покрыть:**
- ✅ Полный цикл входа через Kerberos
- ✅ Полный цикл входа через SSO
- ✅ Синхронизация пользователей из AD
- ✅ Изменение прав при изменении групп в AD

---

## 7. Документация

### 7.1. Техническая документация

**Требуется создать:**
1. ✅ Архитектура аутентификации и авторизации
2. ✅ Руководство по настройке LDAP
3. ✅ Руководство по настройке Kerberos
4. ✅ Руководство по настройке SSO
5. ✅ Руководство по синхронизации пользователей
6. ✅ API документация для endpoints аутентификации

### 7.2. Операционная документация

**Требуется создать:**
1. ✅ Руководство по развертыванию в домене
2. ✅ Руководство по устранению неполадок
3. ✅ Руководство по мониторингу
4. ✅ Руководство по резервному копированию конфигурации

---

## 8. Заключение

### 8.1. Критические пробелы

Проект IP-CSS **не готов** для использования в корпоративной сетевой инфраструктуре с доменом Active Directory и SSO/Kerberos. Отсутствуют критически важные компоненты:

1. 🔴 **Серверная аутентификация** - полностью отсутствует
2. 🔴 **Интеграция с LDAP/AD** - отсутствует
3. 🔴 **Поддержка Kerberos** - отсутствует
4. 🔴 **Поддержка SSO** - отсутствует
5. 🔴 **Синхронизация пользователей** - отсутствует
6. 🔴 **Авторизация на основе групп** - отсутствует

### 8.2. Оценка трудозатрат

**Минимальная реализация (базовая функциональность):**
- Этап 1 (Серверная аутентификация): 1-2 недели
- Этап 2 (LDAP интеграция): 2-3 недели
- Этап 3 (Синхронизация пользователей): 2-3 недели
- **Итого:** 5-8 недель (1.5-2 месяца)

**Полная реализация (включая Kerberos и SSO):**
- Этап 1-3: 5-8 недель
- Этап 4 (Kerberos): 3-4 недели
- Этап 5 (SSO): 4-5 недели
- Этап 6 (Авторизация на группах): 2-3 недели
- **Итого:** 14-20 недель (3.5-5 месяцев)

### 8.3. Приоритеты

**Немедленно (блокеры для продакшена):**
1. ✅ Реализовать серверную аутентификацию (Этап 1)
2. ✅ Исправить критические уязвимости безопасности

**Краткосрочно (1-2 месяца):**
3. ✅ Интегрировать LDAP (Этап 2)
4. ✅ Реализовать синхронизацию пользователей (Этап 3)

**Среднесрочно (3-5 месяцев):**
5. ✅ Добавить поддержку Kerberos (Этап 4)
6. ✅ Добавить поддержку SSO (Этап 5)
7. ✅ Реализовать авторизацию на группах (Этап 6)

### 8.4. Рекомендации

1. **Начать с Этапа 1** - без серверной аутентификации проект не может быть использован в продакшене
2. **Использовать существующие библиотеки** - не изобретать велосипед, использовать проверенные решения (unboundid-ldapsdk, spring-security-saml2)
3. **Постепенная интеграция** - сначала LDAP, потом Kerberos, потом SSO
4. **Тестирование на тестовом окружении** - обязательно иметь тестовый AD домен для разработки и тестирования
5. **Документирование** - документировать каждый этап для упрощения поддержки

---

**Составитель:** AI Security & Architecture Analyst
**Дата:** Декабрь 2025
**Версия отчета:** Alfa-0.0.1



