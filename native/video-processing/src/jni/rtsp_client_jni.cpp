#include <jni.h>
#include <string>
#include <android/log.h>
#include "../../include/rtsp_client.h"

#define LOG_TAG "RTSPClientJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Глобальные ссылки на Java классы и методы
static jclass g_RtspFrameClass = nullptr;
static jclass g_RtspClientStatusClass = nullptr;
static jclass g_RtspStreamTypeClass = nullptr;
static jmethodID g_RtspFrameConstructor = nullptr;
static jmethodID g_RtspClientStatusValueOf = nullptr;
static jmethodID g_RtspStreamTypeValueOf = nullptr;

// Структура для хранения callback'ов
struct CallbackData {
    JavaVM* jvm;
    jobject frameCallback;
    jobject statusCallback;
    jmethodID frameCallbackMethod;
    jmethodID statusCallbackMethod;
};

// C callback для кадров
void frameCallbackWrapper(RTSPFrame* frame, void* userData) {
    if (!frame || !userData) return;

    CallbackData* data = static_cast<CallbackData*>(userData);
    JNIEnv* env = nullptr;

    // Получаем JNI environment (может быть вызван из другого потока)
    jint result = data->jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (result == JNI_EDETACHED) {
        // Поток не прикреплен, прикрепляем его
        if (data->jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE("Failed to attach thread to JVM");
            rtsp_frame_release(frame);
            return;
        }
    } else if (result != JNI_OK) {
        LOGE("Failed to get JNI environment");
        rtsp_frame_release(frame);
        return;
    }

    if (!data->frameCallback || !data->frameCallbackMethod) {
        rtsp_frame_release(frame);
        return;
    }

    bool threadAttached = (result == JNI_EDETACHED);

    try {
        // Создаем Java byte array из данных кадра
        jbyteArray frameData = env->NewByteArray(frame->size);
        if (frameData) {
            env->SetByteArrayRegion(frameData, 0, frame->size,
                                   reinterpret_cast<const jbyte*>(frame->data));
        }

        // Получаем статус через valueOf
        jstring statusName = env->NewStringUTF("VIDEO"); // По умолчанию
        if (frame->type == RTSP_STREAM_AUDIO) {
            statusName = env->NewStringUTF("AUDIO");
        } else if (frame->type == RTSP_STREAM_METADATA) {
            statusName = env->NewStringUTF("METADATA");
        }
        jobject streamType = env->CallStaticObjectMethod(
            g_RtspStreamTypeClass, g_RtspStreamTypeValueOf, statusName);
        env->DeleteLocalRef(statusName);

        // Создаем RtspFrame объект
        jobject rtspFrame = env->NewObject(
            g_RtspFrameClass, g_RtspFrameConstructor,
            frameData,
            static_cast<jlong>(frame->timestamp),
            streamType,
            static_cast<jint>(frame->width),
            static_cast<jint>(frame->height)
        );

        // Вызываем callback
        env->CallVoidMethod(data->frameCallback, data->frameCallbackMethod, rtspFrame);

        // Освобождаем локальные ссылки
        env->DeleteLocalRef(frameData);
        env->DeleteLocalRef(streamType);
        env->DeleteLocalRef(rtspFrame);

        // Освобождаем кадр
        rtsp_frame_release(frame);
    } catch (...) {
        LOGE("Exception in frame callback");
        // Освобождаем кадр даже при ошибке
        if (frame) {
            rtsp_frame_release(frame);
        }
    }

    // Отсоединяем поток, если мы его прикрепили
    if (threadAttached) {
        data->jvm->DetachCurrentThread();
    }
}

