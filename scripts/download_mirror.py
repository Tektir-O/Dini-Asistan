#!/usr/bin/env python3
"""Download and verify offline Qur'an sources; fail closed on missing data.

PDF: King Fahd Complex Hafs 1441 Mushaf via Internet Archive mirror.
MP3: Haramain Recordings' mirror identified as a King Fahd Complex recording.
Mirrors are not the publisher; provenance must still be reviewed before public APK publication.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
import time
from pathlib import Path

import requests

MUSHAF_URL = "https://archive.org/download/MushafMadinaHafsGreen1441/MushafMadinaHafsGreen1441.pdf"
AUDIO_BASE = "https://archive.org/download/HaramainMaahir/"
ROOT = Path("offline-sources")
HEADERS = {"User-Agent": "DiniAsistan-offline-asset-preparation/2.0"}


def download(url: str, output: Path, minimum: int) -> dict:
    """Stream to disk with retries, SHA-256 and minimum size checks."""
    output.parent.mkdir(parents=True, exist_ok=True)
    part = output.with_name(output.name + ".part")
    err = None
    for retry in range(4):
        part.unlink(missing_ok=True)
        try:
            sha = hashlib.sha256()
            length = 0
            with requests.get(url, stream=True, headers=HEADERS, timeout=(40, 180)) as response:
                response.raise_for_status()
                mime = response.headers.get("Content-Type", "").lower()
                if "text/html" in mime or "application/json" in mime:
                    raise RuntimeError(f"Unexpected content type {mime} from {response.url}")
                with part.open("wb") as output_file:
                    for chunk in response.iter_content(1024 * 1024):
                        if not chunk:
                            continue
                        output_file.write(chunk)
                        sha.update(chunk)
                        length += len(chunk)
                        if length > 350_000_000:
                            raise RuntimeError("Download unexpectedly over 350MB")
            if length < minimum:
                raise RuntimeError(f"File too small: {length} bytes")
            part.replace(output)
            return {"url": url, "bytes": length, "sha256": sha.hexdigest()}
        except (requests.RequestException, RuntimeError) as exc:
            err = exc
            print(f"Retry {retry + 1}/4: {url}: {exc}", flush=True)
            if retry < 3:
                time.sleep(5 * (retry + 1))
    part.unlink(missing_ok=True)
    raise RuntimeError(f"Download permanently failed: {url}: {err}")


def probe(path: Path) -> float:
    result = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "default=noprint_wrappers=1:nokey=1", str(path)],
        capture_output=True, text=True, check=True,
    )
    return float(result.stdout.strip())


def mushaf():
    from pypdf import PdfReader

    path = ROOT / "mushaf" / "mushaf-hafs-1441-raw.pdf"
    entry = download(MUSHAF_URL, path, minimum=30_000_000)
    if path.open("rb").read(4) != b"%PDF":
        raise RuntimeError("Mushaf was not downloaded as PDF")
    page_count = len(PdfReader(str(path)).pages)
    if not 604 <= page_count <= 700:
        raise RuntimeError(f"Unexpected PDF page count: {page_count}")
    entry["pdf_pages_including_prelims"] = page_count
    entry["status"] = "Raw 640-page PDF; identifying the actual 604 mushaf pages is pending"
    (ROOT / "mushaf" / "manifest.json").write_text(
        json.dumps(entry, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    print("Mushaf source saved, PDF pages:", page_count, flush=True)


def audio_shard(shard: int, shard_count: int):
    if not 0 <= shard < shard_count:
        raise RuntimeError("Bad shard")
    directory = ROOT / "audio"
    directory.mkdir(parents=True, exist_ok=True)
    manifest = []
    for num in range(1 + shard, 115, shard_count):
        base = f"{num:03}"
        origin = AUDIO_BASE + base + ".mp3"
        raw_path = directory / f"{base}.source.mp3"
        opus_path = directory / f"{base}.opus"
        metadata = download(origin, raw_path, minimum=100_000)
        if probe(raw_path) < 10.0:
            raise RuntimeError(f"Invalid/short recitation file: {base}")
        subprocess.run(
            ["ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-y",
             "-i", str(raw_path), "-map", "0:a:0", "-vn",
             "-c:a", "libopus", "-b:a", "64k", "-vbr", "on",
             "-ac", "1", "-ar", "48000", str(opus_path)],
            check=True,
        )
        if opus_path.stat().st_size < 30_000 or probe(opus_path) < 10.0:
            raise RuntimeError(f"Opus conversion did not verify: {base}")
        metadata["surah"] = num
        metadata["packed_name"] = opus_path.name
        metadata["packed_bytes"] = opus_path.stat().st_size
        manifest.append(metadata)
        raw_path.unlink()
        print(f"Verified surah {num:03}/114 -> {opus_path.stat().st_size} bytes", flush=True)
    (ROOT / f"audio-part-{shard}.json").write_text(
        json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8"
    )


def verify():
    from pypdf import PdfReader

    pdf = ROOT / "mushaf" / "mushaf-hafs-1441-raw.pdf"
    tracks = sorted((ROOT / "audio").glob("*.opus"))
    expected = [f"{i:03}.opus" for i in range(1, 115)]
    if [p.name for p in tracks] != expected:
        raise RuntimeError("Missing or unexpected surah files")
    if not pdf.exists() or not 604 <= len(PdfReader(str(pdf)).pages) <= 700:
        raise RuntimeError("Mushaf PDF missing or invalid")
    for p in tracks:
        if p.stat().st_size < 30_000:
            raise RuntimeError(f"Empty recitation: {p}")
    rows = []
    for f in [pdf] + tracks:
        h = hashlib.sha256()
        with f.open("rb") as stream:
            for block in iter(lambda: stream.read(1024 * 1024), b""):
                h.update(block)
        rows.append({"path": str(f.relative_to(ROOT)), "bytes": f.stat().st_size, "sha256": h.hexdigest()})
    metadata = {
        "mushaf_publisher": "King Fahd Glorious Quran Printing Complex",
        "mushaf_source_pdf_mirror": MUSHAF_URL,
        "mushaf_pdf_pages_including_prelims": len(PdfReader(str(pdf)).pages),
        "printed_mushaf_pages": "NOT YET VERIFIED (target 604)",
        "reciter": "Maher al-Muaiqly",
        "audio_publisher_claim": "Haramain Recordings identifies this Internet Archive recording as a King Fahd Quran Complex recording",
        "audio_mirror": "https://archive.org/details/HaramainMaahir",
        "audio_license_official": "https://qc-dev.qurancomplex.gov.sa/quran-audios/",
        "audio_total": len(tracks),
        "audio_encoding": "Ogg Opus mono 64kbps, complete surahs, optimized for APK",
        "human_review": "Independent confirmation that mirrored recordings match publisher's licensed edition is required before publishing a public APK.",
        "files": rows,
    }
    (ROOT / "SOURCE-MANIFEST.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print("Verified all 114 offline Opus surahs and raw Mushaf PDF", flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("stage", choices=["mushaf", "audio", "verify"])
    parser.add_argument("--shard", type=int, default=0)
    parser.add_argument("--shards", type=int, default=3)
    args = parser.parse_args()
    try:
        if args.stage == "mushaf":
            mushaf()
        elif args.stage == "audio":
            audio_shard(args.shard, args.shards)
        else:
            verify()
    except Exception as exc:
        print(f"FAILED: {type(exc).__name__}: {exc}", file=sys.stderr)
        raise
