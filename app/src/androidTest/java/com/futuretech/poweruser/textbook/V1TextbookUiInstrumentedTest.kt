package com.futuretech.poweruser.textbook

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.futuretech.poweruser.MainActivity
import org.junit.Rule
import org.junit.Test

class V1TextbookUiInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesIntoCurriculumThenOpensSectionReader() {
        compose.onNodeWithTag("curriculum_overview_root").assertExists()
        compose.onNodeWithTag("curriculum_chapter_count").assertTextContains("9권 · 134 Chapter")
        compose.onNodeWithTag("curriculum_chapter_V1-C01").assertExists().performClick()

        compose.onNodeWithTag("v1_textbook_root").assertExists()
        compose.onNodeWithTag("textbook_section_strip").assertExists()
        compose.onNodeWithTag("textbook_reader").assertExists()
        compose.onNodeWithTag("textbook_section_progress").assertTextContains("Section 1/")

        compose.onNodeWithText("AI CODING OS").assertDoesNotExist()
        compose.onNodeWithText("KNOWLEDGE GRAPH").assertDoesNotExist()
        compose.onNodeWithText("LEARNING MATRIX").assertDoesNotExist()
        compose.onNodeWithText(">_ LAB").assertDoesNotExist()
    }
}
