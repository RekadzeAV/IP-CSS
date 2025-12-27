package com.company.ipcamera.android.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Компонент видеоплеера на основе ExoPlayer
 * 
 * @param videoUrl URL видео потока (RTSP, HLS, HTTP и др.)
 * @param autoPlay Автоматически начать воспроизведение
 * @param onPlayerReady Callback когда плеер готов
 * @param onError Callback при ошибке
 */
@Composable
fun ExoVideoPlayer(
    videoUrl: String?,
    autoPlay: Boolean = true,
    modifier: Modifier = Modifier,
    onPlayerReady: ((Player) -> Unit)? = null,
    onError: ((Exception) -> Unit)? = null
) {
    val context = LocalContext.current

    // Создаем и настраиваем ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = autoPlay
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Устанавливаем медиа-источник
    DisposableEffect(videoUrl) {
        if (videoUrl != null && videoUrl.isNotBlank()) {
            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }

        // Callback когда плеер готов
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                onError?.invoke(Exception(error.message, error))
            }
        }
        exoPlayer.addListener(listener)
        onPlayerReady?.invoke(exoPlayer)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Создаем PlayerView для отображения видео
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

