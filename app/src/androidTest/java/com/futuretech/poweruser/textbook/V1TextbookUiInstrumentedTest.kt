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
        compose.onNodeWithText("시스템 전체 그림", substring = true).assertExists()
        compose.onNodeWithTag("textbook_practice_button").assertExists()
        compose.onNodeWithTag("textbook_next_chapter").performClick()
        compose.onNodeWithText("파일·경로", substring = true).assertExists()
    }
}
