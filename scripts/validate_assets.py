#!/usr/bin/env python3
from pathlib import Path
import json
import sys

root = Path("app/src/main/assets/mushaf")
expected = [root / f"p{i}.png" for i in range(1, 605)]
missing = [str(p) for p in expected if not p.exists()]
bad = []

for p in expected:
    if p.exists():
        data = p.read_bytes()[:8]
        if p.stat().st_size < 5000 or data != b"\x89PNG\r\n\x1a\n":
            bad.append(str(p))

extras = [p.name for p in root.glob("*.png") if p.name not in {f"p{i}.png" for i in range(1,605)}]

metadata_path = Path("app/src/main/assets/surahs.json")
try:
    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
except Exception as exc:
    print(f"Invalid surahs.json: {exc}")
    sys.exit(1)

metadata_ok = (
    isinstance(metadata, list)
    and len(metadata) == 114
    and all(
        isinstance(x, dict)
        and x.get("number") == i + 1
        and isinstance(x.get("name"), str)
        and isinstance(x.get("page"), int)
        and 1 <= x["page"] <= 604
        for i, x in enumerate(metadata)
    )
)

if missing or bad or extras or not metadata_ok:
    print("Asset validation failed")
    if missing:
        print("Missing:", missing[:20], "..." if len(missing) > 20 else "")
    if bad:
        print("Bad PNG:", bad[:20], "..." if len(bad) > 20 else "")
    if extras:
        print("Unexpected PNG:", extras[:20])
    if not metadata_ok:
        print("surahs.json must contain exactly 114 ordered entries with pages 1..604")
    sys.exit(1)

size = sum(p.stat().st_size for p in expected)
print(f"Validated 604 PNG pages ({size / 1024 / 1024:.1f} MiB) and 114 surahs.")
