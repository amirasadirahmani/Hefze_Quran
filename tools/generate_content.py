#!/usr/bin/env python3
"""Build deterministic app pages; never synthesize audio alignment."""
import hashlib, json, re
from pathlib import Path
from PIL import ImageFont
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
font = ImageFont.truetype(str(ASSETS / 'fonts/ScheherazadeNew-Regular.ttf'), 44)
source = (ROOT / 'docs/yasin-user-source.txt').read_text()
verses, tokens = [], []
for text, number in re.findall(r'(.+?)\s*\((\d+)\)', source):
    n = int(number)
    # Keep all vocalization. Remove two copy decorations, explicitly documented.
    text = ' '.join(text.replace('× ', '').replace(' ـۚ ', ' ').split())
    words = text.split()
    verses.append(dict(number=n, text=text, words=words, translation=None, topics=[],
                       audio=f'audio/036/036-{n:03d}.mp3', timing=f'timings/036/036-{n:03d}.json'))
    tokens.extend(dict(ayah=n, word=i, text=w) for i,w in enumerate(words))
    tokens.append(dict(ayah=n, word=-1, text='﴿'+str(n).translate(str.maketrans('0123456789','٠١٢٣٤٥٦٧٨٩'))+'﴾'))
assert [v['number'] for v in verses] == list(range(1,84))
def line_width(items):
    width = font.getlength(' '.join(t['text'] for t in items), direction='rtl', language='ar')
    # Android draws the end-of-ayah medallion as a fixed-width ReplacementSpan.
    return width + sum(46 - font.getlength(t['text'], direction='rtl', language='ar') for t in items if t['word'] == -1)

lines, line = [], []
for token in tokens:
    candidate = ' '.join(t['text'] for t in line + [token])
    if line and line_width(line + [token]) > 660:
        lines.append(line); line = []
    line.append(token)
if line: lines.append(line)
pages = [dict(number=i//11+1, lines=lines[i:i+11]) for i in range(0,len(lines),11)]
for verse in verses:
    verse['page'] = next(p['number'] for p in pages if any(t['ayah']==verse['number'] for l in p['lines'] for t in l))
content = dict(schemaVersion=1, surah=36, name='يس', basmala='بِسمِ اللهِ الرَّحمٰنِ الرَّحيمِ',
    reciter='Mishary Rashid Alafasy (EveryAyah, 64 kbps)', source='user-supplied; pending editorial verification',
    layout=dict(id='yasin-app-v2-scheherazade', width=760, height=1072, fontSize=44, lineHeight=96, linesPerPage=11, font='fonts/ScheherazadeNew-Regular.ttf'),
    verses=verses, pages=pages)
(ASSETS/'content/yasin.json').write_text(json.dumps(content,ensure_ascii=False,indent=2)+'\n')
for v in verses:
    path=ASSETS/v['timing']
    if not path.exists():
        path.write_text(json.dumps(dict(schemaVersion=1,surah=36,ayah=v['number'],reviewed=False,
            audioSha256=None, durationMs=None, words=[dict(index=i,text=w,startMs=None,endMs=None) for i,w in enumerate(v['words'])]),ensure_ascii=False,indent=2)+'\n')
print(f'{len(verses)} verses, {len(lines)} fixed lines, {len(pages)} app pages; timings remain unaligned.')
