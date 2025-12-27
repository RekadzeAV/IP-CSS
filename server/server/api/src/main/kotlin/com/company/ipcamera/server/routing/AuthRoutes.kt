package com.company.ipcamera.server.routing

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.middleware.RateLimitMiddleware
import com.company.ipcamera.server.middleware.checkRateLimit
import com.company.ipcamera.server.repository.ServerUserRepository
import com.company.ipcamera.server.security.SecurityLogger
import com.company.ipcamera.server.validation.RequestValidator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger {}

/**
 * Маршруты для аутентификации
 *
 * TODO: Rate limiting для login endpoint
 * TODO: Защита от timing атак (константное время для проверки пароля)
 */
fun Route.authRoutes() {
    val userRepository: ServerUserRepository by inject()
    val rateLimiter: RateLimitMiddleware by inject()

    route("/auth") {
        // POST /api/v1/auth/login - вход в систему
        post("/login") {
            try {
                // Rate limiting для защиты от брутфорса (строгий лимит для login)
                val clientIp = call.request.origin.remoteHost
                if (!call.checkRateLimit("login:$clientIp", rateLimiter, rateLimiter.loginConfig)) {
                    return@post
                }

                val request = call.receive<LoginRequest>()

                // Валидация входных данных
                val validationResult = RequestValidator.validateLoginRequest(request)
                if (validationResult is com.company.ipcamera.server.validation.ValidationResult.Error) {
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

                // Аутентификация пользователя
                val user = userRepository.authenticate(request.username, request.password)

                if (user == null) {
                    // Используем одинаковое сообщение для несуществующих пользователей и неправильных паролей
                    // Это защищает от перечисления пользователей
                    val ipAddress = call.request.origin.remoteHost
                    val userAgent = call.request.headers["User-Agent"]
                    SecurityLogger.logLoginFailure(request.username, ipAddress, userAgent, "Invalid credentials")
                    logger.warn { "Failed login attempt for username: ${request.username} from $ipAddress" }
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiResponse<LoginResponse>(
                            success = false,
                            data = null,
                            message = "Invalid username or password"
                        )
                    )
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
                    maxAge = (JwtConfig.accessTokenExpiration / 1000).toInt(),
                    httpOnly = true,
                    secure = isProduction, // Только HTTPS в продакшене
                    sameSite = io.ktor.http.SameSite.Lax,
                    path = "/"
                )

                // Refresh token cookie (долгоживущий)
                call.response.cookies.append(
                    name = "refresh_token",
                    value = refreshToken,
                    maxAge = cookieMaxAge,
                    httpOnly = true,
                    secure = isProduction,
                    sameSite = io.ktor.http.SameSite.Lax,
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

                val ipAddress = call.request.origin.remoteHost
                val userAgent = call.request.headers["User-Agent"]
                SecurityLogger.logLoginSuccess(user.id, user.username, ipAddress, userAgent)
                logger.info { "User logged in successfully: ${user.username} (id: ${user.id})" }

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
                val verifier = JwtConfig.createVerifier()
                val decodedJWT: DecodedJWT = try {
                    verifier.verify(refreshToken)
                } catch (e: Exception) {
                    val ipAddress = call.request.origin.remoteHost
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

                // Проверяем, что это refresh token
                val tokenType = decodedJWT.getClaim("type").asString()
                if (tokenType != "refresh") {
                    logger.warn { "Token is not a refresh token" }
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiResponse<RefreshTokenResponse>(
                            success = false,
                            data = null,
                            message = "Invalid token type"
                        )
                    )
                    return@post
                }

                // Проверяем, что токен не был отозван
                val userId = decodedJWT.subject
                val storedUserId = userRepository.getUserIdByRefreshToken(refreshToken)

                if (storedUserId == null || storedUserId != userId) {
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

                // Отзываем старый refresh token (rotation)
                userRepository.revokeRefreshToken(refreshToken)

                // Генерируем новые токены
                val newAccessToken = JwtConfig.generateAccessToken(
                    userId = user.id,
                    username = user.username,
                    role = user.role.name,
                    permissions = user.permissions
                )
                val newRefreshToken = JwtConfig.generateRefreshToken(user.id)

                // Сохраняем новый refresh token
                userRepository.saveRefreshToken(newRefreshToken, user.id)

                // Устанавливаем новые токены в cookies
                val isProduction = System.getenv("ENVIRONMENT") == "production"
                call.response.cookies.append(
                    name = "access_token",
                    value = newAccessToken,
                    maxAge = (JwtConfig.accessTokenExpiration / 1000).toInt(),
                    httpOnly = true,
                    secure = isProduction,
                    sameSite = io.ktor.http.SameSite.Lax,
                    path = "/"
                )
                call.response.cookies.append(
                    name = "refresh_token",
                    value = newRefreshToken,
                    maxAge = (JwtConfig.refreshTokenExpiration / 1000).toInt(),
                    httpOnly = true,
                    secure = isProduction,
                    sameSite = io.ktor.http.SameSite.Lax,
                    path = "/"
                )

                val response = RefreshTokenResponse(
                    accessToken = "", // Не отправляем в теле
                    refreshToken = "" // Не отправляем в теле
                )

                logger.info { "Token refreshed for user: ${user.username} (id: ${user.id})" }

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = response,
                        message = "Token refreshed successfully"
                    )
                )
            } catch (e: Exception) {
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

        // POST /api/v1/auth/logout - выход из системы
        authenticate("jwt-auth") {
            post("/logout") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject

                    // Получаем refresh token из cookie
                    val refreshToken = call.request.cookies["refresh_token"]

                    // Отзываем refresh token, если он есть
                    if (refreshToken != null) {
                        userRepository.revokeRefreshToken(refreshToken)
                        logger.info { "Refresh token revoked for user: $userId" }
                    }

                    // Удаляем cookies
                    call.response.cookies.append(
                        name = "access_token",
                        value = "",
                        maxAge = 0,
                        httpOnly = true,
                        secure = System.getenv("ENVIRONMENT") == "production",
                        sameSite = io.ktor.http.SameSite.Lax,
                        path = "/"
                    )
                    call.response.cookies.append(
                        name = "refresh_token",
                        value = "",
                        maxAge = 0,
                        httpOnly = true,
                        secure = System.getenv("ENVIRONMENT") == "production",
                        sameSite = io.ktor.http.SameSite.Lax,
                        path = "/"
                    )

                        val user = userRepository.getUserById(userId)
                        val ipAddress = call.request.origin.remoteHost
                        if (user != null) {
                            SecurityLogger.logLogout(userId, user.username, ipAddress)
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
    }
}

