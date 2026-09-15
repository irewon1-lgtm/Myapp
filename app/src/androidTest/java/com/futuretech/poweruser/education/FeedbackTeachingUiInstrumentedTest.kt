package com.futuretech.poweruser.education

import android.content.Context
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.futuretech.poweruser.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedbackTeachingUiInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun markFirstLectureCompleted() {
        composeRule.activity
            .getSharedPreferences("lecture_progress_v1", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("B01-01.completed", true)
            .putInt("B01-01.section", 0)
            .commit()
    }

    private fun waitForText(text: String, timeoutMillis: Long = 5000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun predictionFeedbackExplainsAndCanReturnToCompletedLecture() {
        composeRule.onNodeWithTag("learning_home").assertExists()
        composeRule.onNodeWithTag("home_continue").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("lecture_root").assertExists()
        composeRule.onNodeWithTag("practice_unlocked_label").assertExists()
        composeRule.onNodeWithTag("lecture_start_practice").performClick()
        composeRule.waitForIdle()

        waitForText("1. 실행 결과 예상")
        composeRule.onNodeWithTag("session_mode_practice").assertExists()

        composeRule.onAllNodes(hasSetTextAction())[0]
            .performTextInput("입력 처리 결과를 먼저 예상하고 실제 실행과 비교합니다")
        composeRule.onNodeWithText("예상 제출").performScrollTo().performClick()

        waitForText("예상 답안을 기준과 비교해 보세요")
        composeRule.onNodeWithText("무엇이 확인됐나").assertExists()
        composeRule.onNodeWithText("헷갈리기 쉬운 지점").assertExists()
        composeRule.onNodeWithText("다시 할 때").assertExists()
        composeRule.onNodeWithText("관련 강의 다시 보기").performScrollTo().performClick()

        composeRule.onNodeWithTag("lecture_root").assertExists()
        composeRule.onNodeWithTag("practice_unlocked_label").assertExists()
        composeRule.onNodeWithTag("lecture_start_practice").assertExists()
        composeRule.onNodeWithTag("practice_locked_label").assertDoesNotExist()
    }
}
