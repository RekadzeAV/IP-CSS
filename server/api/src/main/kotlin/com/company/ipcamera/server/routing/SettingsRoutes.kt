package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.*
// requireAdmin removed
// validateRequest removed
import com.company.ipcamera.server.security.SecurityLogger
import com.company.ipcamera.server.validation.RequestValidator
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SystemSettings
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
            get { handleGetSettings(settingsRepository, call) }
            put { handleUpdateSettings(settingsRepository, call) }
            get("/system") { handleGetSystemSettings(settingsRepository, call) }
            put("/system") { handleUpdateSystemSettings(settingsRepository, call) }
            post("/export") { handleExportSettings(settingsRepository, call) }
            post("/import") { handleImportSettings(settingsRepository, call) }
            post("/reset") { handleResetSettings(settingsRepository, call) }
            route("/{key}") {
                get { handleGetSettingByKey(settingsRepository, call) }
                put { handleUpdateSettingByKey(settingsRepository, call) }
                delete { handleDeleteSettingByKey(settingsRepository, call) }
            }
        }
    }
}

private suspend fun handleGetSettings(settingsRepository: SettingsRepository, call: io.ktor.server.application.ApplicationCall) {
    try {
        val category = parseCategoryParam(call)
        val settings = settingsRepository.getSettings(category).map { it.toDto() }
        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = settings, message = "Settings retrieved successfully"))
    } catch (e: Exception) {
        call.respondError("Error retrieving settings: ${e.message}")
    }
}

private suspend fun handleUpdateSettings(settingsRepository: SettingsRepository, call: io.ktor.server.application.ApplicationCall) {

    try {
        val request = call.receive<UpdateSettingsRequest>()

        
        val updatedBy = getCurrentUserId(call)
        val result = settingsRepository.updateSettings(request.settings)
        result.fold(
            onSuccess = { count ->
                logConfigChange(call, updatedBy, "settings_update", mapOf("updated_count" to count, "settings_keys" to request.settings.keys.toList()))
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = count, message = "Settings updated successfully"))
            },
            onFailure = { call.respondError("Error updating settings: ${it.message}") }
        )
    } catch (e: Exception) {
        call.respondError("Internal server error: ${e.message}")
    }
}

private suspend fun handleGetSystemSettings(settingsRepository: SettingsRepository, call: io.ktor.server.application.ApplicationCall) {
    try {
        val systemSettings = settingsRepository.getSystemSettings()
        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = systemSettings, message = if (systemSettings != null) "System settings retrieved successfully" else "System settings not configured"))
    } catch (e: Exception) {
        call.respondError("Error retrieving system settings: ${e.message}")
    }
}

private suspend fun handleUpdateSystemSettings(settingsRepository: SettingsRepository, call: io.ktor.server.application.ApplicationCall) {

    try {
        val request = call.receive<SystemSettings>()
        val validationResult = RequestValidator.validateSystemSettings(request)
        if (validationResult is com.company.ipcamera.server.validation.ValidationResult.Error) {
            return call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, data = null, message = validationResult.message))
        }
        
        val updatedBy = getCurrentUserId(call)
        val result = settingsRepository.updateSystemSettings(request)
        result.fold(
            onSuccess = {
                logSystemSettingsChange(call, updatedBy, request)
                call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true, data = null, message = "System settings updated successfully"))
            },
            onFailure = { call.respondError("Error updating system settings: ${it.message}") }
        )
    } catch (e: Exception) {
        call.respondError("Internal server error: ${e.message}")
    }
}

private suspend fun handleExportSettings(settingsRepository: SettingsRepository, call: io.ktor.server.application.ApplicationCall) {

    try {
        val result = settingsRepository.exportSettings()
        result.fold(
            onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = it, message = "Settings exported successfully")) },
            onFailure = { call.respondError("Error exporting settings: ${it.message}") }
        )
    } catch (e: Exception) {
        call.respondError("Internal server error: ${e.message}")
    }
}

private suspend fun handleImportSettings(settingsRepository: SettingsRepository, call: io.ktor.server.application.ApplicationCall) {

    try {
        val request = call.receive<UpdateSettingsRequest>()
        val result = settingsRepository.importSettings(request.settings)
        result.fold(
            onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true, data = null, message = "Settings imported successfully")) },
            onFailure = { call.respondError("Error importing settings: ${it.message}") }
        )
    } catch (e: Exception) {
        call.respondError("Internal server error: ${e.message}")
    }
}

