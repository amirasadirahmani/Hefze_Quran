#!/usr/bin/env python3
"""Render a design proof from the bundled artwork, text and font; not an Android screenshot.
Optional tools: pip install weasyprint pymupdf Pillow
"""
from pathlib import Path
import html,json
from PIL import ImageFont
from weasyprint import HTML
import fitz
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
content=json.loads((ASSETS/'content/yasin.json').read_text())
catalog=json.loads((ASSETS/'content/surahs.json').read_text())['surahs']
font_path=ASSETS/'fonts/ScheherazadeNew-Regular.ttf'
font=ImageFont.truetype(str(font_path),44)
fa=lambda n:str(n).translate(str.maketrans('0123456789','۰۱۲۳۴۵۶۷۸۹'))
esc=html.escape
art=(ROOT/'app/src/main/res/drawable-nodpi/mushaf_pistachio.jpg').as_uri()
css=f'''@font-face{{font-family:Q;src:url("{font_path.as_uri()}")}}@page{{size:1320px 935px;margin:0}}*{{box-sizing:border-box}}body{{margin:0;background:#e7eadc;font-family:DejaVu Sans,sans-serif;color:#263521}}.note{{position:absolute;top:20px;left:0;width:1320px;text-align:center;font-size:12px;direction:rtl}}.phone{{position:absolute;top:65px;width:400px;height:844px;background:#faf8eb;overflow:hidden;direction:rtl}}.text{{position:absolute;text-align:center;direction:rtl;white-space:nowrap}}.q{{font-family:Q}}.button{{position:absolute;height:42px;background:#e8efd6;color:#526c39;border-radius:4px;text-align:center;line-height:42px;font-size:12px;direction:rtl}}.primary{{background:#607b43;color:#fff}}.word{{position:absolute;white-space:nowrap;direction:rtl;font-family:Q;font-size:21.42px;line-height:46.74px;text-align:right}}.marker{{position:absolute;width:17px;height:24px;border:1px solid #70884d;border-radius:50%;font:13px/23px Q;text-align:center;color:#607b43}}'''

def label(t,x,y,w,size=12,q=False):
 return f'<div class="text {"q" if q else ""}" style="left:{x}px;top:{y}px;width:{w}px;font-size:{size}px">{esc(t)}</div>'
def button(t,x,y,w,primary=False,ghost=False):
 return f'<div class="button {"primary" if primary else ""}" style="left:{x}px;top:{y}px;width:{w}px;{"background:transparent;" if ghost else ""}">{esc(t)}</div>'
def frame(top,bottom):
 result=[]
 scale=400/592
 top_cap=min(60*scale,top*.4);top_tail=min(50*scale,top*.35)
 ornament=min(bottom,159*scale)
 for sy,sh,dy,dh in ((0,60,0,top_cap),(60,30,top_cap,top-top_cap-top_tail),(90,50,top-top_tail,top_tail),(140,725,top,844-top-ornament),(865,159,844-ornament,ornament)):
  result.append(f'<div style="position:absolute;left:0;top:{dy}px;width:400px;height:{dh}px;overflow:hidden"><img src="{art}" style="position:absolute;left:0;top:{-sy*dh/sh}px;width:400px;height:{1024*dh/sh}px"></div>')
 return ''.join(result)
