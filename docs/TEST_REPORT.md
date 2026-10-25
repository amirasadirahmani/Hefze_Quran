# Verification report

Date: 2026-10-25

## Executed successfully

- Java 17 compilation and execution of the platform-independent playback queue, study-method presets, and word timeline: **36 assertions passed**.
- Python content tests: **5 passed**. Includes missing-asset release rejection, corrupted page text, unreviewed/fake timing metadata and stale audio hashes.
- Development package validation: **0 structural errors**, **166 pending items** (83 audio files and 83 reviewed alignments).
- Java compiler parser: all **10 Android/core Java source files** parsed without syntax diagnostics. This is a parsing check, not Android compilation/type checking.
- Android manifest, theme and icon XML parsed successfully. Resource linking was not run.
- Alignment editor JavaScript passed `node --check`. Browser interaction is not tested.
- All Quran text code points (except the intentionally non-rendering ZWJ) are covered by the bundled Amiri font.
- All 83 verse markers and every word appear exactly once in page order. Six pages / 84 fixed lines. Maximum measured line width: 647.14 reference pixels, within the 680-pixel Android text column.
- All six page layouts visually inspected using Pillow with the bundled font. This does not verify Android shaping or constitute a device screenshot.

## Blocked / not executed

- Local `./gradlew --version` could not download Gradle 8.11.1: **Network is unreachable**; Android SDK is absent locally. GitHub Actions run 40203849671 successfully compiled the Android app, assembled the debug APK, passed lint and uploaded `hefze-yasin-debug`. Installation and emulator/device checks remain unexecuted.
- Playwright browser interaction could not launch: Chromium executable is not installed. No browser interaction test is reported as passed.
- Real audio decode, lifecycle behavior on Android, headset/audio focus, notifications and word synchronization await device testing and actual reciter audio.
- Full release validation is intentionally failing until all audio and reviewed timing files are supplied.
- GitHub Actions run 40203849671 passed, with downloadable APK artifact 12602404623.

The initial push and workflow run are tracked in GitHub Actions.
