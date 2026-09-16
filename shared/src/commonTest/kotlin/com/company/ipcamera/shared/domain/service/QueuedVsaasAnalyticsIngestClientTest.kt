@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.company.ipcamera.shared.domain.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class QueuedVsaasAnalyticsIngestClientTest {
    @Test
    fun queuedClient_retriesThenSends_successPath() =
        runTest {
            var attempts = 0
            val dispatcher = StandardTestDispatcher(testScheduler)
            val client =
                QueuedVsaasAnalyticsIngestClient(
                    transport =
                        VsaasFrameTransport {
                            attempts++
                            if (attempts < 3) {
                                Result.failure(
                                    IllegalStateException("transient"),
                                )
                            } else {
                                Result.success(Unit)
                            }
                        },
                    maxRetries = 3,
                    initialBackoffMs = 10,
                    maxBackoffMs = 20,
                    scope = CoroutineScope(dispatcher),
                )

            client.submitFrame("cam-1", 1L, 10, 10, byteArrayOf(1, 2, 3))
            advanceUntilIdle()

            val m = client.snapshotMetrics()
            assertEquals(1, m.accepted)
            assertEquals(1, m.sent)
            assertEquals(2, m.retried)
            assertEquals(0, m.failed)
            client.close()
        }

    @Test
    fun queuedClient_recordsFailure_afterRetryBudgetExhausted() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val client =
                QueuedVsaasAnalyticsIngestClient(
                    transport = VsaasFrameTransport { Result.failure(IllegalStateException("down")) },
                    maxRetries = 2,
                    initialBackoffMs = 10,
                    maxBackoffMs = 20,
                    scope = CoroutineScope(dispatcher),
                )

            client.submitFrame("cam-2", 2L, 10, 10, byteArrayOf(7))
            advanceUntilIdle()

            val m = client.snapshotMetrics()
            assertEquals(1, m.accepted)
            assertEquals(0, m.sent)
            assertEquals(2, m.retried)
            assertEquals(1, m.failed)
            client.close()
        }
}
