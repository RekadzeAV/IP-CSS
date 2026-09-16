package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.OnvifClient
import com.company.ipcamera.core.network.PtzDirection
import com.company.ipcamera.shared.domain.model.Camera
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Сервис реального управления камерами (PTZ) через ONVIF.
 *
 * Заменяет прежнюю стаб-реализацию POST /api/v1/cameras/{id}/control:
 * команды транслируются в ONVIF-вызовы (ContinuousMove/Stop/GotoPreset/Zoom)
 * через [OnvifClient]. Требуется camera.ptz.enabled == true.
 *
 * Таймаут каждой ONVIF-команды — [commandTimeoutMs], чтобы HTTP-запрос
 * не зависал на недоступной камере.
 */
class CameraControlService(
    private val onvifClient: OnvifClient,
    private val commandTimeoutMs: Long = DEFAULT_COMMAND_TIMEOUT_MS,
) {

    sealed class ControlResult {
        data class Success(val message: String) : ControlResult()
        data class Failure(val message: String) : ControlResult()
    }

    /**
     * Выполнить команду управления камерой.
     *
     * @param camera камера (url/credentials берутся из модели)
     * @param action имя команды (up/down/left/right/…/zoom_in/zoom_out/stop/preset)
     * @param parameters параметры команды (speed, preset)
     */
    suspend fun execute(
        camera: Camera,
        action: String,
        parameters: Map<String, String> = emptyMap(),
    ): ControlResult {
        if (camera.ptz?.enabled != true) {
            return ControlResult.Failure(
                "PTZ is not enabled for camera ${camera.id}. Enable it in camera settings first."
            )
        }

        val speed = parameters["speed"]?.toFloatOrNull()?.coerceIn(MIN_SPEED, MAX_SPEED)
            ?: DEFAULT_SPEED
        val username = camera.username
        val password = camera.password

        return try {
            withTimeout(commandTimeoutMs) {
                executeInternal(camera, action.lowercase().trim(), speed, username, password, parameters)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            logger.warn { "PTZ command '$action' timed out after ${commandTimeoutMs}ms for camera ${camera.id}" }
            ControlResult.Failure("Control command '$action' timed out (${commandTimeoutMs}ms)")
        } catch (e: Exception) {
            logger.error(e) { "PTZ command '$action' failed for camera ${camera.id}: ${e.message}" }
            ControlResult.Failure("Control command '$action' failed: ${e.message}")
        }
    }

    private suspend fun executeInternal(
        camera: Camera,
        normalizedAction: String,
        speed: Float,
        username: String?,
        password: String?,
        parameters: Map<String, String>,
    ): ControlResult {
        val direction = directionFor(normalizedAction)
        val ok = when {
            direction != null ->
                onvifClient.movePtz(
                    url = camera.url,
                    direction = direction,
                    speed = speed,
                    username = username,
                    password = password,
                )

            normalizedAction in ZOOM_IN_ACTIONS ->
                onvifClient.zoomIn(camera.url, speed, username = username, password = password)

            normalizedAction in ZOOM_OUT_ACTIONS ->
                onvifClient.zoomOut(camera.url, speed, username = username, password = password)

            normalizedAction == ACTION_STOP ->
                onvifClient.stopPtz(camera.url, username = username, password = password)

            normalizedAction in PRESET_ACTIONS -> {
                val preset = parameters["preset"]
                    ?: parameters["presetToken"]
                    ?: parameters["token"]
                if (preset.isNullOrBlank()) {
                    return ControlResult.Failure(
                        "Preset command requires 'preset' (or 'presetToken') parameter"
                    )
                }
                onvifClient.gotoPreset(
                    url = camera.url,
                    presetToken = preset,
                    username = username,
                    password = password,
                )
            }

            else ->
                return ControlResult.Failure(
                    "Unsupported action '$normalizedAction'. Supported: " +
                        "up, down, left, right, up_left, up_right, down_left, down_right, " +
                        "zoom_in, zoom_out, stop, preset"
                )
        }

        return if (ok) {
            ControlResult.Success("Control command '$normalizedAction' executed for camera ${camera.id}")
        } else {
            ControlResult.Failure(
                "Camera ${camera.id} rejected command '$normalizedAction' " +
                    "(device unreachable, PTZ service unavailable, or auth failed)"
            )
        }
    }

    private fun directionFor(action: String): PtzDirection? = when (action) {
        "up" -> PtzDirection.UP
        "down" -> PtzDirection.DOWN
        "left" -> PtzDirection.LEFT
        "right" -> PtzDirection.RIGHT
        "up_left", "upleft", "up-left" -> PtzDirection.UP_LEFT
        "up_right", "upright", "up-right" -> PtzDirection.UP_RIGHT
        "down_left", "downleft", "down-left" -> PtzDirection.DOWN_LEFT
        "down_right", "downright", "down-right" -> PtzDirection.DOWN_RIGHT
        else -> null
    }

    companion object {
        const val DEFAULT_COMMAND_TIMEOUT_MS = 10_000L
        const val DEFAULT_SPEED = 0.5f
        const val MIN_SPEED = 0.1f
        const val MAX_SPEED = 1.0f

        const val ACTION_STOP = "stop"
        val ZOOM_IN_ACTIONS = setOf("zoom_in", "zoomin", "zoom-in")
        val ZOOM_OUT_ACTIONS = setOf("zoom_out", "zoomout", "zoom-out")
        val PRESET_ACTIONS = setOf("preset", "goto_preset", "goto-preset", "gotopreset")
    }
}
