package com.futuretech.poweruser.education

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.futuretech.poweruser.MainActivity
import com.futuretech.poweruser.data.AppRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewUiInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val conceptId = "instrumented-review-card"
    private val conceptTitle = "복습 검증 카드"
    private val definition = "정답은 답 확인을 누른 뒤에만 보여야 한다."

    @Before
    fun seedDueReview() {
        val repository = AppRepository(composeRule.activity)
        runBlocking {
            repository.addSpacedRepetitionItem(
                conceptId = conceptId,
                conceptTitle = conceptTitle,
                category = "BEGINNER",
                definition = definition,
                analogy = "앞면을 보고 먼저 떠올린다.",
                example = "answerAfterRecall()",
                comparison = "정답을 먼저 읽는 방식과 구분한다.",
                now = System.currentTimeMillis() - 1_000L
            )
        }
    }

    @Test
    fun answerIsHiddenUntilRevealAndRememberedCardLeavesDueQueue() {
        composeRule.onNodeWithTag("curriculum_review_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("review_screen").assertExists()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(hasText(conceptTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(conceptTitle).assertExists()
        composeRule.onNodeWithText(definition).assertDoesNotExist()
        composeRule.onNodeWithTag("reveal_$conceptId").assertExists().performClick()
        composeRule.onNodeWithText("한 문장 정의").assertExists()
        composeRule.onNodeWithText(definition).assertExists()

        composeRule.onNodeWithTag("remember_$conceptId").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(hasText(conceptTitle)).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText(conceptTitle).assertDoesNotExist()
    }
}
