package com.company.ipcamera.server.service.analytics

import com.company.ipcamera.core.network.RtspFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Происхождение кадров для серверного пайплайна [com.company.ipcamera.server.service.VideoAnalyticsService].
 * Расширяется без смены сигнатур: новые значения enum — additive.
 */
enum class AnalyticsFrameSourceKind {
    /** Декодированные кадры из [com.company.ipcamera.core.network.RtspClient]. */
    RTSP_DECODED,

    /**
     * Зарезервировано: сырые/декодированные кадры из HLS/FFmpeg-конвейера (когда появится вывод в Kotlin).
     * Сейчас в [com.company.ipcamera.server.service.VideoStreamService] аналитика не стартует в HLS-only режиме.
     */
    HLS_DERIVED,

    /** Источник отсутствует; [frames] пустой (явная фиксация в метриках / логах). */
    NONE
}

/**
 * Единый вход: тип источника + поток кадров в формате [RtspFrame] (совместимость с текущим пайплайном).
 */
data class AnalyticsFrameInput(
    val kind: AnalyticsFrameSourceKind,
    val frames: Flow<RtspFrame>
) {
    companion object {
        fun rtspDecodable(frames: Flow<RtspFrame>): AnalyticsFrameInput =
            AnalyticsFrameInput(AnalyticsFrameSourceKind.RTSP_DECODED, frames)

        fun none(): AnalyticsFrameInput =
            AnalyticsFrameInput(AnalyticsFrameSourceKind.NONE, emptyFlow())
    }
}
