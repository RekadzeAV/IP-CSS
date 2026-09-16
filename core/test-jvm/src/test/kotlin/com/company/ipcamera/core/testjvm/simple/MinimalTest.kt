package com.company.ipcamera.core.testjvm.simple

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Minimal test to check if Gradle tests work at all
 */
class MinimalTest {

    @Test
    fun `test basic assertion`() {
        assertTrue(true)
    }

    @Test
    fun `test addition`() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `test string manipulation`() {
        val str = "Hello"
        assertEquals(5, str.length)
    }
}
