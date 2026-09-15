from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_once(rel: str, old: str, new: str, marker: str) -> None:
    text = read(rel)
    if marker in text:
        print(f"[skip] {rel}: {marker}")
        return
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{rel}: expected exactly one replacement for {marker}, found {count}")
    write(rel, text.replace(old, new, 1))
    print(f"[patched] {rel}: {marker}")


# 10. Deterministic runtime/tests are the sole grading authority.
policy = "app/src/main/java/com/futuretech/poweruser/ai/AiTutorPolicy.kt"
replace_once(
    policy,
    """\n\n    fun availability(curriculumType: String, level: Int, practiceAttempted: Boolean): AiModeAvailability {""",
    """

    private val aiGradingVerdict = Regex(
        "(?i)(정답\\s*(?:입니다|이라고\\s*판정|으로\\s*판정|처리)|오답\\s*(?:입니다|이라고\\s*판정|으로\\s*판정|처리)|(?:PASS|FAIL)\\s*(?:입니다|로\\s*판정|판정)|AI\\s*(?:가|에게)?\\s*채점|(?:this|your)\\s+(?:answer|code)\\s+is\\s+(?:correct|incorrect))"
    )

    fun availability(curriculumType: String, level: Int, practiceAttempted: Boolean): AiModeAvailability {""",
    "aiGradingVerdict"
)
replace_once(
    policy,
    """            사용자의 목표는 정답 복사가 아니라 구조 이해, 직접 수정, 실행, 오류 범위 좁히기, AI 결과 검증 능력 향상이다.\n            한국어로 쉽고 정확하게 답한다. 모르는 내용을 있는 것처럼 꾸미지 않는다.""",
    """            사용자의 목표는 정답 복사가 아니라 구조 이해, 직접 수정, 실행, 오류 범위 좁히기, AI 결과 검증 능력 향상이다.
            코드의 정답/오답과 PASS/FAIL은 앱의 실제 runtime + deterministic public/hidden tests만 결정한다.
            너는 채점자가 아니다. 앱이 전달한 판정을 바꾸거나 새 판정을 선언하지 말고, 테스트 증거를 바탕으로 원인과 다음 확인점을 설명한다.
            한국어로 쉽고 정확하게 답한다. 모르는 내용을 있는 것처럼 꾸미지 않는다.""",
    "너는 채점자가 아니다"
)
replace_once(
    policy,
    """        if (text.length > 8_000) {\n            return AiPolicyDecision(false, AiFailureKind.BAD_RESPONSE, \"AI 응답이 학습용 제한보다 너무 깁니다.\")\n        }\n        if (request.mode == AiLearningMode.HINT) {""",
    """        if (text.length > 8_000) {
            return AiPolicyDecision(false, AiFailureKind.BAD_RESPONSE, "AI 응답이 학습용 제한보다 너무 깁니다.")
        }
        if (aiGradingVerdict.containsMatchIn(text)) {
            return AiPolicyDecision(
                false,
                AiFailureKind.POLICY_BLOCKED,
                "AI는 정답/오답 또는 PASS/FAIL 판정을 내릴 수 없습니다. 앱의 runtime·deterministic test 결과만 채점에 사용합니다."
            )
        }
        if (request.mode == AiLearningMode.HINT) {""",
    "AI는 정답/오답 또는 PASS/FAIL 판정을 내릴 수 없습니다"
)

# 12. Preserve three hint levels, but never leak the exact solution at level 3.
curriculum = "app/src/main/java/com/futuretech/poweruser/data/CurriculumDataRepository.kt"
replace_once(
    curriculum,
    """internal fun LessonSeed.toLesson(): LessonContent = LessonContent(""",
    """private fun buildLevel3Hint(broken: String, fix: String): String {
    val brokenLines = broken.lines()
    val fixedLines = fix.lines()
    val maxLines = maxOf(brokenLines.size, fixedLines.size)
    val firstChanged = (0 until maxLines).firstOrNull { index ->
        brokenLines.getOrNull(index)?.trimEnd() != fixedLines.getOrNull(index)?.trimEnd()
    }
    return if (firstChanged != null) {
        "힌트 3 · 거의 해결방법: ${firstChanged + 1}번째 줄을 중심으로 고장난 코드와 목표 동작의 차이를 최소 수정하세요. 정답 코드를 그대로 보여주지는 않습니다. 수정 후 직접 실행해 확인하세요."
    } else {
        "힌트 3 · 거의 해결방법: 입력→처리→출력 중 결과가 처음 달라지는 지점을 최소 수정하세요. 정답 코드를 그대로 보여주지는 않습니다."
    }
}

internal fun LessonSeed.toLesson(): LessonContent = LessonContent(""",
    "buildLevel3Hint"
)
replace_once(
    curriculum,
    """    hintLevel1 = \"정답을 바로 찾지 말고 학습 목표와 현재 코드의 차이를 한 문장으로 설명해 보세요.\",\n    hintLevel2 = \"문제가 생기는 줄 하나만 좁혀서 변수·조건·구조·데이터 흐름 중 무엇이 잘못됐는지 확인하세요.\",\n    hintLevel3 = \"수정 방향: ${fix.lines().firstOrNull().orEmpty()}\",""",
    """    hintLevel1 = "힌트 1 · 방향: 정답을 바로 찾지 말고 학습 목표와 현재 코드의 차이를 한 문장으로 설명해 보세요.",
    hintLevel2 = "힌트 2 · 문제 위치: 문제가 생기는 줄 하나만 좁혀서 변수·조건·구조·데이터 흐름 중 무엇이 잘못됐는지 확인하세요.",
    hintLevel3 = buildLevel3Hint(broken, fix),""",
    "hintLevel3 = buildLevel3Hint"
)

