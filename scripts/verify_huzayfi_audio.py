#!/usr/bin/env python3
"""Block audio release when any of the 114 local recordings is missing or corrupt."""
import hashlib
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DIRECTORY = ROOT / "app/src/main/assets/audio-ar/huzayfi-hafs"
MANIFEST = DIRECTORY / "manifest.json"

def sha256(path):
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

def verify():
    if not MANIFEST.is_file():
        raise ValueError("Official offline audio manifest missing.")
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    if manifest.get("reciter_id") != "huzayfi-hafs" or manifest.get("riwayah") != "Hafs":
        raise ValueError("Reciter or riwayah metadata mismatch.")
    if manifest.get("recording_count") != 114:
        raise ValueError("Expected 114 surahs.")
    if manifest.get("ayah_timing_verified") is not False:
        raise ValueError("Surah recordings cannot falsely claim ayah timing.")
    recordings = manifest.get("recordings", [])
    if len(recordings) != 114:
        raise ValueError("Not exactly 114 entries in the manifest.")
    discovered = list(DIRECTORY.glob("*.ogg"))
    if len(discovered) != 114:
        raise ValueError(f"Expected 114 audio files; found {len(discovered)}.")
    for number, entry in enumerate(recordings, start=1):
        expected = f"{number:03d}.ogg"
        if entry.get("surah") != number or entry.get("filename") != expected:
            raise ValueError(f"Surah order/filename mismatch at {number}.")
        audio = DIRECTORY / expected
        if not audio.is_file() or audio.stat().st_size < 4096:
            raise ValueError(f"Missing or suspicious recording: {expected}")
        if sha256(audio) != entry.get("bundle_sha256"):
            raise ValueError(f"SHA-256 mismatch: {expected}")
        probe = subprocess.run(
            ["ffprobe", "-v", "error", "-select_streams", "a:0",
             "-show_entries", "stream=codec_name", "-of", "default=nw=1:nk=1",
             str(audio)],
            capture_output=True, text=True, check=True)
        if probe.stdout.strip() != "opus":
            raise ValueError(f"Unplayable/wrong codec: {expected}")
    print("Verified Huzayfi Hafs 114/114 Ogg-Opus offline recordings and checksums.")

if __name__ == "__main__":
    try:
        verify()
    except Exception as exc:
        print("AUDIO CHECK FAILED:", exc, file=sys.stderr)
        sys.exit(1)
