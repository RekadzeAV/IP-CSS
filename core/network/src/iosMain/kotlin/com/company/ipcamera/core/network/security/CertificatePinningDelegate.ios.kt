package com.company.ipcamera.core.network.security

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.Security.*
import platform.darwin.*
import platform.objc.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * URLSessionDelegate для проверки certificate pinning
 *
 * Реализует метод URLSession:didReceiveChallenge:completionHandler: для проверки
 * SHA-256 fingerprints сертификатов перед принятием соединения.
 *
 * В Kotlin/Native для iOS, чтобы реализовать URLSessionDelegate протокол,
 * нужно использовать правильный синтаксис методов. Метод должен соответствовать
 * Objective-C сигнатуре: - (void)URLSession:(NSURLSession *)session
 * didReceiveChallenge:(NSURLAuthenticationChallenge *)challenge
 * completionHandler:(void (^)(NSURLSessionAuthChallengeDisposition, NSURLCredential *))completionHandler
 */
@OptIn(ExperimentalForeignApi::class)
class CertificatePinningDelegate(
    private val config: CertificatePinningConfig
) : NSObject() {

    /**
     * Обработка SSL/TLS challenge для проверки certificate pinning
     *
     * Этот метод реализует URLSessionDelegate протокол и будет вызываться
     * NSURLSession при получении SSL/TLS challenge.
     *
     * Objective-C сигнатура метода:
     * - (void)URLSession:(NSURLSession *)session
     *   didReceiveChallenge:(NSURLAuthenticationChallenge *)challenge
     *   completionHandler:(void (^)(NSURLSessionAuthChallengeDisposition, NSURLCredential *))completionHandler
     *
     * В Kotlin/Native метод автоматически экспортируется для Objective-C,
     * если класс наследуется от NSObject и метод имеет правильную сигнатуру.
     */
    fun URLSession_didReceiveChallenge_completionHandler(
        session: NSURLSession,
        challenge: NSURLAuthenticationChallenge,
        completionHandler: ObjCBlock2<NSURLSessionAuthChallengeDisposition, NSURLCredential?, Unit>
    ) {
        try {
            // Проверяем, что это SSL/TLS challenge
            val authMethod = challenge.protectionSpace.authenticationMethod
            if (authMethod != NSURLAuthenticationMethodServerTrust) {
                // Не SSL challenge, используем стандартную обработку
                completionHandler.invoke(NSURLSessionAuthChallengeDispositionPerformDefaultHandling, null)
                return
            }

            // Получаем серверный trust
            val serverTrust = challenge.protectionSpace.serverTrust ?: run {
                logger.error { "Server trust is null" }
                completionHandler.invoke(NSURLSessionAuthChallengeDispositionCancelAuthenticationChallenge, null)
                return
            }

            // Получаем hostname
            val hostname = challenge.protectionSpace.host ?: run {
                logger.error { "Hostname is null" }
                completionHandler.invoke(NSURLSessionAuthChallengeDispositionCancelAuthenticationChallenge, null)
                return
            }

            // Проверяем certificate pinning
            if (!config.enablePinning || config.pinnedCertificates.isEmpty()) {
                // Pinning отключен, используем стандартную валидацию
                completionHandler.invoke(NSURLSessionAuthChallengeDispositionPerformDefaultHandling, null)
                return
            }

            val pins = config.pinnedCertificates[hostname] ?: run {
                // Нет pins для этого hostname, используем стандартную валидацию
                logger.debug { "No certificate pins configured for hostname: $hostname" }
                completionHandler.invoke(NSURLSessionAuthChallengeDispositionPerformDefaultHandling, null)
                return
            }

            // Получаем цепочку сертификатов
            val certificateCount = SecTrustGetCertificateCount(serverTrust)
            if (certificateCount == 0u) {
                logger.error { "No certificates in trust chain" }
                if (config.enforcePinning) {
                    completionHandler.invoke(NSURLSessionAuthChallengeDispositionCancelAuthenticationChallenge, null)
                } else {
                    completionHandler.invoke(NSURLSessionAuthChallengeDispositionPerformDefaultHandling, null)
                }
                return
            }

            // Проверяем каждый сертификат в цепочке
            var pinMatchFound = false
            for (i in 0 until certificateCount.toInt()) {
                val certificate = SecTrustGetCertificateAtIndex(serverTrust, i.toULong())
                if (certificate == null) continue

                // Получаем DER данные сертификата
                val certificateData = SecCertificateCopyData(certificate)
                if (certificateData == null) continue

                // Вычисляем SHA-256 pin
                val pin = calculateSha256Pin(certificateData)

                // Проверяем совпадение с настроенными pins
                if (pins.contains(pin)) {
                    pinMatchFound = true
                    logger.debug { "Certificate pin matched: $pin for hostname: $hostname" }
                    break
                }
            }

            if (!pinMatchFound) {
                logger.error {
                    "Certificate pinning validation failed for hostname: $hostname. " +
                    "No matching pins found in certificate chain."
                }

                if (config.enforcePinning) {
                    // Отклоняем соединение
                    completionHandler.invoke(NSURLSessionAuthChallengeDispositionCancelAuthenticationChallenge, null)
                    return
                } else {
                    // Предупреждаем, но разрешаем соединение
                    logger.warn { "Certificate pin mismatch, but enforcePinning is disabled" }
                }
            }

            // Создаем credential и разрешаем соединение
            val credential = NSURLCredential.credentialForTrust(serverTrust)
            completionHandler.invoke(NSURLSessionAuthChallengeDispositionUseCredential, credential)
        } catch (e: Throwable) {
            logger.error(e) { "Error in certificate pinning validation" }
            // В случае ошибки, если enforcePinning включен, отклоняем соединение
            if (config.enforcePinning) {
                completionHandler.invoke(NSURLSessionAuthChallengeDispositionCancelAuthenticationChallenge, null)
            } else {
                completionHandler.invoke(NSURLSessionAuthChallengeDispositionPerformDefaultHandling, null)
            }
        }
    }

    /**
     * Вычисляет SHA-256 fingerprint сертификата в формате "sha256/..."
     *
     * Использует CommonCrypto для вычисления SHA-256 от DER данных сертификата.
     */
    private fun calculateSha256Pin(certificateData: NSData): String {
        return memScoped {
            // Вычисляем SHA-256 через CommonCrypto
            val hash = allocArray<UByteVar>(32) // SHA-256 = 32 bytes

            val dataBytes = certificateData.bytes ?: run {
                logger.error { "Certificate data bytes are null" }
                return "sha256/INVALID"
            }

            val dataLength = certificateData.length.toInt()
            if (dataLength <= 0) {
                logger.error { "Certificate data length is invalid: $dataLength" }
                return "sha256/INVALID"
            }

            // Вычисляем SHA-256
            val result = CC_SHA256(
                dataBytes,
                dataLength.convert(),
                hash
            )

            if (result == null) {
                logger.error { "Failed to calculate SHA-256 hash" }
                return "sha256/INVALID"
            }

            // Конвертируем hash в NSData для Base64 encoding
            val hashData = NSData.dataWithBytes(hash, 32u)
            val base64String = hashData.base64EncodedStringWithOptions(0u)

            "sha256/$base64String"
        }
    }
}