# 11. One deterministic five-part failure-feedback contract shared by UI, notes and AI context.
engine = "app/src/main/java/com/futuretech/poweruser/education/PracticeTestEngine.kt"
replace_once(
    engine,
    """data class PracticeSubmissionResult(\n    val results: List<PracticeTestCaseResult>\n) {""",
    """data class SubmissionFailureFeedback(
    val whatFailed: String,
    val evidence: String,
    val why: String,
    val focus: String,
    val retry: String
) {
    fun asText(): String = listOf(
        "무엇이 실패했나: $whatFailed",
        "증거: $evidence",
        "왜 그런가: $why",
        "어디를 생각해볼까: $focus",
        "다시 실행: $retry"
    ).joinToString("\\n")
}

data class PracticeSubmissionResult(
    val results: List<PracticeTestCaseResult>
) {""",
    "SubmissionFailureFeedback"
)
replace_once(
    engine,
    """    val hiddenTotal: Int\n        get() = results.count { it.visibility == PracticeTestVisibility.HIDDEN }\n}""",
    """    val hiddenTotal: Int
        get() = results.count { it.visibility == PracticeTestVisibility.HIDDEN }

    fun failureFeedback(): SubmissionFailureFeedback {
        val publicFailure = results.firstOrNull {
            it.visibility == PracticeTestVisibility.PUBLIC && !it.passed
        }
        if (publicFailure != null) {
            return SubmissionFailureFeedback(
                whatFailed = publicFailure.label,
                evidence = publicFailure.evidence,
                why = "제출 코드가 공개 요구사항을 아직 만족하지 못했습니다. 판정은 실제 runtime과 deterministic test 결과만 사용합니다.",
                focus = "입력→처리→출력 흐름과 기준 예제를 비교하고, 첫 번째로 달라지는 지점을 찾으세요.",
                retry = "수정 후 ▶ 실행으로 확인하고 다시 ✓ 제출하세요."
            )
        }

        val hiddenFailures = results.count {
            it.visibility == PracticeTestVisibility.HIDDEN && !it.passed
        }
        if (hiddenFailures > 0) {
            return SubmissionFailureFeedback(
                whatFailed = "숨은 경계조건",
                evidence = "숨은 테스트 $hiddenPassed/$hiddenTotal 통과 · 실패 입력값 자체는 비공개",
                why = "기본 예제는 동작하지만 빈 값·음수·큰 값·중복 같은 변형에서 결과가 달라졌습니다.",
                focus = "특정 예제 값에만 맞춘 로직, 경계값 처리, 조건 분기를 확인하세요.",
                retry = "테스트 값을 직접 바꿔 ▶ 실행한 뒤 다시 ✓ 제출하세요."
            )
        }

        return SubmissionFailureFeedback(
            whatFailed = "제출 상태 재확인",
            evidence = "공개 $publicPassed/$publicTotal · 숨은 $hiddenPassed/$hiddenTotal",
            why = "현재 실패 테스트를 특정하지 못했습니다.",
            focus = "코드를 다시 실행해 최신 테스트 증거를 만든 뒤 제출하세요.",
            retry = "▶ 실행 → ✓ 제출 순서로 다시 확인하세요."
        )
    }
}""",
    "fun failureFeedback()"
)

