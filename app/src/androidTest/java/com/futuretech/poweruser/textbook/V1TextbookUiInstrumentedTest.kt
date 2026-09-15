package com.futuretech.poweruser.textbook

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.futuretech.poweruser.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class V1TextbookUiInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesIntoCurriculumThenOpensSectionReaderWithoutMeaninglessProgressNoise() {
        assertTagExists("curriculum_overview_root")
        compose.onNodeWithTag("curriculum_chapter_count").assertTextContains("9권 · 134장")
        assertTagExists("curriculum_chapter_V1-C01")
        compose.onNodeWithTag("curriculum_chapter_V1-C01").performClick()

        assertTagExists("v1_textbook_root")
        assertTagExists("textbook_section_strip")
        assertTagExists("textbook_reader")
        assertTagExists("textbook_section_progress")

        assertTextAbsent("개념 1/1")
        assertTextAbsent("AI CODING OS")
        assertTextAbsent("KNOWLEDGE GRAPH")
        assertTextAbsent("LEARNING MATRIX")
        assertTextAbsent(">_ LAB")
    }

    private fun assertTagExists(tag: String) {
        val nodes = compose.onAllNodesWithTag(tag).fetchSemanticsNodes()
        assertTrue("Expected learner UI tag to exist: $tag", nodes.isNotEmpty())
    }

    private fun assertTextAbsent(text: String) {
        val nodes = compose.onAllNodesWithText(text).fetchSemanticsNodes()
        assertTrue("Internal label must not be learner-visible: $text", nodes.isEmpty())
    }
}
