package com.company.ipcamera.core.network.security

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinEngineConfig
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * iOS реализация certificate pinner
 *
 * Ktor Darwin engine использует NSURLSession под капотом.
 * Certificate pinning на iOS требует использования URLSessionDelegate методов,
 * что сложнее интегрировать с Ktor. Для полной реализации может потребоваться
 * использование URLSession delegate или альтернативных подходов.
 */
actual class CertificatePinner(private val config: CertificatePinningConfig) {

    // Храним delegate для предотвращения сборки мусора
    private var delegate: CertificatePinningDelegate? = null

    actual fun applyToEngine(engine: HttpClientEngine): HttpClientEngine {
        // Ktor Darwin engine не поддерживает прямое изменение NSURLSession после создания
        // Certificate pinning должен быть настроен при создании engine
        logger.debug { "Certificate pinning should be configured during engine creation for iOS" }
        return engine
    }

    /**
     * Создает Darwin engine с настроенным certificate pinning
     *
     * Реализация использует CertificatePinningEngineWrapper, который создает
     * кастомный NSURLSession с CertificatePinningDelegate для полной поддержки
     * certificate pinning на iOS.
     *
     * Альтернативно можно использовать стандартный Darwin engine, но тогда
     * certificate pinning будет работать только через Network Security Config.
     */
    fun createEngineWithPinning(): HttpClientEngine {
        if (!config.enablePinning || config.pinnedCertificates.isEmpty()) {
            logger.debug { "Certificate pinning disabled, using default Darwin engine" }
            return Darwin.create()
        }

        logger.info {
            "Certificate pinning configured for iOS engine " +
            "(${config.pinnedCertificates.size} hosts, enforce: ${config.enforcePinning})"
        }

        // Создаем delegate для проверки certificate pins
        delegate = CertificatePinningDelegate(config)

        // Используем кастомный engine wrapper для полной поддержки certificate pinning
        // Это обеспечивает полный контроль над NSURLSession и delegate
        return CertificatePinningEngineWrapper(config)
    }

    /**
     * Получить delegate для использования в кастомном NSURLSession
     *
     * Этот метод можно использовать для создания кастомного NSURLSession
     * с certificate pinning delegate, если Ktor не поддерживает это напрямую.
     */
    fun getDelegate(): CertificatePinningDelegate? {
        return delegate
    }

    actual fun isSupported(): Boolean {
        return true
    }
}


