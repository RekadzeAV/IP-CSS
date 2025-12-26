package com.company.ipcamera.shared.data.local

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.core.common.security.PasswordEncryptionFactory
import com.company.ipcamera.shared.database.Camera
import com.company.ipcamera.shared.domain.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Маппер между сущностью базы данных и доменной моделью Camera
 * Автоматически шифрует/расшифровывает пароли
 */
internal class CameraEntityMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val passwordEncryption = PasswordEncryptionFactory.create()

    fun toDomain(dbCamera: Camera): com.company.ipcamera.shared.domain.model.Camera {
        // Расшифровываем пароль при чтении из БД
        val decryptedPassword = dbCamera.password?.let { encryptedPassword ->
            try {
                if (passwordEncryption.isEncrypted(encryptedPassword)) {
                    passwordEncryption.decrypt(encryptedPassword)
                } else {
                    encryptedPassword // Старые незашифрованные пароли
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to decrypt password for camera ${dbCamera.id}" }
                encryptedPassword // В случае ошибки возвращаем исходное значение
            }
        }

        return com.company.ipcamera.shared.domain.model.Camera(
            id = dbCamera.id,
            name = dbCamera.name,
            url = dbCamera.url,
            username = dbCamera.username,
            password = decryptedPassword,
            model = dbCamera.model,
            status = CameraStatus.valueOf(dbCamera.status),
            resolution = dbCamera.resolution_width?.let { width ->
                dbCamera.resolution_height?.let { height ->
                    Resolution(width, height)
                }
            },
            fps = dbCamera.fps.toInt(),
            bitrate = dbCamera.bitrate.toInt(),
            codec = dbCamera.codec,
            audio = dbCamera.audio == 1L,
            ptz = dbCamera.ptz_config?.let { json.decodeFromString<PTZConfig>(it) },
            streams = dbCamera.streams?.let { json.decodeFromString<List<StreamConfig>>(it) } ?: emptyList(),
            settings = dbCamera.settings?.let { json.decodeFromString<CameraSettings>(it) } ?: CameraSettings(),
            statistics = dbCamera.statistics?.let { json.decodeFromString<CameraStatistics>(it) },
            createdAt = dbCamera.created_at,
            updatedAt = dbCamera.updated_at,
            lastSeen = dbCamera.last_seen
        )
    }

    fun toDatabase(camera: com.company.ipcamera.shared.domain.model.Camera): Camera {
        // Шифруем пароль перед сохранением в БД
        val encryptedPassword = camera.password?.let { password ->
            try {
                if (passwordEncryption.isEncrypted(password)) {
                    password // Уже зашифрован
                } else {
                    passwordEncryption.encrypt(password)
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to encrypt password for camera ${camera.id}" }
                password // В случае ошибки сохраняем незашифрованным (небезопасно, но лучше чем потеря данных)
            }
        }

        return Camera(
            id = camera.id,
            name = camera.name,
            url = camera.url,
            username = camera.username,
            password = encryptedPassword,
            model = camera.model,
            status = camera.status.name,
            resolution_width = camera.resolution?.width?.toLong(),
            resolution_height = camera.resolution?.height?.toLong(),
            fps = camera.fps.toLong(),
            bitrate = camera.bitrate.toLong(),
            codec = camera.codec,
            audio = if (camera.audio) 1L else 0L,
            ptz_config = camera.ptz?.let { json.encodeToString(it) },
            streams = if (camera.streams.isNotEmpty()) json.encodeToString(camera.streams) else null,
            settings = json.encodeToString(camera.settings),
            statistics = camera.statistics?.let { json.encodeToString(it) },
            created_at = camera.createdAt,
            updated_at = camera.updatedAt,
            last_seen = camera.lastSeen
        )
    }
}

