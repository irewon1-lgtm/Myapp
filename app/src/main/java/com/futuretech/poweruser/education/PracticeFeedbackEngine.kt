package com.futuretech.poweruser.education

import com.futuretech.poweruser.data.LessonContent
import com.futuretech.poweruser.sandbox.ExecutionResult

enum class FeedbackVerdict { CORRECT, INCORRECT, REVIEW, ERROR }

data class PracticeFeedback(
    val verdict: FeedbackVerdict,
    val headline: String,
    val learnerAnswer: String,
    val correctAnswer: String,
    val why: String,
    val misconception: String,
    val memoryTip: String,
    val retryGuidance: String,
    val reviewSectionIndex: Int
)

object PracticeFeedbackEngine {
    private const val DEFINITION = 2
    private const val FLOW = 4
    private const val VOCABULARY = 5
    private const val WORKED = 6
    private const val COMPARE = 8
    private const val MISTAKES = 9
    private const val REAL_WORLD = 10
    private const val RECAP = 11

    fun predictionReview(lesson: LessonContent, answer: String) = PracticeFeedback(
        FeedbackVerdict.REVIEW,
        "예상 답안을 기준과 비교해 보세요",
        answer.trim(),
        lesson.expectedOutcome,
        "결과 예상은 문장 하나를 기계적으로 맞히는 문제가 아닙니다. 실행 전에 입력→처리→출력 흐름을 스스로 설명했는지가 핵심입니다. 아래 학습 목표와 내 예상의 공통점과 빠진 부분을 비교하세요.",
        "글자 수를 채우는 것만으로 정답 처리하지 않습니다. 예상과 실제가 다르면 어디에서 생각이 달라졌는지 찾는 것이 학습입니다.",
        "실행 전에는 ‘무엇을 입력받고 → 어떤 규칙으로 처리하고 → 무엇을 내보내나’를 먼저 말합니다.",
        "학습 목표와 비교해 내 예상에서 빠진 입력·조건·출력을 한 문장 보완하세요.",
        FLOW
    )

    fun readingReview(lesson: LessonContent, answer: String) = PracticeFeedback(
        FeedbackVerdict.REVIEW,
        "코드 읽기 답안을 기준과 비교해 보세요",
        answer.trim(),
        lesson.expectedOutcome,
        "코드 읽기는 특정 표현을 외우는 것보다 핵심 줄이 어떤 역할을 하는지 설명하는 훈련입니다. 답안에 ‘어느 줄/구조’와 ‘왜 중요한지’가 둘 다 있는지 확인하세요.",
        "길게 썼다고 자동으로 맞는 답은 아닙니다. 핵심 역할을 잘못 짚었거나 이유가 없으면 다시 읽어야 합니다.",
        "코드를 읽을 때 변수·조건·반복·함수·요청·출력 중 어떤 역할인지 먼저 표시합니다.",
        "핵심 줄 하나를 다시 고르고 ‘이 줄이 없으면 무엇이 달라지는가’를 덧붙이세요.",
        WORKED
    )

    fun fillBlank(lesson: LessonContent, answer: String, accepted: List<String>): PracticeFeedback {
        val ok = accepted.any { normalize(it) == normalize(answer) }
        val canonical = accepted.firstOrNull().orEmpty()
        val definition = glossaryDefinition(lesson, canonical)
        return PracticeFeedback(
            if (ok) FeedbackVerdict.CORRECT else FeedbackVerdict.INCORRECT,
            if (ok) "정답입니다" else "오답입니다",
            answer.trim().ifEmpty { "(입력 없음)" },
            accepted.joinToString(" / ").ifEmpty { "정답 데이터 없음" },
            if (ok) "‘$canonical’은(는) 이 레슨에서 요구한 핵심 개념입니다. $definition"
            else "이 빈칸에서 요구한 개념은 ‘$canonical’입니다. $definition 네 답 ‘${answer.trim()}’은(는) 이 문제에서 인정하는 개념과 일치하지 않습니다.",
            if (ok) "정답 단어만 외우지 말고 실제 흐름에서 어떤 역할인지 연결해야 오래 기억됩니다."
            else "비슷하게 들리는 주변 개념을 고르면 역할을 혼동한 것입니다. 이름보다 ‘무슨 일을 하는가’를 다시 확인하세요.",
            memoryHook(lesson, canonical),
            if (ok) "다음 문제에서 이 용어를 직접 설명하거나 코드에서 찾아보세요."
            else "강의의 ‘오늘 처음 나오는 말 정리’를 다시 본 뒤 정답을 가리고 다시 입력하세요.",
            VOCABULARY
        )
    }

