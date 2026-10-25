# Verification report — pistachio / optional audio edition

Date: 2026-10-26
Base commit: b0308eef504af857f2b9d3335bf1ff48a64873e9

## Passed

- Java core tests: **36 assertions** for playback, study presets and word timelines.
- Audio transfer tests: **15 assertions** for completed writes, truncated responses, decoder rejection, cancellation, empty/chunked responses, size limits and preservation of an existing file on failure.
- Python content/catalog tests: **7 passed**.
- Content validation: **0 structural errors**. The 166 pending reference-package items are 83 MP3s (now optional runtime downloads) and 83 unreviewed word timelines.
- Reproducible generation: committed content matches the pinned-Pillow generator. All 83 original verse texts and all timing templates remain unchanged.
- Layout geometry: **108 lines / 10 app pages**, maximum 11 lines per page; maximum measured width **659.78 / 660**, maximum ink height **88 / 96** reference pixels.
- Scheherazade New 4.500 covers all characters in the supplied Quran text. The uploaded legacy font did not cover U+0617, U+0618 or U+061A.
- Android API 35 compilation, resource linking, debug APK assembly and Android Lint completed successfully locally using JDK 17. Lint reports **0 errors / 8 warnings** (portrait orientation, existing view constructors/backup configuration and programmatic text/gravity).
- Packaging probe: a temporary `.mp3` was placed inside the local assets directory during the build. The resulting APK contains **no MP3 or partial files** and includes the text, catalog, font and artwork. The temporary fixture was removed afterward. Final local APK: **2,545,314 bytes**.
- The first EveryAyah Yasin file was downloaded over HTTPS and inspected with ffprobe: 38,033 bytes, 4.728163 seconds. This is a source/file check, not Android playback validation.
- The design proof in `docs/previews/pistachio-preview.png` was rendered from the actual bundled text, font and background. It is explicitly labeled as a design proof, not a running Android screenshot.
- `git diff --check` passed.

## Still requires device testing / editorial work

- Actual Android rendering, taps, accessibility, download interruptions and playback/audio-focus behavior on a phone or emulator. No device test is claimed.
- Word-by-word highlighting requires reviewed timing JSON tied by SHA-256 to the downloaded recordings. Downloading audio alone does not supply timings.
- Only Yasin has a reading/study pack. The remaining 113 catalog entries are visibly unavailable.
- Quran editorial review and comparison with a chosen reference mushaf. The 10 pages are app-specific, not official printed page numbers.
- Browser interaction of the optional HTML preview was not tested: Chromium launch was blocked by the execution environment. The static design proof was rendered with WeasyPrint/PyMuPDF instead.

The GitHub workflow independently runs the core/content/layout checks, builds and lints the APK, verifies that it contains no recordings, and uploads `hefze-yasin-debug`. See the workflow run for the corresponding commit for its status.
