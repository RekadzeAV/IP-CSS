package com.company.ipcamera.core.security

/**
 * Конфигурация модуля безопасности
 */
data class SecurityConfig(
    /**
     * Включить шифрование паролей
     */
    val enablePasswordEncryption: Boolean = true,

    /**
     * Включить шифрование локальных данных
     */
    val enableLocalDataEncryption: Boolean = true,

    /**
     * Включить проверку сертификатов (certificate pinning)
     */
    val enableCertificatePinning: Boolean = true,

    /**
     * Путь к файлу конфигурации certificate pinning
     */
    val certificatePinningConfigPath: String = "security/certificate-pinning.json",

    /**
     * Включить валидацию ввода
     */
    val enableInputValidation: Boolean = true,

    /**
     * Строгий режим (отклонять все сомнения)
     */
    val strictMode: Boolean = false,

    /**
     * Логирование попыток атак
     */
    val logAttackAttempts: Boolean = true,

    /**
     * Путь к keystore/keychain
     */
    val keyStorePath: String? = null
)

/**
 * Билдер для SecurityConfig
 */
class SecurityConfigBuilder {
    private var config = SecurityConfig()

    fun enablePasswordEncryption(enabled: Boolean) = apply {
        config = config.copy(enablePasswordEncryption = enabled)
    }

    fun enableLocalDataEncryption(enabled: Boolean) = apply {
        config = config.copy(enableLocalDataEncryption = enabled)
    }

    fun enableCertificatePinning(enabled: Boolean) = apply {
        config = config.copy(enableCertificatePinning = enabled)
    }

    fun certificatePinningConfigPath(path: String) = apply {
        config = config.copy(certificatePinningConfigPath = path)
    }

    fun enableInputValidation(enabled: Boolean) = apply {
        config = config.copy(enableInputValidation = enabled)
    }

    fun strictMode(enabled: Boolean) = apply {
        config = config.copy(strictMode = enabled)
    }

    fun logAttackAttempts(enabled: Boolean) = apply {
        config = config.copy(logAttackAttempts = enabled)
    }

    fun keyStorePath(path: String?) = apply {
        config = config.copy(keyStorePath = path)
    }

    fun build(): SecurityConfig = config
}

/**
 * Фабрика для создания SecurityConfig
 */
object SecurityConfigFactory {
    fun create(block: SecurityConfigBuilder.() -> Unit = {}): SecurityConfig {
        return SecurityConfigBuilder().apply(block).build()
    }

    /**
     * Создание production конфигурации
     */
    fun createProduction(): SecurityConfig {
        return SecurityConfig(
            enablePasswordEncryption = true,
            enableLocalDataEncryption = true,
            enableCertificatePinning = true,
            strictMode = true,
            logAttackAttempts = true
        )
    }

    /**
     * Создание development конфигурации
     */
    fun createDevelopment(): SecurityConfig {
        return SecurityConfig(
            enablePasswordEncryption = true,
            enableLocalDataEncryption = false,
            enableCertificatePinning = false,
            strictMode = false,
            logAttackAttempts = true
        )
    }
}
