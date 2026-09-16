package com.company.ipcamera.server.cluster

import com.company.ipcamera.server.config.ClusterConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.*

class HealthCheckServiceTest {

    private val healthCheckService = HealthCheckService()

    @Test
    fun `checkNode returns unhealthy for invalid URL`() = runTest {
        val result = healthCheckService.checkNode("http://192.0.2.1:9999")
        assertFalse(result.isHealthy, "Non-routable IP should be unhealthy")
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `checkNode returns unhealthy for timeout`() = runTest {
        val result = healthCheckService.checkNode("http://10.255.255.1:1")
        assertFalse(result.isHealthy, "Unreachable host should timeout and be unhealthy")
    }

    @Test
    fun `checkAllNodes returns results for all nodes`() = runTest {
        val urls = listOf(
            "http://192.0.2.1:9999",
            "http://192.0.2.2:9999"
        )
        val results = healthCheckService.checkAllNodes(urls)
        assertEquals(2, results.size)
        assertTrue(results.all { !it.isHealthy })
    }

    @Test
    fun `getHealthResult reports unhealthy when all nodes down`() = runTest {
        val urls = listOf("http://192.0.2.1:9999")
        val result = healthCheckService.getHealthResult(urls)
        assertFalse(result.isHealthy)
        assertEquals(1, result.nodeCount)
        assertEquals(1, result.unhealthyNodes.size)
    }

    @Test
    fun `reset clears health state`() = runTest {
        val urls = listOf("http://192.0.2.1:9999")
        healthCheckService.getHealthResult(urls)
        healthCheckService.reset()
        // После сброса состояние должно быть пустым
        val result = healthCheckService.getHealthResult(urls)
        assertFalse(result.isHealthy)
    }
}
