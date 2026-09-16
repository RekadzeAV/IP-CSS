package com.company.ipcamera.core.network.onvif

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Парсер для ONVIF Imaging Service ответов
 */
object OnvifImagingParser {

    /**
     * Парсинг ответа GetImagingSettings
     */
    fun parseGetImagingSettingsResponse(xml: String): Result<OnvifImagingSettings> {
        return try {
            val brightness = extractFloatValue(xml, "Brightness", "timg:Brightness")
            val contrast = extractFloatValue(xml, "Contrast", "timg:Contrast")
            val colorSaturation = extractFloatValue(xml, "ColorSaturation", "timg:ColorSaturation")
            val sharpness = extractFloatValue(xml, "Sharpness", "timg:Sharpness")

            val exposure = parseExposureSettings(xml)
            val focus = parseFocusSettings(xml)
            val whiteBalance = parseWhiteBalanceSettings(xml)
            val wideDynamicRange = parseWideDynamicRangeSettings(xml)
            val backlightCompensation = parseBacklightCompensationSettings(xml)

            val settings = OnvifImagingSettings(
                brightness = brightness,
                contrast = contrast,
                colorSaturation = colorSaturation,
                sharpness = sharpness,
                exposure = exposure,
                focus = focus,
                whiteBalance = whiteBalance,
                wideDynamicRange = wideDynamicRange,
                backlightCompensation = backlightCompensation
            )

            Result.success(settings)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse GetImagingSettings response" }
            Result.failure(e)
        }
    }

    /**
     * Парсинг ответа GetOptions
     */
    fun parseGetOptionsResponse(xml: String): Result<OnvifImagingOptions> {
        return try {
            val brightnessRange = extractFloatRange(xml, "Brightness", "timg:Brightness")
            val contrastRange = extractFloatRange(xml, "Contrast", "timg:Contrast")
            val colorSaturationRange = extractFloatRange(xml, "ColorSaturation", "timg:ColorSaturation")
            val sharpnessRange = extractFloatRange(xml, "Sharpness", "timg:Sharpness")

            val exposureTimeRange = extractFloatRange(xml, "ExposureTime", "timg:ExposureTime")
            val irisRange = extractFloatRange(xml, "Iris", "timg:Iris")
            val gainRange = extractFloatRange(xml, "Gain", "timg:Gain")
            val focusPositionRange = extractFloatRange(xml, "FocusPosition", "timg:FocusPosition")
            val colorTemperatureRange = extractFloatRange(xml, "ColorTemperature", "timg:ColorTemperature")

            val options = OnvifImagingOptions(
                brightnessRange = brightnessRange,
                contrastRange = contrastRange,
                colorSaturationRange = colorSaturationRange,
                sharpnessRange = sharpnessRange,
                exposureTimeRange = exposureTimeRange,
                irisRange = irisRange,
                gainRange = gainRange,
                focusPositionRange = focusPositionRange,
                colorTemperatureRange = colorTemperatureRange
            )

            Result.success(options)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse GetOptions response" }
            Result.failure(e)
        }
    }

    /**
     * Парсинг ответа GetMoveOptions
     */
    fun parseGetMoveOptionsResponse(xml: String): Result<OnvifMoveOptions> {
        return try {
            val supported = extractValue(xml, "Supported", "timg:Supported")?.toBoolean() ?: false
            val speedRange = extractFloatRange(xml, "Speed", "timg:Speed")

            val options = OnvifMoveOptions(
                supported = supported,
                speed = speedRange
            )

            Result.success(options)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse GetMoveOptions response" }
            Result.failure(e)
        }
    }

    /**
     * Парсинг ответа GetStatus
     */
    fun parseGetStatusResponse(xml: String): Result<OnvifImagingStatus> {
        return try {
            val videoSourceToken = extractValue(xml, "VideoSourceToken", "timg:VideoSourceToken")
                ?: return Result.failure(IllegalArgumentException("VideoSourceToken not found"))

            val focusStatusStr = extractValue(xml, "FocusStatus", "timg:FocusStatus")
            val focusStatus = when (focusStatusStr?.uppercase()) {
                "IDLE" -> FocusStatus.IDLE
                "MOVING" -> FocusStatus.MOVING
                "FAILED" -> FocusStatus.FAILED
                else -> null
            }

            val focusPosition = extractFloatValue(xml, "FocusPosition", "timg:FocusPosition")

            val status = OnvifImagingStatus(
                videoSourceToken = videoSourceToken,
                focusStatus = focusStatus,
                focusPosition = focusPosition
            )

            Result.success(status)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse GetStatus response" }
            Result.failure(e)
        }
    }

    /**
     * Парсинг ответа GetPresets
     */
    fun parseGetPresetsResponse(xml: String): Result<List<OnvifImagingPreset>> {
        return try {
            val presets = mutableListOf<OnvifImagingPreset>()

            val presetRegex = Regex(
                """(?s)<timg:Preset[^>]*Token\s*=\s*["']([^"']+)["'][^>]*>(.*?)</timg:Preset>"""
            )

            val matches = presetRegex.findAll(xml)
            for (match in matches) {
                val token = match.groupValues.getOrNull(1)
                val presetXml = match.groupValues.getOrNull(2) ?: ""

                if (token != null) {
                    val name = extractValue(presetXml, "Name", "timg:Name") ?: "Preset $token"
                    val settings = parseGetImagingSettingsResponse(presetXml).getOrNull()

                    presets.add(
                        OnvifImagingPreset(
                            token = token,
                            name = name,
                            settings = settings
                        )
                    )
                }
            }

            Result.success(presets)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse GetPresets response" }
            Result.failure(e)
        }
    }

