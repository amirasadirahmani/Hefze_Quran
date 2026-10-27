import json
from pathlib import Path
p=Path(__file__).resolve().parents[1]/'app/src/main/assets/content'
s=json.loads((p/'quran-uthmani.json').read_text())['data']['surahs']
a=[v for surah in s for v in surah['ayahs']]
assert len(s)==114 and len(a)==6236
assert [x['number'] for x in s]==list(range(1,115))
assert [x['number'] for x in a]==list(range(1,6237))
for x in s:
 assert [v['numberInSurah'] for v in x['ayahs']]==list(range(1,len(x['ayahs'])+1))
for field,limit in [('page',604),('juz',30),('hizbQuarter',240)]:
 values=[v[field] for v in a]
 assert values==sorted(values) and set(values)==set(range(1,limit+1))
r=json.loads((p/'reciters.json').read_text());ed=json.loads((p/'audio-editions.json').read_text())['data']
assert {x['id'] for x in r}=={x['identifier'] for x in ed if x['language']=='ar'}
assert all(x['available'] for x in r)
print('PASS: complete Quran metadata and',len(r),'Arabic audio editions')

reference=json.loads((Path(__file__).resolve().parents[1]/'tests/fixtures/page-reference.json').read_text())
actual=[]
for page in range(1,605):
 verses=[v for v in a if v['page']==page]
 actual.append([page,verses[0]['number'],verses[-1]['number'],verses[0]['juz']])
assert actual==reference['pages'], 'Reference page mapping changed'
print('PASS: all 604 page boundaries match the captured official API reference')
