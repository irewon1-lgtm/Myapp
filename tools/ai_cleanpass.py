#!/usr/bin/env python3
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UNIT_XML = ROOT / "app/build/test-results/testDebugUnitTest/TEST-com.futuretech.poweruser.ai.AiExtreme60Test.xml"
ANDROID_RESULTS = ROOT / "app/build/outputs/androidTest-results/connected"
APK = ROOT / "app/build/outputs/apk/debug/app-debug.apk"

POLICY = (ROOT / "app/src/main/java/com/futuretech/poweruser/ai/AiTutorPolicy.kt").read_text(encoding="utf-8")
GATEWAY = (ROOT / "app/src/main/java/com/futuretech/poweruser/ai/OpenAiTutorGateway.kt").read_text(encoding="utf-8")
COORDINATOR = (ROOT / "app/src/main/java/com/futuretech/poweruser/ai/AiLearningCoordinator.kt").read_text(encoding="utf-8")
PANEL = (ROOT / "app/src/main/java/com/futuretech/poweruser/ui/components/AiLearningPanel.kt").read_text(encoding="utf-8")
MODELS = (ROOT / "app/src/main/java/com/futuretech/poweruser/ai/AiTutorModels.kt").read_text(encoding="utf-8")
KEYSTORE = (ROOT / "app/src/main/java/com/futuretech/poweruser/data/SecureKeyStorage.kt").read_text(encoding="utf-8")
MANIFEST = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
MAIN_SOURCE = "\n".join(p.read_text(encoding="utf-8", errors="ignore") for p in (ROOT / "app/src/main").rglob("*") if p.is_file() and p.suffix in {".kt", ".java", ".xml"})


def parse_suite(path: Path):
    if not path.exists():
        return None
    root = ET.parse(path).getroot()
    cases = []
    for tc in root.findall("testcase"):
        cases.append({
            "name": tc.attrib.get("name", ""),
            "classname": tc.attrib.get("classname", ""),
            "failed": tc.find("failure") is not None or tc.find("error") is not None,
            "skipped": tc.find("skipped") is not None,
        })
    return {
        "tests": int(root.attrib.get("tests", len(cases))),
        "failures": int(root.attrib.get("failures", 0)),
        "errors": int(root.attrib.get("errors", 0)),
        "skipped": int(root.attrib.get("skipped", 0)),
        "cases": cases,
    }


def all_android_suites():
    suites = []
    if ANDROID_RESULTS.exists():
        for path in ANDROID_RESULTS.rglob("*.xml"):
            try:
                root = ET.parse(path).getroot()
            except ET.ParseError:
                continue
            cases = []
            for tc in root.findall("testcase"):
                cases.append((tc.attrib.get("classname", ""), tc.attrib.get("name", ""), tc.find("failure") is not None or tc.find("error") is not None))
            suites.extend(cases)
    return suites


def unit_case_ok(case_id: str) -> bool:
    if not unit:
        return False
    needle = f"[{case_id}]"
    return any(needle in c["name"] and not c["failed"] and not c["skipped"] for c in unit["cases"])


def android_class_ok(fragment: str, expected_min: int = 1) -> bool:
    matching = [c for c in android_cases if fragment in c[0]]
    return len(matching) >= expected_min and all(not c[2] for c in matching)


unit = parse_suite(UNIT_XML)
android_cases = all_android_suites()
unit_extreme_pass = bool(unit and unit["tests"] == 60 and unit["failures"] == 0 and unit["errors"] == 0 and unit["skipped"] == 0)

case_rows = []
for i in range(1, 61):
    case_id = f"AI-X{i:02d}"
    case_rows.append({"id": case_id, "pass": unit_case_ok(case_id)})

extreme_report = {
    "suite": "AI_MODE_EXTREME_60",
    "scope": "three AI learning modes, progressive unlock policy, failure handling, privacy guard, Responses API transport contract",
    "live_provider_success_call": "NOT-RUN: no user API key is stored in CI and no paid live inference is attempted",
    "total": 60,
    "passed": sum(1 for r in case_rows if r["pass"]),
    "failed": sum(1 for r in case_rows if not r["pass"]),
    "cases": case_rows,
}
(ROOT / "ai_extreme_report.json").write_text(json.dumps(extreme_report, ensure_ascii=False, indent=2), encoding="utf-8")

hardcoded_key = re.search(r"sk-[A-Za-z0-9_-]{20,}", MAIN_SOURCE) is not None

