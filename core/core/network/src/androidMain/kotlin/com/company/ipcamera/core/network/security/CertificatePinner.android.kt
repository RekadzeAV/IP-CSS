package com.company.ipcamera.core.network.security

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.engine.android.AndroidEngineConfig
import mu.KotlinLogging
import okhttp3.CertificatePinner as OkHttpCertificatePinner
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion

private val logger = KotlinLogging.logger {}

/**
 * Android реализация certificate pinner используя OkHttp CertificatePinner
 *
 * Ktor Android engine использует OkHttp под капотом.
 * Certificate pinning настраивается при создании engine через preconfigured OkHttpClient.
 *
 * Примечание: Для полной интеграции с Ktor Android engine требуется использование
 * AndroidEngineConfig.preconfigured, который доступен в Ktor 2.x.
 */
actual class CertificatePinner(private val config: CertificatePinningConfig) {

    actual fun applyToEngine(engine: HttpClientEngine): HttpClientEngine {
        // Ktor Android engine не поддерживает изменение OkHttpClient после создания
        // Certificate pinning должен быть настроен при создании engine
        logger.debug { "Certificate pinning should be configured during engine creation" }
        return engine
    }

    /**
     * Создает OkHttp CertificatePinner из конфигурации
     */
    fun createOkHttpCertificatePinner(): OkHttpCertificatePinner? {
        if (!config.enablePinning || config.pinnedCertificates.isEmpty()) {
            return null
        }

        val builder = OkHttpCertificatePinner.Builder()
        config.pinnedCertificates.forEach { (host, pins) ->
            pins.forEach { pin ->
                builder.add(host, pin)
                logger.debug { "Added certificate pin for host: $host - $pin" }
            }
        }
        return builder.build()
    }

    /**
     * Создает Android engine с настроенным certificate pinning
     *
     * Примечание: Ktor Android engine использует OkHttp под капотом, но не предоставляет
     * прямого способа передать предварительно настроенный OkHttpClient с certificate pinning.
     *
     * Для полной реализации certificate pinning на Android рекомендуется использовать:
     * 1. Android Network Security Config (XML файл) - предпочтительный способ
     * 2. Или использовать прямой OkHttpClient вместо Ktor для запросов с pinning
     *
     * Данная реализация возвращает стандартный engine. Certificate pinning должен быть
     * настроен через Android Network Security Config или другим способом.
     */
    fun createEngineWithPinning(): HttpClientEngine {
        val certificatePinner = createOkHttpCertificatePinner()

        if (certificatePinner != null) {
            logger.info { "Certificate pinning configuration provided (${config.pinnedCertificates.size} hosts)" }
            logger.warn {
                "Full certificate pinning integration requires Android Network Security Config. " +
                "Current implementation uses default engine. " +
                "See: https://developer.android.com/training/articles/security-config"
            }
        }

        // Возвращаем стандартный engine
        // Для полной реализации требуется использовать Network Security Config
        // или создать кастомный engine с прямым доступом к OkHttpClient
        return Android.create()
    }

    actual fun isSupported(): Boolean = true
}

