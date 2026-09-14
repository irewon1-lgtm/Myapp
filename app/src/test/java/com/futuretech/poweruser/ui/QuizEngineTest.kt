package com.futuretech.poweruser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizEngineTest {

    @Test
    fun `fill blank hides embedded answer`() {
        val question = parseFillBlankPrompt("접속 규칙을 [ API ]라고 부릅니다.")

        assertEquals("접속 규칙을 [          ]라고 부릅니다.", question.displayText)
        assertEquals(listOf("API"), question.acceptedAnswers)
        assertFalse(question.displayText.contains("API"))
    }

    @Test
    fun `fill blank accepts alternate slash answers`() {
        val question = parseFillBlankPrompt("현상을 [ 환각 / Hallucination ]이라고 합니다.")

        assertTrue(isFillBlankCorrect("환각", question.acceptedAnswers))
        assertTrue(isFillBlankCorrect("hallucination", question.acceptedAnswers))
        assertFalse(isFillBlankCorrect("정상", question.acceptedAnswers))
    }

    @Test
    fun `debug grading ignores formatting and comments`() {
        val expected = "request_method = 'GET' # 조회는 GET"
        val input = "request_method='GET'"

        assertTrue(isDebugFixCorrect(input, expected))
        assertFalse(isDebugFixCorrect("request_method='POST'", expected))
    }
}
