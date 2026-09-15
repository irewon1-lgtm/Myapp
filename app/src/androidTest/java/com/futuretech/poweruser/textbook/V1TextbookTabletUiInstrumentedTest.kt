package com.futuretech.poweruser.textbook

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.futuretech.poweruser.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

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

        // Capture the exact verified three-column frame while the Activity is still alive.
        val appScreenshot = File(context.filesDir, "v1_textbook_tablet.png")
        if (appScreenshot.exists()) appScreenshot.delete()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val compressed = FileOutputStream(appScreenshot).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        assertTrue("Galaxy Tab screenshot must encode as PNG", compressed)
        assertTrue(
            "Galaxy Tab screenshot must be a non-trivial image",
            appScreenshot.exists() && appScreenshot.length() > 10_000
        )

        // Copy before test teardown. The outer shell opens /data/local/tmp while run-as reads
        // the target app's private file, so the evidence survives app/test cleanup.
        val shellPath = "/data/local/tmp/v1_textbook_tablet.png"
        shell(instrumentation, "rm -f $shellPath")
        shell(
            instrumentation,
            "/system/bin/sh -c 'run-as com.futuretech.poweruser cat files/v1_textbook_tablet.png > $shellPath'"
        )
        val byteCount = shell(instrumentation, "stat -c %s $shellPath").trim().toLongOrNull() ?: 0L
        assertTrue("Shell-owned Galaxy Tab screenshot must exceed 10KB", byteCount > 10_000L)
    }

    private fun shell(instrumentation: android.app.Instrumentation, command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }
}
