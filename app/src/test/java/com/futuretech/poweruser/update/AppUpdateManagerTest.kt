package com.futuretech.poweruser.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {
    @Test
    fun newerPatchVersionIsDetected() {
        assertTrue(isNewerVersion("1.2.2", "1.2.1"))
    }

    @Test
    fun newerMinorVersionIsDetected() {
        assertTrue(isNewerVersion("1.3.0", "1.2.9"))
    }

    @Test
    fun newerMajorVersionIsDetected() {
        assertTrue(isNewerVersion("2.0.0", "1.99.99"))
    }

    @Test
    fun sameVersionIsNotAnUpdate() {
        assertFalse(isNewerVersion("1.2.1", "1.2.1"))
    }

    @Test
    fun olderVersionIsNotAnUpdate() {
        assertFalse(isNewerVersion("1.2.0", "1.2.1"))
    }

    @Test
    fun vPrefixAndPreReleaseSuffixDoNotBreakComparison() {
        assertTrue(isNewerVersion("v1.2.2-beta1", "1.2.1"))
        assertFalse(isNewerVersion("v1.2.1", "1.2.1-beta1"))
    }
}
