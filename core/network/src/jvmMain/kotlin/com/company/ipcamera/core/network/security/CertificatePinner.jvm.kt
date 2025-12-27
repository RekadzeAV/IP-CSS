package com.company.ipcamera.core.network.security

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java
import mu.KotlinLogging
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

private val logger = KotlinLogging.logger {}

/**
 * JVM реализация certificate pinner используя кастомный TrustManager
 *
 * Создает кастомный TrustManager, который проверяет SHA-256 fingerprints
 * сертификатов перед принятием соединения.
 */
actual class CertificatePinner(private val config: CertificatePinningConfig) {

    actual fun applyToEngine(engine: HttpClientEngine): HttpClientEngine {
        // JVM engine не поддерживает изменение SSLContext после создания
        // Certificate pinning должен быть настроен при создании engine
        logger.debug { "Certificate pinning should be configured during engine creation for JVM" }
        return engine
    }

    /**
     * Создает Java engine с настроенным certificate pinning
     */
    fun createEngineWithPinning(): HttpClientEngine {
        if (!config.enablePinning || config.pinnedCertificates.isEmpty()) {
            logger.debug { "Certificate pinning disabled, using default Java engine" }
            return Java.create()
        }

        // Создаем кастомный TrustManager с certificate pinning
        val trustManager = createPinningTrustManager()

        // Создаем SSLContext с нашим TrustManager
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), null)

        // Создаем Java engine с настроенным SSLContext
        return Java.create {
            this.sslContext = sslContext
            logger.info { "Certificate pinning configured for Java engine" }
        }
    }

    /**
     * Создает кастомный TrustManager с проверкой certificate pinning
     */
    private fun createPinningTrustManager(): X509TrustManager {
        // Получаем системный TrustManager как fallback для базовой валидации
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as java.security.KeyStore?)
        val defaultTrustManager = trustManagerFactory.trustManagers.first { it is X509TrustManager } as X509TrustManager

        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                // Для клиентских сертификатов используем системную валидацию
                defaultTrustManager.checkClientTrusted(chain, authType)
            }

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                // Сначала выполняем базовую системную валидацию
                defaultTrustManager.checkServerTrusted(chain, authType)

                if (!config.enablePinning || config.pinnedCertificates.isEmpty()) {
                    return // Pinning отключен, используем только системную валидацию
                }

                // Вычисляем pins для всех сертификатов в цепочке
                val certificatePins = chain.map { calculateSha256Pin(it) }.toSet()

                // Проверяем, что хотя бы один pin из конфигурации совпадает
                // Примечание: В идеале нужно знать hostname из контекста запроса,
                // но TrustManager не предоставляет эту информацию. Поэтому проверяем
                // все настроенные pins.
                var pinMatchFound = false
                for ((host, pinnedPins) in config.pinnedCertificates) {
                    for (pinnedPin in pinnedPins) {
                        if (certificatePins.contains(pinnedPin)) {
                            pinMatchFound = true
                            logger.debug { "Certificate pin matched: $pinnedPin for configured host: $host" }
                            break
                        }
                    }
                    if (pinMatchFound) break
                }

                if (!pinMatchFound && config.enforcePinning) {
                    logger.error { "Certificate pinning validation failed: no matching pins found" }
                    throw javax.net.ssl.SSLPeerUnverifiedException(
                        "Certificate pinning validation failed: no matching certificate pins found"
                    )
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return defaultTrustManager.acceptedIssuers
            }
        }
    }

    /**
     * Вычисляет SHA-256 fingerprint сертификата в формате "sha256/..."
     */
    private fun calculateSha256Pin(certificate: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val encoded = digest.digest(certificate.encoded)
        val pin = java.util.Base64.getEncoder().encodeToString(encoded)
        return "sha256/$pin"
    }


    actual fun isSupported(): Boolean = true
}
