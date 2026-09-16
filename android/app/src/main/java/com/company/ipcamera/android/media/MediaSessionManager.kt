package com.company.ipcamera.android.media

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession

/**
 * Менеджер для управления MediaSession (Media3)
 * Обеспечивает фоновое воспроизведение и управление через уведомления
 */
@OptIn(UnstableApi::class)
class MediaSessionManager(
    private val context: Context,
    private val player: Player
) {
    private var mediaSession: MediaSession? = null

    companion object {
        private const val MEDIA_SESSION_TAG = "IPCameraMediaSession"
    }

    /**
     * Инициализировать MediaSession
     */
    fun initialize() {
        mediaSession = MediaSession.Builder(context, player)
            .build()
    }

    /**
     * Обновить метаданные
     */
    fun updateMetadata(title: String? = null, subtitle: String? = null) {
        // Media3 автоматически синхронизирует метаданные из MediaItem
        // Можно обновить через player.setMediaMetadata()
    }

    /**
     * Получить MediaSession
     */
    fun getMediaSession(): MediaSession? {
        return mediaSession
    }

    /**
     * Освободить ресурсы
     */
    fun release() {
        mediaSession?.release()
        mediaSession = null
    }
}
