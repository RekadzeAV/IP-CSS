package com.company.ipcamera.server.middleware

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.validation.RequestValidator
import com.company.ipcamera.server.validation.ValidationResult
import io.ktor.http.*
import io.ktor.server.application.*
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


