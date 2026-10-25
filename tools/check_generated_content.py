#!/usr/bin/env python3
"""Regenerate content in isolation and compare it byte-for-byte with the checked-in data."""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory() as directory:
    replica = Path(directory)
    for relative in ('tools/generate_content.py', 'docs/yasin-user-source.txt',
                     'app/src/main/assets/fonts/ScheherazadeNew-Regular.ttf'):
        target = replica / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(root / relative, target)
    (replica / 'app/src/main/assets/content').mkdir(parents=True)
    (replica / 'app/src/main/assets/timings/036').mkdir(parents=True)
    subprocess.run([sys.executable, str(replica / 'tools/generate_content.py')], check=True)
    relative = Path('app/src/main/assets/content/yasin.json')
    if (replica / relative).read_bytes() != (root / relative).read_bytes():
        raise SystemExit('Generated content differs from committed yasin.json')
print('PASS: content regeneration matches the committed data')
