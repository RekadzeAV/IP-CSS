package com.company.ipcamera.core.network.rtsp

/**
 * RTSP URL Parser
 * Parses RTSP URLs from different camera manufacturers
 */
object RtspUrlParser {

    data class ParsedRtspUrl(
        val host: String,
        val port: Int,
        val path: String,
        val username: String? = null,
        val password: String? = null,
        val scheme: String = "rtsp"
    )

    /**
     * Parse RTSP URL into components
     * Supports formats:
     * - rtsp://user:pass@host:port/path
     * - rtsp://host:port/path
     * - rtsp://host/path (default port 554)
     */
    fun parse(url: String): ParsedRtspUrl? {
        try {
            if (!url.startsWith("rtsp://")) {
                return null
            }

            val withoutScheme = url.substring("rtsp://".length)

            // Extract credentials if present
            var hostPortPath = withoutScheme
            var username: String? = null
            var password: String? = null

            if ("@" in withoutScheme) {
                val parts = withoutScheme.split("@")
                val credentials = parts[0]
                hostPortPath = parts[1]

                if (":" in credentials) {
                    val credParts = credentials.split(":")
                    username = credParts[0]
                    password = credParts[1]
                }
            }

            // Extract host and port
            var host = hostPortPath
            var port = 554 // Default RTSP port

            if (":" in hostPortPath) {
                val colonIndex = hostPortPath.indexOf(":")
                host = hostPortPath.substring(0, colonIndex)
                val portStr = hostPortPath.substring(colonIndex + 1)
                port = portStr.takeWhile { it.isDigit() }.toIntOrNull() ?: 554
            }

            // Extract path
            val path = if ("/" in hostPortPath) {
                hostPortPath.substring(hostPortPath.indexOf("/"))
            } else {
                ""
            }

            return ParsedRtspUrl(
                host = host,
                port = port,
                path = path,
                username = username,
                password = password
            )
        } catch (e: Exception) {
            return null
        }
    }
}
