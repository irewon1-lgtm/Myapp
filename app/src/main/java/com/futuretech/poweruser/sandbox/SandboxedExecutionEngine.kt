package com.futuretech.poweruser.sandbox

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class ProgrammingLanguage {
    PYTHON,
    HTML_JS,
    SQL,
    TYPESCRIPT
}

data class ExecutionResult(
    val isSuccess: Boolean,
    val output: String,
    val errorMessage: String? = null,
    val executionTimeMs: Long = 0,
    val isSecurityViolation: Boolean = false,
    val isTimeout: Boolean = false
)

class SandboxedExecutionEngine(private val context: Context?) {
    private val timeoutMs = 2500L
    private val pythonEngine by lazy { RealPythonExecutionEngine(context) }
    private val typeScriptEngine by lazy { TypeScriptExecutionEngine(context) }

    suspend fun execute(
        language: ProgrammingLanguage,
        code: String,
        userRole: String = "BEGINNER"
    ): ExecutionResult {
        if (code.length > 32768) {
            return ExecutionResult(
                false, "", "[security] code is limited to 32768 characters",
                isSecurityViolation = true
            )
        }
        return when (language) {
            ProgrammingLanguage.PYTHON -> pythonEngine.execute(code, timeoutMs)
            ProgrammingLanguage.TYPESCRIPT -> typeScriptEngine.execute(code, timeoutMs)
            ProgrammingLanguage.SQL -> executeSqlSandbox(code)
            ProgrammingLanguage.HTML_JS -> executeHtmlJsSandbox(code)
        }
    }

    private suspend fun executeSqlSandbox(sql: String): ExecutionResult = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val upper = sql.uppercase()
        val forbidden = listOf("ATTACH ", "DETACH ", "PRAGMA ", "VACUUM", "LOAD_EXTENSION")
        if (forbidden.any { upper.contains(it) }) {
            return@withContext ExecutionResult(
                false, "", "[security] SQL operation is not allowed in the isolated practice database",
                System.currentTimeMillis() - startedAt, isSecurityViolation = true
            )
        }
        if (context == null) {
            return@withContext ExecutionResult(false, "", "SQL runtime requires Android context")
        }

        val isolatedDbFile = File(context.cacheDir, "isolated_practice_sandbox.db")
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openOrCreateDatabase(isolatedDbFile, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS stock_practice (id INTEGER PRIMARY KEY, name TEXT, price INTEGER, rating TEXT);")
            db.execSQL("DELETE FROM stock_practice;")
            db.execSQL("INSERT INTO stock_practice (name, price, rating) VALUES ('삼성전자', 75000, 'A');")
            db.execSQL("INSERT INTO stock_practice (name, price, rating) VALUES ('SK하이닉스', 140000, 'A+');")
            db.execSQL("INSERT INTO stock_practice (name, price, rating) VALUES ('현대차', 220000, 'B');")

            val output = StringBuilder()
            val statements = sql.split(";").map { it.trim() }.filter { it.isNotEmpty() }
            for (stmt in statements) {
                if (stmt.uppercase().startsWith("SELECT")) {
                    db.rawQuery(stmt, null).use { cursor ->
                        output.append("[").append(cursor.columnNames.joinToString(" | ")).append("]\n")
                        while (cursor.moveToNext()) {
                            val row = (0 until cursor.columnCount).map { cursor.getString(it) ?: "NULL" }
                            output.append(row.joinToString(" | ")).append("\n")
                        }
                    }
                } else {
                    db.execSQL(stmt)
                    output.append("쿼리 실행 완료: ").append(stmt).append("\n")
                }
            }
            ExecutionResult(true, output.toString().ifEmpty { "쿼리 실행 완료." }, executionTimeMs = System.currentTimeMillis() - startedAt)
        } catch (e: Exception) {
            ExecutionResult(false, "", "SQL error: ${e.localizedMessage}", System.currentTimeMillis() - startedAt)
        } finally {
            db?.close()
        }
    }

    private fun executeHtmlJsSandbox(code: String): ExecutionResult {
        val lower = code.lowercase()
        val blocked = listOf("fetch(", "xmlhttprequest", "websocket", "file://", "content://")
        if (blocked.any { lower.contains(it) }) {
            return ExecutionResult(
                false, "", "[security] external network/file access is blocked in HTML/JS practice",
                isSecurityViolation = true
            )
        }
        return ExecutionResult(
            true,
            "HTML/CSS/JS preview code accepted. Rendering occurs in the isolated preview WebView."
        )
    }
}
