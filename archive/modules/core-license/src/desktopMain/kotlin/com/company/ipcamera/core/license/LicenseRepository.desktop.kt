package com.company.ipcamera.core.license

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files

/**
 * Desktop (JVM) actual implementation of LicenseRepository.
 * Stores license data as a JSON file in the user's config directory.
 */
actual class LicenseRepository actual constructor(context: Any?) {

    private val licenseFile: File by lazy {
        val baseDir = System.getProperty("user.home")?.let { File(it, ".ipcamera") }
            ?: File(System.getProperty("java.io.tmpdir"), ".ipcamera")
        baseDir.mkdirs()
        File(baseDir, "license.json")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    actual fun saveLicense(license: ActivatedLicense) {
        licenseFile.writeText(json.encodeToString(license))
    }

    actual fun loadLicense(): ActivatedLicense? {
        if (!licenseFile.exists()) return null
        return try {
            json.decodeFromString<ActivatedLicense>(licenseFile.readText())
        } catch (e: Exception) {
            null
        }
    }

    actual fun deleteLicense() {
        if (licenseFile.exists()) {
            licenseFile.delete()
        }
    }
}
