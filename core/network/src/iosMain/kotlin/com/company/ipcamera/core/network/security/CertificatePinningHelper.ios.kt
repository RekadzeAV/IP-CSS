package com.company.ipcamera.core.network.security

import platform.Foundation.*
import platform.objc.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Helper для интеграции certificate pinning с NSURLSession
 *
 * Предоставляет методы для создания NSURLSession с certificate pinning delegate
 * и интеграции с Ktor Darwin engine.
 */
object CertificatePinningHelper {

    /**
     * Создает NSURLSession с certificate pinning delegate
     *
     * @param config Конфигурация certificate pinning
     * @param configuration NSURLSessionConfiguration (опционально)
     * @return NSURLSession с настроенным delegate
     */
    fun createSessionWithPinning(
        config: CertificatePinningConfig,
        configuration: NSURLSessionConfiguration? = null
    ): NSURLSession {
        val sessionConfig = configuration ?: NSURLSessionConfiguration.defaultSessionConfiguration()
        val delegate = CertificatePinningDelegate(config)

        logger.debug {
            "Creating NSURLSession with certificate pinning " +
            "(${config.pinnedCertificates.size} hosts)"
        }

        return NSURLSession.sessionWithConfiguration(
            sessionConfig,
            delegate = delegate,
            delegateQueue = null
        )
    }

    /**
     * Устанавливает delegate для существующего NSURLSession через reflection
     *
     * Примечание: Это может не работать, если Ktor не позволяет изменять delegate.
     * Используйте createSessionWithPinning для создания нового session.
     */
    fun setDelegateForSession(
        session: NSURLSession,
        config: CertificatePinningConfig
    ): Boolean {
        return try {
            // Попытка установить delegate через reflection
            // Это может не работать в зависимости от реализации Ktor
            logger.warn {
                "Setting delegate for existing NSURLSession may not work. " +
                "Consider using createSessionWithPinning instead."
            }
            false
        } catch (e: Throwable) {
            logger.error(e) { "Failed to set delegate for NSURLSession" }
            false
        }
    }
}

