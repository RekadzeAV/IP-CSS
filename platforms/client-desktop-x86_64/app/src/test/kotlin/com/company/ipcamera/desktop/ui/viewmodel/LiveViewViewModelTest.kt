package com.company.ipcamera.desktop.ui.viewmodel

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.usecase.GetCamerasUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.util.prefs.Preferences
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LiveViewViewModelTest {

    private lateinit var getCamerasUseCase: GetCamerasUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        getCamerasUseCase = mock()
        // Isolate tests from persisted desktop preferences.
        Preferences.userRoot().node("com.company.ipcamera.desktop.live.state").clear()
    }

    @Test
    fun `loadCameras should update state with cameras`() = runTest(testDispatcher) {
        // Arrange
        val cameras = listOf(
            Camera(id = "1", name = "Camera 1", url = "http://test.com"),
            Camera(id = "2", name = "Camera 2", url = "http://test2.com")
        )
        whenever(getCamerasUseCase.invoke()).thenReturn(cameras)

        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        // Act
        viewModel.loadCameras()
        advanceUntilIdle()

        // Assert
        val state = viewModel.state.value
        assertEquals(cameras, state.cameras)
        assertTrue(!state.isLoading)
        assertEquals(null, state.error)
    }

    @Test
    fun `loadCameras should handle error`() = runTest(testDispatcher) {
        // Arrange
        val errorMessage = "Network error"
        whenever(getCamerasUseCase.invoke()).thenThrow(RuntimeException(errorMessage))

        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        // Act
        viewModel.loadCameras()
        advanceUntilIdle()

        // Assert
        val state = viewModel.state.value
        assertTrue(!state.isLoading)
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `selectCamera should add camera to selected list`() = runTest(testDispatcher) {
        // Arrange
        val cameras = listOf(
            Camera(id = "1", name = "Camera 1", url = "http://test.com"),
            Camera(id = "2", name = "Camera 2", url = "http://test2.com")
        )
        whenever(getCamerasUseCase.invoke()).thenReturn(cameras)

        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        viewModel.loadCameras()
        advanceUntilIdle()

        // Act
        viewModel.selectCamera("1")

        // Assert
        val state = viewModel.state.value
        assertTrue(state.selectedCameras.contains("1"))
    }

    @Test
    fun `selectCamera should remove camera if already selected`() = runTest(testDispatcher) {
        // Arrange
        val cameras = listOf(
            Camera(id = "1", name = "Camera 1", url = "http://test.com")
        )
        whenever(getCamerasUseCase.invoke()).thenReturn(cameras)

        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        viewModel.loadCameras()
        advanceUntilIdle()
        viewModel.selectCamera("1")

        // Act
        viewModel.selectCamera("1")

        // Assert
        val state = viewModel.state.value
        assertTrue(!state.selectedCameras.contains("1"))
    }

    @Test
    fun `selectCamera should respect grid layout max cameras limit`() = runTest(testDispatcher) {
        // Arrange
        val cameras = listOf(
            Camera(id = "1", name = "Camera 1", url = "http://test.com"),
            Camera(id = "2", name = "Camera 2", url = "http://test2.com"),
            Camera(id = "3", name = "Camera 3", url = "http://test3.com"),
            Camera(id = "4", name = "Camera 4", url = "http://test4.com"),
            Camera(id = "5", name = "Camera 5", url = "http://test5.com")
        )
        whenever(getCamerasUseCase.invoke()).thenReturn(cameras)

        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        viewModel.loadCameras()
        advanceUntilIdle()
        viewModel.setGridLayout(GridLayout.GRID_4) // Максимум 4 камеры

        // Act
        viewModel.selectCamera("1")
        viewModel.selectCamera("2")
        viewModel.selectCamera("3")
        viewModel.selectCamera("4")
        viewModel.selectCamera("5") // Должна быть проигнорирована

        // Assert
        val state = viewModel.state.value
        assertEquals(4, state.selectedCameras.size)
        assertTrue(!state.selectedCameras.contains("5"))
    }

    @Test
    fun `setGridLayout should update layout and remove excess cameras`() = runTest(testDispatcher) {
        // Arrange
        val cameras = listOf(
            Camera(id = "1", name = "Camera 1", url = "http://test.com"),
            Camera(id = "2", name = "Camera 2", url = "http://test2.com"),
            Camera(id = "3", name = "Camera 3", url = "http://test3.com"),
            Camera(id = "4", name = "Camera 4", url = "http://test4.com"),
            Camera(id = "5", name = "Camera 5", url = "http://test5.com")
        )
        whenever(getCamerasUseCase.invoke()).thenReturn(cameras)

        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        viewModel.loadCameras()
        advanceUntilIdle()
        viewModel.setGridLayout(GridLayout.GRID_9) // Максимум 9 камер
        viewModel.selectCamera("1")
        viewModel.selectCamera("2")
        viewModel.selectCamera("3")
        viewModel.selectCamera("4")
        viewModel.selectCamera("5")

        // Act
        viewModel.setGridLayout(GridLayout.GRID_4) // Уменьшаем до 4 камер

        // Assert
        val state = viewModel.state.value
        assertEquals(GridLayout.GRID_4, state.gridLayout)
        assertEquals(4, state.selectedCameras.size)
    }

    @Test
    fun `getSelectedCameras should return cameras from selected IDs`() = runTest(testDispatcher) {
        // Arrange
        val cameras = listOf(
            Camera(id = "1", name = "Camera 1", url = "http://test.com"),
            Camera(id = "2", name = "Camera 2", url = "http://test2.com")
        )
        whenever(getCamerasUseCase.invoke()).thenReturn(cameras)

        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        viewModel.loadCameras()
        advanceUntilIdle()
        viewModel.setGridLayout(GridLayout.GRID_4)
        viewModel.selectCamera("1")
        viewModel.selectCamera("2")

        // Act
        val selected = viewModel.getSelectedCameras()

        // Assert
        assertEquals(2, selected.size)
        assertTrue(selected.any { it.id == "1" })
        assertTrue(selected.any { it.id == "2" })
    }

    @Test
    fun `setFocusedCamera should update focused camera`() = runTest(testDispatcher) {
        val cameras = listOf(
            Camera(id = "1", name = "Camera 1", url = "http://test.com"),
            Camera(id = "2", name = "Camera 2", url = "http://test2.com")
        )
        whenever(getCamerasUseCase.invoke()).thenReturn(cameras)

        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        viewModel.loadCameras()
        advanceUntilIdle()
        viewModel.setGridLayout(GridLayout.GRID_4)
        viewModel.selectCamera("1")
        viewModel.selectCamera("2")

        viewModel.setFocusedCamera("2")

        assertEquals("2", viewModel.state.value.focusedCameraId)
        assertEquals(StreamPriority.HIGH, viewModel.getStreamPriority("2"))
    }

    @Test
    fun `getStreamPriority should mark non-focused cameras based on budget`() = runTest(testDispatcher) {
        val cameras = (1..8).map {
            Camera(id = it.toString(), name = "Camera $it", url = "http://test$it.com")
        }
        whenever(getCamerasUseCase.invoke()).thenReturn(cameras)

        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        viewModel.loadCameras()
        advanceUntilIdle()
        viewModel.setGridLayout(GridLayout.GRID_9)
        (1..8).forEach { viewModel.selectCamera(it.toString()) }
        viewModel.setFocusedCamera("1")

        assertEquals(StreamPriority.HIGH, viewModel.getStreamPriority("1"))
        assertEquals(StreamPriority.NORMAL, viewModel.getStreamPriority("2"))
        assertEquals(StreamPriority.NORMAL, viewModel.getStreamPriority("4"))
        assertEquals(StreamPriority.BACKGROUND, viewModel.getStreamPriority("8"))
    }

    @Test
    fun `setVisibilityGraceMs should switch mode to custom`() = runTest(testDispatcher) {
        whenever(getCamerasUseCase.invoke()).thenReturn(emptyList())
        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        advanceUntilIdle()

        viewModel.setVisibilityGraceMs(2400L)

        assertEquals(2400L, viewModel.state.value.visibilityGraceMs)
        assertEquals(VisibilityGraceMode.CUSTOM, viewModel.state.value.visibilityGraceMode)
    }

    @Test
    fun `setVisibilityGraceAuto should restore auto mode and recommended value`() = runTest(testDispatcher) {
        whenever(getCamerasUseCase.invoke()).thenReturn(emptyList())
        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        advanceUntilIdle()
        viewModel.setGridLayout(GridLayout.GRID_16)
        viewModel.setVisibilityGraceMs(2400L)

        viewModel.setVisibilityGraceAuto()

        assertEquals(3000L, viewModel.state.value.visibilityGraceMs)
        assertEquals(VisibilityGraceMode.AUTO, viewModel.state.value.visibilityGraceMode)
    }

    @Test
    fun `auto mode should adjust on layout change but custom mode should keep value`() = runTest(testDispatcher) {
        whenever(getCamerasUseCase.invoke()).thenReturn(emptyList())
        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        advanceUntilIdle()

        viewModel.setVisibilityGraceAuto()
        viewModel.setGridLayout(GridLayout.GRID_16)
        assertEquals(3000L, viewModel.state.value.visibilityGraceMs)
        assertEquals(VisibilityGraceMode.AUTO, viewModel.state.value.visibilityGraceMode)

        viewModel.setVisibilityGraceMs(2200L)
        viewModel.setGridLayout(GridLayout.GRID_4)
        assertEquals(2200L, viewModel.state.value.visibilityGraceMs)
        assertEquals(VisibilityGraceMode.CUSTOM, viewModel.state.value.visibilityGraceMode)
    }

    @Test
    fun `visibility grace mode should persist between view model instances`() = runTest(testDispatcher) {
        whenever(getCamerasUseCase.invoke()).thenReturn(emptyList())

        val first = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        advanceUntilIdle()
        first.setVisibilityGraceMs(2100L)
        assertEquals(VisibilityGraceMode.CUSTOM, first.state.value.visibilityGraceMode)

        val second = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        advanceUntilIdle()
        assertEquals(2100L, second.state.value.visibilityGraceMs)
        assertEquals(VisibilityGraceMode.CUSTOM, second.state.value.visibilityGraceMode)
    }

    @Test
    fun `auto visibility grace mode should persist between view model instances`() = runTest(testDispatcher) {
        whenever(getCamerasUseCase.invoke()).thenReturn(emptyList())

        val first = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        advanceUntilIdle()
        first.setGridLayout(GridLayout.GRID_16)
        first.setVisibilityGraceAuto()
        assertEquals(3000L, first.state.value.visibilityGraceMs)
        assertEquals(VisibilityGraceMode.AUTO, first.state.value.visibilityGraceMode)

        val second = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        advanceUntilIdle()
        assertEquals(3000L, second.state.value.visibilityGraceMs)
        assertEquals(VisibilityGraceMode.AUTO, second.state.value.visibilityGraceMode)
    }

    @Test
    fun `updateCameraMetrics should aggregate render fps and dropped frames`() = runTest(testDispatcher) {
        val cameras = listOf(
            Camera(id = "1", name = "Camera 1", url = "http://test.com"),
            Camera(id = "2", name = "Camera 2", url = "http://test2.com")
        )
        whenever(getCamerasUseCase.invoke()).thenReturn(cameras)

        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        viewModel.loadCameras()
        advanceUntilIdle()
        viewModel.setGridLayout(GridLayout.GRID_4)
        viewModel.selectCamera("1")
        viewModel.selectCamera("2")

        viewModel.updateCameraMetrics(
            "1",
            CameraPlaybackMetrics(renderFps = 18, droppedFrames = 12)
        )
        viewModel.updateCameraMetrics(
            "2",
            CameraPlaybackMetrics(renderFps = 12, droppedFrames = 8)
        )

        val summary = viewModel.state.value.telemetrySummary
        assertEquals(15, summary.avgRenderFps)
        assertEquals(20, summary.totalDroppedFrames)
    }

    @Test
    fun `updateCameraMetrics should skip noisy render-only updates`() = runTest(testDispatcher) {
        val cameras = listOf(
            Camera(id = "1", name = "Camera 1", url = "http://test.com")
        )
        whenever(getCamerasUseCase.invoke()).thenReturn(cameras)

        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        viewModel.loadCameras()
        advanceUntilIdle()
        viewModel.selectCamera("1")

        viewModel.updateCameraMetrics(
            "1",
            CameraPlaybackMetrics(renderFps = 10, droppedFrames = 10)
        )
        val first = viewModel.state.value.cameraMetrics["1"]!!
        assertEquals(10, first.renderFps)
        assertEquals(10, first.droppedFrames)

        // Небольшой шум не должен триггерить перепубликацию немедленно.
        viewModel.updateCameraMetrics(
            "1",
            CameraPlaybackMetrics(renderFps = 11, droppedFrames = 11)
        )
        val afterNoise = viewModel.state.value.cameraMetrics["1"]!!
        assertEquals(10, afterNoise.renderFps)
        assertEquals(10, afterNoise.droppedFrames)

        // Значимое изменение публикуется сразу.
        viewModel.updateCameraMetrics(
            "1",
            CameraPlaybackMetrics(renderFps = 14, droppedFrames = 11)
        )
        val afterSignificant = viewModel.state.value.cameraMetrics["1"]!!
        assertEquals(14, afterSignificant.renderFps)
    }

    @Test
    fun `updateCameraMetrics should respect custom publish config`() = runTest(testDispatcher) {
        val cameras = listOf(
            Camera(id = "1", name = "Camera 1", url = "http://test.com")
        )
        whenever(getCamerasUseCase.invoke()).thenReturn(cameras)

        val viewModel = LiveViewViewModel(
            getCamerasUseCase = getCamerasUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher),
            metricsPublishConfig = MetricsPublishConfig(
                minMetricsPublishIntervalMs = 10_000L,
                minRenderFpsDeltaToPublish = 5,
                minDroppedFramesDeltaToPublish = 10
            )
        )
        viewModel.loadCameras()
        advanceUntilIdle()
        viewModel.selectCamera("1")

        viewModel.updateCameraMetrics("1", CameraPlaybackMetrics(renderFps = 20, droppedFrames = 100))
        val baseline = viewModel.state.value.cameraMetrics["1"]!!
        assertEquals(20, baseline.renderFps)
        assertEquals(100, baseline.droppedFrames)

        // Ниже кастомных порогов — апдейт должен быть отброшен.
        viewModel.updateCameraMetrics("1", CameraPlaybackMetrics(renderFps = 23, droppedFrames = 105))
        val afterSmallDelta = viewModel.state.value.cameraMetrics["1"]!!
        assertEquals(20, afterSmallDelta.renderFps)
        assertEquals(100, afterSmallDelta.droppedFrames)

        // Достаточный delta fps — апдейт должен пройти сразу.
        viewModel.updateCameraMetrics("1", CameraPlaybackMetrics(renderFps = 25, droppedFrames = 105))
        val afterThreshold = viewModel.state.value.cameraMetrics["1"]!!
        assertEquals(25, afterThreshold.renderFps)
    }
}
