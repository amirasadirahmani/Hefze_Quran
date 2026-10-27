#!/usr/bin/env bash
set -uo pipefail
bash gradlew --no-daemon :app:connectedDebugAndroidTest
status=$?
mkdir -p app/build/ui-shots
for shot in $(adb shell 'ls /sdcard/Download/hefze-ui-*.png' | tr -d '\r'); do
    adb pull "$shot" app/build/ui-shots/ || true
done
exit "$status"
