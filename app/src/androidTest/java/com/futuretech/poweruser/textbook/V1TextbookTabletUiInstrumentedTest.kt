package com.futuretech.poweruser.textbook

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.futuretech.poweruser.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class V1TextbookTabletUiInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun galaxyTabSizedWindowUsesThreeColumnUniversityTextbookLayout() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val widthDp = context.resources.configuration.screenWidthDp
        if (widthDp < 1080) {
            assertTrue("Default phone regression run is expected to be narrower than expanded threshold", widthDp < 1080)
            return
        }

        compose.onNodeWithTag("textbook_v1_entry").assertExists().performClick()
        compose.onNodeWithTag("v1_textbook_root").assertExists()
        compose.onNodeWithTag("textbook_layout_expanded").assertExists()
        compose.onNodeWithTag("textbook_toc").assertExists()
        compose.onNodeWithTag("textbook_reader").assertExists()
        compose.onNodeWithTag("textbook_insight_rail").assertExists()

        // Capture while the verified three-column screen is still on screen. The shell-owned
        // /data/local/tmp path survives test/app cleanup and avoids scoped-storage behavior.
        val screenshotPath = "/data/local/tmp/v1_textbook_tablet.png"
        shell(instrumentation, "rm -f $screenshotPath")
        shell(instrumentation, "screencap -p $screenshotPath")
        val listing = shell(instrumentation, "ls -l $screenshotPath")
        assertTrue("Galaxy Tab screenshot must exist in shell temp storage", listing.contains("v1_textbook_tablet.png"))
    }

    private fun shell(instrumentation: android.app.Instrumentation, command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }
}
