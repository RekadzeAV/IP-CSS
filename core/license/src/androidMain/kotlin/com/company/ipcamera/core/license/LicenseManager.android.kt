package com.company.ipcamera.core.license

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

actual class PlatformCrypto {
    actual fun getSecureDeviceFingerprint(): String {
        // TODO: Реализовать получение уникального идентификатора устройства через Android Keystore
        // Временная реализация через статический доступ (требует Context в будущем)
        return "android_device_id" // TODO: Реализовать через Context
    }
    
    actual fun decryptOfflineCode(code: String): OfflineActivationData {
        // TODO: Реализовать расшифровку через Android Keystore
        throw NotImplementedError("Offline activation not implemented yet")
    }
    
    actual fun schedulePeriodicCheck(checkCallback: (ActivatedLicense) -> Unit) {
        // TODO: Реализовать периодическую проверку через WorkManager
    }
}

actual class LicenseRepository(context: Any?) {
    private val androidContext: Context = context as? Context 
        ?: throw IllegalArgumentException("Context is required for Android LicenseRepository")
    private val prefs: SharedPreferences = androidContext.getSharedPreferences("license_prefs", Context.MODE_PRIVATE)
    
    actual fun saveLicense(license: ActivatedLicense) {
        // TODO: Сохранить в защищенное хранилище (EncryptedSharedPreferences или Keystore)
        // Временная реализация через SharedPreferences
        val json = Json.encodeToString(license)
        prefs.edit().putString("license", json).apply()
    }
    
    actual fun loadLicense(): ActivatedLicense? {
        // TODO: Загрузить из защищенного хранилища
        val json = prefs.getString("license", null) ?: return null
        return try {
            Json.decodeFromString<ActivatedLicense>(json)
        } catch (e: Exception) {
            null
        }
    }
    
    actual fun deleteLicense() {
        prefs.edit().remove("license").apply()
    }
}

actual fun getPlatformCrypto(): PlatformCrypto {
    return PlatformCrypto()
}

actual fun createLicenseRepository(context: Any?): LicenseRepository {
    return LicenseRepository(context)
}

