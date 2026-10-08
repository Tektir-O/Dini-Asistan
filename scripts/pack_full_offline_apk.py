#!/usr/bin/env python3
"""Prepare assets for a single, completely offline Android test APK.

PDF mapping: page 1 of the printed Madinah Mushaf is provisionally PDF
page 4 (zero-based page index 3); confirm first/last pages with the
publisher edition before signing or public distribution.

All 114 Ogg Opus surahs are copied byte-for-byte from the fully tested
GitHub Actions source archive. No streaming, no app-time download.
This does not invent page-specific playback timestamps.
"""
from __future__ import annotations
import hashlib
import json
import os
import shutil
import sys
from pathlib import Path

import fitz
from PIL import Image

SOURCE = Path("offline-sources")
DEST = Path("app/src/main/assets")
PAGE_COUNT = 604
PDF_FIRST_PAGE_INDEX = 3
SOURCE_MANIFEST = SOURCE / "SOURCE-MANIFEST.json"

def file_digest(p):
    h = hashlib.sha256()
    with p.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

def main():
    meta = json.loads(SOURCE_MANIFEST.read_text(encoding="utf-8"))
    indexed = {f["path"]: f for f in meta.get("files", [])}
    expected = [f"audio/{n:03}.opus" for n in range(1,115)]
    expected.append("mushaf/mushaf-hafs-1441-raw.pdf")
    if len(indexed) != 115 or set(indexed) != set(expected):
        raise RuntimeError("The source archive must contain exactly 114 Opus surahs and one original PDF")
    for key in expected:
        source = SOURCE / key
        item = indexed[key]
        if not source.exists() or source.stat().st_size != item["bytes"]:
            raise RuntimeError("Missing or incorrectly sized source asset: " + key)
        if file_digest(source) != item["sha256"]:
            raise RuntimeError("Source SHA256 mismatch: " + key)
    print("Source SHA256: 115/115 PASS", flush=True)

    audiodir = DEST / "audio" / "surah"
    audiodir.mkdir(parents=True, exist_ok=True)
    for number in range(1,115):
        src = SOURCE / "audio" / f"{number:03}.opus"
        tgt = audiodir / src.name
        shutil.copyfile(src, tgt)
        if tgt.stat().st_size != src.stat().st_size:
            raise RuntimeError("Audio copy failure: " + str(tgt))
        if number % 15 == 0:
            print("Packaged audio: %d/114" % number, flush=True)

    mushafdir = DEST / "mushaf"
    mushafdir.mkdir(parents=True, exist_ok=True)
    source_pdf = SOURCE / "mushaf" / "mushaf-hafs-1441-raw.pdf"
    with fitz.open(source_pdf) as document:
        if document.needs_pass or document.page_count != 640:
            raise RuntimeError("Unexpected source PDF count or encryption; 640 pages expected")
        if document.page_count < PDF_FIRST_PAGE_INDEX + PAGE_COUNT:
            raise RuntimeError("Not enough source PDF pages")
        diagnostic = {}
        for n in [0,1,2,3,4,606,607,608,639]:
            page = document[n]
            diagnostic[str(n+1)] = page.get_text()[:900]
        print("PDF text diagnostics on front/back pages:",json.dumps(diagnostic,ensure_ascii=False)[:2500],flush=True)

        for number in range(1,PAGE_COUNT+1):
            page = document[PDF_FIRST_PAGE_INDEX+number-1]
            if page.rect.width < 200 or page.rect.height < 300:
                raise RuntimeError("Bad print page geometry: " + str(number))
            pixmap = page.get_pixmap(matrix=fitz.Matrix(1.8,1.8), colorspace=fitz.csRGB, alpha=False)
            image = Image.frombytes("RGB", [pixmap.width,pixmap.height], pixmap.samples)
            path = mushafdir / f"{number:03}.webp"
            image.save(path,format="WEBP",quality=87,method=5)
            if path.stat().st_size < 4500:
                raise RuntimeError("Unexpected empty mushaf page: " + str(number))
            if number in (1,2,3,602,603,604) or number % 100 == 0:
                print("Mushaf page prepared",number,"source PDF page",PDF_FIRST_PAGE_INDEX+number,"size",path.stat().st_size,flush=True)
    page_images=sorted(mushafdir.glob("???.webp"))
    audio_files=sorted(audiodir.glob("???.opus"))
    if len(page_images)!=604 or len(audio_files)!=114:
        raise RuntimeError("Incomplete APK assets: %d pages, %d surahs" % (len(page_images),len(audio_files)))
    # Do not invent timing data. A blank page-audio-index remains a deliberate guard.
    page_cues = json.loads((DEST/"quran"/"page-audio-index.json").read_text(encoding="utf-8"))
    if page_cues.get("pages") != {}:
        raise RuntimeError("Page cue metadata must be independently verified before embedding.")
    report={
        "package":"Dini Asistan offline test build",
        "mushaf_source_pages":640,
        "page_images":604,
        "page_source_index_base_zero":PDF_FIRST_PAGE_INDEX,
        "page_mapping_provisional":True,
        "audio_complete_surahs":114,
        "audio_shas_verified":True,
        "page_bounded_tilavet_ready":False,
        "audio_distribution_rights_for_mirror_verified":False,
        "app_downloads_during_use":False,
        "note":"All Arabic surah audio is embedded. Page-specific playback blocked until word-accurate timestamps are verified. The 604 printed-page mapping and third-party mirror rights still need independent verification."
    }
    (DEST/"source-package-status.json").write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding="utf-8")
    print("PACKAGED: 604 Mushaf image pages and 114 Ogg Opus recitations into app/src/main/assets",flush=True)
    print("Total source audio GB:",round(sum(f.stat().st_size for f in audio_files)/1e9,3),flush=True)

if __name__=="__main__":
    try:main()
    except Exception as ex:
        print("OFFLINE PACK FAILED:",repr(ex),file=sys.stderr,flush=True)
        raise
