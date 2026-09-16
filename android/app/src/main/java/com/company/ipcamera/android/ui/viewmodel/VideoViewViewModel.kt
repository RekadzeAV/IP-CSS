package com.company.ipcamera.android.ui.viewmodel

import com.company.ipcamera.android.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ipcamera.core.network.api.StreamApiService
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.CameraObservationSummary
import com.company.ipcamera.shared.domain.model.ObservationCompliance
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.service.AnalyticsFrameProcessor
import com.company.ipcamera.shared.domain.service.AnalyticsFrameProcessorFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Тип потока для воспроизведения
 */
enum class StreamProtocol {
    RTSP,  // Прямой RTSP поток (низкая задержка, для локальных сетей)
    HLS    // HLS поток (более стабильный, для мобильных сетей)
}

/**
 * Качество потока
 */
enum class StreamQuality {
    LOW,       // 640x360, 500kbps, 15fps
    MEDIUM,    // 1280x720, 1500kbps, 25fps
    HIGH,      // 1920x1080, 3000kbps, 30fps
    ULTRA,     // 1920x1080, 6000kbps, 30fps
    QHD1440,   // 2560x1440
    UHD4K,     // 3840x2160
    QHD1440_H264, // 2560x1440 H.264 fallback
    UHD4K_H264 // 3840x2160 H.264 fallback
}

data class VideoViewUiState(
    val camera: Camera? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPlaying: Boolean = false,
    val isRecording: Boolean = false,
    val rtspUrl: String? = null,
    val hlsUrl: String? = null,
    val streamActive: Boolean = false,
    val streamProtocol: StreamProtocol = StreamProtocol.RTSP, // По умолчанию RTSP для Android
    val streamQuality: StreamQuality = StreamQuality.MEDIUM,
    val retryCount: Int = 0,
    val observationSummary: CameraObservationSummary? = null
)

