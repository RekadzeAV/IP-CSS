package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.license.LicenseManager
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.api.LicenseApiService
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.domain.model.License
import com.company.ipcamera.shared.domain.model.LicenseStatus
import com.company.ipcamera.shared.domain.model.LicenseType
import com.company.ipcamera.shared.domain.repository.LicenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация LicenseRepository с использованием API сервиса и LicenseManager
 */
class LicenseRepositoryImpl(
    private val licenseApiService: LicenseApiService,
    private val licenseManager: LicenseManager = LicenseManager.getInstance()
) : LicenseRepository {
    
    override suspend fun getLicense(): License? = withContext(Dispatchers.Default) {
        try {
            val result = licenseApiService.getLicense()
            result.fold(
                onSuccess = { dto -> dto.toDomain() },
                onError = { error ->
                    logger.error(error) { "Error getting license" }
                    null
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting license" }
            null
        }
    }
    
    override suspend fun activateLicense(licenseKey: String, deviceId: String?): Result<License> = withContext(Dispatchers.Default) {
        try {
            val request = ActivateLicenseRequest(licenseKey = licenseKey, deviceId = deviceId)
            val result = licenseApiService.activateLicense(request)
            result.fold(
                onSuccess = { response ->
                    response.license?.let {
                        Result.success(it.toDomain())
                    } ?: Result.failure(Exception(response.message ?: "License activation failed"))
                },
                onError = { error ->
                    logger.error(error) { "Error activating license" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error activating license" }
            Result.failure(e)
        }
    }
    
    override suspend fun validateLicense(): Result<License> = withContext(Dispatchers.Default) {
        try {
            val result = licenseApiService.validateLicense()
            result.fold(
                onSuccess = { response ->
                    response.license?.let {
                        Result.success(it.toDomain())
                    } ?: Result.failure(Exception(response.error ?: "License validation failed"))
                },
                onError = { error ->
                    logger.error(error) { "Error validating license" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error validating license" }
            Result.failure(e)
        }
    }
    
    override suspend fun deactivateLicense(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val result = licenseApiService.deactivateLicense()
            result.fold(
                onSuccess = { 
                    Result.success(Unit)
                },
                onError = { error ->
                    logger.error(error) { "Error deactivating license" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error deactivating license" }
            Result.failure(e)
        }
    }
    
    override suspend fun transferLicense(newDeviceId: String): Result<License> = withContext(Dispatchers.Default) {
        try {
            val result = licenseApiService.transferLicense(newDeviceId)
            result.fold(
                onSuccess = { response ->
                    // After transfer, get updated license
                    getLicense()?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("Failed to get license after transfer"))
                },
                onError = { error ->
                    logger.error(error) { "Error transferring license" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error transferring license" }
            Result.failure(e)
        }
    }
    
    override suspend fun getAvailableFeatures(): Result<List<String>> = withContext(Dispatchers.Default) {
        try {
            val result = licenseApiService.getAvailableFeatures()
            result.fold(
                onSuccess = { response ->
                    Result.success(response.data ?: emptyList())
                },
                onError = { error ->
                    logger.error(error) { "Error getting available features" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting available features" }
            Result.failure(e)
        }
    }
    
    override suspend fun checkFeatureAvailability(featureName: String): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            val result = licenseApiService.checkFeatureAvailability(featureName)
            result.fold(
                onSuccess = { response ->
                    val available = response.data?.get("available") as? Boolean ?: false
                    Result.success(available)
                },
                onError = { error ->
                    logger.error(error) { "Error checking feature availability: $featureName" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error checking feature availability: $featureName" }
            Result.failure(e)
        }
    }
    
    private fun LicenseResponse.toDomain(): License {
        return License(
            id = id,
            licenseKey = licenseKey,
            type = LicenseType.valueOf(type.uppercase()),
            status = LicenseStatus.valueOf(status.uppercase()),
            features = features,
            maxCameras = maxCameras,
            maxUsers = maxUsers,
            expiresAt = expiresAt,
            activatedAt = activatedAt,
            deviceId = deviceId,
            isValid = isValid
        )
    }
}

