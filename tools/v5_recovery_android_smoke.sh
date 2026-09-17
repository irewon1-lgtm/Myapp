#!/usr/bin/env bash
set -euo pipefail

mkdir -p recovery-validation
APK="app/build/outputs/apk/debug/app-debug.apk"

test -s "$APK"
adb install -r "$APK" | tee recovery-validation/adb_install.txt
adb logcat -c
adb shell am force-stop com.futuretech.poweruser
adb shell am start -W -n com.futuretech.poweruser/.MainActivity | tee recovery-validation/am_start.txt
sleep 5

adb shell pidof com.futuretech.poweruser | tee recovery-validation/pid.txt
test -s recovery-validation/pid.txt

adb shell dumpsys activity activities > recovery-validation/dumpsys_activity.txt
grep -q "com.futuretech.poweruser/.MainActivity" recovery-validation/dumpsys_activity.txt

adb shell uiautomator dump /sdcard/window.xml >/dev/null
adb pull /sdcard/window.xml recovery-validation/window.xml >/dev/null

adb logcat -d > recovery-validation/logcat_full.txt
if grep -E "FATAL EXCEPTION|Process: com\.futuretech\.poweruser" recovery-validation/logcat_full.txt; then
  echo "RECOVERY_LAUNCH_CRASH_DETECTED=1" | tee recovery-validation/runtime_gate.txt
  exit 1
fi

echo "RECOVERY_INSTALL_LAUNCH_GATE=PASS" | tee recovery-validation/runtime_gate.txt
