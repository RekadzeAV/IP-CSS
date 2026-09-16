package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.EventSeverity

class EventGenerator {
    suspend fun generateEvent(cameraId: String, type: EventType, description: String? = null, metadata: Map<String, String> = emptyMap()): Event {
        return Event(
            id = System.currentTimeMillis().toString(),
            cameraId = cameraId,
            type = type,
            severity = EventSeverity.INFO,
            timestamp = System.currentTimeMillis(),
            description = description,
            metadata = metadata
        )
    }
}