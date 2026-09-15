#!/usr/bin/env bash
set -euo pipefail

APK="final-dist/FutureTechPowerUser_v1.0.0_FINAL.apk"
PACKAGE="com.futuretech.poweruser"
ACTIVITY="com.futuretech.poweruser/.MainActivity"

mkdir -p final-dist
adb logcat -c || true
adb install -r "$APK"
adb shell am force-stop "$PACKAGE"
adb shell am start -W -n "$ACTIVITY" | tee final-dist/launch_smoke.txt
sleep 3

PID="$(adb shell pidof "$PACKAGE" | tr -d '\r' || true)"
if [[ -z "$PID" ]]; then
  echo "PROCESS_EXITED_AFTER_LAUNCH" | tee -a final-dist/launch_smoke.txt
  adb logcat -d -v threadtime > final-dist/logcat_full.txt || true
  grep -E -i 'FATAL EXCEPTION|AndroidRuntime|futuretech|Process: com.futuretech.poweruser|Exception|Error' final-dist/logcat_full.txt | tail -n 250 > final-dist/logcat_crash_excerpt.txt || true
  cat final-dist/logcat_crash_excerpt.txt || true
  exit 1
fi

echo "PID=$PID" | tee -a final-dist/launch_smoke.txt
adb shell dumpsys package "$PACKAGE" | grep -E 'versionCode|versionName' | head -n 4 | tee -a final-dist/launch_smoke.txt
adb logcat -d -v threadtime > final-dist/logcat_full.txt || true

echo "FINAL_RELEASE_INSTALL_LAUNCH_PASS" | tee -a final-dist/launch_smoke.txt
