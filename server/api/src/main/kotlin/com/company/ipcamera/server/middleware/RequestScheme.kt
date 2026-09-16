package com.company.ipcamera.server.middleware

import io.ktor.server.application.ApplicationCall

/**
 * Эффективная схема запроса: учитывает [X-Forwarded-Proto] за reverse proxy (nginx, Traefik),
 * иначе [io.ktor.server.request.local] схему соединения с Ktor.
 */
fun ApplicationCall.effectiveRequestScheme(): String {
    val forwarded = request.headers["X-Forwarded-Proto"]?.trim()?.lowercase()
    if (!forwarded.isNullOrBlank()) return forwarded
    return request.local.scheme.lowercase()
}
