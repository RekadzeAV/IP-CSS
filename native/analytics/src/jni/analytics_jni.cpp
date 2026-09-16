#include <jni.h>
#include <string>
#include <memory>
#include <vector>
#include "motion_detector.h"
#include "object_detector.h"
#include "object_tracker.h"

// Глобальные ссылки на Java классы
static jclass motionResultClass = nullptr;
static jclass objectResultClass = nullptr;
static jclass detectedObjectClass = nullptr;
static jclass trackingResultClass = nullptr;
static jclass trackedObjectClass = nullptr;

// Методы для получения полей
static jfieldID motionDetectedField = nullptr;
static jfieldID confidenceField = nullptr;
static jfieldID xField = nullptr;
static jfieldID yField = nullptr;
static jfieldID widthField = nullptr;
static jfieldID heightField = nullptr;
static jfieldID objectCountField = nullptr;
static jfieldID objectsField = nullptr;
static jfieldID objectTypeField = nullptr;
static jfieldID trackIdField = nullptr;
static jfieldID lastSeenField = nullptr;

// Хранилище детекторов и трекеров
struct DetectorHandle {
    MotionDetector* motionDetector = nullptr;
    ObjectDetector* objectDetector = nullptr;
    ObjectTracker* objectTracker = nullptr;
};

// Инициализация JNI
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    // Здесь можно загрузить классы и поля, если нужно
    return JNI_VERSION_1_6;
}

// ==================== Motion Detector ====================

