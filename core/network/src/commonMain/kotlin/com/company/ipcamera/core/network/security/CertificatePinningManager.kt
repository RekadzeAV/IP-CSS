package com.company.ipcamera.core.network.security

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Менеджер для централизованного управления certificate pinning.
 *
 * Поддерживает загрузку конфигурации из файла/конфига на всех клиентах (Android, iOS, Desktop/JVM):
 * - JSON-файл, по умолчанию [DEFAULT_CONFIG_PATH] (config/certificate-pins.json)
 * - Переменные окружения (CERTIFICATE_PINS_FILE, CERTIFICATE_PINNING_ENABLED, CERTIFICATE_PINS_*)
 * - Программная конфигурация через [createFromMap]
 *
 * Формат JSON совместим с [config/certificate-pins.example.json]:
 * - "enabled" / "enablePinning", "enforce" / "enforcePinning", "certificates" / "hosts"
 */
object CertificatePinningManager {

    /** Путь к конфигу по умолчанию (во всех клиентах: Android — assets/filesDir, iOS — bundle, JVM — рабочая директория). */
    const val DEFAULT_CONFIG_PATH: String = "config/certificate-pins.json"

    /**
     * Формат JSON для хранения certificate pins.
     * Поддерживает оба варианта имён полей (example: enabled, enforce, certificates).
     */
    @Serializable
    data class CertificatePinsConfig(
        val hosts: Map<String, List<String>> = emptyMap(),
        val certificates: Map<String, List<String>> = emptyMap(),
        val enablePinning: Boolean = true,
        val enabled: Boolean? = null,
        val enforcePinning: Boolean = true,
        val enforce: Boolean? = null
    ) {
        fun hostsOrCertificates(): Map<String, List<String>> = hosts.ifEmpty { certificates }
        fun enable(): Boolean = enabled ?: enablePinning
        fun enforce(): Boolean = enforce ?: enforcePinning
    }

    /**
     * Загружает конфигурацию из JSON-файла.
     * На Android — из assets или filesDir, на iOS — из bundle, на JVM/Desktop — из файловой системы.
     *
     * @param filePath Путь к JSON-файлу (например [DEFAULT_CONFIG_PATH])
     * @return CertificatePinningConfig или null, если файл не найден
     */
    fun loadFromFile(filePath: String): CertificatePinningConfig? {
        return try {
            val jsonContent = CertificatePinningConfigLoader.readFile(filePath)
                ?: run {
                    logger.debug { "Certificate pins file not found: $filePath" }
                    return null
                }
            val config = Json { ignoreUnknownKeys = true }.decodeFromString<CertificatePinsConfig>(jsonContent)
            val hosts = config.hostsOrCertificates()
            val enablePinning = config.enable()
            val enforcePinning = config.enforce()
            val validatedConfig = validateAndCreateConfig(hosts, enablePinning, enforcePinning)
            logger.info { "Certificate pins loaded from file: $filePath (${hosts.size} hosts)" }
            validatedConfig
        } catch (e: Exception) {
            logger.error(e) { "Failed to load certificate pins from file: $filePath" }
            null
        }
    }

    /**
     * Загружает конфигурацию из переменных окружения.
     * Формат: CERTIFICATE_PINNING_ENABLED, CERTIFICATE_PINNING_ENFORCE, CERTIFICATE_PINS_<host>=sha256/...,...
     * На iOS список переменных недоступен — метод возвращает null, используется загрузка из файла.
     *
     * @return CertificatePinningConfig или null, если переменные не заданы
     */
    fun loadFromEnvironment(): CertificatePinningConfig? {
        val envMap = CertificatePinningConfigLoader.getEnvMap() ?: run {
            logger.debug { "Environment map not available on this platform" }
            return null
        }
        val enablePinning = CertificatePinningConfigLoader.getEnv("CERTIFICATE_PINNING_ENABLED")
            ?.toBoolean() ?: false
        if (!enablePinning) {
            logger.debug { "Certificate pinning disabled via environment variable" }
            return null
        }
        val enforcePinning = CertificatePinningConfigLoader.getEnv("CERTIFICATE_PINNING_ENFORCE")
            ?.toBoolean() ?: true
        val pinsMap = mutableMapOf<String, List<String>>()
        envMap.forEach { (key, value) ->
            if (key.startsWith("CERTIFICATE_PINS_")) {
                val host = key.removePrefix("CERTIFICATE_PINS_").lowercase()
                val pins = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (pins.isNotEmpty()) pinsMap[host] = pins
            }
        }
        if (pinsMap.isEmpty()) {
            logger.debug { "No certificate pins found in environment variables" }
            return null
        }
        val validatedConfig = validateAndCreateConfig(pinsMap, enablePinning, enforcePinning)
        logger.info { "Certificate pins loaded from environment (${pinsMap.size} hosts)" }
        return validatedConfig
    }

