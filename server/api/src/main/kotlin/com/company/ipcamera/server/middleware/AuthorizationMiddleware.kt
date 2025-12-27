package com.company.ipcamera.server.middleware

import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.shared.domain.model.UserRole
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Middleware для проверки прав доступа (RBAC)
 *
 * Поддерживает:
 * - Проверку ролей (ADMIN, OPERATOR, VIEWER, GUEST)
 * - Проверку разрешений (permissions)
 * - Wildcard разрешения ("*" для всех разрешений)
 */
object AuthorizationMiddleware {
    /**
     * Проверяет, имеет ли пользователь требуемую роль
     * @param requiredRole Минимальная требуемая роль
     * @param additionalRoles Дополнительные разрешенные роли
     */
    suspend fun PipelineContext<Unit, ApplicationCall>.requireRole(
        requiredRole: UserRole,
        vararg additionalRoles: UserRole
    ) {
        val principal = call.principal<JWTPrincipal>()

        if (principal == null) {
            logger.warn { "Unauthorized access attempt to ${call.request.path()}" }
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = "Authentication required"
                )
            )
            return
        }

        val userRoleString = principal.payload.getClaim("role")?.asString()
        if (userRoleString == null) {
            logger.warn { "User token missing role claim" }
            call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = "Access denied: invalid token"
                )
            )
            return
        }

        val userRole = try {
            UserRole.valueOf(userRoleString.uppercase())
        } catch (e: Exception) {
            logger.warn { "Invalid role in token: $userRoleString" }
            call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = "Access denied: invalid role"
                )
            )
            return
        }

        // Иерархия ролей: ADMIN > OPERATOR > VIEWER > GUEST
        val roleHierarchy = mapOf(
            UserRole.ADMIN to 4,
            UserRole.OPERATOR to 3,
            UserRole.VIEWER to 2,
            UserRole.GUEST to 1
        )

        val allowedRoles = listOf(requiredRole) + additionalRoles.toList()
        val userRoleLevel = roleHierarchy[userRole] ?: 0
        val requiredRoleLevel = roleHierarchy[requiredRole] ?: 0

        // Проверяем точное совпадение или иерархию (пользователь с более высокой ролью имеет доступ)
        val hasAccess = allowedRoles.contains(userRole) || userRoleLevel >= requiredRoleLevel

        if (!hasAccess) {
            val userId = principal.payload.subject
            logger.warn { "Access denied for user $userId (role: $userRole) to ${call.request.path()} (required: $requiredRole)" }
            call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = "Access denied: insufficient privileges"
                )
            )
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
     * Поддерживает wildcard разрешения ("*" для всех разрешений)
     * @param permission Требуемое разрешение (например, "cameras:delete", "users:create")
     */
    suspend fun PipelineContext<Unit, ApplicationCall>.requirePermission(
        permission: String
    ) {
        val principal = call.principal<JWTPrincipal>()

        if (principal == null) {
            logger.warn { "Unauthorized access attempt to ${call.request.path()}" }
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = "Authentication required"
                )
            )
            return
        }

        val permissions = principal.payload.getClaim("permissions")
            ?.asList(String::class.java) ?: emptyList()

        // Проверяем wildcard разрешение ("*" дает все права)
        val hasWildcard = permissions.contains("*")

        // Проверяем точное разрешение или wildcard
        val hasPermission = hasWildcard || permissions.contains(permission)

        if (!hasPermission) {
            val userId = principal.payload.subject
            logger.warn { "Permission denied for user $userId: missing permission '$permission' for ${call.request.path()}" }
            call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = "Access denied: missing permission '$permission'"
                )
            )
            return
        }
    }

    /**
     * Проверяет, имеет ли пользователь хотя бы одно из разрешений
     */
    suspend fun PipelineContext<Unit, ApplicationCall>.requireAnyPermission(
        vararg permissions: String
    ) {
        val principal = call.principal<JWTPrincipal>()

        if (principal == null) {
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = "Authentication required"
                )
            )
            return
        }

        val userPermissions = principal.payload.getClaim("permissions")
            ?.asList(String::class.java) ?: emptyList()

        // Проверяем wildcard
        if (userPermissions.contains("*")) {
            return
        }

        // Проверяем, есть ли хотя бы одно разрешение
        val hasAnyPermission = permissions.any { userPermissions.contains(it) }

        if (!hasAnyPermission) {
            val userId = principal.payload.subject
            logger.warn { "Permission denied for user $userId: missing any of permissions ${permissions.joinToString()} for ${call.request.path()}" }
            call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse<Unit>(
                    success = false,
                    data = null,
                    message = "Access denied: missing required permissions"
                )
            )
            return
        }
    }
}

