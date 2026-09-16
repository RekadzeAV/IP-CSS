package com.company.ipcamera.core.network.security

import io.ktor.client.engine.HttpClientEngine
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Тесты для Android реализации CertificatePinner.
 *
 * Проверяют (2.1.4): успешное прохождение при валидном pin, отказ при неверном сертификате.
 * Используется OkHttp CertificatePinner.check() для проверки цепочки без реального соединения.
 */
class CertificatePinnerAndroidTest {

    private fun getSystemTrustedCertificate(): X509Certificate? {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as java.security.KeyStore?)
        val tm = tmf.trustManagers.firstOrNull { it is X509TrustManager } as? X509TrustManager ?: return null
        return tm.acceptedIssuers?.firstOrNull()
    }

    private fun computeSha256Pin(cert: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val encoded = digest.digest(cert.encoded)
        return "sha256/${java.util.Base64.getEncoder().encodeToString(encoded)}"
    }

    @Test
    fun testValidPin_acceptsConnection() {
        // Успешное соединение при валидном pin: check() с совпадающим сертификатом не выбрасывает
        val cert = getSystemTrustedCertificate()
        assumeTrue(
            "System trust store has no CA certificates (e.g. emulator); skipping pin-accept test",
            cert != null
        )
        val validPin = computeSha256Pin(cert!!)
        val config = CertificatePinningConfig.create(
            mapOf("example.com" to listOf(validPin)),
            enforcePinning = true
        )
        val pinner = CertificatePinner(config)
        val okHttpPinner = pinner.createOkHttpCertificatePinner()
        assertNotNull(okHttpPinner, "OkHttp pinner should be created for valid pin config")
        okHttpPinner!!.check("example.com", listOf(cert))
        // не выбрасывает — проверка пройдена
    }

    @Test
    fun testInvalidPin_rejectsConnection() {
        // Отказ при неверном сертификате: check() с несовпадающим pin выбрасывает SSLPeerUnverifiedException
        val cert = getSystemTrustedCertificate()
        assumeTrue(
            "System trust store has no CA certificates; skipping pin-reject test",
            cert != null
        )
        val wrongPin = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        val config = CertificatePinningConfig.create(
            mapOf("example.com" to listOf(wrongPin)),
            enforcePinning = true
        )
        val pinner = CertificatePinner(config)
        val okHttpPinner = pinner.createOkHttpCertificatePinner()
        assertNotNull(okHttpPinner, "Pinner must be present to enforce rejection on wrong cert")
        val thrown = assertFailsWith<SSLPeerUnverifiedException> {
            okHttpPinner!!.check("example.com", listOf(cert!!))
        }
        assertTrue(
            thrown.message?.contains("Certificate pinning", ignoreCase = true) == true ||
                thrown.message?.contains("pin", ignoreCase = true) == true,
            "Exception message should mention pinning: ${thrown.message}"
        )
    }

    @Test
    fun testCreateOkHttpCertificatePinner_validPins() {
        val certificates = mapOf(
            "example.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="),
            "api.example.com" to listOf("sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
        )
        val config = CertificatePinningConfig.create(certificates)
        val pinner = CertificatePinner(config)

        val okHttpPinner = pinner.createOkHttpCertificatePinner()

        assertNotNull(okHttpPinner, "OkHttp CertificatePinner should be created")
    }

    @Test
    fun testCreateOkHttpCertificatePinner_disabled() {
        val config = CertificatePinningConfig.disabled()
        val pinner = CertificatePinner(config)

        val okHttpPinner = pinner.createOkHttpCertificatePinner()

        assertNull(okHttpPinner, "OkHttp CertificatePinner should be null when pinning is disabled")
    }

    @Test
    fun testCreateOkHttpCertificatePinner_emptyCertificates() {
        val config = CertificatePinningConfig.create(emptyMap())
        val pinner = CertificatePinner(config)

        val okHttpPinner = pinner.createOkHttpCertificatePinner()

        assertNull(okHttpPinner, "OkHttp CertificatePinner should be null when certificates are empty")
    }

    @Test
    fun testCreateEngineWithPinning_enabled() {
        val certificates = mapOf(
            "example.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        )
        val config = CertificatePinningConfig.create(certificates)
        val pinner = CertificatePinner(config)

        val engine = pinner.createEngineWithPinning()

        assertNotNull(engine, "Engine should be created")
        assertTrue(engine is HttpClientEngine, "Engine should be HttpClientEngine")
    }

    @Test
    fun testCreateEngineWithPinning_disabled() {
        val config = CertificatePinningConfig.disabled()
        val pinner = CertificatePinner(config)

        val engine = pinner.createEngineWithPinning()

        assertNotNull(engine, "Engine should be created even when pinning is disabled")
    }

    @Test
    fun testCreateEngineWithPinning_multipleHosts() {
        val certificates = mapOf(
            "api.example.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="),
            "cdn.example.com" to listOf("sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
        )
        val config = CertificatePinningConfig.create(certificates)
        val pinner = CertificatePinner(config)

        val engine = pinner.createEngineWithPinning()

        assertNotNull(engine, "Engine should be created with multiple hosts")
    }

    @Test
    fun testCreateEngineWithPinning_multiplePinsPerHost() {
        val certificates = mapOf(
            "example.com" to listOf(
                "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
            )
        )
        val config = CertificatePinningConfig.create(certificates)
        val pinner = CertificatePinner(config)

        val engine = pinner.createEngineWithPinning()

        assertNotNull(engine, "Engine should be created with multiple pins per host")
    }

    @Test
    fun testIsSupported() {
        val config = CertificatePinningConfig.disabled()
        val pinner = CertificatePinner(config)

        assertTrue(pinner.isSupported(), "Certificate pinning should be supported on Android")
    }

    @Test
    fun testApplyToEngine() {
        val config = CertificatePinningConfig.disabled()
        val pinner = CertificatePinner(config)

        // Создаем базовый engine
        val baseEngine = pinner.createEngineWithPinning()

        // Применяем pinning (должно вернуть engine без изменений, так как pinning настраивается при создании)
        val resultEngine = pinner.applyToEngine(baseEngine)

        assertNotNull(resultEngine, "Engine should be returned")
        assertEquals(baseEngine, resultEngine, "Engine should be returned unchanged")
    }
}
