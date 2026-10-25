#!/usr/bin/env python3
"""Confirm the lightweight APK contains the reading assets but no bundled recordings."""
import sys,zipfile
from pathlib import Path
path=Path(sys.argv[1]) if len(sys.argv)>1 else Path(__file__).resolve().parents[1]/'app/build/outputs/apk/debug/app-debug.apk'
with zipfile.ZipFile(path) as archive:
    names=archive.namelist()
    audio=[n for n in names if n.lower().endswith(('.mp3','.part'))]
    if audio:raise SystemExit('Unexpected bundled recordings: '+', '.join(audio))
    for required in ('assets/content/yasin.json','assets/content/surahs.json','assets/fonts/ScheherazadeNew-Regular.ttf'):
        if required not in names:raise SystemExit('Missing APK reading asset: '+required)
    if not any('mushaf_pistachio' in n for n in names):raise SystemExit('Missing pistachio artwork')
print(f'PASS: APK {path.stat().st_size:,} bytes; reading assets present; no MP3/partial files')
