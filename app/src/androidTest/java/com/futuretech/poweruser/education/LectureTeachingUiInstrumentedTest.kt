package com.futuretech.poweruser.education

import android.content.Context
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.futuretech.poweruser.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LectureTeachingUiInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetLectureProgress() {
        composeRule.activity
            .getSharedPreferences("lecture_progress_v1", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun assertLearningHome() {
        composeRule.onNodeWithTag("learning_home").assertExists()
        composeRule.waitForIdle()
    }

    @Test
    fun lessonStartsWithLectureAndPracticeIsNotVisible() {
        assertLearningHome()
        composeRule.onNodeWithTag("home_continue").performClick()
        composeRule.onNodeWithTag("lecture_root").assertExists()
        composeRule.onNodeWithTag("practice_locked_label").assertExists()
        composeRule.onNodeWithText("1. 실행 결과 예상").assertDoesNotExist()
    }

    @Test
    fun focusedPracticeOpensOnlyAfterAll13LectureSections() {
        assertLearningHome()
        composeRule.onNodeWithTag("home_continue").performClick()
        repeat(12) {
            composeRule.onNodeWithTag("lecture_next").performScrollTo().performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag("lecture_finish").performScrollTo().assertExists().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("1. 실행 결과 예상").assertExists()
        composeRule.onNodeWithText("B01-01 · 문제 1/6").assertExists()
        composeRule.onNodeWithTag("session_mode_practice").assertExists()
        composeRule.onNodeWithTag("practice_locked_label").assertDoesNotExist()
    }
}
