package com.futuretech.poweruser.textbook

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.futuretech.poweruser.MainActivity
import org.junit.Rule
import org.junit.Test

class V1TextbookUiInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun appLaunchesIntoSectionedTextbookAndMovesOneConceptAtATime() {
        compose.onNodeWithTag("v1_textbook_root").assertExists()
        compose.onNodeWithTag("textbook_chapter_title").assertExists()
        compose.onNodeWithText("짧은 확인", substring = true).assertExists()

        // Item 4: the learner no longer sees the old OS/dashboard chrome or permanent knowledge rails.
        compose.onNodeWithText("AI CODING OS").assertDoesNotExist()
        compose.onNodeWithText("KNOWLEDGE GRAPH").assertDoesNotExist()
        compose.onNodeWithText("LEARNING MATRIX").assertDoesNotExist()

        // Item 5: every concept ends in a low-stakes inline problem before moving on.
        compose.onNodeWithText("다음 개념 →")
            .performScrollTo()
            .assertExists()
            .performClick()
        compose.waitForIdle()
        compose.onNodeWithText("짧은 확인", substring = true).assertExists()
        compose.onNodeWithText("다음 개념 →")
            .performScrollTo()
            .assertExists()
    }
}