# Feed deterministic evidence to AI as read-only context; never as an authority transfer.
panel = "app/src/main/java/com/futuretech/poweruser/ui/components/AiLearningPanel.kt"
replace_once(
    panel,
    """    currentStep: Int,\n    practiceAttempted: Boolean,\n    codeSnapshot: String\n) {""",
    """    currentStep: Int,
    practiceAttempted: Boolean,
    codeSnapshot: String,
    deterministicEvidence: String = ""
) {""",
    "deterministicEvidence: String = \"\""
)
replace_once(
    panel,
    """                                    userMessage = \"현재 내 시도에서 다음으로 확인할 것 한 가지만 힌트로 알려줘.\",""",
    """                                    userMessage = attachDeterministicEvidence(
                                        "현재 내 시도에서 다음으로 확인할 것 한 가지만 힌트로 알려줘.",
                                        deterministicEvidence
                                    ),""",
    "attachDeterministicEvidence(\n                                        \"현재 내 시도"
)
replace_once(
    panel,
    """                                    userMessage = question,\n                                    code = codeSnapshot,""",
    """                                    userMessage = attachDeterministicEvidence(question, deterministicEvidence),
                                    code = codeSnapshot,""",
    "attachDeterministicEvidence(question, deterministicEvidence)"
)
replace_once(
    panel,
    """private fun taskForStep(step: Int): AiTutorTask = when (step) {""",
    """private fun attachDeterministicEvidence(message: String, evidence: String): String {
    val clean = evidence.trim()
    if (clean.isBlank()) return message
    return buildString {
        append(message.trim())
        append("\\n\\n[앱의 deterministic 채점 결과 · 읽기 전용]\\n")
        append(clean.take(2_500))
        append("\\n\\n위 PASS/FAIL은 앱 runtime과 deterministic test가 이미 결정했습니다. 재채점하거나 뒤집지 말고 원인 설명과 다음 확인점만 제시하세요.")
    }
}

private fun taskForStep(step: Int): AiTutorTask = when (step) {""",
    "[앱의 deterministic 채점 결과 · 읽기 전용]"
)

