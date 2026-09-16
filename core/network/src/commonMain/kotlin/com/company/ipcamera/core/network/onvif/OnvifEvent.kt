package com.company.ipcamera.core.network.onvif

import kotlinx.serialization.Serializable

/**
 * Модель события ONVIF
 */
@Serializable
data class OnvifEvent(
    /**
     * Topic события (например, "tns1:VideoSource/MotionAlarm")
     */
    val topic: String,

    /**
     * Время события (Unix timestamp в миллисекундах)
     */
    val timestamp: Long,

    /**
     * Сообщение события
     */
    val message: String? = null,

    /**
     * Свойства события (ключ-значение пары)
     */
    val properties: Map<String, String> = emptyMap(),

    /**
     * Источник события (URL камеры)
     */
    val source: String? = null,

    /**
     * Данные события (XML или JSON)
     */
    val data: String? = null
)

/**
 * Типы событий ONVIF (основные)
 */
object OnvifEventType {
    // Видео события
    const val MOTION_ALARM = "tns1:VideoSource/MotionAlarm"
    const val VIDEO_SOURCE_CONFIGURATION = "tns1:VideoSource/VideoSourceConfiguration"
    const val VIDEO_SOURCE_LOST = "tns1:VideoSource/VideoSourceLost"
    const val VIDEO_SOURCE_RECOVERED = "tns1:VideoSource/VideoSourceRecovered"

    // События правил
    const val LINE_DETECTOR_CROSSED = "tns1:RuleEngine/LineDetector/Crossed"
    const val INTRUSION_DETECTOR = "tns1:RuleEngine/IntrusionDetector"
    const val LOITERING_DETECTOR = "tns1:RuleEngine/LoiteringDetector"

    // События устройства
    const val DEVICE_IO_PORT_STATE = "tns1:Device/IO/PortState"
    const val DEVICE_TAMPER_DETECTED = "tns1:Device/TamperDetected"
    const val SYSTEM_DATE_TIME_CHANGED = "tns1:System/SystemDateTimeChanged"

    // События аналитики
    const val ANALYTICS_STREAM = "tns1:Analytics/AnalyticsStream"
    const val CELL_MOTION_DETECTOR = "tns1:Analytics/CellMotionDetector"

    // Хранилище и аппаратные сбои (разные вендоры используют разные топики)
    const val DEVICE_HARDWARE_FAILURE = "tns1:Device/HardwareFailure"

    /**
     * Проверка, является ли событие детекцией движения
     */
    fun isMotionDetection(topic: String): Boolean {
        return topic.contains("Motion", ignoreCase = true) ||
            topic == MOTION_ALARM ||
            topic == CELL_MOTION_DETECTOR
    }

    /**
     * Проверка, является ли событие детекцией вторжения
     */
    fun isIntrusionDetection(topic: String): Boolean {
        return topic.equals(INTRUSION_DETECTOR, ignoreCase = true) ||
            topic.equals(LINE_DETECTOR_CROSSED, ignoreCase = true) ||
            topic.equals(LOITERING_DETECTOR, ignoreCase = true) ||
            topic.contains("IntrusionDetector", ignoreCase = true) ||
            topic.contains("LineDetector", ignoreCase = true) ||
            topic.contains("LoiteringDetector", ignoreCase = true)
    }

    /**
     * Проверка, является ли событие тревогой
     */
    fun isAlarm(topic: String): Boolean {
        return topic.contains("Alarm", ignoreCase = true) ||
            topic.contains("Detector", ignoreCase = true) ||
            topic == DEVICE_TAMPER_DETECTED
    }

    /**
     * Проверка, является ли событие потерей видеоисточника
     */
    fun isVideoSourceLost(topic: String): Boolean =
        topic.contains("VideoSourceLost", ignoreCase = true) || topic == VIDEO_SOURCE_LOST

    /**
     * Проверка, является ли событие восстановлением видеоисточника
     */
    fun isVideoSourceRecovered(topic: String): Boolean =
        topic.contains("VideoSourceRecovered", ignoreCase = true) || topic == VIDEO_SOURCE_RECOVERED

    /**
     * Проверка, является ли топик LineDetector/Crossed (пересечение линии)
     */
    fun isLineDetectorCrossed(topic: String): Boolean =
        topic == LINE_DETECTOR_CROSSED || topic.contains("LineDetector", ignoreCase = true)

    /**
     * Проверка, является ли топик LoiteringDetector (событие «слоняющийся»)
     */
    fun isLoiteringDetector(topic: String): Boolean =
        topic == LOITERING_DETECTOR || topic.contains("LoiteringDetector", ignoreCase = true)

    /**
     * Проверка, является ли топик системной ошибкой (System + Error), а не служебным событием вроде SystemDateTimeChanged
     */
    fun isSystemError(topic: String): Boolean =
        topic.contains("System", ignoreCase = true) && topic.contains("Error", ignoreCase = true)

    /**
     * Проверка, является ли топик состоянием порта ввода-вывода (Device/IO/PortState)
     */
    fun isDeviceIoPortState(topic: String): Boolean =
        topic == DEVICE_IO_PORT_STATE || topic.contains("PortState", ignoreCase = true)

    /**
     * Проверка, является ли топик изменением конфигурации видеоисточника
     */
    fun isVideoSourceConfiguration(topic: String): Boolean =
        topic == VIDEO_SOURCE_CONFIGURATION || topic.contains("VideoSourceConfiguration", ignoreCase = true)

    /**
     * Проверка, является ли топик изменением системной даты/времени
     */
    fun isSystemDateTimeChanged(topic: String): Boolean =
        topic == SYSTEM_DATE_TIME_CHANGED || topic.contains("SystemDateTimeChanged", ignoreCase = true)

    /**
     * Проверка, является ли топик потоком аналитики
     */
    fun isAnalyticsStream(topic: String): Boolean =
        topic == ANALYTICS_STREAM || topic.contains("AnalyticsStream", ignoreCase = true)

    /**
     * Проверка, является ли топик событием переполнения хранилища (Storage full / StorageFailure).
     */
    fun isStorageFull(topic: String): Boolean =
        (topic.contains("Storage", ignoreCase = true) && topic.contains("Full", ignoreCase = true)) ||
            topic.contains("StorageFailure", ignoreCase = true)

    /**
     * Проверка, является ли топик аппаратным сбоем устройства (HardwareFailure и подтипы).
     */
    fun isHardwareFailure(topic: String): Boolean =
        topic == DEVICE_HARDWARE_FAILURE || topic.contains("HardwareFailure", ignoreCase = true)
}
