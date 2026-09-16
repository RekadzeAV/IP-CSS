# LDAP/AD Integration Guide

**Дата:** 2026-01-28  
**Статус:** ✅ Завершено  
**Версия:** 1.0

---

## 📋 Обзор

LDAP/AD интеграция позволяет использовать корпоративные учётные записи для аутентификации в IP-CSS.

### Реализованные компоненты:

1. ✅ **LDAP Authentication** — аутентификация через bind
2. ✅ **Active Directory** — поддержка AD через LDAP
3. ✅ **Group-based Roles** — маппинг групп в роли
4. ✅ **User Sync** — синхронизация пользователей в локальную БД
5. ✅ **Health Check** — проверка доступности LDAP сервера

---

## 🏗️ Архитектура

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│   IP-CSS UI     │─────▶│  LdapAuthService │─────▶│  LDAP/AD Server │
│   (Login)       │      │                  │      │  (192.168.10.37)│
└─────────────────┘      └──────────────────┘      └─────────────────┘
                                │
                                ▼
                         ┌──────────────────┐
                         │LdapUserDetailsService│
                         │                  │
                         └──────────────────┘
                                │
                                ▼
                         ┌──────────────────┐
                         │  UserRepository  │
                         │  (Local PostgreSQL)│
                         └──────────────────┘
```

---

## ⚙️ Конфигурация

### 1. Базовые настройки

```bash
# .env.production

# Включить LDAP аутентификацию
LDAP_ENABLED=true

# LDAP сервер
AUTH_LDAP_SERVER=ldap://192.168.10.37:389

# Base DN для поиска
AUTH_LDAP_BASE_DN=dc=surveillance,dc=local

# Учётная запись для bind (сервисный аккаунт)
AUTH_LDAP_BIND_DN=cn=admin,dc=surveillance,dc=local
AUTH_LDAP_BIND_PASSWORD=secure_password

# Поиск пользователей
AUTH_LDAP_USER_SEARCH_BASE=ou=users,dc=surveillance,dc=local
AUTH_LDAP_USER_SEARCH_FILTER=(uid={0})

# Поиск групп
AUTH_LDAP_GROUP_SEARCH_BASE=ou=groups,dc=surveillance,dc=local
AUTH_LDAP_GROUP_SEARCH_FILTER=(member={0})

# Таймауты
AUTH_LDAP_CONNECTION_TIMEOUT=5000
AUTH_LDAP_READ_TIMEOUT=5000
```

### 2. Active Directory конфигурация

```bash
# .env.production (Active Directory)

# AD сервер
AUTH_LDAP_SERVER=ldap://ad.company.local:389
# Или LDAPS (безопасное соединение)
# AUTH_LDAP_SERVER=ldaps://ad.company.local:636

# Base DN
AUTH_LDAP_BASE_DN=dc=company,dc=local

# Service account
AUTH_LDAP_BIND_DN=cn=ipcss-service,ou=ServiceAccounts,dc=company,dc=local
AUTH_LDAP_BIND_PASSWORD=service_password

# Поиск пользователей (AD использует sAMAccountName)
AUTH_LDAP_USER_SEARCH_BASE=ou=Users,dc=company,dc=local
AUTH_LDAP_USER_SEARCH_FILTER=(sAMAccountName={0})

# Поиск групп (AD использует memberOf)
AUTH_LDAP_GROUP_SEARCH_BASE=cn=Users,dc=company,dc=local
AUTH_LDAP_GROUP_SEARCH_FILTER=(member={0})

# Атрибуты
AUTH_LDAP_USER_NAME_ATTRIBUTE=sAMAccountName
AUTH_LDAP_USER_FULL_NAME_ATTRIBUTE=displayName
AUTH_LDAP_USER_EMAIL_ATTRIBUTE=mail
```

### 3. Маппинг ролей

```bash
# .env.production

# Маппинг LDAP групп в роли IP-CSS
AUTH_LDAP_ROLE_MAPPING_ADMIN=CN=IP-CSS Admins,cn=users,dc=company,dc=local
AUTH_LDAP_ROLE_MAPPING_OPERATOR=CN=IP-CSS Operators,cn=users,dc=company,dc=local
AUTH_LDAP_ROLE_MAPPING_VIEWER=CN=IP-CSS Viewers,cn=users,dc=company,dc=local
```

---

## 🔧 Компоненты

### 1. LdapConfig

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/config/LdapConfig.kt`

**Функции:**
- `testConnection()` — проверка подключения
- `connect()` — создание соединения
- `findUserByUsername(username)` — поиск пользователя
- `getUserGroups(username)` — получение групп
- `validateCredentials(username, password)` — проверка пароля

