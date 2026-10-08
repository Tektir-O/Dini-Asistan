#!/usr/bin/env python3
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from urllib.request import Request, urlopen
import time

COUNT = 604
OUT = Path("app/src/main/assets/mushaf")
OUT.mkdir(parents=True, exist_ok=True)

SOURCES = [
    "https://raw.githubusercontent.com/SakinaDevGroup/mushaf-madani-cdn/main/light/p{page}.png",
    "https://cdn.jsdelivr.net/gh/SakinaDevGroup/mushaf-madani-cdn@main/light/p{page}.png",
]

PNG = b"\x89PNG\r\n\x1a\n"

def valid(path: Path) -> bool:
    if not path.exists() or path.stat().st_size < 5000:
        return False
    with path.open("rb") as f:
        return f.read(8) == PNG

def fetch(page: int):
    target = OUT / f"p{page}.png"
    if valid(target):
        return page, target.stat().st_size, "cached"

    last_error = None
    for attempt in range(3):
        for template in SOURCES:
            url = template.format(page=page)
            try:
                req = Request(url, headers={"User-Agent": "Dini-Asistan-Build/1.0"})
                with urlopen(req, timeout=45) as response:
                    data = response.read()
                if len(data) < 5000 or not data.startswith(PNG):
                    raise RuntimeError("invalid PNG response")
                tmp = target.with_suffix(".tmp")
                tmp.write_bytes(data)
                tmp.replace(target)
                return page, len(data), url
            except Exception as exc:
                last_error = exc
        time.sleep(1 + attempt)

    raise RuntimeError(f"page {page} download failed: {last_error}")

with ThreadPoolExecutor(max_workers=16) as pool:
    futures = [pool.submit(fetch, page) for page in range(1, COUNT + 1)]
    done = 0
    total = 0
    for future in as_completed(futures):
        page, size, source = future.result()
        done += 1
        total += size
        if done % 50 == 0 or done == COUNT:
            print(f"{done}/{COUNT} pages ready")

print(f"All {COUNT} pages ready, {total / 1024 / 1024:.1f} MiB")
