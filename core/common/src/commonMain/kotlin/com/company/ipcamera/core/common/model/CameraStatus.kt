package com.company.ipcamera.core.common.model

import kotlinx.serialization.Serializable

/**
 * Статус камеры
 */
@Serializable
enum class CameraStatus {
    ONLINE,
    OFFLINE,
    ERROR,
    CONNECTING,
    UNKNOWN
}



