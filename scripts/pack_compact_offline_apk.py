#!/usr/bin/env python3
"""Build a compact all-in-one Android APK asset set without any network access.

All 114 Quran surahs still included in full. Recode Opus at 20kbps mono for
a manageable single-APK install. This is a compressed spoken-word tradeoff.
Mushaf page dimensions reduced while retaining Fatiha/604 order and image
ornaments without cropping. No fabricated per-page audio timings.
"""
import hashlib
import json
import subprocess
import sys
from pathlib import Path
import fitz
from PIL import Image
from mushaf_alignment import normalize_page

src=Path("offline-sources")
dest=Path("app/src/main/assets")
manifest=json.loads((src/"SOURCE-MANIFEST.json").read_text(encoding="utf-8"))
indexed={x["path"]:x for x in manifest.get("files",[])}
expected={f"audio/{i:03}.opus" for i in range(1,115)}
expected.add("mushaf/mushaf-hafs-1441-raw.pdf")
if set(indexed)!=expected:
    sys.exit("Invalid source manifest, 114 tracks + PDF required")

def digest(path):
    sha=hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda:f.read(1024*1024),b""):
            sha.update(block)
    return sha.hexdigest()

for key in sorted(expected):
    path=src/key
    if not path.exists() or path.stat().st_size != indexed[key]["bytes"] or digest(path)!=indexed[key]["sha256"]:
        raise RuntimeError("Asset SHA256 failed: "+key)
print("PASS original source SHA256: 115/115",flush=True)

audio=dest/"audio/surah"
audio.mkdir(parents=True,exist_ok=True)
durations=[]
for number in range(1,115):
    original=src/f"audio/{number:03}.opus"
    converted=audio/f"{number:03}.opus"
    subprocess.run(["ffmpeg","-nostdin","-hide_banner","-loglevel","error","-y",
      "-i",str(original),"-vn","-map","0:a:0","-c:a","libopus","-application","audio",
      "-compression_level","5","-vbr","constrained","-b:a","20k","-ac","1","-ar","48000",
      str(converted)],check=True)
    if converted.stat().st_size<15000:
        raise RuntimeError(f"Empty track {number}")
    p=subprocess.run(["ffprobe","-v","error","-show_entries","format=duration","-of",
        "default=noprint_wrappers=1:nokey=1",str(converted)],text=True,capture_output=True,check=True)
    duration=float(p.stdout)
    if duration<15:
        raise RuntimeError(f"Suspicious track {number}: {duration}")
    durations.append(duration)
    if number%10==0 or number==114:
        print(f"Compressed complete surahs {number}/114",flush=True)

pdf=src/"mushaf/mushaf-hafs-1441-raw.pdf"
pages=dest/"mushaf"
pages.mkdir(parents=True,exist_ok=True)
shifts={}
with fitz.open(pdf) as document:
    if document.page_count!=640 or document.needs_pass:
        raise RuntimeError("Expected 640-page unencrypted source")
    for n in range(1,605):
        page=document[n+2] # PDF page 4 -> printed Mushaf page 1
        pix=page.get_pixmap(matrix=fitz.Matrix(1.35,1.35),colorspace=fitz.csRGB,alpha=False)
        img=Image.frombytes("RGB",(pix.width,pix.height),pix.samples)
        img,dx=normalize_page(img)
        shifts[str(n)]=dx
        target=pages/f"{n:03}.webp"
        img.save(target,format="WEBP",quality=76,method=5)
        if target.stat().st_size<4500:
            raise RuntimeError(f"Bad print page {n}")
        if n%100==0 or n==604:
            print(f"Prepared Mushaf images {n}/604",flush=True)

report={"page_first":1,"page_last":604,"source_page_first":4,"source_page_last":607,
        "strategy":"conservative horizontal frame centering","shift_pixels_by_page":shifts,
        "all_print_page_image_count":604,
        "all_surah_count":114,
        "audio_target_codec":"Opus mono 20kbps constrained VBR",
        "audio_duration_sum_seconds":sum(durations),
        "page_audio_alignment_complete":False,
        "distribution_rights_third_party_audio_mirror_verified":False}
(pages/"frame-alignment-report.json").write_text(json.dumps(report,indent=2),encoding="utf-8")
(dest/"source-package-status.json").write_text(json.dumps(report,indent=2),encoding="utf-8")
assert len(list(pages.glob("???.webp")))==604
assert len(list(audio.glob("???.opus")))==114
total=sum(p.stat().st_size for p in audio.glob("???.opus"))
total+=sum(p.stat().st_size for p in pages.glob("???.webp"))
print(f"PASS 604 Mushaf pages and 114 complete surah files. Asset bytes={total}",flush=True)
if total>490_000_000:
    raise RuntimeError(f"Compressed media exceeded requested size: {total}")
