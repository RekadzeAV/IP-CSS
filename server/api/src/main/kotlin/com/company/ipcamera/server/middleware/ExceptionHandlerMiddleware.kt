package com.company.ipcamera.server.middleware

import com.company.ipcamera.server.dto.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import mu.KotlinLogging
import java.util.concurrent.TimeoutException

private val logger = KotlinLogging.logger {}

/**
 * Middleware для глобальной обработки исключений
 *
 * Обрабатывает различные типы исключений и возвращает соответствующие HTTP ответы
 */
fun Application.configureExceptionHandling() {
    install(StatusPages) {
        // Обработка исключений валидации
        exception<IllegalArgumentException> { call, cause ->
            logger.warn(cause) { "Validation error: ${cause.message}" }
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = cause.message ?: "Invalid request"
                )
            )
        }

        // Обработка исключений состояния (например, операция не может быть выполнена в текущем состоянии)
        exception<IllegalStateException> { call, cause ->
            logger.warn(cause) { "Illegal state error: ${cause.message}" }
            call.respond(
                HttpStatusCode.Conflict,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = cause.message ?: "Operation cannot be performed in current state"
                )
            )
        }

        // Обработка исключений безопасности
        exception<SecurityException> { call, cause ->
            logger.warn(cause) { "Security error: ${cause.message}" }
            call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = cause.message ?: "Access denied"
                )
            )
        }

        // Обработка исключений аутентификации
        exception<AuthenticationException> { call, cause ->
            logger.warn(cause) { "Authentication error: ${cause.message}" }
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = cause.message ?: "Authentication required"
                )
            )
        }

        // Обработка исключений таймаута
        exception<TimeoutException> { call, cause ->
            logger.error(cause) { "Timeout error: ${cause.message}" }
            call.respond(
                HttpStatusCode.RequestTimeout,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = "Request timeout: ${cause.message ?: "Operation took too long"}"
                )
            )
        }

        // Обработка исключений "не найдено"
        exception<NotFoundException> { call, cause ->
            logger.debug(cause) { "Resource not found: ${cause.message}" }
            call.respond(
                HttpStatusCode.NotFound,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = cause.message ?: "Resource not found"
                )
            )
        }

        // Обработка исключений "недоступен"
        exception<ServiceUnavailableException> { call, cause ->
            logger.warn(cause) { "Service unavailable: ${cause.message}" }
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = cause.message ?: "Service temporarily unavailable"
                )
            )
        }

        // Обработка всех остальных исключений
        exception<Exception> { call, cause ->
            logger.error(cause) {
                "Unhandled exception: ${cause.javaClass.simpleName} - ${cause.message}"
            }

            // В production не раскрываем детали ошибки
            val isProduction = System.getenv("ENVIRONMENT") == "production"
            val errorMessage = if (isProduction) {
                "Internal server error"
            } else {
                cause.message ?: "Internal server error: ${cause.javaClass.simpleName}"
            }

            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = errorMessage
                )
            )
        }
    }
}

/**
 * Класс для исключений аутентификации
 */
class AuthenticationException(message: String) : Exception(message)

/**
 * Класс для исключений "не найдено"
 */
class NotFoundException(message: String) : Exception(message)

/**
 * Класс для исключений "недоступен"
 */
class ServiceUnavailableException(message: String) : Exception(message)
