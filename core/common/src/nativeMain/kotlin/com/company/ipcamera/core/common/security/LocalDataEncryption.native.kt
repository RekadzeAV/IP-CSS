package com.company.ipcamera.core.common.security

private val nativeEncryptionPrefix = "ENC:".encodeToByteArray()
private val nativeEncryptionKey = "ipcss-native-local-key".encodeToByteArray()

actual class SecureLocalDataEncryption actual constructor() : LocalDataEncryption {
    actual override fun encrypt(data: ByteArray): ByteArray {
        if (data.isEmpty() || isEncrypted(data)) return data
        val encrypted = xorWithKey(data)
        return nativeEncryptionPrefix + encrypted
    }

    actual override fun decrypt(encryptedData: ByteArray): ByteArray {
        if (encryptedData.isEmpty() || !isEncrypted(encryptedData)) return encryptedData
        val payload = encryptedData.copyOfRange(nativeEncryptionPrefix.size, encryptedData.size)
        return xorWithKey(payload)
    }

    actual override fun encryptString(data: String): String {
        return encrypt(data.encodeToByteArray()).toHexString()
    }

    actual override fun decryptString(encryptedData: String): String {
        val bytes = encryptedData.hexToByteArrayOrNull() ?: return encryptedData
        return decrypt(bytes).decodeToString()
    }

    actual override fun isEncrypted(data: ByteArray): Boolean {
        if (data.size < nativeEncryptionPrefix.size) return false
        return data.copyOfRange(0, nativeEncryptionPrefix.size).contentEquals(nativeEncryptionPrefix)
    }

    private fun xorWithKey(input: ByteArray): ByteArray {
        val out = ByteArray(input.size)
        for (i in input.indices) {
            out[i] = (input[i].toInt() xor nativeEncryptionKey[i % nativeEncryptionKey.size].toInt()).toByte()
        }
        return out
    }
}

private fun ByteArray.toHexString(): String = joinToString("") { byte ->
    val intValue = byte.toInt() and 0xFF
    intValue.toString(16).padStart(2, '0')
}

private fun String.hexToByteArrayOrNull(): ByteArray? {
    if (length % 2 != 0) return null
    return runCatching {
        ByteArray(length / 2) { index ->
            val chunk = substring(index * 2, index * 2 + 2)
            chunk.toInt(16).toByte()
        }
    }.getOrNull()
}