def reader(page_number):
 first=page_number==1;current=content['pages'][page_number-1]['lines'][0][0]['ayah']
 result=frame(100,182)
 result+=label(content['basmala'] if first else 'سورة يس',67,18,266,22,True)
 result+=label('صفحهٔ '+fa(page_number)+' از '+fa(len(content['pages'])),117,60,166,12)
 result+=button('فهرست',205,106,160)+button('بیشتر',35,106,160)
 result+='<div style="position:absolute;left:16px;top:152px;width:368px;height:510px;overflow:hidden">'
 scale=370/760
 for li,line in enumerate(content['pages'][page_number-1]['lines']):
  widths=[46 if t['word']<0 else font.getlength(t['text'],direction='rtl',language='ar') for t in line]
  gap=(660-sum(widths))/(len(line)-1) if len(line)>1 else 0
  x=730*scale; y=li*96*scale
  for token,width in zip(line,widths):
   x-=width*scale
   if token['word']<0:
    result+=f'<div class="marker" style="left:{x+3}px;top:{y+12}px">{fa(token["ayah"])}</div>'
   else:
    bg='background:#edf2dc;' if token['ayah']==current else ''
    result+=f'<div class="word" style="left:{x}px;top:{y}px;width:{width*scale+.5}px;{bg}">{esc(token["text"])}</div>'
   x-=gap*scale
 result+='</div>'
 for x,title in ((34,'صفحهٔ بعد'),(147,'آزمون حفظ'),(260,'صفحهٔ قبل')):result+=button(title,x,670,106)
 result+=label('آیهٔ '+fa(current)+' • تکرار ۱ از ۳ • محدودهٔ ۱ تا ۸۳',34,705,332,10)+label('برای شنیدن این آیه، صوت آن را دریافت کنید.',30,727,340,10)
 result+='<div style="position:absolute;left:34px;top:750px;width:332px;height:3px;background:#dce5c8"></div>'
 for x,title,w in ((67,'بعد',74),(148,'دریافت صوت',104),(259,'قبل',74)):result+=button(title,x,771,w,title=='دریافت صوت',title!='دریافت صوت')
 return result
index=frame(100,128)+label('قرآن کریم',34,18,332,30,True)
for x,title in ((34,'تنظیمات'),(147,'پیشرفت'),(260,'درباره')):index+=button(title,x,106,106)
index+='<div style="position:absolute;left:108px;top:157px;width:266px;height:42px;background:#fffdf3;border:1px solid #c2d29d"></div>'+label('جستجوی نام یا شمارهٔ سوره',112,170,258,11)
for i in range(11):index+=button('همه' if i==0 else 'جزء '+fa(i),26,157+i*49,64)
for i,surah in enumerate(catalog[:7]):
 y=205+i*73;name=surah['name'].replace('سُورَةُ ','')
 index+=f'<div style="position:absolute;left:108px;top:{y+69}px;width:266px;height:1px;background:#d1ddba"></div>'
 index+=f'<div class="text q" style="left:114px;top:{y+2}px;width:252px;text-align:right;font-size:25px">{esc(name)}</div>'
 index+=f'<div class="text" style="left:114px;top:{y+48}px;width:252px;text-align:right;font-size:9px">{fa(surah["number"])} • {fa(surah["ayahs"])} آیه • هنوز افزوده نشده</div>'
index+=label('بستهٔ فعال: یاسین • ۸۳ آیه',34,746,332,11)+button('ادامهٔ مطالعهٔ یاسین',88,779,224,True)
doc=f'<html><meta charset="utf-8"><style>{css}</style><body><div class="note">پیش‌نمایش قاب ارسالی، متن و فونت پروژه — تصویر اجرای اندروید نیست • فهرست / آغاز سوره / ادامهٔ سوره</div><div class="phone" style="left:20px">{index}</div><div class="phone" style="left:460px">{reader(1)}</div><div class="phone" style="left:900px">{reader(2)}</div></body></html>'
output=ROOT/'docs/previews';output.mkdir(parents=True,exist_ok=True)
pdf=HTML(string=doc).write_pdf();render=fitz.open(stream=pdf,filetype='pdf');assert len(render)==1
image_bytes=render[0].get_pixmap(matrix=fitz.Matrix(2,2)).tobytes('png')
assert image_bytes.startswith(b'\x89PNG\r\n\x1a\n') and len(image_bytes)>10000
temporary=output/'pistachio-preview.tmp'
temporary.write_bytes(image_bytes)
temporary.replace(output/'pistachio-preview.png')
print('Rendered design proof:',output/'pistachio-preview.png')
