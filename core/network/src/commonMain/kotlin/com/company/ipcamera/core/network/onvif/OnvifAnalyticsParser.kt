package com.company.ipcamera.core.network.onvif

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Парсер для ONVIF Analytics Service ответов
 */
object OnvifAnalyticsParser {

    /**
     * Парсинг ответа GetAnalyticsEngines
     */
    fun parseGetAnalyticsEnginesResponse(xml: String): Result<List<OnvifAnalyticsEngine>> {
        return try {
            val engines = mutableListOf<OnvifAnalyticsEngine>()

            // Парсинг через regex (упрощенная версия)
            val engineRegex = Regex(
                """(?s)<tana:AnalyticsEngine[^>]*Token\s*=\s*["']([^"']+)["'][^>]*>(.*?)</tana:AnalyticsEngine>"""
            )

            val matches = engineRegex.findAll(xml)
            for (match in matches) {
                val token = match.groupValues.getOrNull(1)
                val engineXml = match.groupValues.getOrNull(2) ?: ""

                if (token != null) {
                    val name = extractValue(engineXml, "Name") ?: extractValue(engineXml, "tana:Name")
                    val type = extractValue(engineXml, "Type") ?: extractValue(engineXml, "tana:Type")

                    engines.add(
                        OnvifAnalyticsEngine(
                            token = token,
                            name = name,
                            type = type,
                            status = parseEngineStatus(engineXml)
                        )
                    )
                }
            }

            // Fallback: поиск по другому паттерну
            if (engines.isEmpty()) {
                val tokenRegex = Regex("""Token\s*=\s*["']([^"']+)["']""")
                val tokens = tokenRegex.findAll(xml).map { it.groupValues[1] }.distinct()
                for (token in tokens) {
                    engines.add(
                        OnvifAnalyticsEngine(
                            token = token,
                            name = null,
                            type = null,
                            status = AnalyticsEngineStatus.UNKNOWN
                        )
                    )
                }
            }

            Result.success(engines)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse GetAnalyticsEngines response" }
            Result.failure(e)
        }
    }

    /**
     * Парсинг ответа GetAnalyticsEngine
     */
    fun parseGetAnalyticsEngineResponse(xml: String): Result<OnvifAnalyticsEngine> {
        return try {
            val token = extractValue(xml, "EngineToken") ?: extractValue(xml, "tana:EngineToken")
                ?: Regex("""(?s)<tana:AnalyticsEngine[^>]*Token\s*=\s*["']([^"']+)["']""")
                    .find(xml)
                    ?.groupValues
                    ?.getOrNull(1)
                ?: return Result.failure(IllegalArgumentException("EngineToken not found"))

            val name = extractValue(xml, "Name") ?: extractValue(xml, "tana:Name")
            val type = extractValue(xml, "Type") ?: extractValue(xml, "tana:Type")
            val status = parseEngineStatus(xml)

            val engine = OnvifAnalyticsEngine(
                token = token,
                name = name,
                type = type,
                status = status
            )

            Result.success(engine)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse GetAnalyticsEngine response" }
            Result.failure(e)
        }
    }

    /**
     * Парсинг ответа GetAnalyticsEngineInputs
     */
    fun parseGetAnalyticsEngineInputsResponse(xml: String): Result<List<OnvifAnalyticsEngineInput>> {
        return try {
            val inputs = mutableListOf<OnvifAnalyticsEngineInput>()

            val inputRegex = Regex(
                """(?s)<tana:AnalyticsEngineInput[^>]*Token\s*=\s*["']([^"']+)["'][^>]*>(.*?)</tana:AnalyticsEngineInput>"""
            )

            val matches = inputRegex.findAll(xml)
            for (match in matches) {
                val token = match.groupValues.getOrNull(1)
                val inputXml = match.groupValues.getOrNull(2) ?: ""

                if (token != null) {
                    val type = extractValue(inputXml, "Type") ?: extractValue(inputXml, "tana:Type")
                    val sourceToken = extractValue(inputXml, "SourceToken") ?: extractValue(
                        inputXml,
                        "tana:SourceToken"
                    )

                    inputs.add(
                        OnvifAnalyticsEngineInput(
                            token = token,
                            type = type,
                            sourceToken = sourceToken
                        )
                    )
                }
            }

            // Fallback
            if (inputs.isEmpty()) {
                val tokenRegex = Regex("""InputToken[^>]*>([^<]+)<""")
                val tokens = tokenRegex.findAll(xml).map { it.groupValues[1].trim() }.distinct()
                for (token in tokens) {
                    inputs.add(
                        OnvifAnalyticsEngineInput(
                            token = token,
                            type = null,
                            sourceToken = null
                        )
                    )
                }
            }

            Result.success(inputs)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse GetAnalyticsEngineInputs response" }
            Result.failure(e)
        }
    }

    /**
     * Парсинг ответа GetAnalyticsEngineInput
     */
    fun parseGetAnalyticsEngineInputResponse(xml: String): Result<OnvifAnalyticsEngineInput> {
        return try {
            val token = extractValue(xml, "InputToken") ?: extractValue(xml, "tana:InputToken")
                ?: Regex("""(?s)<tana:AnalyticsEngineInput[^>]*Token\s*=\s*["']([^"']+)["']""")
                    .find(xml)
                    ?.groupValues
                    ?.getOrNull(1)
                ?: return Result.failure(IllegalArgumentException("InputToken not found"))

            val type = extractValue(xml, "Type") ?: extractValue(xml, "tana:Type")
            val sourceToken = extractValue(xml, "SourceToken") ?: extractValue(xml, "tana:SourceToken")

            val input = OnvifAnalyticsEngineInput(
                token = token,
                type = type,
                sourceToken = sourceToken
            )

            Result.success(input)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse GetAnalyticsEngineInput response" }
            Result.failure(e)
        }
    }

    /**
     * Парсинг ответа CreateAnalyticsEngine
     */
    fun parseCreateAnalyticsEngineResponse(xml: String): Result<OnvifAnalyticsEngine> {
        // Аналогично GetAnalyticsEngine
        return parseGetAnalyticsEngineResponse(xml)
    }

    /**
     * Извлечение значения из XML
     */
    private fun extractValue(xml: String, tagName: String): String? {
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

        return null
    }

    /**
     * Парсинг статуса движка
     */
    private fun parseEngineStatus(xml: String): AnalyticsEngineStatus {
        val statusStr = extractValue(xml, "Status") ?: extractValue(xml, "tana:Status")
            ?: return AnalyticsEngineStatus.UNKNOWN

        return when (statusStr.uppercase()) {
            "ACTIVE", "RUNNING", "ENABLED" -> AnalyticsEngineStatus.ACTIVE
            "INACTIVE", "STOPPED", "DISABLED" -> AnalyticsEngineStatus.INACTIVE
            "INITIALIZING", "STARTING" -> AnalyticsEngineStatus.INITIALIZING
            "ERROR", "FAILED" -> AnalyticsEngineStatus.ERROR
            else -> AnalyticsEngineStatus.UNKNOWN
        }
    }
}
