package com.company.ipcamera.core.network

import com.company.ipcamera.core.network.utils.MediaFrame
import kotlinx.coroutines.flow.StateFlow

/**
 * RTSPS Client - RTSP over TLS/SSL
 * * Защищённая версия RTSP клиента с шифрованием трафика
 */
expect class RTSPSClient(config: RTSPSClientConfig) : RtspClientInterface {

    override suspend fun connect(): Boolean
    override suspend fun disconnect()
    override suspend fun play(): Boolean
    override suspend fun pause(): Boolean
    override suspend fun teardown()

    override fun getStatus(): StateFlow<RtspClientStatus>
    override fun getVideoInfo(): VideoStreamInfo?
    override fun getAudioInfo(): AudioStreamInfo?
    override suspend fun getVideoFrame(timeoutMs: Long): MediaFrame?
    override suspend fun getAudioFrame(timeoutMs: Long): MediaFrame?

    override fun close()

    /**
     * Проверить валидность сертификата
     */
    fun isCertificateValid(): Boolean

    /**
     * Получить информацию о сертификате
     */
    fun getCertificateInfo(): CertificateInfo?
}

/**
 * Конфигурация RTSPS клиента
 */
data class RTSPSClientConfig(
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val timeoutMs: Long = 5000,
    val enableVideo: Boolean = true,
    val enableAudio: Boolean = true,
    val rtpTransport: RtpTransport = RtpTransport.TCP,

    // TLS настройки
    val verifyCertificate: Boolean = true,
    val trustAllCertificates: Boolean = false,
    val caCertificatePath: String? = null,
    val clientCertificatePath: String? = null,
    val clientPrivateKeyPath: String? = null,
    val minTlsVersion: TlsVersion = TlsVersion.TLS_1_2
)

/**
 * Версия TLS
 */
enum class TlsVersion {
    TLS_1_0,
    TLS_1_1,
    TLS_1_2,
    TLS_1_3
}

/**
 * Информация о сертификате
 */
data class CertificateInfo(
    val subject: String,
    val issuer: String,
    val validFrom: Long,
    val validTo: Long,
    val serialNumber: String,
    val signatureAlgorithm: String,
    val isSelfSigned: Boolean,
    val isExpired: Boolean,
    val isRevoked: Boolean,
    val dnsMatches: Boolean
)

/**
 * Результат проверки сертификата
 */
data class CertificateValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * Интерфейс для проверки сертификатов
 */
interface CertificateValidator {

    /**
     * Проверить валидность сертификата
     */
    fun validateCertificate(certData: ByteArray): CertificateValidationResult

    /**
     * Проверить цепочку сертификатов
     */
    fun validateChain(certChain: List<ByteArray>): CertificateValidationResult

    /**
     * Проверить отозван ли сертификат
     */
    fun isCertificateRevoked(certData: ByteArray): Boolean
}

/**
 * События TLS подключения
 */
sealed class TlsEvent {
    data class HandshakeStarted(val peer: String) : TlsEvent()
    data class HandshakeCompleted(val protocol: String, val cipher: String) : TlsEvent()
    data class HandshakeFailed(val error: String) : TlsEvent()
    data class CertificateReceived(val certInfo: CertificateInfo) : TlsEvent()
    data class CertificateValidated(val result: CertificateValidationResult) : TlsEvent()
    data class Error(val message: String) : TlsEvent()
}

/**
 * Проверка поддержки TLS на платформе
 */
expect fun isTlsSupported(): Boolean

/**
 * Получить версию TLS библиотеки
 */
expect fun getTlsLibraryVersion(): String?

/**
 * Создание TLS защищённого RTSP клиента
 */
fun createSecureRtspClient(config: RTSPSClientConfig): RtspClientInterface {
    return try {
        RTSPSClient(config)
    } catch (e: Exception) {
        // Fallback на обычный RTSP если TLS не поддерживается
        Live555RTSPClient(config.toRtspConfig())
    }
}

/**
 * Конвертация в обычный RTSP конфиг
 */
private fun RTSPSClientConfig.toRtspConfig(): RtspClientConfig {
    return RtspClientConfig(
        url = url.replace("rtsps://", "rtsp://"),
        username = username,
        password = password,
        timeoutMs = timeoutMs,
        enableVideo = enableVideo,
        enableAudio = enableAudio,
        rtpTransport = rtpTransport
    )
}

/**
 * Extension для проверки URL
 */
val String.isSecureRtspUrl: Boolean
    get() = startsWith("rtsps://", ignoreCase = true) || startsWith("rtspS://", ignoreCase = true)

/**
 * Extension для получения порта из URL
 */
fun String.getRtspPort(): Int {
    return when {
        startsWith("rtsps://", ignoreCase = true) -> 322
        startsWith("rtsp://", ignoreCase = true) -> 554
        else -> 554
    }
}