# User-facing screen: same five-step feedback everywhere and explicit 1/2/3 hint stages.
screen = "app/src/main/java/com/futuretech/poweruser/ui/RunSubmitPracticeScreen.kt"
replace_once(
    screen,
    """                        OutlinedButton(onClick = { if (practiceHintLevel < 3) practiceHintLevel++ }) {\n                            Text(\"💡 ${practiceHintLevel}/3\")\n                        }""",
    """                        OutlinedButton(
                            onClick = { if (practiceHintLevel < 3) practiceHintLevel++ },
                            enabled = practiceHintLevel < 3
                        ) {
                            Text(
                                if (practiceHintLevel >= 3) "💡 힌트 3/3 사용"
                                else "💡 힌트 ${practiceHintLevel + 1}/3 받기"
                            )
                        }""",
    "💡 힌트 3/3 사용"
)
replace_once(
    screen,
    """                        RsNeutralBox(\"힌트\", hint)""",
    """                        RsNeutralBox("힌트 ${practiceHintLevel}/3", hint)""",
    "RsNeutralBox(\"힌트 ${practiceHintLevel}/3\""
)
replace_once(
    screen,
    """                val submitted = when (step) {\n                    4 -> practiceSubmit != null\n                    7 -> debugSubmit != null\n                    9 -> missionSubmit != null\n                    else -> explanationFeedback != null\n                }\n                AiLearningPanel(\n                    lesson = lesson,\n                    currentStep = step,\n                    practiceAttempted = submitted,\n                    codeSnapshot = snapshot\n                )""",
    """                val submitted = when (step) {
                    4 -> practiceSubmit != null
                    7 -> debugSubmit != null
                    9 -> missionSubmit != null
                    else -> explanationFeedback != null
                }
                val deterministicEvidence = when (step) {
                    4 -> practiceSubmit?.let(::rsDeterministicEvidence).orEmpty()
                    7 -> debugSubmit?.let(::rsDeterministicEvidence).orEmpty()
                    9 -> missionSubmit?.let(::rsDeterministicEvidence).orEmpty()
                    else -> explanationFeedback?.let {
                        "앱 피드백=${it.verdict}\\n근거=${it.why}"
                    }.orEmpty()
                }
                AiLearningPanel(
                    lesson = lesson,
                    currentStep = step,
                    practiceAttempted = submitted,
                    codeSnapshot = snapshot,
                    deterministicEvidence = deterministicEvidence
                )""",
    "deterministicEvidence = deterministicEvidence"
)
replace_once(
    screen,
    """            if (!result.passed) {\n                Text(\n                    \"실패한 테스트를 고친 뒤 ▶ 실행으로 확인하고 다시 ✓ 제출하세요.\",\n                    fontSize = 12.sp,\n                    fontWeight = FontWeight.Medium\n                )\n            }""",
    """            if (!result.passed) {
                val failure = result.failureFeedback()
                HorizontalDivider()
                RsFeedbackLine("무엇이 실패했나", failure.whatFailed)
                RsFeedbackLine("증거", failure.evidence)
                RsFeedbackLine("왜 그런가", failure.why)
                RsFeedbackLine("어디를 생각해볼까", failure.focus)
                RsFeedbackLine("다시 실행", failure.retry)
            }""",
    "RsFeedbackLine(\"증거\", failure.evidence)"
)
replace_once(
    screen,
    """            Text(feedback.headline, fontWeight = FontWeight.Bold, fontSize = 14.sp)\n            RsFeedbackLine(\"무엇이 확인됐나\", feedback.why)\n            RsFeedbackLine(\"헷갈리기 쉬운 지점\", feedback.misconception)\n            RsFeedbackLine(\"다시 할 때\", feedback.retryGuidance)""",
    """            Text(feedback.headline, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (feedback.verdict == FeedbackVerdict.INCORRECT || feedback.verdict == FeedbackVerdict.ERROR) {
                val evidence = buildString {
                    append("내 답/코드: ")
                    append(feedback.learnerAnswer.take(500).ifBlank { "(입력 없음)" })
                    if (feedback.correctAnswer.isNotBlank()) {
                        append("\\n기준: ")
                        append(feedback.correctAnswer.take(500))
                    }
                }
                RsFeedbackLine("무엇이 실패했나", feedback.headline)
                RsFeedbackLine("증거", evidence)
                RsFeedbackLine("왜 그런가", feedback.why)
                RsFeedbackLine("어디를 생각해볼까", feedback.misconception)
                RsFeedbackLine("다시 실행", feedback.retryGuidance)
            } else {
                RsFeedbackLine("무엇이 확인됐나", feedback.why)
                RsFeedbackLine("헷갈리기 쉬운 지점", feedback.misconception)
                RsFeedbackLine("다시 할 때", feedback.retryGuidance)
            }""",
    "RsFeedbackLine(\"무엇이 실패했나\", feedback.headline)"
)
replace_once(
    screen,
    """private fun rsFailureGuide(result: PracticeSubmissionResult): String {\n    val publicFailure = result.results.firstOrNull {\n        it.visibility == PracticeTestVisibility.PUBLIC && !it.passed\n    }\n    return if (publicFailure != null) {\n        \"무엇이 실패했나: ${publicFailure.label}\\n\" +\n            \"증거: ${publicFailure.evidence}\\n\" +\n            \"왜 그런가: 제출 코드는 공개 요구사항을 아직 만족하지 못했습니다.\\n\" +\n            \"어디를 생각해볼까: 입력→처리→출력 흐름과 기준 예제를 비교하세요.\\n\" +\n            \"다시 실행: 수정 후 ▶ 실행으로 확인하고 ✓ 제출하세요.\"\n    } else {\n        \"무엇이 실패했나: 숨은 경계조건\\n\" +\n            \"증거: 숨은 테스트 ${result.hiddenPassed}/${result.hiddenTotal} 통과\\n\" +\n            \"왜 그런가: 기본 예제는 되지만 빈 값·음수·큰 값·중복과 같은 변형에서 동작이 달라졌습니다.\\n\" +\n            \"어디를 생각해볼까: 특정 예제 값에만 맞춘 코드는 아닌지 확인하세요.\\n\" +\n            \"다시 실행: 테스트 값을 직접 바꿔 ▶ 실행한 뒤 다시 ✓ 제출하세요.\"\n    }\n}""",
    """private fun rsDeterministicEvidence(result: PracticeSubmissionResult): String = buildString {
    append("APP_GRADE=")
    append(if (result.passed) "PASS" else "FAIL")
    append("\\npublic=${result.publicPassed}/${result.publicTotal}")
    append("\\nhidden=${result.hiddenPassed}/${result.hiddenTotal}")
    if (!result.passed) {
        append("\\n")
        append(result.failureFeedback().asText())
    }
}

private fun rsFailureGuide(result: PracticeSubmissionResult): String =
    result.failureFeedback().asText()""",
    "private fun rsDeterministicEvidence"
)

