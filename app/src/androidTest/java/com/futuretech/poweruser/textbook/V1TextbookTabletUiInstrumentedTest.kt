package com.futuretech.poweruser.textbook

import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.futuretech.poweruser.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class V1TextbookTabletUiInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun galaxyTabSizedWindowUsesFocusedReadingFlowWithoutPermanentSideRails() {
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
                    "Galaxy Tab verification must actually enter tablet width: widthDp=$widthDp heightDp=$heightDp",
                    widthDp >= 1080
                )
            }
            assertTrue("Default phone regression run is expected to be narrower than tablet threshold", widthDp < 1080)
            return
        }

        compose.onNodeWithTag("v1_textbook_root").assertExists()
        compose.onNodeWithTag("textbook_chapter_title").assertExists()
        compose.onNodeWithText("짧은 확인", substring = true).assertExists()

        // Reading mode must not resurrect the old permanent TOC / insight rails on a wide tablet.
        compose.onNodeWithTag("textbook_toc").assertDoesNotExist()
        compose.onNodeWithTag("textbook_insight_rail").assertDoesNotExist()
        compose.onNodeWithText("AI CODING OS").assertDoesNotExist()
        compose.onNodeWithText("KNOWLEDGE GRAPH").assertDoesNotExist()

        Log.i(EVIDENCE_TAG, "READY_FOR_SCREENSHOT widthDp=$widthDp heightDp=$heightDp mode=focused-reading")
        Thread.sleep(15_000)
        Log.i(EVIDENCE_TAG, "SCREENSHOT_WINDOW_COMPLETE")
    }

    companion object {
        const val EVIDENCE_TAG = "V1TabletEvidence"
    }
}