extern "C" JNIEXPORT jlong JNICALL
Java_com_company_ipcamera_shared_analytics_native_NativeMotionDetector_nativeCreate(
    JNIEnv* env, jobject thiz, jint width, jint height, jfloat threshold, jint minArea) {
    MotionDetectorParams params;
    params.threshold = threshold;
    params.minArea = minArea;
    params.useGaussianBlur = true;
    params.blurSize = 5;

    MotionDetector* detector = motion_detector_create(width, height, &params);
    if (!detector) {
        return 0;
    }

    DetectorHandle* handle = new DetectorHandle();
    handle->motionDetector = detector;
    return reinterpret_cast<jlong>(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_company_ipcamera_shared_analytics_native_NativeMotionDetector_nativeDestroy(
    JNIEnv* env, jobject thiz, jlong handle) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (h && h->motionDetector) {
        motion_detector_destroy(h->motionDetector);
        delete h;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_company_ipcamera_shared_analytics_native_NativeMotionDetector_nativeDetect(
    JNIEnv* env, jobject thiz, jlong handle, jbyteArray frameData, jint width, jint height, jobject result) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (!h || !h->motionDetector) {
        return JNI_FALSE;
    }

    jbyte* data = env->GetByteArrayElements(frameData, nullptr);
    if (!data) {
        return JNI_FALSE;
    }

    MotionDetectionResult nativeResult;
    bool success = motion_detector_detect(
        h->motionDetector,
        reinterpret_cast<const uint8_t*>(data),
        width,
        height,
        &nativeResult
    );

    env->ReleaseByteArrayElements(frameData, data, JNI_ABORT);

    if (success && result) {
        // Заполняем Java объект результата
        jclass cls = env->GetObjectClass(result);
        if (cls) {
            jfieldID field = env->GetFieldID(cls, "motionDetected", "Z");
            if (field) env->SetBooleanField(result, field, nativeResult.motionDetected ? JNI_TRUE : JNI_FALSE);

            field = env->GetFieldID(cls, "confidence", "F");
            if (field) env->SetFloatField(result, field, nativeResult.confidence);

            field = env->GetFieldID(cls, "x", "I");
            if (field) env->SetIntField(result, field, nativeResult.x);

            field = env->GetFieldID(cls, "y", "I");
            if (field) env->SetIntField(result, field, nativeResult.y);

            field = env->GetFieldID(cls, "width", "I");
            if (field) env->SetIntField(result, field, nativeResult.width);

            field = env->GetFieldID(cls, "height", "I");
            if (field) env->SetIntField(result, field, nativeResult.height);
        }
    }

    return success ? JNI_TRUE : JNI_FALSE;
}

// ==================== Object Detector ====================

extern "C" JNIEXPORT jlong JNICALL
Java_com_company_ipcamera_shared_analytics_native_NativeObjectDetector_nativeCreate(
    JNIEnv* env, jobject thiz, jfloat confidenceThreshold, jint maxObjects, jboolean useGPU) {
    ObjectDetectorParams params;
    params.confidenceThreshold = confidenceThreshold;
    params.maxObjects = maxObjects;
    params.useGPU = useGPU == JNI_TRUE;

    ObjectDetector* detector = object_detector_create(&params);
    if (!detector) {
        return 0;
    }

    DetectorHandle* handle = new DetectorHandle();
    handle->objectDetector = detector;
    return reinterpret_cast<jlong>(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_company_ipcamera_shared_analytics_native_NativeObjectDetector_nativeDestroy(
    JNIEnv* env, jobject thiz, jlong handle) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (h && h->objectDetector) {
        object_detector_destroy(h->objectDetector);
        delete h;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_company_ipcamera_shared_analytics_native_NativeObjectDetector_nativeLoadModel(
    JNIEnv* env, jobject thiz, jlong handle, jstring modelPath) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (!h || !h->objectDetector) {
        return JNI_FALSE;
    }

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    if (!path) {
        return JNI_FALSE;
    }

    bool success = object_detector_load_model(h->objectDetector, path);
    env->ReleaseStringUTFChars(modelPath, path);

    return success ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_company_ipcamera_shared_analytics_native_NativeObjectDetector_nativeIsModelLoaded(
    JNIEnv* env, jobject thiz, jlong handle) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (!h || !h->objectDetector) {
        return JNI_FALSE;
    }

    return object_detector_is_model_loaded(h->objectDetector) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_company_ipcamera_shared_analytics_native_NativeObjectDetector_nativeDetect(
    JNIEnv* env, jobject thiz, jlong handle, jbyteArray frameData, jint width, jint height, jobject result) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (!h || !h->objectDetector) {
        return JNI_FALSE;
    }

    jbyte* data = env->GetByteArrayElements(frameData, nullptr);
    if (!data) {
        return JNI_FALSE;
    }

    DetectionResult nativeResult;
    bool success = object_detector_detect(
        h->objectDetector,
        reinterpret_cast<const uint8_t*>(data),
        width,
        height,
        &nativeResult
    );

    env->ReleaseByteArrayElements(frameData, data, JNI_ABORT);

    if (success && result) {
        // Создаем массив объектов
        jclass cls = env->GetObjectClass(result);
        if (cls) {
            jfieldID countField = env->GetFieldID(cls, "objectCount", "I");
            if (countField) {
                env->SetIntField(result, countField, nativeResult.objectCount);
            }

            if (nativeResult.objectCount > 0) {
                // Находим класс DetectedObject
                jclass objectClass = env->FindClass("com/company/ipcamera/shared/analytics/native/DetectedObject");
                if (objectClass) {
                    jmethodID constructor = env->GetMethodID(objectClass, "<init>", "()V");
                    jfieldID typeField = env->GetFieldID(objectClass, "type", "I");
                    jfieldID confField = env->GetFieldID(objectClass, "confidence", "F");
                    jfieldID xField = env->GetFieldID(objectClass, "x", "I");
                    jfieldID yField = env->GetFieldID(objectClass, "y", "I");
                    jfieldID wField = env->GetFieldID(objectClass, "width", "I");
                    jfieldID hField = env->GetFieldID(objectClass, "height", "I");

                    jobjectArray objectArray = env->NewObjectArray(nativeResult.objectCount, objectClass, nullptr);
                    if (objectArray) {
                        for (int i = 0; i < nativeResult.objectCount; i++) {
                            jobject obj = env->NewObject(objectClass, constructor);
                            if (obj) {
                                env->SetIntField(obj, typeField, static_cast<jint>(nativeResult.objects[i].type));
                                env->SetFloatField(obj, confField, nativeResult.objects[i].confidence);
                                env->SetIntField(obj, xField, nativeResult.objects[i].x);
                                env->SetIntField(obj, yField, nativeResult.objects[i].y);
                                env->SetIntField(obj, wField, nativeResult.objects[i].width);
                                env->SetIntField(obj, hField, nativeResult.objects[i].height);
                                env->SetObjectArrayElement(objectArray, i, obj);
                            }
                        }

                        jfieldID objectsField = env->GetFieldID(cls, "objects", "[Lcom/company/ipcamera/shared/analytics/native/DetectedObject;");
                        if (objectsField) {
                            env->SetObjectField(result, objectsField, objectArray);
                        }
                    }
                }
            }

            // Освобождаем память результата (объекты уже скопированы в Java массив)
            detection_result_release(&nativeResult);
        } else {
            // Если не успешно, все равно освобождаем память
            detection_result_release(&nativeResult);
        }
    }

    return success ? JNI_TRUE : JNI_FALSE;
}

// ==================== Object Tracker ====================

extern "C" JNIEXPORT jlong JNICALL
Java_com_company_ipcamera_shared_analytics_native_NativeObjectTracker_nativeCreate(
    JNIEnv* env, jobject thiz, jfloat iouThreshold, jint maxAge, jfloat minConfidence) {
    ObjectTrackerParams params;
    params.iouThreshold = iouThreshold;
    params.maxAge = maxAge;
    params.minConfidence = minConfidence;

    ObjectTracker* tracker = object_tracker_create(&params);
    if (!tracker) {
        return 0;
    }

    DetectorHandle* handle = new DetectorHandle();
    handle->objectTracker = tracker;
    return reinterpret_cast<jlong>(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_company_ipcamera_shared_analytics_native_NativeObjectTracker_nativeDestroy(
    JNIEnv* env, jobject thiz, jlong handle) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (h && h->objectTracker) {
        object_tracker_destroy(h->objectTracker);
        delete h;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_company_ipcamera_shared_analytics_native_NativeObjectTracker_nativeUpdate(
    JNIEnv* env, jobject thiz, jlong handle, jobject detections, jobject result) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (!h || !h->objectTracker) {
        return JNI_FALSE;
    }

    // Конвертируем Java DetectionResult в нативный
    jclass detectionsClass = env->GetObjectClass(detections);
    jfieldID countField = env->GetFieldID(detectionsClass, "objectCount", "I");
    jfieldID objectsField = env->GetFieldID(detectionsClass, "objects", "[Lcom/company/ipcamera/shared/analytics/native/DetectedObject;");

    int count = env->GetIntField(detections, countField);
    jobjectArray objectsArray = static_cast<jobjectArray>(env->GetObjectField(detections, objectsField));

    DetectionResult nativeDetections;
    nativeDetections.objectCount = count;
    if (count > 0) {
        nativeDetections.objects = new DetectedObject[count];
        jclass objectClass = env->FindClass("com/company/ipcamera/shared/analytics/native/DetectedObject");
        jfieldID typeField = env->GetFieldID(objectClass, "type", "I");
        jfieldID confField = env->GetFieldID(objectClass, "confidence", "F");
        jfieldID xField = env->GetFieldID(objectClass, "x", "I");
        jfieldID yField = env->GetFieldID(objectClass, "y", "I");
        jfieldID wField = env->GetFieldID(objectClass, "width", "I");
        jfieldID hField = env->GetFieldID(objectClass, "height", "I");

        for (int i = 0; i < count; i++) {
            jobject obj = env->GetObjectArrayElement(objectsArray, i);
            nativeDetections.objects[i].type = static_cast<ObjectType>(env->GetIntField(obj, typeField));
            nativeDetections.objects[i].confidence = env->GetFloatField(obj, confField);
            nativeDetections.objects[i].x = env->GetIntField(obj, xField);
            nativeDetections.objects[i].y = env->GetIntField(obj, yField);
            nativeDetections.objects[i].width = env->GetIntField(obj, wField);
            nativeDetections.objects[i].height = env->GetIntField(obj, hField);
        }
    } else {
        nativeDetections.objects = nullptr;
    }

    TrackingResult nativeResult;
    bool success = object_tracker_update(h->objectTracker, &nativeDetections, 0LL, &nativeResult);

    // Освобождаем память
    if (nativeDetections.objects) {
        delete[] nativeDetections.objects;
    }

    if (success && result) {
        // Заполняем Java объект результата
        jclass resultClass = env->GetObjectClass(result);
        jfieldID countField = env->GetFieldID(resultClass, "objectCount", "I");
        if (countField) {
            env->SetIntField(result, countField, nativeResult.objectCount);
        }

        if (nativeResult.objectCount > 0) {
            jclass trackedObjectClass = env->FindClass("com/company/ipcamera/shared/analytics/native/TrackedObject");
            if (trackedObjectClass) {
                jmethodID constructor = env->GetMethodID(trackedObjectClass, "<init>", "()V");
                jfieldID idField = env->GetFieldID(trackedObjectClass, "id", "I");
                jfieldID typeField = env->GetFieldID(trackedObjectClass, "type", "I");
                jfieldID confField = env->GetFieldID(trackedObjectClass, "confidence", "F");
                jfieldID xField = env->GetFieldID(trackedObjectClass, "x", "I");
                jfieldID yField = env->GetFieldID(trackedObjectClass, "y", "I");
                jfieldID wField = env->GetFieldID(trackedObjectClass, "width", "I");
                jfieldID hField = env->GetFieldID(trackedObjectClass, "height", "I");
                jfieldID lastSeenField = env->GetFieldID(trackedObjectClass, "lastSeen", "J");

                jobjectArray trackedArray = env->NewObjectArray(nativeResult.objectCount, trackedObjectClass, nullptr);
                if (trackedArray) {
                    for (int i = 0; i < nativeResult.objectCount; i++) {
                        jobject obj = env->NewObject(trackedObjectClass, constructor);
                        if (obj) {
                            env->SetIntField(obj, idField, nativeResult.objects[i].id);
                            env->SetIntField(obj, typeField, static_cast<jint>(nativeResult.objects[i].type));
                            env->SetFloatField(obj, confField, nativeResult.objects[i].confidence);
                            env->SetIntField(obj, xField, nativeResult.objects[i].x);
                            env->SetIntField(obj, yField, nativeResult.objects[i].y);
                            env->SetIntField(obj, wField, nativeResult.objects[i].width);
                            env->SetIntField(obj, hField, nativeResult.objects[i].height);
                            env->SetLongField(obj, lastSeenField, nativeResult.objects[i].lastSeen);
                            env->SetObjectArrayElement(trackedArray, i, obj);
                        }
                    }

                    jfieldID objectsField = env->GetFieldID(resultClass, "objects", "[Lcom/company/ipcamera/shared/analytics/native/TrackedObject;");
                    if (objectsField) {
                        env->SetObjectField(result, objectsField, trackedArray);
                    }
                }
            }
        }

        tracking_result_release(&nativeResult);
    }

    return success ? JNI_TRUE : JNI_FALSE;
}
