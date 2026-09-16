package com.company.ipcamera.server.service

/**
 * Путь к бинарнику FFmpeg: `FFMPEG_PATH` или `ffmpeg` в PATH.
 */
fun ffmpegExecutable(): String =
    System.getenv("FFMPEG_PATH")?.trim()?.takeIf { it.isNotEmpty() } ?: "ffmpeg"

/**
 * Путь к ffprobe: `FFPROBE_PATH` или `ffprobe` в PATH.
 */
fun ffprobeExecutable(): String =
    System.getenv("FFPROBE_PATH")?.trim()?.takeIf { it.isNotEmpty() } ?: "ffprobe"

/**
 * Аргументы FFmpeg для входа: `-rtsp_transport tcp`, затем `-i URL`.
 *
 * Примечание: опции `-rw_timeout` и `-stimeout` удалены, так как не поддерживаются
 * в FFmpeg 8.x (Alpine). При необходимости таймауты можно задать через `-timeout`
 * непосредственно в вызывающем коде или через переменные окружения обёртки.
 */
fun rtspFfmpegInputArgs(rtspUrl: String): List<String> {
    if (!rtspUrl.startsWith("rtsp://", ignoreCase = true)) {
        return listOf("-i", rtspUrl)
    }
    val out = mutableListOf<String>()
    out.addAll(listOf("-rtsp_transport", "tcp"))
    out.addAll(listOf("-fflags", "+nobuffer"))
    out.addAll(listOf("-flags2", "+fastseek"))
    out.addAll(listOf("-probesize", "32"))
    out.addAll(listOf("-analyzeduration", "500000"))
    out.add("-i")
    out.add(rtspUrl)
    return out
}

/**
 * Флаги перед `-i` для HLS из файла записи (стабилизация PTS).
 *
 * `FFMPEG_RECORDING_HLS_GENPTS`: не задано / `1` / `true` — добавить `-fflags +genpts`; `0` / `false` — не добавлять.
 */
fun ffmpegRecordingToHlsInputFlags(): List<String> {
    val raw = System.getenv("FFMPEG_RECORDING_HLS_GENPTS")?.trim()?.lowercase()
    val enabled = raw.isNullOrEmpty() || raw == "1" || raw == "true"
    if (!enabled) return emptyList()
    return listOf("-fflags", "+genpts")
}