    fun codeRewrite(lesson: LessonContent, actual: String, expected: String, attempt: Int): PracticeFeedback {
        val preserveIndentation = lesson.practiceLanguage.equals("PYTHON", ignoreCase = true)
        val ok = canonical(actual, preserveIndentation) == canonical(expected, preserveIndentation)
        val diff = firstDifference(actual, expected, preserveIndentation)
        return PracticeFeedback(
            if (ok) FeedbackVerdict.CORRECT else FeedbackVerdict.INCORRECT,
            if (ok) "핵심 코드 재작성 성공" else "아직 핵심 코드가 다릅니다",
            actual.trim().ifEmpty { "(입력 없음)" },
            expected.trim(),
            if (ok) "공백과 주석을 제외한 핵심 코드 구조가 학습 예제와 일치합니다. 기억에서 구조를 다시 꺼내는 연습을 통과했습니다."
            else "현재 문제는 자유 코딩이 아니라 방금 배운 기본 구조를 기억에서 다시 만드는 문제입니다. ${diff.ifBlank { "핵심 줄의 순서나 표현이 학습 예제와 다릅니다." }}",
            if (ok) "코드 모양뿐 아니라 각 줄이 무엇을 입력받고 무엇을 바꾸는지 설명할 수 있어야 합니다."
            else if (attempt <= 1) "변수 이름, 조건, 실행 줄 중 하나를 빠뜨렸을 가능성이 큽니다." else "여러 번 틀렸다면 코드 전체를 외우지 말고 줄별 역할을 다시 확인하세요.",
            "코드는 ‘값 준비 → 처리 → 결과 확인’ 덩어리로 묶어 기억합니다.",
            if (ok) "다음 단계에서 같은 구조의 고장난 코드를 직접 고칩니다."
            else "강의의 ‘첫 번째 예제 — 한 줄씩 읽기’를 다시 보고, 예시를 닫은 뒤 다시 작성하세요.",
            WORKED
        )
    }

    fun debug(lesson: LessonContent, actual: String, attempt: Int): PracticeFeedback {
        val expected = lesson.brokenCodeFix
        val preserveIndentation = lesson.practiceLanguage.equals("PYTHON", ignoreCase = true)
        val ok = canonical(actual, preserveIndentation) == canonical(expected, preserveIndentation)
        val diff = firstDifference(actual, expected, preserveIndentation)
        return PracticeFeedback(
            if (ok) FeedbackVerdict.CORRECT else FeedbackVerdict.INCORRECT,
            if (ok) "디버깅 성공" else "아직 오류가 해결되지 않았습니다",
            actual.trim().ifEmpty { "(입력 없음)" },
            expected.trim(),
            if (ok) "고장난 예제와 수정 예제의 차이를 찾아 핵심 오류를 바로잡았습니다. ${fixReason(lesson.brokenCode, expected, preserveIndentation)}"
            else "수정안이 목표 수정과 아직 다릅니다. ${diff.ifBlank { fixReason(lesson.brokenCode, expected, preserveIndentation) }}",
            if (ok) "정답 코드를 외우기보다 ‘증상 → 원인 → 최소 수정’을 기억해야 다음 오류에도 적용할 수 있습니다."
            else when (attempt) {
                1 -> "처음부터 전체 코드를 바꾸면 원인을 놓치기 쉽습니다. 고장난 줄 하나만 좁혀보세요."
                2 -> "증상과 원인을 혼동했을 수 있습니다. 에러가 보이는 위치와 실제 원인이 같은지 다시 확인하세요."
                else -> "정답을 봤더라도 그대로 복사하지 말고 수정 전/후 차이를 한 줄씩 비교하세요."
            },
            "디버깅은 전체를 다시 쓰는 일이 아니라 가장 작은 원인을 찾아 최소한으로 고치는 일입니다.",
            if (ok) "수정 전/후 한 줄 차이를 말로 설명해 보세요."
            else "강의의 ‘초보자가 자주 틀리는 지점’을 다시 본 뒤 한 줄만 수정해 재검사하세요.",
            MISTAKES
        )
    }

