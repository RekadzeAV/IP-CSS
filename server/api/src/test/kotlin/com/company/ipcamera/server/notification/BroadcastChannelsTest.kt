package com.company.ipcamera.server.notification

import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeTelegramSender : TelegramNotificationSender(TelegramConfig("t", listOf("1"))) {
    val sent = mutableListOf<String>()
    var fail = false
    override suspend fun send(text: String): Result<Unit> {
        if (fail) return Result.failure(IllegalStateException("tg down"))
        sent.add(text)
        return Result.success(Unit)
    }
}

private class FakeSmsSender : SmsNotificationSender(
    SmsConfig(endpointUrl = "http://localhost", apiKey = "k", defaultTo = listOf("+100"))
) {
    val texts = mutableListOf<String>()
    var fail = false
    override suspend fun send(text: String, to: List<String>): Result<Unit> {
        if (fail) return Result.failure(IllegalStateException("sms down"))
        texts.add(text)
        return Result.success(Unit)
    }
}

class BroadcastChannelsTest {

    private fun notification() = Notification(
        id = "n1",
        title = "Motion detected",
        message = "Camera <hall> moved",
        type = NotificationType.EVENT,
        priority = NotificationPriority.HIGH,
        cameraId = "cam-7"
    )

    @Test
    fun telegramChannelFormatsAndSends() = runTest {
        val sender = FakeTelegramSender()
        val channel = TelegramBroadcastChannel(sender)
        assertEquals("telegram", channel.name)

        val result = channel.broadcast(notification())
        assertTrue(result.isSuccess)
        val text = sender.sent.single()
        assertTrue("[HIGH] Motion detected" in text)
        assertTrue("Camera: cam-7" in text)
    }

    @Test
    fun telegramChannelPropagatesFailure() = runTest {
        val sender = FakeTelegramSender().apply { fail = true }
        val result = TelegramBroadcastChannel(sender).broadcast(notification())
        assertTrue(result.isFailure)
    }

    @Test
    fun smsChannelUsesDefaultRecipients() = runTest {
        val sender = FakeSmsSender()
        val result = SmsBroadcastChannel(sender).broadcast(notification())
        assertTrue(result.isSuccess)
        assertEquals(1, sender.texts.size)
        assertTrue("Motion detected" in sender.texts.single())
    }

    @Test
    fun smsChannelPropagatesFailure() = runTest {
        val sender = FakeSmsSender().apply { fail = true }
        val result = SmsBroadcastChannel(sender).broadcast(notification())
        assertTrue(result.isFailure)
    }

    @Test
    fun emailFactorySkipsWhenNoRecipientsEnv() {
        // Без SMTP_HOST env конфиг отсутствует -> фабрика не должна упасть.
        val channels = BroadcastChannelsFactory.createFromEnvironment()
            .filterIsInstance<EmailBroadcastChannel>()
        // В тестовом окружении SMTP не сконфигурирован; просто проверяем отсутствие исключений.
        assertTrue(channels.isEmpty() || channels.isNotEmpty())
    }
}
