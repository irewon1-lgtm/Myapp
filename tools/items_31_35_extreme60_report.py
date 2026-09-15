#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
results_dir = ROOT / "app/build/test-results/testDebugUnitTest"
files = list(results_dir.glob("TEST-*Items31To35UserFlowExtreme60Test.xml"))
if not files:
    raise SystemExit("Items31To35UserFlowExtreme60Test XML not found")

tests = failures = errors = skipped = 0
for path in files:
    suite = ET.parse(path).getroot()
    tests += int(suite.attrib.get("tests", "0"))
    failures += int(suite.attrib.get("failures", "0"))
    errors += int(suite.attrib.get("errors", "0"))
    skipped += int(suite.attrib.get("skipped", "0"))

passed = tests - failures - errors - skipped
expected = 60
ok = tests == expected and passed == expected and failures == 0 and errors == 0 and skipped == 0
report = ROOT / "items_31_35_extreme60_report.md"
report.write_text(
    "# Items 31-35 User-Flow Extreme60\n\n"
    "This report is generated from the actual JVM parameterized test XML. "
    "It is not labelled as a physical-device run.\n\n"
    f"- Executed scenarios: {tests}\n"
    f"- Passed: {passed}\n"
    f"- Failed: {failures}\n"
    f"- Errors: {errors}\n"
    f"- Skipped: {skipped}\n"
    f"- Verdict: {'60/60 PASS' if ok else 'FAIL'}\n",
    encoding="utf-8",
)
print(report.read_text(encoding="utf-8"))
if not ok:
    raise SystemExit(1)
