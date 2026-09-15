#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

home = read("app/src/main/java/com/futuretech/poweruser/ui/MainHomeScreen.kt")
adaptive_practice = read("app/src/main/java/com/futuretech/poweruser/ui/AdaptiveFocusedPracticeScreen.kt")
adaptive_reader = read("app/src/main/java/com/futuretech/poweruser/ui/AdaptiveTextbookScreen.kt")
reader = read("app/src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt")
focused = read("app/src/main/java/com/futuretech/poweruser/ui/FocusedPracticeScreen.kt")
manifest = read("app/src/main/AndroidManifest.xml")
color = read("app/src/main/java/com/futuretech/poweruser/ui/theme/Color.kt")
policy = read("app/src/main/java/com/futuretech/poweruser/education/LearningPracticePolicy.kt")
ui_policy = read("app/src/main/java/com/futuretech/poweruser/education/LearningUi21To25Policy.kt")
quality = read("app/src/main/java/com/futuretech/poweruser/education/Items31To35QualityContract.kt")
update_gateway = read("app/src/main/java/com/futuretech/poweruser/ai/OpenAiTutorGateway.kt")

checks = [
    ("C01 360dp horizontal overflow policy", 'testTag("practice_phone_vertical")' in adaptive_practice and 'TABLET_BREAKPOINT_DP = 600' in ui_policy),
    ("C02 tablet reader width capped", 'widthIn(max = 780.dp)' in reader and 'READER_MAX_WIDTH_DP = 780' in ui_policy),
    ("C03 no fixed overlap offsets in focused practice", '.offset(' not in focused),
    ("C04 no fluorescent cyan/magenta/yellow hardcodes", all(x not in color.upper() for x in ("00FFFF", "FF00FF", "FFFF00"))),
    ("C05 long chapter is sectioned into focused concepts", 'TextbookSectioner.split' in reader and 'TextbookLearningFlow.buildConcepts' in reader),
    ("C06 primary home touch card has large vertical padding", 'vertical = 17.dp' in home and '.clickable(onClick = onClick)' in home),
    ("C07 submit does not navigate or replace whole screen", 'practiceSubmit = result' in focused and 'navController' not in focused),
    ("C08 rotation keeps activity/practice composition alive", 'orientation|screenSize|screenLayout|keyboardHidden' in manifest),
    ("C09 phone practice has no permanent split", 'if (split == null)' in adaptive_practice and 'practice_phone_vertical' in adaptive_practice),
    ("C10 tablet practice problem pane is 38-42 percent", 'TABLET_PROBLEM_WEIGHT = 0.40f' in ui_policy and 'TABLET_WORKSPACE_WEIGHT = 0.60f' in ui_policy),
    ("C11 tablet reading stays single column", 'reader_single_column' in adaptive_reader),
    ("C12 table of contents uses temporary drawer", 'ModalNavigationDrawer' in adaptive_reader and 'reader_toc_button' in adaptive_reader),
    ("C13 no permanent chapter sidebar in adaptive reader", 'NavigationRail' not in adaptive_reader and 'PermanentNavigationDrawer' not in adaptive_reader),
    ("C14 answer state is not color-only", '"✓ 제출 통과"' in focused and '"! 제출 실패"' in focused),
    ("C15 Run and Submit are textually distinct", 'Text("▶ 실행")' in focused and 'Text("✓ 제출")' in focused),
    ("C16 failed submission keeps editable code in place", '"수정 후 ▶ 실행으로 확인하고 다시 ✓ 제출하세요."' in focused),
    ("C17 result opens in current practice context", 'FocusedSubmissionCard' in focused and 'practiceSubmit?.let' in focused),
    ("C18 code editors reserve usable height", focused.count('minLines = 5') >= 3),
    ("C19 body code and helper typography roles are separate", 'CodeTypography' in focused and 'onSurfaceVariant' in adaptive_practice),
    ("C20 home hides internal problem engine jargon", all(x not in home for x in ("TabRow", "hidden test", "테스트 엔진", "문제 시스템", "초급 40", "중급 50"))),
    ("C21 home centers five learner actions", all(x in home for x in ("오늘 12분", "이어서 학습", "복습 ${dueReviewCount", "프로젝트", "전체 과정"))),
    ("C22 mastery uses stages rather than one numeric score", 'enum class MasteryStage' in policy and 'VERIFIABLE("검증 가능"' in policy),
    ("C23 challenge locks hint AI and solution reveal", all(x in policy for x in ('hintsAllowed', 'aiAssistanceAllowed', 'solutionRevealAllowed')) and 'mode == LearningSessionMode.PRACTICE' in policy),
    ("C24 reading position persists locally", 'TextbookProgressStore' in reader and 'saveSelectedSectionIndex' in reader and 'selectedSectionIndex' in reader),
    ("C25 network failure does not block base learning", '기본 학습은 계속' in update_gateway),
]

assert len(checks) == 25
passed = sum(1 for _, ok in checks if ok)
lines = [
    "# Items 31-35 CLEAN25",
    "",
    f"Result: {passed}/25 PASS" if passed == 25 else f"Result: {passed}/25 PASS, {25-passed} FAIL",
    "",
]
for name, ok in checks:
    lines.append(f"- {'PASS' if ok else 'FAIL'} — {name}")

report = ROOT / "items_31_35_clean25_report.md"
report.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("\n".join(lines))

if passed != 25:
    raise SystemExit(1)
