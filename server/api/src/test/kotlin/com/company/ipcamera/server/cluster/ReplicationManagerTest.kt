package com.company.ipcamera.server.cluster

import com.company.ipcamera.server.config.ClusterConfig
import com.company.ipcamera.server.config.RedisClientWrapper
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.*
import io.mockk.coEvery
import io.mockk.mockk

class ReplicationManagerTest {

    private val redis: RedisClientWrapper = mockk()
    private val clusterService: ClusterService = mockk()
    
    @Test
    fun `getShardOwner returns null when cluster disabled`() = runTest {
        val configMock = mockk<ClusterConfig>()
        coEvery { configMock.enabled } returns false
        val mgr = ReplicationManager(redis, clusterService, configMock)
        
        val owner = mgr.getShardOwner("test-recording-123")
        assertNull(owner)
    }

    @Test
    fun `replicateRecording returns success when cluster disabled`() = runTest {
        val configMock = mockk<ClusterConfig>()
        coEvery { configMock.enabled } returns false
        val mgr = ReplicationManager(redis, clusterService, configMock)
        
        val result = mgr.replicateRecording("rec-1", "{\"test\": true}")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `resolveConflict picks higher timestamp`() = runTest {
        val mgr = ReplicationManager(redis, clusterService, ClusterConfig)
        
        val localData = """{"timestamp": 1000, "data": "local"}"""
        val remoteData = """{"timestamp": 2000, "data": "remote"}"""
        
        val resolved = mgr.resolveConflict("rec-1", localData, remoteData)
        assertTrue(resolved.contains("remote"), "Remote has higher timestamp, should win")
    }

    @Test
    fun `resolveConflict picks local when local timestamp higher`() = runTest {
        val mgr = ReplicationManager(redis, clusterService, ClusterConfig)
        
        val localData = """{"timestamp": 3000, "data": "local"}"""
        val remoteData = """{"timestamp": 2000, "data": "remote"}"""
        
        val resolved = mgr.resolveConflict("rec-1", localData, remoteData)
        assertTrue(resolved.contains("local"), "Local has higher timestamp, should win")
    }

    @Test
    fun `resolveConflict handles equal timestamps`() = runTest {
        val mgr = ReplicationManager(redis, clusterService, ClusterConfig)
        
        val localData = """{"timestamp": 1000, "data": "local"}"""
        val remoteData = """{"timestamp": 1000, "data": "remote"}"""
        
        val resolved = mgr.resolveConflict("rec-1", localData, remoteData)
        assertTrue(resolved.contains("local"), "Equal timestamp: local wins")
    }

    @Test
    fun `resolveConflict handles malformed data`() = runTest {
        val mgr = ReplicationManager(redis, clusterService, ClusterConfig)
        
        val localData = "invalid json"
        val remoteData = """{"timestamp": 1000, "data": "remote"}"""
        
        val resolved = mgr.resolveConflict("rec-1", localData, remoteData)
        assertTrue(resolved.contains("remote"), "Malformed local: remote wins")
    }
}