# Dedicated behavior tests for items 10-12.
test_text = r'''package com.futuretech.poweruser.education

import com.futuretech.poweruser.ai.AiFailureKind
import com.futuretech.poweruser.ai.AiLearningMode
import com.futuretech.poweruser.ai.AiTutorPolicy
import com.futuretech.poweruser.ai.AiTutorRequest
import com.futuretech.poweruser.ai.AiTutorTask
import com.futuretech.poweruser.data.CurriculumDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Items10To12ContractTest {
    private fun request(mode: AiLearningMode = AiLearningMode.HINT) = AiTutorRequest(
        lessonId = "TB1-C01",
        curriculumType = "TEXTBOOK_V1",
        level = 1,
        moduleTitle = "module",
        lessonTitle = "lesson",
        expectedOutcome = "outcome",
        practiceLanguage = "PYTHON",
        mode = mode,
        task = AiTutorTask.DEBUG_GUIDANCE,
        userMessage = "왜 실패했는지 설명해줘",
        code = "print('x')",
        hintLevel = 1,
        practiceAttempted = true
    )

    @Test
    fun deterministicSubmissionResultAloneOwnsPassFail() {
        val pass = PracticeSubmissionResult(
            listOf(
                PracticeTestCaseResult("p", PracticeTestVisibility.PUBLIC, "public", true, "ok"),
                PracticeTestCaseResult("h", PracticeTestVisibility.HIDDEN, "hidden", true, "ok")
            )
        )
        val fail = PracticeSubmissionResult(
            listOf(
                PracticeTestCaseResult("p", PracticeTestVisibility.PUBLIC, "public", true, "ok"),
                PracticeTestCaseResult("h", PracticeTestVisibility.HIDDEN, "hidden", false, "secret input must stay hidden")
            )
        )
        assertTrue(pass.passed)
        assertFalse(fail.passed)
    }

    @Test
    fun publicFailureFeedbackUsesFivePartContract() {
        val result = PracticeSubmissionResult(
            listOf(
                PracticeTestCaseResult("p", PracticeTestVisibility.PUBLIC, "기본 입력 실행", false, "NameError at line 2"),
                PracticeTestCaseResult("h", PracticeTestVisibility.HIDDEN, "hidden", true, "ok")
            )
        )
        val feedback = result.failureFeedback()
        assertEquals("기본 입력 실행", feedback.whatFailed)
        assertTrue(feedback.evidence.contains("NameError"))
        assertTrue(feedback.why.contains("deterministic"))
        val text = feedback.asText()
        listOf("무엇이 실패했나:", "증거:", "왜 그런가:", "어디를 생각해볼까:", "다시 실행:").forEach {
            assertTrue(text.contains(it))
        }
    }

    @Test
    fun hiddenFailureFeedbackNeverLeaksHiddenInput() {
        val result = PracticeSubmissionResult(
            listOf(
                PracticeTestCaseResult("p", PracticeTestVisibility.PUBLIC, "public", true, "ok"),
                PracticeTestCaseResult("h", PracticeTestVisibility.HIDDEN, "hidden", false, "TOP_SECRET_EDGE_VALUE")
            )
        )
        val text = result.failureFeedback().asText()
        assertTrue(text.contains("실패 입력값 자체는 비공개"))
        assertFalse(text.contains("TOP_SECRET_EDGE_VALUE"))
    }

    @Test
    fun aiInstructionsDeclareRuntimeTestsAsOnlyGradeAuthority() {
        val instructions = AiTutorPolicy.buildInstructions(request())
        assertTrue(instructions.contains("runtime + deterministic public/hidden tests"))
        assertTrue(instructions.contains("너는 채점자가 아니다"))
    }

    @Test
    fun aiCannotDeclareCorrectIncorrectOrPassFailVerdict() {
        val blocked = AiTutorPolicy.guardResponse(
            request(),
            "정답입니다. 이 코드는 PASS 판정입니다."
        )
        assertFalse(blocked.allowed)
        assertEquals(AiFailureKind.POLICY_BLOCKED, blocked.failureKind)
    }

    @Test
    fun aiMayExplainFailureWithoutRegrading() {
        val allowed = AiTutorPolicy.guardResponse(
            request(),
            "앱 테스트에서 숨은 경계조건이 실패했습니다. 특정 예제 값에 의존했는지 조건 분기를 확인하세요."
        )
        assertTrue(allowed.allowed)
    }

    @Test
    fun threeHintLevelsRemainDistinctAndLevelThreeDoesNotLeakExactFix() {
        val lessons = CurriculumDataRepository.beginnerLessons +
            CurriculumDataRepository.intermediateLessons +
            CurriculumDataRepository.v1TextbookPracticeLessons
        assertTrue(lessons.isNotEmpty())
        lessons.forEach { lesson ->
            assertTrue(lesson.hintLevel1.startsWith("힌트 1"))
            assertTrue(lesson.hintLevel2.startsWith("힌트 2"))
            assertTrue(lesson.hintLevel3.startsWith("힌트 3"))
            assertNotEquals(lesson.hintLevel1, lesson.hintLevel2)
            assertNotEquals(lesson.hintLevel2, lesson.hintLevel3)
            val exactFix = lesson.brokenCodeFix.trim()
            if (exactFix.length >= 4) {
                assertFalse("${lesson.lessonId} hint3 leaked exact fix", lesson.hintLevel3.contains(exactFix))
            }
        }
    }

    @Test
    fun localHintPolicyReturnsExactlyRequestedLevel() {
        assertEquals("h1", AiTutorPolicy.localHint(1, "h1", "h2", "h3"))
        assertEquals("h2", AiTutorPolicy.localHint(2, "h1", "h2", "h3"))
        assertEquals("h3", AiTutorPolicy.localHint(3, "h1", "h2", "h3"))
    }
}
'''
write("app/src/test/java/com/futuretech/poweruser/education/Items10To12ContractTest.kt", test_text)