class VideoViewViewModel(
    private val cameraRepository: CameraRepository,
    private val streamApiService: StreamApiService,
    private val analyticsFrameProcessorFactory: AnalyticsFrameProcessorFactory,
    private val cameraId: String
) : ViewModel() {

    private var analyticsProcessor: AnalyticsFrameProcessor? = null

    private val _uiState = MutableStateFlow(VideoViewUiState())
    val uiState: StateFlow<VideoViewUiState> = _uiState.asStateFlow()

    init {
        loadCamera()
    }

    fun loadCamera() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val camera = cameraRepository.getCameraById(cameraId)
                _uiState.value = _uiState.value.copy(
                    camera = camera,
                    observationSummary = camera?.let { ObservationCompliance.summarize(it) },
                    isLoading = false
                )
                if (camera != null) {
                    attachAnalyticsProcessor(camera)
                } else {
                    analyticsProcessor?.dispose()
                    analyticsProcessor = null
                }
                // Загружаем URL для трансляции
                loadStreamUrls()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load camera"
                )
            }
        }
    }

    /**
     * Загрузить URL для RTSP и HLS потоков
     */
    private fun loadStreamUrls() {
        viewModelScope.launch {
            try {
                // Получаем RTSP URL
                val rtspResult = streamApiService.getRtspUrl(cameraId)
                rtspResult.fold(
                    onSuccess = { url ->
                        _uiState.value = _uiState.value.copy(rtspUrl = url)
                    },
                    onError = { /* Не критично */ }
                )

                // Получаем статус стрима для HLS URL
                val statusResult = streamApiService.getStreamStatus(cameraId)
                statusResult.fold(
                    onSuccess = { status ->
                        _uiState.value = _uiState.value.copy(
                            hlsUrl = streamApiService.resolveHlsUrlForPlayback(cameraId, status.hlsUrl),
                            streamActive = status.active
                        )
                    },
                    onError = { /* Не критично */ }
                )
            } catch (e: Exception) {
                // Игнорируем ошибки загрузки URL
            }
        }
    }

    /**
     * Начать трансляцию
     */
    fun startStream() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = streamApiService.startStream(cameraId)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            streamActive = true,
                            isLoading = false,
                            retryCount = 0
                        )
                        // Обновляем URL после запуска стрима
                        loadStreamUrls()
                    },
                    onError = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to start stream"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to start stream"
                )
            }
        }
    }

    /**
     * Остановить трансляцию
     */
    fun stopStream() {
        viewModelScope.launch {
            try {
                val result = streamApiService.stopStream(cameraId)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            streamActive = false,
                            isPlaying = false,
                            retryCount = 0
                        )
                    },
                    onError = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Failed to stop stream"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to stop stream"
                )
            }
        }
    }

    /**
     * Изменить качество потока
     */
    fun setStreamQuality(quality: StreamQuality) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val qualityStr = quality.name.lowercase()

                val result = streamApiService.setStreamQuality(cameraId, qualityStr)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            streamQuality = quality,
                            isLoading = false
                        )
                        // Перезапускаем стрим с новым качеством
                        if (_uiState.value.streamActive) {
                            stopStream()
                            kotlinx.coroutines.delay(500)
                            startStream()
                        }
                    },
                    onError = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to change stream quality"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to change stream quality"
                )
            }
        }
    }

    /**
     * Изменить протокол потока (RTSP/HLS)
     */
    fun setStreamProtocol(protocol: StreamProtocol) {
        _uiState.value = _uiState.value.copy(streamProtocol = protocol)
    }

    /**
     * Получить URL для текущего протокола
     */
    fun getCurrentStreamUrl(): String? {
        return when (_uiState.value.streamProtocol) {
            StreamProtocol.RTSP -> _uiState.value.rtspUrl
            StreamProtocol.HLS -> _uiState.value.hlsUrl
        }
    }

    /**
     * Переподключиться к потоку
     */
    fun reconnect() {
        viewModelScope.launch {
            val currentRetryCount = _uiState.value.retryCount
            if (currentRetryCount >= 3) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to reconnect after 3 attempts"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                retryCount = currentRetryCount + 1,
                isLoading = true,
                error = null
            )

            // Останавливаем текущий стрим
            if (_uiState.value.streamActive) {
                stopStream()
                kotlinx.coroutines.delay(1000)
            }

            // Запускаем заново
            startStream()
        }
    }

    fun togglePlayback() {
        _uiState.value = _uiState.value.copy(isPlaying = !_uiState.value.isPlaying)
    }

    fun toggleRecording() {
        _uiState.value = _uiState.value.copy(isRecording = !_uiState.value.isRecording)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Кадр с поверхности плеера (RGB24). Учитывается только при [isLocalFrameAnalyticsEnabled].
     * Троттлинг по интервалу задаётся в [ExoVideoPlayer].
     */
    fun submitLocalAnalyticsFrame(width: Int, height: Int, rgb: ByteArray) {
        if (!isLocalFrameAnalyticsEnabled()) return
        val proc = analyticsProcessor ?: return
        proc.processFrame(rgb, width, height)
    }

    /** Env `IPCSS_LOCAL_FRAME_ANALYTICS=true` или debug-сборка с `-Pipcss.localFrameAnalytics=true`. */
    fun isLocalFrameAnalyticsEnabled(): Boolean =
        System.getenv("IPCSS_LOCAL_FRAME_ANALYTICS") == "true" ||
            BuildConfig.LOCAL_FRAME_ANALYTICS

    private fun attachAnalyticsProcessor(camera: Camera) {
        analyticsProcessor?.dispose()
        // Отдельный scope внутри процессора: dispose() не должен отменять viewModelScope.
        analyticsProcessor = analyticsFrameProcessorFactory.create(camera)
    }

    override fun onCleared() {
        analyticsProcessor?.dispose()
        analyticsProcessor = null
        super.onCleared()
        // Останавливаем стрим при очистке ViewModel
        if (_uiState.value.streamActive) {
            stopStream()
        }
        _uiState.value = _uiState.value.copy(isPlaying = false, isRecording = false)
    }
}