// C callback для статуса
void statusCallbackWrapper(RTSPStatus status, const char* message, void* userData) {
    if (!userData) return;

    CallbackData* data = static_cast<CallbackData*>(userData);
    JNIEnv* env = nullptr;

    // Получаем JNI environment (может быть вызван из другого потока)
    jint result = data->jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (result == JNI_EDETACHED) {
        // Поток не прикреплен, прикрепляем его
        if (data->jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE("Failed to attach thread to JVM");
            return;
        }
    } else if (result != JNI_OK) {
        LOGE("Failed to get JNI environment");
        return;
    }

    if (!data->statusCallback || !data->statusCallbackMethod) {
        return;
    }

    bool threadAttached = (result == JNI_EDETACHED);

    try {
        // Конвертируем статус в строку
        const char* statusName = "DISCONNECTED";
        switch (status) {
            case RTSP_STATUS_CONNECTING:
                statusName = "CONNECTING";
                break;
            case RTSP_STATUS_CONNECTED:
                statusName = "CONNECTED";
                break;
            case RTSP_STATUS_PLAYING:
                statusName = "PLAYING";
                break;
            case RTSP_STATUS_ERROR:
                statusName = "ERROR";
                break;
            default:
                statusName = "DISCONNECTED";
        }

        jstring statusStr = env->NewStringUTF(statusName);
        jobject rtspStatus = env->CallStaticObjectMethod(
            g_RtspClientStatusClass, g_RtspClientStatusValueOf, statusStr);
        env->DeleteLocalRef(statusStr);

        jstring messageStr = message ? env->NewStringUTF(message) : nullptr;

        // Вызываем callback
        env->CallVoidMethod(data->statusCallback, data->statusCallbackMethod,
                           rtspStatus, messageStr);

        // Освобождаем локальные ссылки
        env->DeleteLocalRef(rtspStatus);
        if (messageStr) {
            env->DeleteLocalRef(messageStr);
        }
    } catch (...) {
        LOGE("Exception in status callback");
    }

    // Отсоединяем поток, если мы его прикрепили
    if (threadAttached) {
        data->jvm->DetachCurrentThread();
    }
}

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    // Инициализируем классы для работы с callbacks
    jclass rtspFrameClass = env->FindClass("com/company/ipcamera/core/network/RtspFrame");
    jclass rtspStatusClass = env->FindClass("com/company/ipcamera/core/network/RtspClientStatus");
    jclass rtspStreamTypeClass = env->FindClass("com/company/ipcamera/core/network/RtspStreamType");

    if (!rtspFrameClass || !rtspStatusClass || !rtspStreamTypeClass) {
        LOGE("Failed to find required classes");
        return JNI_ERR;
    }

    // Сохраняем глобальные ссылки
    g_RtspFrameClass = static_cast<jclass>(env->NewGlobalRef(rtspFrameClass));
    g_RtspClientStatusClass = static_cast<jclass>(env->NewGlobalRef(rtspStatusClass));
    g_RtspStreamTypeClass = static_cast<jclass>(env->NewGlobalRef(rtspStreamTypeClass));

    // Получаем методы
    g_RtspFrameConstructor = env->GetMethodID(g_RtspFrameClass, "<init>",
        "([BJLcom/company/ipcamera/core/network/RtspStreamType;II)V");

    jmethodID valueOfMethod = env->GetStaticMethodID(g_RtspClientStatusClass, "valueOf",
        "(Ljava/lang/String;)Lcom/company/ipcamera/core/network/RtspClientStatus;");
    g_RtspClientStatusValueOf = valueOfMethod;

    jmethodID streamTypeValueOf = env->GetStaticMethodID(g_RtspStreamTypeClass, "valueOf",
        "(Ljava/lang/String;)Lcom/company/ipcamera/core/network/RtspStreamType;");
    g_RtspStreamTypeValueOf = streamTypeValueOf;

    // Освобождаем локальные ссылки
    env->DeleteLocalRef(rtspFrameClass);
    env->DeleteLocalRef(rtspStatusClass);
    env->DeleteLocalRef(rtspStreamTypeClass);

    if (!g_RtspFrameConstructor || !g_RtspClientStatusValueOf || !g_RtspStreamTypeValueOf) {
        LOGE("Failed to get required method IDs");
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return;
    }

    // Освобождаем глобальные ссылки
    if (g_RtspFrameClass) {
        env->DeleteGlobalRef(g_RtspFrameClass);
        g_RtspFrameClass = nullptr;
    }
    if (g_RtspClientStatusClass) {
        env->DeleteGlobalRef(g_RtspClientStatusClass);
        g_RtspClientStatusClass = nullptr;
    }
    if (g_RtspStreamTypeClass) {
        env->DeleteGlobalRef(g_RtspStreamTypeClass);
        g_RtspStreamTypeClass = nullptr;
    }
}

JNIEXPORT jlong JNICALL
Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativeCreate(JNIEnv* env, jobject thiz) {
    RTSPClient* client = rtsp_client_create();
    return reinterpret_cast<jlong>(client);
}

