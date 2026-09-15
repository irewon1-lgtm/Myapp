package com.futuretech.poweruser.textbook

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.futuretech.poweruser.MainActivity
import org.junit.Rule
import org.junit.Test

class V1TextbookUiInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun appLaunchesDirectlyIntoAiTextbookAndChapterNavigationWorks() {
        compose.onNodeWithTag("v1_textbook_root").assertExists()
        compose.onNodeWithTag("textbook_reader").assertExists()
        compose.onNodeWithText("AI CODING OS").assertExists()
        compose.onNodeWithText("Chapter 01").assertExists()

        compose.onNodeWithTag("textbook_reader")
            .performScrollToNode(hasTestTag("textbook_practice_button"))
        compose.onNodeWithTag("textbook_practice_button").assertExists()
        compose.onNodeWithTag("textbook_next_chapter").assertExists().performClick()
        compose.onNodeWithText("Chapter 02").assertExists()
    }
}
