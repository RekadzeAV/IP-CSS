package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireAdmin
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireRole
import com.company.ipcamera.server.middleware.validateRequest
import com.company.ipcamera.server.repository.ServerUserRepository
import com.company.ipcamera.server.security.SecurityLogger
import com.company.ipcamera.server.validation.RequestValidator
import com.company.ipcamera.shared.domain.model.UserRole
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
 * Маршруты для управления пользователями
 * Все маршруты требуют JWT аутентификации
 * Большинство операций требуют прав администратора
 */
fun Route.userRoutes() {
    val userRepository: ServerUserRepository by inject()

    authenticate("jwt-auth") {
        route("/users") {
            // GET /api/v1/users/me - получение текущего пользователя
            get("/me") {
                try {
                    val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                    val userId = principal?.payload?.subject
                        ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<UserDto>(
                                success = false,
                                data = null,
                                message = "User not authenticated"
                            )
                        )

                    val user = userRepository.getUserById(userId)
                    if (user != null) {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = user.toDto(),
                                message = "User retrieved successfully"
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<UserDto>(
                                success = false,
                                data = null,
                                message = "User not found"
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error retrieving current user" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<UserDto>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/users - список пользователей (только для администраторов)
            get {
                requireAdmin()

                try {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                    val roleStr = call.request.queryParameters["role"]
                    val role = roleStr?.let {
                        try { UserRole.valueOf(it.uppercase()) }
                        catch (e: Exception) { null }
                    }

                    // Используем метод с пагинацией из репозитория
                    val paginatedResult = userRepository.getUsers(
                        page = page,
                        limit = limit,
                        role = role
                    )

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = paginatedResult.toDto(),
                            message = "Users retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error retrieving users" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<PaginatedUserResponse>(
                            success = false,
                            data = null,
                            message = "Error retrieving users: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/users - создание нового пользователя (только для администраторов)
            post {
                requireAdmin()

                try {
                    val request = call.receive<CreateUserRequest>()

                    // Валидация запроса
                    if (!validateRequest(request) { RequestValidator.validateCreateUserRequest(it) }) {
                        return@post
                    }

                    val role = try {
                        UserRole.valueOf(request.role.uppercase())
                    } catch (e: Exception) {
                        UserRole.VIEWER
                    }

                    val user = userRepository.createUser(
                        username = request.username,
                        email = request.email,
                        password = request.password,
                        fullName = request.fullName,
                        role = role
                    )

                    val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                    val createdBy = principal?.payload?.subject ?: "unknown"
                    SecurityLogger.logUserCreated(createdBy, user.id, user.username)
                    logger.info { "User created: ${user.username} (id: ${user.id}) by admin" }

                    call.respond(
                        HttpStatusCode.Created,
                        ApiResponse(
                            success = true,
                            data = user.toDto(),
                            message = "User created successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error creating user" }
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<UserDto>(
                            success = false,
                            data = null,
                            message = "Error creating user: ${e.message}"
                        )
                    )
                }
            }

            route("/{id}") {
                // GET /api/v1/users/{id} - получение пользователя по ID (только для администраторов)
                get {
                    requireAdmin()

                    try {
                        val id = call.parameters["id"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<UserDto>(
                                success = false,
                                data = null,
                                message = "User ID is required"
                            )
                        )

                        val user = userRepository.getUserById(id)
                        if (user != null) {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = user.toDto(),
                                    message = "User retrieved successfully"
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<UserDto>(
                                    success = false,
                                    data = null,
                                    message = "User not found"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Error retrieving user" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<UserDto>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }

                // PUT /api/v1/users/{id} - обновление пользователя (только для администраторов)
                put {
                    requireAdmin()

                    try {
                        val id = call.parameters["id"] ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<UserDto>(
                                success = false,
                                data = null,
                                message = "User ID is required"
                            )
                        )

                        val request = call.receive<UpdateUserRequest>()

                        // Валидация запроса
                        if (!validateRequest(request) { RequestValidator.validateUpdateUserRequest(it) }) {
                            return@put
                        }

                        val existingUser = userRepository.getUserById(id)

                        if (existingUser == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<UserDto>(
                                    success = false,
                                    data = null,
                                    message = "User not found"
                                )
                            )
                            return@put
                        }

                        val role = request.role?.let {
                            try { UserRole.valueOf(it.uppercase()) }
                            catch (e: Exception) { null }
                        } ?: existingUser.role

                        val updatedUser = existingUser.copy(
                            email = request.email ?: existingUser.email,
                            fullName = request.fullName ?: existingUser.fullName,
                            role = role,
                            isActive = request.isActive ?: existingUser.isActive
                        )

                        val result = userRepository.updateUser(updatedUser)
                        result.fold(
                            onSuccess = { user ->
                                logger.info { "User updated: $id by admin" }
                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse(
                                        success = true,
                                        data = user.toDto(),
                                        message = "User updated successfully"
                                    )
                                )
                            },
                            onFailure = { error ->
                                logger.error(error) { "Error updating user: $id" }
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<UserDto>(
                                        success = false,
                                        data = null,
                                        message = "Error updating user: ${error.message}"
                                    )
                                )
                            }
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Error updating user" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<UserDto>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }

                // DELETE /api/v1/users/{id} - удаление пользователя (только для администраторов)
                delete {
                    requireAdmin()

                    try {
                        val id = call.parameters["id"] ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "User ID is required"
                            )
                        )

                        val existingUser = userRepository.getUserById(id)
                        if (existingUser == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Unit>(
                                    success = false,
                                    data = null,
                                    message = "User not found"
                                )
                            )
                            return@delete
                        }

                        val result = userRepository.deleteUser(id)
                        result.fold(
                            onSuccess = {
                                val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                                val deletedBy = principal?.payload?.subject ?: "unknown"
                                SecurityLogger.logUserDeleted(deletedBy, id, existingUser.username)
                                logger.info { "User deleted: $id by admin" }
                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse<Unit>(
                                        success = true,
                                        data = null,
                                        message = "User deleted successfully"
                                    )
                                )
                            },
                            onFailure = { error ->
                                logger.error(error) { "Error deleting user: $id" }
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<Unit>(
                                        success = false,
                                        data = null,
                                        message = "Error deleting user: ${error.message}"
                                    )
                                )
                            }
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Error deleting user" }
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
}

