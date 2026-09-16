package com.company.ipcamera.server.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedDeque
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val logger = KotlinLogging.logger {}

@Serializable
data class WebhookDeliveryLogEntry(
    val timestamp: Long,
    val ruleId: String,
    val cameraId: String,
    val url: String,
    val attempt: Int,
    val maxAttempts: Int,
    val success: Boolean,
    val statusCode: Int?,
    val error: String?
)

@Serializable
data class WebhookDeliveryStats(
    val totalAttempts: Int,
    val successfulAttempts: Int,
    val failedAttempts: Int,
    val successRatePercent: Double
)

data class WebhookDeliveryLogsPage(
    val items: List<WebhookDeliveryLogEntry>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)

data class WebhookDeliverySummaryEntry(
    val ruleId: String,
    val cameraId: String,
    val totalAttempts: Int,
    val successfulAttempts: Int,
    val failedAttempts: Int,
    val successRatePercent: Double,
    val lastAttemptTimestamp: Long?
)

/**
 * Delivery service for analytics webhooks triggered by rules.
 */
class AnalyticsWebhookService(
    private val maxAttempts: Int = 3,
    private val webhookSecret: String? = System.getenv("ANALYTICS_WEBHOOK_SECRET"),
    private val requestTimeoutMs: Long = 5000L,
    private val retryBaseDelayMs: Long = 250L,
    private val logRetentionMs: Long = 24 * 60 * 60 * 1000L,
    private val customHttpClient: HttpClient? = null
) {
    private val client = customHttpClient ?: HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = requestTimeoutMs
            connectTimeoutMillis = requestTimeoutMs
            socketTimeoutMillis = requestTimeoutMs
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    private val deliveryLogs = ConcurrentLinkedDeque<WebhookDeliveryLogEntry>()
    private val maxLogEntries = 1000

    suspend fun send(
        url: String,
        payload: JsonObject,
        ruleId: String,
        cameraId: String
    ): Result<Unit> {
        var attempt = 0
        var lastError: Throwable? = null
        val payloadRaw = payload.toString()
        val deliveryId = UUID.randomUUID().toString()
        val signature = webhookSecret?.let { secret ->
            signPayload(payloadRaw, secret)
        }

        while (attempt < maxAttempts) {
            attempt++
            try {
                val response = client.post(url) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header("X-IPCSS-Rule-Id", ruleId)
                    header("X-IPCSS-Attempt", attempt.toString())
                    header("X-IPCSS-Delivery-Id", deliveryId)
                    webhookSecret?.let { header("X-IPCSS-Webhook-Secret", it) }
                    signature?.let { header("X-IPCSS-Signature", it) }
                    setBody(payload)
                }

                if (response.status.isSuccess()) {
                    appendLog(
                        WebhookDeliveryLogEntry(
                            timestamp = System.currentTimeMillis(),
                            ruleId = ruleId,
                            cameraId = cameraId,
                            url = url,
                            attempt = attempt,
                            maxAttempts = maxAttempts,
                            success = true,
                            statusCode = response.status.value,
                            error = null
                        )
                    )
                    return Result.success(Unit)
                }

                val errorBody = runCatching { response.body<String>() }.getOrDefault("")
                lastError = IllegalStateException(
                    "Webhook request failed with status ${response.status.value}. Body: $errorBody"
                )
                appendLog(
                    WebhookDeliveryLogEntry(
                        timestamp = System.currentTimeMillis(),
                        ruleId = ruleId,
                        cameraId = cameraId,
                        url = url,
                        attempt = attempt,
                        maxAttempts = maxAttempts,
                        success = false,
                        statusCode = response.status.value,
                        error = errorBody.ifBlank { "HTTP ${response.status.value}" }
                    )
                )
                logger.warn {
                    "Webhook attempt $attempt/$maxAttempts failed for rule $ruleId: HTTP ${response.status.value}"
                }
            } catch (e: Exception) {
                lastError = e
                appendLog(
                    WebhookDeliveryLogEntry(
                        timestamp = System.currentTimeMillis(),
                        ruleId = ruleId,
                        cameraId = cameraId,
                        url = url,
                        attempt = attempt,
                        maxAttempts = maxAttempts,
                        success = false,
                        statusCode = null,
                        error = e.message
                    )
                )
                logger.warn(e) { "Webhook attempt $attempt/$maxAttempts failed for rule $ruleId" }
            }

            if (attempt < maxAttempts) {
                val backoff = (retryBaseDelayMs * (1L shl (attempt - 1))).coerceAtMost(5000L)
                delay(backoff)
            }
        }

        return Result.failure(lastError ?: IllegalStateException("Webhook delivery failed"))
    }

    fun getDeliveryLogs(
        ruleId: String? = null,
        cameraId: String? = null,
        success: Boolean? = null,
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null,
        limit: Int = 100
    ): List<WebhookDeliveryLogEntry> {
        pruneExpiredLogs()
        val normalizedLimit = limit.coerceIn(1, 500)
        return deliveryLogs
            .asSequence()
            .filter { ruleId == null || it.ruleId == ruleId }
            .filter { cameraId == null || it.cameraId == cameraId }
            .filter { success == null || it.success == success }
            .filter { fromTimestamp == null || it.timestamp >= fromTimestamp }
            .filter { toTimestamp == null || it.timestamp <= toTimestamp }
            .sortedByDescending { it.timestamp }
            .take(normalizedLimit)
            .toList()
    }

    fun getDeliveryLogsPage(
        ruleId: String? = null,
        cameraId: String? = null,
        success: Boolean? = null,
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null,
        page: Int = 1,
        limit: Int = 100
    ): WebhookDeliveryLogsPage {
        pruneExpiredLogs()
        val normalizedPage = page.coerceAtLeast(1)
        val normalizedLimit = limit.coerceIn(1, 500)
        val filtered = deliveryLogs
            .asSequence()
            .filter { ruleId == null || it.ruleId == ruleId }
            .filter { cameraId == null || it.cameraId == cameraId }
            .filter { success == null || it.success == success }
            .filter { fromTimestamp == null || it.timestamp >= fromTimestamp }
            .filter { toTimestamp == null || it.timestamp <= toTimestamp }
            .sortedByDescending { it.timestamp }
            .toList()
        val total = filtered.size
        val start = ((normalizedPage - 1) * normalizedLimit).coerceAtMost(total)
        val end = (start + normalizedLimit).coerceAtMost(total)
        return WebhookDeliveryLogsPage(
            items = filtered.subList(start, end),
            total = total,
            page = normalizedPage,
            limit = normalizedLimit,
            hasMore = end < total
        )
    }

    fun getDeliveryStats(
        ruleId: String? = null,
        cameraId: String? = null,
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null
    ): WebhookDeliveryStats {
        pruneExpiredLogs()
        val filtered = deliveryLogs
            .asSequence()
            .filter { ruleId == null || it.ruleId == ruleId }
            .filter { cameraId == null || it.cameraId == cameraId }
            .filter { fromTimestamp == null || it.timestamp >= fromTimestamp }
            .filter { toTimestamp == null || it.timestamp <= toTimestamp }
            .toList()
        val total = filtered.size
        val success = filtered.count { it.success }
        val failed = total - success
        val rate = if (total == 0) 0.0 else (success.toDouble() / total.toDouble()) * 100.0
        return WebhookDeliveryStats(
            totalAttempts = total,
            successfulAttempts = success,
            failedAttempts = failed,
            successRatePercent = rate
        )
    }

    fun getDeliverySummary(
        ruleId: String? = null,
        cameraId: String? = null,
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null,
        limit: Int = 100
    ): List<WebhookDeliverySummaryEntry> {
        pruneExpiredLogs()
        val normalizedLimit = limit.coerceIn(1, 500)
        val filtered = deliveryLogs
            .asSequence()
            .filter { ruleId == null || it.ruleId == ruleId }
            .filter { cameraId == null || it.cameraId == cameraId }
            .filter { fromTimestamp == null || it.timestamp >= fromTimestamp }
            .filter { toTimestamp == null || it.timestamp <= toTimestamp }
            .toList()

        return filtered
            .groupBy { it.ruleId to it.cameraId }
            .map { (key, entries) ->
                val success = entries.count { it.success }
                val total = entries.size
                val failed = total - success
                val rate = if (total == 0) 0.0 else (success.toDouble() / total.toDouble()) * 100.0
                WebhookDeliverySummaryEntry(
                    ruleId = key.first,
                    cameraId = key.second,
                    totalAttempts = total,
                    successfulAttempts = success,
                    failedAttempts = failed,
                    successRatePercent = rate,
                    lastAttemptTimestamp = entries.maxOfOrNull { it.timestamp }
                )
            }
            .sortedByDescending { it.totalAttempts }
            .take(normalizedLimit)
    }

    fun clearDeliveryLogs(
        ruleId: String? = null,
        cameraId: String? = null,
        success: Boolean? = null,
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null
    ): Int {
        pruneExpiredLogs()
        
        if (ruleId == null && cameraId == null && success == null && fromTimestamp == null && toTimestamp == null) {
            return clearAllLogs()
        }
        
        val predicate = buildClearPredicate(ruleId, cameraId, success, fromTimestamp, toTimestamp)
        return clearFilteredLogs(predicate)
    }

    private fun clearAllLogs(): Int {
        val removed = deliveryLogs.size
        deliveryLogs.clear()
        return removed
    }

    private fun buildClearPredicate(
        ruleId: String?,
        cameraId: String?,
        success: Boolean?,
        fromTimestamp: Long?,
        toTimestamp: Long?
    ): (WebhookDeliveryLogEntry) -> Boolean = { entry ->
        (ruleId == null || entry.ruleId == ruleId) &&
            (cameraId == null || entry.cameraId == cameraId) &&
            (success == null || entry.success == success) &&
            (fromTimestamp == null || entry.timestamp >= fromTimestamp) &&
            (toTimestamp == null || entry.timestamp <= toTimestamp)
    }

    private fun clearFilteredLogs(predicate: (WebhookDeliveryLogEntry) -> Boolean): Int {
        val toRemove = deliveryLogs.filter(predicate)
        toRemove.forEach { deliveryLogs.remove(it) }
        return toRemove.size
    }

    private fun appendLog(entry: WebhookDeliveryLogEntry) {
        pruneExpiredLogs()
        deliveryLogs.addLast(entry)
        while (deliveryLogs.size > maxLogEntries) {
            deliveryLogs.pollFirst()
        }
    }

    internal fun appendLogForTests(entry: WebhookDeliveryLogEntry) {
        appendLog(entry)
    }

    private fun pruneExpiredLogs() {
        val cutoff = System.currentTimeMillis() - logRetentionMs
        while (true) {
            val first = deliveryLogs.peekFirst() ?: break
            if (first.timestamp >= cutoff) break
            deliveryLogs.pollFirst()
        }
    }

    private fun signPayload(payload: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val raw = mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
        val hex = raw.joinToString("") { byte -> "%02x".format(byte) }
        return "sha256=$hex"
    }
}
