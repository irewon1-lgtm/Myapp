#!/usr/bin/env python3
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path('app/build/test-results/testDebugUnitTest')
OUT = Path('review_evidence')
OUT.mkdir(exist_ok=True)


def read_suite(contains: str):
    matches = list(ROOT.glob(f'*{contains}*.xml'))
    if not matches:
        raise SystemExit(f'MISSING_JUNIT_XML: {contains}')
    tests = failures = errors = skipped = 0
    names = []
    for path in matches:
        root = ET.parse(path).getroot()
        tests += int(root.attrib.get('tests', '0'))
        failures += int(root.attrib.get('failures', '0'))
        errors += int(root.attrib.get('errors', '0'))
        skipped += int(root.attrib.get('skipped', '0'))
        names.extend(node.attrib.get('name', '') for node in root.findall('.//testcase'))
    return tests, failures, errors, skipped, names


extreme = read_suite('ReviewExtreme60Test')
clean = read_suite('ReviewClean25Test')
engine = read_suite('SpacedRepetitionEngineTest')

problems = []
if extreme[0] != 60 or extreme[1] or extreme[2] or extreme[3]:
    problems.append(
        f'Review Extreme expected 60/60, got tests={extreme[0]} failures={extreme[1]} errors={extreme[2]} skipped={extreme[3]}'
    )
if clean[0] != 25 or clean[1] or clean[2] or clean[3]:
    problems.append(
        f'Review CLEAN expected 25/25, got tests={clean[0]} failures={clean[1]} errors={clean[2]} skipped={clean[3]}'
    )
if engine[0] < 9 or engine[1] or engine[2] or engine[3]:
    problems.append(
        f'SpacedRepetitionEngine broad tests not clean: tests={engine[0]} failures={engine[1]} errors={engine[2]} skipped={engine[3]}'
    )

report = f'''# Spaced Review Engine Extreme/CLEAN Evidence

- Review Extreme scenarios: **{extreme[0]}/60**, failures={extreme[1]}, errors={extreme[2]}, skipped={extreme[3]}
- Review CLEAN gates: **{clean[0]}/25**, failures={clean[1]}, errors={clean[2]}, skipped={clean[3]}
- Review engine broad tests: **{engine[0]}**, failures={engine[1]}, errors={engine[2]}, skipped={engine[3]}
- Overall spaced-review evidence: **{'PASS' if not problems else 'FAIL'}**

## Scope
Checks same-day first recall, 1/3/7/14-day progression, final mastery, lapse regression, 10-minute same-day retry, no negative stages, no timestamp overflow, mastered-card idempotency, due-state correctness, and the memorize/understand/AI-assist guidance split.
'''

(OUT / 'REVIEW_EXTREME60_CLEAN25_REPORT.md').write_text(report, encoding='utf-8')
print(report)

if problems:
    for problem in problems:
        print('FAIL:', problem, file=sys.stderr)
    raise SystemExit(1)
