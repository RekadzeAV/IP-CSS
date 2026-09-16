package com.company.ipcamera.server.middleware

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*

/**
 * Middleware для чтения JWT токена из cookie и установки его в заголовок Authorization
 * Это позволяет JWT аутентификации работать с httpOnly cookies
 */
fun Application.configureCookieAuth() {
    intercept(ApplicationCallPipeline.Call) {
        val call = context
        val tokenFromCookie = call.request.cookies["access_token"]

        // Если токен есть в cookie, но нет в заголовке Authorization — передаём через атрибуты (request в Ktor неизменяем)
        if (tokenFromCookie != null && call.request.headers["Authorization"] == null) {
            call.attributes.put(cookieBearerKey, "Bearer $tokenFromCookie")
        }
        proceed()
    }
}

private val cookieBearerKey = io.ktor.util.AttributeKey<String>("CookieBearerToken")

/**
 * Extension функция для получения JWT токена из атрибутов (cookie), cookie или заголовка Authorization
 */
fun ApplicationCall.getJwtToken(): String? {
    // Из атрибута (установлено middleware из cookie)
    runCatching { attributes.get(cookieBearerKey) }.getOrNull()?.let { bearer -> return bearer.removePrefix("Bearer ").trim() }
    // Cookie
    val tokenFromCookie = request.cookies["access_token"]
    if (tokenFromCookie != null) return tokenFromCookie
    // Заголовок Authorization
    val authHeader = request.headers["Authorization"]
    if (authHeader != null && authHeader.startsWith("Bearer ")) return authHeader.removePrefix("Bearer ").trim()
    return null
}

