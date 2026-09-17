#!/usr/bin/env bash
set -euo pipefail

mkdir -p alltracks-validation final-dist

./gradlew connectedDebugAndroidTest --stacktrace \
  -Pandroid.testInstrumentationRunnerArguments.class=com.futuretech.poweruser.textbook.AllTracksReaderRoutingInstrumentedTest

APK="app/build/outputs/apk/debug/app-debug.apk"
test -s "$APK"

adb install -r "$APK" | tee alltracks-validation/adb_install.txt
adb logcat -c
adb shell am force-stop com.futuretech.poweruser
adb shell am start -W -n com.futuretech.poweruser/.MainActivity | tee alltracks-validation/am_start.txt
sleep 5
adb shell pidof com.futuretech.poweruser | tee alltracks-validation/pid.txt
test -s alltracks-validation/pid.txt

adb shell dumpsys activity activities > alltracks-validation/dumpsys_activity.txt
grep -q "com.futuretech.poweruser/.MainActivity" alltracks-validation/dumpsys_activity.txt
adb shell uiautomator dump /sdcard/window.xml >/dev/null
adb pull /sdcard/window.xml alltracks-validation/window.xml >/dev/null
adb logcat -d > alltracks-validation/logcat_full.txt

if grep -E "FATAL EXCEPTION|Process: com\.futuretech\.poweruser" alltracks-validation/logcat_full.txt; then
  echo "ALLTRACKS_RUNTIME_GATE=FAIL" | tee alltracks-validation/runtime_gate.txt
  exit 1
fi

cp "$APK" final-dist/Coding_Textbook_Recovery_11TRACKS_20260918.apk
sha256sum final-dist/Coding_Textbook_Recovery_11TRACKS_20260918.apk | tee final-dist/Coding_Textbook_Recovery_11TRACKS_20260918.sha256

BUILD_TOOLS="$(ls -1 "$ANDROID_HOME/build-tools" | sort -V | tail -n 1)"
"$ANDROID_HOME/build-tools/$BUILD_TOOLS/apksigner" verify --verbose --print-certs \
  final-dist/Coding_Textbook_Recovery_11TRACKS_20260918.apk \
  | tee alltracks-validation/apksigner_verify.txt

echo "ALLTRACKS_RUNTIME_GATE=PASS" | tee alltracks-validation/runtime_gate.txt
