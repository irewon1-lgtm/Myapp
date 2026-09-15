package com.futuretech.poweruser.education

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun completedLessonCanOpenPracticeAndTenProblemChallengeLocksAssists() {
        composeRule.onNodeWithTag("learning_home").assertExists()
        composeRule.onNodeWithTag("home_continue").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("lecture_root").assertExists()
        composeRule.onNodeWithTag("practice_unlocked_label").assertExists()

        composeRule.onNodeWithTag("lecture_start_practice").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("session_mode_practice").assertExists()

        composeRule.onNodeWithText("Challenge").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chapter_challenge_root").assertExists()
        composeRule.onNodeWithText("Chapter Challenge").assertExists()
        composeRule.onNodeWithText("힌트·AI·정답 보기 없음 · 제출 결과만 숙련도에 반영").assertExists()
        composeRule.onNodeWithText("B01-01 · 1/10").assertExists()
        composeRule.onNodeWithText("AI 학습 모드").assertDoesNotExist()
        composeRule.onNodeWithTag("practice_hint_button").assertDoesNotExist()
    }
}
