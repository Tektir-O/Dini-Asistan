#!/usr/bin/env python3
"""Reject absent, duplicate, out-of-order or overlapping page playback cues.

A technical consistency check is NOT a guarantee that a particular recitation
contains the precise printed-page word boundaries. Human review is required.
"""
import json
import sys
from pathlib import Path

PATH = Path("app/src/main/assets/quran/page-audio-index.json")
REQUIRE_ALL = "--require-all" in sys.argv
data = json.loads(PATH.read_text(encoding="utf-8"))
pages = data.get("pages", {})
if not isinstance(pages, dict):
    raise SystemExit("pages must be an object")

for p_str, slices in pages.items():
    page = int(p_str)
    if not 1 <= page <= 604 or not isinstance(slices, list) or not slices:
        raise SystemExit(f"Page {p_str}: bad page number or empty cues")
    previous_surah = 0
    previous_to = -1
    for s in slices:
        surah, start, end = s["surah"], s["fromMs"], s["toMs"]
        if (not isinstance(surah, int) or not 1 <= surah <= 114
                or not isinstance(start, int) or not isinstance(end, int)
                or not 0 <= start < end <= 2_147_483_647):
            raise SystemExit(f"Page {p_str}: invalid slice {s}")
        if surah < previous_surah or (surah == previous_surah and start < previous_to):
            raise SystemExit(f"Page {p_str}: out-of-order or overlapping playback")
        previous_surah, previous_to = surah, end

if REQUIRE_ALL and set(map(int, pages)) != set(range(1, 605)):
    raise SystemExit("Not all 604 pages have reviewed audio timing")

print(f"VALID: {len(pages)} page cue entries. Human source alignment and publication-rights review still required.")
