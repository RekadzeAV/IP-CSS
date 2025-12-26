package com.company.ipcamera.core.license

import platform.Foundation.*
import platform.UIKit.UIDevice
import platform.Security.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.cinterop.*
import platform.darwin.OSStatus

actual class PlatformCrypto {
    actual fun getSecureDeviceFingerprint(): String {
        // Используем identifierForVendor как базовый идентификатор
        // В продакшене можно комбинировать с другими параметрами устройства
        return UIDevice.currentDevice().identifierForVendor?.UUIDString ?: "unknown"
    }

    actual fun decryptOfflineCode(code: String): OfflineActivationData {
        // Реализация расшифровки офлайн кода через Security framework
        return try {
            // TODO: Реализовать полную расшифровку с использованием Keychain Services
            // Временная реализация
            Json.decodeFromString<OfflineActivationData>(code)
        } catch (e: Exception) {
            throw LicenseException(LicenseError.INVALID_ACTIVATION_CODE)
        }
    }

    actual fun schedulePeriodicCheck(checkCallback: (ActivatedLicense) -> Unit) {
        // TODO: Реализовать периодическую проверку через iOS Background Tasks
        // Использовать BGTaskScheduler для фоновых задач
    }
}

actual class LicenseRepository(context: Any?) {
    private val keychainService = "com.company.ipcamera.license"
    private val keychainKey = "license_data"

    actual fun saveLicense(license: ActivatedLicense) {
        try {
            val json = Json.encodeToString(license)
            val data = json.encodeToNSData()

            // Сохраняем в Keychain для безопасности
            val query = mapOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to keychainService,
                kSecAttrAccount to keychainKey,
                kSecValueData to data
            )

            // Удаляем старую запись если существует
            SecItemDelete(query.toCFDictionary())

            // Добавляем новую запись
            val status = SecItemAdd(query.toCFDictionary(), null)
            if (status != errSecSuccess.toInt()) {
                // Fallback на UserDefaults если Keychain не доступен
                NSUserDefaults.standardUserDefaults.setObject(json, "license")
            }
        } catch (e: Exception) {
            // Fallback на UserDefaults
            val json = Json.encodeToString(license)
            NSUserDefaults.standardUserDefaults.setObject(json, "license")
        }
    }

    actual fun loadLicense(): ActivatedLicense? {
        return try {
            // Пытаемся загрузить из Keychain
            val query = mapOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to keychainService,
                kSecAttrAccount to keychainKey,
                kSecReturnData to kCFBooleanTrue,
                kSecMatchLimit to kSecMatchLimitOne
            )

            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query.toCFDictionary(), result.ptr)

            if (status == errSecSuccess.toInt()) {
                val data = result.value as? NSData
                val json = data?.let { NSString.create(stringEncoding = NSUTF8StringEncoding, data = it)?.toString() }
                json?.let { Json.decodeFromString<ActivatedLicense>(it) }
            } else {
                // Fallback на UserDefaults
                val json = NSUserDefaults.standardUserDefaults.stringForKey("license")
                json?.let { Json.decodeFromString<ActivatedLicense>(it) }
            }
        } catch (e: Exception) {
            // Fallback на UserDefaults
            val json = NSUserDefaults.standardUserDefaults.stringForKey("license")
            json?.let { Json.decodeFromString<ActivatedLicense>(it) }
        }
    }

    actual fun deleteLicense() {
        try {
            // Удаляем из Keychain
            val query = mapOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to keychainService,
                kSecAttrAccount to keychainKey
            )
            SecItemDelete(query.toCFDictionary())
        } catch (e: Exception) {
            // Игнорируем ошибки
        }

        // Также удаляем из UserDefaults
        NSUserDefaults.standardUserDefaults.removeObjectForKey("license")
    }

    private fun String.encodeToNSData(): NSData {
        return NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding) ?: NSData.create()
    }

    private fun Map<*, *>.toCFDictionary(): CFDictionary {
        // Преобразование Map в CFDictionary для Keychain API
        // В реальной реализации использовать правильное преобразование
        return NSDictionary.create(dictionary = this) as CFDictionary
    }
}

actual fun getPlatformCrypto(): PlatformCrypto {
    return PlatformCrypto()
}

actual fun createLicenseRepository(context: Any?): LicenseRepository {
    return LicenseRepository(context)
}

