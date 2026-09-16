package com.company.ipcamera.core.network

/**
 * Базовое исключение для ONVIF операций
 */
open class OnvifException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Исключение для SOAP Fault ответов
 *
 * @param faultCode Код ошибки (например, "s:Sender")
 * @param faultSubcode Подкод ошибки (опционально)
 * @param faultReason Причина ошибки (человекочитаемое сообщение)
 * @param faultDetail Детали ошибки (опционально)
 */
class OnvifFaultException(
    val faultCode: String,
    val faultSubcode: String? = null,
    val faultReason: String,
    val faultDetail: String? = null,
    cause: Throwable? = null
) : OnvifException(
    message = buildMessage(faultCode, faultSubcode, faultReason, faultDetail),
    cause = cause
) {
    companion object {
        private fun buildMessage(
            code: String,
            subcode: String?,
            reason: String,
            detail: String?
        ): String {
            val parts = mutableListOf<String>()
            parts.add("ONVIF Fault: $code")
            subcode?.let { parts.add("Subcode: $it") }
            parts.add("Reason: $reason")
            detail?.let { parts.add("Detail: $it") }
            return parts.joinToString(" | ")
        }
    }
}

/**
 * Исключение для ошибок аутентификации (401, 403)
 */
class OnvifAuthenticationException(
    message: String = "Authentication failed",
    val httpStatusCode: Int? = null,
    cause: Throwable? = null
) : OnvifException(message, cause)

/**
 * Исключение для случаев, когда метод не поддерживается камерой
 */
class OnvifNotSupportedException(
    method: String,
    cause: Throwable? = null
) : OnvifException("Method not supported: $method", cause)

/**
 * Исключение для таймаутов
 */
class OnvifTimeoutException(
    operation: String,
    timeoutMillis: Long,
    cause: Throwable? = null
) : OnvifException(
    "Operation '$operation' timed out after ${timeoutMillis}ms",
    cause
)

/**
 * Исключение для сетевых ошибок
 */
class OnvifNetworkException(
    message: String,
    cause: Throwable? = null
) : OnvifException("Network error: $message", cause)

/**
 * Исключение для ошибок парсинга XML/SOAP
 */
class OnvifParseException(
    message: String,
    val xmlContent: String? = null,
    cause: Throwable? = null
) : OnvifException("Parse error: $message", cause)

/**
 * Исключение для ошибок обнаружения устройств
 */
class OnvifDiscoveryException(
    message: String,
    cause: Throwable? = null
) : OnvifException("Discovery error: $message", cause)

/**
 * Парсер SOAP Fault для извлечения детальной информации об ошибке
 */
object OnvifFaultParser {
    /**
     * Парсинг SOAP Fault из XML ответа
     *
     * @param xml XML ответ с SOAP Fault
     * @return OnvifFaultException с детальной информацией
     */
    fun parseSoapFault(xml: String): OnvifFaultException {
        try {
            if (!isSoapFault(xml)) {
                throw IllegalArgumentException("Input XML does not contain a SOAP Fault")
            }
            // Парсинг fault code
            val faultCode = extractFaultCode(xml) ?: "s:Receiver"

            // Парсинг fault subcode
            val faultSubcode = extractFaultSubcode(xml)

            // Парсинг fault reason
            val faultReason = extractFaultReason(xml) ?: "Unknown error"

            // Парсинг fault detail
            val faultDetail = extractFaultDetail(xml)

            return OnvifFaultException(
                faultCode = faultCode,
                faultSubcode = faultSubcode,
                faultReason = faultReason,
                faultDetail = faultDetail
            )
        } catch (e: Exception) {
            // Если не удалось распарсить, возвращаем общее исключение
            return OnvifFaultException(
                faultCode = "s:Receiver",
                faultReason = "Failed to parse SOAP Fault: ${e.message}",
                faultDetail = xml.take(500) // Первые 500 символов для отладки
            )
        }
    }

    /**
     * Извлечение fault code
     *
     * Формат: <s:Code><s:Value>s:Sender</s:Value></s:Code>
     */
    private fun extractFaultCode(xml: String): String? {
        val patterns = listOf(
            Regex("""<s:Code[^>]*>\s*<s:Value[^>]*>([^<]+)</s:Value>""", RegexOption.IGNORE_CASE),
            Regex("""<Code[^>]*>\s*<Value[^>]*>([^<]+)</Value>""", RegexOption.IGNORE_CASE),
            Regex("""faultcode[^>]*>([^<]+)<""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(xml)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }

        return null
    }

    /**
     * Извлечение fault subcode
     *
     * Формат: <s:Code><s:Subcode><s:Value>...</s:Value></s:Subcode></s:Code>
     */
    private fun extractFaultSubcode(xml: String): String? {
        val patterns = listOf(
            Regex("""<s:Subcode[^>]*>\s*<s:Value[^>]*>([^<]+)</s:Value>""", RegexOption.IGNORE_CASE),
            Regex("""<Subcode[^>]*>\s*<Value[^>]*>([^<]+)</Value>""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(xml)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }

        return null
    }

    /**
     * Извлечение fault reason
     *
     * Формат: <s:Reason><s:Text xml:lang="en">...</s:Text></s:Reason>
     */
    private fun extractFaultReason(xml: String): String? {
        val patterns = listOf(
            Regex("""<s:Reason[^>]*>\s*<s:Text[^>]*>([^<]+)</s:Text>""", RegexOption.IGNORE_CASE),
            Regex("""<Reason[^>]*>\s*<Text[^>]*>([^<]+)</Text>""", RegexOption.IGNORE_CASE),
            Regex("""faultstring[^>]*>([^<]+)<""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(xml)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }

        return null
    }

    /**
     * Извлечение fault detail
     *
     * Формат: <s:Detail>...</s:Detail>
     */
    private fun extractFaultDetail(xml: String): String? {
        val patterns = listOf(
            Regex("""(?is)<s:Detail[^>]*>(.*?)</s:Detail>"""),
            Regex("""(?is)<Detail[^>]*>(.*?)</Detail>"""),
            Regex("""(?is)detail[^>]*>(.*?)</detail>""")
        )

        for (pattern in patterns) {
            val match = pattern.find(xml)
            if (match != null) {
                val detail = match.groupValues[1].trim()
                // Ограничиваем длину деталей
                return if (detail.length > 1000) {
                    detail.take(1000) + "..."
                } else {
                    detail
                }
            }
        }

        return null
    }

    /**
     * Проверка, является ли XML ответом SOAP Fault
     */
    fun isSoapFault(xml: String): Boolean {
        return xml.contains("<soap:Fault", ignoreCase = true) ||
            xml.contains("<s:Fault", ignoreCase = true) ||
            xml.contains("<Fault", ignoreCase = true) ||
            xml.contains("faultcode", ignoreCase = true)
    }
}