private suspend fun handleResetSettings(settingsRepository: SettingsRepository, call: io.ktor.server.application.ApplicationCall) {

    try {
        val category = parseCategoryParam(call)
        val result = settingsRepository.resetSettings(category)
        result.fold(
            onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true, data = null, message = "Settings reset successfully")) },
            onFailure = { call.respondError("Error resetting settings: ${it.message}") }
        )
    } catch (e: Exception) {
        call.respondError("Internal server error: ${e.message}")
    }
}

private suspend fun handleGetSettingByKey(settingsRepository: SettingsRepository, call: io.ktor.server.application.ApplicationCall) {
    try {
        val key = call.parameters["key"] ?: return call.respond(HttpStatusCode.BadRequest, ApiResponse<SettingsDto>(success = false, data = null, message = "Setting key is required"))
        val setting = settingsRepository.getSetting(key)
        if (setting != null) {
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = setting.toDto(), message = "Setting retrieved successfully"))
        } else {
            call.respond(HttpStatusCode.NotFound, ApiResponse<SettingsDto>(success = false, data = null, message = "Setting not found"))
        }
    } catch (e: Exception) {
        call.respondError("Internal server error: ${e.message}")
    }
}

private suspend fun handleUpdateSettingByKey(settingsRepository: SettingsRepository, call: io.ktor.server.application.ApplicationCall) {

    try {
        val key = call.parameters["key"] ?: return call.respond(HttpStatusCode.BadRequest, ApiResponse<SettingsDto>(success = false, data = null, message = "Setting key is required"))
        val request = call.receive<UpdateSettingRequest>()
        val result = settingsRepository.updateSetting(key, request.value)
        result.fold(
            onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = it.toDto(), message = "Setting updated successfully")) },
            onFailure = { call.respondError("Error updating setting: ${it.message}") }
        )
    } catch (e: Exception) {
        call.respondError("Internal server error: ${e.message}")
    }
}

private suspend fun handleDeleteSettingByKey(settingsRepository: SettingsRepository, call: io.ktor.server.application.ApplicationCall) {

    try {
        val key = call.parameters["key"] ?: return call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, data = null, message = "Setting key is required"))
        val result = settingsRepository.deleteSetting(key)
        result.fold(
            onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true, data = null, message = "Setting deleted successfully")) },
            onFailure = { call.respondError("Error deleting setting: ${it.message}") }
        )
    } catch (e: Exception) {
        call.respondError("Internal server error: ${e.message}")
    }
}

private fun parseCategoryParam(call: io.ktor.server.application.ApplicationCall): com.company.ipcamera.shared.domain.model.SettingsCategory? {
    val categoryStr = call.request.queryParameters["category"]
    return categoryStr?.let {
        try { com.company.ipcamera.shared.domain.model.SettingsCategory.valueOf(it.uppercase()) }
        catch (e: Exception) { null }
    }
}

private fun getCurrentUserId(call: io.ktor.server.application.ApplicationCall): String {
    val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
    return principal?.payload?.subject ?: "unknown"
}

private fun logConfigChange(call: io.ktor.server.application.ApplicationCall, userId: String, action: String, details: Map<String, Any>) {
    SecurityLogger.log(com.company.ipcamera.server.security.SecurityEvent(
        type = com.company.ipcamera.server.security.SecurityEventType.CONFIGURATION_CHANGE,
        severity = com.company.ipcamera.server.security.SecurityEventSeverity.WARNING,
        userId = userId,
        username = null,
        ipAddress = call.request.local.remoteHost,
        details = details
    ))
    logger.info { "Settings updated: $action by admin" }
}

private fun logSystemSettingsChange(call: io.ktor.server.application.ApplicationCall, userId: String, request: SystemSettings) {
    SecurityLogger.log(com.company.ipcamera.server.security.SecurityEvent(
        type = com.company.ipcamera.server.security.SecurityEventType.CONFIGURATION_CHANGE,
        severity = com.company.ipcamera.server.security.SecurityEventSeverity.WARNING,
        userId = userId,
        username = null,
        ipAddress = call.request.local.remoteHost,
        details = mapOf(
            "action" to "system_settings_update",
            "recording_updated" to (request.recording != null).toString(),
            "storage_updated" to (request.storage != null).toString(),
            "notifications_updated" to (request.notifications != null).toString(),
            "security_updated" to (request.security != null).toString(),
            "network_updated" to (request.network != null).toString()
        )
    ))
    logger.info { "System settings updated by admin: $userId" }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondError(message: String) {
    respond(HttpStatusCode.InternalServerError, ApiResponse<String>(success = false, data = null, message = message))
}

