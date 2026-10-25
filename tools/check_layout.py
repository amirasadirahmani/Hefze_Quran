#!/usr/bin/env python3
"""Check reference-page geometry with the exact bundled font (not a device rendering test)."""
import json
from pathlib import Path
from PIL import ImageFont
root=Path(__file__).resolve().parents[1]
assets=root/'app/src/main/assets'
c=json.loads((assets/'content/yasin.json').read_text());layout=c['layout']
f=ImageFont.truetype(str(assets/layout['font']),layout['fontSize'])
count=0;max_width=0;max_height=0
for page in c['pages']:
    assert len(page['lines'])<=layout['linesPerPage']
    for line in page['lines']:
        text=' '.join(t['text'] for t in line)
        width=f.getlength(text,direction='rtl',language='ar')+sum(46-f.getlength(t['text'],direction='rtl',language='ar') for t in line if t['word']==-1)
        box=f.getbbox(text,direction='rtl',language='ar');height=box[3]-box[1]
        assert width<=660, (page['number'],width)
        assert height<layout['lineHeight'], (page['number'],height)
        max_width=max(max_width,width);max_height=max(max_height,height);count+=1
assert layout['height']>=layout['linesPerPage']*layout['lineHeight']+16
print(f'PASS: {count} lines; max reference width {max_width:.2f}/660; max ink height {max_height}/{layout["lineHeight"]}')