    /**
     * Парсинг ответа SetPreset
     */
    fun parseSetPresetResponse(xml: String): Result<String> {
        return try {
            val token = extractValue(xml, "PresetToken", "timg:PresetToken")
                ?: return Result.failure(IllegalArgumentException("PresetToken not found"))

            Result.success(token)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse SetPreset response" }
            Result.failure(e)
        }
    }

    // Вспомогательные методы парсинга

    private fun parseExposureSettings(xml: String): OnvifExposureSettings? {
        val modeStr = extractValue(xml, "Mode", "timg:Mode")
        val mode = when (modeStr?.uppercase()) {
            "AUTO" -> ExposureMode.AUTO
            "MANUAL" -> ExposureMode.MANUAL
            else -> ExposureMode.AUTO
        }

        val exposureTime = extractFloatValue(xml, "ExposureTime", "timg:ExposureTime")
        val iris = extractFloatValue(xml, "Iris", "timg:Iris")
        val gain = extractFloatValue(xml, "Gain", "timg:Gain")

        return OnvifExposureSettings(
            mode = mode,
            exposureTime = exposureTime,
            iris = iris,
            gain = gain
        )
    }

    private fun parseFocusSettings(xml: String): OnvifFocusSettings? {
        val modeStr = extractValue(xml, "AutoFocusMode", "timg:AutoFocusMode")
        val mode = when (modeStr?.uppercase()) {
            "AUTO" -> FocusMode.AUTO
            "MANUAL" -> FocusMode.MANUAL
            else -> FocusMode.AUTO
        }

        val position = extractFloatValue(xml, "Position", "timg:Position")

        return OnvifFocusSettings(
            mode = mode,
            position = position
        )
    }

    private fun parseWhiteBalanceSettings(xml: String): OnvifWhiteBalanceSettings? {
        val modeStr = extractValue(xml, "Mode", "timg:Mode")
        val mode = when (modeStr?.uppercase()) {
            "AUTO" -> WhiteBalanceMode.AUTO
            "MANUAL" -> WhiteBalanceMode.MANUAL
            else -> WhiteBalanceMode.AUTO
        }

        val colorTemperature = extractFloatValue(xml, "ColorTemperature", "timg:ColorTemperature")

        return OnvifWhiteBalanceSettings(
            mode = mode,
            colorTemperature = colorTemperature
        )
    }

    private fun parseWideDynamicRangeSettings(xml: String): OnvifWideDynamicRangeSettings? {
        val enabled = extractValue(xml, "Enabled", "timg:Enabled")?.toBoolean() ?: false
        val level = extractFloatValue(xml, "Level", "timg:Level")

        return OnvifWideDynamicRangeSettings(
            enabled = enabled,
            level = level
        )
    }

    private fun parseBacklightCompensationSettings(xml: String): OnvifBacklightCompensationSettings? {
        val modeStr = extractValue(xml, "Mode", "timg:Mode")
        val mode = when (modeStr?.uppercase()) {
            "ON" -> BacklightCompensationMode.ON
            "OFF" -> BacklightCompensationMode.OFF
            else -> BacklightCompensationMode.OFF
        }

        return OnvifBacklightCompensationSettings(
            enabled = mode == BacklightCompensationMode.ON,
            mode = mode
        )
    }

    private fun extractValue(xml: String, vararg tagNames: String): String? {
        for (tagName in tagNames) {
            val patterns = listOf(
                Regex("""<$tagName[^>]*>([^<]+)</$tagName>""", RegexOption.IGNORE_CASE),
                Regex("""<[^:]*:$tagName[^>]*>([^<]+)</[^:]*:$tagName>""", RegexOption.IGNORE_CASE)
            )

            for (pattern in patterns) {
                val match = pattern.find(xml)
                if (match != null) {
                    val value = match.groupValues.getOrNull(1)?.trim()
                    if (!value.isNullOrBlank()) {
                        return value
                    }
                }
            }
        }
        return null
    }

    private fun extractFloatValue(xml: String, vararg tagNames: String): Float? {
        val value = extractValue(xml, *tagNames)
        return value?.toFloatOrNull()
    }

    private fun extractFloatRange(xml: String, vararg tagNames: String): FloatRange? {
        val minMaxFromNested = tagNames.asSequence().mapNotNull { tagName ->
            val blockRegex = Regex(
                """(?is)<$tagName[^>]*>(.*?)</$tagName>"""
            )
            val block = blockRegex.find(xml)?.groupValues?.getOrNull(1) ?: return@mapNotNull null
            val min = extractFloatValue(block, "Min", "tt:Min", "timg:Min")
            val max = extractFloatValue(block, "Max", "tt:Max", "timg:Max")
            if (min != null && max != null) FloatRange(min, max) else null
        }.firstOrNull()

        if (minMaxFromNested != null) {
            return minMaxFromNested
        }

        val min = extractFloatValue(xml, *tagNames.map { "${it}Min" }.toTypedArray())
        val max = extractFloatValue(xml, *tagNames.map { "${it}Max" }.toTypedArray())
        return if (min != null && max != null) {
            FloatRange(min, max)
        } else {
            null
        }
    }
}
