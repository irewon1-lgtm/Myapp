package com.futuretech.poweruser.textbook

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.futuretech.poweruser.MainActivity
import org.junit.Rule
import org.junit.Test

class V1TextbookUiInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun homeOpensRealV1TextbookAndChapterNavigationWorks() {
        compose.onNodeWithTag("textbook_v1_entry").assertExists().performClick()
        compose.onNodeWithTag("v1_textbook_root").assertExists()
        compose.onNodeWithTag("textbook_reader").assertExists()
        // Chapter titles also appear in the TOC, so verify the reader's unique
        // "Chapter NN" label instead of requiring the duplicated title text
        // to resolve to exactly one semantics node.
        compose.onNodeWithText("Chapter 01").assertExists()
        compose.onNodeWithTag("textbook_practice_button").assertExists()
        compose.onNodeWithTag("textbook_next_chapter").performClick()
        compose.onNodeWithText("Chapter 02").assertExists()
    }
}
