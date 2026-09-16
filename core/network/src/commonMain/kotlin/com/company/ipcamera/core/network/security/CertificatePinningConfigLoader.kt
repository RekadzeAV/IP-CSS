package com.company.ipcamera.core.network.security

/**
 * Платформо-специфичная загрузка конфигурации certificate pinning из файла и окружения.
 * Используется [CertificatePinningManager] для загрузки из config/certificate-pins.json
 * (или пути из CERTIFICATE_PINS_FILE) на всех клиентах: Android, iOS, Desktop/JVM.
 */
internal expect object CertificatePinningConfigLoader {

    /**
     * Читает содержимое файла по заданному пути.
     * Путь интерпретируется платформой: JVM — файловая система, Android — assets/filesDir, iOS — bundle.
     *
     * @param path Путь к файлу (например "config/certificate-pins.json" или "certificate-pins.json")
     * @return Содержимое файла или null, если файл не найден/не удалось прочитать
     */
    fun readFile(path: String): String?

    /**
     * Возвращает значение переменной окружения.
     *
     * @param name Имя переменной (например "CERTIFICATE_PINS_FILE")
     * @return Значение или null
     */
    fun getEnv(name: String): String?

    /**
     * Возвращает все переменные окружения (для поиска CERTIFICATE_PINS_*).
     * На платформах без доступа к полному списку (например iOS) может возвращать null.
     *
     * @return Map имя -> значение или null, если недоступно
     */
    fun getEnvMap(): Map<String, String>?

    /**
     * SHA-256 fingerprint в формате sha256/Base64 (для справки и тестов).
     * На iOS возвращает пустую строку, если платформа не предоставляет API.
     */
    fun calculateSha256Pin(certificateData: ByteArray): String
}
