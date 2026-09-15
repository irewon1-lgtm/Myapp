#!/usr/bin/env python3
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path('app/build/test-results/testDebugUnitTest')
OUT = Path('feedback_evidence')
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


extreme = read_suite('FeedbackExtreme60Test')
clean = read_suite('FeedbackClean25Test')
engine = read_suite('PracticeFeedbackEngineTest')

problems = []
if extreme[0] != 60 or extreme[1] or extreme[2] or extreme[3]:
    problems.append(f'Feedback Extreme expected 60/60, got tests={extreme[0]} failures={extreme[1]} errors={extreme[2]} skipped={extreme[3]}')
if clean[0] != 25 or clean[1] or clean[2] or clean[3]:
    problems.append(f'Feedback CLEAN expected 25/25, got tests={clean[0]} failures={clean[1]} errors={clean[2]} skipped={clean[3]}')
if engine[1] or engine[2] or engine[3]:
    problems.append(f'PracticeFeedbackEngine tests not clean: tests={engine[0]} failures={engine[1]} errors={engine[2]} skipped={engine[3]}')

report = f'''# Feedback Engine Extreme/CLEAN Evidence

- Feedback Extreme scenarios: **{extreme[0]}/60**, failures={extreme[1]}, errors={extreme[2]}, skipped={extreme[3]}
- Feedback CLEAN gates: **{clean[0]}/25**, failures={clean[1]}, errors={clean[2]}, skipped={clean[3]}
- Feedback engine broad tests: **{engine[0]}**, failures={engine[1]}, errors={engine[2]}, skipped={engine[3]}
- Overall feedback-engine evidence: **{'PASS' if not problems else 'FAIL'}**

## Scope
Checks explicit correct/incorrect/review/error verdicts, learner answer display, correct answer display, why-explanation, misconception correction, memory cue, retry guidance, lecture-backlink index, fill-blank normalization, code/debug comparison, AI judgement rationale, runtime error classification, and rejection of fake green grading for open-ended answers.
'''

(OUT / 'FEEDBACK_EXTREME60_CLEAN25_REPORT.md').write_text(report, encoding='utf-8')
print(report)

if problems:
    for p in problems:
        print('FAIL:', p, file=sys.stderr)
    raise SystemExit(1)
