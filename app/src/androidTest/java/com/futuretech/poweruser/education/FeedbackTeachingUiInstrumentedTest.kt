package com.futuretech.poweruser.education

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.futuretech.poweruser.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedbackTeachingUiInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String, timeoutMillis: Long = 5000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openFirstV2TrackPractice() {
        composeRule.onNodeWithTag("curriculum_chapter_V1-C01").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("textbook_reader")
            .performScrollToNode(hasTestTag("textbook_practice_button"))
        composeRule.onNodeWithTag("textbook_practice_button").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun predictionFeedbackExplainsAndCanReturnToV2LessonReader() {
        openFirstV2TrackPractice()

        waitForText("1. 실행 결과 예상")
        composeRule.onNodeWithTag("session_mode_practice").assertExists()
        composeRule.onNodeWithText("V2-T01 · 문제 1/6").assertExists()

        composeRule.onAllNodes(hasSetTextAction())[0]
            .performTextInput("입력 처리 결과를 먼저 예상하고 실제 실행과 비교합니다")
        composeRule.onNodeWithText("예상 제출").performScrollTo().performClick()

        waitForText("예상 답안을 기준과 비교해 보세요")
        composeRule.onNodeWithText("무엇이 확인됐나").assertExists()
        composeRule.onNodeWithText("헷갈리기 쉬운 지점").assertExists()
        composeRule.onNodeWithText("다시 할 때").assertExists()
        composeRule.onNodeWithText("관련 강의 다시 보기").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("v1_textbook_root").assertExists()
        composeRule.onNodeWithTag("textbook_reader").assertExists()
        // Practice is opened from the bottom of this LazyColumn. Back navigation restores that
        // scroll position, so bring the header item back into composition before asserting it.
        composeRule.onNodeWithTag("textbook_reader")
            .performScrollToNode(hasTestTag("textbook_section_progress"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("textbook_section_progress").assertExists()
        composeRule.onNodeWithTag("session_mode_practice").assertDoesNotExist()
    }
}
