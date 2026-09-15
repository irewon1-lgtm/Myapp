package com.futuretech.poweruser.ai

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.ui.components.AiLearningPanel
import com.futuretech.poweruser.ui.theme.FutureTechTheme
import org.junit.Rule
import org.junit.Test

class AiLearningPanelInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun beginnerCollaborationChipIsLocked() {
        val lesson = CurriculumDataRepository.beginnerLessons.first()
        composeRule.setContent {
            FutureTechTheme {
                AiLearningPanel(
                    lesson = lesson,
                    currentStep = 4,
                    practiceAttempted = true,
                    codeSnapshot = lesson.initialPracticeCode
                )
            }
        }
        composeRule.onNodeWithTag("ai_mode_solo").assertIsEnabled()
        composeRule.onNodeWithTag("ai_mode_hint").assertIsEnabled()
        composeRule.onNodeWithTag("ai_mode_collab").assertIsNotEnabled()
    }

    @Test
    fun intermediateCollaborationChipIsUnlocked() {
        val lesson = CurriculumDataRepository.intermediateLessons.first()
        composeRule.setContent {
            FutureTechTheme {
                AiLearningPanel(
                    lesson = lesson,
                    currentStep = 4,
                    practiceAttempted = false,
                    codeSnapshot = lesson.initialPracticeCode
                )
            }
        }
        composeRule.onNodeWithTag("ai_mode_collab").assertIsEnabled()
    }
}
