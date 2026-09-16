package com.company.ipcamera.core.network.rtsp;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;

/**
 * Simple test to verify native library can be loaded manually.
 */
public class NativeLibraryLoadTest {

    @Test
    public void testLoadNativeLibraryManually() {
        // Find project root
        String userDir = System.getProperty("user.dir");
        File dllFile = new File(userDir, "native/video-processing/lib/windows/x64/video_processing.dll");
        
        System.out.println("User dir: " + userDir);
        System.out.println("DLL path: " + dllFile.getAbsolutePath());
        System.out.println("DLL exists: " + dllFile.exists());
        
        assertTrue("DLL should exist at: " + dllFile.getAbsolutePath(), dllFile.exists());
        
        try {
            System.load(dllFile.getAbsolutePath());
            System.out.println("SUCCESS: Native library loaded!");
            assertTrue("Library should load successfully", true);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("FAILED: " + e.getMessage());
            e.printStackTrace();
            fail("Failed to load native library: " + e.getMessage());
        }
    }
}
