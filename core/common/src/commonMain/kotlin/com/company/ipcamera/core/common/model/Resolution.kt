package com.company.ipcamera.core.common.model

import kotlinx.serialization.Serializable

/**
 * Модель разрешения видео
 */
@Serializable
data class Resolution(
    val width: Int,
    val height: Int
) {
    override fun toString(): String = "${width}x${height}"
}

