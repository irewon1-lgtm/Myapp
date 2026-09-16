package com.futuretech.poweruser.textbook

import android.content.Context
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.futuretech.poweruser.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class V1TextbookUiInstrumentedTest {
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
    fun appLaunchesIntoBookShelfThenOpensFixedPageReader() {
        assertTagExists("learning_home")
        assertTagExists("home_book_cover")
        assertTagExists("home_continue")
        assertTagAbsent("curriculum_overview_root")

        compose.onNodeWithTag("home_continue").performClick()

        assertTagExists("ebook_textbook_root")
        assertTagExists("ebook_reader")
        assertTagExists("ebook_page_counter")
        assertTagExists("ebook_track_cover")
        assertTagExists("reader_toc_button")
        assertTagExists("ebook_left_edge")
        assertTagExists("ebook_right_edge")

        assertTextAbsent("9권 · 134장")
        assertTextAbsent("권 → 장 → 단원")
        assertTextAbsent("개념 1/1")
        assertTextAbsent("AI CODING OS")
        assertTextAbsent("KNOWLEDGE GRAPH")
        assertTextAbsent("LEARNING MATRIX")
        assertTextAbsent(">_ LAB")
    }

    @Test
    fun tableOfContentsStillOpensFromShelfWithoutBeingTheLaunchScreen() {
        compose.onNodeWithTag("home_curriculum").performClick()
        assertTagExists("curriculum_overview_root")
        compose.onNodeWithTag("curriculum_chapter_count").assertTextContains("TRACK 11개", substring = true)
        assertTagExists("curriculum_chapter_V1-C01")
    }

    private fun assertTagExists(tag: String) {
        val nodes = compose.onAllNodesWithTag(tag).fetchSemanticsNodes()
        assertTrue("Expected learner UI tag to exist: $tag", nodes.isNotEmpty())
    }

    private fun assertTagAbsent(tag: String) {
        val nodes = compose.onAllNodesWithTag(tag).fetchSemanticsNodes()
        assertTrue("Expected learner UI tag to be absent: $tag", nodes.isEmpty())
    }

    private fun assertTextAbsent(text: String) {
        val nodes = compose.onAllNodesWithText(text).fetchSemanticsNodes()
        assertTrue("Legacy/internal label must not be learner-visible: $text", nodes.isEmpty())
    }
}
