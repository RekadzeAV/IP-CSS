package com.company.ipcamera.android.ui.components

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import android.util.Log
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.company.ipcamera.android.media.MediaSessionManager
import com.company.ipcamera.android.media.toRgb24ByteArray
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import org.koin.compose.koinInject

/**
 * Тип видеопотока
 */
enum class StreamType {
    RTSP,  // Прямой RTSP поток (низкая задержка)
    HLS    // HLS поток (более стабильный)
}

/**
 * Статистика воспроизведения
 */
data class PlaybackStats(
    val videoFormat: Format? = null,
    val videoBitrate: Int = 0,
    val videoFrameRate: Float = 0f,
    val audioFormat: Format? = null,
    val audioBitrate: Int = 0,
    val bufferHealth: Long = 0,
    val playbackPosition: Long = 0,
    val bufferedPosition: Long = 0
)

/**
 * Компонент видеоплеера на основе ExoPlayer с поддержкой RTSP и HLS
 *
 * @param videoUrl URL видео потока (RTSP, HLS, HTTP и др.)
 * @param hlsFallbackUrl Опциональный HLS URL с сервера (из getStreamStatus). Используется при ошибке RTSP для переключения на HLS.
 * @param streamType Тип потока (RTSP или HLS). Если null, определяется автоматически по URL
 * @param autoPlay Автоматически начать воспроизведение
 * @param enableLowLatency Включить режим низкой задержки для RTSP потоков
 * @param onPlayerReady Callback когда плеер готов
 * @param onError Callback при ошибке
 * @param onRetry Callback для повторной попытки подключения
 * @param onStatsUpdate Callback для обновления статистики (опционально, для debug)
 * @param enableBackgroundPlayback Включить фоновое воспроизведение через MediaSession
 * @param mediaTitle Заголовок для MediaSession (для уведомлений)
 * @param mediaSubtitle Подзаголовок для MediaSession
 * @param localFrameAnalyticsEnabled Периодический захват кадра с поверхности плеера (SurfaceView / TextureView) для локальной аналитики
 * @param localFrameAnalyticsIntervalMs Интервал между захватами (мс)
 * @param onLocalAnalyticsRgbFrame RGB24 [width*height*3], порядок R,G,B как на Desktop; null — не захватывать
 */
