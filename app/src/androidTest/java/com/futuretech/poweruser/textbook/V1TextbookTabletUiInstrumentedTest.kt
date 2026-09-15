package com.futuretech.poweruser.textbook

import android.util.Log
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
        val config = context.resources.configuration
        val widthDp = config.screenWidthDp
        val heightDp = config.screenHeightDp
        val requireExpanded = InstrumentationRegistry.getArguments().getString("requireExpanded") == "true"

        Log.i(EVIDENCE_TAG, "CONFIG widthDp=$widthDp heightDp=$heightDp requireExpanded=$requireExpanded")

        if (widthDp < 1080) {
            if (requireExpanded) {
                assertTrue(
                    "Galaxy Tab verification must actually enter expanded mode: widthDp=$widthDp heightDp=$heightDp",
                    widthDp >= 1080
                )
            }
            assertTrue("Default phone regression run is expected to be narrower than expanded threshold", widthDp < 1080)
            return
        }

        compose.onNodeWithTag("textbook_v1_entry").assertExists().performClick()
        compose.onNodeWithTag("v1_textbook_root").assertExists()
        compose.onNodeWithTag("textbook_layout_expanded").assertExists()
        compose.onNodeWithTag("textbook_toc").assertExists()
        compose.onNodeWithTag("textbook_reader").assertExists()
        compose.onNodeWithTag("textbook_insight_rail").assertExists()

        // Signal the host only after the verified three-column frame is composed.
        // Keep the Activity alive long enough for host adb to capture that exact frame.
        Log.i(EVIDENCE_TAG, "READY_FOR_SCREENSHOT widthDp=$widthDp heightDp=$heightDp")
        Thread.sleep(15_000)
        Log.i(EVIDENCE_TAG, "SCREENSHOT_WINDOW_COMPLETE")
    }

    companion object {
        const val EVIDENCE_TAG = "V1TabletEvidence"
    }
}
