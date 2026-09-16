package com.company.ipcamera.server.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.sql.Connection
import java.sql.DatabaseMetaData
import javax.sql.DataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Юнит-тесты [DatabasePerformanceService]: graceful-поведение для не-Hikari
 * DataSource и health-check через реальное соединение (mock).
 */
class DatabasePerformanceServiceTest {

    @Test
    fun `getPoolStats returns empty for non-Hikari datasource`() {
        val dataSource = mockk<DataSource>()
        val service = DatabasePerformanceService(dataSource)
        // DataSource не является HikariDataSource → пустая статистика (без краша)
        assertTrue(service.getPoolStats().isEmpty())
    }

    @Test
    fun `checkHealth reads metadata from connection`() {
        val metaData = mockk<DatabaseMetaData>()
        every { metaData.databaseProductName } returns "PostgreSQL"
        every { metaData.databaseProductVersion } returns "16.0"

        val connection = mockk<Connection>()
        every { connection.isValid(2) } returns true
        every { connection.metaData } returns metaData
        every { connection.close() } returns Unit

        val dataSource = mockk<DataSource>()
        every { dataSource.connection } returns connection

        val service = DatabasePerformanceService(dataSource)

        val health = service.checkHealth()

        assertEquals(true, health["healthy"])
        assertEquals("PostgreSQL", health["database"])
        assertEquals("16.0", health["version"])
        verify { connection.close() }
    }

    @Test
    fun `checkHealth reports unhealthy on connection failure`() {
        val dataSource = mockk<DataSource> {
            every { connection } throws RuntimeException("db down")
        }
        val service = DatabasePerformanceService(dataSource)

        val health = service.checkHealth()

        assertEquals(false, health["healthy"])
        assertTrue(health.containsKey("error"))
    }
}