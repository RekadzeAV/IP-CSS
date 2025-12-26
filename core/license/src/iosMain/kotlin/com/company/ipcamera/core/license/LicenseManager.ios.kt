package com.company.ipcamera.core.license

import platform.Foundation.NSUserDefaults
import platform.UIKit.UIDevice
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

actual class PlatformCrypto {
    actual fun getSecureDeviceFingerprint(): String {
        // TODO: Реализовать получение уникального идентификатора устройства через iOS Keychain
        return UIDevice.currentDevice().identifierForVendor?.UUIDString ?: "unknown"
    }
    
    actual fun decryptOfflineCode(code: String): OfflineActivationData {
        // TODO: Реализовать расшифровку через iOS Keychain
        throw NotImplementedError("Offline activation not implemented yet")
    }
    
    actual fun schedulePeriodicCheck(checkCallback: (ActivatedLicense) -> Unit) {
        // TODO: Реализовать периодическую проверку через iOS Background Tasks
    }
}

actual class LicenseRepository(context: Any?) {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    actual fun saveLicense(license: ActivatedLicense) {
        // TODO: Сохранить в iOS Keychain
        // Временная реализация через UserDefaults
        val json = Json.encodeToString(license)
        userDefaults.setObject(json, "license")
    }
    
    actual fun loadLicense(): ActivatedLicense? {
        // TODO: Загрузить из iOS Keychain
        val json = userDefaults.stringForKey("license") ?: return null
        return try {
            Json.decodeFromString<ActivatedLicense>(json)
        } catch (e: Exception) {
            null
        }
    }
    
    actual fun deleteLicense() {
        userDefaults.removeObjectForKey("license")
    }
}

actual fun getPlatformCrypto(): PlatformCrypto {
    return PlatformCrypto()
}

actual fun createLicenseRepository(context: Any?): LicenseRepository {
    return LicenseRepository(context)
}

