package com.futuretech.poweruser.education

import android.content.Context
import androidx.compose.ui.test.hasSetTextAction
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
class LearningPracticeModeUiInstrumentedTest {
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

    @Test
    fun challengeUsesSeparateRouteAndLocksHintsAndAi() {
        composeRule.onNodeWithTag("learning_home_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("이어서 학습 ▶").performClick()
        composeRule.onNodeWithTag("session_mode_practice").assertExists()

        composeRule.onNodeWithText("Challenge").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("session_mode_challenge").assertExists()
        composeRule.onNodeWithText("Chapter Challenge · 힌트/AI/정답 보기 잠금 · 제출 결과로만 독립 수행을 확인")
            .assertExists()

        composeRule.onAllNodes(hasSetTextAction())[0]
            .performTextInput("입력에서 처리 과정을 거쳐 결과가 나온다고 예상합니다")
        composeRule.onNodeWithText("예상 제출").performScrollTo().performClick()
        composeRule.onNodeWithText("다음 →").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("2. 코드 수정 → 실행 → 제출").assertExists()
        composeRule.onNodeWithText("Chapter Challenge에서는 힌트가 잠깁니다.").assertExists()
        composeRule.onNodeWithTag("practice_hint_button").assertDoesNotExist()
        composeRule.onNodeWithText("AI 학습 모드").assertDoesNotExist()
    }
}