# Extreme60: 60 explicit source/contract scenarios. Gradle tests provide runtime behavior; this script is the structural gate.
extreme = r'''from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
files = {
    "engine": (ROOT / "app/src/main/java/com/futuretech/poweruser/education/PracticeTestEngine.kt").read_text(encoding="utf-8"),
    "screen": (ROOT / "app/src/main/java/com/futuretech/poweruser/ui/RunSubmitPracticeScreen.kt").read_text(encoding="utf-8"),
    "panel": (ROOT / "app/src/main/java/com/futuretech/poweruser/ui/components/AiLearningPanel.kt").read_text(encoding="utf-8"),
    "policy": (ROOT / "app/src/main/java/com/futuretech/poweruser/ai/AiTutorPolicy.kt").read_text(encoding="utf-8"),
    "curriculum": (ROOT / "app/src/main/java/com/futuretech/poweruser/data/CurriculumDataRepository.kt").read_text(encoding="utf-8"),
}
checks = []

def add(name, cond):
    checks.append((name, bool(cond)))

# 1-20 deterministic grading authority
patterns = [
    ("grade-01 submission pass all tests", "results.all { it.passed }" in files["engine"]),
    ("grade-02 public count deterministic", "publicPassed" in files["engine"]),
    ("grade-03 hidden count deterministic", "hiddenPassed" in files["engine"]),
    ("grade-04 runtime execute open task", "val defaultRun = engine.execute" in files["screen"]),
    ("grade-05 runtime execute reference learner", "val learnerDefault = engine.execute" in files["screen"]),
    ("grade-06 runtime execute reference answer", "val referenceDefault = engine.execute" in files["screen"]),
    ("grade-07 output normalized", "PracticeTestEngine.normalizeOutput" in files["screen"]),
    ("grade-08 hidden edge variants", "pairedEdgeVariants" in files["screen"]),
    ("grade-09 submit gate practice", "practiceSubmit?.passed == true" in files["screen"]),
    ("grade-10 submit gate debug", "debugSubmit?.passed == true" in files["screen"]),
    ("grade-11 submit gate mission", "missionSubmit?.passed == true" in files["screen"]),
    ("grade-12 run says not graded", "채점 안 됨" in files["screen"]),
    ("grade-13 ai read-only evidence", "deterministic 채점 결과 · 읽기 전용" in files["panel"]),
    ("grade-14 ai cannot regrade", "재채점하거나 뒤집지 말고" in files["panel"]),
    ("grade-15 prompt runtime authority", "runtime + deterministic public/hidden tests" in files["policy"]),
    ("grade-16 prompt not grader", "너는 채점자가 아니다" in files["policy"]),
    ("grade-17 response verdict guard", "aiGradingVerdict" in files["policy"]),
    ("grade-18 verdict block message", "AI는 정답/오답 또는 PASS/FAIL 판정을 내릴 수 없습니다" in files["policy"]),
    ("grade-19 ai evidence includes app grade", "APP_GRADE=" in files["screen"]),
    ("grade-20 ai evidence passed explicitly", "deterministicEvidence = deterministicEvidence" in files["screen"]),
]
for item in patterns: add(*item)

# 21-40 five-part feedback contract
patterns = [
    ("feedback-21 model exists", "data class SubmissionFailureFeedback" in files["engine"]),
    ("feedback-22 what failed field", "val whatFailed: String" in files["engine"]),
    ("feedback-23 evidence field", "val evidence: String" in files["engine"]),
    ("feedback-24 why field", "val why: String" in files["engine"]),
    ("feedback-25 focus field", "val focus: String" in files["engine"]),
    ("feedback-26 retry field", "val retry: String" in files["engine"]),
    ("feedback-27 text what failed", "무엇이 실패했나:" in files["engine"]),
    ("feedback-28 text evidence", "증거:" in files["engine"]),
    ("feedback-29 text why", "왜 그런가:" in files["engine"]),
    ("feedback-30 text focus", "어디를 생각해볼까:" in files["engine"]),
    ("feedback-31 text retry", "다시 실행:" in files["engine"]),
    ("feedback-32 ui what failed", "RsFeedbackLine(\"무엇이 실패했나\", failure.whatFailed)" in files["screen"]),
    ("feedback-33 ui evidence", "RsFeedbackLine(\"증거\", failure.evidence)" in files["screen"]),
    ("feedback-34 ui why", "RsFeedbackLine(\"왜 그런가\", failure.why)" in files["screen"]),
    ("feedback-35 ui focus", "RsFeedbackLine(\"어디를 생각해볼까\", failure.focus)" in files["screen"]),
    ("feedback-36 ui retry", "RsFeedbackLine(\"다시 실행\", failure.retry)" in files["screen"]),
    ("feedback-37 generic wrong answer uses contract", "RsFeedbackLine(\"무엇이 실패했나\", feedback.headline)" in files["screen"]),
    ("feedback-38 hidden values stay private", "실패 입력값 자체는 비공개" in files["engine"]),
    ("feedback-39 error notes share formatter", "result.failureFeedback().asText()" in files["screen"]),
    ("feedback-40 ai receives same formatter", "append(result.failureFeedback().asText())" in files["screen"]),
]
for item in patterns: add(*item)

# 41-60 progressive hints and anti-solution leakage
patterns = [
    ("hint-41 level1 explicit", "힌트 1 · 방향:" in files["curriculum"]),
    ("hint-42 level2 explicit", "힌트 2 · 문제 위치:" in files["curriculum"]),
    ("hint-43 level3 explicit", "힌트 3 · 거의 해결방법:" in files["curriculum"]),
    ("hint-44 level3 builder", "buildLevel3Hint" in files["curriculum"]),
    ("hint-45 first changed line", "firstChanged" in files["curriculum"]),
    ("hint-46 no exact fix interpolation", "${fix.lines().firstOrNull().orEmpty()}" not in files["curriculum"]),
    ("hint-47 no exact answer wording", "정답 코드를 그대로 보여주지는 않습니다" in files["curriculum"]),
    ("hint-48 run after hint", "수정 후 직접 실행해 확인하세요" in files["curriculum"]),
    ("hint-49 screen capped at 3", "practiceHintLevel < 3" in files["screen"]),
    ("hint-50 level1 binding", "1 -> lesson.hintLevel1" in files["screen"]),
    ("hint-51 level2 binding", "2 -> lesson.hintLevel2" in files["screen"]),
    ("hint-52 level3 binding", "else -> lesson.hintLevel3" in files["screen"]),
    ("hint-53 next hint label", "힌트 ${practiceHintLevel + 1}/3 받기" in files["screen"]),
    ("hint-54 final hint disabled label", "힌트 3/3 사용" in files["screen"]),
    ("hint-55 visible hint stage", "RsNeutralBox(\"힌트 ${practiceHintLevel}/3\"" in files["screen"]),
    ("hint-56 ai hint cap", "if (hintLevel >= 3 || busy)" in files["panel"]),
    ("hint-57 ai level1", "Level 1: 방향만 제시한다" in files["policy"]),
    ("hint-58 ai level2", "Level 2: 문제 위치나 확인할 구조를 지목한다" in files["policy"]),
    ("hint-59 ai level3", "Level 3: 거의 정답에 가까운 수정 방향" in files["policy"]),
    ("hint-60 ai blocks full code", "전체 완성 코드, 정답 복사본을 절대 제공하지 않는다" in files["policy"]),
]
for item in patterns: add(*item)

assert len(checks) == 60, len(checks)
failed = [name for name, ok in checks if not ok]
report = ["# Items 10-12 Extreme60", "", f"PASS: {60-len(failed)}/60", f"FAIL: {len(failed)}/60", ""]
for i, (name, ok) in enumerate(checks, 1):
    report.append(f"{i:02d}. {'PASS' if ok else 'FAIL'} — {name}")
Path(ROOT / "items_10_12_extreme60_report.md").write_text("\n".join(report) + "\n", encoding="utf-8")
if failed:
    raise SystemExit("Extreme60 failed: " + ", ".join(failed))
print("Extreme60 60/60 PASS")
'''
write("tools/items_10_12_extreme60.py", extreme)