**Пример использования:**
```kotlin
// Проверка подключения
val isConnected = LdapConfig.testConnection()

// Поиск пользователя
val user = LdapConfig.findUserByUsername("admin")
println("User: ${user?.getAttributeValue("cn")}")

// Проверка пароля
val isValid = LdapConfig.validateCredentials("admin", "password")
```

---

### 2. LdapAuthService

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/security/LdapAuthService.kt`

**Функции:**
- `authenticate(username, password)` — аутентификация пользователя
- `checkAvailability()` — проверка доступности LDAP
- `searchUserDn(connection, username)` — поиск DN пользователя
- `resolveRole(entry)` — определение роли по группам

**Пример использования:**
```kotlin
val ldapAuthService = LdapAuthService(config, createOrGetLocalUser)

// Аутентификация
val user = ldapAuthService.authenticate("admin", "password")
if (user != null) {
    println("Authenticated: ${user.username}")
    println("Role: ${user.role}")
}
```

---

### 3. LdapUserDetailsService

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/security/LdapUserDetailsService.kt`

**Функции:**
- `loadUserByUsername(username)` — загрузка пользователя из LDAP
- `mapRole(groups)` — маппинг групп в роль
- `mapPermissions(groups)` — маппинг групп в права
- `mapRolesToSpringRoles(groups)` — маппинг в Spring Security роли

**Пример использования:**
```kotlin
val userDetails = ldapUserDetailsService.loadUserByUsername("admin")
println("Username: ${userDetails.username}")
println("Authorities: ${userDetails.authorities}")
```

---

## 📊 Маппинг ролей

### LDAP Groups → IP-CSS Roles

| LDAP Group | IP-CSS Role | Permissions |
|------------|-------------|-------------|
| `admin` / `IP-CSS Admins` | `ADMIN` | `*` (полный доступ) |
| `operator` / `IP-CSS Operators` | `OPERATOR` | `read:cameras`, `write:cameras`, `read:recordings`, `write:recordings`, `read:events`, `write:events` |
| `viewer` / `IP-CSS Viewers` | `VIEWER` | `read:cameras`, `read:recordings`, `read:events` |
| Другие | `VIEWER` | `read:cameras`, `read:recordings`, `read:events` |

---

## 🚀 Быстрый старт

### 1. Настройка LDAP сервера

```bash
# Проверка доступности
ldapsearch -x -H ldap://192.168.10.37:389 -b "dc=surveillance,dc=local" -s sub "(uid=admin)"

# Тест аутентификации
ldapsearch -x -H ldap://192.168.10.37:389 -D "cn=admin,dc=surveillance,dc=local" -w password -b "dc=surveillance,dc=local" "(uid=testuser)"
```

### 2. Настройка IP-CSS

```bash
# .env.production
LDAP_ENABLED=true
AUTH_LDAP_SERVER=ldap://192.168.10.37:389
AUTH_LDAP_BASE_DN=dc=surveillance,dc=local
AUTH_LDAP_BIND_DN=cn=admin,dc=surveillance,dc=local
AUTH_LDAP_BIND_PASSWORD=secure_password
```

### 3. Запуск сервера

```bash
cd server/api
./gradlew bootRun
```

### 4. Проверка логов

```
INFO  LdapConfig - Testing LDAP connection to ldap://192.168.10.37:389
INFO  LdapConfig - LDAP connection test successful
INFO  LdapUserDetailsService - Loading user from LDAP: admin
INFO  LdapUserDetailsService - User saved to local database: admin
```

---

## 🧪 Тестирование

### 1. Unit тесты

```bash
# Запуск тестов
./gradlew :server:api:test --tests "*LdapAuthServiceTest*"
```

### 2. Интеграционные тесты

```bash
# Тест подключения
curl -X POST http://localhost:8080/api/v1/auth/ldap/test \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

### 3. Ручное тестирование

```bash
# LDAP search
ldapsearch -x -H ldap://192.168.10.37:389 \
  -D "cn=admin,dc=surveillance,dc=local" \
  -w password \
  -b "ou=users,dc=surveillance,dc=local" \
  "(uid=admin)"

# LDAP bind test
ldapwhoami -x -H ldap://192.168.10.37:389 \
  -D "cn=admin,dc=surveillance,dc=local" \
  -w password
