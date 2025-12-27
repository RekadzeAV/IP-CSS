package com.company.ipcamera.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Базовый ответ API
 */
@Serializable
data class ApiResponse<T>(
    val data: T? = null,
    val error: ApiError? = null,
    val success: Boolean = true
)

/**
 * Ошибка API
 */
@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val timestamp: String? = null
)

/**
 * Пагинация
 */
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)