    /**
     * Создаёт конфигурацию из программных параметров.
     */
    fun createFromMap(
        certificates: Map<String, List<String>>,
        enablePinning: Boolean = true,
        enforcePinning: Boolean = true
    ): CertificatePinningConfig {
        return validateAndCreateConfig(certificates, enablePinning, enforcePinning)
    }

    /**
     * Загружает конфигурацию с автоматическим определением источника.
     * Порядок: 1) переменные окружения, 2) файл из CERTIFICATE_PINS_FILE, 3) файл по умолчанию [DEFAULT_CONFIG_PATH].
     *
     * @param defaultFilePath Путь к файлу по умолчанию
     * @return CertificatePinningConfig (при отсутствии источника — disabled)
     */
    fun loadConfig(defaultFilePath: String = DEFAULT_CONFIG_PATH): CertificatePinningConfig {
        loadFromEnvironment()?.let {
            logger.info { "Certificate pinning config loaded from environment variables" }
            return it
        }
        CertificatePinningConfigLoader.getEnv("CERTIFICATE_PINS_FILE")?.let { filePath ->
            loadFromFile(filePath)?.let {
                logger.info { "Certificate pinning config loaded from environment file: $filePath" }
                return it
            }
        }
        loadFromFile(defaultFilePath)?.let {
            logger.info { "Certificate pinning config loaded from default file: $defaultFilePath" }
            return it
        }
        logger.info { "No certificate pinning config found, using disabled config" }
        return CertificatePinningConfig.disabled()
    }

    private fun validateAndCreateConfig(
        certificates: Map<String, List<String>>,
        enablePinning: Boolean,
        enforcePinning: Boolean
    ): CertificatePinningConfig {
        val validatedCertificates = mutableMapOf<String, List<String>>()
        certificates.forEach { (host, pins) ->
            val validatedPins = pins.mapNotNull { pin ->
                if (validatePinFormat(pin)) {
                    pin
                } else {
                    logger.warn { "Invalid pin format for host $host: $pin (expected format: sha256/...)" }
                    null
                }
            }
            if (validatedPins.isNotEmpty()) {
                validatedCertificates[host] = validatedPins
                logger.debug { "Validated ${validatedPins.size} pins for host: $host" }
            } else {
                logger.warn { "No valid pins found for host: $host" }
            }
        }
        if (validatedCertificates.isEmpty() && enablePinning) {
            logger.warn { "Certificate pinning enabled but no valid pins found" }
        }
        return CertificatePinningConfig(
            pinnedCertificates = validatedCertificates,
            enablePinning = enablePinning && validatedCertificates.isNotEmpty(),
            enforcePinning = enforcePinning
        )
    }

    fun validatePinFormat(pin: String): Boolean {
        if (!pin.startsWith("sha256/")) return false
        val base64Part = pin.removePrefix("sha256/")
        if (base64Part.isEmpty()) return false
        return try {
            base64Part.length in 43..44 && base64Part.matches(Regex("^[A-Za-z0-9+/=]+$"))
        } catch (_: Exception) {
            false
        }
    }

    /**
     * SHA-256 fingerprint в формате sha256/Base64 (для справки и тестов).
     * На iOS возвращает пустую строку.
     */
    fun calculateSha256Pin(certificateData: ByteArray): String =
        CertificatePinningConfigLoader.calculateSha256Pin(certificateData)
}
