package com.company.ipcamera.server.middleware

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.security.SecurityLogger
import com.company.ipcamera.server.validation.RequestValidator
import com.company.ipcamera.server.validation.ValidationResult
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Extension функция для валидации запроса
 * @param validator функция валидации
 * @return true, если валидация прошла успешно, false если была ошибка
 */
suspend fun <T> PipelineContext<Unit, ApplicationCall>.validateRequest(
    request: T,
    validator: (T) -> ValidationResult
): Boolean {
    val validationResult = validator(request)
    return when (validationResult) {
        is ValidationResult.Success -> true
        is ValidationResult.Error -> {
            logValidationFailure(call, request, validationResult)
            logger.warn { "Validation failed for request: ${validationResult.message} (field: ${validationResult.field})" }
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = validationResult.message
                )
            )
            false
        }
    }
}

/**
 * Валидация параметров пагинации из query parameters
 */
suspend fun PipelineContext<Unit, ApplicationCall>.validatePagination(): Pair<Int, Int>? {
    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

    val validationResult = RequestValidator.validatePagination(page, limit)
    return when (validationResult) {
        is ValidationResult.Success -> Pair(page, limit)
        is ValidationResult.Error -> {
            logValidationFailure(call, null, validationResult)
            logger.warn { "Pagination validation failed: ${validationResult.message}" }
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = validationResult.message
                )
            )
            null
        }
    }
}

/**
 * Валидация фильтров из query parameters
 *
 * @param validTypes Список допустимых типов (опционально)
 * @param validPriorities Список допустимых приоритетов (опционально)
 * @param validStatuses Список допустимых статусов (опционально)
 * @return Triple(type, priority, status) или null при ошибке валидации
 */
suspend fun PipelineContext<Unit, ApplicationCall>.validateFilters(
    validTypes: List<String>? = null,
    validPriorities: List<String>? = null,
    validStatuses: List<String>? = null
): Triple<String?, String?, String?>? {
    val type = call.request.queryParameters["type"]
    val priority = call.request.queryParameters["priority"]
    val status = call.request.queryParameters["status"]

    val validationResult = RequestValidator.validateFilters(type, priority, status, validTypes, validPriorities, validStatuses)
    return when (validationResult) {
        is ValidationResult.Success -> Triple(type, priority, status)
        is ValidationResult.Error -> {
            logValidationFailure(call, null, validationResult)
            logger.warn { "Filter validation failed: ${validationResult.message}" }
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = validationResult.message
                )
            )
            null
        }
    }
}

/**
 * Валидация date range параметров из query parameters
 *
 * @return Pair(startDate, endDate) в миллисекундах или null при ошибке валидации
 */
suspend fun PipelineContext<Unit, ApplicationCall>.validateDateRange(): Pair<Long?, Long?>? {
    val startDateStr = call.request.queryParameters["startDate"]
    val endDateStr = call.request.queryParameters["endDate"]

    val validationResult = RequestValidator.validateDateRange(startDateStr, endDateStr)
    return when (validationResult) {
        is ValidationResult.Success -> {
            val startDate = startDateStr?.toLongOrNull()
            val endDate = endDateStr?.toLongOrNull()
            Pair(startDate, endDate)
        }
        is ValidationResult.Error -> {
            logValidationFailure(call, null, validationResult)
            logger.warn { "Date range validation failed: ${validationResult.message}" }
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = validationResult.message
                )
            )
            null
        }
    }
}

/**
 * Валидация сортировки из query parameters
 *
 * @param validFields Список допустимых полей для сортировки
 * @param defaultField Поле по умолчанию
 * @param defaultOrder Порядок по умолчанию (asc/desc)
 * @return Pair(field, order) или null при ошибке валидации
 */
suspend fun PipelineContext<Unit, ApplicationCall>.validateSorting(
    validFields: List<String>,
    defaultField: String = "createdAt",
    defaultOrder: String = "desc"
): Pair<String, String>? {
    val sortBy = call.request.queryParameters["sortBy"] ?: defaultField
    val sortOrder = call.request.queryParameters["sortOrder"] ?: defaultOrder

    val validationResult = RequestValidator.validateSorting(sortBy, sortOrder, validFields)
    return when (validationResult) {
        is ValidationResult.Success -> Pair(sortBy, sortOrder)
        is ValidationResult.Error -> {
            logValidationFailure(call, null, validationResult)
            logger.warn { "Sorting validation failed: ${validationResult.message}" }
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = validationResult.message
                )
            )
            null
        }
    }
}

private fun logValidationFailure(call: ApplicationCall, request: Any?, result: ValidationResult.Error) {
    val principal = call.principal<JWTPrincipal>()
    val userId = principal?.payload?.subject
    val username = principal?.payload?.getClaim("username")?.asString()
    val ipAddress = call.request.local.remoteHost
    val field = result.field ?: "unknown"
    val requestValue = extractValidationValue(request, field)
    SecurityLogger.logInputValidationFailed(
        ipAddress = ipAddress,
        userId = userId,
        username = username,
        field = field,
        reason = result.message,
        value = requestValue ?: call.request.uri.take(200)
    )
}

private fun extractValidationValue(request: Any?, field: String): String? {
    return when (request) {
        is CreateCameraRequest -> when (field) {
            "url" -> request.url
            "name" -> request.name
            "username" -> request.username
            "password" -> maskSensitive(request.password)
            else -> null
        }
        is UpdateCameraRequest -> when (field) {
            "url" -> request.url
            "name" -> request.name
            "username" -> request.username
            "password" -> maskSensitive(request.password)
            else -> null
        }
        else -> null
    }?.take(200)
}

private fun maskSensitive(value: String?): String? =
    value?.takeIf { it.isNotBlank() }?.let { "***redacted***" }


