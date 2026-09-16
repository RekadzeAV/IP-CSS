package com.company.ipcamera.core.network.onvif

import kotlinx.serialization.Serializable

/**
 * Диапазон значений Float (сериализуемый аналог для ONVIF).
 */
@Serializable
data class FloatRange(val min: Float, val max: Float)

/**
 * Настройки изображения ONVIF
 */
@Serializable
data class OnvifImagingSettings(
    /**
     * Яркость (0.0 - 1.0)
     */
    val brightness: Float? = null,

    /**
     * Контрастность (0.0 - 1.0)
     */
    val contrast: Float? = null,

    /**
     * Насыщенность (0.0 - 1.0)
     */
    val colorSaturation: Float? = null,

    /**
     * Резкость (0.0 - 1.0)
     */
    val sharpness: Float? = null,

    /**
     * Настройки экспозиции
     */
    val exposure: OnvifExposureSettings? = null,

    /**
     * Настройки фокуса
     */
    val focus: OnvifFocusSettings? = null,

    /**
     * Настройки баланса белого
     */
    val whiteBalance: OnvifWhiteBalanceSettings? = null,

    /**
     * Настройки широкого динамического диапазона (WDR)
     */
    val wideDynamicRange: OnvifWideDynamicRangeSettings? = null,

    /**
     * Настройки компенсации задней подсветки
     */
    val backlightCompensation: OnvifBacklightCompensationSettings? = null
)

/**
 * Настройки экспозиции
 */
@Serializable
data class OnvifExposureSettings(
    /**
     * Режим экспозиции
     */
    val mode: ExposureMode = ExposureMode.AUTO,

    /**
     * Приоритет экспозиции (для режима AUTO)
     */
    val priority: ExposurePriority = ExposurePriority.LowNoise,

    /**
     * Время экспозиции в секундах (для ручного режима)
     */
    val exposureTime: Float? = null,

    /**
     * Значение диафрагмы (F-number)
     */
    val iris: Float? = null,

    /**
     * Значение ISO/Gain
     */
    val gain: Float? = null,

    /**
     * Минимальная экспозиция
     */
    val minExposureTime: Float? = null,

    /**
     * Максимальная экспозиция
     */
    val maxExposureTime: Float? = null
)

/**
 * Режим экспозиции
 */
@Serializable
enum class ExposureMode {
    /**
     * Автоматический режим
     */
    AUTO,

    /**
     * Ручной режим
     */
    MANUAL
}

/**
 * Приоритет экспозиции
 */
@Serializable
enum class ExposurePriority {
    /**
     * Низкий шум
     */
    LowNoise,

    /**
     * Высокая скорость
     */
    FrameRate
}

/**
 * Настройки фокуса
 */
@Serializable
data class OnvifFocusSettings(
    /**
     * Режим фокуса
     */
    val mode: FocusMode = FocusMode.AUTO,

    /**
     * Позиция фокуса (для ручного режима, 0.0 - 1.0)
     */
    val position: Float? = null,

    /**
     * Скорость автофокуса
     */
    val autoFocusSpeed: AutoFocusSpeed? = null
)

/**
 * Режим фокуса
 */
@Serializable
enum class FocusMode {
    /**
     * Автоматический фокус
     */
    AUTO,

    /**
     * Ручной фокус
     */
    MANUAL
}

/**
 * Скорость автофокуса
 */
@Serializable
enum class AutoFocusSpeed {
    /**
     * Нормальная скорость
     */
    NORMAL,

    /**
     * Высокая скорость
     */
    FAST
}

/**
 * Настройки баланса белого
 */
@Serializable
data class OnvifWhiteBalanceSettings(
    /**
     * Режим баланса белого
     */
    val mode: WhiteBalanceMode = WhiteBalanceMode.AUTO,

    /**
     * Температура цвета в Кельвинах (для ручного режима)
     */
    val colorTemperature: Float? = null,

    /**
     * Смещение красного (для ручного режима)
     */
    val crGain: Float? = null,

    /**
     * Смещение синего (для ручного режима)
     */
    val cbGain: Float? = null
)

