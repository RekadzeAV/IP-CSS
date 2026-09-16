package com.company.ipcamera.desktop.ui.viewmodel

import com.company.ipcamera.desktop.data.CacheManager
import com.company.ipcamera.desktop.data.CacheKeys
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.usecase.DeleteCameraUseCase
import com.company.ipcamera.shared.domain.usecase.DiscoverCamerasUseCase
import com.company.ipcamera.shared.domain.usecase.GetCamerasUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CamerasViewModelTest {

    private lateinit var getCamerasUseCase: GetCamerasUseCase
    private lateinit var deleteCameraUseCase: DeleteCameraUseCase
    private lateinit var discoverCamerasUseCase: DiscoverCamerasUseCase
    private lateinit var cacheManager: CacheManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        getCamerasUseCase = mock()
        deleteCameraUseCase = mock()
        discoverCamerasUseCase = mock()
        cacheManager = CacheManager()
    }

    @Test
    fun `loadCameras returns Success when use case provides data`() = runTest(testDispatcher) {
        val cameras = listOf(
            Camera(id = "1", name = "Camera 1", url = "http://test.com"),
            Camera(id = "2", name = "Camera 2", url = "http://test2.com")
        )
        whenever(getCamerasUseCase.invoke()).thenReturn(cameras)

        val viewModel = CamerasViewModel(
            getCamerasUseCase = getCamerasUseCase,
            deleteCameraUseCase = deleteCameraUseCase,
            discoverCamerasUseCase = discoverCamerasUseCase,
            cacheManager = cacheManager,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        viewModel.loadCameras(useCache = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CamerasUiState.Success)
        assertEquals(cameras, state.cameras)
    }

    @Test
    fun `loadCameras returns Error on exception`() = runTest(testDispatcher) {
        whenever(getCamerasUseCase.invoke()).thenThrow(RuntimeException("Network error"))

        val viewModel = CamerasViewModel(
            getCamerasUseCase = getCamerasUseCase,
            deleteCameraUseCase = deleteCameraUseCase,
            discoverCamerasUseCase = discoverCamerasUseCase,
            cacheManager = cacheManager,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        viewModel.loadCameras(useCache = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CamerasUiState.Error)
    }

    @Test
    fun `deleteCamera calls use case and keeps state consistent`() = runTest(testDispatcher) {
        val cameraId = "1"
        whenever(deleteCameraUseCase.invoke(cameraId)).thenReturn(Result.success(Unit))
        whenever(getCamerasUseCase.invoke()).thenReturn(emptyList())

        val viewModel = CamerasViewModel(
            getCamerasUseCase = getCamerasUseCase,
            deleteCameraUseCase = deleteCameraUseCase,
            discoverCamerasUseCase = discoverCamerasUseCase,
            cacheManager = cacheManager,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        viewModel.deleteCamera(cameraId)
        advanceUntilIdle()

        verify(deleteCameraUseCase).invoke(cameraId)
        assertTrue(viewModel.uiState.value is CamerasUiState.Empty)
    }

    @Test
    fun `getFilteredCameras should return filtered cameras based on search query`() = runTest(testDispatcher) {
        // Arrange
        val cameras = listOf(
            Camera(id = "1", name = "Camera 1", url = "http://test.com", model = "Model A"),
            Camera(id = "2", name = "Camera 2", url = "http://test2.com", model = "Model B")
        )
        whenever(getCamerasUseCase.invoke()).thenReturn(cameras)

        val viewModel = CamerasViewModel(
            getCamerasUseCase = getCamerasUseCase,
            deleteCameraUseCase = deleteCameraUseCase,
            discoverCamerasUseCase = discoverCamerasUseCase,
            cacheManager = cacheManager,
            coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        viewModel.loadCameras(useCache = false)
        advanceUntilIdle()

        // Act
        viewModel.updateSearchQuery("Camera 1")
        val filtered = viewModel.getFilteredCameras()

        // Assert
        assertEquals(1, filtered.size)
        assertEquals("Camera 1", filtered.first().name)
    }
}
