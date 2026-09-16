@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.company.ipcamera.shared.domain.service

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.shared.domain.model.AnalyticsSettings
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.CameraSettings
import com.company.ipcamera.shared.domain.model.DetectionZone
import com.company.ipcamera.shared.domain.usecase.DetectFacesUseCase
import com.company.ipcamera.shared.domain.usecase.DetectMotionUseCase
import com.company.ipcamera.shared.domain.usecase.DetectObjectsUseCase
import com.company.ipcamera.shared.domain.usecase.RecognizeLicensePlateUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse

class AnalyticsFrameProcessorFactoryTest {
    private class FakeA : AnalyticsService {
        override suspend fun detectMotion(
            camera: Camera,
            frameData: ByteArray,
            previousFrameData: ByteArray?,
            zones: List<DetectionZone>,
            threshold: Float,
            minArea: Int?,
        ) = MotionDetectionResult(false, 0f)

        override suspend fun detectObjects(
            camera: Camera,
            frameData: ByteArray,
            objectTypes: List<String>,
            minConfidence: Float,
        ) = ObjectDetectionResult(emptyList())

        override suspend fun detectFaces(
            camera: Camera,
            frameData: ByteArray,
            minConfidence: Float,
            includeLandmarks: Boolean,
            includeEmbeddings: Boolean,
        ) = FaceDetectionResult(emptyList())

        override suspend fun recognizeLicensePlates(
            camera: Camera,
            frameData: ByteArray,
            minConfidence: Float,
            country: String?,
        ) = LicensePlateRecognitionResult(emptyList())

        override suspend fun trackObjects(
            camera: Camera,
            frameData: ByteArray,
        ) = emptyList<TrackInfo>()

        override suspend fun compareFaces(
            embedding1: FloatArray,
            embedding2: FloatArray,
        ) = 0f
    }

    @Test
    fun create_buildsProcessorThatRunsMotionPath() =
        runTest {
            val fake = FakeA()
            val factory =
                AnalyticsFrameProcessorFactory(
                    detectMotionUseCase = DetectMotionUseCase(fake, null),
                    detectObjectsUseCase = DetectObjectsUseCase(fake, null),
                    detectFacesUseCase = DetectFacesUseCase(fake, null),
                    recognizeLicensePlateUseCase = RecognizeLicensePlateUseCase(fake, null, null),
                )
            val cam =
                Camera(
                    id = "c",
                    name = "N",
                    url = "rtsp://x",
                    resolution = Resolution(2, 2),
                    status = CameraStatus.ONLINE,
                    settings = CameraSettings(analytics = AnalyticsSettings(motionDetection = true)),
                )
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val processor = factory.create(cam, scope = scope)
            processor.processFrame(ByteArray(2 * 2 * 3) { 1 }, 2, 2)
            processor.dispose()
            assertFalse(scope.isActive)
        }
}
