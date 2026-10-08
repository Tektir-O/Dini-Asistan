"""Conservative horizontal centering of the decorative Mushaf page frame.

The original PDF is a book-layout scan with different inside/outside margins
on alternating right/left pages. Align the *printed frame*, not the text or
margin ornament; preserve all non-white source pixels without clipping.
Never re-order pages or alter calligraphy.
"""
from __future__ import annotations
from PIL import Image, ImageChops

def normalize_page(source: Image.Image) -> tuple[Image.Image,int]:
    img=source.convert("RGB")
    original_w, original_h = img.size
    if original_w < 50 or original_h < 50:
        return img, 0
    sample=img.copy()
    sample.thumbnail((240,340),Image.Resampling.BILINEAR)
    w,h=sample.size
    px=sample.load()

    # Find long green/teal decorative columns at the two sides of the
    # Quran's central ornamental text frame (exclude short margin badges).
    # No use of OCR, guessing printed numbers, or cropping any verses.
    ymin,ymax=int(h*.15),int(h*.84)
    sample_step = 3
    def frame_candidates(start,end):
        scored=[]
        for x in range(int(start*w),int(end*w)):
            score=0
            for y in range(ymin,ymax,sample_step):
                red,green,blue=px[x,y]
                if green > red+10 and green > blue+6 and 45 < green < 240:
                    score+=1
            scored.append((score,x))
        return max(scored) if scored else (0,0)
    left_score,left_x=frame_candidates(.09,.45)
    right_score,right_x=frame_candidates(.55,.91)
    required=max(5,(ymax-ymin)//sample_step//10)
    if left_score<required or right_score<required:
        return img,0
    if not .29*w <= (right_x-left_x) <= .83*w:
        return img,0

    center=(left_x+right_x)*.5
    proposed=round((w*.5-center)*original_w/w)
    max_shift=int(original_w*.07)
    proposed=max(-max_shift,min(max_shift,proposed))
    if abs(proposed)<2: return img,0

    # Refuse clipping any visible content, including the Arabic text, page
    # number and outside ornaments. Limit shift to the truly white margins.
    difference=ImageChops.difference(sample,Image.new("RGB",sample.size,"white"))
    mask=difference.convert("L").point(lambda p: 255 if p>37 else 0)
    bbox=mask.getbbox()
    if bbox is None: return img,0
    x0,_,x1,_=bbox
    white_left=max(0,round(x0*original_w/w)-5)
    white_right=max(0,round((w-x1)*original_w/w)-5)
    dx=max(-white_left,min(white_right,proposed))
    if dx==0:return img,0
    out=Image.new("RGB",img.size,(255,255,255))
    out.paste(img,(dx,0))
    return out,dx
