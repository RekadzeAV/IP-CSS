# Реализация хранения токенов в httpOnly cookies

**Версия проекта:** Alfa-0.0.1
**Дата создания:** 26 January 2026
**Статус:** ✅ Реализовано

> **📚 Связанные документы:**
> - [SECURITY_BEST_PRACTICES.md](SECURITY_BEST_PRACTICES.md) - Лучшие практики безопасности
> - [API.md](API.md) - API документация
> - [SERVER_IMPLEMENTATION_PLAN.md](SERVER_IMPLEMENTATION_PLAN.md) - План реализации сервера

---

## 📋 Содержание

1. [Обзор](#обзор)
2. [Архитектура решения](#архитектура-решения)
3. [Серверная реализация](#серверная-реализация)
4. [Клиентская реализация](#клиентская-реализация)
5. [Безопасность](#безопасность)
6. [Миграция с localStorage](#миграция-с-localstorage)
7. [Тестирование](#тестирование)
8. [Troubleshooting](#troubleshooting)

---

## Обзор

Проект использует **httpOnly cookies** для хранения JWT токенов вместо localStorage. Это обеспечивает защиту от XSS атак, так как токены недоступны из JavaScript.

### Преимущества

- ✅ **Защита от XSS:** Токены недоступны из JavaScript
- ✅ **Автоматическая отправка:** Cookies отправляются автоматически с каждым запросом
- ✅ **Защита от CSRF:** Используется `sameSite=Lax` атрибут
- ✅ **Безопасность в production:** `secure=true` для HTTPS

### Компоненты

- **Сервер:** `AuthRoutes.kt` - установка токенов в cookies
- **Middleware:** `CookieAuthMiddleware.kt` - чтение токенов из cookies
- **Клиент:** `authService.ts` - работа с аутентификацией через cookies

---

## Архитектура решения

```
┌─────────────────┐
│   Клиент (Web)  │
│                 │
│  authService.ts │
│  withCredentials│
└────────┬────────┘
         │ HTTP Request (with cookies)
         ▼
┌─────────────────┐
│  CookieAuth     │
│  Middleware     │
│  (Application)  │
└────────┬────────┘
         │ Extract token from cookie
         │ Set Authorization header
         ▼
┌─────────────────┐
│  JWT Auth       │
│  Middleware     │
│  (Application)  │
└────────┬────────┘
         │ Validate token
         ▼
┌─────────────────┐
│  AuthRoutes     │
│  (Login/Refresh)│
└────────┬────────┘
         │ Set cookies in response
         ▼
┌─────────────────┐
│   Response      │
│  (with cookies) │
└─────────────────┘
```

---

## Серверная реализация

### 1. Установка токенов в cookies (Login)

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/routing/AuthRoutes.kt`

```kotlin
// Генерируем токены
val accessToken = JwtConfig.generateAccessToken(
    userId = user.id,
    username = user.username,
    role = user.role.name,
    permissions = user.permissions
)
val refreshToken = JwtConfig.generateRefreshToken(user.id)

// Сохраняем refresh token в БД
userRepository.saveRefreshToken(refreshToken, user.id)

// Устанавливаем токены в httpOnly cookies
val isProduction = System.getenv("ENVIRONMENT") == "production"
val cookieMaxAge = (JwtConfig.refreshTokenExpiration / 1000).toInt()

// Access token cookie (короткоживущий)
call.response.cookies.append(
    name = "access_token",
    value = accessToken,
    maxAge = (JwtConfig.accessTokenExpiration / 1000).toInt(),
    httpOnly = true,
    secure = isProduction,  // Только HTTPS в продакшене
    sameSite = SameSite.Lax,
    path = "/"
)

// Refresh token cookie (долгоживущий)
call.response.cookies.append(
    name = "refresh_token",
    value = refreshToken,
    maxAge = cookieMaxAge,
    httpOnly = true,
    secure = isProduction,
    sameSite = SameSite.Lax,
    path = "/"
)

// Формируем ответ (без токенов в теле для безопасности)
val response = LoginResponse(
    accessToken = "",  // Не отправляем в теле
    refreshToken = "",  // Не отправляем в теле
    user = UserInfoDto(...)
)
```

### 2. CookieAuthMiddleware

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/CookieAuthMiddleware.kt`

```kotlin
fun Application.configureCookieAuth() {
    intercept(ApplicationCallPipeline.Authentication) {
        val tokenFromCookie = call.request.cookies["access_token"]

        // Если токен есть в cookie, но нет в заголовке Authorization, устанавливаем его
        if (tokenFromCookie != null && call.request.headers["Authorization"] == null) {
            call.request.headers.append("Authorization", "Bearer $tokenFromCookie")
        }
    }
}

// Extension функция для получения токена
fun ApplicationCall.getJwtToken(): String? {
    // Сначала проверяем cookie (приоритет для httpOnly cookies)
    val tokenFromCookie = request.cookies["access_token"]
    if (tokenFromCookie != null) {
        return tokenFromCookie
    }

    // Затем проверяем заголовок Authorization (для обратной совместимости)
    val authHeader = request.headers["Authorization"]
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        return authHeader.removePrefix("Bearer ")
    }

    return null
}
```

### 3. Подключение middleware

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/Application.kt`

```kotlin
fun Application.module() {
    // ...

    // Cookie Auth Middleware - должен быть ДО JWT Authentication
    configureCookieAuth()

    // JWT Authentication
    install(Authentication) {
        jwt("jwt-auth") {
            realm = JwtConfig.realm
            verifier(JwtConfig.createVerifier())
            // Токен теперь может быть в заголовке Authorization (установлен middleware из cookie)
            validate { credential ->
                if (credential.payload.issuer == "ip-camera-server" &&
                    credential.payload.audience.contains("ip-camera-client")) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    // ...
}
```

### 4. Обновление токена (Refresh)

```kotlin
// Получаем refresh token из cookie
val refreshToken = call.request.cookies["refresh_token"]
    ?: try {
        val request = call.receive<RefreshTokenRequest>()
        request.refreshToken
    } catch (e: Exception) {
        ""
    }

// Проверяем и валидируем токен
// ...

// Генерируем новые токены
val newAccessToken = JwtConfig.generateAccessToken(...)
val newRefreshToken = JwtConfig.generateRefreshToken(user.id)

// Устанавливаем новые токены в cookies
call.response.cookies.append(
    name = "access_token",
    value = newAccessToken,
    maxAge = (JwtConfig.accessTokenExpiration / 1000).toInt(),
    httpOnly = true,
    secure = isProduction,
    sameSite = SameSite.Lax,
    path = "/"
)
// ... аналогично для refresh_token
```

### 5. Выход из системы (Logout)

```kotlin
// Отзываем refresh token
if (refreshToken != null) {
    userRepository.revokeRefreshToken(refreshToken)
}

// Удаляем cookies
call.response.cookies.append(
    name = "access_token",
    value = "",
    maxAge = 0,  // Удалить cookie
    httpOnly = true,
    secure = System.getenv("ENVIRONMENT") == "production",
    sameSite = SameSite.Lax,
    path = "/"
)
// ... аналогично для refresh_token
```

### 6. Настройка CORS

**Важно:** Для работы с cookies необходимо настроить CORS:

```kotlin
install(CORS) {
    val allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS")
        ?.split(",")
        ?.map { it.trim() }
        ?: listOf("http://localhost:3000", "http://localhost:8080")

    allowedOrigins.forEach { origin ->
        allowHost(origin, schemes = listOf("http", "https"))
    }

    allowHeader("Content-Type")
    allowHeader("Authorization")
    allowMethod(HttpMethod.Get)
    allowMethod(HttpMethod.Post)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Options)
    allowCredentials = true  // ⚠️ ОБЯЗАТЕЛЬНО для cookies
}
```

---

## Клиентская реализация

### 1. AuthService

**Файл:** `server/web/src/services/authService.ts`

```typescript
import apiClient from '@/utils/api';

export const authService = {
  /**
   * Вход в систему
   * Токены теперь хранятся в httpOnly cookies, автоматически устанавливаются сервером
   */
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', credentials, {
      withCredentials: true,  // ⚠️ Важно для отправки cookies
    });
    if (response.data.success && response.data.data) {
      // Токены теперь в httpOnly cookies, не нужно сохранять в localStorage
      // Сервер устанавливает cookies автоматически
      return response.data.data;
    }
    throw new Error(response.data.message || 'Login failed');
  },

  /**
   * Выход из системы
   * Сервер удалит cookies автоматически
   */
  async logout(): Promise<void> {
    try {
      await apiClient.post('/auth/logout', {}, { withCredentials: true });
      // Cookies будут удалены сервером автоматически
    } catch (error) {
      console.error('Logout error:', error);
    }
  },

  /**
   * Обновить токен
   * Refresh token берется из cookie автоматически
   */
  async refreshToken(): Promise<void> {
    const response = await apiClient.post<ApiResponse<LoginResponse>>('/auth/refresh', {}, {
      withCredentials: true,  // ⚠️ Важно для отправки cookies
    });

    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to refresh token');
    }
    // Новые токены устанавливаются в cookies сервером
  },

  /**
   * Получить информацию о текущем пользователе
   */
  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<ApiResponse<User>>('/users/me', {
      withCredentials: true,  // ⚠️ Важно для отправки cookies
    });
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to get current user');
  },

  /**
   * Проверить, авторизован ли пользователь
   * Проверяем через запрос к API, так как токен в httpOnly cookie недоступен из JS
   */
  async isAuthenticated(): Promise<boolean> {
    try {
      await this.getCurrentUser();
      return true;
    } catch {
      return false;
    }
  },

  /**
   * Получить токен из cookie
   * Токен в httpOnly cookie недоступен из JavaScript для безопасности
   * Используйте getCurrentUser() для проверки аутентификации
   */
  getToken(): string | null {
    // Токен в httpOnly cookie недоступен из JavaScript
    // Это сделано для защиты от XSS атак
    return null;
  },
};
```

### 2. Redux Slice

**Файл:** `server/web/src/store/slices/authSlice.ts`

```typescript
const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<{ token: string; user: User }>) => {
      // Токен теперь в httpOnly cookie, не сохраняем его в state
      state.token = null;
      state.user = action.payload.user;
      state.isAuthenticated = true;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.fulfilled, (state, action) => {
        state.loading = false;
        // Токены теперь в httpOnly cookies, не сохраняем их в state
        state.token = null;
        state.user = action.payload.user;
        state.isAuthenticated = true;
        state.error = null;
      })
      // ...
  },
});
```

### 3. Axios конфигурация

**Файл:** `server/web/src/utils/api.ts`

```typescript
import axios from 'axios';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1',
  withCredentials: true,  // ⚠️ Важно: отправляет cookies с каждым запросом
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor для обработки ошибок
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Попытка обновить токен
      try {
        await authService.refreshToken();
        // Повторяем оригинальный запрос
        return apiClient.request(error.config);
      } catch (refreshError) {
        // Если refresh не удался, перенаправляем на логин
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

---

## Безопасность

### Параметры cookies

| Параметр | Значение | Описание |
|----------|----------|----------|
| `httpOnly` | `true` | Защита от XSS - токены недоступны из JavaScript |
| `secure` | `isProduction` | Только HTTPS в продакшене |
| `sameSite` | `Lax` | Защита от CSRF атак |
| `path` | `"/"` | Cookie доступен для всех путей |
| `maxAge` | Зависит от типа токена | Access: 15 минут, Refresh: 7 дней |

### Защита от атак

1. **XSS (Cross-Site Scripting):**
   - ✅ Токены в httpOnly cookies недоступны из JavaScript
   - ✅ Невозможно украсть токены через XSS

2. **CSRF (Cross-Site Request Forgery):**
   - ✅ `sameSite=Lax` предотвращает отправку cookies с других доменов
   - ⚠️ Для дополнительной защиты можно добавить CSRF токены

3. **Man-in-the-Middle:**
   - ✅ `secure=true` в production обеспечивает передачу только по HTTPS
   - ✅ Certificate pinning (планируется)

### Рекомендации

1. **Production:**
   - Убедитесь, что `ENVIRONMENT=production` установлен
   - Проверьте, что `secure=true` работает корректно
   - Настройте правильные CORS origins

2. **Development:**
   - `secure=false` для работы через HTTP
   - Настройте CORS для localhost

3. **Мониторинг:**
   - Логируйте все попытки аутентификации
   - Мониторьте подозрительную активность
   - Отслеживайте частоту обновления токенов

---

## Миграция с localStorage

### Что изменилось

**До миграции:**
```typescript
// ❌ Старый способ (небезопасный)
const token = response.data.token;
localStorage.setItem('access_token', token);
```

**После миграции:**
```typescript
// ✅ Новый способ (безопасный)
// Токены автоматически в httpOnly cookies
// Не нужно ничего сохранять вручную
```

### Шаги миграции

1. ✅ Обновлен сервер для установки токенов в cookies
2. ✅ Добавлен CookieAuthMiddleware
3. ✅ Обновлен клиент для использования `withCredentials: true`
4. ✅ Удалено использование localStorage для токенов
5. ✅ Обновлен Redux slice

### Обратная совместимость

Для обратной совместимости поддерживается заголовок `Authorization`:
- Если токен есть в cookie, он используется (приоритет)
- Если токена нет в cookie, проверяется заголовок Authorization
- Это позволяет постепенно мигрировать клиенты

---

## Тестирование

### Ручное тестирование

1. **Логин:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}' \
     -c cookies.txt -v
   ```
   Проверьте, что cookies установлены:
   - `access_token` (httpOnly, secure)
   - `refresh_token` (httpOnly, secure)

2. **Запрос с токеном:**
   ```bash
   curl -X GET http://localhost:8080/api/v1/cameras \
     -b cookies.txt -v
   ```
   Проверьте, что запрос успешен.

3. **Обновление токена:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/refresh \
     -b cookies.txt -c cookies.txt -v
   ```
   Проверьте, что новые cookies установлены.

4. **Выход:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/logout \
     -b cookies.txt -v
   ```
   Проверьте, что cookies удалены (maxAge=0).

### Автоматическое тестирование

```kotlin
@Test
fun `test login sets httpOnly cookies`() {
    val response = client.post("/api/v1/auth/login") {
        contentType(ContentType.Application.Json)
        setBody(LoginRequest("admin", "admin123"))
    }

    assertEquals(HttpStatusCode.OK, response.status)

    val cookies = response.setCookie()
    assertNotNull(cookies["access_token"])
    assertNotNull(cookies["refresh_token"])
    assertTrue(cookies["access_token"]?.httpOnly == true)
    assertTrue(cookies["refresh_token"]?.httpOnly == true)
}
```

---

## Troubleshooting

### Проблема: Cookies не устанавливаются

**Причины:**
1. CORS не настроен с `allowCredentials = true`
2. Неправильный origin в CORS
3. Браузер блокирует cookies (третьи лица)

**Решение:**
```kotlin
install(CORS) {
    allowCredentials = true  // ⚠️ Обязательно
    allowHost("http://localhost:3000")
    // ...
}
```

### Проблема: Cookies не отправляются

**Причины:**
1. Клиент не использует `withCredentials: true`
2. Неправильный domain/path
3. Браузер блокирует cookies

**Решение:**
```typescript
// Убедитесь, что используете withCredentials
const response = await apiClient.post('/auth/login', data, {
  withCredentials: true
});
```

### Проблема: 401 Unauthorized после логина

**Причины:**
1. CookieAuthMiddleware не подключен
2. Неправильный порядок middleware
3. JWT verifier не настроен

**Решение:**
```kotlin
// CookieAuthMiddleware должен быть ДО JWT Authentication
configureCookieAuth()  // Сначала
install(Authentication) { ... }  // Потом
```

### Проблема: Cookies не работают в production

**Причины:**
1. `secure=true` требует HTTPS
2. Неправильный domain
3. SameSite политика

**Решение:**
- Убедитесь, что используете HTTPS в production
- Проверьте настройки `secure` и `sameSite`
- Проверьте domain cookies

---

## Заключение

Реализация хранения токенов в httpOnly cookies обеспечивает:

- ✅ Защиту от XSS атак
- ✅ Автоматическую отправку токенов
- ✅ Безопасное хранение
- ✅ Защиту от CSRF (через sameSite)

**Статус:** ✅ Полностью реализовано и протестировано

**Последнее обновление:** 26 January 2026
