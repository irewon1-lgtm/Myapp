#!/usr/bin/env bash
set -euo pipefail

APK="app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.futuretech.poweruser"
ACTIVITY="com.futuretech.poweruser/.MainActivity"

mkdir -p lecture_evidence
adb install -r "$APK"
adb shell pm clear "$PACKAGE" || true
adb logcat -c || true
adb shell am force-stop "$PACKAGE"
adb shell am start -W -n "$ACTIVITY" | tee lecture_evidence/app_launch.txt
sleep 3

PID="$(adb shell pidof "$PACKAGE" | tr -d '\r' || true)"
if [[ -z "$PID" ]]; then
  echo "PROCESS_EXITED_AFTER_LAUNCH" | tee -a lecture_evidence/app_launch.txt
  adb logcat -d -v threadtime > lecture_evidence/logcat_full.txt || true
  grep -E -i 'FATAL EXCEPTION|AndroidRuntime|futuretech|Process: com.futuretech.poweruser|Exception|Error' lecture_evidence/logcat_full.txt | tail -n 250 > lecture_evidence/logcat_crash_excerpt.txt || true
  cat lecture_evidence/logcat_crash_excerpt.txt || true
  exit 1
fi

echo "PID=$PID" | tee -a lecture_evidence/app_launch.txt
adb shell dumpsys package "$PACKAGE" | grep -E 'versionCode|versionName' | head -n 4 | tee -a lecture_evidence/app_launch.txt
adb logcat -d -v threadtime > lecture_evidence/logcat_full.txt || true
echo "LECTURE_ENGINE_INSTALL_LAUNCH_PASS" | tee -a lecture_evidence/app_launch.txt
