#!/usr/bin/env python3
"""Offline Maher-al-Muaiqly ayah-by-ayah audio -> 604 page Opus clips.

Provenance:
* Quran page start boundaries: ouryhamdalaye/madinah-mushaf-json, CC BY-SA 4.0,
  itself derived from zonetecde/mushaf-layout (upstream license unclear).
* Maher Al Muaiqly 64kbps verse-separated MP3: everyayah.com mirror.
* Exact ayah audio boundaries are the individual MP3 files.
* Ayahs that physically cross a printed page are played in full at their
  starting page, not split at a guessed word position.
Redistribution permissions for the source recording must still be reviewed.
"""
from __future__ import annotations
from concurrent.futures import ThreadPoolExecutor,as_completed
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import time
import zipfile
from urllib.parse import urljoin
import requests
import fitz
from PIL import Image
from mutagen.mp3 import MP3
from mushaf_alignment import normalize_page

OUT=Path("app/src/main/assets")
SOURCE=Path("build-source")
MP3_ROOT=SOURCE/"ayat"
PAGE_DEST=OUT/"audio"/"pages"
QURAN_DEST=OUT/"quran"
PRINT_DEST=OUT/"mushaf"
PAGE_STARTS_URL="https://raw.githubusercontent.com/ouryhamdalaye/madinah-mushaf-json/main/pages/verse-to-page.json"
META_URL="https://raw.githubusercontent.com/ouryhamdalaye/madinah-mushaf-json/main/pages/meta.json"
SURA_ZIPS="https://everyayah.com/data/Maher_AlMuaiqly_64kbps/zips/"
MUSHAF_PDF="https://archive.org/download/MushafMadinaHafsGreen1441/MushafMadinaHafsGreen1441.pdf"

def get(url,timeout=(35,120)):
    for attempt in range(5):
        try:
            r=requests.get(url,timeout=timeout,headers={"User-Agent":"DiniAsistanOfflineBuild/1.0"})
            r.raise_for_status()
            return r.content
        except Exception:
            if attempt==4:raise
            time.sleep(min(attempt*3+2,15))
def load_json(url):
    return json.loads(get(url))

def source_metadata():
    starts=load_json(PAGE_STARTS_URL)
    meta=load_json(META_URL)
    if len(starts)!=604 or len(meta)!=114:
        raise RuntimeError("Source Quran metadata must have 604 page starts and 114 surah records")
    counts={}
    for idx,item in enumerate(meta,1):
        if int(item["id"])!=idx:raise RuntimeError("Out of order surah counts")
        counts[idx]=int(item["total_verses"])
    if sum(counts.values())!=6236:raise RuntimeError("Not a 6236-ayah Hafs corpus")
    keys=[(int(x["surah"]),int(x["ayah"])) for x in starts]
    if keys[0]!=(1,1) or keys[1]!=(2,1) or keys[2]!=(2,6):
        raise RuntimeError("Unexpected first pages, aborting wrong Quran indexing")
    if len(set(keys))!=604 or keys!=sorted(keys):
        raise RuntimeError("604 page starts must be strictly increasing by chapter+ayah")
    expected={(s,a) for s,length in counts.items() for a in range(1,length+1)}
    for key in keys:
        if key not in expected:raise RuntimeError("Invalid page start ayah "+str(key))
    with (QURAN_DEST/"page-boundary-provenance.json").open("w",encoding="utf8") as f:
        json.dump({"source":PAGE_STARTS_URL,"license":"CC BY-SA 4.0; upstream rights require review",
            "first":list(keys[0]),"last":list(keys[-1]),"quran_ayat":6236,
            "rule":"Ayahs are assigned to page where ayah begins; cross-page ayahs not split mid-verse."},f,indent=2)
    print("PASS Madinah 604-page starts, 114 chapters, 6236 ayahs",flush=True)
    return keys,counts