    fun aiJudgement(lesson: LessonContent, selectedIndex: Int): PracticeFeedback {
        val learner = lesson.aiHallucinationOptions.getOrNull(selectedIndex) ?: "(선택 없음)"
        val answer = lesson.aiHallucinationOptions.getOrNull(lesson.correctOptionIndex) ?: "(정답 데이터 없음)"
        val ok = selectedIndex == lesson.correctOptionIndex && selectedIndex in lesson.aiHallucinationOptions.indices
        return PracticeFeedback(
            if (ok) FeedbackVerdict.CORRECT else FeedbackVerdict.INCORRECT,
            if (ok) "AI 답 판별 정답" else "AI 답 판별 오답",
            learner,
            answer,
            if (ok) "정답 선택지는 이번 레슨의 핵심 원칙과 맞습니다. ${lesson.explanation}"
            else "이 문제의 정답은 ‘$answer’입니다. ${lesson.explanation} 선택한 답 ‘$learner’은(는) 이번 레슨의 핵심 원칙과 맞지 않습니다.",
            "AI가 자신 있게 말하는지보다 실제 구조·공식 규칙·실행 결과로 검증해야 합니다.",
            "AI 답은 ‘주장 → 근거 → 실제 확인 방법’ 세 칸으로 나눠 검증합니다.",
            if (ok) "같은 기준으로 다른 AI 답도 검토해 보세요."
            else "강의의 ‘헷갈리는 개념과 구분하기’를 다시 보고 정답 선택지가 왜 더 검증 가능한지 비교하세요.",
            COMPARE
        )
    }

    fun execution(lesson: LessonContent, code: String, result: ExecutionResult): PracticeFeedback {
        if (result.isSuccess) {
            return PracticeFeedback(
                FeedbackVerdict.CORRECT,
                "실행 성공",
                code.trim(),
                "실행 성공",
                "코드가 실제 ${lesson.practiceLanguage} 실행 환경에서 오류 없이 끝났습니다. 실제 출력: ${result.output.ifBlank { "출력 없음" }}",
                "실행 성공만으로 이해가 끝난 것은 아닙니다. 왜 이 출력이 나왔는지 설명할 수 있어야 합니다.",
                "실행 전 예상과 실제 출력을 반드시 비교하세요.",
                "값이나 조건 하나를 바꿔 출력이 어떻게 달라지는지 확인하세요.",
                WORKED
            )
        }
        val error = result.errorMessage.orEmpty().ifBlank { "알 수 없는 실행 오류" }
        val c = classifyError(error, result)
        return PracticeFeedback(
            FeedbackVerdict.ERROR,
            "실행 오류 — ${c.first}",
            code.trim(),
            "오류를 고친 뒤 정상 실행",
            "${c.second}\n실제 오류 메시지: $error",
            c.third,
            "오류 메시지의 첫 핵심 단어와 줄 번호를 보고 한 번에 한 군데만 수정합니다.",
            "강의 예제와 현재 코드의 차이를 한 줄씩 비교한 뒤 가장 작은 수정부터 다시 실행하세요.",
            MISTAKES
        )
    }

