package com.company.ipcamera.core.network.rtsp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Simple Java test to verify native library loading and basic FFI functionality.
 * This avoids Kotlin compilation issues in other test files.
 */
public class NativeRtspClientSmokeTest {

    @Test
    public void testNativeLibraryLoads() {
        // Test that we can create an instance
        NativeRtspClient client = new NativeRtspClient();
        
        // Test create() returns non-zero handle
        long handle = client.create();
        
        assertNotNull("Client should be created", client);
        assertNotEquals("Handle should be non-zero", 0L, handle);
        
        // Clean up
        client.destroy(handle);
    }

    @Test
    public void testGetStatusReturnsValue() {
        NativeRtspClient client = new NativeRtspClient();
        long handle = client.create();
        
        try {
            RtspClientStatus status = client.getStatus(handle);
            assertNotNull("Status should not be null", status);
        } finally {
            client.destroy(handle);
        }
    }

    @Test
    public void testSetReconnectParams() {
        NativeRtspClient client = new NativeRtspClient();
        long handle = client.create();
        
        try {
            // Should not throw exception
            client.setReconnectParams(
                handle,
                true,       // enabled
                5,          // maxRetries
                1000,       // initialDelayMs
                30000,      // maxDelayMs
                2.0f        // backoffMultiplier
            );
            
            assertTrue("Should set reconnect params without error", true);
        } finally {
            client.destroy(handle);
        }
    }

    @Test
    public void testInvalidHandleOperations() {
        NativeRtspClient client = new NativeRtspClient();
        
        // Operations on invalid handle (0) should not crash
        RtspClientStatus status = client.getStatus(0L);
        assertEquals("Status for invalid handle should be DISCONNECTED", 
                     RtspClientStatus.DISCONNECTED, status);
        
        int streamCount = client.getStreamCount(0L);
        assertEquals("Stream count for invalid handle should be 0", 0, streamCount);
        
        // destroy(0) should not crash
        client.destroy(0L);
    }

    @Test
    public void testMultipleCreateDestroyCycles() {
        NativeRtspClient client = new NativeRtspClient();
        
        for (int i = 0; i < 3; i++) {
            long handle = client.create();
            assertNotEquals("Handle " + i + " should be non-zero", 0L, handle);
            client.destroy(handle);
        }
        
        assertTrue("Multiple cycles completed successfully", true);
    }
}
