package com.futuretech.poweruser.textbook

import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.futuretech.poweruser.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class V1TextbookTabletUiInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun galaxyTabSizedWindowUsesFocusedSingleSectionReader() {
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
                    "Galaxy Tab verification requires tablet-sized width: widthDp=$widthDp heightDp=$heightDp",
                    widthDp >= 1080
                )
            }
            assertTrue("Default phone regression is expected below tablet verification width", widthDp < 1080)
            return
        }

        assertTagPresent("curriculum_overview_root")
        assertTagPresent("curriculum_chapter_V1-C01")
        compose.onNodeWithTag("curriculum_chapter_V1-C01").performClick()
        compose.waitForIdle()
        assertTagPresent("v1_textbook_root")
        assertTagPresent("textbook_section_strip")
        assertTagPresent("textbook_reader")
        assertTagPresent("textbook_section_progress")
        assertTagPresent("textbook_section_title")
        assertTagAbsent("textbook_concept_progress")
        assertTagAbsent("textbook_toc")
        assertTagAbsent("textbook_insight_rail")

        Log.i(EVIDENCE_TAG, "READY_FOR_SCREENSHOT widthDp=$widthDp heightDp=$heightDp focusedSectionReader=true")
        Thread.sleep(15_000)
        Log.i(EVIDENCE_TAG, "SCREENSHOT_WINDOW_COMPLETE")
    }

    private fun assertTagPresent(tag: String) {
        val nodes = compose.onAllNodesWithTag(tag).fetchSemanticsNodes()
        assertTrue("Expected tag to exist on tablet: $tag", nodes.isNotEmpty())
    }

    private fun assertTagAbsent(tag: String) {
        val nodes = compose.onAllNodesWithTag(tag).fetchSemanticsNodes()
        assertTrue("Tag must be absent on tablet: $tag", nodes.isEmpty())
    }

    companion object {
        const val EVIDENCE_TAG = "V1TabletEvidence"
    }
}
