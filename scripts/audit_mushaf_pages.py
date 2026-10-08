#!/usr/bin/env python3
"""Generate visual contact sheets to audit the real PDF-to-Mushaf page map.

Outputs GitHub Actions log lines with base64 JPEG for visual review.
Never changes Quran text or numbered assets.
"""
import base64
from io import BytesIO
import sys
from pathlib import Path
import fitz
from PIL import Image, ImageDraw, ImageFont

pdf=Path(sys.argv[1]) if len(sys.argv)>1 else Path("mushaf-source.pdf")
with fitz.open(pdf) as doc:
    if len(doc)!=640: raise RuntimeError(f"Expected 640 PDF pages, got {len(doc)}")
    sheets={
        "front":list(range(1,25)),
        "end":[580,590,597,600,601,602,603,604,605,606,607,608,609,610,611,612,613,614,615,616,617,620,630,640],
        "middle":[25,26,27,28,29,30,31,32,80,81,82,83,300,301,302,303,400,401,402,403,500,501,502,503]
    }
    font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",17)
    for name,numbers in sheets.items():
        width,height=220,312
        mosaic=Image.new("RGB",(6*width,4*(height+27)),(230,232,232))
        painter=ImageDraw.Draw(mosaic)
        for idx,number in enumerate(numbers):
            page=doc[number-1]
            pix=page.get_pixmap(matrix=fitz.Matrix(.42,.42),colorspace=fitz.csRGB,alpha=False)
            img=Image.frombytes("RGB",(pix.width,pix.height),pix.samples)
            img.thumbnail((width-8,height-8),Image.Resampling.LANCZOS)
            x=(idx%6)*width+(width-img.width)//2
            y=(idx//6)*(height+27)+22+(height-img.height)//2
            mosaic.paste(img,(x,y))
            painter.text(((idx%6)*width+12,(idx//6)*(height+27)+3),f"PDF {number}",font=font,fill=(16,44,65))
        buf=BytesIO()
        mosaic.save(buf,format="JPEG",quality=58,optimize=True)
        print(f"CONTACT_SHEET_BEGIN_{name.upper()}:"+base64.b64encode(buf.getvalue()).decode("ascii")+f":CONTACT_SHEET_END_{name.upper()}",flush=True)
    print("PDF contact sheets generated",flush=True)
