#!/usr/bin/env python3
"""Synthetic layout-only tests: never require changing Qur'an characters."""
from PIL import Image, ImageDraw
from mushaf_alignment import normalize_page

def fake(left,right, badge=False):
    im=Image.new("RGB",(500,700),"white")
    d=ImageDraw.Draw(im)
    green=(43,150,78)
    for x in (left,right):
        d.rectangle((x,100,x+7,640),fill=green)
    if badge:
        d.ellipse((10,260,46,315),outline=green,width=4)
    d.rectangle((left+15,130,right-15,620),outline=(20,20,20),width=3)
    return im

centered,zero=normalize_page(fake(95,395))
assert abs(zero)<=3, f"Centered page shifted {zero}"
left,page_shift=normalize_page(fake(65,365))
assert page_shift>0, f"Left-shifted print frame failed to center: {page_shift}"
right,page_shift2=normalize_page(fake(125,425))
assert page_shift2<0, f"Right-shifted print frame failed to center: {page_shift2}"
bare=Image.new("RGB",(500,700),"white")
_,offset=normalize_page(bare)
assert offset==0, "Blank pages must never shift"
print(f"PASS: right-to-left alternating page layout is horizontally normalized without cropping; shifts={zero},{page_shift},{page_shift2}")
