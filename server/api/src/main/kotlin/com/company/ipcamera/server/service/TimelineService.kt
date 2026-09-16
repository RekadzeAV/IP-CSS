package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.Event

class TimelineService {
    suspend fun getTimeline(cameraId: String, startTime: Long, endTime: Long, limit: Int): List<Event> = emptyList()
    suspend fun getEventsByType(cameraId: String, eventType: String, limit: Int): List<Event> = emptyList()
    suspend fun getEventById(id: String): Event? = null
}