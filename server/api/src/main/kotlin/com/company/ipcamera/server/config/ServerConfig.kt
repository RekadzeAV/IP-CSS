package com.company.ipcamera.server.config

import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Конфигурация сервера
 */
data class ServerConfig(
    val host: String = "0.0.0.0",
    val httpPort: Int = 8080,
    val httpsPort: Int = 8443,
    val useHttps: Boolean = false,
    val sslKeyStorePath: String? = null,
    val sslKeyStorePassword: String? = null,
    val sslKeyAlias: String? = null,
    val forceHttps: Boolean = false,
    val dbMode: String = "embedded",
    val allowExternalTlsTermination: Boolean = false
) {
    companion object {
        /**
         * Загружает конфигурацию из переменных окружения
         */
        fun fromEnvironment(): ServerConfig {
            val env = loadEnvironment()
            val useHttps = env.useHttps
            val dbMode = loadDbMode(env.isProduction)
            
            validateSslConfiguration(useHttps, env.sslKeyStorePath, env.sslKeyStorePassword, env.isProduction)
            validateProductionSecrets(env.isProduction)
            validateForceHttps(env)
            
            return buildServerConfig(env, dbMode)
        }

        private fun loadEnvironment(): EnvironmentContext {
            val environment = System.getenv("ENVIRONMENT")?.trim()?.lowercase()
            val nodeEnv = System.getenv("NODE_ENV")?.trim()?.lowercase()
            val isProduction = environment == "production" || nodeEnv == "production"
            
            return EnvironmentContext(
                isProduction = isProduction,
                useHttps = System.getenv("USE_HTTPS")?.toBoolean() ?: false,
                forceHttps = System.getenv("FORCE_HTTPS")?.toBoolean() ?: isProduction,
                allowExternalTlsTermination = System.getenv("ALLOW_EXTERNAL_TLS_TERMINATION")?.toBoolean() ?: false,
                httpPort = System.getenv("HTTP_PORT")?.toIntOrNull() ?: 8080,
                httpsPort = System.getenv("HTTPS_PORT")?.toIntOrNull() ?: 8443,
                sslKeyStorePath = System.getenv("SSL_KEYSTORE_PATH"),
                sslKeyStorePassword = System.getenv("SSL_KEYSTORE_PASSWORD"),
                sslKeyAlias = System.getenv("SSL_KEY_ALIAS") ?: "server"
            )
        }

        private fun loadDbMode(isProduction: Boolean): String {
            val dbModeRaw = System.getenv("DB_MODE")?.trim()?.lowercase()
            return when (dbModeRaw) {
                null, "" -> if (isProduction) "postgres" else "embedded"
                "embedded", "postgres" -> dbModeRaw
                else -> {
                    logger.warn { "Unknown DB_MODE='$dbModeRaw'. Falling back to 'embedded'." }
                    "embedded"
                }
            }
        }

        private fun validateSslConfiguration(useHttps: Boolean, sslKeyStorePath: String?, sslKeyStorePassword: String?, isProduction: Boolean) {
            if (!useHttps) return
            
            if (sslKeyStorePath == null || sslKeyStorePassword == null) {
                val message = "USE_HTTPS=true but SSL certificates not configured (SSL_KEYSTORE_PATH / SSL_KEYSTORE_PASSWORD)."
                if (isProduction) {
                    throw IllegalStateException("$message Refusing to start in secure mode.")
                }
                logger.warn { "$message Falling back to HTTP in non-production mode." }
            }

            if (sslKeyStorePath != null) {
                val keyStoreFile = File(sslKeyStorePath)
                if (!keyStoreFile.exists()) {
                    val message = "SSL keystore file not found: $sslKeyStorePath."
                    if (isProduction) {
                        throw IllegalStateException("$message Refusing to start in secure mode.")
                    }
                    logger.warn { "$message Falling back to HTTP in non-production mode." }
                }
            }
        }

        private fun validateForceHttps(env: EnvironmentContext) {
            if (env.forceHttps && !env.useHttps && !env.allowExternalTlsTermination) {
                throw IllegalStateException(
                    "FORCE_HTTPS=true requires either USE_HTTPS=true (direct TLS) " +
                        "or ALLOW_EXTERNAL_TLS_TERMINATION=true (trusted reverse proxy)."
                )
            }
        }

        private fun buildServerConfig(env: EnvironmentContext, dbMode: String): ServerConfig {
            val config = ServerConfig(
                host = System.getenv("HOST") ?: "0.0.0.0",
                httpPort = env.httpPort,
                httpsPort = env.httpsPort,
                useHttps = env.useHttps,
                sslKeyStorePath = env.sslKeyStorePath,
                sslKeyStorePassword = env.sslKeyStorePassword,
                sslKeyAlias = env.sslKeyAlias,
                forceHttps = env.forceHttps,
                dbMode = dbMode,
                allowExternalTlsTermination = env.allowExternalTlsTermination
            )

            logger.info {
                "Server config loaded: " +
                "HTTP=${config.httpPort}, " +
                "HTTPS=${if (config.useHttps) config.httpsPort else "disabled"}, " +
                "ForceHTTPS=${config.forceHttps}, " +
                "ExternalTLS=${config.allowExternalTlsTermination}, " +
                "DB_MODE=${config.dbMode}"
            }

            return config
        }

        private data class EnvironmentContext(
            val isProduction: Boolean,
            val useHttps: Boolean,
            val forceHttps: Boolean,
            val allowExternalTlsTermination: Boolean,
            val httpPort: Int,
            val httpsPort: Int,
            val sslKeyStorePath: String?,
            val sslKeyStorePassword: String?,
            val sslKeyAlias: String?
        )

        private fun validateProductionSecrets(isProduction: Boolean) {
            if (!isProduction) return

            val weakSecretMarkers = listOf(
                "changeme",
                "change-me",
                "default",
                "password",
                "secret"
            )

            fun requireStrongEnv(name: String, minLength: Int = 24) {
                val value = System.getenv(name)?.trim()
                    ?: throw IllegalStateException("Missing required environment variable: $name")
                val lowered = value.lowercase()
                if (value.length < minLength || weakSecretMarkers.any { lowered.contains(it) }) {
                    throw IllegalStateException(
                        "Environment variable $name is too weak for production. " +
                            "Use a strong random value (>= $minLength chars)."
                    )
                }
            }

            requireStrongEnv("JWT_SECRET", minLength = 32)
            requireStrongEnv("DB_PASSWORD", minLength = 16)
            requireStrongEnv("REDIS_PASSWORD", minLength = 16)
        }
    }
}
