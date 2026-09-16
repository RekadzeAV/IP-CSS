package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamInfo
import com.company.ipcamera.core.network.RtspStreamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * JVM реализация RTSP клиента через JNI
 * Использует нативную библиотеку video_processing с Desktop JNI оберткой
 *
 * Библиотека загружается в следующем порядке:
 * 1. Пытается загрузить из системного пути (System.loadLibrary)
 * 2. Если не найдена, загружает из локальной директории проекта
 * 3. Определяет путь в зависимости от ОС и архитектуры
 */
actual class NativeRtspClient {
    private val nativeAvailable: Boolean

    companion object {
        private var libraryLoaded = false
        private val lock = Any()

        /**
         * Загружает нативную библиотеку video_processing
         * Поддерживает Windows, Linux и macOS
         */
        private fun loadNativeLibrary(): Boolean {
            synchronized(lock) {
                if (libraryLoaded) return true

                try {
                    // Пытаемся загрузить библиотеку из системного пути
                    System.loadLibrary("video_processing")
                    libraryLoaded = true
                    System.out.println("Native library 'video_processing' loaded from system path")
                    return true
                } catch (e: UnsatisfiedLinkError) {
                    // Если не найдена в системном пути, пытаемся загрузить из локальной директории
                    try {
                        val os = System.getProperty("os.name").lowercase()
                        val arch = System.getProperty("os.arch").lowercase()

                        val (libName, libPath) = when {
                            os.contains("win") -> {
                                val name = "video_processing.dll"
                                val path = "native/video-processing/lib/windows/x64/$name"
                                name to path
                            }
                            os.contains("mac") -> {
                                val macArch = when {
                                    arch.contains("aarch64") || arch.contains("arm") -> "arm64"
                                    else -> "x64"
                                }
                                val name = "libvideo_processing.dylib"
                                val path = "native/video-processing/lib/macos/$macArch/$name"
                                name to path
                            }
                            else -> {
                                // Linux
                                val linuxArch = when {
                                    arch.contains("aarch64") || arch.contains("arm") -> "arm64"
                                    else -> "x64"
                                }
                                val name = "libvideo_processing.so"
                                val path = "native/video-processing/lib/linux/$linuxArch/$name"
                                name to path
                            }
                        }

                        // Пытаемся найти библиотеку относительно корня проекта
                        val projectRoot = findProjectRoot()
                        val libFile = when {
                            projectRoot != null -> File(projectRoot, libPath)
                            else -> File(libPath)
                        }

                        if (libFile.exists()) {
                            System.load(libFile.absolutePath)
                            libraryLoaded = true
                            System.out.println("Native library 'video_processing' loaded from: ${libFile.absolutePath}")
                        } else {
                            // Пытаемся найти в других возможных местах
                            val alternativePaths = listOf(
                                File(System.getProperty("user.dir"), libPath),
                                File(".").absoluteFile.resolve(libPath),
                                File("../..").absoluteFile.resolve(libPath)
                            )

                            var loaded = false
                            for (altPath in alternativePaths) {
                                if (altPath.exists()) {
                                    System.load(altPath.absolutePath)
                                    libraryLoaded = true
                                    loaded = true
                                    System.out.println(
                                        "Native library 'video_processing' loaded from: ${altPath.absolutePath}"
                                    )
                                    break
                                }
                            }

                            if (!loaded) {
                                System.err.println("Failed to load native library 'video_processing'")
                                System.err.println("   Tried paths:")
                                System.err.println("   - System library path")
                                System.err.println("   - ${libFile.absolutePath}")
                                alternativePaths.forEach { path ->
                                    System.err.println("   - ${path.absolutePath}")
                                }
                                System.err.println("   OS: $os, Arch: $arch")
                                System.err.println("   Library name: $libName")
                                return false
                            }
                        }
                    } catch (e2: Exception) {
                        System.err.println("Failed to load native library 'video_processing': ${e2.message}")
                        e2.printStackTrace()
                        return false
                    }
                }
            }
            return libraryLoaded
        }

        /**
         * Находит корень проекта, ища характерные файлы/директории
         */
        private fun findProjectRoot(): File? {
            var current = File(System.getProperty("user.dir")).absoluteFile

            // Ищем корень проекта по характерным файлам/директориям
            val markers = listOf(
                "settings.gradle.kts",
                "settings.gradle",
                "build.gradle.kts",
                "build.gradle",
                ".git",
                "native/video-processing"
            )

            var depth = 0
            while (depth < 10 && current != null) {
                val hasMarker = markers.any { marker ->
                    val markerFile = File(current, marker)
                    markerFile.exists()
                }

                if (hasMarker) {
                    return current
                }

                current = current.parentFile
                depth++
            }

            return null
        }
    }

    init {
        // Загрузка нативной библиотеки при первом использовании
        nativeAvailable = loadNativeLibrary()
    }

    actual fun create(): NativeRtspClientHandle {
        if (!nativeAvailable) return 0L
        return nativeCreate()
    }

    actual suspend fun connect(
        handle: NativeRtspClientHandle,
        url: String,
        username: String?,
        password: String?,
        timeoutMs: Int
    ): Boolean = withContext(Dispatchers.IO) {
        if (!nativeAvailable || handle == 0L) return@withContext false
        runCatching { nativeConnect(handle, url, username, password, timeoutMs) }.getOrDefault(false)
    }

    actual suspend fun disconnect(handle: NativeRtspClientHandle) {
        withContext(Dispatchers.IO) {
            if (!nativeAvailable || handle == 0L) return@withContext
            runCatching { nativeDisconnect(handle) }
        }
    }

    actual fun getStatus(handle: NativeRtspClientHandle): RtspClientStatus {
        if (!nativeAvailable || handle == 0L) return RtspClientStatus.DISCONNECTED
        return runCatching { convertNativeStatus(nativeGetStatus(handle)) }.getOrDefault(RtspClientStatus.DISCONNECTED)
    }

    actual suspend fun play(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.IO) {
        if (!nativeAvailable || handle == 0L) return@withContext false
        runCatching { nativePlay(handle) }.getOrDefault(false)
    }

    actual suspend fun stop(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.IO) {
        if (!nativeAvailable || handle == 0L) return@withContext false
        runCatching { nativeStop(handle) }.getOrDefault(false)
    }

    actual suspend fun pause(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.IO) {
        if (!nativeAvailable || handle == 0L) return@withContext false
        runCatching { nativePause(handle) }.getOrDefault(false)
    }

    actual fun getStreamCount(handle: NativeRtspClientHandle): Int {
        if (!nativeAvailable || handle == 0L) return 0
        return runCatching { nativeGetStreamCount(handle) }.getOrDefault(0)
    }

    actual fun getStreamType(handle: NativeRtspClientHandle, streamIndex: Int): RtspStreamType? {
        if (!nativeAvailable || handle == 0L) return null
        return runCatching { convertNativeStreamType(nativeGetStreamType(handle, streamIndex)) }.getOrNull()
    }

    actual fun getStreamInfo(handle: NativeRtspClientHandle, streamIndex: Int): RtspStreamInfo? {
        val widthArray = IntArray(1)
        val heightArray = IntArray(1)
        val fpsArray = IntArray(1)
        val codecArray = ByteArray(64)

        if (!nativeAvailable || handle == 0L) return null
        val success = runCatching {
            nativeGetStreamInfo(handle, streamIndex, widthArray, heightArray, fpsArray, codecArray)
        }.getOrDefault(false)

        if (!success) {
            return null
        }

        val streamType = getStreamType(handle, streamIndex) ?: return null
        val codec = codecArray.decodeToString().trim('\u0000')
        val resolution = if (widthArray[0] > 0 && heightArray[0] > 0) {
            com.company.ipcamera.core.common.model.Resolution(widthArray[0], heightArray[0])
        } else {
            null
        }

        return RtspStreamInfo(streamIndex, streamType, resolution, fpsArray[0], codec)
    }

    actual fun setFrameCallback(
        handle: NativeRtspClientHandle,
        streamType: RtspStreamType,
        callback: (RtspFrame) -> Unit
    ) {
        val callbackInterface = object : java.util.function.Consumer<RtspFrame> {
            override fun accept(frame: RtspFrame) {
                callback(frame)
            }
        }
        if (!nativeAvailable || handle == 0L) return
        runCatching { nativeSetFrameCallback(handle, convertStreamTypeToInt(streamType), callbackInterface) }
    }

    actual fun setStatusCallback(
        handle: NativeRtspClientHandle,
        callback: (RtspClientStatus, String?) -> Unit
    ) {
        val callbackInterface = object : java.util.function.BiConsumer<RtspClientStatus, String?> {
            override fun accept(status: RtspClientStatus, message: String?) {
                callback(status, message)
            }
        }
        if (!nativeAvailable || handle == 0L) return
        runCatching { nativeSetStatusCallback(handle, callbackInterface) }
    }

    actual fun setReconnectParams(
        handle: NativeRtspClientHandle,
        enabled: Boolean,
        maxRetries: Int,
        initialDelayMs: Int,
        maxDelayMs: Int,
        backoffMultiplier: Float
    ) {
        if (!nativeAvailable || handle == 0L) return
        runCatching {
            nativeSetReconnectParams(
                handle,
                enabled,
                maxRetries,
                initialDelayMs,
                maxDelayMs,
                backoffMultiplier
            )
        }
    }

    actual fun destroy(handle: NativeRtspClientHandle) {
        if (!nativeAvailable || handle == 0L) return
        runCatching { nativeDestroy(handle) }
    }

    private fun convertStreamTypeToInt(streamType: RtspStreamType): Int {
        return when (streamType) {
            RtspStreamType.VIDEO -> 0
            RtspStreamType.AUDIO -> 1
            RtspStreamType.METADATA -> 2
        }
    }

    // Вспомогательные функции для конвертации (будут использоваться после реализации JNI)
    private fun convertNativeStatus(status: Int): RtspClientStatus {
        return when (status) {
            0 -> RtspClientStatus.DISCONNECTED
            1 -> RtspClientStatus.CONNECTING
            2 -> RtspClientStatus.CONNECTED
            3 -> RtspClientStatus.PLAYING
            4 -> RtspClientStatus.ERROR
            else -> RtspClientStatus.DISCONNECTED
        }
    }

    private fun convertNativeStreamType(type: Int): RtspStreamType? {
        return when (type) {
            0 -> RtspStreamType.VIDEO
            1 -> RtspStreamType.AUDIO
            2 -> RtspStreamType.METADATA
            else -> null
        }
    }

    // JNI функции
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeConnect(handle: Long, url: String, username: String?, password: String?, timeoutMs: Int): Boolean
    private external fun nativeDisconnect(handle: Long)
    private external fun nativeGetStatus(handle: Long): Int
    private external fun nativePlay(handle: Long): Boolean
    private external fun nativeStop(handle: Long): Boolean
    private external fun nativePause(handle: Long): Boolean
    private external fun nativeGetStreamCount(handle: Long): Int
    private external fun nativeGetStreamType(handle: Long, streamIndex: Int): Int
    private external fun nativeGetStreamInfo(
        handle: Long,
        streamIndex: Int,
        width: IntArray,
        height: IntArray,
        fps: IntArray,
        codec: ByteArray
    ): Boolean
    private external fun nativeSetFrameCallback(
        handle: Long,
        streamType: Int,
        callback: java.util.function.Consumer<RtspFrame>
    )
    private external fun nativeSetStatusCallback(
        handle: Long,
        callback: java.util.function.BiConsumer<RtspClientStatus, String?>
    )
    private external fun nativeSetReconnectParams(
        handle: Long,
        enabled: Boolean,
        maxRetries: Int,
        initialDelayMs: Int,
        maxDelayMs: Int,
        backoffMultiplier: Float
    )
}