@OptIn(UnstableApi::class)
@Composable
fun ExoVideoPlayer(
    videoUrl: String?,
    hlsFallbackUrl: String? = null,
    streamType: StreamType? = null,
    autoPlay: Boolean = true,
    enableLowLatency: Boolean = true,
    modifier: Modifier = Modifier,
    onPlayerReady: ((Player) -> Unit)? = null,
    onError: ((Exception) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onStatsUpdate: ((PlaybackStats) -> Unit)? = null,
    enableBackgroundPlayback: Boolean = true,
    mediaTitle: String? = null,
    mediaSubtitle: String? = null,
    localFrameAnalyticsEnabled: Boolean = false,
    localFrameAnalyticsIntervalMs: Long = 1000L,
    onLocalAnalyticsRgbFrame: ((Int, Int, ByteArray) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val okHttpClient: OkHttpClient = koinInject()
    val mediaSourceFactory = remember(okHttpClient, context) {
        DefaultMediaSourceFactory(
            DefaultDataSource.Factory(context, OkHttpDataSource.Factory(okHttpClient))
        )
    }
    var retryCount by remember { mutableStateOf(0) }
    var fallbackToHls by remember { mutableStateOf(false) }
    var currentStreamType by remember { mutableStateOf(streamType) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    val maxRetries = 3

    // MediaSession для фонового воспроизведения
    val mediaSessionManager = remember {
        if (enableBackgroundPlayback) {
            // Будет инициализирован после создания плеера
            null
        } else {
            null
        }
    }

    // Определяем тип потока автоматически, если не указан
    val detectedStreamType = currentStreamType ?: remember(videoUrl) {
        when {
            videoUrl?.startsWith("rtsp://") == true -> StreamType.RTSP
            videoUrl?.endsWith(".m3u8") == true || videoUrl?.contains("/hls/") == true -> StreamType.HLS
            else -> StreamType.HLS // По умолчанию HLS
        }
    }

    // Создаем и настраиваем ExoPlayer с оптимизацией для типа потока (HLS/http — OkHttp + куки Ktor)
    val exoPlayer = remember(okHttpClient, mediaSourceFactory) {
        val loadControl: LoadControl = if (detectedStreamType == StreamType.RTSP && enableLowLatency && !fallbackToHls) {
            // Низкая задержка для RTSP потоков
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    1000,  // minBufferMs - минимальный буфер
                    2000,  // maxBufferMs - максимальный буфер
                    500,   // bufferForPlaybackMs - буфер перед началом воспроизведения
                    500    // bufferForPlaybackAfterRebufferMs - буфер после перебуферизации
                )
                .build()
        } else {
            // Стандартные настройки для HLS
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    5000,  // minBufferMs
                    15000, // maxBufferMs
                    2000,  // bufferForPlaybackMs
                    5000   // bufferForPlaybackAfterRebufferMs
                )
                .build()
        }

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = autoPlay
                repeatMode = Player.REPEAT_MODE_OFF

                // Оптимизация для RTSP потоков
                if (detectedStreamType == StreamType.RTSP) {
                    val trackSelectionParameters = trackSelectionParameters
                        .buildUpon()
                        .build()
                    this.trackSelectionParameters = trackSelectionParameters
                }
            }
    }

    // Устанавливаем медиа-источник
    DisposableEffect(videoUrl, hlsFallbackUrl, fallbackToHls) {
        val actualUrl = if (fallbackToHls && detectedStreamType == StreamType.RTSP) {
            hlsFallbackUrl?.takeIf { it.isNotBlank() } ?: videoUrl
        } else {
            videoUrl
        }

        if (actualUrl != null && actualUrl.isNotBlank()) {
            val mediaItem = MediaItem.fromUri(actualUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            Log.d("ExoVideoPlayer", "Setting media source: $actualUrl (fallback: $fallbackToHls)")
        }

        // Callback когда плеер готов
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)

                val errorType = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "NETWORK"
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "HTTP"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "PARSING"
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "MANIFEST"
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "DECODER"
                    PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> "DECODER_QUERY"
                    PlaybackException.ERROR_CODE_DECODING_FAILED -> "DECODING"
                    else -> "UNKNOWN"
                }

                Log.e("ExoVideoPlayer", "Player error: $errorType - ${error.message}", error)

                // Автоматическое переключение на HLS при ошибках RTSP
                if (detectedStreamType == StreamType.RTSP &&
                    !fallbackToHls &&
                    errorType in listOf("NETWORK", "PARSING", "DECODER", "DECODING")) {
                    Log.i("ExoVideoPlayer", "Switching to HLS fallback due to error: $errorType")

                    val hlsUrl = hlsFallbackUrl?.takeIf { it.isNotBlank() }
                    if (hlsUrl != null) {
                        fallbackToHls = true
                        retryCount = 0
                        // Обновляем URL для переключения на HLS
                        val mediaItem = MediaItem.fromUri(hlsUrl)
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        Log.d("ExoVideoPlayer", "Switched to HLS: $hlsUrl")
                        return
                    } else {
                        Log.w("ExoVideoPlayer", "HLS fallback URL not available, will retry RTSP")
                    }
                }

                // Детальная классификация ошибок для повторных попыток
                val isRetryable = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> true
                    else -> false
                }

                // Повторные попытки для других ошибок
                if (retryCount < maxRetries && isRetryable) {
                    retryCount++
                    Log.d("ExoVideoPlayer", "Retrying connection (attempt $retryCount/$maxRetries)")
                    onRetry?.invoke()
                } else {
                    val exception = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                            Exception("Network connection failed: ${error.message}", error)
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                            Exception("Network connection timeout: ${error.message}", error)
                        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                            Exception("HTTP error: ${error.message}", error)
                        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                            Exception("Invalid video format: ${error.message}", error)
                        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                            Exception("Invalid stream manifest: ${error.message}", error)
                        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                            Exception("Decoder initialization failed: ${error.message}", error)
                        PlaybackException.ERROR_CODE_DECODING_FAILED ->
                            Exception("Decoding failed: ${error.message}", error)
                        else -> Exception("Playback error ($errorType): ${error.message}", error)
                    }
                    onError?.invoke(exception)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    Player.STATE_READY -> {
                        retryCount = 0 // Сбрасываем счетчик при успешном подключении
                        Log.d("ExoVideoPlayer", "Player ready")
                    }
                    Player.STATE_BUFFERING -> {
                        Log.d("ExoVideoPlayer", "Player buffering")
                    }
                    Player.STATE_ENDED -> {
                        Log.d("ExoVideoPlayer", "Player ended")
                    }
                }
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                super.onTracksChanged(tracks)
                // Обновляем статистику при изменении треков
                updateStats(exoPlayer, onStatsUpdate)
            }
        }

        exoPlayer.addListener(listener)
        onPlayerReady?.invoke(exoPlayer)

        // Инициализация MediaSession для фонового воспроизведения
        val sessionManager = if (enableBackgroundPlayback) {
            MediaSessionManager(context, exoPlayer).also {
                it.initialize()
                it.updateMetadata(mediaTitle, mediaSubtitle)
            }
        } else {
            null
        }

        onDispose {
            exoPlayer.removeListener(listener)
            sessionManager?.release()
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Управление жизненным циклом для фонового воспроизведения
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // В режиме PiP продолжаем воспроизведение
                    if ((context as? android.app.Activity)?.isInPictureInPictureMode != true) {
                        // Можно приостановить, если не в PiP
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Возобновляем, если было приостановлено
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Автоматическое переподключение при ошибках
    LaunchedEffect(retryCount, fallbackToHls) {
        if (retryCount > 0 && retryCount <= maxRetries && videoUrl != null) {
            delay(2000L * retryCount) // Экспоненциальная задержка

            val actualUrl = if (fallbackToHls && detectedStreamType == StreamType.RTSP) {
                hlsFallbackUrl?.takeIf { it.isNotBlank() } ?: videoUrl
            } else {
                videoUrl
            }

            Log.d("ExoVideoPlayer", "Retrying with URL: $actualUrl")
            val mediaItem = MediaItem.fromUri(actualUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    // Периодическое обновление статистики
    LaunchedEffect(exoPlayer, onStatsUpdate) {
        if (onStatsUpdate == null) return@LaunchedEffect
        while (true) {
            delay(1000) // Обновляем каждую секунду
            updateStats(exoPlayer, onStatsUpdate)
        }
    }

    LaunchedEffect(
        localFrameAnalyticsEnabled,
        localFrameAnalyticsIntervalMs,
        onLocalAnalyticsRgbFrame,
        playerViewRef,
        exoPlayer
    ) {
        if (!localFrameAnalyticsEnabled || onLocalAnalyticsRgbFrame == null) return@LaunchedEffect
        while (true) {
            delay(localFrameAnalyticsIntervalMs)
            if (!exoPlayer.isPlaying) continue
            val pv = playerViewRef ?: continue
            val surfaceChild = pv.videoSurfaceView
            val triple = pixelCopyVideoSurfaceToRgb(surfaceChild) ?: continue
            onLocalAnalyticsRgbFrame(triple.first, triple.second, triple.third)
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
        modifier = modifier.fillMaxSize(),
        update = { view ->
            playerViewRef = view
            // Обновляем плеер при изменении URL
            if (view.player != exoPlayer) {
                view.player = exoPlayer
            }
        }
    )
}

private suspend fun pixelCopyVideoSurfaceToRgb(surfaceView: View?): Triple<Int, Int, ByteArray>? {
    surfaceView ?: return null
    val w = surfaceView.width
    val h = surfaceView.height
    if (w <= 0 || h <= 0) return null

    when (surfaceView) {
        is TextureView -> {
            val b = try {
                surfaceView.getBitmap(w, h)
            } catch (_: Exception) {
                null
            } ?: return null
            val rgb = b.toRgb24ByteArray()
            b.recycle()
            return Triple(w, h, rgb)
        }
        is SurfaceView -> {
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            return suspendCancellableCoroutine { cont ->
                val handler = Handler(Looper.getMainLooper())
                val listener = PixelCopy.OnPixelCopyFinishedListener { result ->
                    if (result != PixelCopy.SUCCESS) {
                        bmp.recycle()
                        if (cont.isActive) cont.resume(null)
                    } else {
                        val rgb = bmp.toRgb24ByteArray()
                        bmp.recycle()
                        if (cont.isActive) cont.resume(Triple(w, h, rgb))
                    }
                }
                try {
                    PixelCopy.request(surfaceView, bmp, listener, handler)
                } catch (_: Exception) {
                    bmp.recycle()
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
        else -> return null
    }
}

/**
 * Обновление статистики воспроизведения
 */
@OptIn(UnstableApi::class)
private fun updateStats(
    player: Player,
    onStatsUpdate: ((PlaybackStats) -> Unit)?
) {
    if (onStatsUpdate == null) return

    val stats = PlaybackStats(
        videoFormat = null,
        videoBitrate = 0,
        videoFrameRate = 0f,
        audioFormat = null,
        audioBitrate = 0,
        bufferHealth = player.bufferedPercentage.toLong(),
        playbackPosition = player.currentPosition,
        bufferedPosition = player.bufferedPosition
    )

    onStatsUpdate(stats)
}

