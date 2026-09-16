package com.company.ipcamera.server.integration

import com.company.ipcamera.server.config.DatabaseConfig
import kotlin.test.Test
import kotlin.test.assertTrue

class DatabaseComposeIntegrationTest {

    @Test
    fun `postgres mode creates datasource and applies migrations`() {
        val dbMode = System.getenv("DB_MODE")
        val databaseUrl = System.getenv("DATABASE_URL")
        if (dbMode != "postgres" || databaseUrl.isNullOrBlank()) {
            // Test is designed for compose-backed integration run.
            return
        }

        val driver = DatabaseConfig.createPostgresDriver()
        try {
            assertTrue(driver != null, "SQL driver must be created in postgres mode")

            val dataSource = DatabaseConfig.getDataSource()
            assertTrue(dataSource != null, "Hikari data source must be initialized")

            dataSource!!.connection.use { connection ->
                val auditTableExists = connection.tableExists("audit_log")
                assertTrue(auditTableExists, "audit_log table must exist after migrations")
                val cameraTableExists = connection.tableExists("camera")
                assertTrue(cameraTableExists, "camera table must exist after migrations")

                val migrationCount = connection.countRows("flyway_schema_history")
                assertTrue(migrationCount >= 1, "Expected at least one applied Flyway migration")

                val auditIndexCount = connection.countIndexesForTable("audit_log")
                assertTrue(auditIndexCount >= 1, "Expected at least one index for audit_log table")

                val hasHashChainColumns = connection.hasColumns(
                    tableName = "audit_log",
                    expectedColumns = listOf("previous_hash", "integrity_hash")
                )
                assertTrue(hasHashChainColumns, "audit_log integrity chain columns must exist (V5)")
            }
        } finally {
            DatabaseConfig.closeDataSource()
        }
    }

    private fun java.sql.Connection.tableExists(tableName: String): Boolean {
        metaData.getTables(null, null, tableName, arrayOf("TABLE")).use { rs ->
            return rs.next()
        }
    }

    private fun java.sql.Connection.countRows(tableName: String): Int {
        createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM $tableName").use { rs ->
                assertTrue(rs.next(), "COUNT query must return one row for $tableName")
                return rs.getInt(1)
            }
        }
    }

    private fun java.sql.Connection.countIndexesForTable(tableName: String): Int {
        prepareStatement(
            "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND tablename = ?"
        ).use { statement ->
            statement.setString(1, tableName)
            statement.executeQuery().use { rs ->
                assertTrue(rs.next(), "Index count query must return one row for $tableName")
                return rs.getInt(1)
            }
        }
    }

    private fun java.sql.Connection.hasColumns(tableName: String, expectedColumns: List<String>): Boolean {
        val existing = mutableSetOf<String>()
        metaData.getColumns(null, null, tableName, null).use { rs ->
            while (rs.next()) {
                existing += rs.getString("COLUMN_NAME")
            }
        }
        return expectedColumns.all { it in existing }
    }
}
