package com.company.ipcamera.server.middleware

import com.company.ipcamera.shared.domain.model.UserRole
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

/**
 * Middleware для проверки прав доступа
 */
object AuthorizationMiddleware {
    /**
     * Проверяет, имеет ли пользователь требуемую роль
     */
    suspend fun PipelineContext<Unit, ApplicationCall>.requireRole(
        requiredRole: UserRole,
        vararg additionalRoles: UserRole
    ) {
        val principal = call.principal<JWTPrincipal>()
        
        if (principal == null) {
            call.respond(io.ktor.http.HttpStatusCode.Unauthorized)
            return
        }
        
        val userRoleString = principal.payload.getClaim("role").asString()
        val userRole = try {
            UserRole.valueOf(userRoleString.uppercase())
        } catch (e: Exception) {
            call.respond(io.ktor.http.HttpStatusCode.Forbidden)
            return
        }
        
        val allowedRoles = listOf(requiredRole) + additionalRoles.toList()
        
        if (!allowedRoles.contains(userRole)) {
            call.respond(io.ktor.http.HttpStatusCode.Forbidden)
            return
        }
    }
    
    /**
     * Проверяет, является ли пользователь администратором
     */
    suspend fun PipelineContext<Unit, ApplicationCall>.requireAdmin() {
        requireRole(UserRole.ADMIN)
    }
    
    /**
     * Проверяет, имеет ли пользователь требуемое разрешение
     */
    suspend fun PipelineContext<Unit, ApplicationCall>.requirePermission(
        permission: String
    ) {
        val principal = call.principal<JWTPrincipal>()
        
        if (principal == null) {
            call.respond(io.ktor.http.HttpStatusCode.Unauthorized)
            return
        }
        
        val permissions = principal.payload.getClaim("permissions")
            .asList(String::class.java) ?: emptyList()
        
        if (!permissions.contains(permission)) {
            call.respond(io.ktor.http.HttpStatusCode.Forbidden)
            return
        }
    }
}