```

---

## 🔍 Troubleshooting

### Проблема: "LDAP connection refused"

**Решение:**
1. Проверить доступность сервера: `telnet 192.168.10.37 389`
2. Проверить firewall: `ufw status`
3. Проверить LDAP сервис: `systemctl status slapd`

### Проблема: "Invalid credentials"

**Решение:**
1. Проверить bind DN и пароль
2. Проверить права сервисного аккаунта
3. Проверить атрибуты пользователя

### Проблема: "User not found"

**Решение:**
1. Проверить `AUTH_LDAP_USER_SEARCH_BASE`
2. Проверить `AUTH_LDAP_USER_SEARCH_FILTER`
3. Проверить существование пользователя: `ldapsearch ... "(uid=username)"`

### Проблема: "Неверная роль"

**Решение:**
1. Проверить членство в группах: `ldapsearch ... "(member=cn=user,...)"`
2. Проверить `AUTH_LDAP_ROLE_MAPPING_*`
3. Проверить названия групп (case-sensitive)

---

## 📚 Active Directory vs OpenLDAP

### Active Directory отличия

| Аспект | Active Directory | OpenLDAP |
|--------|-----------------|----------|
| **Атрибут username** | `sAMAccountName` | `uid` |
| **Атрибут full name** | `displayName` | `cn` |
| **Атрибут email** | `mail` | `mail` |
| **Поиск групп** | `memberOf` (обратный) | `member` (прямой) |
| **Base DN** | `dc=company,dc=local` | `dc=company,dc=local` |
| **Порт LDAP** | 389 | 389 |
| **Порт LDAPS** | 636 | 636 |

### AD пример конфигурации

```bash
AUTH_LDAP_SERVER=ldaps://ad.company.local:636
AUTH_LDAP_BASE_DN=dc=company,dc=local
AUTH_LDAP_BIND_DN=cn=ipcss-service,ou=ServiceAccounts,dc=company,dc=local
AUTH_LDAP_USER_SEARCH_BASE=ou=Users,dc=company,dc=local
AUTH_LDAP_USER_SEARCH_FILTER=(sAMAccountName={0})
AUTH_LDAP_GROUP_SEARCH_FILTER=(member={0})
```

---

## 🔐 Безопасность

### 1. LDAPS (LDAP over SSL)

```bash
# Использовать безопасное соединение
AUTH_LDAP_SERVER=ldaps://ldap.company.local:636

# Импортировать сертификат в JVM truststore
keytool -importcert -file ldap.crt -keystore $JAVA_HOME/lib/security/cacerts -alias ldap
```

### 2. StartTLS

```bash
# StartTLS (upgrade connection)
AUTH_LDAP_SERVER=ldap://ldap.company.local:389
AUTH_LDAP_USE_STARTTLS=true
```

### 3. Service Account

```bash
# Создать сервисный аккаунт с минимальными правами
# Только read доступ к users и groups
dn: cn=ipcss-service,ou=ServiceAccounts,dc=company,dc=local
objectClass: simpleSecurityObject
objectClass: organizationalRole
cn: ipcss-service
userPassword: {SSHA}hashed_password
description: IP-CSS service account for LDAP authentication
```

---

## 📊 Мониторинг

### Метрики

| Метрика | Описание | Источник |
|---------|----------|----------|
| `ldap.connections.active` | Активные соединения | HikariCP |
| `ldap.authenticate.success` | Успешные аутентификации | Audit Log |
| `ldap.authenticate.failure` | Неудачные аутентификации | Audit Log |
| `ldap.search.duration` | Время поиска | LdapConfig |
| `ldap.health.status` | Статус подключения | Health Check |

### Логи

```bash
# Включить debug логи
LOG_LEVEL_COM_COMPANY_IPCAMERA_SERVER_SECURITY=DEBUG
LOG_LEVEL_COM_UNBOUNDID_LDAP_SDK=DEBUG
```

---

## 📚 Связанные документы

- [SECURITY_IMPLEMENTATION_REPORT.md](docs/SECURITY_IMPLEMENTATION_REPORT.md)
- [EnterpriseAuthConfig.kt](server/api/src/main/kotlin/com/company/ipcamera/server/config/EnterpriseAuthConfig.kt)
- [LdapAuthService.kt](server/api/src/main/kotlin/com/company/ipcamera/server/security/LdapAuthService.kt)

---

## 🎯 Следующие шаги

1. ✅ LDAP Authentication — завершено
2. ✅ AD Integration — завершено
3. ✅ Group-based Roles — завершено
4. ⏳ LDAP Sync Service — запланировано
5. ⏳ Multi-domain support — запланировано

---

**Подготовлено:** NLP-Core-Team  
**Дата:** 2026-01-28  
**Статус:** ✅ Завершено
