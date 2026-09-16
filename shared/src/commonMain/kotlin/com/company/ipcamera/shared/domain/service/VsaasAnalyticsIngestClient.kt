package com.company.ipcamera.shared.domain.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Минимальный контракт облачного аналитического конвейера (VSaaS ingest).
 * MVP: отправка кадра + метаданных камеры во внешний pipeline.
 */
interface VsaasAnalyticsIngestClient {
    suspend fun submitFrame(
        cameraId: String,
        timestampMs: Long,
        width: Int,
        height: Int,
        frameData: ByteArray,
    ): Result<Unit>
}

/**
 * Безопасный fallback-клиент: ничего не отправляет, но не роняет поток.
 */
class NoOpVsaasAnalyticsIngestClient : VsaasAnalyticsIngestClient {
    override suspend fun submitFrame(
        cameraId: String,
        timestampMs: Long,
        width: Int,
        height: Int,
        frameData: ByteArray,
    ): Result<Unit> = Result.success(Unit)
}

/**
 * Низкоуровневый транспорт отправки кадра в облако.
 */
fun interface VsaasFrameTransport {
    suspend fun send(frame: VsaasFrameEnvelope): Result<Unit>
}

data class VsaasFrameEnvelope(
    val cameraId: String,
    val timestampMs: Long,
    val width: Int,
    val height: Int,
    val frameData: ByteArray,
)

data class VsaasIngestMetrics(
    val accepted: Long = 0L,
    val sent: Long = 0L,
    val dropped: Long = 0L,
    val retried: Long = 0L,
    val failed: Long = 0L,
)

/**
 * Очередь отправки в VSaaS с retry/backoff без блокировки кадрового потока.
 */
class QueuedVsaasAnalyticsIngestClient(
    private val transport: VsaasFrameTransport,
    queueCapacity: Int = 128,
    private val maxRetries: Int = 3,
    private val initialBackoffMs: Long = 200L,
    private val maxBackoffMs: Long = 3000L,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : VsaasAnalyticsIngestClient {
    private val workerScope = scope
    private val queue =
        Channel<VsaasFrameEnvelope>(
            capacity = queueCapacity,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    private val metricsMutex = Mutex()

    @Volatile
    private var metrics = VsaasIngestMetrics()
    private val workerJob: Job

    init {
        workerJob =
            workerScope.launch {
                while (isActive) {
                    val frame = queue.receive()
                    processFrame(frame)
                }
            }
    }

    override suspend fun submitFrame(
        cameraId: String,
        timestampMs: Long,
        width: Int,
        height: Int,
        frameData: ByteArray,
    ): Result<Unit> {
        val envelope = VsaasFrameEnvelope(cameraId, timestampMs, width, height, frameData.copyOf())
        val sent = queue.trySend(envelope)
        return if (sent.isSuccess) {
            incAccepted()
            Result.success(Unit)
        } else {
            incDropped()
            Result.failure(IllegalStateException("VSaaS ingest queue is full"))
        }
    }

    fun snapshotMetrics(): VsaasIngestMetrics = metrics

    fun close() {
        workerJob.cancel()
        workerScope.cancel()
    }

    private suspend fun processFrame(frame: VsaasFrameEnvelope) {
        var attempt = 0
        var backoff = initialBackoffMs
        while (attempt <= maxRetries) {
            val result = transport.send(frame)
            if (result.isSuccess) {
                incSent()
                return
            }
            attempt++
            if (attempt > maxRetries) {
                incFailed()
                return
            }
            incRetried()
            delay(backoff.coerceAtMost(maxBackoffMs))
            backoff = (backoff * 2).coerceAtMost(maxBackoffMs)
        }
    }

    private suspend fun incAccepted() =
        metricsMutex.withLock { metrics = metrics.copy(accepted = metrics.accepted + 1) }

    private suspend fun incSent() = metricsMutex.withLock { metrics = metrics.copy(sent = metrics.sent + 1) }

    private suspend fun incDropped() = metricsMutex.withLock { metrics = metrics.copy(dropped = metrics.dropped + 1) }

    private suspend fun incRetried() = metricsMutex.withLock { metrics = metrics.copy(retried = metrics.retried + 1) }

    private suspend fun incFailed() = metricsMutex.withLock { metrics = metrics.copy(failed = metrics.failed + 1) }
}
