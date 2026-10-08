#!/usr/bin/env python3
"""Comprehensive checks for the complete Quran source package.

Checks every SHA256 against the original acquisition manifest, every Opus
file by fully decoding the audio, a representative non-silent 15s slice
from every surah, and renders every source PDF page using MuPDF.
Not a recitation/provenance/printed-page-order certification.
"""
from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

import fitz

BASE = Path("offline-sources")
SOURCE = BASE / "SOURCE-MANIFEST.json"
OUTPUT = Path("quran-verification-report.json")


def sha256(path):
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(4 * 1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def process(args, context):
    p = subprocess.run(args, capture_output=True, text=True, errors="replace")
    if p.returncode:
        raise RuntimeError(f"{context} failed (exit={p.returncode}) {p.stderr[-1400:]}")
    return p.stdout + "\n" + p.stderr


def verify_manifest():
    if not SOURCE.is_file():
        raise RuntimeError("SOURCE-MANIFEST.json is missing")
    manifest = json.loads(SOURCE.read_text(encoding="utf-8"))
    rows = manifest.get("files", [])
    expected = ["mushaf/mushaf-hafs-1441-raw.pdf"] + [f"audio/{i:03}.opus" for i in range(1, 115)]
    listed = [x.get("path") for x in rows]
    if len(rows) != 115 or len(set(listed)) != 115 or set(listed) != set(expected):
        raise RuntimeError("Manifest must list one PDF and EXACTLY 114 numbered, nonduplicate Opus tracks")
    for row in rows:
        path = BASE / row["path"]
        if not path.is_file():
            raise RuntimeError(f"Missing asset: {path}")
        size = path.stat().st_size
        if size != row["bytes"]:
            raise RuntimeError(f"Size mismatch: {path} expected {row['bytes']} actual {size}")
        fingerprint = sha256(path)
        if fingerprint != row["sha256"]:
            raise RuntimeError(f"SHA256 mismatch: {path}")
    print("PASS SHA256: 115/115 assets exactly match source acquisition manifest", flush=True)
    return manifest


def verify_pdf():
    path = BASE / "mushaf" / "mushaf-hafs-1441-raw.pdf"
    pages_with_visible_marks = []
    pages_rendered = 0
    with fitz.open(path) as doc:
        if doc.needs_pass:
            raise RuntimeError("PDF is password-protected")
        count = doc.page_count
        if count != 640:
            raise RuntimeError(f"Expected original source PDF to have 640 pages; found {count}")
        for idx, page in enumerate(doc):
            if page.rect.width <= 10 or page.rect.height <= 10:
                raise RuntimeError(f"Invalid PDF geometry on page {idx + 1}")
            # Render all pages at preview resolution. Checks renderer can decode each page.
            pix = page.get_pixmap(matrix=fitz.Matrix(0.2, 0.2), colorspace=fitz.csGRAY, alpha=False)
            if pix.width < 20 or pix.height < 20 or not pix.samples:
                raise RuntimeError(f"PDF page {idx + 1} cannot render")
            dark = sum(v < 180 for v in pix.samples)
            if dark > 0.001 * len(pix.samples):
                pages_with_visible_marks.append(idx + 1)
            pages_rendered += 1
    if len(pages_with_visible_marks) < 600:
        raise RuntimeError(f"Only {len(pages_with_visible_marks)} PDF pages contain visible marks; inspect PDF manually")
    print(f"PASS PDF: {pages_rendered} pages parsed and rendered; {len(pages_with_visible_marks)} show visible marks", flush=True)
    return {
        "pdf_total_pages": pages_rendered,
        "rendered_pages": pages_rendered,
        "pages_with_visible_marks": len(pages_with_visible_marks),
        "printed_604_mushaf_pages_verified": False,
        "note": "Original PDF has 640 PDF pages, not an independently checked 604-page printed Mushaf. Manual page-by-page content and order audit pending.",
    }


def audio_probe(path):
    text = process(
        ["ffprobe", "-v", "error", "-select_streams", "a:0",
         "-show_entries", "stream=codec_name,channels,sample_rate:format=duration",
         "-of", "json", str(path)], f"ffprobe {path}"
    )
    data = json.loads(text)
    if len(data.get("streams", [])) != 1:
        raise RuntimeError(f"Missing or duplicate primary audio stream in {path}")
    stream = data["streams"][0]
    if stream.get("codec_name") != "opus":
        raise RuntimeError(f"Unexpected audio codec in {path}: {stream.get('codec_name')}")
    if int(stream.get("sample_rate", 0)) != 48000 or int(stream.get("channels", 0)) != 1:
        raise RuntimeError(f"Unexpected sample rate/channels in {path}")
    dur = float(data["format"]["duration"])
    if not 15 <= dur <= 40000:
        raise RuntimeError(f"Implausible surah duration in {path}: {dur}")
    return dur


def verify_audio():
    report = []
    for num in range(1, 115):
        path = BASE / "audio" / f"{num:03}.opus"
        duration = audio_probe(path)
        if num == 2 and duration < 3000:
            raise RuntimeError("Surah al-Baqarah appears truncated (<50min)")
        if num == 1 and duration > 1200:
            raise RuntimeError("Surah al-Fatiha appears too long (>20min)")

        # Full decode checks corruption even if metadata and header are valid.
        process(
            ["ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-xerror",
             "-i", str(path), "-map", "0:a:0", "-f", "null", "-"],
            f"complete decode surah {num:03}",
        )
        # Test signal at a sample away from start/end, where intros/silence may occur.
        offset = max(0, duration * 0.45)
        if offset + 15 > duration:
            offset = max(0, duration - 16)
        loudness_log = process(
            ["ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "info", "-xerror",
             "-ss", f"{offset:.3f}", "-i", str(path), "-t", "15",
             "-af", "volumedetect", "-f", "null", "-"],
            f"volume detection surah {num:03}",
        )
        matches = re.findall(r"max_volume:\s*(-?[\d.]+)\s*dB", loudness_log)
        if not matches:
            raise RuntimeError(f"Cannot estimate voice signal on surah {num:03}")
        peak_db = float(matches[-1])
        if peak_db < -60:
            raise RuntimeError(f"Surah {num:03} is inaudible in midpoint sample ({peak_db} dB)")
        report.append({"surah": num, "duration_seconds": round(duration, 2),
                       "midpoint_peak_dbfs": peak_db, "full_decode_passed": True})
        print(f"PASS SURA {num:03}/114: {duration:.1f}s, peak={peak_db}dBFS, full decode OK", flush=True)
    print("PASS AUDIO: all 114 files decode fully, have sound and expected encoding", flush=True)
    return {"total": len(report), "total_duration_hours": round(sum(x["duration_seconds"] for x in report) / 3600, 2),
            "all_fully_decoded": True, "all_midpoints_non_silent": True, "tracks": report}


def main():
    manifest = verify_manifest()
    pdf = verify_pdf()
    audio = verify_audio()
    result = {
        "checked_at_utc": datetime.now(timezone.utc).isoformat(),
        "overall_technical_result": "PASS",
        "source_manifest_sha256": sha256(SOURCE),
        "expected_sha256_count": 115,
        "pdf": pdf,
        "audio": audio,
        "rights_confirmed_for_third_party_mirror": False,
        "audio_text_verbatim_verified_by_listening": False,
        "application_apk_built_or_offline_tested": False,
        "limitations": [
            "A digital signal test cannot verify each recited word or verse order.",
            "The mirror's content has not been conclusively matched against the publisher's licensed recordings.",
            "Printed 604-page sequence still needs visual/content verification.",
            "An APK must exist before Android offline and UI tests can be performed.",
        ],
    }
    OUTPUT.write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")
    print("OVERALL TECHNICAL CHECKS PASSED. Report written to", OUTPUT, flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"DEEP VERIFICATION FAILED: {type(exc).__name__}: {exc}", file=sys.stderr, flush=True)
        sys.exit(1)
