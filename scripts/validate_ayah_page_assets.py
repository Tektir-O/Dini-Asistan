#!/usr/bin/env python3
"""Check all 604 Mushaf page clips and ayah markers before APK release."""
import argparse
import json
import re
import subprocess
from pathlib import Path

def check_index(path):
    data=json.loads(Path(path).read_text(encoding="utf8"))
    pages=data["pages"]
    if len(pages)!=604 or set(pages)!={str(i) for i in range(1,605)}:
        raise RuntimeError("Page index is not exactly all pages 1..604")
    keys=[]
    for page in range(1,605):
        arr=pages[str(page)]
        if not arr:raise RuntimeError(f"Page {page}: empty ayah list")
        end=0
        for verse in arr:
            surah=int(verse["surah"]);ayah=int(verse["ayah"])
            a=int(verse["fromMs"]);b=int(verse["toMs"])
            if surah not in range(1,115) or ayah not in range(1,287):
                raise RuntimeError(f"Invalid ayah {surah}:{ayah}")
            if a<end or a-end>3000 or b<=a or b-a<100:
                raise RuntimeError(f"Overlapping/nonmonotone time on {page} ayah {surah}:{ayah}: {a},{b}")
            keys.append((surah,ayah))
            end=b
    if len(keys)!=6236 or len(set(keys))!=6236:
        raise RuntimeError(f"Not precisely 6236 unique ayahs: {len(keys)}")
    if keys != sorted(keys):
        raise RuntimeError("Ayahs out of Quran order")
    if keys[0]!=(1,1) or keys[-1]!=(114,6):
        raise RuntimeError("Wrong first or last verse")
    if [tuple((v["surah"],v["ayah"])) for v in pages["1"]] != [(1,i) for i in range(1,8)]:
        raise RuntimeError("Page 1 must contain the seven ayahs of Al-Fatiha")
    counts=data.get("surahVerseCounts",{})
    if len(counts)!=114 or sum(int(x) for x in counts.values())!=6236:
        raise RuntimeError("Missing exact 114-surah verse counts")
    for surah in range(1,115):
        curr=[a for s,a in keys if s==surah]
        if curr!=list(range(1,int(counts[str(surah)])+1)):
            raise RuntimeError("Missing or duplicate ayah in surah "+str(surah))
    print("PASS: 604 complete ordered pages, 114 surahs, 6236 unique ayahs, valid monotone timestamps",flush=True)

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--index-only",default="")
    args=p.parse_args()
    index=Path(args.index_only) if args.index_only else Path("app/src/main/assets/quran/ayah-page-index.json")
    check_index(index)
    if args.index_only:return
    images=Path("app/src/main/assets/mushaf")
    audio=Path("app/src/main/assets/audio/pages")
    if len(list(images.glob("???.webp")))!=604:raise RuntimeError("Missing Mushaf images")
    if len(list(audio.glob("???.opus")))!=604:raise RuntimeError("Missing page audio clips")
    data=json.loads(index.read_text(encoding="utf8"))
    for page in (1,2,3,195,254,301,604):
        track=audio/f"{page:03}.opus"
        p=subprocess.run(["ffprobe","-v","error","-show_entries","format=duration",
            "-of","default=noprint_wrappers=1:nokey=1",str(track)],
            capture_output=True,text=True,check=True)
        seconds=float(p.stdout.strip())
        planned=data["pages"][str(page)][-1]["toMs"]/1000
        if abs(seconds-planned)>1.2:raise RuntimeError(f"Wrong duration page {page}: {seconds} != {planned}")
        subprocess.run(["ffmpeg","-v","error","-i",str(track),"-f","null","-"],
            stdout=subprocess.DEVNULL,check=True)
        print(f"PASS: page {page} audio decodes at {seconds:.2f}s with {len(data['pages'][str(page)])} ayahs",flush=True)
if __name__=="__main__":main()
