package com.company.ipcamera.shared.data.repository

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
 * Реализация LicenseRepository с использованием LicenseApiService.
 * LicenseApiService возвращает данные через getOrThrow(), обрабатываем в try/catch.
 */
class LicenseRepositoryImpl(
    private val licenseApiService: LicenseApiService,
) : LicenseRepository {
    override suspend fun getLicense(): License? =
        withContext(Dispatchers.Default) {
            try {
                licenseApiService.getLicense().toDomain()
            } catch (e: Exception) {
                logger.error(e) { "Error getting license" }
                null
            }
        }

    override suspend fun activateLicense(
        licenseKey: String,
        deviceId: String?,
    ): Result<License> =
        withContext(
            Dispatchers.Default,
        ) {
            try {
                val request = ActivateLicenseRequest(licenseKey = licenseKey, deviceId = deviceId)
                val response = licenseApiService.activateLicense(request)
                response.license?.let { Result.success(it.toDomain()) }
                    ?: Result.failure(Exception(response.message ?: "License activation failed"))
            } catch (e: Exception) {
                logger.error(e) { "Error activating license" }
                Result.failure(e)
            }
        }

    override suspend fun validateLicense(): Result<License> =
        withContext(Dispatchers.Default) {
            try {
                val response = licenseApiService.validateLicense()
                response.license?.let { Result.success(it.toDomain()) }
                    ?: Result.failure(Exception(response.error ?: "License validation failed"))
            } catch (e: Exception) {
                logger.error(e) { "Error validating license" }
                Result.failure(e)
            }
        }

    override suspend fun deactivateLicense(): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                licenseApiService.deactivateLicense()
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error deactivating license" }
                Result.failure(e)
            }
        }

    override suspend fun transferLicense(newDeviceId: String): Result<License> =
        withContext(Dispatchers.Default) {
            try {
                licenseApiService.transferLicense(newDeviceId)
                getLicense()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Failed to get license after transfer"))
            } catch (e: Exception) {
                logger.error(e) { "Error transferring license" }
                Result.failure(e)
            }
        }

    override suspend fun getAvailableFeatures(): Result<List<String>> =
        withContext(Dispatchers.Default) {
            try {
                val response = licenseApiService.getAvailableFeatures()
                Result.success(response.data ?: emptyList())
            } catch (e: Exception) {
                logger.error(e) { "Error getting available features" }
                Result.failure(e)
            }
        }

    override suspend fun checkFeatureAvailability(featureName: String): Result<Boolean> =
        withContext(
            Dispatchers.Default,
        ) {
            try {
                val response = licenseApiService.checkFeatureAvailability(featureName)
                val available = response.data?.get("available") as? Boolean ?: false
                Result.success(available)
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
            isValid = isValid,
        )
    }
}
