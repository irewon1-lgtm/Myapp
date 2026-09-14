package com.futuretech.poweruser.sandbox

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SandboxedExecutionEngineTest {

    private val sandbox = SandboxedExecutionEngine(null)

    @Test
    fun testPythonExecutionSuccess() = runBlocking {
        val code = """
            a = 10
            b = 20
            c = a + b
            print(c)
        """.trimIndent()

        val result = sandbox.execute(ProgrammingLanguage.PYTHON, code)
        assertTrue(result.isSuccess)
        assertTrue(result.output.contains("30"))
    }

    @Test
    fun testPythonIfElseBranching() = runBlocking {
        val code = """
            score = 85
            if score >= 80:
                print("합격")
            else:
                print("불합격")
        """.trimIndent()

        val result = sandbox.execute(ProgrammingLanguage.PYTHON, code)
        assertTrue(result.isSuccess)
        assertTrue(result.output.contains("합격"))
        assertFalse(result.output.contains("불합격"))
    }

    @Test
    fun testPythonInfiniteLoopGuard() = runBlocking {
        val code = """
            x = 1
            while x < 10:
                print(x)
        """.trimIndent()

        val result = sandbox.execute(ProgrammingLanguage.PYTHON, code)
        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage?.contains("무한루프") == true || result.errorMessage?.contains("SyntaxError") == true)
    }

    @Test
    fun testSqlSandboxExecution() = runBlocking {
        val code = "SELECT name, price FROM stock_practice WHERE price > 100000;"
        val result = sandbox.execute(ProgrammingLanguage.SQL, code)

        assertTrue(result.isSuccess)
        assertTrue(result.output.contains("SK하이닉스") || result.output.contains("현대차") || result.output.contains("SELECT"))
    }

    @Test
    fun testSecurityEscapeTest_AppDbAccess() = runBlocking {
        val code = "open('/data/data/com.futuretech.poweruser/databases/app.db')"
        val result = sandbox.execute(ProgrammingLanguage.PYTHON, code)

        assertFalse(result.isSuccess)
        assertTrue(result.isSecurityViolation)
        assertTrue(result.errorMessage?.contains("보안 규칙 위반") == true)
    }

    @Test
    fun testSecurityEscapeTest_NetworkBlocked() = runBlocking {
        val code = "import requests; requests.get('https://example.com')"
        val result = sandbox.execute(ProgrammingLanguage.PYTHON, code)

        assertFalse(result.isSuccess)
        assertTrue(result.isSecurityViolation)
        assertTrue(result.errorMessage?.contains("네트워크 호출이 금지") == true)
    }
}
