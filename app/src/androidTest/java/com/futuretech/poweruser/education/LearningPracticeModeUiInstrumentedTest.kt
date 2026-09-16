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
class LearningPracticeModeUiInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun openFirstV2TrackPractice() {
        composeRule.onNodeWithTag("curriculum_chapter_V1-C01").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("textbook_reader")
            .performScrollToNode(hasTestTag("textbook_practice_button"))
        composeRule.onNodeWithTag("textbook_practice_button").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun v2TrackPracticeCanOpenTenProblemChallengeAndChallengeLocksAssists() {
        openFirstV2TrackPractice()

        composeRule.onNodeWithTag("session_mode_practice").assertExists()
        composeRule.onNodeWithText("V2-T01 · 문제 1/6").assertExists()

        composeRule.onNodeWithText("Challenge").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("chapter_challenge_root").assertExists()
        composeRule.onNodeWithText("Chapter Challenge").assertExists()
        composeRule.onNodeWithText("힌트·AI·정답 보기 없음 · 제출 결과만 숙련도에 반영").assertExists()
        composeRule.onNodeWithText("V2-T01 · 1/10").assertExists()
        composeRule.onNodeWithText("AI 학습 모드").assertDoesNotExist()
        composeRule.onNodeWithTag("practice_hint_button").assertDoesNotExist()
    }
}
