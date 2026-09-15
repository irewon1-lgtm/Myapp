#!/usr/bin/env bash
set +e

# First run through Gradle so Android test XML/HTML evidence is produced.
./gradlew connectedDebugAndroidTest --stacktrace
TEST_STATUS=$?

# connectedDebugAndroidTest may uninstall the debug/test packages during cleanup.
# Reinstall the exact APKs and run the same instrumentation once more so the
# per-case JSON can be copied before either package is removed.
APP_APK="app/build/outputs/apk/debug/app-debug.apk"
TEST_APK="$(find app/build/outputs/apk/androidTest -type f -name '*androidTest.apk' | head -n 1)"

adb install -r "$APP_APK" >/tmp/runtime-app-install.log 2>&1
APP_INSTALL_STATUS=$?
adb install -r "$TEST_APK" >/tmp/runtime-test-install.log 2>&1
TEST_INSTALL_STATUS=$?

adb shell am instrument -w -r \
  -e class com.futuretech.poweruser.sandbox.RuntimeExtremeInstrumentedTest \
  com.futuretech.poweruser.test/androidx.test.runner.AndroidJUnitRunner \
  | tee runtime_instrumentation_output.txt
MANUAL_STATUS=${PIPESTATUS[0]}

adb shell run-as com.futuretech.poweruser cat files/runtime_extreme_report.json > runtime_extreme_report.json 2>/dev/null
PULL_STATUS=$?

python tools/runtime_cleanpass.py runtime_extreme_report.json
CLEAN_STATUS=$?

if [ "$TEST_STATUS" -ne 0 ] || [ "$APP_INSTALL_STATUS" -ne 0 ] || [ "$TEST_INSTALL_STATUS" -ne 0 ] || [ "$MANUAL_STATUS" -ne 0 ] || [ "$PULL_STATUS" -ne 0 ] || [ "$CLEAN_STATUS" -ne 0 ]; then
  echo "Runtime validation failed: gradleAndroidTest=$TEST_STATUS appInstall=$APP_INSTALL_STATUS testInstall=$TEST_INSTALL_STATUS manual=$MANUAL_STATUS pull=$PULL_STATUS clean=$CLEAN_STATUS"
  echo "--- app install ---"; cat /tmp/runtime-app-install.log || true
  echo "--- test install ---"; cat /tmp/runtime-test-install.log || true
  exit 1
fi
