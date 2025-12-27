package com.company.ipcamera.core.network.native

import com.company.ipcamera.core.network.native.codecs.*

/**
 * Kotlin обертка для работы с кодеками
 */
object CodecsWrapper {
    /**
     * Получает информацию о кодеке
     */
    fun getCodecInfo(type: CodecType): CodecInfo? {
        return memScoped {
            val info = alloc<CodecInfoVar>()
            if (codec_get_info(type, info.ptr)) {
                CodecInfo(
                    type = info.pointed.type,
                    name = info.pointed.name?.toKString() ?: "",
                    hardwareAccelerated = info.pointed.hardwareAccelerated,
                    maxWidth = info.pointed.maxWidth,
                    maxHeight = info.pointed.maxHeight
                )
            } else {
                null
            }
        }
    }

    /**
     * Проверяет поддержку кодека
     */
    fun isCodecSupported(type: CodecType): Boolean {
        return codec_is_supported(type)
    }

    /**
     * Проверяет наличие аппаратного ускорения для кодека
     */
    fun hasHardwareAcceleration(type: CodecType): Boolean {
        return codec_has_hardware_acceleration(type)
    }

    /**
     * Автоматически выбирает лучший кодек
     * @param preferredType Предпочтительный тип кодека
     * @param preferHardware Предпочитать ли кодек с аппаратным ускорением
     * @return Выбранный тип кодека
     */
    fun selectBestCodec(preferredType: CodecType, preferHardware: Boolean = true): CodecType {
        return codec_select_best(preferredType, preferHardware)
    }
}

/**
 * Информация о кодеке
 */
data class CodecInfo(
    val type: CodecType,
    val name: String,
    val hardwareAccelerated: Boolean,
    val maxWidth: Int,
    val maxHeight: Int
)

