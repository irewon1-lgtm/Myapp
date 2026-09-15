package com.futuretech.poweruser.sandbox

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests cannot start Android WebView or Chaquopy's Android runtime.
 * Real Python/TypeScript semantics and escape tests live in
 * RuntimeExtremeInstrumentedTest on an Android emulator.
 */
class SandboxedExecutionEngineTest {

    private val sandbox = SandboxedExecutionEngine(null)

    @Test
    fun pythonWithoutAndroidContextFailsClearly() = runBlocking {
        val result = sandbox.execute(ProgrammingLanguage.PYTHON, "print(1)")
        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage?.contains("Android context") == true)
    }

    @Test
    fun typeScriptWithoutAndroidContextFailsClearly() = runBlocking {
        val result = sandbox.execute(ProgrammingLanguage.TYPESCRIPT, "const x:number=1; console.log(x);")
        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage?.contains("Android context") == true)
    }

    @Test
    fun sqlWithoutAndroidContextFailsClearly() = runBlocking {
        val result = sandbox.execute(ProgrammingLanguage.SQL, "SELECT 1;")
        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage?.contains("Android context") == true)
    }

    @Test
    fun htmlPreviewAcceptsSafeOfflineCode() = runBlocking {
        val result = sandbox.execute(ProgrammingLanguage.HTML_JS, "<h1>Hello</h1><script>console.log('ok')</script>")
        assertTrue(result.isSuccess)
    }

    @Test
    fun htmlPreviewBlocksExternalNetworkCode() = runBlocking {
        val dynamic = "fe" + "tch('https://example.invalid')"
        val result = sandbox.execute(ProgrammingLanguage.HTML_JS, dynamic)
        assertFalse(result.isSuccess)
        assertTrue(result.isSecurityViolation)
    }

    @Test
    fun oversizedCodeIsRejectedBeforeRuntime() = runBlocking {
        val result = sandbox.execute(ProgrammingLanguage.PYTHON, "x".repeat(40000))
        assertFalse(result.isSuccess)
        assertTrue(result.isSecurityViolation)
    }
}
