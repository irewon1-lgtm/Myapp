#!/usr/bin/env python3
from __future__ import annotations

import glob
import os
import sys
import xml.etree.ElementTree as ET

RESULT_DIR = "app/build/test-results/testDebugUnitTest"
OUT_DIR = "lecture_evidence"
OUT_FILE = os.path.join(OUT_DIR, "LECTURE_EXTREME60_CLEAN25_REPORT.md")


def collect(class_fragment: str):
    tests = 0
    failures = 0
    errors = 0
    names = []
    for path in glob.glob(os.path.join(RESULT_DIR, "TEST-*.xml")):
        root = ET.parse(path).getroot()
        for case in root.iter("testcase"):
            classname = case.attrib.get("classname", "")
            if class_fragment in classname:
                tests += 1
                names.append(case.attrib.get("name", ""))
                failures += len(case.findall("failure"))
                errors += len(case.findall("error"))
    return tests, failures, errors, names


extreme = collect("LectureExtreme60Test")
clean = collect("LectureClean25Test")
content = collect("LectureContentRepositoryTest")
progress = collect("LectureProgressStoreTest")

ok = (
    extreme[0] == 60 and extreme[1] == 0 and extreme[2] == 0
    and clean[0] == 25 and clean[1] == 0 and clean[2] == 0
    and content[0] >= 3 and content[1] == 0 and content[2] == 0
    and progress[0] >= 2 and progress[1] == 0 and progress[2] == 0
)

os.makedirs(OUT_DIR, exist_ok=True)
with open(OUT_FILE, "w", encoding="utf-8") as f:
    f.write("# Lecture Engine Extreme/CLEAN Evidence\n\n")
    f.write(f"- Lecture Extreme scenarios: **{extreme[0]}/60**, failures={extreme[1]}, errors={extreme[2]}\n")
    f.write(f"- Lecture CLEAN gates: **{clean[0]}/25**, failures={clean[1]}, errors={clean[2]}\n")
    f.write(f"- Expanded-content unit tests: **{content[0]}**, failures={content[1]}, errors={content[2]}\n")
    f.write(f"- Lecture resume/persistence tests: **{progress[0]}**, failures={progress[1]}, errors={progress[2]}\n")
    f.write(f"- Overall lecture-engine evidence: **{'PASS' if ok else 'FAIL'}**\n\n")
    f.write("## Scope\n")
    f.write("This report validates the new lecture-first engine, 90-lesson expanded lecture generation, novice-start assumptions, section structure, glossary coverage, worked examples, line-by-line code explanations, common mistakes, real-world links, readiness gating data, and section resume persistence. Android UI gating is verified separately by instrumentation tests.\n")

print(open(OUT_FILE, encoding="utf-8").read())
if not ok:
    sys.exit(1)
