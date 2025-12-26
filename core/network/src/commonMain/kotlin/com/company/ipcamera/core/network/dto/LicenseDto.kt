package com.company.ipcamera.core.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO для лицензий
 */
@Serializable
data class LicenseResponse(
    val id: String,
    val licenseKey: String,
    val type: String,
    val status: String,
    val features: List<String> = emptyList(),
    val maxCameras: Int? = null,
    val maxUsers: Int? = null,
    val expiresAt: Long? = null,
    val activatedAt: Long? = null,
    val deviceId: String? = null,
    val isValid: Boolean
)

@Serializable
data class ActivateLicenseRequest(
    val licenseKey: String,
    val deviceId: String? = null
)

@Serializable
data class ActivateLicenseResponse(
    val success: Boolean,
    val license: LicenseResponse? = null,
    val message: String? = null
)

@Serializable
data class ValidateLicenseResponse(
    val isValid: Boolean,
    val license: LicenseResponse? = null,
    val error: String? = null
)


