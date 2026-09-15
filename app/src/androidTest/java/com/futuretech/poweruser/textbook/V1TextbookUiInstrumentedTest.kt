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

    @Test fun homeOpensRealV1TextbookAndChapterNavigationWorks() {
        compose.onNodeWithTag("textbook_v1_entry").assertExists().performClick()
        compose.onNodeWithTag("v1_textbook_root").assertExists()
        compose.onNodeWithTag("textbook_reader").assertExists()
        compose.onNodeWithText("Chapter 01").assertExists()

        // The action card is intentionally at the end of the long university-style
        // chapter. LazyColumn does not compose it until it is brought into view,
        // so exercise the real reading/scroll path instead of assuming it exists
        // in the initial semantics tree.
        compose.onNodeWithTag("textbook_reader")
            .performScrollToNode(hasTestTag("textbook_practice_button"))
        compose.onNodeWithTag("textbook_practice_button").assertExists()
        compose.onNodeWithTag("textbook_next_chapter").assertExists().performClick()
        compose.onNodeWithText("Chapter 02").assertExists()
    }
}
