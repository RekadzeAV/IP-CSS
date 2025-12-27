package com.company.ipcamera.shared.data.local

import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SettingsType
import com.company.ipcamera.shared.test.TestDataFactory
import kotlin.test.*

/**
 * Тесты для SettingsEntityMapper
 */
class SettingsEntityMapperTest {

    private val mapper = SettingsEntityMapper()

    @Test
    fun `test toDomain basic settings`() {
        // Arrange
        val dbSetting = com.company.ipcamera.shared.database.Setting(
            id = "setting-1",
            category = "SYSTEM",
            key = "test.key",
            value = "test-value",
            type = "STRING",
            description = "Test setting",
            updated_at = 1000L
        )

        // Act
        val domainSettings = mapper.toDomain(dbSetting)

        // Assert
        assertEquals("setting-1", domainSettings.id)
        assertEquals(SettingsCategory.SYSTEM, domainSettings.category)
        assertEquals("test.key", domainSettings.key)
        assertEquals("test-value", domainSettings.value)
        assertEquals(SettingsType.STRING, domainSettings.type)
        assertEquals("Test setting", domainSettings.description)
        assertEquals(1000L, domainSettings.updatedAt)
    }

    @Test
    fun `test toDatabase basic settings`() {
        // Arrange
        val domainSettings = TestDataFactory.createTestSettings(
            id = "setting-1",
            category = SettingsCategory.SYSTEM,
            key = "test.key",
            value = "test-value"
        )

        // Act
        val dbSetting = mapper.toDatabase(domainSettings)

        // Assert
        assertEquals("setting-1", dbSetting.id)
        assertEquals("SYSTEM", dbSetting.category)
        assertEquals("test.key", dbSetting.key)
        assertEquals("test-value", dbSetting.value)
    }

    @Test
    fun `test round trip conversion`() {
        // Arrange
        val originalSettings = TestDataFactory.createTestSettings(
            id = "setting-1",
            category = SettingsCategory.RECORDING,
            key = "recording.path",
            value = "/recordings",
            type = SettingsType.STRING
        )

        // Act
        val dbSetting = mapper.toDatabase(originalSettings)
        val convertedSettings = mapper.toDomain(dbSetting)

        // Assert
        assertEquals(originalSettings.id, convertedSettings.id)
        assertEquals(originalSettings.category, convertedSettings.category)
        assertEquals(originalSettings.key, convertedSettings.key)
        assertEquals(originalSettings.value, convertedSettings.value)
        assertEquals(originalSettings.type, convertedSettings.type)
    }

    @Test
    fun `test toDomain all categories`() {
        val categories = SettingsCategory.values()

        categories.forEach { category ->
            val dbSetting = com.company.ipcamera.shared.database.Setting(
                id = "setting-$category",
                category = category.name,
                key = "test.key",
                value = "test-value",
                type = "STRING",
                description = null,
                updated_at = System.currentTimeMillis()
            )

            val domainSettings = mapper.toDomain(dbSetting)
            assertEquals(category, domainSettings.category)
        }
    }

    @Test
    fun `test toDomain all types`() {
        val types = SettingsType.values()

        types.forEach { type ->
            val dbSetting = com.company.ipcamera.shared.database.Setting(
                id = "setting-$type",
                category = "SYSTEM",
                key = "test.key",
                value = "test-value",
                type = type.name,
                description = null,
                updated_at = System.currentTimeMillis()
            )

            val domainSettings = mapper.toDomain(dbSetting)
            assertEquals(type, domainSettings.type)
        }
    }
}