    fun mission(lesson: LessonContent, code: String, changed: Boolean, result: ExecutionResult?): PracticeFeedback {
        if (!changed) return PracticeFeedback(
            FeedbackVerdict.INCORRECT, "아직 응용이 아닙니다", code.trim(), "원본에서 최소 한 곳을 목적 있게 변경한 코드",
            "기본 실습 코드를 그대로 실행하면 복습이지 응용은 아닙니다. 값·조건·문자열·HTML 내용 중 하나를 바꾸고 결과 변화를 확인해야 합니다.",
            "실행만 성공하면 응용했다고 착각하기 쉽습니다. 무엇을 왜 바꿨는지가 있어야 합니다.",
            "응용 = 구조는 유지 + 입력/조건/상황을 바꿔 결과 변화를 확인", "원본과 달라질 한 가지를 정하고 수정한 뒤 실행하세요.", REAL_WORLD
        )
        if (result == null) return PracticeFeedback(
            FeedbackVerdict.REVIEW, "수정은 했지만 아직 실행 전입니다", code.trim(), "수정 후 실제 실행 결과 확인",
            "코드가 달라진 것은 확인했지만 실제로 동작하는지는 아직 모릅니다.", "코드 변경과 동작 확인을 같은 것으로 생각하면 안 됩니다.",
            "수정 → 실행 → 결과 비교까지 한 묶음입니다.", "응용 코드를 실행하고 실제 결과를 확인하세요.", REAL_WORLD
        )
        return execution(lesson, code, result).copy(
            headline = if (result.isSuccess) "응용 미션 성공" else "응용 미션 실행 실패",
            reviewSectionIndex = if (result.isSuccess) RECAP else MISTAKES
        )
    }

    fun explanation(
        lesson: LessonContent,
        input: String,
        matched: List<String>,
        missing: List<String>,
        passed: Boolean
    ): PracticeFeedback = PracticeFeedback(
        if (passed) FeedbackVerdict.CORRECT else FeedbackVerdict.INCORRECT,
        if (passed) "자기 설명 기준 통과" else "자기 설명에 빠진 핵심이 있습니다",
        input.trim().ifEmpty { "(입력 없음)" },
        "핵심개념: ${lesson.explainKeywords.joinToString(", ")}",
        if (passed) "설명 안에 핵심개념 ${matched.joinToString(", ")}이(가) 포함되고 최소 분량 기준도 충족했습니다. 자동 검사는 사실관계 전체를 완벽히 판정하지 않으므로 강의 핵심정리와 한 번 더 비교하세요."
        else "현재 확인된 핵심개념은 ${matched.joinToString(", ").ifEmpty { "없음" }}이고, 빠진 개념은 ${missing.joinToString(", ").ifEmpty { "없음" }}입니다.",
        if (passed) "키워드를 넣었다고 모든 문장이 자동으로 사실이 되는 것은 아닙니다." else "용어만 나열하거나 너무 짧게 쓰면 실제 이해를 확인하기 어렵습니다.",
        "좋은 설명은 ‘무엇인지 → 왜 필요한지 → 실제 예시’ 순서로 말합니다.",
        if (passed) "강의 핵심정리와 내 설명을 비교해 틀린 문장이 없는지 확인하세요." else "강의의 ‘핵심 정리’를 다시 보고 빠진 개념을 자기 말로 추가하세요.",
        RECAP
    )

    private fun glossaryDefinition(lesson: LessonContent, answer: String): String {
        val entry = LectureContentRepository.forLesson(lesson).glossary.firstOrNull { normalize(it.term) == normalize(answer) }
        return entry?.plainDefinition ?: "이 레슨에서는 ‘$answer’을(를) ${lesson.expectedOutcome}라는 목표 안에서 역할과 흐름으로 이해해야 합니다."
    }

    private fun memoryHook(lesson: LessonContent, answer: String): String =
        LectureContentRepository.forLesson(lesson).glossary.firstOrNull { normalize(it.term) == normalize(answer) }?.memoryHook
            ?: "‘$answer’을(를) 보면 ${lesson.moduleTitle}에서 어떤 역할인지 먼저 떠올리세요."

    private fun fixReason(broken: String, fixed: String, preserveIndentation: Boolean = false) =
        firstDifference(broken, fixed, preserveIndentation).ifBlank { "수정 전후의 핵심 구조 차이를 확인했습니다." }

