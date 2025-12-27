package com.company.ipcamera.server.routing

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.company.ipcamera.server.config.JwtConfig
import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.middleware.RateLimitMiddleware
import com.company.ipcamera.server.middleware.checkRateLimit
import com.company.ipcamera.server.repository.ServerUserRepository
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
                // Rate limiting для защиты от брутфорса
                val clientIp = call.request.origin.remoteHost
                if (!call.checkRateLimit("login:$clientIp", rateLimiter)) {
                    return@post
                }
                
                val request = call.receive<LoginRequest>()
                
                // Валидация входных данных
                if (request.username.isEmpty() || request.password.isEmpty()) {
                    logger.warn { "Login attempt with empty credentials from $clientIp" }
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<LoginResponse>(
                            success = false,
                            data = null,
                            message = "Username and password are required"
                        )
                    )
                    return@post
                }
                
                // Аутентификация пользователя
                val user = userRepository.authenticate(request.username, request.password)
                
                if (user == null) {
                    // Используем одинаковое сообщение для несуществующих пользователей и неправильных паролей
                    // Это защищает от перечисления пользователей
                    logger.warn { "Failed login attempt for username: ${request.username} from ${call.request.origin.remoteHost}" }
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
                
                // Формируем ответ
                val response = LoginResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    user = UserInfoDto(
                        id = user.id,
                        username = user.username,
                        email = user.email,
                        fullName = user.fullName,
                        role = user.role.name,
                        permissions = user.permissions
                    )
                )
                
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
                val request = call.receive<RefreshTokenRequest>()
                
                if (request.refreshToken.isEmpty()) {
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
                    verifier.verify(request.refreshToken)
                } catch (e: Exception) {
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
                val storedUserId = userRepository.getUserIdByRefreshToken(request.refreshToken)
                
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
                userRepository.revokeRefreshToken(request.refreshToken)
                
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
                
                val response = RefreshTokenResponse(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken
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
                    
                    // Получаем refresh token из тела запроса (если есть)
                    val refreshToken = try {
                        val request = call.receiveOrNull<RefreshTokenRequest>()
                        request?.refreshToken
                    } catch (e: Exception) {
                        null
                    }
                    
                    // Отзываем refresh token, если он передан
                    if (refreshToken != null) {
                        userRepository.revokeRefreshToken(refreshToken)
                        logger.info { "Refresh token revoked for user: $userId" }
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