def download_surah(surah):
    file=SOURCE/"zips"/f"{surah:03}.zip"
    file.parent.mkdir(parents=True,exist_ok=True)
    if not file.exists():
        file.write_bytes(get(f"{SURA_ZIPS}{surah:03}.zip",(45,210)))
    with zipfile.ZipFile(file) as z:
        members=[n for n in z.namelist() if re.search(r"[0-9]{6}\.mp3$",n)]
        if not members:raise RuntimeError(f"No ayah MP3 tracks in surah {surah} ZIP")
        for member in members:
            filename=Path(member).name
            if not re.fullmatch(r"[0-9]{6}\.mp3",filename):continue
            key=filename[:3]
            if int(key)!=surah:raise RuntimeError("Unexpected surah track in "+file.name)
            target=MP3_ROOT/filename
            target.parent.mkdir(parents=True,exist_ok=True)
            with z.open(member) as src, target.open("wb") as dst:
                while True:
                    block=src.read(128*1024)
                    if not block:break
                    dst.write(block)
            if target.stat().st_size<1000:raise RuntimeError("Empty ayah track "+filename)
    file.unlink()
    return surah,len(members)

def download_all(counts):
    print("Downloading Maher ayah-separated recitations in 114 surah archives",flush=True)
    with ThreadPoolExecutor(max_workers=8) as executor:
        futures={executor.submit(download_surah,s):s for s in range(1,115)}
        for i,job in enumerate(as_completed(futures),1):
            surah,num=job.result()
            if i%15==0 or i==114:print(f"Downloaded surah archives {i}/114",flush=True)
    missing=[f"{s:03}{a:03}.mp3" for s,total in counts.items() for a in range(1,total+1)
             if not (MP3_ROOT/f"{s:03}{a:03}.mp3").is_file()]
    if missing: raise RuntimeError(f"Missing {len(missing)} individual Quran ayahs: {missing[:30]}")
    print("PASS: 6236/6236 separate Maher recitation ayahs exist",flush=True)

def page_ayah_keys(start_keys,counts):
    flat=[(s,a) for s,total in counts.items() for a in range(1,total+1)]
    positions={k:i for i,k in enumerate(flat)}
    groups={}
    for idx,start in enumerate(start_keys):
        first=positions[start]
        last=positions[start_keys[idx+1]] if idx<603 else len(flat)
        groups[idx+1]=flat[first:last]
        if not groups[idx+1]:raise RuntimeError("Empty page "+str(idx+1))
    if sum(len(x) for x in groups.values())!=6236:
        raise RuntimeError("Verse split or duplication discovered")
    return groups

def ffprobe(path):
    p=subprocess.run(["ffprobe","-v","error","-show_entries","format=duration",
        "-of","default=noprint_wrappers=1:nokey=1",str(path)],
        capture_output=True,text=True,check=True)
    return float(p.stdout.strip())