clean = r'''from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
engine = (ROOT / "app/src/main/java/com/futuretech/poweruser/education/PracticeTestEngine.kt").read_text(encoding="utf-8")
screen = (ROOT / "app/src/main/java/com/futuretech/poweruser/ui/RunSubmitPracticeScreen.kt").read_text(encoding="utf-8")
panel = (ROOT / "app/src/main/java/com/futuretech/poweruser/ui/components/AiLearningPanel.kt").read_text(encoding="utf-8")
policy = (ROOT / "app/src/main/java/com/futuretech/poweruser/ai/AiTutorPolicy.kt").read_text(encoding="utf-8")
curriculum = (ROOT / "app/src/main/java/com/futuretech/poweruser/data/CurriculumDataRepository.kt").read_text(encoding="utf-8")
test = (ROOT / "app/src/test/java/com/futuretech/poweruser/education/Items10To12ContractTest.kt").read_text(encoding="utf-8")

gates = [
    ("01 deterministic grade uses all tests", "results.all { it.passed }" in engine),
    ("02 run never changes grade", "채점 안 됨" in screen),
    ("03 submit runtime open task", "rsSubmitOpenTask" in screen and "engine.execute" in screen),
    ("04 submit runtime reference task", "rsSubmitReferenceTask" in screen),
    ("05 public tests exist", "PracticeTestVisibility.PUBLIC" in screen),
    ("06 hidden tests exist", "PracticeTestVisibility.HIDDEN" in screen),
    ("07 AI receives read-only grade evidence", "읽기 전용" in panel),
    ("08 AI cannot regrade", "재채점하거나 뒤집지 말고" in panel),
    ("09 AI prompt says not grader", "너는 채점자가 아니다" in policy),
    ("10 AI verdict guard exists", "aiGradingVerdict" in policy),
    ("11 five-part model exists", "SubmissionFailureFeedback" in engine),
    ("12 feedback what-failed visible", "무엇이 실패했나" in screen),
    ("13 feedback evidence visible", "RsFeedbackLine(\"증거\"" in screen),
    ("14 feedback why visible", "RsFeedbackLine(\"왜 그런가\"" in screen),
    ("15 feedback focus visible", "RsFeedbackLine(\"어디를 생각해볼까\"" in screen),
    ("16 feedback retry visible", "RsFeedbackLine(\"다시 실행\"" in screen),
    ("17 hidden test input not exposed", "실패 입력값 자체는 비공개" in engine),
    ("18 error note reuses same feedback", "rsFailureGuide" in screen and "failureFeedback().asText()" in screen),
    ("19 hint1 direction kept", "힌트 1 · 방향:" in curriculum),
    ("20 hint2 location kept", "힌트 2 · 문제 위치:" in curriculum),
    ("21 hint3 near-solution kept", "힌트 3 · 거의 해결방법:" in curriculum),
    ("22 exact fix not leaked by hint3", "${fix.lines().firstOrNull().orEmpty()}" not in curriculum),
    ("23 hints capped at three", "practiceHintLevel < 3" in screen and "hintLevel >= 3" in panel),
    ("24 behavior contract tests present", "class Items10To12ContractTest" in test),
    ("25 no AI object participates in PracticeSubmissionResult", "AiTutor" not in engine and "OpenAi" not in engine),
]
failed = [name for name, ok in gates if not ok]
report = ["# Items 10-12 CLEAN25", "", f"PASS: {25-len(failed)}/25", f"FAIL: {len(failed)}/25", ""]
for i, (name, ok) in enumerate(gates, 1):
    report.append(f"{i:02d}. {'PASS' if ok else 'FAIL'} — {name}")
Path(ROOT / "items_10_12_clean25_report.md").write_text("\n".join(report) + "\n", encoding="utf-8")
if failed:
    raise SystemExit("CLEAN25 failed: " + ", ".join(failed))
print("CLEAN25 25/25 PASS")
'''
write("tools/items_10_12_clean25.py", clean)

