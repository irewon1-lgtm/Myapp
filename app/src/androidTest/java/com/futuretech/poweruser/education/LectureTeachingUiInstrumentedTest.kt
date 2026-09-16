package com.futuretech.poweruser.education

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.futuretech.poweruser.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LectureTeachingUiInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun openFirstV2TrackReader() {
        composeRule.onNodeWithTag("curriculum_chapter_V1-C01").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun v2TrackStartsWithLessonReaderBeforeFocusedPractice() {
        openFirstV2TrackReader()

        composeRule.onNodeWithTag("v1_textbook_root").assertExists()
        composeRule.onNodeWithTag("textbook_section_strip").assertExists()
        composeRule.onNodeWithTag("textbook_reader").assertExists()
        composeRule.onNodeWithTag("textbook_section_progress").assertExists()
        composeRule.onNodeWithTag("session_mode_practice").assertDoesNotExist()
        composeRule.onNodeWithTag("lecture_root").assertDoesNotExist()
    }

    @Test
    fun v2TrackPracticeOpensFromLessonReaderWithAlignedPracticeId() {
        openFirstV2TrackReader()

        composeRule.onNodeWithTag("textbook_reader")
            .performScrollToNode(hasTestTag("textbook_practice_button"))
        composeRule.onNodeWithTag("textbook_practice_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("focused_practice_root").assertExists()
        composeRule.onNodeWithTag("session_mode_practice").assertExists()
        composeRule.onNodeWithText("1. 실행 결과 예상").assertExists()
        composeRule.onNodeWithText("V2-T01 · 문제 1/6").assertExists()
        composeRule.onNodeWithText("B01-01 · 문제 1/6").assertDoesNotExist()
    }
}
