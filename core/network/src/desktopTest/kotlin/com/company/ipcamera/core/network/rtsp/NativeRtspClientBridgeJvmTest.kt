package com.company.ipcamera.core.network.rtsp

import kotlin.test.Test
import kotlin.test.assertNotEquals

/**
 * JVM JNI bridge smoke: [NativeRtspClient.create] must return a non-zero handle when
 * `video_processing` is on the library path (local dev or CI that builds the native lib).
 *
 * In CI jobs that ship the library, set environment variable **`REQUIRE_NATIVE_RTSP_BRIDGE=true`**
 * so a zero handle fails the build (1.8.4 / block F). Without it, the test is a no-op when
 * the library is absent (expected on many developer machines).
 */
class NativeRtspClientBridgeJvmTest {

    @Test
    fun nativeCreate_returnsNonZero_whenRequireNativeRtspBridgeEnabled() {
        val require = System.getenv("REQUIRE_NATIVE_RTSP_BRIDGE")?.equals("true", ignoreCase = true) == true
        val client = NativeRtspClient()
        val handle = client.create()
        try {
            if (require) {
                assertNotEquals(
                    0L,
                    handle,
                    "REQUIRE_NATIVE_RTSP_BRIDGE=true but JNI create() returned 0 (library missing or load failed)"
                )
            }
        } finally {
            if (handle != 0L) {
                client.destroy(handle)
            }
        }
    }
}
