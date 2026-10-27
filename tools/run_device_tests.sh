#!/usr/bin/env bash
set -uo pipefail
bash gradlew --no-daemon :app:connectedDebugAndroidTest
status=$?
mkdir -p app/build/ui-shots
adb pull /sdcard/Android/data/ir.hefze.quran.review07/files/ui-shots/. app/build/ui-shots/ || true
exit "$status"
