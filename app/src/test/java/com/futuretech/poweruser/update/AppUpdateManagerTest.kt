package com.futuretech.poweruser.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
    @Test
    fun malformedExternalReleaseJsonIsRejectedWithoutCrash() {
        assertNull(parseReleaseJson("{not-json"))
        assertNull(parseReleaseJson("[]"))
        assertNull(parseReleaseJson("{\"tag_name\":\"v1.2.7\"}"))
    }

    @Test
    fun malformedAssetsAreIgnoredButValidHttpsAssetSurvives() {
        val parsed = parseReleaseJson(
            """{
                "tag_name":"v1.2.7",
                "body":"ok",
                "assets":[
                    null,
                    {"name":"bad.apk","browser_download_url":""},
                    {"name":"good.apk","browser_download_url":"https://example.test/good.apk"}
                ]
            }""".trimIndent()
        )
        assertEquals("v1.2.7", parsed?.tagName)
        assertEquals(1, parsed?.assets?.size)
        assertEquals("good.apk", parsed?.assets?.single()?.name)
    }
}
