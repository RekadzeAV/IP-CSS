package com.company.ipcamera.server.routing

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.config.RedisConfig
import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireAdmin
import com.company.ipcamera.server.middleware.RateLimitMiddleware
import com.company.ipcamera.server.middleware.checkRateLimit
import com.company.ipcamera.server.middleware.getJwtToken
import com.company.ipcamera.server.repository.ServerUserRepository
import com.company.ipcamera.server.security.CaptchaValidator
import com.company.ipcamera.server.security.ExternalAuthProvider
import com.company.ipcamera.server.security.LoginAttemptTracker
import com.company.ipcamera.server.security.SecurityLogger
import com.company.ipcamera.server.security.OAuth2Service
import com.company.ipcamera.server.security.OAuth2StateStore
import com.company.ipcamera.server.security.TotpService
import com.company.ipcamera.server.security.TokenBlacklistService
import com.company.ipcamera.server.security.TokenRotationService
import com.company.ipcamera.server.config.EnterpriseAuthConfig
import com.company.ipcamera.server.service.ApiMetricsService
import com.company.ipcamera.server.validation.RequestValidator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger {}

private data class ExternalAuthProviderStatusDto(
    val id: String,
    val enabled: Boolean,
    val implemented: Boolean,
    val mode: String,
    val reachable: Boolean,
    val statusMessage: String
)

/**
 * Маршруты для аутентификации
 *
 * ✅ Rate limiting для login endpoint - реализовано
 * ✅ Защита от timing атак - реализовано в ServerUserRepository.authenticate()
 */
