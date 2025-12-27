package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.core.network.OnvifClient
import com.company.ipcamera.core.network.PtzDirection
import com.company.ipcamera.shared.common.createHttpClientEngine
import com.company.ipcamera.shared.domain.model.Camera

/**
 * Use case для управления PTZ камерой через ONVIF
 */
class ControlPtzUseCase {
    /**
     * Выполнить PTZ команду
     */
    suspend operator fun invoke(
        camera: Camera,
        command: String,
        speed: Float = 0.5f
    ): Result<Boolean> {
        if (camera.ptz?.enabled != true) {
            return Result.failure(IllegalStateException("PTZ не включен для этой камеры"))
        }

        val engine = createHttpClientEngine()
        val onvifClient = OnvifClient(engine)
        return try {
            val deviceUrl = normalizeUrl(camera.url)
            val direction = when (command.lowercase()) {
                "up" -> PtzDirection.UP
                "down" -> PtzDirection.DOWN
                "left" -> PtzDirection.LEFT
                "right" -> PtzDirection.RIGHT
                "up_left" -> PtzDirection.UP_LEFT
                "up_right" -> PtzDirection.UP_RIGHT
                "down_left" -> PtzDirection.DOWN_LEFT
                "down_right" -> PtzDirection.DOWN_RIGHT
                "zoom_in" -> PtzDirection.ZOOM_IN
                "zoom_out" -> PtzDirection.ZOOM_OUT
                "stop" -> PtzDirection.STOP
                else -> return Result.failure(IllegalArgumentException("Неизвестная команда: $command"))
            }

            val success = when (direction) {
                PtzDirection.ZOOM_IN -> {
                    onvifClient.zoomIn(
                        url = deviceUrl,
                        speed = speed,
                        username = camera.username,
                        password = camera.password
                    )
                }
                PtzDirection.ZOOM_OUT -> {
                    onvifClient.zoomOut(
                        url = deviceUrl,
                        speed = speed,
                        username = camera.username,
                        password = camera.password
                    )
                }
                PtzDirection.STOP -> {
                    onvifClient.stopPtz(
                        url = deviceUrl,
                        username = camera.username,
                        password = camera.password
                    )
                }
                else -> {
                    onvifClient.movePtz(
                        url = deviceUrl,
                        direction = direction,
                        speed = speed,
                        username = camera.username,
                        password = camera.password
                    )
                }
            }

            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Не удалось выполнить PTZ команду"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            onvifClient.close()
        }
    }

    private fun normalizeUrl(url: String): String {
        return if (url.startsWith("rtsp://")) {
            url.replace("rtsp://", "http://").substringBefore("/")
        } else if (!url.startsWith("http")) {
            "http://$url"
        } else {
            url
        }
    }
}

