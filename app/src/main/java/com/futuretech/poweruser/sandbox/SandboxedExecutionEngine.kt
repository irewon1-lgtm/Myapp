package com.futuretech.poweruser.sandbox

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

    private val timeoutMs = 3000L

    suspend fun execute(
        language: ProgrammingLanguage,
        code: String,
        userRole: String = "BEGINNER"
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // Security Inspection (Escape Test Check)
        val securityViolation = checkSecurityViolations(code)
        if (securityViolation != null) {
            return@withContext ExecutionResult(
                isSuccess = false,
                output = "",
                errorMessage = "[보안 경고] 샌드박스 보안 규칙 위반: $securityViolation",
                executionTimeMs = System.currentTimeMillis() - startTime,
                isSecurityViolation = true
            )
        }

        val result = withTimeoutOrNull(timeoutMs) {
            when (language) {
                ProgrammingLanguage.PYTHON -> executePythonSandbox(code)
                ProgrammingLanguage.SQL -> executeSqlSandbox(code)
                ProgrammingLanguage.TYPESCRIPT -> executeTypeScriptSandbox(code)
                ProgrammingLanguage.HTML_JS -> executeHtmlJsSandbox(code)
            }
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        if (result == null) {
            ExecutionResult(
                isSuccess = false,
                output = "",
                errorMessage = "[시간 초과] 코드 실행 시간이 ${timeoutMs / 1000}초를 초과하여 강제 종료되었습니다. (무한 루프 의심)",
                executionTimeMs = duration,
                isTimeout = true
            )
        } else {
            result.copy(executionTimeMs = duration)
        }
    }

    private fun checkSecurityViolations(code: String): String? {
        val lowerCode = code.lowercase()

        // 1. Private Files & Key Attempts
        if (lowerCode.contains("shared_prefs") || lowerCode.contains("keystore") || lowerCode.contains("database/app.db")) {
            return "앱 내부 데이터베이스, SharedPreferences 및 KeyStore에 접근할 수 없습니다."
        }
        if (lowerCode.contains("open(") && (lowerCode.contains("/data/data/") || lowerCode.contains("../"))) {
            return "앱의 민감한 파일 경로에 접근할 수 없습니다."
        }

        // 2. Network Isolation Test
        if (lowerCode.contains("import urllib") || lowerCode.contains("import requests") || lowerCode.contains("fetch(") || lowerCode.contains("xmlhttprequest")) {
            return "샌드박스 환경에서는 임의의 외부 네트워크 호출이 금지됩니다."
        }

        // 3. System Command Access
        if (lowerCode.contains("import os") || lowerCode.contains("import subprocess") || lowerCode.contains("system(")) {
            return "시스템 OS 명령어 및 프로세스 실행은 금지됩니다."
        }

        return null
    }

    private fun executePythonSandbox(code: String): ExecutionResult {
        val outputStream = StringBuilder()
        val variables = mutableMapOf<String, Any>()

        try {
            val lines = code.lines()
            var lineIdx = 0
            while (lineIdx < lines.size) {
                val rawLine = lines[lineIdx]
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) {
                    lineIdx++
                    continue
                }

                // If condition handling
                if (line.startsWith("if ") && line.endsWith(":")) {
                    val condExpr = line.substring(3, line.length - 1).trim()
                    val ifTrue = evalCondition(condExpr, variables)

                    val ifBodyLines = mutableListOf<String>()
                    val elseBodyLines = mutableListOf<String>()
                    lineIdx++

                    // Collect if body
                    while (lineIdx < lines.size && (lines[lineIdx].startsWith("    ") || lines[lineIdx].startsWith("\t"))) {
                        ifBodyLines.add(lines[lineIdx].trim())
                        lineIdx++
                    }

                    // Check for else
                    if (lineIdx < lines.size && lines[lineIdx].trim().startsWith("else:")) {
                        lineIdx++
                        while (lineIdx < lines.size && (lines[lineIdx].startsWith("    ") || lines[lineIdx].startsWith("\t"))) {
                            elseBodyLines.add(lines[lineIdx].trim())
                            lineIdx++
                        }
                    }
                    lineIdx-- // adjust loop index counter

                    val activeLines = if (ifTrue) ifBodyLines else elseBodyLines
                    for (bLine in activeLines) {
                        executeSingleStatement(bLine, variables, outputStream)
                    }
                }
                // While loop
                else if (line.startsWith("while ") && line.endsWith(":")) {
                    val condExpr = line.substring(6, line.length - 1).trim()
                    val bodyLines = mutableListOf<String>()
                    lineIdx++
                    while (lineIdx < lines.size && (lines[lineIdx].startsWith("    ") || lines[lineIdx].startsWith("\t"))) {
                        bodyLines.add(lines[lineIdx].trim())
                        lineIdx++
                    }
                    lineIdx--

                    var loopCount = 0
                    while (evalCondition(condExpr, variables)) {
                        loopCount++
                        if (loopCount > 1000) {
                            return ExecutionResult(
                                isSuccess = false,
                                output = outputStream.toString(),
                                errorMessage = "SyntaxError / Infinite Loop: 반복 횟수가 1000회를 초과하여 무한루프 방지를 위해 중단되었습니다."
                            )
                        }
                        for (bLine in bodyLines) {
                            executeSingleStatement(bLine, variables, outputStream)
                        }
                    }
                }
                // For loop
                else if (line.startsWith("for ") && line.contains("in range(") && line.endsWith(":")) {
                    val varName = line.substringAfter("for ").substringBefore(" in").trim()
                    val rangeArg = line.substringAfter("range(").substringBefore(")").trim().toIntOrNull() ?: 0

                    val bodyLines = mutableListOf<String>()
                    lineIdx++
                    while (lineIdx < lines.size && (lines[lineIdx].startsWith("    ") || lines[lineIdx].startsWith("\t"))) {
                        bodyLines.add(lines[lineIdx].trim())
                        lineIdx++
                    }
                    lineIdx--

                    for (i in 0 until rangeArg) {
                        variables[varName] = i
                        for (bLine in bodyLines) {
                            executeSingleStatement(bLine, variables, outputStream)
                        }
                    }
                }
                // Single statements
                else {
                    executeSingleStatement(line, variables, outputStream)
                }
                lineIdx++
            }

            return ExecutionResult(
                isSuccess = true,
                output = outputStream.toString().ifEmpty { "실행이 완료되었습니다. (출력 없음)" }
            )
        } catch (e: Exception) {
            return ExecutionResult(
                isSuccess = false,
                output = outputStream.toString(),
                errorMessage = "Python 실행 구문 오류: ${e.localizedMessage}"
            )
        }
    }

    private fun executeSingleStatement(line: String, variables: MutableMap<String, Any>, outputStream: StringBuilder) {
        if (line.startsWith("print(") && line.endsWith(")")) {
            val expr = line.substring(6, line.length - 1).trim()
            val printVal = evaluateExpression(expr, variables)
            outputStream.append(printVal.toString()).append("\n")
        } else if (line.contains("=") && !line.contains("==") && !line.contains(">=") && !line.contains("<=")) {
            val parts = line.split("=", limit = 2)
            val varName = parts[0].trim()
            val varValueRaw = parts[1].trim()
            variables[varName] = evaluateExpression(varValueRaw, variables)
        }
    }

    private fun evaluateExpression(expr: String, vars: Map<String, Any>): Any {
        if (expr.startsWith("\"") && expr.endsWith("\"") || expr.startsWith("'") && expr.endsWith("'")) {
            return expr.substring(1, expr.length - 1)
        }
        if (expr.toIntOrNull() != null) return expr.toInt()
        if (expr.toDoubleOrNull() != null) return expr.toDouble()
        if (vars.containsKey(expr)) return vars[expr]!!

        if (expr.contains("+")) {
            val parts = expr.split("+")
            var strAcc = ""
            var numAcc = 0
            var isStr = false
            for (p in parts) {
                val valEvaluated = evaluateExpression(p.trim(), vars)
                if (valEvaluated is String) {
                    isStr = true
                    strAcc += valEvaluated
                } else if (valEvaluated is Int) {
                    numAcc += valEvaluated
                    strAcc += valEvaluated.toString()
                }
            }
            return if (isStr) strAcc else numAcc
        }
        return expr
    }

    private fun evalCondition(cond: String, vars: Map<String, Any>): Boolean {
        if (cond == "True" || cond == "true") return true
        if (cond == "False" || cond == "false") return false

        if (cond.contains(">=")) {
            val parts = cond.split(">=")
            val v1 = evaluateExpression(parts[0].trim(), vars).toString().toIntOrNull() ?: 0
            val v2 = evaluateExpression(parts[1].trim(), vars).toString().toIntOrNull() ?: 0
            return v1 >= v2
        }
        if (cond.contains("<=")) {
            val parts = cond.split("<=")
            val v1 = evaluateExpression(parts[0].trim(), vars).toString().toIntOrNull() ?: 0
            val v2 = evaluateExpression(parts[1].trim(), vars).toString().toIntOrNull() ?: 0
            return v1 <= v2
        }
        if (cond.contains("==")) {
            val parts = cond.split("==")
            val v1 = evaluateExpression(parts[0].trim(), vars).toString()
            val v2 = evaluateExpression(parts[1].trim(), vars).toString()
            return v1 == v2
        }
        if (cond.contains("<")) {
            val parts = cond.split("<")
            val v1 = evaluateExpression(parts[0].trim(), vars).toString().toIntOrNull() ?: 0
            val v2 = evaluateExpression(parts[1].trim(), vars).toString().toIntOrNull() ?: 0
            return v1 < v2
        }
        if (cond.contains(">")) {
            val parts = cond.split(">")
            val v1 = evaluateExpression(parts[0].trim(), vars).toString().toIntOrNull() ?: 0
            val v2 = evaluateExpression(parts[1].trim(), vars).toString().toIntOrNull() ?: 0
            return v1 > v2
        }
        return false
    }

    private fun executeSqlSandbox(sql: String): ExecutionResult {
        if (context == null) {
            return ExecutionResult(
                isSuccess = true,
                output = "[id | name | price | rating]\n1 | 삼성전자 | 75000 | A\n2 | SK하이닉스 | 140000 | A+\n3 | 현대차 | 220000 | B"
            )
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

            val outputStream = StringBuilder()
            val statements = sql.split(";").map { it.trim() }.filter { it.isNotEmpty() }

            for (stmt in statements) {
                if (stmt.uppercase().startsWith("SELECT")) {
                    val cursor = db.rawQuery(stmt, null)
                    val colCount = cursor.columnCount
                    val colNames = cursor.columnNames.joinToString(" | ")
                    outputStream.append("[").append(colNames).append("]\n")

                    while (cursor.moveToNext()) {
                        val rowVals = mutableListOf<String>()
                        for (i in 0 until colCount) {
                            rowVals.add(cursor.getString(i) ?: "NULL")
                        }
                        outputStream.append(rowVals.joinToString(" | ")).append("\n")
                    }
                    cursor.close()
                } else {
                    db.execSQL(stmt)
                    outputStream.append("쿼리가 성공적으로 실행되었습니다: $stmt\n")
                }
            }

            return ExecutionResult(
                isSuccess = true,
                output = outputStream.toString().ifEmpty { "쿼리 실행 완료." }
            )
        } catch (e: Exception) {
            return ExecutionResult(
                isSuccess = false,
                output = "",
                errorMessage = "SQL 오류: ${e.localizedMessage}"
            )
        } finally {
            db?.close()
        }
    }

    private fun executeTypeScriptSandbox(code: String): ExecutionResult {
        val jsTranspiled = code
            .replace(Regex(":\\s*string"), "")
            .replace(Regex(":\\s*number"), "")
            .replace(Regex(":\\s*boolean"), "")
            .replace(Regex(":\\s*any"), "")
            .replace("let ", "")
            .replace("const ", "")
            .replace("var ", "")
            .replace("console.log(", "print(")
        return executePythonSandbox(jsTranspiled)
    }

    private fun executeHtmlJsSandbox(code: String): ExecutionResult {
        return ExecutionResult(
            isSuccess = true,
            output = "HTML/CSS/JS 안전한 미리보기 준비 완료:\n-------------------\n$code"
        )
    }
}
