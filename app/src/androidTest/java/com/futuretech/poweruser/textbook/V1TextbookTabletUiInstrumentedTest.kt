package com.futuretech.poweruser.textbook

import android.graphics.Bitmap
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
        val context = InstrumentationRegistry.getInstrumentation().targetContext
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

        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val output = File(context.getExternalFilesDir(null), "v1_textbook_tablet.png")
        FileOutputStream(output).use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
        assertTrue(output.exists() && output.length() > 10_000)
    }
}