def create_audio(groups):
    PAGE_DEST.mkdir(parents=True,exist_ok=True)
    durations={}
    for keys in groups.values():
        for surah,ayah in keys:
            key=f"{surah:03}{ayah:03}"
            if key not in durations:
                value=MP3(MP3_ROOT/f"{key}.mp3").info.length
                if value<.25 or value>1500:raise RuntimeError("Bad ayah duration "+key)
                durations[key]=value
    index={}
    for page,keys in groups.items():
        cumul=0.
        cue=[]
        lines=[]
        for surah,ayah in keys:
            key=f"{surah:03}{ayah:03}"
            p=(MP3_ROOT/f"{key}.mp3").resolve()
            # paths are restricted to our six-digit numeric MP3 filenames
            lines.append("file '"+str(p)+"'")
            elapsed=int(round(cumul*1000))
            cumul+=durations[key]
            cue.append({"surah":surah,"ayah":ayah,"fromMs":elapsed,"toMs":int(round(cumul*1000))})
        if any(x["toMs"]<=x["fromMs"] for x in cue):
            raise RuntimeError("Nonpositive ayah clip")
        ffcat=SOURCE/"page-assembly.txt"
        ffcat.write_text("\n".join(lines)+"\n",encoding="utf8")
        outfile=PAGE_DEST/f"{page:03}.opus"
        subprocess.run(["ffmpeg","-nostdin","-hide_banner","-loglevel","error","-y",
            "-safe","0","-f","concat","-i",str(ffcat),"-map","0:a:0",
            "-c:a","libopus","-b:a","20k","-vbr","constrained",
            "-application","audio","-ac","1","-ar","48000","-compression_level","3",
            str(outfile)],check=True)
        actual=ffprobe(outfile)
        if abs(actual-cumul)>max(3.,cumul*.03):
            raise RuntimeError(f"Bad page duration page {page}: source={cumul:.2f}, output={actual:.2f}")
        # ffmpeg re-encode may cause a few ms discrepancy. Bound to actual length.
        factor=actual/cumul
        for x in cue:
            x["fromMs"]=int(round(x["fromMs"]*factor))
            x["toMs"]=int(round(x["toMs"]*factor))
        cue[-1]["toMs"]=int(actual*1000)
        index[str(page)]=cue
        if page%50==0 or page==604:
            print(f"Prepared authentic ayah-by-ayah audio for pages {page}/604",flush=True)
    (QURAN_DEST/"ayah-page-index.json").write_text(
        json.dumps({"schemaVersion":1,"reciter":"Maher al-Muaiqly",
            "source":"everyayah.com/data/Maher_AlMuaiqly_64kbps",
            "partial_ayah_policy":"Play full ayah where verse starts; no fabricated word timestamps",
            "pages":index},ensure_ascii=False,separators=(",",":")),encoding="utf8")
    if len(index)!=604:raise RuntimeError("Incomplete page cue index")
    print("PASS 604 page-specific Opus audio files and 6236 verse cues",flush=True)

def render_pages():
    PRINT_DEST.mkdir(parents=True,exist_ok=True)
    pdf=SOURCE/"source-mushaf.pdf"
    if not pdf.is_file():pdf.write_bytes(get(MUSHAF_PDF,(50,220)))
    with fitz.open(pdf) as document:
        if len(document)!=640:raise RuntimeError("Expected 640 PDF pages")
        for page in range(1,605):
            pix=document[page+2].get_pixmap(matrix=fitz.Matrix(1.33,1.33),colorspace=fitz.csRGB,alpha=False)
            frame=Image.frombytes("RGB",(pix.width,pix.height),pix.samples)
            frame,_=normalize_page(frame)
            frame.save(PRINT_DEST/f"{page:03}.webp",format="WEBP",quality=77,method=4)
            if page%100==0:print(f"Rendered print Mushaf {page}/604",flush=True)
    assert len(list(PRINT_DEST.glob("???.webp")))==604

def main():
    for d in [SOURCE,MP3_ROOT,PAGE_DEST,QURAN_DEST,PRINT_DEST]:
        d.mkdir(parents=True,exist_ok=True)
    starts,counts=source_metadata()
    groups=page_ayah_keys(starts,counts)
    download_all(counts)
    create_audio(groups)
    index_path=QURAN_DEST/"ayah-page-index.json"
    assembled=json.loads(index_path.read_text(encoding="utf8"))
    assembled["surahVerseCounts"]={str(k):v for k,v in counts.items()}
    index_path.write_text(json.dumps(assembled,ensure_ascii=False,separators=(",",":")),encoding="utf8")
    render_pages()
    total=sum(p.stat().st_size for p in PAGE_DEST.glob("*.opus"))
    total+=sum(p.stat().st_size for p in PRINT_DEST.glob("*.webp"))
    print(f"PACKAGED 604 pages + 6236 ayahs, total bytes={total}",flush=True)
    if total>520_000_000:raise RuntimeError("APK assets exceed 520 MB")
if __name__=="__main__":main()
