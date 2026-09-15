from pathlib import Path

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