fun Route.authRoutes() {
    val userRepository: ServerUserRepository by inject()
    val rateLimiter: RateLimitMiddleware by inject()
    val redisCommands: io.lettuce.core.api.coroutines.RedisCoroutinesCommands<String, String> by inject()
    val loginAttemptTracker = LoginAttemptTracker(redisCommands)
    val externalAuthProviders: List<ExternalAuthProvider> by inject()
    val oauth2Service: OAuth2Service by inject()
    val oauth2StateStore: OAuth2StateStore by inject()
    val apiMetricsService: ApiMetricsService by inject()
    val tokenBlacklistService = TokenBlacklistService(redisCommands)
    val tokenRotationService: TokenRotationService by inject()

    route("/auth") {
        // GET /api/v1/auth/oauth2/login - редирект на IdP
        if (oauth2Service.isEnabled()) {
            get("/oauth2/login") {
                val returnTo = call.request.queryParameters["return_to"]
                val state = oauth2StateStore.createState(returnTo)
                val url = oauth2Service.buildAuthorizationUrl(state)
                call.respondRedirect(url)
            }
            get("/oauth2/callback") {
                val code = call.request.queryParameters["code"]
                val state = call.request.queryParameters["state"]
                val errorParam = call.request.queryParameters["error"]
                val successUrl = oauth2StateStore.consumeState(state ?: "")?.takeIf { it.isNotBlank() }
                    ?: EnterpriseAuthConfig.oauth2FrontendSuccessUrl
                val errorUrl = EnterpriseAuthConfig.oauth2FrontendErrorUrl
                if (errorParam != null) {
                    call.respondRedirect("$errorUrl&oauth_error=$errorParam")
                    return@get
                }
                if (code.isNullOrBlank() || state.isNullOrBlank()) {
                    call.respondRedirect("$errorUrl&reason=missing_params")
                    return@get
                }
                val redirectUri = EnterpriseAuthConfig.oauth2RedirectUri!!
                val user = oauth2Service.authenticateWithCode(code, redirectUri)
                if (user == null) {
                    call.respondRedirect("$errorUrl&reason=auth_failed")
                    return@get
                }
                val accessToken = JwtConfig.generateAccessToken(user.id, user.username, user.role.name, user.permissions)
                val refreshToken = JwtConfig.generateRefreshToken(user.id)
                userRepository.saveRefreshToken(refreshToken, user.id)
                val isProduction = System.getenv("ENVIRONMENT") == "production"
                call.response.cookies.append("access_token", accessToken, maxAge = (JwtConfig.accessTokenExpiration / 1000).toLong(), httpOnly = true, secure = isProduction, extensions = mapOf("SameSite" to "Lax"), path = "/")
                call.response.cookies.append("refresh_token", refreshToken, maxAge = (JwtConfig.refreshTokenExpiration / 1000).toLong(), httpOnly = true, secure = isProduction, extensions = mapOf("SameSite" to "Lax"), path = "/")
                SecurityLogger.logLoginSuccess(user.id, user.username, call.request.local.remoteHost, call.request.headers["User-Agent"])
                call.respondRedirect(successUrl)
            }
        }

        // POST /api/v1/auth/login - вход в систему
        post("/login") {
            try {
                val clientIp = call.request.local.remoteHost
                val identifier = "login:$clientIp"

                // Проверяем информацию о попытках входа
                val attemptInfo = loginAttemptTracker.getAttemptInfo(identifier)

                // Проверяем, не заблокирован ли IP
                if (attemptInfo.isBlocked) {
                    apiMetricsService.markLoginFailure()
                    logger.warn { "Login blocked for $clientIp after ${attemptInfo.attemptCount} failed attempts" }
                    SecurityLogger.logSuspiciousActivity(
                        "Login blocked due to too many failed attempts",
                        null,
                        clientIp,
                        mapOf("attempt_count" to attemptInfo.attemptCount)
                    )
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        ApiResponse<LoginResponse>(
                            success = false,
                            data = null,
                            message = "Too many failed login attempts. Please try again later."
                        )
                    )
                    return@post
                }

                // Rate limiting для защиты от брутфорса (строгий лимит для login)
                if (!call.checkRateLimit(identifier, rateLimiter, rateLimiter.loginConfig)) {
                    return@post
                }

                val request = call.receive<LoginRequest>()

                // Валидация входных данных
                val validationResult = RequestValidator.validateLoginRequest(request)
                if (validationResult is com.company.ipcamera.server.validation.ValidationResult.Error) {
                    apiMetricsService.markLoginFailure()
                    logger.warn { "Login validation failed from $clientIp: ${validationResult.message}" }
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<LoginResponse>(
                            success = false,
                            data = null,
                            message = validationResult.message
                        )
                    )
                    return@post
                }

                // Проверяем CAPTCHA, если требуется
                if (attemptInfo.requiresCaptcha) {
                    if (request.captchaToken == null || !CaptchaValidator.validateToken(request.captchaToken)) {
                        apiMetricsService.markLoginFailure()
                        logger.warn { "CAPTCHA validation failed for $clientIp" }
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<LoginResponse>(
                                success = false,
                                data = null,
                                message = "CAPTCHA verification required. Please complete the CAPTCHA and try again."
                            )
                        )
                        return@post
                    }
                }

                // Применяем задержку для защиты от timing атак
                loginAttemptTracker.applyDelay(attemptInfo.attemptCount)

                // Аутентификация: сначала внешние провайдеры (LDAP/AD, SSO), затем локальная.
                // Если внешний провайдер временно недоступен, продолжаем по цепочке, не ломая вход.
                var user = null as com.company.ipcamera.shared.domain.model.User?
                for (provider in externalAuthProviders) {
                    if (!provider.isEnabled()) continue
                    user = try {
                        provider.authenticate(request.username, request.password)
                    } catch (e: Exception) {
                        logger.warn(e) { "External auth provider ${provider.id} failed, fallback to next provider/local auth" }
                        null
                    }
                    if (user != null) break
                }
                if (user == null) {
                    user = userRepository.authenticate(request.username, request.password)
                }

                if (user == null) {
                    apiMetricsService.markLoginFailure()
                    // Регистрируем неудачную попытку
                    val newAttemptInfo = loginAttemptTracker.recordFailedAttempt(identifier)

                    // Используем одинаковое сообщение для несуществующих пользователей и неправильных паролей
                    // Это защищает от перечисления пользователей
                    val ipAddress = call.request.local.remoteHost
                    val userAgent = call.request.headers["User-Agent"]
                    SecurityLogger.logLoginFailure(request.username, ipAddress, userAgent, "Invalid credentials")
                    logger.warn { "Failed login attempt for username: ${request.username} from $ipAddress (attempts: ${newAttemptInfo.attemptCount})" }

                    val message = if (newAttemptInfo.requiresCaptcha && request.captchaToken == null) {
                        "Invalid username or password. CAPTCHA verification required."
                    } else {
                        "Invalid username or password"
                    }

                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiResponse<LoginResponse>(
                            success = false,
                            data = null,
                            message = message
                        )
                    )
                    return@post
                }

                // Успешный вход - сбрасываем счетчик попыток
                loginAttemptTracker.resetAttempts(identifier)

                // Если включена 2FA — возвращаем tempToken, полные токены после проверки кода
                if (userRepository.isTotpEnabled(user.id)) {
                    val tempToken = JwtConfig.generate2FaTempToken(user.id)
                    val response = LoginResponse(
                        accessToken = "",
                        refreshToken = "",
                        user = UserInfoDto(
                            id = user.id,
                            username = user.username,
                            email = user.email,
                            fullName = user.fullName,
                            role = user.role.name,
                            permissions = user.permissions
                        ),
                        needs2fa = true,
                        tempToken = tempToken
                    )
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = response, message = "2FA code required"))
                    return@post
                }

                // Генерируем токены
                val accessToken = JwtConfig.generateAccessToken(
                    userId = user.id,
                    username = user.username,
                    role = user.role.name,
                    permissions = user.permissions
                )
                val refreshToken = JwtConfig.generateRefreshToken(user.id)

                // Сохраняем refresh token
                userRepository.saveRefreshToken(refreshToken, user.id)

                // Устанавливаем токены в httpOnly cookies для защиты от XSS
                val isProduction = System.getenv("ENVIRONMENT") == "production"
                val cookieMaxAge = (JwtConfig.refreshTokenExpiration / 1000).toInt() // в секундах

                // Access token cookie (короткоживущий)
                call.response.cookies.append(
                    name = "access_token",
                    value = accessToken,
                    maxAge = (JwtConfig.accessTokenExpiration / 1000).toLong(),
                    httpOnly = true,
                    secure = isProduction, // Только HTTPS в продакшене
                    extensions = mapOf("SameSite" to "Lax"),
                    path = "/"
                )

                // Refresh token cookie (долгоживущий)
                call.response.cookies.append(
                    name = "refresh_token",
                    value = refreshToken,
                    maxAge = cookieMaxAge.toLong(),
                    httpOnly = true,
                    secure = isProduction,
                    extensions = mapOf("SameSite" to "Lax"),
                    path = "/"
                )

                // Формируем ответ (без токенов в теле для безопасности)
                val response = LoginResponse(
                    accessToken = "", // Не отправляем в теле
                    refreshToken = "", // Не отправляем в теле
                    user = UserInfoDto(
                        id = user.id,
                        username = user.username,
                        email = user.email,
                        fullName = user.fullName,
                        role = user.role.name,
                        permissions = user.permissions
                    )
                )

                val ipAddress = call.request.local.remoteHost
                val userAgent = call.request.headers["User-Agent"]
                SecurityLogger.logLoginSuccess(user.id, user.username, ipAddress, userAgent)
                logger.info { "User logged in successfully: ${user.username} (id: ${user.id})" }
                apiMetricsService.markLoginSuccess()

                // Сбрасываем rate limit после успешного входа
                rateLimiter.resetLimit("login:$clientIp")

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = response,
                        message = "Login successful"
                    )
                )
            } catch (e: Exception) {
                apiMetricsService.markLoginFailure()
                logger.error(e) { "Error during login" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<LoginResponse>(
                        success = false,
                        data = null,
                        message = "Internal server error: ${e.message}"
                    )
                )
            }
        }

        // POST /api/v1/auth/2fa/verify - проверка кода 2FA и выдача токенов
        post("/2fa/verify") {
            try {
                val request = call.receive<Verify2FaRequest>()
                if (request.tempToken.isBlank() || request.code.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<LoginResponse>(success = false, data = null, message = "tempToken and code are required"))
                    return@post
                }
                val verifier = JwtConfig.createVerifier()
                val decoded = try { verifier.verify(request.tempToken) } catch (e: Exception) {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponse<LoginResponse>(success = false, data = null, message = "Invalid or expired temporary token"))
                    return@post
                }
                if (decoded.getClaim("type").asString() != "2fa_temp") {
                    call.respond(HttpStatusCode.Unauthorized, ApiResponse<LoginResponse>(success = false, data = null, message = "Invalid token type"))
                    return@post
                }
                val userId = decoded.subject
                val user = userRepository.getUserById(userId) ?: run {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<LoginResponse>(success = false, data = null, message = "User not found"))
                    return@post
                }
                val secret = userRepository.getTotpSecret(userId) ?: run {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<LoginResponse>(success = false, data = null, message = "2FA not configured"))
                    return@post
                }
                if (!TotpService.verifyCode(secret, request.code)) {
                    SecurityLogger.logSuspiciousActivity("Invalid 2FA code", userId, call.request.local.remoteHost, emptyMap())
                    call.respond(HttpStatusCode.Unauthorized, ApiResponse<LoginResponse>(success = false, data = null, message = "Invalid 2FA code"))
                    return@post
                }
                val accessToken = JwtConfig.generateAccessToken(user.id, user.username, user.role.name, user.permissions)
                val refreshToken = JwtConfig.generateRefreshToken(user.id)
                userRepository.saveRefreshToken(refreshToken, user.id)
                val isProduction = System.getenv("ENVIRONMENT") == "production"
                call.response.cookies.append("access_token", accessToken, maxAge = (JwtConfig.accessTokenExpiration / 1000).toLong(), httpOnly = true, secure = isProduction, extensions = mapOf("SameSite" to "Lax"), path = "/")
                call.response.cookies.append("refresh_token", refreshToken, maxAge = (JwtConfig.refreshTokenExpiration / 1000).toLong(), httpOnly = true, secure = isProduction, extensions = mapOf("SameSite" to "Lax"), path = "/")
                val response = LoginResponse(accessToken = "", refreshToken = "", user = UserInfoDto(user.id, user.username, user.email, user.fullName, user.role.name, user.permissions))
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = response, message = "Login successful"))
            } catch (e: Exception) {
                logger.error(e) { "Error during 2FA verify" }
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<LoginResponse>(success = false, data = null, message = "Internal server error"))
            }
        }

        // POST /api/v1/auth/refresh - обновление токена
        post("/refresh") {
            try {
                // Получаем refresh token из cookie или из тела запроса (для обратной совместимости)
                val refreshToken = call.request.cookies["refresh_token"]
                    ?: try {
                        val request = call.receive<RefreshTokenRequest>()
                        request.refreshToken
                    } catch (e: Exception) {
                        ""
                    }

                if (refreshToken.isEmpty()) {
                    apiMetricsService.markRefreshFailure()
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<RefreshTokenResponse>(
                            success = false,
                            data = null,
                            message = "Refresh token is required"
                        )
                    )
                    return@post
                }

                // Проверяем refresh token
                val decodedJWT = try {
                    tokenRotationService.jwtService.validateRefreshToken(refreshToken)
                } catch (e: Exception) {
                    apiMetricsService.markRefreshFailure()
                    val ipAddress = call.request.local.remoteHost
                    SecurityLogger.logInvalidToken(ipAddress, e.message)
                    logger.warn { "Invalid refresh token: ${e.message}" }
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiResponse<RefreshTokenResponse>(
                            success = false,
                            data = null,
                            message = "Invalid refresh token"
                        )
                    )
                    return@post
                }

                // Получаем userId из токена
                val userId = decodedJWT.subject

                // Проверяем, что токен не был отозван (через blacklist)
                if (tokenRotationService.tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
                    apiMetricsService.markRefreshFailure()
                    logger.warn { "Refresh token is blacklisted for user: $userId" }
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiResponse<RefreshTokenResponse>(
                            success = false,
                            data = null,
                            message = "Refresh token has been revoked"
                        )
                    )
                    return@post
                }

                // Проверяем, что токен существует в БД
                val storedUserId = userRepository.getUserIdByRefreshToken(refreshToken)
                if (storedUserId == null || storedUserId != userId) {
                    apiMetricsService.markRefreshFailure()
                    logger.warn { "Refresh token not found or revoked for user: $userId" }
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiResponse<RefreshTokenResponse>(
                            success = false,
                            data = null,
                            message = "Refresh token not found or revoked"
                        )
                    )
                    return@post
                }

                // Получаем пользователя
                val user = userRepository.getUserById(userId)
                if (user == null || !user.isActive) {
                    apiMetricsService.markRefreshFailure()
                    logger.warn { "User not found or inactive: $userId" }
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiResponse<RefreshTokenResponse>(
                            success = false,
                            data = null,
                            message = "User not found or inactive"
                        )
                    )
                    return@post
                }

                // Выполняем полную ротацию токенов через TokenRotationService
                val result = tokenRotationService.fullTokenRotation(
                    oldRefreshToken = refreshToken,
                    userId = user.id,
                    username = user.username,
                    role = user.role.name,
                    permissions = user.permissions
                )

                // Устанавливаем новые токены в cookies
                val isProduction = System.getenv("ENVIRONMENT") == "production"
                call.response.cookies.append(
                    name = "access_token",
                    value = result.accessToken,
                    maxAge = (JwtConfig.accessTokenExpiration / 1000).toLong(),
                    httpOnly = true,
                    secure = isProduction,
                    extensions = mapOf("SameSite" to "Lax"),
                    path = "/"
                )
                call.response.cookies.append(
                    name = "refresh_token",
                    value = result.refreshToken,
                    maxAge = (JwtConfig.refreshTokenExpiration / 1000).toLong(),
                    httpOnly = true,
                    secure = isProduction,
                    extensions = mapOf("SameSite" to "Lax"),
                    path = "/"
                )

                val response = RefreshTokenResponse(
                    accessToken = "", // Не отправляем в теле
                    refreshToken = "" // Не отправляем в теле
                )

                logger.info { "Token refreshed (rotated) for user: ${user.username} (id: ${user.id})" }
                apiMetricsService.markRefreshSuccess()

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = response,
                        message = "Token refreshed successfully"
                    )
                )
            } catch (e: Exception) {
                apiMetricsService.markRefreshFailure()
                logger.error(e) { "Error during token refresh" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<RefreshTokenResponse>(
                        success = false,
                        data = null,
                        message = "Internal server error: ${e.message}"
                    )
                )
            }
        }

        // POST /api/v1/auth/rotate - ручная ротация токена
        authenticate("jwt-auth") {
            post("/rotate") {
                try {
                    val refreshToken = call.request.cookies["refresh_token"]
                        ?: try {
                            val request = call.receive<com.company.ipcamera.server.dto.RotateTokenRequest>()
                            request.refreshToken
                        } catch (e: Exception) {
                            ""
                        }

                    if (refreshToken.isEmpty()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<com.company.ipcamera.server.dto.RotateTokenResponse>(
                                success = false,
                                data = null,
                                message = "Refresh token is required"
                            )
                        )
                        return@post
                    }

                    // Проверяем refresh token
                    val decodedJWT = try {
                        tokenRotationService.jwtService.validateRefreshToken(refreshToken)
                    } catch (e: Exception) {
                        logger.warn { "Invalid refresh token for rotation: ${e.message}" }
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<com.company.ipcamera.server.dto.RotateTokenResponse>(
                                success = false,
                                data = null,
                                message = "Invalid refresh token"
                            )
                        )
                        return@post
                    }

                    // Проверяем, что токен не в blacklist
                    if (tokenRotationService.tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<com.company.ipcamera.server.dto.RotateTokenResponse>(
                                success = false,
                                data = null,
                                message = "Refresh token has been revoked"
                            )
                        )
                        return@post
                    }

                    // Получаем userId
                    val userId = decodedJWT.subject

                    // Проверяем пользователя
                    val user = userRepository.getUserById(userId)
                    if (user == null || !user.isActive) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<com.company.ipcamera.server.dto.RotateTokenResponse>(
                                success = false,
                                data = null,
                                message = "User not found or inactive"
                            )
                        )
                        return@post
                    }

                    // Выполняем полную ротацию
                    val result = tokenRotationService.fullTokenRotation(
                        oldRefreshToken = refreshToken,
                        userId = user.id,
                        username = user.username,
                        role = user.role.name,
                        permissions = user.permissions
                    )

                    // Устанавливаем новые токены в cookies
                    val isProduction = System.getenv("ENVIRONMENT") == "production"
                    call.response.cookies.append(
                        name = "access_token",
                        value = result.accessToken,
                        maxAge = (JwtConfig.accessTokenExpiration / 1000).toLong(),
                        httpOnly = true,
                        secure = isProduction,
                        extensions = mapOf("SameSite" to "Lax"),
                        path = "/"
                    )
                    call.response.cookies.append(
                        name = "refresh_token",
                        value = result.refreshToken,
                        maxAge = (JwtConfig.refreshTokenExpiration / 1000).toLong(),
                        httpOnly = true,
                        secure = isProduction,
                        extensions = mapOf("SameSite" to "Lax"),
                        path = "/"
                    )

                    val response = com.company.ipcamera.server.dto.RotateTokenResponse(
                        accessToken = result.accessToken,
                        refreshToken = result.refreshToken,
                        accessTokenExpiresIn = result.accessTokenExpiresIn,
                        refreshTokenExpiresIn = result.refreshTokenExpiresIn
                    )

                    logger.info { "Token rotated manually for user: ${user.username} (id: ${user.id})" }

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = response,
                            message = "Token rotated successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error during token rotation" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<com.company.ipcamera.server.dto.RotateTokenResponse>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }
        }

        // GET /api/v1/auth/ws-token - получение токена для WebSocket подключения
        authenticate("jwt-auth") {
            // GET /api/v1/auth/providers/status - статус внешних провайдеров auth (только админ)
            get("/providers/status") {
                requireAdmin()

                try {
                    val statuses = externalAuthProviders.map { provider ->
                        val enabled = provider.isEnabled()
                        val implemented = provider !is com.company.ipcamera.server.security.KerberosAuthService
                        val availability = when (provider) {
                            is com.company.ipcamera.server.security.LdapAuthService -> provider.checkAvailability()
                            is com.company.ipcamera.server.security.KerberosAuthService -> provider.checkAvailability()
                            else -> (enabled to if (enabled) "enabled" else "disabled")
                        }

                        ExternalAuthProviderStatusDto(
                            id = provider.id,
                            enabled = enabled,
                            implemented = implemented,
                            mode = if (implemented) "active_or_fallback" else "stub",
                            reachable = availability.first,
                            statusMessage = availability.second
                        )
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = statuses,
                            message = "External auth providers status retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error retrieving external auth providers status" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<List<ExternalAuthProviderStatusDto>>(
                            success = false,
                            data = null,
                            message = "Internal server error"
                        )
                    )
                }
            }

            get("/ws-token") {
                try {
                    val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                    val tokenFromCookie = call.getJwtToken()
                    val fallbackVerifiedUserId = tokenFromCookie?.let { token ->
                        runCatching {
                            val verifier = JwtConfig.createVerifier()
                            verifier.verify(token).subject
                        }.getOrNull()
                    }
                    val userId = principal?.payload?.subject ?: fallbackVerifiedUserId
                    if (userId.isNullOrBlank()) {
                        apiMetricsService.markWsTokenFailure()
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<Map<String, String>>(
                                success = false,
                                data = null,
                                message = "User not authenticated"
                            )
                        )
                        return@get
                    }

                    val token = tokenFromCookie ?: call.request.header(HttpHeaders.Authorization)
                        ?.removePrefix("Bearer ")
                        ?.trim()
                    if (token.isNullOrBlank()) {
                        apiMetricsService.markWsTokenFailure()
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<Map<String, String>>(
                                success = false,
                                data = null,
                                message = "Token not found"
                            )
                        )
                        return@get
                    }

                    // Возвращаем токен для WebSocket подключения
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = mapOf("token" to token),
                            message = "Token retrieved successfully"
                        )
                    )
                    apiMetricsService.markWsTokenSuccess()
                } catch (e: Exception) {
                    apiMetricsService.markWsTokenFailure()
                    logger.error(e) { "Error retrieving WebSocket token" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Map<String, String>>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }
        }

        // POST /api/v1/auth/logout - выход из системы
        authenticate("jwt-auth") {
            post("/logout") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject

                    // Получаем refresh token из cookie
                    val refreshToken = call.request.cookies["refresh_token"]

                    // Отзываем refresh token и добавляем в blacklist
                    if (refreshToken != null) {
                        userRepository.revokeRefreshToken(refreshToken)
                        
                        // Добавляем токен в Redis blacklist
                        tokenBlacklistService.blacklistToken(refreshToken)
                        
                        logger.info { "Refresh token revoked and blacklisted for user: $userId" }
                    }

                    // Удаляем cookies
                    call.response.cookies.append(
                        name = "access_token",
                        value = "",
                        maxAge = 0L,
                        httpOnly = true,
                        secure = System.getenv("ENVIRONMENT") == "production",
                        extensions = mapOf("SameSite" to "Lax"),
                        path = "/"
                    )
                    call.response.cookies.append(
                        name = "refresh_token",
                        value = "",
                        maxAge = 0L,
                        httpOnly = true,
                        secure = System.getenv("ENVIRONMENT") == "production",
                        extensions = mapOf("SameSite" to "Lax"),
                        path = "/"
                    )

                    // Удаляем CSRF токен
                    call.response.cookies.append(
                        name = "csrf_token",
                        value = "",
                        maxAge = 0L,
                        httpOnly = false,
                        secure = System.getenv("ENVIRONMENT") == "production",
                        extensions = mapOf("SameSite" to "Strict"),
                        path = "/"
                    )

                    // Clear-Site-Data header для очистки всех данных сайта в браузере
                    call.response.headers.append(
                        "Clear-Site-Data",
                        "\"cache\", \"cookies\", \"storage\""
                    )

                        val ipAddress = call.request.local.remoteHost
                        if (userId != null) {
                            val user = userRepository.getUserById(userId)
                            if (user != null) {
                                SecurityLogger.logLogout(userId, user.username, ipAddress)
                            }
                        }
                        logger.info { "User logged out: $userId" }

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse<Unit>(
                            success = true,
                            data = null,
                            message = "Logout successful"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error during logout" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Unit>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }
        }

        // 2FA setup/confirm/disable (требуют JWT)
        authenticate("jwt-auth") {
            post("/2fa/setup") {
                try {
                    val principal = call.principal<JWTPrincipal>() ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse<TwoFaSetupResponse>(success = false, data = null, message = "Not authenticated"))
                    val userId = principal.payload.subject
                    val user = userRepository.getUserById(userId) ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse<TwoFaSetupResponse>(success = false, data = null, message = "User not found"))
                    val secret = TotpService.generateSecret()
                    userRepository.setTotpSecret(userId, secret)
                    userRepository.setTotpEnabled(userId, false)
                    val qrData = TotpService.getQrCodeData(secret, user.username)
                    val qrPng = TotpService.generateQrCodePng(secret, user.username)?.let { java.util.Base64.getEncoder().encodeToString(it) }
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = TwoFaSetupResponse(secret = secret, qrData = qrData, qrPngBase64 = qrPng), message = "Scan QR with authenticator app, then confirm with code"))
                } catch (e: Exception) {
                    logger.error(e) { "Error during 2FA setup" }
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<TwoFaSetupResponse>(success = false, data = null, message = "Internal server error"))
                }
            }
            post("/2fa/confirm") {
                try {
                    val principal = call.principal<JWTPrincipal>() ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse<Unit>(success = false, data = null, message = "Not authenticated"))
                    val userId = principal.payload.subject
                    val request = call.receive<Confirm2FaRequest>()
                    val code = request.code
                    if (code.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, data = null, message = "code required"))
                        return@post
                    }
                    val secret = userRepository.getTotpSecret(userId) ?: run {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, data = null, message = "Call /2fa/setup first"))
                        return@post
                    }
                    if (!TotpService.verifyCode(secret, code)) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, data = null, message = "Invalid code"))
                        return@post
                    }
                    userRepository.setTotpEnabled(userId, true)
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true, data = null, message = "2FA enabled"))
                } catch (e: Exception) {
                    logger.error(e) { "Error during 2FA confirm" }
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, data = null, message = "Internal server error"))
                }
            }
            post("/2fa/disable") {
                try {
                    val principal = call.principal<JWTPrincipal>() ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse<Unit>(success = false, data = null, message = "Not authenticated"))
                    val userId = principal.payload.subject
                    userRepository.clearTotp(userId)
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true, data = null, message = "2FA disabled"))
                } catch (e: Exception) {
                    logger.error(e) { "Error during 2FA disable" }
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, data = null, message = "Internal server error"))
                }
            }
        }
    }
}

