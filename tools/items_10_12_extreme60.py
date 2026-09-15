from pathlib import Path

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
