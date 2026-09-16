package com.company.ipcamera.server.config

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Тесты DatabaseConfig без внешнего PostgreSQL.
 * В CI/локально env-переменные (DATABASE_URL и т.п.) не заданы — проверяем
 * дефолтное (не-prod, embedded) поведение и preflight-деградацию.
 */
class DatabaseConfigTest {

    // Внимание: эти тесты зависят от отсутствия ENVIRONMENT/NODE_ENV/DATABASE_* в env процесса.
    // Локально по умолчанию их нет, поэтому тесты детерминированы.

    @Test
    fun `isProductionEnvironment is false without env`() {
        assertFalse(DatabaseConfig.isProductionEnvironment())
    }

    @Test
    fun `isPostgresModeEnabled is false without DB_MODE`() {
        assertFalse(DatabaseConfig.isPostgresModeEnabled())
    }

    @Test
    fun `isPostgresConfigured is false without postgres url`() {
        val configured = DatabaseConfig.isPostgresConfigured()
        // Проверяем, что метод не бросает и возвращает boolean (ожидаем false при чистом env)
        assertFalse(configured)
    }

    @Test
    fun `validateDatabaseRequirements does not block in dev mode`() {
        // Не production + не postgres mode -> метод должен вернуться без require-блокировок
        DatabaseConfig.validateDatabaseRequirementsForServerStartup()
        // Если работает — значит preflight не заблокировал (dev-режим)
    }

    @Test
    fun `data source is null before initialization`() {
        assertNull(DatabaseConfig.getDataSource())
        assertNull(DatabaseConfig.getReadReplicaDataSource())
    }

    @Test
    fun `closeDataSource is no-op when nothing initialized`() {
        // Не должно бросать исключение при отсутствии пулов
        DatabaseConfig.closeDataSource()
    }
}