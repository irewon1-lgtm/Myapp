#!/usr/bin/env python3
import json
import sys
import zipfile
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
report_path = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "runtime_extreme_report.json"
if not report_path.is_absolute():
    report_path = ROOT / report_path

results = []

def add(cid, title, ok, detail=""):
    results.append((cid, title, bool(ok), detail))

def read(path):
    p = ROOT / path
    return p.read_text(encoding="utf-8") if p.exists() else ""

def xml_clean(pattern):
    files = list(ROOT.glob(pattern))
    if not files:
        return False, "no XML results"
    failures = 0
    tests = 0
    for f in files:
        try:
            root = ET.parse(f).getroot()
            failures += int(root.attrib.get("failures", 0)) + int(root.attrib.get("errors", 0))
            tests += int(root.attrib.get("tests", 0))
        except Exception as exc:
            return False, f"parse error: {exc}"
    return failures == 0 and tests > 0, f"tests={tests}, failures+errors={failures}"

try:
    extreme = json.loads(report_path.read_text(encoding="utf-8"))
except Exception as exc:
    extreme = {"total": 0, "passed": 0, "failed": 999, "cases": []}
    extreme_error = str(exc)
else:
    extreme_error = ""

case_map = {row.get("id"): row for row in extreme.get("cases", [])}
py = read("app/src/main/python/safe_exec.py")
ts_engine = read("app/src/main/java/com/futuretech/poweruser/sandbox/TypeScriptExecutionEngine.kt")
main_engine = read("app/src/main/java/com/futuretech/poweruser/sandbox/SandboxedExecutionEngine.kt")
ts_html = read("app/src/main/assets/ts_runtime.html")
manifest = read("app/src/main/AndroidManifest.xml")
root_gradle = read("build.gradle.kts")
app_gradle = read("app/build.gradle.kts")

def case_ok(cid):
    return bool(case_map.get(cid, {}).get("pass"))

add("RC-01", "Chaquopy real Python plugin configured", 'com.chaquo.python' in root_gradle and 'com.chaquo.python' in app_gradle and 'version = "3.13"' in app_gradle)
add("RC-02", "Python engine uses CPython bridge, not handwritten parser", "com.chaquo.python.Python" in read("app/src/main/java/com/futuretech/poweruser/sandbox/RealPythonExecutionEngine.kt") and "executePythonSandbox" not in main_engine)
add("RC-03", "Python AST security validation present", "ast.parse" in py and "SecurityValidator" in py)
add("RC-04", "Python execution budget/timeout present", "sys.settrace" in py and "SandboxTimeoutError" in py and case_ok("RX-P23"))
add("RC-05", "Python list/index semantics verified", case_ok("RX-P03"))
add("RC-06", "Python function/return semantics verified", case_ok("RX-P08"))
add("RC-07", "Python comprehension semantics verified", case_ok("RX-P10"))
add("RC-08", "Python syntax/runtime errors are failures", case_ok("RX-P15") and case_ok("RX-P16") and case_ok("RX-P17"))
add("RC-09", "Python import/file escape attempts blocked", case_ok("RX-P18") and case_ok("RX-P19") and case_ok("RX-P20") and case_ok("RX-P22"))
add("RC-10", "Python output cap verified", case_ok("RX-P25"))
add("RC-11", "Official TypeScript compiler asset generated", (ROOT / "app/src/main/assets/typescript.js").exists() and (ROOT / "app/src/main/assets/typescript.js").stat().st_size > 1_000_000)
add("RC-12", "TypeScript standard library bundle generated", (ROOT / "app/src/main/assets/ts_libs.js").exists() and (ROOT / "app/src/main/assets/ts_libs.js").stat().st_size > 100_000)
add("RC-13", "TypeScript full compiler Program/type diagnostics used", "ts.createProgram" in ts_html and "getPreEmitDiagnostics" in ts_html)
add("RC-14", "TypeScript semantic type errors rejected", case_ok("RX-T17") and case_ok("RX-T30"))
add("RC-15", "TypeScript runtime semantics verified", case_ok("RX-T03") and case_ok("RX-T05") and case_ok("RX-T09") and case_ok("RX-T10"))
add("RC-16", "TypeScript worker timeout/termination verified", "worker.terminate" in ts_html and case_ok("RX-T25"))
add("RC-17", "TypeScript network/worker escape attempts blocked", case_ok("RX-T19") and case_ok("RX-T20") and case_ok("RX-T21") and case_ok("RX-T22") and case_ok("RX-T23") and case_ok("RX-T24"))
add("RC-18", "WebView external requests blocked by asset loader", "WebViewAssetLoader" in ts_engine and "blockedResponse" in ts_engine)
add("RC-19", "No Android JavaScript bridge exposed", "addJavascriptInterface" not in ts_engine and "addJavascriptInterface" not in ts_html)
add("RC-20", "App has no INTERNET permission", "android.permission.INTERNET" not in manifest)
add("RC-21", "Legacy string-replacement TypeScript engine removed", "executeTypeScriptSandbox" not in main_engine and "replace(Regex" not in main_engine)
unit_ok, unit_detail = xml_clean("app/build/test-results/testDebugUnitTest/*.xml")
add("RC-22", "JVM unit tests pass", unit_ok, unit_detail)
inst_ok, inst_detail = xml_clean("app/build/outputs/androidTest-results/connected/debug/**/*.xml")
if not inst_ok:
    inst_ok, inst_detail = xml_clean("app/build/outputs/androidTest-results/connected/**/*.xml")
add("RC-23", "Android emulator instrumentation passes", inst_ok and extreme.get("total") == 60 and extreme.get("passed") == 60, f"{inst_detail}; extreme={extreme.get('passed')}/{extreme.get('total')} {extreme_error}")
apk = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
add("RC-24", "Debug APK built with Python native runtimes for device+emulator", apk.exists() and apk.stat().st_size > 0 and (lambda names: any(n.startswith('lib/arm64-v8a/') and 'python' in n.lower() for n in names) and any(n.startswith('lib/x86_64/') and 'python' in n.lower() for n in names))(zipfile.ZipFile(apk).namelist()) if apk.exists() else False, f"apk={apk}")
add("RC-25", "Runtime Extreme60 evidence complete; scoped pass only", extreme.get("total") == 60 and extreme.get("passed") == 60 and extreme.get("failed") == 0 and len(case_map) == 60, f"passed={extreme.get('passed')}, total={extreme.get('total')}")

passed = sum(1 for _,_,ok,_ in results if ok)
lines = [
    "# Runtime Issue #1 — Extreme60 / CLEAN25 evidence",
    "",
    "Scope: real Python and TypeScript execution engines only. This is NOT the full-app Extreme60/CLEAN25 completion claim.",
    "",
    f"Result: **{passed}/25 PASS**",
    "",
    "| ID | Check | Result | Detail |",
    "|---|---|---|---|",
]
for cid, title, ok, detail in results:
    lines.append(f"| {cid} | {title} | {'PASS' if ok else 'FAIL'} | {detail.replace('|','/')} |")
(ROOT / "runtime_cleanpass_report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
print("\n".join(lines))
if passed != 25:
    raise SystemExit(1)
