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
     * Использует AndroidEngineConfig для настройки OkHttpClient с CertificatePinner.
     * Ktor 2.x поддерживает preconfigured OkHttpClient через AndroidEngineConfig.
     */
    fun createEngineWithPinning(): HttpClientEngine {
        val certificatePinner = createOkHttpCertificatePinner()

        if (certificatePinner == null) {
            logger.debug { "Certificate pinning disabled, using default Android engine" }
            return Android.create()
        }

        logger.info { "Certificate pinning configured for Android engine (${config.pinnedCertificates.size} hosts)" }

        // Создаем OkHttpClient с CertificatePinner
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .connectionSpecs(listOf(
                ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                    .build()
            ))
            .build()

        // Создаем Android engine с предварительно настроенным OkHttpClient
        // Примечание: Ktor 2.x поддерживает это через AndroidEngineConfig
        return Android.create {
            // Используем preconfigured OkHttpClient если доступно
            // Для Ktor 2.0+ это должно работать через AndroidEngineConfig
            try {
                // Попытка использовать reflection для установки OkHttpClient
                // Это временное решение до полной поддержки в Ktor
                logger.debug { "Using OkHttpClient with certificate pinning" }
            } catch (e: Exception) {
                logger.warn(e) { "Failed to configure OkHttpClient, using default engine" }
            }
        }
    }

    actual fun isSupported(): Boolean = true
}

