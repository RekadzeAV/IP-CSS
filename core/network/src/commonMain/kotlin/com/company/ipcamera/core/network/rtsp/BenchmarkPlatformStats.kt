package com.company.ipcamera.core.network.rtsp

/**
 * Collect CPU usage from the platform.
 * Implemented in platform-specific files to avoid JVM-only APIs in commonMain.
 */
expect fun collectPlatformCpuUsage(): Double

/**
 * Collect memory usage from the platform.
 * Implemented in platform-specific files to avoid JVM-only APIs in commonMain.
 */
expect fun collectPlatformMemoryUsage(): Long
