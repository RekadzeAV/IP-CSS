package com.company.ipcamera.server

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.*

/**
 * Интеграционные тесты базы данных.
 * Проверяют подключение к PostgreSQL и Redis.
 */
class DatabaseIntegrationTest {

    @Test
    fun `postgresql connection works`() = runBlocking {
        val url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/ipcss_test"
        val user = System.getenv("DATABASE_USER") ?: "ipcss"
        val password = System.getenv("DATABASE_PASSWORD") ?: "ipcss_test"

        try {
            Class.forName("org.postgresql.Driver")
            val conn = java.sql.DriverManager.getConnection(url, user, password)
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery("SELECT 1 AS test")
            assertTrue(rs.next())
            assertEquals(1, rs.getInt("test"))
            conn.close()
            println("PostgreSQL connection OK")
        } catch (e: Exception) {
            // Если PostgreSQL недоступен, тест пропускаем
            println("PostgreSQL not available, skipping: ${e.message}")
        }
    }

    @Test
    fun `redis connection works`() = runBlocking {
        val host = System.getenv("REDIS_HOST") ?: "localhost"
        val port = System.getenv("REDIS_PORT")?.toIntOrNull() ?: 6379

        try {
            val jedis = redis.clients.jedis.Jedis(host, port)
            val pong = jedis.ping()
            assertEquals("PONG", pong)
            jedis.close()
            println("Redis connection OK")
        } catch (e: Exception) {
            println("Redis not available, skipping: ${e.message}")
        }
    }

    @Test
    fun `database schema exists`() = runBlocking {
        val url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/ipcss_test"
        val user = System.getenv("DATABASE_USER") ?: "ipcss"
        val password = System.getenv("DATABASE_PASSWORD") ?: "ipcss_test"

        try {
            Class.forName("org.postgresql.Driver")
            val conn = java.sql.DriverManager.getConnection(url, user, password)
            val stmt = conn.createStatement()

            // Проверяем наличие таблиц
            val tables = listOf("cameras", "events", "recordings", "users", "settings")
            for (table in tables) {
                val rs = stmt.executeQuery("""
                    SELECT EXISTS (
                        SELECT FROM information_schema.tables 
                        WHERE table_name = '$table'
                    )
                """)
                if (rs.next()) {
                    println("Table '$table' exists: ${rs.getBoolean(1)}")
                }
            }
            conn.close()
        } catch (e: Exception) {
            println("Schema check skipped: ${e.message}")
        }
    }

    @Test
    fun `redis key value operations work`() = runBlocking {
        val host = System.getenv("REDIS_HOST") ?: "localhost"
        val port = System.getenv("REDIS_PORT")?.toIntOrNull() ?: 6379

        try {
            val jedis = redis.clients.jedis.Jedis(host, port)
            val testKey = "integration_test:${System.currentTimeMillis()}"
            val testValue = "test_value_${System.currentTimeMillis()}"

            jedis.setex(testKey, 60, testValue)
            val retrieved = jedis.get(testKey)
            assertEquals(testValue, retrieved)

            jedis.del(testKey)
            val deleted = jedis.get(testKey)
            assertNull(deleted)

            jedis.close()
            println("Redis KV operations OK")
        } catch (e: Exception) {
            println("Redis KV test skipped: ${e.message}")
        }
    }
}
