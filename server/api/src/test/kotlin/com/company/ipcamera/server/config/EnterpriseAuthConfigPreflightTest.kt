package com.company.ipcamera.server.config

import kotlin.test.Test
import kotlin.test.assertFailsWith

class EnterpriseAuthConfigPreflightTest {

    @Test
    fun `preflight passes when encryption is not required in non production`() {
        EnterpriseAuthConfig.validateSecurityRequirementsForServerStartup(
            isProductionEnvironment = false,
            envReader = { null }
        )
    }

    @Test
    fun `preflight fails when encryption key is missing and explicitly required`() {
        assertFailsWith<IllegalArgumentException> {
            EnterpriseAuthConfig.validateSecurityRequirementsForServerStartup(
                isProductionEnvironment = false,
                envReader = { name ->
                    when (name) {
                        "DATA_ENCRYPTION_REQUIRED" -> "true"
                        else -> null
                    }
                }
            )
        }
    }

    @Test
    fun `preflight fails when encryption key format is invalid`() {
        assertFailsWith<IllegalArgumentException> {
            EnterpriseAuthConfig.validateSecurityRequirementsForServerStartup(
                isProductionEnvironment = true,
                envReader = { name ->
                    when (name) {
                        "DATA_ENCRYPTION_KEY" -> "not-a-hex-key"
                        else -> null
                    }
                }
            )
        }
    }

    @Test
    fun `preflight passes when encryption key has valid hex length`() {
        EnterpriseAuthConfig.validateSecurityRequirementsForServerStartup(
            isProductionEnvironment = true,
            envReader = { name ->
                when (name) {
                    "DATA_ENCRYPTION_KEY" -> "00112233445566778899aabbccddeeff"
                    else -> null
                }
            }
        )
    }
}
