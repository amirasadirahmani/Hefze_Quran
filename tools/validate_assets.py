#!/usr/bin/env python3
"""Validate shipped text/layout; --release additionally requires real, reviewed audio alignment."""
import argparse, hashlib, json, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def validate(root=ROOT, release=False):
    assets=root/'app/src/main/assets'; errors=[]; pending=[]
    content=json.loads((assets/'content/yasin.json').read_text())
    if content['surah']!=36 or [v['number'] for v in content['verses']]!=list(range(1,84)):errors.append('Expected surah 36 with 83 ordered verses')
    catalog=json.loads((assets/'content/surahs.json').read_text())['surahs']
    if [s['number'] for s in catalog]!=list(range(1,115)) or sum(s['ayahs'] for s in catalog)!=6236:errors.append('Invalid surah catalog')
    if any(not 1<=s['firstJuz']<=s['lastJuz']<=30 for s in catalog):errors.append('Invalid juz range')
    if [s['number'] for s in catalog if s['available']]!=[36]:errors.append('Only Yasin has a learning pack')
    if (catalog[35]['firstJuz'],catalog[35]['lastJuz'])!=(22,23):errors.append('Yasin must span juz 22 and 23')
    tokens=[]; markers=[]
    for number,page in enumerate(content['pages'],1):
        if page['number']!=number or not 1<=len(page['lines'])<=content['layout']['linesPerPage']:errors.append(f'Invalid page {number}')
        for line in page['lines']:
            if not line:errors.append(f'Empty line on page {number}')
            for t in line:
                if t['word']==-1:markers.append(t['ayah'])
                else:tokens.append((t['ayah'],t['word'],t['text']))
    expected=[(v['number'],i,w) for v in content['verses'] for i,w in enumerate(v['words'])]
    if tokens!=expected:errors.append('Page words are missing, duplicated, reordered or changed')
    if markers!=list(range(1,84)):errors.append('Verse markers do not cover the surah once')
    for v in content['verses']:
        n=v['number']; base=f'036-{n:03d}'
        if v['audio']!=f'audio/036/{base}.mp3' or v['timing']!=f'timings/036/{base}.json':errors.append(f'{n}: invalid asset paths')
        if v['text']!=' '.join(v['words']):errors.append(f'{n}: text/token mismatch')
        audio=assets/v['audio']; timing=assets/v['timing']
        first_page=next(p['number'] for p in content['pages'] if any(t['ayah']==n for l in p['lines'] for t in l))
        if v['page']!=first_page:errors.append(f'{n}: wrong first page')
        if not audio.is_file() or audio.stat().st_size==0:pending.append(f'{n}: missing audio')
        if not timing.is_file():pending.append(f'{n}: missing timing');continue
        try:
            data=json.loads(timing.read_text())
            if data['surah']!=36 or data['ayah']!=n or data['schemaVersion']!=1:raise ValueError('Wrong timeline identity')
            words=data['words']
            if [(w['index'],w['text']) for w in words]!=list(enumerate(v['words'])):raise ValueError('Timing words differ from display')
            if not data.get('reviewed'):
                pending.append(f'{n}: alignment not reviewed');continue
            if not isinstance(data['durationMs'],int) or data['durationMs']<=0:raise ValueError('Bad duration')
            end=0
            for w in words:
                a,b=w['startMs'],w['endMs']
                if type(a) is not int or type(b) is not int or a<end or b<=a or b>data['durationMs']:raise ValueError('Invalid/overlapping word boundaries')
                end=b
            if not audio.is_file():raise ValueError('Reviewed timeline has no audio')
            if data['audioSha256']!=hashlib.sha256(audio.read_bytes()).hexdigest():raise ValueError('Timing belongs to a different audio file')
        except (KeyError,ValueError,TypeError) as e:errors.append(f'{n}: {e}')
    if release:errors+=pending
    return errors,pending

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--release',action='store_true');args=parser.parse_args()
    errors,pending=validate(release=args.release)
    print(f'{len(errors)} errors; {len(pending)} pending asset items')
    for error in errors:print('ERROR:',error)
    if pending and not args.release:print('Content is valid. Missing MP3 files are optional runtime downloads; unreviewed word timings remain pending.')
    sys.exit(bool(errors))