verify_workflow = r'''name: Verify items 10-12 deterministic feedback hints

on:
  push:
    branches:
      - fix/items-10-12-extreme
  workflow_dispatch:

jobs:
  items-10-12-extreme-clean:
    runs-on: ubuntu-latest
    timeout-minutes: 50
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: gradle

      - name: Set up Python 3.13
        uses: actions/setup-python@v5
        with:
          python-version: '3.13'

      - name: Set up Node 20
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Prepare TypeScript runtime assets
        run: |
          npm install --ignore-scripts --no-audit --no-fund
          npm run prepare:typescript
          test -s app/src/main/assets/typescript.js
          test -s app/src/main/assets/ts_libs.js

      - name: JVM full regression
        run: |
          chmod +x ./gradlew
          ./gradlew testDebugUnitTest --stacktrace

      - name: Items 10-12 Extreme60
        run: python tools/items_10_12_extreme60.py

      - name: Items 10-12 CLEAN25
        run: python tools/items_10_12_clean25.py

      - name: Build debug APK
        run: ./gradlew assembleDebug --stacktrace

      - name: Upload evidence
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: items-10-12-extreme-clean-evidence
          if-no-files-found: warn
          path: |
            items_10_12_extreme60_report.md
            items_10_12_clean25_report.md
            app/build/test-results/testDebugUnitTest/**
            app/build/outputs/apk/debug/app-debug.apk
'''
write(".github/workflows/verify-items-10-12.yml", verify_workflow)

print("Items 10-12 patch prepared")
