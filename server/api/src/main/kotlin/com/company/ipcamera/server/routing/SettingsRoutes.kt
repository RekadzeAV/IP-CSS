package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.middleware.requireAdmin
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.repository.SettingsRepository
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
 * Маршруты для управления настройками
 * Все маршруты требуют JWT аутентификации
 * Большинство операций требуют прав администратора
 */
fun Route.settingsRoutes() {
    val settingsRepository: SettingsRepository by inject()
    
    authenticate("jwt-auth") {
        route("/settings") {
            // GET /api/v1/settings - получение всех настроек
            get {
                try {
                    val categoryStr = call.request.queryParameters["category"]
                    val category = categoryStr?.let { 
                        try { SettingsCategory.valueOf(it.uppercase()) } 
                        catch (e: Exception) { null }
                    }
                    
                    val settings = settingsRepository.getSettings(category)
                    val settingsDto = settings.map { it.toDto() }
                    
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = settingsDto,
                            message = "Settings retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error retrieving settings" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<List<SettingsDto>>(
                            success = false,
                            data = null,
                            message = "Error retrieving settings: ${e.message}"
                        )
                    )
                }
            }
            
            // PUT /api/v1/settings - обновление настроек (только для администраторов)
            put {
                requireAdmin()
                
                try {
                    val request = call.receive<UpdateSettingsRequest>()
                    
                    val result = settingsRepository.updateSettings(request.settings)
                    result.fold(
                        onSuccess = { updatedCount ->
                            logger.info { "Settings updated: $updatedCount settings by admin" }
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = updatedCount,
                                    message = "Settings updated successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Error updating settings" }
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Int>(
                                    success = false,
                                    data = null,
                                    message = "Error updating settings: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error updating settings" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Int>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }
            
            // GET /api/v1/settings/system - получение системных настроек
            get("/system") {
                try {
                    val systemSettings = settingsRepository.getSystemSettings()
                    if (systemSettings != null) {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = systemSettings,
                                message = "System settings retrieved successfully"
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse<SystemSettings>(
                                success = true,
                                data = null,
                                message = "System settings not configured"
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error retrieving system settings" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<SystemSettings>(
                            success = false,
                            data = null,
                            message = "Error retrieving system settings: ${e.message}"
                        )
                    )
                }
            }
            
            // POST /api/v1/settings/export - экспорт настроек (только для администраторов)
            post("/export") {
                requireAdmin()
                
                try {
                    val result = settingsRepository.exportSettings()
                    result.fold(
                        onSuccess = { exported ->
                            logger.info { "Settings exported by admin" }
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = exported,
                                    message = "Settings exported successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Error exporting settings" }
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Map<String, String>>(
                                    success = false,
                                    data = null,
                                    message = "Error exporting settings: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error exporting settings" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Map<String, String>>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }
            
            // POST /api/v1/settings/import - импорт настроек (только для администраторов)
            post("/import") {
                requireAdmin()
                
                try {
                    val request = call.receive<UpdateSettingsRequest>()
                    
                    val result = settingsRepository.importSettings(request.settings)
                    result.fold(
                        onSuccess = {
                            logger.info { "Settings imported by admin" }
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse<Unit>(
                                    success = true,
                                    data = null,
                                    message = "Settings imported successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Error importing settings" }
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(
                                    success = false,
                                    data = null,
                                    message = "Error importing settings: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error importing settings" }
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
            
            // POST /api/v1/settings/reset - сброс настроек (только для администраторов)
            post("/reset") {
                requireAdmin()
                
                try {
                    val categoryStr = call.request.queryParameters["category"]
                    val category = categoryStr?.let { 
                        try { SettingsCategory.valueOf(it.uppercase()) } 
                        catch (e: Exception) { null }
                    }
                    
                    val result = settingsRepository.resetSettings(category)
                    result.fold(
                        onSuccess = {
                            logger.info { "Settings reset by admin for category: ${category ?: "all"}" }
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse<Unit>(
                                    success = true,
                                    data = null,
                                    message = "Settings reset successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Error resetting settings" }
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(
                                    success = false,
                                    data = null,
                                    message = "Error resetting settings: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error resetting settings" }
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
            
            route("/{key}") {
                // GET /api/v1/settings/{key} - получение настройки по ключу
                get {
                    try {
                        val key = call.parameters["key"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<SettingsDto>(
                                success = false,
                                data = null,
                                message = "Setting key is required"
                            )
                        )
                        
                        val setting = settingsRepository.getSetting(key)
                        if (setting != null) {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = setting.toDto(),
                                    message = "Setting retrieved successfully"
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<SettingsDto>(
                                    success = false,
                                    data = null,
                                    message = "Setting not found"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Error retrieving setting" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<SettingsDto>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }
                
                // PUT /api/v1/settings/{key} - обновление настройки (только для администраторов)
                put {
                    requireAdmin()
                    
                    try {
                        val key = call.parameters["key"] ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<SettingsDto>(
                                success = false,
                                data = null,
                                message = "Setting key is required"
                            )
                        )
                        
                        val request = call.receive<UpdateSettingRequest>()
                        
                        val result = settingsRepository.updateSetting(key, request.value)
                        result.fold(
                            onSuccess = { setting ->
                                logger.info { "Setting updated: $key = ${request.value} by admin" }
                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse(
                                        success = true,
                                        data = setting.toDto(),
                                        message = "Setting updated successfully"
                                    )
                                )
                            },
                            onFailure = { error ->
                                logger.error(error) { "Error updating setting: $key" }
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<SettingsDto>(
                                        success = false,
                                        data = null,
                                        message = "Error updating setting: ${error.message}"
                                    )
                                )
                            }
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Error updating setting" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<SettingsDto>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }
                
                // DELETE /api/v1/settings/{key} - удаление настройки (только для администраторов)
                delete {
                    requireAdmin()
                    
                    try {
                        val key = call.parameters["key"] ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "Setting key is required"
                            )
                        )
                        
                        val result = settingsRepository.deleteSetting(key)
                        result.fold(
                            onSuccess = {
                                logger.info { "Setting deleted: $key by admin" }
                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse<Unit>(
                                        success = true,
                                        data = null,
                                        message = "Setting deleted successfully"
                                    )
                                )
                            },
                            onFailure = { error ->
                                logger.error(error) { "Error deleting setting: $key" }
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<Unit>(
                                        success = false,
                                        data = null,
                                        message = "Error deleting setting: ${error.message}"
                                    )
                                )
                            }
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Error deleting setting" }
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

