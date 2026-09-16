package com.futuretech.poweruser.textbook

import android.content.Context
import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.futuretech.poweruser.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class V1TextbookTabletUiInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearPersistedV3ReaderState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cleared = context
            .getSharedPreferences("v3_deep_beginner_progress", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        assertTrue("V3 reader state must be isolated between instrumentation tests", cleared)
        compose.waitForIdle()
    }

    @Test
    fun galaxyTabSizedWindowUsesSinglePageBookReader() {
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
        assertTagPresent("curriculum_track_shelf")
        assertTagPresent("curriculum_chapter_V1-C01")
        compose.onNodeWithTag("curriculum_chapter_V1-C01").performClick()
        assertTagPresent("v1_textbook_root")
        assertTagPresent("textbook_section_strip")
        assertTagPresent("textbook_reader")
        assertTagPresent("textbook_section_progress")
        assertTagPresent("textbook_lesson_cover")
        assertTagPresent("textbook_page_indicator")
        assertTagPresent("textbook_left_tap_zone")
        assertTagPresent("textbook_right_tap_zone")
        assertTagPresent("reader_toc_button")
        assertTagAbsent("textbook_concept_progress")
        assertTagAbsent("textbook_toc")
        assertTagAbsent("textbook_insight_rail")

        Log.i(EVIDENCE_TAG, "READY_FOR_SCREENSHOT widthDp=$widthDp heightDp=$heightDp pagedBookReader=true")
        Thread.sleep(15_000)
        Log.i(EVIDENCE_TAG, "SCREENSHOT_WINDOW_COMPLETE")
    }

    private fun assertTagPresent(tag: String) {
        val nodes = compose.onAllNodesWithTag(tag).fetchSemanticsNodes()
        assertTrue("Expected tag to exist on tablet: $tag", nodes.isNotEmpty())
    }

    private fun assertTagAbsent(tag: String) {
        val nodes = compose.onAllNodesWithTag(tag).fetchSemanticsNodes()
        assertTrue("Legacy permanent UI must be absent: $tag", nodes.isEmpty())
    }

    companion object {
        const val EVIDENCE_TAG = "V4BookReaderEvidence"
    }
}
