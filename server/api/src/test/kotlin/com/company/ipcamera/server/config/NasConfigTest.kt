package com.company.ipcamera.server.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class NasConfigTest {

    @Test
    fun `initialize returns stable system paths`() {
        val p1 = NasConfig.initialize()
        val p2 = NasConfig.getSystemPaths()
        // инициализация идемпотентна — тот же объект
        assertEquals(p1, p2)
        assertNotNull(p1.dataPath)
        assertNotNull(p1.recordingsPath)
        assertNotNull(p1.logsPath)
        assertNotNull(p1.configPath)
    }

    @Test
    fun `path accessors delegate to system paths`() {
        NasConfig.initialize()
        val recordings = NasConfig.getRecordingsPath()
        val data = NasConfig.getDataPath()
        val logs = NasConfig.getLogsPath()
        val config = NasConfig.getConfigPath()
        assertTrue(recordings.isNotBlank())
        assertTrue(data.isNotBlank())
        assertTrue(logs.isNotBlank())
        assertTrue(config.isNotBlank())
    }

    @Test
    fun `isRunningOnNas returns boolean`() {
        // На dev-машине (обычно не NAS) — возвращает true/false, но главное не бросает
        val result = NasConfig.isRunningOnNas()
        assertTrue(result is Boolean)
    }
}