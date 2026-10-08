#!/usr/bin/env python3
"""Download an attributed offline MP3 collection of Ali al-Hudhaifi's Hafs recitation.

Audio provider: https://www.mp3quran.net/eng/hthfi
Permissions: https://www.mp3quran.net/ar/privacy
Provider CDN: https://cdn.mp3quran.net/audio/ali-hudhaifi/r1/
Each recording is checked for ID3/MP3 decoding, plausible duration and SHA-256.
This does not claim these recordings are King Fahd Complex masters.
"""
import argparse
import concurrent.futures
import hashlib
import json
import os
import subprocess
import time
import urllib.request
import zipfile
from pathlib import Path

BASE = "https://cdn.mp3quran.net/audio/ali-hudhaifi/r1/"
ROOT = Path("dist/huzayfi-hafs")
MIN_BYTES = 20000

def checksum(path):
    h = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

def download(number):
    ROOT.mkdir(parents=True, exist_ok=True)
    file = ROOT / f"{number:03d}.mp3"
    url = BASE + f"{number:03d}.mp3"
    for retry in range(5):
        try:
            req = urllib.request.Request(url, headers={
                "User-Agent":"DiniAsistanim-offline-recitation/1.0 (+https://github.com/Tektir-O/Dini-Asistan)",
                "Accept": "audio/mpeg,application/octet-stream,*/*"
            })
            with urllib.request.urlopen(req,timeout=65) as response, file.open("wb") as target:
                if response.status != 200:
                    raise ValueError(f"HTTP {response.status}")
                while True:
                    block = response.read(1024*1024)
                    if not block: break
                    target.write(block)
            if file.stat().st_size < MIN_BYTES:
                raise ValueError(f"File too small: {file.stat().st_size}")
            probe = subprocess.run([
                "ffprobe","-v","error","-select_streams","a:0",
                "-show_entries","stream=codec_name,duration",
                "-of","json",str(file)
            ],capture_output=True,text=True,check=True,timeout=60)
            streams = json.loads(probe.stdout).get("streams",[])
            if not streams or streams[0].get("codec_name") != "mp3":
                raise ValueError("Unsupported or non-MP3 recording")
            duration = float(streams[0].get("duration","0"))
            if not 5 < duration < 25000:
                raise ValueError(f"Invalid duration: {duration}")
            return {"surah":number,"filename":file.name,"bytes":file.stat().st_size,
                "duration_seconds":round(duration,2),"sha256":checksum(file),"source":url}
        except Exception as exc:
            file.unlink(missing_ok=True)
            print(f"RETRY {number:03d} {retry+1}/5 {exc}",flush=True)
            if retry == 4: raise
            time.sleep(min(4*(retry+1),15))

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sample",action="store_true")
    parser.add_argument("--start",type=int,default=1)
    parser.add_argument("--end",type=int,default=114)
    args = parser.parse_args()
    if args.start < 1 or args.end > 114 or args.start > args.end:
        raise SystemExit("Invalid surah range")
    chosen = [1,112,113,114] if args.sample else list(range(args.start,args.end+1))
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        records = list(pool.map(download,chosen))
    records.sort(key=lambda r:r["surah"])
    assert [x["surah"] for x in records] == chosen
    manifest={
       "reader":"Ali Abdul Rahman al-Hudhaifi",
       "riwayah":"Hafs an Asim",
       "publisher":"MP3Quran.net",
       "source_collection":"https://www.mp3quran.net/eng/hthfi",
       "licensing_terms":"https://www.mp3quran.net/ar/privacy",
       "cdn":BASE,
       "number_of_surahs":len(records),
       "full_complete_quran":len(records)==114,
       "surahs":records
    }
    (ROOT/"manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    (ROOT/"KAYNAK-VE-IZIN.txt").write_text(
       "Ali el-Huzeyfi - Hafs an Asim\n"+
       "Ses kaynagi: MP3Quran.net\n"+
       "Okuyucu: https://www.mp3quran.net/eng/hthfi\n"+
       "Yayim/izin: https://www.mp3quran.net/ar/privacy\n"+
       "Dosyalar mp3quran.net sunucusundan alinmistir; Kral Fahd Kompleksi ana kayitlari\n"+
       "olarak etiketlenmez. Saglayicinin kaynak bilgisini koruyunuz.\n"+
       "Her sure icin dosya uzunlugu ve SHA256 manifest.json icinde bulunur.\n",
       encoding="utf-8")
    archive_name = ("Ali-el-Huzeyfi-Hafs-ORNEK-4-Sure.zip" if args.sample else
      "Ali-el-Huzeyfi-Hafs-114-Sure-MP3.zip" if (args.start==1 and args.end==114) else
      f"Ali-el-Huzeyfi-Hafs-{args.start:03d}-{args.end:03d}.zip")
    archive = Path("dist")/archive_name
    with zipfile.ZipFile(archive,"w",compression=zipfile.ZIP_STORED,allowZip64=True) as z:
        for file in sorted(ROOT.iterdir()):
            z.write(file,arcname=file.name)
    print("RESULT",archive,"surahs",len(records),"size",archive.stat().st_size,
          "bytes","sha256",checksum(archive),flush=True)
    if not args.sample and len(records) != args.end-args.start+1:
        raise SystemExit("ERROR: Missing surahs in requested range.")
if __name__ == "__main__":
    main()
