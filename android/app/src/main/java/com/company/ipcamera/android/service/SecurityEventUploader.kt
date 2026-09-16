package com.company.ipcamera.android.service

import com.company.ipcamera.core.common.security.MobileSecurityEvent
import com.company.ipcamera.core.common.security.MobileSecurityEventType
import com.company.ipcamera.core.common.security.SecureMobileSecurityLogger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.core.context.GlobalContext

/**
 * Отправка критических мобильных security-событий на сервер
 * (REMAINING_TASKS 2.6 / PLAN_EXECUTION_MASTER этап 5.5).
 *
 * Транспорт: POST /api/v1/audit/client-events (публичный, батч ≤50,
 * валидация enum на сервере — AuditRoutes). Используется DI OkHttpClient
 * с ApiClientCookieJar. Вызывается из SecureMobileSecurityLogger.remoteSink
 * только для severity=CRITICAL — должен быть неблокирующим (свой поток).
 */
object SecurityEventUploader {

    private const val TAG = "SecurityEventUploader"
    private const val CLIENT_EVENTS_PATH = "/api/v1/audit/client-events"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    /** Регистрация приёмника критических событий (вызывается из MainActivity.onCreate после Koin). */
    fun install() {
        SecureMobileSecurityLogger.remoteSink = { event -> send(event) }
    }

    fun send(event: MobileSecurityEvent) {
        Thread {
            try {
                val koin = GlobalContext.getOrNull() ?: return@Thread
                val payload = payload(event) ?: return@Thread
                val okClient = koin.get<OkHttpClient>()
                val base = koin.get<com.company.ipcamera.core.network.ApiClientConfig>().baseUrl
                val request = Request.Builder()
                    .url(base.trimEnd('/') + CLIENT_EVENTS_PATH)
                    .post(payload.toRequestBody(JSON))
                    .build()
                okClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        android.util.Log.w(TAG, "client-events HTTP ${resp.code}")
                    }
                }
            } catch (e: Exception) {
                // Аудит не должен ломать клиент — любые ошибки глушим с предупреждением.
                android.util.Log.w(TAG, "client-events upload failed", e)
            }
        }.start()
    }

    /**
     * Маппинг мобильных типов на серверные SecurityEventType.
     * Сервер валидирует enum и отбрасывает неизвестные значения (rejected++),
     * поэтому типы без серверного аналога не отправляются (null).
     * when без else — компилятор напомнит о новых значениях enum.
     */
    private fun serverType(type: MobileSecurityEventType): String? = when (type) {
        MobileSecurityEventType.AUTHENTICATION_SUCCESS -> "LOGIN_SUCCESS"
        MobileSecurityEventType.AUTHENTICATION_FAILURE -> "LOGIN_FAILURE"
        MobileSecurityEventType.CERTIFICATE_PINNING_FAILURE -> "CERTIFICATE_PINNING_FAILURE"
        MobileSecurityEventType.SUSPICIOUS_ACTIVITY -> "SUSPICIOUS_ACTIVITY"
        MobileSecurityEventType.DATA_ACCESS -> "DATA_ACCESS"
        MobileSecurityEventType.TOKEN_EXPIRED -> "TOKEN_EXPIRED"
        MobileSecurityEventType.ENCRYPTION_SUCCESS -> null
        MobileSecurityEventType.ENCRYPTION_FAILURE -> null
        MobileSecurityEventType.DECRYPTION_SUCCESS -> null
        MobileSecurityEventType.DECRYPTION_FAILURE -> null
        MobileSecurityEventType.KEYSTORE_ACCESS -> null
        MobileSecurityEventType.KEYSTORE_ERROR -> null
        MobileSecurityEventType.CERTIFICATE_PINNING_SUCCESS -> null
        MobileSecurityEventType.TLS_ERROR -> null
        MobileSecurityEventType.NETWORK_ERROR -> null
        MobileSecurityEventType.TOKEN_REFRESH -> null
        MobileSecurityEventType.FILE_ACCESS_ERROR -> null
    }

    private fun payload(event: MobileSecurityEvent): String? {
        val type = serverType(event.type) ?: return null
        val details = event.details.entries.take(10).joinToString(",") { (k, v) ->
            "\"${jsonEscape(k.take(50))}\":\"${jsonEscape(v.take(200))}\""
        }
        return "{\"events\":[{\"type\":\"$type\",\"severity\":\"${event.severity.name}\"," +
            "\"details\":{$details},\"timestamp\":${event.timestamp}}]}"
    }

    private fun jsonEscape(s: String): String = buildString(s.length) {
        for (c in s) when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
        }
    }
}
