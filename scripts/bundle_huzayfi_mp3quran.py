#!/usr/bin/env python3
"""Bundle the complete 114-surah Ali al-Hudhaifi Hafs offline Quran audio into Android assets.

Downloads ALL recordings from the MP3Quran publisher using the validated downloader,
transcodes speech to Ogg Opus to save space, and verifies each file's duration/codec
and SHA-256. Any absent, truncated or undecodable track FAILS the build.

Publisher: MP3Quran.net (not King Fahd Complex official masters).
Source and attribution: https://www.mp3quran.net/eng/hthfi
Use/redistribution terms: https://www.mp3quran.net/ar/privacy
"""
import concurrent.futures
import hashlib
import json
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "dist/huzayfi-hafs"
DEST = ROOT / "app/src/main/assets/audio-ar/huzayfi-hafs"
MANIFEST = SOURCE / "manifest.json"
OUTPUT_BITRATE = "32k"

def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda:source.read(1_048_576), b""):
            digest.update(chunk)
    return digest.hexdigest()

def probe(path):
    result = subprocess.run([
        "ffprobe", "-v", "error", "-select_streams", "a:0",
        "-show_entries", "stream=codec_name:format=duration",
        "-of", "json", str(path)],
        capture_output=True, text=True, check=True, timeout=80)
    obj = json.loads(result.stdout)
    tracks = obj.get("streams", [])
    assert len(tracks) >= 1, f"Audio stream missing: {path.name}"
    return tracks[0].get("codec_name"), float(obj["format"]["duration"])

def process(entry):
    number = entry["surah"]
    assert 1 <= number <= 114
    src = SOURCE / f"{number:03d}.mp3"
    dst = DEST / f"{number:03d}.ogg"
    assert src.exists(), f"Missing source surah {number}"
    assert sha256(src) == entry["sha256"], f"MP3 integrity mismatch {number}"
    src_codec, src_duration = probe(src)
    assert src_codec == "mp3" and src_duration > 5, f"Invalid MP3 surah {number}"
    result = subprocess.run([
        "ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error",
        "-y", "-i", str(src), "-map", "0:a:0", "-vn",
        "-map_metadata", "-1", "-c:a", "libopus",
        "-b:a", OUTPUT_BITRATE, "-vbr", "on", "-compression_level", "8",
        "-ac", "1", "-ar", "24000", "-application", "audio", str(dst)
    ], capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError(f"Opus encoding {number}: {result.stderr[-1000:]}")
    assert dst.exists() and dst.stat().st_size >= 4096, f"Missing encoded Ogg {number}"
    codec, duration = probe(dst)
    assert codec == "opus", f"Invalid Ogg Opus codec for {number}: {codec}"
    assert abs(duration - src_duration) < max(5.0, src_duration * 0.01), (
       f"Encoded duration mismatch for {number}: {duration} vs {src_duration}")
    record = {
        "surah":number, "filename":dst.name,
        "duration_ms":round(duration*1000),
        "source_file_sha256":entry["sha256"],
        "source_url":entry["source"],
        "bundle_sha256":sha256(dst),
        "bytes":dst.stat().st_size
    }
    src.unlink()  # No duplicate MP3 files after verified Opus has been created.
    print(f"Bundled {number:03d}/114, {round(dst.stat().st_size/1_048_576,2)} MiB",flush=True)
    return record

def main():
    subprocess.run([sys.executable,str(ROOT/"scripts/download_huzayfi_mp3quran.py"),
                    "--no-archive"],cwd=ROOT,check=True)
    original = json.loads(MANIFEST.read_text(encoding="utf-8"))
    entries = original["surahs"]
    assert len(entries) == 114 and [x["surah"] for x in entries] == list(range(1,115)), (
        "114 ordered surah files required")
    assert original["reader"] == "Ali Abdul Rahman al-Hudhaifi"
    assert original["riwayah"] == "Hafs an Asim"
    DEST.mkdir(parents=True,exist_ok=True)
    for stale in DEST.glob("*.ogg"):
        stale.unlink()
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as executor:
        records = list(executor.map(process,entries))
    records.sort(key=lambda x:x["surah"])
    assert len(records)==114 and [x["surah"] for x in records]==list(range(1,115))
    manifest = {
        "reciter_id":"huzayfi-hafs",
        "reciter_name":"Ali Abdul Rahman al-Hudhaifi",
        "riwayah":"Hafs",
        "publisher":"MP3Quran.net",
        "source_page":"https://www.mp3quran.net/eng/hthfi",
        "permission_page":"https://www.mp3quran.net/ar/privacy",
        "recording_count":114,
        "format":"ogg-opus",
        "bitrate":OUTPUT_BITRATE,
        "tracking_level":"surah",
        "ayah_timing_verified":False,
        "recordings":records
    }
    (DEST/"manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print("BUNDLE_SIZE_MiB",round(sum(x["bytes"] for x in records)/1_048_576,1),flush=True)
    subprocess.run([sys.executable,str(ROOT/"scripts/verify_huzayfi_audio.py")],
                   cwd=ROOT,check=True)
    assert len(list(DEST.glob("*.ogg")))==114
    print("READY: All 114 offline Hafs surahs packaged and verified.",flush=True)

if __name__=="__main__":
    main()
