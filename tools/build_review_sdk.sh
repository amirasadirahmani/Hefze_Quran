#!/usr/bin/env bash
# Offline SDK fallback for this review. CI uses Gradle.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?Set ANDROID_HOME}"
: "${JAVA_HOME:?Set JAVA_HOME}"
export PATH="$JAVA_HOME/bin:$PATH"
bt="$ANDROID_HOME/build-tools/35.0.0"
work=app/build/sdk-review
mkdir -p "$work/gen" "$work/classes" "$work/dex" app/build/outputs/apk/debug
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/AndroidManifest.xml').read_text().replace('<manifest ','<manifest package="ir.hefze.quran.review07" ',1).replace('android:name=".','android:name="ir.hefze.quran.').replace('<application ','<application android:debuggable="true" ',1)
Path('app/build/sdk-review/AndroidManifest.xml').write_text(s)
PY
"$bt/aapt2" compile --dir app/src/main/res -o "$work/res.zip"
"$bt/aapt2" link -o "$work/resources.apk" --manifest "$work/AndroidManifest.xml" -I "$ANDROID_HOME/platforms/android-35/android.jar" --java "$work/gen" --custom-package ir.hefze.quran --min-sdk-version 26 --target-sdk-version 35 --version-code 7 --version-name 0.7.0 -A app/src/main/assets "$work/res.zip"
find app/src/main/java "$work/gen" -name '*.java' > "$work/sources.txt"
javac --release 17 -cp "$ANDROID_HOME/platforms/android-35/android.jar" -d "$work/classes" @"$work/sources.txt"
find "$work/classes" -name '*.class' > "$work/classes.txt"
"$bt/d8" --min-api 26 --lib "$ANDROID_HOME/platforms/android-35/android.jar" --output "$work/dex" @"$work/classes.txt"
python3 - <<'PY'
from pathlib import Path
import zipfile,shutil
r=Path('app/build/sdk-review');shutil.copyfile(r/'resources.apk',r/'unsigned.apk')
with zipfile.ZipFile(r/'unsigned.apk','a',compression=zipfile.ZIP_DEFLATED) as z:z.write(r/'dex/classes.dex','classes.dex')
PY
"$bt/zipalign" -f 4 "$work/unsigned.apk" "$work/aligned.apk"
if [ ! -f "$work/review.keystore" ]; then keytool -genkeypair -keystore "$work/review.keystore" -storepass android -keypass android -alias androiddebugkey -dname 'CN=Review Debug' -keyalg RSA -validity 3650; fi
"$bt/apksigner" sign --ks "$work/review.keystore" --ks-pass pass:android --out app/build/outputs/apk/debug/app-debug.apk "$work/aligned.apk"
"$bt/apksigner" verify app/build/outputs/apk/debug/app-debug.apk
python3 tools/check_apk.py