    private fun firstDifference(actual: String, expected: String, preserveIndentation: Boolean = false): String {
        val a = meaningfulLines(actual, preserveIndentation)
        val e = meaningfulLines(expected, preserveIndentation)
        for (i in 0 until maxOf(a.size, e.size)) {
            val av = a.getOrNull(i)
            val ev = e.getOrNull(i)
            if (normalizeCodeLine(av.orEmpty(), preserveIndentation) != normalizeCodeLine(ev.orEmpty(), preserveIndentation)) {
                return when {
                    av == null -> "${i + 1}번째 핵심 줄이 빠졌습니다. 기대 줄: `${ev.orEmpty()}`"
                    ev == null -> "${i + 1}번째에 불필요한 줄이 있습니다: `$av`"
                    preserveIndentation && normalizeCodeLine(av, false) == normalizeCodeLine(ev, false) && leadingIndent(av) != leadingIndent(ev) ->
                        "${i + 1}번째 핵심 줄의 들여쓰기가 다릅니다. 내 코드 들여쓰기 ${leadingIndent(av)}칸 / 학습 예제 ${leadingIndent(ev)}칸"
                    else -> "${i + 1}번째 핵심 줄이 다릅니다. 내 코드: `$av` / 학습 예제: `$ev`"
                }
            }
        }
        return ""
    }

    private fun classifyError(message: String, result: ExecutionResult): Triple<String, String, String> {
        val m = message.lowercase()
        return when {
            result.isSecurityViolation || "security" in m -> Triple("보안 제한", "학습 샌드박스가 파일·네트워크·프로세스 같은 위험 동작을 차단했습니다.", "외부 시스템 접근을 시도했을 가능성이 있습니다.")
            result.isTimeout || "timeout" in m || "timed out" in m -> Triple("시간 초과", "제한 시간 안에 끝나지 않았습니다. 종료 조건 없는 반복문이나 너무 큰 작업을 확인하세요.", "while/반복문의 종료 조건을 빠뜨리는 실수가 흔합니다.")
            "syntax" in m || "parse" in m -> Triple("문법 오류", "언어 규칙에 맞지 않는 기호·괄호·들여쓰기·키워드가 있습니다.", "문법 오류는 로직보다 기호와 구조를 먼저 비교해야 합니다.")
            "nameerror" in m || "not defined" in m || "unresolved" in m -> Triple("이름 오류", "변수나 함수 이름을 만들지 않았거나 철자가 다릅니다.", "변수 이름의 철자와 정의 순서를 확인하세요.")
            "type" in m -> Triple("타입 오류", "서로 맞지 않는 종류의 값을 연산하거나 함수에 전달했습니다.", "숫자·문자열·리스트처럼 값의 종류를 확인하세요.")
            "sql" in m || "sqlite" in m || "no such" in m -> Triple("SQL/데이터 오류", "SQL 문법, 표 이름, 열 이름 또는 데이터 조건을 다시 확인해야 합니다.", "SELECT 대상과 WHERE 조건, 테이블/컬럼 이름을 혼동하기 쉽습니다.")
            else -> Triple("실행 오류", "코드가 실행 중 오류를 만났습니다. 오류 메시지와 강의 예제를 비교해 원인을 한 단계씩 좁혀야 합니다.", "오류 전체를 한꺼번에 고치지 말고 첫 번째 원인부터 처리하세요.")
        }
    }

    private fun meaningfulLines(code: String, preserveIndentation: Boolean = false) = code.lines()
        .map { stripComment(it).replace("\t", "    ").let { line -> if (preserveIndentation) line.trimEnd() else line.trim() } }
        .filter { it.trim().isNotEmpty() }
    private fun canonical(code: String, preserveIndentation: Boolean = false) =
        meaningfulLines(code, preserveIndentation).joinToString("\n") { normalizeCodeLine(it, preserveIndentation) }
    private fun normalizeCodeLine(value: String, preserveIndentation: Boolean = false): String {
        if (!preserveIndentation) return value.replace(Regex("\\s+"), "")
        val expanded = value.replace("\t", "    ")
        return "${leadingIndent(expanded)}:${expanded.trimStart().replace(Regex("\\s+"), "")}"
    }
    private fun leadingIndent(value: String): Int {
        val expanded = value.replace("\t", "    ")
        return expanded.length - expanded.trimStart().length
    }
    private fun normalize(value: String) = value.trim().lowercase().replace(Regex("\\s+"), "").replace("-", "").replace("_", "")
    private fun stripComment(line: String): String {
        val cut = listOf(line.indexOf('#'), line.indexOf("//")).filter { it >= 0 }.minOrNull() ?: line.length
        return line.substring(0, cut)
    }
}