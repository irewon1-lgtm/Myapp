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

    @Test
    fun `self explanation requires length and concept coverage`() {
        val keywords = listOf("API", "서버", "요청", "응답")
        val good = evaluateExplanation(
            "API는 앱이 서버에 요청을 보내고 서버가 응답을 돌려받기 위한 접점입니다. 그래서 요청과 응답 흐름을 같이 확인해야 합니다.",
            keywords
        )
        val shortKeywordDump = evaluateExplanation("API 서버 요청 응답", keywords)
        val longButIrrelevant = evaluateExplanation(
            "오늘은 날씨가 좋고 점심을 먹었으며 산책을 오래 했습니다. 이 문장은 충분히 길지만 수업의 핵심 개념을 설명하지 않습니다.",
            keywords
        )
        assertTrue(good.passed)
        assertFalse(shortKeywordDump.passed)
        assertFalse(longButIrrelevant.passed)
    }
}