checks = [
    ("AI-C01", "3개 모드(SOLO/HINT/COLLABORATE) 정의", all(x in MODELS for x in ["SOLO", "HINT", "COLLABORATE"])),
    ("AI-C02", "혼자 풀기 모드는 AI 호출 정책 차단", unit_case_ok("AI-X09")),
    ("AI-C03", "초급 AI 협업 잠금 + 실제 UI 비활성", unit_case_ok("AI-X01") and android_class_ok("AiLearningPanelInstrumentedTest", 2)),
    ("AI-C04", "중급 AI 협업 해금", unit_case_ok("AI-X03") and "collaborateEnabled = collaboration" in POLICY),
    ("AI-C05", "중급 Level 1~2 전체코드 대신작성 잠금", unit_case_ok("AI-X04") and unit_case_ok("AI-X05")),
    ("AI-C06", "Level 3+도 직접 시도 후에만 전체코드 해금", unit_case_ok("AI-X06") and unit_case_ok("AI-X07")),
    ("AI-C07", "힌트 1/2/3 단계 제어", all(unit_case_ok(x) for x in ["AI-X24", "AI-X25", "AI-X33", "AI-X34"])),
    ("AI-C08", "힌트 모드 전체 정답/완성코드 차단", unit_case_ok("AI-X11") and unit_case_ok("AI-X37")),
    ("AI-C09", "실제 HTTPS Responses API 연결 코드", "https://api.openai.com/v1/responses" in GATEWAY and "HttpURLConnection" in GATEWAY),
    ("AI-C10", "앱 main 소스에 실제 API Key 하드코딩 없음", not hardcoded_key),
    ("AI-C11", "API Key Android Keystore 기반 암호화 저장 + 실기 저장/삭제 시험", "EncryptedSharedPreferences" in KEYSTORE and "MasterKey" in KEYSTORE and android_class_ok("AiCredentialInstrumentedTest")),
    ("AI-C12", "INTERNET 권한 + cleartext 차단", "android.permission.INTERNET" in MANIFEST and 'usesCleartextTraffic="false"' in MANIFEST),
    ("AI-C13", "Key 없음 시 힌트는 로컬 fallback, 협업은 가짜 성공 금지", unit_case_ok("AI-X57") and unit_case_ok("AI-X59")),
    ("AI-C14", "오프라인 오류 분류", unit_case_ok("AI-X55") and "AiFailureKind.OFFLINE" in GATEWAY),
    ("AI-C15", "인증/권한 오류 분류", unit_case_ok("AI-X46") and "AiFailureKind.AUTH" in GATEWAY),
    ("AI-C16", "Rate Limit 오류 분류 + 힌트 fallback", unit_case_ok("AI-X47") and unit_case_ok("AI-X58")),
    ("AI-C17", "Timeout 오류 분류", unit_case_ok("AI-X50") and unit_case_ok("AI-X56")),
    ("AI-C18", "5xx 서버 실패 안전 처리", unit_case_ok("AI-X48")),
    ("AI-C19", "민감정보 전송 전 차단", all(unit_case_ok(x) for x in ["AI-X19", "AI-X20", "AI-X21", "AI-X22", "AI-X60"])),
    ("AI-C20", "질문/코드/응답 길이 제한", all(unit_case_ok(x) for x in ["AI-X17", "AI-X18", "AI-X43"])),
    ("AI-C21", "Responses API output_text/배열 응답 파싱", unit_case_ok("AI-X51") and unit_case_ok("AI-X52")),
    ("AI-C22", "AI가 정책 밖 답을 주면 후처리 차단", unit_case_ok("AI-X54") and "guardResponse" in GATEWAY),
    ("AI-C23", "UI에 비용·전송범위·민감정보 경고", all(text in PANEL for text in ["API 비용", "AI 제공자에게 전송", "개인정보·비밀번호·토큰"])),
    ("AI-C24", "AI Extreme60 60/60", unit_extreme_pass),
    ("AI-C25", "Debug APK 빌드 + Android AI UI/Key 계측시험", APK.exists() and android_class_ok("AiCredentialInstrumentedTest") and android_class_ok("AiLearningPanelInstrumentedTest", 2)),
]

passed = sum(1 for _, _, ok in checks if ok)
lines = [
    "# Priority Issue #2 — AI 3-Mode Scoped CLEAN25",
    "",
    "범위: 혼자 풀기 / 힌트 / AI와 같이 풀기, 초급 제한, 중급 해금, API 실패·보안·개인정보 처리.",
    "",
    f"- Extreme60: {extreme_report['passed']}/60 PASS",
    f"- CLEAN25: {passed}/25 PASS",
    f"- Android AI credential/UI tests observed: {sum(1 for c in android_cases if 'AiCredentialInstrumentedTest' in c[0] or 'AiLearningPanelInstrumentedTest' in c[0])}",
    f"- Debug APK present: {'YES' if APK.exists() else 'NO'}",
    "- Live paid provider success call: NOT-RUN (CI에 사용자 API Key를 저장하지 않음). 성공 경로는 mock transport로 프로토콜/파싱/정책을 검증하고, 실제 Android에서는 Key 암호화 저장을 검증함.",
    "",
    "| ID | 검증 | 결과 |",
    "|---|---|---|",
]
for cid, desc, ok in checks:
    lines.append(f"| {cid} | {desc} | {'PASS' if ok else 'FAIL'} |")
lines += [
    "",
    "## 판정",
    "",
    "`AI_MODE_SCOPED_CLEAN_PASS`" if passed == 25 and extreme_report["failed"] == 0 else "`AI_MODE_SCOPED_CLEAN_FAIL`",
    "",
    "주의: 이 판정은 Priority Issue #2에 한정되며 앱 전체 Extreme60/CLEAN25 완료를 의미하지 않는다.",
]
(ROOT / "ai_cleanpass_report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

print(f"AI Extreme60: {extreme_report['passed']}/60")
print(f"AI CLEAN25: {passed}/25")
if extreme_report["failed"] or passed != 25:
    for cid, desc, ok in checks:
        if not ok:
            print(f"FAIL {cid}: {desc}")
    sys.exit(1)
