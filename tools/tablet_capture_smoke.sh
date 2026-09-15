#!/usr/bin/env bash
set -euo pipefail

EVIDENCE_DIR="${1:-textbook_evidence}"
APP_ID="com.futuretech.poweruser"
TEST_ID="com.futuretech.poweruser.test"
RUNNER="$TEST_ID/androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS="com.futuretech.poweruser.textbook.V1TextbookTabletUiInstrumentedTest"
APP_APK="app/build/outputs/apk/debug/app-debug.apk"
TEST_APK="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
INSTRUMENTATION_OUT="$EVIDENCE_DIR/tablet_instrumentation.txt"
SCREENSHOT="$EVIDENCE_DIR/v1_textbook_tablet.png"
LOGCAT_OUT="$EVIDENCE_DIR/tablet_capture_logcat.txt"
REMOTE_META="$EVIDENCE_DIR/tablet_device_metrics.txt"

mkdir -p "$EVIDENCE_DIR"
test -s "$APP_APK"
test -s "$TEST_APK"

INSTR_PID=""
cleanup() {
  set +e
  if [[ -n "$INSTR_PID" ]] && kill -0 "$INSTR_PID" 2>/dev/null; then
    kill "$INSTR_PID" 2>/dev/null
    wait "$INSTR_PID" 2>/dev/null
  fi
  adb shell wm size reset >/dev/null 2>&1
  adb shell wm density reset >/dev/null 2>&1
  adb shell settings put system accelerometer_rotation 1 >/dev/null 2>&1
  adb uninstall "$TEST_ID" >/dev/null 2>&1
  adb uninstall "$APP_ID" >/dev/null 2>&1
  set -e
}
trap cleanup EXIT

adb uninstall "$TEST_ID" >/dev/null 2>&1 || true
adb uninstall "$APP_ID" >/dev/null 2>&1 || true
adb install -r "$APP_APK"
adb install -r "$TEST_APK"
adb shell pm list instrumentation | tee "$EVIDENCE_DIR/instrumentation_list.txt"
grep -q "$RUNNER" "$EVIDENCE_DIR/instrumentation_list.txt"

adb logcat -c
adb shell wm size 2560x1600
adb shell wm density 320
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb shell wm size > "$REMOTE_META"
adb shell wm density >> "$REMOTE_META"
adb shell am force-stop "$APP_ID" || true

adb shell am instrument -w -r -e class "$TEST_CLASS" "$RUNNER" > "$INSTRUMENTATION_OUT" 2>&1 &
INSTR_PID=$!

ready=0
for _ in $(seq 1 30); do
  if adb logcat -d -s V1TabletEvidence:I '*:S' | grep -q 'READY_FOR_SCREENSHOT'; then
    ready=1
    break
  fi
  if ! kill -0 "$INSTR_PID" 2>/dev/null; then
    break
  fi
  sleep 1
done

adb logcat -d -s V1TabletEvidence:I '*:S' > "$LOGCAT_OUT" || true
if [[ "$ready" -ne 1 ]]; then
  echo "TABLET_READY_MARKER_MISSING" >&2
  cat "$INSTRUMENTATION_OUT" >&2 || true
  cat "$LOGCAT_OUT" >&2 || true
  exit 1
fi

# Capture from the host adb shell while the verified Activity is deliberately held open.
adb exec-out screencap -p > "$SCREENSHOT"
test -s "$SCREENSHOT"
SCREENSHOT_BYTES="$(wc -c < "$SCREENSHOT")"
if [[ "$SCREENSHOT_BYTES" -le 10000 ]]; then
  echo "TABLET_SCREENSHOT_TOO_SMALL=$SCREENSHOT_BYTES" >&2
  exit 1
fi
printf 'TABLET_SCREENSHOT_BYTES=%s\n' "$SCREENSHOT_BYTES" | tee "$EVIDENCE_DIR/tablet_screenshot_size.txt"
sha256sum "$SCREENSHOT" | tee "$EVIDENCE_DIR/tablet_screenshot.sha256"

set +e
wait "$INSTR_PID"
INSTR_STATUS=$?
set -e
INSTR_PID=""
cat "$INSTRUMENTATION_OUT"
if [[ "$INSTR_STATUS" -ne 0 ]]; then
  echo "TABLET_INSTRUMENTATION_EXIT=$INSTR_STATUS" >&2
  exit "$INSTR_STATUS"
fi
grep -q 'OK (1 test)' "$INSTRUMENTATION_OUT"
grep -q 'INSTRUMENTATION_CODE: -1' "$INSTRUMENTATION_OUT"
grep -q 'READY_FOR_SCREENSHOT' "$LOGCAT_OUT"

echo "GALAXY_TAB_3COLUMN_SCREENSHOT_PASS"
trap - EXIT
cleanup
