package com.company.ipcamera.android.ui.components

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.company.ipcamera.android.R
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Instrumented тесты для ExoVideoPlayer
 * 
 * Запуск:
 * ./gradlew :android:app:connectedAndroidTest --tests "*ExoVideoPlayerInstrumentedTest*"
 */
@RunWith(AndroidJUnit4::class)
class ExoVideoPlayerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var exoPlayer: ExoPlayer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        exoPlayer = ExoPlayer.Builder(context)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    @After
    fun teardown() {
        exoPlayer.release()
    }

    @Test
    fun testPlayerInitialization() {
        // Given
        val testUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        
        // When
        val mediaItem = MediaItem.fromUri(testUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        
        // Then
        assertNotNull(exoPlayer)
        assertEquals(Player.STATE_IDLE, exoPlayer.playbackState)
    }

    @Test
    fun testPlayerReadyState() {
        // Given
        val testUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        val mediaItem = MediaItem.fromUri(testUrl)
        
        // When
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        
        // Ждем готовности (до 5 секунд)
        var ready = false
        repeat(50) {
            if (exoPlayer.playbackState == Player.STATE_READY) {
                ready = true
                return@repeat
            }
            Thread.sleep(100)
        }
        
        // Then
        assertTrue(ready, "Player should be ready within 5 seconds")
    }

    @Test
    fun testStreamTypeDetection() {
        // Given
        val rtspUrl = "rtsp://192.168.1.100:554/stream1"
        val hlsUrl = "https://server.com/hls/stream/playlist.m3u8"
        val httpUrl = "https://server.com/stream.mp4"
        
        // When & Then
        val rtspType = detectStreamType(rtspUrl)
        val hlsType = detectStreamType(hlsUrl)
        val httpType = detectStreamType(httpUrl)
        
        assertEquals(StreamType.RTSP, rtspType)
        assertEquals(StreamType.HLS, hlsType)
        assertEquals(StreamType.HLS, httpType) // По умолчанию HLS
    }

    @Test
    fun testLowLatencyBufferConfig() {
        // Given
        val lowLatencyConfig = createLowLatencyLoadControl()
        
        // When
        val minBufferMs = lowLatencyConfig.bufferDurationsMs[0]
        val maxBufferMs = lowLatencyConfig.bufferDurationsMs[1]
        val bufferForPlaybackMs = lowLatencyConfig.bufferDurationsMs[2]
        val bufferForPlaybackAfterRebufferMs = lowLatencyConfig.bufferDurationsMs[3]
        
        // Then
        assertEquals(1000, minBufferMs)
        assertEquals(2000, maxBufferMs)
        assertEquals(500, bufferForPlaybackMs)
        assertEquals(500, bufferForPlaybackAfterRebufferMs)
    }

    @Test
    fun testStandardLatencyBufferConfig() {
        // Given
        val standardConfig = createStandardLoadControl()
        
        // When
        val minBufferMs = standardConfig.bufferDurationsMs[0]
        val maxBufferMs = standardConfig.bufferDurationsMs[1]
        val bufferForPlaybackMs = standardConfig.bufferDurationsMs[2]
        val bufferForPlaybackAfterRebufferMs = standardConfig.bufferDurationsMs[3]
        
        // Then
        assertEquals(5000, minBufferMs)
        assertEquals(15000, maxBufferMs)
        assertEquals(2000, bufferForPlaybackMs)
        assertEquals(5000, bufferForPlaybackAfterRebufferMs)
    }

    @Test
    fun testMediaItemCreation() {
        // Given
        val testUrl = "rtsp://192.168.1.100:554/stream1"
        
        // When
        val mediaItem = MediaItem.fromUri(testUrl)
        
        // Then
        assertNotNull(mediaItem)
        assertEquals(testUrl, mediaItem.localConfiguration?.uri.toString())
    }

    @Test
    fun testPlayerStateTransitions() {
        // Given
        val testUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        val mediaItem = MediaItem.fromUri(testUrl)
        
        // When
        exoPlayer.setMediaItem(mediaItem)
        
        val initialState = exoPlayer.playbackState
        exoPlayer.prepare()
        Thread.sleep(100)
        val preparingState = exoPlayer.playbackState
        
        exoPlayer.playWhenReady = true
        Thread.sleep(3000)
        val playingState = exoPlayer.playbackState
        
        // Then
        assertEquals(Player.STATE_IDLE, initialState)
        assertTrue(preparingState == Player.STATE_BUFFERING || preparingState == Player.STATE_READY)
        assertEquals(Player.STATE_READY, playingState)
    }

    @Test
    fun testPlaybackPosition() {
        // Given
        val testUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        val mediaItem = MediaItem.fromUri(testUrl)
        
        // When
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        Thread.sleep(2000)
        
        val currentPosition = exoPlayer.currentPosition
        val bufferedPosition = exoPlayer.bufferedPosition
        
        // Then
        assertTrue(currentPosition >= 0, "Current position should be >= 0")
        assertTrue(bufferedPosition >= currentPosition, "Buffered position should be >= current")
    }

    @Test
    fun testBufferHealth() {
        // Given
        val testUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        val mediaItem = MediaItem.fromUri(testUrl)
        
        // When
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        Thread.sleep(2000)
        
        val bufferPercentage = exoPlayer.bufferedPercentage
        
        // Then
        assertTrue(bufferPercentage >= 0, "Buffer percentage should be >= 0")
        assertTrue(bufferPercentage <= 100, "Buffer percentage should be <= 100")
    }

    // Helper методы
    
    private fun detectStreamType(url: String): StreamType {
        return when {
            url.startsWith("rtsp://") -> StreamType.RTSP
            url.endsWith(".m3u8") || url.contains("/hls/") -> StreamType.HLS
            else -> StreamType.HLS
        }
    }
    
    private fun createLowLatencyLoadControl(): androidx.media3.exoplayer.LoadControl {
        return androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(1000, 2000, 500, 500)
            .build()
    }
    
    private fun createStandardLoadControl(): androidx.media3.exoplayer.LoadControl {
        return androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(5000, 15000, 2000, 5000)
            .build()
    }
}
