package com.company.ipcamera.core.common.security

private const val nativePasswordPrefix = "ENC:"

actual class SecurePasswordEncryption actual constructor() : PasswordEncryption {
    private val localEncryption = SecureLocalDataEncryption()

    actual override fun encrypt(password: String): String {
        if (password.isEmpty() || isEncrypted(password)) return password
        return nativePasswordPrefix + localEncryption.encryptString(password)
    }

    actual override fun decrypt(encryptedPassword: String): String {
        if (encryptedPassword.isEmpty() || !isEncrypted(encryptedPassword)) return encryptedPassword
        return localEncryption.decryptString(encryptedPassword.removePrefix(nativePasswordPrefix))
    }

    actual override fun isEncrypted(value: String): Boolean = value.startsWith(nativePasswordPrefix)
}
