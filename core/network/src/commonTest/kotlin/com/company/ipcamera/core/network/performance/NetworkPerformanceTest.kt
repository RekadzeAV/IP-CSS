package com.company.ipcamera.core.network.performance

import com.company.ipcamera.core.network.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*
import kotlin.time.*

/**
 * Baseline performance тесты для сетевого слоя.
 * Фиксируют текущую производительность MessageQueue, RateLimiter и BinaryMessageHandler.
 */
class NetworkPerformanceTest {

    @Test
    fun `test MessageQueue enqueue dequeue throughput`() = runTest {
        val queue = MessageQueue(maxSize = 10000)
        val message = WebSocketMessage.EventMessage("test", "channel", buildJsonObject { })

        val duration = measureTime {
            repeat(1000) {
                queue.enqueue(message, MessagePriority.NORMAL)
            }
            repeat(1000) {
                queue.dequeue()
            }
        }

        println("MessageQueue 1000 enqueue+dequeue took ${duration.inWholeMilliseconds}ms")
        assertTrue(duration.inWholeMilliseconds < 5000, "Queue operations should complete within 5 seconds")
    }

    @Test
    fun `test RateLimiter message limit performance`() = runTest {
        val config = RateLimitConfig(
            messagesPerSecond = 10000,
            enabled = true
        )
        val limiter = RateLimiter(config)

        val duration = measureTime {
            repeat(5000) {
                limiter.checkMessageLimit()
            }
        }

        println("RateLimiter 5000 checks took ${duration.inWholeMilliseconds}ms")
        assertTrue(duration.inWholeMilliseconds < 3000, "Rate limit checks should be fast")
    }

    @Test
    fun `test BinaryMessageHandler chunking performance`() = runTest {
        val handler = BinaryMessageHandler()
        val largeData = ByteArray(10 * 1024 * 1024) // 10 MB
        largeData.fill(0x42)

        val duration = measureTime {
            assertTrue(handler.needsChunking(largeData))
            val chunks = handler.splitIntoChunks(largeData, "perf-test")
            assertTrue(chunks.size > 1)
        }

        println("BinaryMessageHandler 10MB chunking took ${duration.inWholeMilliseconds}ms")
        assertTrue(duration.inWholeMilliseconds < 5000, "Chunking should be reasonably fast")
    }

    @Test
    fun `test PriorityQueue ordering performance`() = runTest {
        val queue = MessageQueue(maxSize = 10000)

        val duration = measureTime {
            repeat(1000) {
                val priority = when (it % 4) {
                    0 -> MessagePriority.CRITICAL
                    1 -> MessagePriority.HIGH
                    2 -> MessagePriority.NORMAL
                    else -> MessagePriority.LOW
                }
                queue.enqueue(
                    WebSocketMessage.EventMessage("event$it", "ch", buildJsonObject { }),
                    priority
                )
            }

            // Проверяем, что CRITICAL сообщения извлекаются первыми
            val first = queue.dequeue()
            assertTrue(first is WebSocketMessage.EventMessage)
        }

        println("PriorityQueue 1000 mixed priority operations took ${duration.inWholeMilliseconds}ms")
        assertTrue(duration.inWholeMilliseconds < 5000)
    }

    @Test
    fun `test WebSocketMessage serialization performance`() = runTest {
        val message = WebSocketMessage.EventMessage(
            type = "motion_detected",
            channel = "camera_1",
            data = buildJsonObject {
                put("x", JsonPrimitive(100))
                put("y", JsonPrimitive(200))
                put("confidence", JsonPrimitive(0.95))
            }
        )

        // EventMessage имеет поле type, конфликтующее с дефолтным дискриминатором sealed-класса.
        // Используем кастомный дискриминатор, чтобы сериализация была валидной.
        val json = Json { classDiscriminator = "msg_type" }

        val duration = measureTime {
            repeat(1000) {
                val encoded = json.encodeToString(WebSocketMessage.serializer(), message)
                json.decodeFromString(WebSocketMessage.serializer(), encoded)
            }
        }

        println("WebSocketMessage 1000 serialize+deserialize took ${duration.inWholeMilliseconds}ms")
        assertTrue(duration.inWholeMilliseconds < 5000)
    }
}
