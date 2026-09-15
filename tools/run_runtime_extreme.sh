#!/usr/bin/env bash
set +e

./gradlew connectedDebugAndroidTest --stacktrace
TEST_STATUS=$?

adb shell run-as com.futuretech.poweruser cat files/runtime_extreme_report.json > runtime_extreme_report.json 2>/dev/null
PULL_STATUS=$?

python tools/runtime_cleanpass.py runtime_extreme_report.json
CLEAN_STATUS=$?

if [ "$TEST_STATUS" -ne 0 ] || [ "$PULL_STATUS" -ne 0 ] || [ "$CLEAN_STATUS" -ne 0 ]; then
  echo "Runtime validation failed: androidTest=$TEST_STATUS pull=$PULL_STATUS clean=$CLEAN_STATUS"
  exit 1
fi
