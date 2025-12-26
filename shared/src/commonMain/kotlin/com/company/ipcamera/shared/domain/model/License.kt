package com.company.ipcamera.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Модель лицензии (упрощенная для доменного слоя)
 * Полная реализация находится в core.license.LicenseManager
 */
@Serializable
data class License(
    val id: String,
    val licenseKey: String,
    val type: LicenseType,
    val status: LicenseStatus,
    val features: List<String> = emptyList(),
    val maxCameras: Int? = null,
    val maxUsers: Int? = null,
    val expiresAt: Long? = null,
    val activatedAt: Long? = null,
    val deviceId: String? = null,
    val isValid: Boolean
) {
    /**
     * Проверка, истекла ли лицензия
     */
    fun isExpired(): Boolean {
        return expiresAt?.let { it < System.currentTimeMillis() } ?: false
    }
    
    /**
     * Проверка, активна ли лицензия
     */
    fun isActive(): Boolean = isValid && !isExpired() && status == LicenseStatus.ACTIVE
    
    /**
     * Проверка, доступна ли функция
     */
    fun hasFeature(feature: String): Boolean = features.contains(feature)
}

@Serializable
enum class LicenseType {
    TRIAL,
    BASIC,
    PROFESSIONAL,
    ENTERPRISE
}

@Serializable
enum class LicenseStatus {
    ACTIVE,
    INACTIVE,
    EXPIRED,
    REVOKED,
    PENDING
}
