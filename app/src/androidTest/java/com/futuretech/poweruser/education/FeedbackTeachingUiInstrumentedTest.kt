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
    fun wrongFillAnswerShowsReasonCorrectAnswerAndLectureReturn() {
        composeRule.onNodeWithTag("learning_home_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("이어서 학습 ▶").performClick()
        waitForText("Step 1. 강의 핵심 30초 복습")
        composeRule.onNodeWithText("Step 1. 강의 핵심 30초 복습").assertExists()
        composeRule.onNodeWithText("다음 →").performScrollTo().performClick()
        waitForText("Step 2. 실행 전에 결과 예상")

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("입력 처리 결과를 예상해서 충분히 적은 답안입니다")
        composeRule.onNodeWithText("예상 제출·비교").performScrollTo().performClick()
        waitForText("↔ 비교가 필요한 서술형")
        composeRule.onNodeWithText("↔ 비교가 필요한 서술형").assertExists()
        composeRule.onNodeWithText("다음 →").performScrollTo().performClick()
        waitForText("Step 3. 코드/구조 읽기")
        composeRule.onNodeWithText("Step 3. 코드/구조 읽기").assertExists()

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("핵심 줄의 역할과 왜 중요한지 충분히 설명한 답안입니다")
        waitForText("읽기 답안 제출·비교")
        composeRule.onNodeWithText("읽기 답안 제출·비교").performScrollTo().performClick()
        waitForText("↔ 비교가 필요한 서술형")
        composeRule.onNodeWithText("↔ 비교가 필요한 서술형").assertExists()
        composeRule.onNodeWithText("다음 →").performScrollTo().performClick()
        waitForText("Step 4. 수정하고 실제 실행")

        composeRule.onNodeWithText("▶ 실제 실행").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 8000) {
            composeRule.onAllNodes(hasText("✓ 맞았습니다")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("다음 →").performScrollTo().performClick()
        waitForText("Step 5. 핵심 개념 빈칸")

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("완전히틀린답")
        composeRule.onNodeWithText("정답 확인").performScrollTo().performClick()
        composeRule.onNodeWithTag("feedback_fill").assertExists()
        composeRule.onNodeWithText("✗ 틀렸습니다").assertExists()
        composeRule.onNodeWithText("정답/기준").assertExists()
        composeRule.onNodeWithText("왜 그런가").assertExists()
        composeRule.onNodeWithText("어디서 헷갈렸나").assertExists()
        composeRule.onNodeWithText("기억할 한 문장").assertExists()
        composeRule.onNodeWithText("다시 할 때").assertExists()
        composeRule.onNodeWithText("관련 강의로 돌아가서 다시 보기").performScrollTo().performClick()
        composeRule.onNodeWithTag("lecture_root").assertExists()
        composeRule.onNodeWithTag("practice_locked_label").assertExists()
    }
}
