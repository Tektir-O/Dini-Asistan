#!/usr/bin/env python3
"""Prepare authentic surah-level offline audio from a verified source archive.

Example (on the build machine):
  python3 scripts/prepare_huzayfi_audio.py --archive /path/to/official-hafs-surahs.zip
  python3 scripts/prepare_huzayfi_audio.py --url "$HUZAYFI_AUDIO_ARCHIVE_URL"

Required: Python 3, ffmpeg with libopus, ffprobe.
The archive must contain exactly one decodable sound recording for each surah 001..114.
It must be the OFFICIAL Ali al-Hudhayfi Hafs surah archive, not a similar reciter/riwayah.
This script does not invent verse timing: playback is whole-surah only.
"""
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import tempfile
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "app/src/main/assets/audio-ar/huzayfi-hafs"
ALLOWED = {".mp3", ".m4a", ".ogg", ".wav", ".flac", ".opus"}
OFFICIAL_SOURCE = "https://qurancomplex.gov.sa/category/kfgqpc-quran-audio/recite/moratal/hafs/huthify/"

def digest(path):
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

def command(args):
    return subprocess.run(args, check=True, capture_output=True, text=True).stdout.strip()

def entry_surah(name):
    filename = Path(name).name
    if Path(filename).suffix.lower() not in ALLOWED:
        return None
    # Accept filenames such as 001.mp3, 001_Fatiha.mp3 or surah_001.mp3.
    match = re.search(r"(?:^|[^0-9])([0-9]{3})(?=[^0-9]|$)", Path(filename).stem)
    if not match:
        return None
    number = int(match.group(1))
    return number if 1 <= number <= 114 else None

def main():
    parser = argparse.ArgumentParser(description="Bundle official Huzayfi Hafs surah recordings")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--archive", type=Path, help="Downloaded official 114-surah ZIP")
    group.add_argument("--url", help="Direct URL of the official 114-surah ZIP archive")
    parser.add_argument("--expected-sha256", default="", help="Optional trusted source ZIP digest")
    args = parser.parse_args()

    with tempfile.TemporaryDirectory(prefix="huzayfi-build-") as work:
        temp = Path(work)
        archive = temp / "source.zip"
        if args.url:
            if not args.url.startswith("https://"):
                raise SystemExit("Official archive URL must use HTTPS.")
            with urllib.request.urlopen(args.url, timeout=120) as data, archive.open("wb") as out:
                shutil.copyfileobj(data, out)
        else:
            shutil.copy2(args.archive, archive)

        source_hash = digest(archive)
        if args.expected_sha256 and source_hash != args.expected_sha256.lower():
            raise SystemExit("Source ZIP SHA-256 mismatch. Refusing to package.")
        if not zipfile.is_zipfile(archive):
            raise SystemExit("Source was not a ZIP archive.")
        with zipfile.ZipFile(archive) as z:
            entries = {}
            for member in z.infolist():
                if member.is_dir():
                    continue
                n = entry_surah(member.filename)
                if n is None:
                    continue
                if n in entries:
                    raise SystemExit(f"Duplicate recording for surah {n}: {entries[n].filename} / {member.filename}")
                entries[n] = member
            missing = sorted(set(range(1, 115)) - set(entries))
            if missing:
                raise SystemExit("Source archive missing surahs: " + ", ".join(map(str, missing)))

            prepared = temp / "prepared"
            prepared.mkdir()
            records = []
            for n in range(1, 115):
                member = entries[n]
                original = temp / ("source-" + str(n).zfill(3) + Path(member.filename).suffix.lower())
                if member.file_size < 4096:
                    raise SystemExit(f"Suspiciously short recording: surah {n}")
                with z.open(member) as src, original.open("wb") as dst:
                    shutil.copyfileobj(src, dst)
                duration = command(["ffprobe", "-v", "error", "-show_entries",
                    "format=duration", "-of", "default=noprint_wrappers=1:nokey=1",
                    str(original)])
                try:
                    seconds = float(duration)
                except ValueError as e:
                    raise SystemExit(f"No valid duration for surah {n}") from e
                if not 2 < seconds < 25000:
                    raise SystemExit(f"Invalid recording duration for surah {n}: {seconds}")
                output = prepared / f"{n:03}.ogg"
                subprocess.run(["ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error",
                    "-y", "-i", str(original), "-vn", "-c:a", "libopus", "-b:a",
                    "40k", "-vbr", "on", "-application", "audio", str(output)],
                    check=True)
                if output.stat().st_size < 4096:
                    raise SystemExit(f"Output corrupt/empty: surah {n}")
                records.append({"surah": n, "filename": output.name,
                    "duration_ms": round(seconds * 1000),
                    "source_sha256": digest(original), "bundle_sha256": digest(output)})
                original.unlink()
                print(f"Prepared surah {n:03}/114", flush=True)

        manifest = {"reciter_id": "huzayfi-hafs",
            "reciter_name": "Ali al-Hudhayfi", "riwayah": "Hafs",
            "source_page": OFFICIAL_SOURCE,
            "source_archive_sha256": source_hash, "tracking_level": "surah",
            "recording_count": 114, "format": "ogg-opus",
            "recordings": records, "ayah_timing_verified": False}
        (prepared / "manifest.json").write_text(
            json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        # Do not leave half-packaged audio if an earlier conversion failed.
        DEST.mkdir(parents=True, exist_ok=True)
        for stale in DEST.glob("*.ogg"):
            stale.unlink()
        for output in prepared.iterdir():
            shutil.move(str(output), DEST / output.name)
        print("Prepared 114/114 offline surah files with SHA-256 manifest.")

if __name__ == "__main__":
    main()
