#include <jni.h>
#include <string>
#include <memory>
#include <vector>
#include "motion_detector.h"
#include "object_detector.h"
#include "object_tracker.h"
#include "face_detector.h"
#include "anpr_engine.h"

// Хранилище детекторов и трекеров
struct DetectorHandle {
    MotionDetector* motionDetector = nullptr;
    ObjectDetector* objectDetector = nullptr;
    ObjectTracker* objectTracker = nullptr;
    FaceDetector* faceDetector = nullptr;
    ANPREngine* anprEngine = nullptr;
};

// Версия нативной библиотеки аналитики (для тестов и отладки)
static const char* const kAnalyticsNativeVersion = "1.0.0-phase1";

// Инициализация JNI
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    (void)reserved;
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}

// Выгрузка JNI
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    (void)vm;
    (void)reserved;
}

// ==================== NativeAnalytics API ====================
// Новые JNI методы для соответствия NativeAnalytics API

// Motion Detector
extern "C" JNIEXPORT jlong JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeCreateMotionDetector(
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
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeDestroyMotionDetector(
    JNIEnv* env, jobject thiz, jlong handle) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (h && h->motionDetector) {
        motion_detector_destroy(h->motionDetector);
        h->motionDetector = nullptr;
        if (!h->objectDetector && !h->objectTracker && !h->faceDetector && !h->anprEngine) {
            delete h;
        }
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeDetectMotion(
    JNIEnv* env, jobject thiz, jlong handle, jbyteArray frameData, jint width, jint height) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (!h || !h->motionDetector) {
        return nullptr;
    }

    jbyte* data = env->GetByteArrayElements(frameData, nullptr);
    if (!data) {
        return nullptr;
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

    if (!success) {
        return nullptr;
    }

    // Создаем Java объект MotionDetectionResult
    jclass resultClass = env->FindClass("com/company/ipcamera/core/network/analytics/MotionDetectionResult");
    if (!resultClass) {
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "(ZFIIII)V");
    if (!constructor) {
        return nullptr;
    }

    jobject result = env->NewObject(resultClass, constructor,
        nativeResult.motionDetected ? JNI_TRUE : JNI_FALSE,
        nativeResult.confidence,
        nativeResult.x,
        nativeResult.y,
        nativeResult.width,
        nativeResult.height
    );

    return result;
}

// Object Detector
extern "C" JNIEXPORT jlong JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeCreateObjectDetector(
    JNIEnv* env, jobject thiz, jfloat confidenceThreshold, jint maxObjects, jboolean useGPU, jint inputSize) {
    ObjectDetectorParams params;
    params.confidenceThreshold = confidenceThreshold;
    params.maxObjects = maxObjects;
    params.useGPU = useGPU == JNI_TRUE;
    int size = (inputSize > 0) ? inputSize : 640;
    params.inputWidth = size;
    params.inputHeight = size;

    ObjectDetector* detector = object_detector_create(&params);
    if (!detector) {
        return 0;
    }

    DetectorHandle* handle = new DetectorHandle();
    handle->objectDetector = detector;
    return reinterpret_cast<jlong>(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeDestroyObjectDetector(
    JNIEnv* env, jobject thiz, jlong handle) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (h && h->objectDetector) {
        object_detector_destroy(h->objectDetector);
        h->objectDetector = nullptr;
        if (!h->motionDetector && !h->objectTracker && !h->faceDetector && !h->anprEngine) {
            delete h;
        }
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeLoadObjectDetectorModel(
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

extern "C" JNIEXPORT jobject JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeDetectObjects(
    JNIEnv* env, jobject thiz, jlong handle, jbyteArray frameData, jint width, jint height) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (!h || !h->objectDetector) {
        return nullptr;
    }

    jbyte* data = env->GetByteArrayElements(frameData, nullptr);
    if (!data) {
        return nullptr;
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

    if (!success) {
        return nullptr;
    }

    // Создаем Java объект ObjectDetectionResult
    jclass resultClass = env->FindClass("com/company/ipcamera/core/network/analytics/ObjectDetectionResult");
    jclass objectClass = env->FindClass("com/company/ipcamera/core/network/analytics/DetectedObject");
    jclass objectTypeClass = env->FindClass("com/company/ipcamera/core/network/analytics/ObjectType");

    if (!resultClass || !objectClass || !objectTypeClass) {
        detection_result_release(&nativeResult);
        return nullptr;
    }

    // Создаем список объектов
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "(I)V");
    jmethodID arrayListAdd = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jmethodID objectConstructor = env->GetMethodID(objectClass, "<init>",
        "(Lcom/company/ipcamera/core/network/analytics/ObjectType;FIIII)V");
    jmethodID objectTypeValueOf = env->GetStaticMethodID(objectTypeClass, "valueOf",
        "(Ljava/lang/String;)Lcom/company/ipcamera/core/network/analytics/ObjectType;");

    jobject objectsList = env->NewObject(arrayListClass, arrayListConstructor, nativeResult.objectCount);

    for (int i = 0; i < nativeResult.objectCount; i++) {
        const char* typeName = nullptr;
        switch (nativeResult.objects[i].type) {
            case OBJECT_TYPE_PERSON:
                typeName = "PERSON";
                break;
            case OBJECT_TYPE_VEHICLE:
                typeName = "VEHICLE";
                break;
            case OBJECT_TYPE_BICYCLE:
                typeName = "BICYCLE";
                break;
            case OBJECT_TYPE_MOTORCYCLE:
                typeName = "MOTORCYCLE";
                break;
            default:
                typeName = "UNKNOWN";
                break;
        }

        jstring typeNameStr = env->NewStringUTF(typeName);
        jobject objectType = env->CallStaticObjectMethod(objectTypeClass, objectTypeValueOf, typeNameStr);
        env->DeleteLocalRef(typeNameStr);

        jobject obj = env->NewObject(objectClass, objectConstructor,
            objectType,
            nativeResult.objects[i].confidence,
            nativeResult.objects[i].x,
            nativeResult.objects[i].y,
            nativeResult.objects[i].width,
            nativeResult.objects[i].height
        );

        env->CallBooleanMethod(objectsList, arrayListAdd, obj);
        env->DeleteLocalRef(obj);
        env->DeleteLocalRef(objectType);
    }

    detection_result_release(&nativeResult);

    // Создаем ObjectDetectionResult
    jmethodID resultConstructor = env->GetMethodID(resultClass, "<init>", "(Ljava/util/List;)V");
    jobject result = env->NewObject(resultClass, resultConstructor, objectsList);

    return result;
}

// Face Detector
extern "C" JNIEXPORT jlong JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeCreateFaceDetector(
    JNIEnv* env, jobject thiz, jfloat scaleFactor, jint minNeighbors, jint minSize, jint maxSize) {
    FaceDetectorParams params;
    params.scaleFactor = scaleFactor;
    params.minNeighbors = minNeighbors;
    params.minSize = minSize;
    params.maxSize = maxSize;

    FaceDetector* detector = face_detector_create(&params);
    if (!detector) {
        return 0;
    }

    DetectorHandle* handle = new DetectorHandle();
    handle->faceDetector = detector;
    return reinterpret_cast<jlong>(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeDestroyFaceDetector(
    JNIEnv* env, jobject thiz, jlong handle) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (h && h->faceDetector) {
        face_detector_destroy(h->faceDetector);
        h->faceDetector = nullptr;
        if (!h->motionDetector && !h->objectDetector && !h->objectTracker && !h->anprEngine) {
            delete h;
        }
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeLoadFaceDetectorCascade(
    JNIEnv* env, jobject thiz, jlong handle, jstring cascadePath) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (!h || !h->faceDetector) {
        return JNI_FALSE;
    }

    const char* path = env->GetStringUTFChars(cascadePath, nullptr);
    if (!path) {
        return JNI_FALSE;
    }

    bool success = face_detector_load_cascade(h->faceDetector, path);
    env->ReleaseStringUTFChars(cascadePath, path);

    return success ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeDetectFaces(
    JNIEnv* env, jobject thiz, jlong handle, jbyteArray frameData, jint width, jint height) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (!h || !h->faceDetector) {
        return nullptr;
    }

    jbyte* data = env->GetByteArrayElements(frameData, nullptr);
    if (!data) {
        return nullptr;
    }

    FaceDetectionResult nativeResult;
    bool success = face_detector_detect(
        h->faceDetector,
        reinterpret_cast<const uint8_t*>(data),
        width,
        height,
        &nativeResult
    );

    env->ReleaseByteArrayElements(frameData, data, JNI_ABORT);

    if (!success) {
        return nullptr;
    }

    // Создаем Java объект FaceDetectionResult
    jclass resultClass = env->FindClass("com/company/ipcamera/core/network/analytics/FaceDetectionResult");
    jclass faceClass = env->FindClass("com/company/ipcamera/core/network/analytics/DetectedFace");
    jclass arrayListClass = env->FindClass("java/util/ArrayList");

    if (!resultClass || !faceClass || !arrayListClass) {
        face_detection_result_release(&nativeResult);
        return nullptr;
    }

    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "(I)V");
    jmethodID arrayListAdd = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jmethodID faceConstructor = env->GetMethodID(faceClass, "<init>", "(FIIII[I)V");
    jmethodID resultConstructor = env->GetMethodID(resultClass, "<init>", "(Ljava/util/List;)V");

    jobject facesList = env->NewObject(arrayListClass, arrayListConstructor, nativeResult.faceCount);

    for (int i = 0; i < nativeResult.faceCount; i++) {
        jintArray landmarksArray = nullptr;
        if (nativeResult.faces[i].landmarks) {
            landmarksArray = env->NewIntArray(10);
            env->SetIntArrayRegion(landmarksArray, 0, 10, nativeResult.faces[i].landmarks);
        }

        jobject face = env->NewObject(faceClass, faceConstructor,
            nativeResult.faces[i].confidence,
            nativeResult.faces[i].x,
            nativeResult.faces[i].y,
            nativeResult.faces[i].width,
            nativeResult.faces[i].height,
            landmarksArray
        );

        env->CallBooleanMethod(facesList, arrayListAdd, face);
        env->DeleteLocalRef(face);
        if (landmarksArray) {
            env->DeleteLocalRef(landmarksArray);
        }
    }

    face_detection_result_release(&nativeResult);

    jobject result = env->NewObject(resultClass, resultConstructor, facesList);
    return result;
}

// ANPR Engine
extern "C" JNIEXPORT jlong JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeCreateANPREngine(
    JNIEnv* env, jobject thiz, jfloat confidenceThreshold, jstring language) {
    ANPREngineParams params;
    params.confidenceThreshold = confidenceThreshold;

    const char* lang = env->GetStringUTFChars(language, nullptr);
    if (!lang) {
        return 0;
    }
    params.language = lang;

    ANPREngine* engine = anpr_engine_create(&params);
    env->ReleaseStringUTFChars(language, lang);

    if (!engine) {
        return 0;
    }

    // Инициализируем OCR
    if (!anpr_engine_init_ocr(engine)) {
        anpr_engine_destroy(engine);
        return 0;
    }

    DetectorHandle* handle = new DetectorHandle();
    handle->anprEngine = engine;
    return reinterpret_cast<jlong>(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeDestroyANPREngine(
    JNIEnv* env, jobject thiz, jlong handle) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (h && h->anprEngine) {
        anpr_engine_destroy(h->anprEngine);
        h->anprEngine = nullptr;
        if (!h->motionDetector && !h->objectDetector && !h->objectTracker && !h->faceDetector) {
            delete h;
        }
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeRecognizeLicensePlates(
    JNIEnv* env, jobject thiz, jlong handle, jbyteArray frameData, jint width, jint height) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (!h || !h->anprEngine) {
        return nullptr;
    }

    jbyte* data = env->GetByteArrayElements(frameData, nullptr);
    if (!data) {
        return nullptr;
    }

    ANPRResult nativeResult;
    bool success = anpr_engine_recognize(
        h->anprEngine,
        reinterpret_cast<const uint8_t*>(data),
        width,
        height,
        &nativeResult
    );

    env->ReleaseByteArrayElements(frameData, data, JNI_ABORT);

    if (!success) {
        return nullptr;
    }

    // Создаем Java объект ANPRResult
    jclass resultClass = env->FindClass("com/company/ipcamera/core/network/analytics/ANPRResult");
    jclass plateClass = env->FindClass("com/company/ipcamera/core/network/analytics/RecognizedPlate");
    jclass arrayListClass = env->FindClass("java/util/ArrayList");

    if (!resultClass || !plateClass || !arrayListClass) {
        anpr_result_release(&nativeResult);
        return nullptr;
    }

    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "(I)V");
    jmethodID arrayListAdd = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jmethodID plateConstructor = env->GetMethodID(plateClass, "<init>", "(Ljava/lang/String;FIIII)V");
    jmethodID resultConstructor = env->GetMethodID(resultClass, "<init>", "(Ljava/util/List;)V");

    jobject platesList = env->NewObject(arrayListClass, arrayListConstructor, nativeResult.plateCount);

    for (int i = 0; i < nativeResult.plateCount; i++) {
        jstring text = env->NewStringUTF(nativeResult.plates[i].text ? nativeResult.plates[i].text : "");

        jobject plate = env->NewObject(plateClass, plateConstructor,
            text,
            nativeResult.plates[i].confidence,
            nativeResult.plates[i].x,
            nativeResult.plates[i].y,
            nativeResult.plates[i].width,
            nativeResult.plates[i].height
        );

        env->CallBooleanMethod(platesList, arrayListAdd, plate);
        env->DeleteLocalRef(plate);
        env->DeleteLocalRef(text);
    }

    anpr_result_release(&nativeResult);

    jobject result = env->NewObject(resultClass, resultConstructor, platesList);
    return result;
}

// Object Tracker
extern "C" JNIEXPORT jlong JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeCreateObjectTracker(
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
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeDestroyObjectTracker(
    JNIEnv* env, jobject thiz, jlong handle) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (h && h->objectTracker) {
        object_tracker_destroy(h->objectTracker);
        h->objectTracker = nullptr;
        if (!h->motionDetector && !h->objectDetector && !h->faceDetector && !h->anprEngine) {
            delete h;
        }
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeUpdateTracking(
    JNIEnv* env, jobject thiz, jlong handle, jobject detections, jlong timestampMs) {
    DetectorHandle* h = reinterpret_cast<DetectorHandle*>(handle);
    if (!h || !h->objectTracker) {
        return nullptr;
    }

    // Конвертируем Java ObjectDetectionResult в нативный DetectionResult
    jclass detectionsClass = env->GetObjectClass(detections);
    jmethodID getObjects = env->GetMethodID(detectionsClass, "getObjects", "()Ljava/util/List;");

    jobject objectsList = env->CallObjectMethod(detections, getObjects);
    jclass listClass = env->GetObjectClass(objectsList);
    jmethodID listSize = env->GetMethodID(listClass, "size", "()I");
    jmethodID listGet = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");

    jint count = env->CallIntMethod(objectsList, listSize);

    DetectionResult nativeDetections;
    nativeDetections.objectCount = count;
    if (count > 0) {
        nativeDetections.objects = new DetectedObject[count];
        jclass objectClass = env->FindClass("com/company/ipcamera/core/network/analytics/DetectedObject");
        jmethodID getType = env->GetMethodID(objectClass, "getType", "()Lcom/company/ipcamera/core/network/analytics/ObjectType;");
        jmethodID getConfidence = env->GetMethodID(objectClass, "getConfidence", "()F");
        jmethodID getX = env->GetMethodID(objectClass, "getX", "()I");
        jmethodID getY = env->GetMethodID(objectClass, "getY", "()I");
        jmethodID getWidth = env->GetMethodID(objectClass, "getWidth", "()I");
        jmethodID getHeight = env->GetMethodID(objectClass, "getHeight", "()I");

        jclass objectTypeClass = env->FindClass("com/company/ipcamera/core/network/analytics/ObjectType");
        jmethodID objectTypeName = env->GetMethodID(objectTypeClass, "name", "()Ljava/lang/String;");

        for (int i = 0; i < count; i++) {
            jobject obj = env->CallObjectMethod(objectsList, listGet, i);
            jobject typeObj = env->CallObjectMethod(obj, getType);
            jstring typeNameStr = (jstring)env->CallObjectMethod(typeObj, objectTypeName);
            const char* typeName = env->GetStringUTFChars(typeNameStr, nullptr);

            ObjectType type = OBJECT_TYPE_UNKNOWN;
            if (strcmp(typeName, "PERSON") == 0) {
                type = OBJECT_TYPE_PERSON;
            } else if (strcmp(typeName, "VEHICLE") == 0) {
                type = OBJECT_TYPE_VEHICLE;
            } else if (strcmp(typeName, "BICYCLE") == 0) {
                type = OBJECT_TYPE_BICYCLE;
            } else if (strcmp(typeName, "MOTORCYCLE") == 0) {
                type = OBJECT_TYPE_MOTORCYCLE;
            }

            nativeDetections.objects[i].type = type;
            nativeDetections.objects[i].confidence = env->CallFloatMethod(obj, getConfidence);
            nativeDetections.objects[i].x = env->CallIntMethod(obj, getX);
            nativeDetections.objects[i].y = env->CallIntMethod(obj, getY);
            nativeDetections.objects[i].width = env->CallIntMethod(obj, getWidth);
            nativeDetections.objects[i].height = env->CallIntMethod(obj, getHeight);

            env->ReleaseStringUTFChars(typeNameStr, typeName);
            env->DeleteLocalRef(typeNameStr);
            env->DeleteLocalRef(typeObj);
            env->DeleteLocalRef(obj);
        }
    } else {
        nativeDetections.objects = nullptr;
    }

    TrackingResult nativeResult;
    bool success = object_tracker_update(h->objectTracker, &nativeDetections,
        static_cast<int64_t>(timestampMs), &nativeResult);

    // Освобождаем память
    if (nativeDetections.objects) {
        delete[] nativeDetections.objects;
    }

    if (!success) {
        return nullptr;
    }

    // Создаем Java объект TrackingResult
    jclass resultClass = env->FindClass("com/company/ipcamera/core/network/analytics/TrackingResult");
    jclass trackedObjectClass = env->FindClass("com/company/ipcamera/core/network/analytics/TrackedObject");
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jclass objectTypeClass = env->FindClass("com/company/ipcamera/core/network/analytics/ObjectType");

    if (!resultClass || !trackedObjectClass || !arrayListClass || !objectTypeClass) {
        tracking_result_release(&nativeResult);
        return nullptr;
    }

    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "(I)V");
    jmethodID arrayListAdd = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jmethodID trackedObjectConstructor = env->GetMethodID(trackedObjectClass, "<init>",
        "(ILcom/company/ipcamera/core/network/analytics/ObjectType;FIIIIJ)V");
    jmethodID resultConstructor = env->GetMethodID(resultClass, "<init>", "(Ljava/util/List;)V");
    jmethodID objectTypeValueOf = env->GetStaticMethodID(objectTypeClass, "valueOf",
        "(Ljava/lang/String;)Lcom/company/ipcamera/core/network/analytics/ObjectType;");

    jobject trackedList = env->NewObject(arrayListClass, arrayListConstructor, nativeResult.objectCount);

    for (int i = 0; i < nativeResult.objectCount; i++) {
        const char* typeName = nullptr;
        switch (nativeResult.objects[i].type) {
            case OBJECT_TYPE_PERSON:
                typeName = "PERSON";
                break;
            case OBJECT_TYPE_VEHICLE:
                typeName = "VEHICLE";
                break;
            case OBJECT_TYPE_BICYCLE:
                typeName = "BICYCLE";
                break;
            case OBJECT_TYPE_MOTORCYCLE:
                typeName = "MOTORCYCLE";
                break;
            default:
                typeName = "UNKNOWN";
                break;
        }

        jstring typeNameStr = env->NewStringUTF(typeName);
        jobject objectType = env->CallStaticObjectMethod(objectTypeClass, objectTypeValueOf, typeNameStr);
        env->DeleteLocalRef(typeNameStr);

        jobject trackedObj = env->NewObject(trackedObjectClass, trackedObjectConstructor,
            nativeResult.objects[i].id,
            objectType,
            nativeResult.objects[i].confidence,
            nativeResult.objects[i].x,
            nativeResult.objects[i].y,
            nativeResult.objects[i].width,
            nativeResult.objects[i].height,
            nativeResult.objects[i].lastSeen
        );

        env->CallBooleanMethod(trackedList, arrayListAdd, trackedObj);
        env->DeleteLocalRef(trackedObj);
        env->DeleteLocalRef(objectType);
    }

    tracking_result_release(&nativeResult);

    jobject result = env->NewObject(resultClass, resultConstructor, trackedList);
    return result;
}

// ==================== Версия библиотеки (инфраструктура, фаза 1) ====================
extern "C" JNIEXPORT jstring JNICALL
Java_com_company_ipcamera_core_network_analytics_NativeAnalytics_nativeGetVersion(
    JNIEnv* env, jobject thiz) {
    (void)thiz;
    return env->NewStringUTF(kAnalyticsNativeVersion);
}
