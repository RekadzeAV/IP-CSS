package com.company.ipcamera.server.middleware

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

/**
 * Middleware для чтения JWT токена из cookie и установки его в заголовок Authorization
 * Это позволяет JWT аутентификации работать с httpOnly cookies
 */
fun Application.configureCookieAuth() {
    intercept(ApplicationCallPipeline.Authentication) {
        val tokenFromCookie = call.request.cookies["access_token"]

        // Если токен есть в cookie, но нет в заголовке Authorization, устанавливаем его
        if (tokenFromCookie != null && call.request.headers["Authorization"] == null) {
            // Устанавливаем заголовок Authorization для JWT аутентификации
            call.request.headers.append("Authorization", "Bearer $tokenFromCookie")
        }
    }
}

/**
 * Extension функция для получения JWT токена из cookie или заголовка Authorization
 */
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

