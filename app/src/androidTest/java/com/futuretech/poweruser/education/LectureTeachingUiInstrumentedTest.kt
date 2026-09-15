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

    private fun openLearningHome() {
        composeRule.onNodeWithTag("learning_home_button").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun lessonStartsWithLectureAndPracticeIsNotVisible() {
        openLearningHome()
        composeRule.onNodeWithText("이어서 학습 ▶").performClick()
        composeRule.onNodeWithTag("lecture_root").assertExists()
        composeRule.onNodeWithTag("practice_locked_label").assertExists()
        composeRule.onNodeWithText("Step 2. 실행 전에 결과 예상").assertDoesNotExist()
    }

    @Test
    fun practiceOpensOnlyAfterAll13LectureSections() {
        openLearningHome()
        composeRule.onNodeWithText("이어서 학습 ▶").performClick()
        repeat(12) {
            composeRule.onNodeWithTag("lecture_next").performScrollTo().performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag("lecture_finish").performScrollTo().assertExists().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Step 1. 강의 핵심 30초 복습").assertExists()
        composeRule.onNodeWithTag("practice_locked_label").assertDoesNotExist()
    }
}