JNIEXPORT void JNICALL
Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativeDestroy(JNIEnv* env, jobject thiz, jlong handle) {
    RTSPClient* client = reinterpret_cast<RTSPClient*>(handle);
    if (client) {
        // Освобождаем callbacks перед уничтожением клиента
        rtsp_client_set_frame_callback(client, RTSP_STREAM_VIDEO, nullptr, nullptr);
        rtsp_client_set_frame_callback(client, RTSP_STREAM_AUDIO, nullptr, nullptr);
        rtsp_client_set_status_callback(client, nullptr, nullptr);

        rtsp_client_destroy(client);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativeConnect(
    JNIEnv* env, jobject thiz, jlong handle, jstring url, jstring username, jstring password, jint timeoutMs) {
    RTSPClient* client = reinterpret_cast<RTSPClient*>(handle);
    if (!client) {
        return JNI_FALSE;
    }

    const char* urlStr = env->GetStringUTFChars(url, nullptr);
    const char* usernameStr = username ? env->GetStringUTFChars(username, nullptr) : nullptr;
    const char* passwordStr = password ? env->GetStringUTFChars(password, nullptr) : nullptr;

    bool result = rtsp_client_connect(client, urlStr, usernameStr, passwordStr, timeoutMs);

    env->ReleaseStringUTFChars(url, urlStr);
    if (usernameStr) {
        env->ReleaseStringUTFChars(username, usernameStr);
    }
    if (passwordStr) {
        env->ReleaseStringUTFChars(password, passwordStr);
    }

    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativeDisconnect(JNIEnv* env, jobject thiz, jlong handle) {
    RTSPClient* client = reinterpret_cast<RTSPClient*>(handle);
    if (client) {
        rtsp_client_disconnect(client);
    }
}

JNIEXPORT jint JNICALL
Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativeGetStatus(JNIEnv* env, jobject thiz, jlong handle) {
    RTSPClient* client = reinterpret_cast<RTSPClient*>(handle);
    if (!client) {
        return 0; // DISCONNECTED
    }
    return static_cast<jint>(rtsp_client_get_status(client));
}

JNIEXPORT jboolean JNICALL
Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativePlay(JNIEnv* env, jobject thiz, jlong handle) {
    RTSPClient* client = reinterpret_cast<RTSPClient*>(handle);
    if (!client) {
        return JNI_FALSE;
    }
    return rtsp_client_play(client) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativeStop(JNIEnv* env, jobject thiz, jlong handle) {
    RTSPClient* client = reinterpret_cast<RTSPClient*>(handle);
    if (!client) {
        return JNI_FALSE;
    }
    return rtsp_client_stop(client) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativePause(JNIEnv* env, jobject thiz, jlong handle) {
    RTSPClient* client = reinterpret_cast<RTSPClient*>(handle);
    if (!client) {
        return JNI_FALSE;
    }
    return rtsp_client_pause(client) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativeGetStreamCount(JNIEnv* env, jobject thiz, jlong handle) {
    RTSPClient* client = reinterpret_cast<RTSPClient*>(handle);
    if (!client) {
        return 0;
    }
    return rtsp_client_get_stream_count(client);
}

JNIEXPORT jint JNICALL
Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativeGetStreamType(JNIEnv* env, jobject thiz, jlong handle, jint streamIndex) {
    RTSPClient* client = reinterpret_cast<RTSPClient*>(handle);
    if (!client) {
        return -1;
    }
    return static_cast<jint>(rtsp_client_get_stream_type(client, streamIndex));
}

JNIEXPORT jboolean JNICALL
Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativeGetStreamInfo(
    JNIEnv* env, jobject thiz, jlong handle, jint streamIndex, jintArray width, jintArray height, jintArray fps, jbyteArray codec) {
    RTSPClient* client = reinterpret_cast<RTSPClient*>(handle);
    if (!client) {
        return JNI_FALSE;
    }

    int w = 0, h = 0, f = 0;
    char codecBuffer[64] = {0};

    bool result = rtsp_client_get_stream_info(client, streamIndex, &w, &h, &f, codecBuffer, sizeof(codecBuffer));

    if (result) {
        jint widthVal = w;
        jint heightVal = h;
        jint fpsVal = f;
        env->SetIntArrayRegion(width, 0, 1, &widthVal);
        env->SetIntArrayRegion(height, 0, 1, &heightVal);
        env->SetIntArrayRegion(fps, 0, 1, &fpsVal);

        jsize codecLen = strlen(codecBuffer);
        if (codecLen > 0) {
            env->SetByteArrayRegion(codec, 0, codecLen, reinterpret_cast<const jbyte*>(codecBuffer));
        }
    }

    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativeSetFrameCallback(
    JNIEnv* env, jobject thiz, jlong handle, jint streamType, jobject callback) {
    RTSPClient* client = reinterpret_cast<RTSPClient*>(handle);
    if (!client || !callback) {
        return;
    }

    // Сохраняем callback как глобальную ссылку
    jobject globalCallback = env->NewGlobalRef(callback);

    // Получаем метод callback'а (Consumer<RtspFrame>)
    jclass callbackClass = env->GetObjectClass(globalCallback);
    jmethodID callbackMethod = env->GetMethodID(callbackClass, "accept",
        "(Lcom/company/ipcamera/core/network/RtspFrame;)V");

    if (!callbackMethod) {
        // Пробуем альтернативный метод (для функциональных интерфейсов)
        callbackMethod = env->GetMethodID(callbackClass, "invoke",
            "(Lcom/company/ipcamera/core/network/RtspFrame;)V");
    }

    if (!callbackMethod) {
        LOGE("Failed to find callback method (accept or invoke)");
        env->DeleteGlobalRef(globalCallback);
        return;
    }

    // Создаем структуру данных для callback'а
    JavaVM* jvm;
    env->GetJavaVM(&jvm);

    CallbackData* data = new CallbackData();
    data->jvm = jvm;
    data->frameCallback = globalCallback;
    data->frameCallbackMethod = callbackMethod;

    // Устанавливаем callback в нативную библиотеку
    rtsp_client_set_frame_callback(
        client,
        static_cast<RTSPStreamType>(streamType),
        frameCallbackWrapper,
        data
    );
}

JNIEXPORT void JNICALL
Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativeSetStatusCallback(
    JNIEnv* env, jobject thiz, jlong handle, jobject callback) {
    RTSPClient* client = reinterpret_cast<RTSPClient*>(handle);
    if (!client || !callback) {
        return;
    }

    // Сохраняем callback как глобальную ссылку
    jobject globalCallback = env->NewGlobalRef(callback);

    // Получаем метод callback'а (BiConsumer<RtspClientStatus, String>)
    jclass callbackClass = env->GetObjectClass(globalCallback);
    jmethodID callbackMethod = env->GetMethodID(callbackClass, "accept",
        "(Lcom/company/ipcamera/core/network/RtspClientStatus;Ljava/lang/String;)V");

    if (!callbackMethod) {
        // Пробуем альтернативный метод (для функциональных интерфейсов)
        callbackMethod = env->GetMethodID(callbackClass, "invoke",
            "(Lcom/company/ipcamera/core/network/RtspClientStatus;Ljava/lang/String;)V");
    }

    if (!callbackMethod) {
        LOGE("Failed to find status callback method (accept or invoke)");
        env->DeleteGlobalRef(globalCallback);
        return;
    }

    // Создаем структуру данных для callback'а
    JavaVM* jvm;
    env->GetJavaVM(&jvm);

    CallbackData* data = new CallbackData();
    data->jvm = jvm;
    data->statusCallback = globalCallback;
    data->statusCallbackMethod = callbackMethod;

    // Устанавливаем callback в нативную библиотеку
    rtsp_client_set_status_callback(
        client,
        statusCallbackWrapper,
        data
    );
}

JNIEXPORT void JNICALL
Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativeSetReconnectParams(
    JNIEnv* env, jobject thiz, jlong handle, jboolean enabled, jint maxRetries,
    jint initialDelayMs, jint maxDelayMs, jfloat backoffMultiplier) {
    RTSPClient* client = reinterpret_cast<RTSPClient*>(handle);
    if (!client) {
        return;
    }

    RTSPReconnectParams params;
    params.enabled = enabled == JNI_TRUE;
    params.maxRetries = maxRetries;
    params.initialDelayMs = initialDelayMs;
    params.maxDelayMs = maxDelayMs;
    params.backoffMultiplier = backoffMultiplier;

    rtsp_client_set_reconnect_params(client, &params);
}

} // extern "C"



