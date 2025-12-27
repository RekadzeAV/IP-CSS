package com.company.ipcamera.core.network.native

import kotlinx.cinterop.*
import com.company.ipcamera.core.network.native.analytics.*

/**
 * Kotlin обертка для нативного ObjectDetector
 */
class ObjectDetectorWrapper private constructor(
    private val detector: CPointer<ObjectDetectorVar>
) {
    companion object {
        fun create(
            confidenceThreshold: Float = 0.5f,
            maxObjects: Int = 10,
            useGPU: Boolean = false
        ): ObjectDetectorWrapper? {
            return memScoped {
                val params = alloc<ObjectDetectorParams>()
                params.confidenceThreshold = confidenceThreshold
                params.maxObjects = maxObjects
                params.useGPU = useGPU

                val detector = object_detector_create(params.ptr)
                detector?.let { ObjectDetectorWrapper(it) }
            }
        }
    }

    fun loadModel(modelPath: String): Boolean {
        return modelPath.cstr.usePinned { pinned ->
            object_detector_load_model(detector, pinned.addressOf(0))
        }
    }

    fun detect(frameData: ByteArray, width: Int, height: Int): DetectionResult? {
        return memScoped {
            val result = alloc<DetectionResultVar>()
            val dataPtr = allocArray<UByteVar>(frameData.size)
            frameData.forEachIndexed { index, byte ->
                dataPtr[index] = byte.toUByte()
            }

            if (object_detector_detect(detector, dataPtr, width, height, result.ptr)) {
                val objects = mutableListOf<DetectedObject>()
                result.pointed.objects?.let { objectsPtr ->
                    for (i in 0 until result.pointed.objectCount) {
                        val obj = objectsPtr[i]
                        objects.add(DetectedObject(
                            type = obj.type,
                            confidence = obj.confidence,
                            x = obj.x,
                            y = obj.y,
                            width = obj.width,
                            height = obj.height
                        ))
                    }
                }
                DetectionResult(objects)
            } else {
                null
            }
        }
    }

    fun release() {
        object_detector_destroy(detector)
    }
}

/**
 * Kotlin обертка для нативного ANPREngine
 */
class ANPREngineWrapper private constructor(
    private val engine: CPointer<ANPREngineVar>
) {
    companion object {
        fun create(
            confidenceThreshold: Float = 0.5f,
            language: String = "eng"
        ): ANPREngineWrapper? {
            return memScoped {
                val params = alloc<ANPREngineParams>()
                params.confidenceThreshold = confidenceThreshold
                params.language = language.cstr.ptr

                val engine = anpr_engine_create(params.ptr)
                engine?.let {
                    val wrapper = ANPREngineWrapper(it)
                    if (wrapper.initOCR()) {
                        wrapper
                    } else {
                        anpr_engine_destroy(it)
                        null
                    }
                }
            }
        }
    }

    private fun initOCR(): Boolean {
        return anpr_engine_init_ocr(engine)
    }

    fun recognize(frameData: ByteArray, width: Int, height: Int): ANPRResult? {
        return memScoped {
            val result = alloc<ANPRResultVar>()
            val dataPtr = allocArray<UByteVar>(frameData.size)
            frameData.forEachIndexed { index, byte ->
                dataPtr[index] = byte.toUByte()
            }

            if (anpr_engine_recognize(engine, dataPtr, width, height, result.ptr)) {
                val plates = mutableListOf<RecognizedPlate>()
                result.pointed.plates?.let { platesPtr ->
                    for (i in 0 until result.pointed.plateCount) {
                        val plate = platesPtr[i]
                        plates.add(RecognizedPlate(
                            text = plate.text?.toKString() ?: "",
                            confidence = plate.confidence,
                            x = plate.x,
                            y = plate.y,
                            width = plate.width,
                            height = plate.height
                        ))
                    }
                }
                ANPRResult(plates)
            } else {
                null
            }
        }
    }

    fun release() {
        anpr_engine_destroy(engine)
    }
}

/**
 * Результат детекции объектов
 */
data class DetectionResult(
    val objects: List<DetectedObject>
)

/**
 * Обнаруженный объект
 */
data class DetectedObject(
    val type: ObjectType,
    val confidence: Float,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

/**
 * Результат распознавания номеров
 */
data class ANPRResult(
    val plates: List<RecognizedPlate>
)

/**
 * Распознанный номерной знак
 */
data class RecognizedPlate(
    val text: String,
    val confidence: Float,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

