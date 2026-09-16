package com.company.ipcamera.core.license

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

actual class PlatformCrypto {
    actual fun getSecureDeviceFingerprint(): String {
        // Используем Android ID как базовый идентификатор
        // В продакшене можно комбинировать с другими параметрами устройства
        return try {
            // TODO: Получить Android ID через Settings.Secure.ANDROID_ID
            // Требуется Context для доступа к системным настройкам
            // Реализовать после интеграции Context в LicenseManager
            "android_device_id_placeholder"
        } catch (e: Exception) {
            "android_device_id_fallback"
        }
    }

    actual fun decryptOfflineCode(code: String): OfflineActivationData {
        // Реализация расшифровки офлайн кода
        // В продакшене использовать Android Keystore для хранения ключей
        return try {
            // Добавляем BouncyCastle провайдер если еще не добавлен
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }

            // TODO: Реализовать полную расшифровку через Android Keystore
            // Использовать Cipher с ключом из KeyStore для безопасной расшифровки офлайн кода
            Json.decodeFromString<OfflineActivationData>(code)
        } catch (e: Exception) {
            throw LicenseException(LicenseError.INVALID_ACTIVATION_CODE)
        }
    }

    actual fun schedulePeriodicCheck(checkCallback: (ActivatedLicense) -> Unit) {
        // TODO: Реализовать периодическую проверку лицензии через WorkManager
        // Требуется добавить зависимость androidx.work:work-runtime-ktx
        // и настроить PeriodicWorkRequest для проверки лицензии
    }
}

actual class LicenseRepository(context: Any?) {
    private val androidContext: Context = context as? Context
        ?: throw IllegalArgumentException("Context is required for Android LicenseRepository")

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(androidContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            androidContext,
            "license_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    actual fun saveLicense(license: ActivatedLicense) {
        try {
            val json = Json.encodeToString(license)
            encryptedPrefs.edit()
                .putString("license", json)
                .apply()
        } catch (e: Exception) {
            throw LicenseException(LicenseError.SERVER_ERROR)
        }
    }

    actual fun loadLicense(): ActivatedLicense? {
        return try {
            val json = encryptedPrefs.getString("license", null) ?: return null
            Json.decodeFromString<ActivatedLicense>(json)
        } catch (e: Exception) {
            null
        }
    }

    actual fun deleteLicense() {
        encryptedPrefs.edit()
            .remove("license")
            .apply()
    }

    private fun encryptLicenseData(data: String): String {
        // Дополнительное шифрование данных лицензии
        // В продакшене использовать Android Keystore
        return try {
            // TODO: Реализовать дополнительное шифрование через Android Keystore
            // Использовать Cipher с ключом из KeyStore для дополнительной защиты данных
            data
        } catch (e: Exception) {
            throw LicenseException(LicenseError.SERVER_ERROR)
        }
    }
}

actual fun getPlatformCrypto(): PlatformCrypto {
    return PlatformCrypto()
}

actual fun createLicenseRepository(context: Any?): LicenseRepository {
    return LicenseRepository(context ?: throw IllegalArgumentException("Context is required"))
}

