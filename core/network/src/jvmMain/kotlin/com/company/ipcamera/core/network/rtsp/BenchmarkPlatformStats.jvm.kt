package com.company.ipcamera.core.network.rtsp

import java.lang.management.ManagementFactory

/**
 * JVM implementation of platform CPU usage collection.
 */
actual fun collectPlatformCpuUsage(): Double {
    return try {
        val osMXBean = ManagementFactory.getOperatingSystemMXBean()
        if (osMXBean is com.sun.management.OperatingSystemMXBean) {
            osMXBean.systemLoadAverage?.takeIf { it >= 0 } ?: 0.0
        } else {
            0.0
        }
    } catch (e: Exception) {
        0.0
    }
}

/**
 * JVM implementation of platform memory usage collection.
 */
actual fun collectPlatformMemoryUsage(): Long {
    return try {
        val memoryMXBean = ManagementFactory.getMemoryMXBean()
        memoryMXBean.heapMemoryUsage.used
    } catch (e: Exception) {
        0L
    }
}
