package com.futuretech.poweruser.textbook

import android.content.Context
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import com.futuretech.poweruser.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AllTracksReaderRoutingInstrumentedTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetReaderProgress() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cleared = context
            .getSharedPreferences("v3_deep_beginner_progress", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        assertTrue("Reader progress must be reset before 11-TRACK routing verification", cleared)
        compose.waitForIdle()
    }

    @Test
    fun allElevenTracksOpenInTheInstalledReader() {
        waitForTag("curriculum_overview_root")
        compose.onNodeWithTag("curriculum_chapter_count")
            .assertTextContains("TRACK 11개", substring = true)

        val bundledV5Tracks = setOf(1, 2, 7, 10)

        for (trackNumber in 1..11) {
            val chapterId = "V1-C" + trackNumber.toString().padStart(2, '0')
            val chapterTag = "curriculum_chapter_$chapterId"

            compose.onNodeWithTag("curriculum_track_shelf")
                .performScrollToNode(hasTestTag(chapterTag))
            compose.onNodeWithTag(chapterTag).performClick()

            if (trackNumber in bundledV5Tracks) {
                waitForTag("v5_book_root")
                waitForTag("v5_part_strip")
                waitForTag("v5_part_reader")
            } else {
                waitForTag("v1_textbook_root")
                waitForTag("textbook_section_strip")
                waitForTag("textbook_reader")
            }

            compose.onNodeWithText("‹ 서재").performClick()
            waitForTag("curriculum_overview_root")
        }
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