/**
 * Режим баланса белого
 */
@Serializable
enum class WhiteBalanceMode {
    /**
     * Автоматический баланс белого
     */
    AUTO,

    /**
     * Ручной баланс белого
     */
    MANUAL
}

/**
 * Настройки широкого динамического диапазона (WDR)
 */
@Serializable
data class OnvifWideDynamicRangeSettings(
    /**
     * Включен ли WDR
     */
    val enabled: Boolean = false,

    /**
     * Уровень WDR (0.0 - 1.0)
     */
    val level: Float? = null
)

/**
 * Настройки компенсации задней подсветки
 */
@Serializable
data class OnvifBacklightCompensationSettings(
    /**
     * Включена ли компенсация
     */
    val enabled: Boolean = false,

    /**
     * Режим компенсации
     */
    val mode: BacklightCompensationMode = BacklightCompensationMode.OFF
)

/**
 * Режим компенсации задней подсветки
 */
@Serializable
enum class BacklightCompensationMode {
    /**
     * Выключено
     */
    OFF,

    /**
     * Включено
     */
    ON
}

/**
 * Опции для настроек изображения
 */
@Serializable
data class OnvifImagingOptions(
    /**
     * Диапазон яркости
     */
    val brightnessRange: FloatRange? = null,

    /**
     * Диапазон контрастности
     */
    val contrastRange: FloatRange? = null,

    /**
     * Диапазон насыщенности
     */
    val colorSaturationRange: FloatRange? = null,

    /**
     * Диапазон резкости
     */
    val sharpnessRange: FloatRange? = null,

    /**
     * Поддерживаемые режимы экспозиции
     */
    val supportedExposureModes: List<ExposureMode> = emptyList(),

    /**
     * Диапазон времени экспозиции
     */
    val exposureTimeRange: FloatRange? = null,

    /**
     * Диапазон диафрагмы
     */
    val irisRange: FloatRange? = null,

    /**
     * Диапазон усиления
     */
    val gainRange: FloatRange? = null,

    /**
     * Поддерживаемые режимы фокуса
     */
    val supportedFocusModes: List<FocusMode> = emptyList(),

    /**
     * Диапазон позиции фокуса
     */
    val focusPositionRange: FloatRange? = null,

    /**
     * Поддерживаемые режимы баланса белого
     */
    val supportedWhiteBalanceModes: List<WhiteBalanceMode> = emptyList(),

    /**
     * Диапазон температуры цвета
     */
    val colorTemperatureRange: FloatRange? = null
)

/**
 * Опции для перемещения (PTZ)
 */
@Serializable
data class OnvifMoveOptions(
    /**
     * Поддерживается ли перемещение
     */
    val supported: Boolean = false,

    /**
     * Скорость перемещения
     */
    val speed: FloatRange? = null
)

/**
 * Статус Imaging Service
 */
@Serializable
data class OnvifImagingStatus(
    /**
     * Токен видеоисточника
     */
    val videoSourceToken: String,

    /**
     * Статус фокуса
     */
    val focusStatus: FocusStatus? = null,

    /**
     * Позиция фокуса
     */
    val focusPosition: Float? = null
)

/**
 * Статус фокуса
 */
@Serializable
enum class FocusStatus {
    /**
     * Фокус установлен
     */
    IDLE,

    /**
     * Фокус в процессе
     */
    MOVING,

    /**
     * Фокус не установлен
     */
    FAILED
}

/**
 * Пресет изображения
 */
@Serializable
data class OnvifImagingPreset(
    /**
     * Токен пресета
     */
    val token: String,

    /**
     * Имя пресета
     */
    val name: String,

    /**
     * Настройки изображения пресета
     */
    val settings: OnvifImagingSettings? = null
)
