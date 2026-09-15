package com.futuretech.poweruser.sandbox

import android.content.Context
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class RealPythonExecutionEngine(private val context: Context?) {

    suspend fun execute(code: String, timeoutMs: Long): ExecutionResult = withContext(Dispatchers.Default) {
        val ctx = context?.applicationContext
            ?: return@withContext ExecutionResult(false, "", "Python runtime requires Android context")
        val startedAt = System.currentTimeMillis()

        try {
            ensureStarted(ctx)
            val json = Python.getInstance()
                .getModule("safe_exec")
                .callAttr("run_code", code, timeoutMs.toInt(), 50000, 65536)
                .toString()
            val obj = JSONObject(json)
            ExecutionResult(
                isSuccess = obj.optBoolean("success", false),
                output = obj.optString("output", ""),
                errorMessage = obj.optString("errorMessage", null),
                executionTimeMs = System.currentTimeMillis() - startedAt,
                isSecurityViolation = obj.optBoolean("isSecurityViolation", false),
                isTimeout = obj.optBoolean("isTimeout", false)
            )
        } catch (e: PyException) {
            ExecutionResult(
                isSuccess = false,
                output = "",
                errorMessage = "Python runtime error: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startedAt
            )
        } catch (e: Exception) {
            ExecutionResult(
                isSuccess = false,
                output = "",
                errorMessage = "Python engine initialization error: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startedAt
            )
        }
    }

    private fun ensureStarted(context: Context) {
        if (Python.isStarted()) return
        synchronized(Python::class.java) {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
        }
    }
}
