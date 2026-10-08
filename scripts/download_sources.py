#!/usr/bin/env python3
"""Kral Fehd kaynaklarına dayanan çevrimdışı APK için kaynak arşivleri indir.
Doğrulama başarısızsa işlem başarısız olur, eksik içeriği 'tam' diye sunmaz.
"""
import hashlib
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path
from urllib.parse import urljoin, urlparse

import requests
from bs4 import BeautifulSoup
from pypdf import PdfReader

ROOT = Path("offline-sources")
ROOT.mkdir(exist_ok=True)
MUSHAF_URL = "https://archive.org/download/MushafMadinaHafsGreen1441/MushafMadinaHafsGreen1441.pdf"
HAFS_CATEGORY = "https://qurancomplex.gov.sa/category/kfgqpc-quran-audio/recite/hafs/"
headers = {"User-Agent": "DiniAsistan/0.1 offline-asset-preparation (GitHub Actions)"}
session = requests.Session()
session.headers.update(headers)


def page(url):
    res = session.get(url, timeout=45)
    res.raise_for_status()
    return BeautifulSoup(res.text, "html.parser")


def looks_like_maher(value):
    low = value.casefold()
    return ("ماهر" in low and ("المعيقلي" in low or "المعيقلى" in low)) or ("maher" in low and ("muai" in low or "moai" in low or "mua" in low))


def discover_maher_page():
    urls = [HAFS_CATEGORY, "https://qurancomplex.gov.sa/category/moratal/hafs/"]
    for url in urls:
        try:
            soup = page(url)
            for a in soup.find_all("a", href=True):
                href = urljoin(url, a["href"])
                ctx = a.get_text(" ", strip=True) + " " + str(a.parent.get_text(" ", strip=True))[:250]
                if looks_like_maher(ctx) and href.startswith("https://qurancomplex.gov.sa/") and href != url:
                    try:
                        candidate = page(href)
                        txt = candidate.get_text(" ", strip=True)
                        if looks_like_maher(txt) and ("تحميل سور" in txt or "Download whole Surahs" in txt):
                            return href, candidate
                    except requests.RequestException:
                        continue
        except requests.RequestException as e:
            print("Kaynak liste erişilemedi:", url, e, flush=True)
    raise RuntimeError("Mahir el-Muaykılî için resmî sure indirme sayfası otomatik bulunamadı.")


def discover_sura_url():
    override = os.environ.get("MAHER_SURAHS_URL", "").strip()
    if override:
        return override, "Kullanıcı tarafından verilen doğrudan arşiv bağlantısı"
    source_page, soup = discover_maher_page()
    candidates = []
    for a in soup.find_all("a", href=True):
        href = urljoin(source_page, a["href"])
        label = a.get_text(" ", strip=True)
        parent = a.parent.get_text(" ", strip=True)[:300] if a.parent else ""
        lower = (label + " " + parent).casefold()
        if any(w in lower for w in ("تحميل سور المصحف", "تحميل السور", "download whole surahs", "سور المصحف")):
            if href.startswith("http") and href != source_page:
                candidates.append(href)
    if not candidates:
        # Bazı WordPress temaları linkin metnini gizler; arşiv uzantısı da denenebilir.
        for a in soup.find_all("a", href=True):
            href = urljoin(source_page, a["href"])
            if re.search(r"\.(zip|rar|7z)(?:\?|$)", href, re.I):
                candidates.append(href)
    candidates = list(dict.fromkeys(candidates))
    if len(candidates) != 1:
        raise RuntimeError(f"Resmî arşiv sayfasında tekil sure paketi belirlenemedi ({len(candidates)} aday): {source_page}. MAHER_SURAHS_URL gerekli.")
    return candidates[0], source_page


def download(url, path, minimum_bytes):
    urlparse_result = urlparse(url)
    if urlparse_result.scheme != "https":
        raise RuntimeError("Yalnızca HTTPS bağlantıları kabul ediliyor")
    partial = path.with_suffix(path.suffix + ".part")
    for attempt in range(3):
        try:
            digest = hashlib.sha256()
            size = 0
            with session.get(url, timeout=(40, 180), stream=True, allow_redirects=True) as response:
                response.raise_for_status()
                if "text/html" in response.headers.get("content-type", "").lower():
                    raise RuntimeError(f"Dosya yerine HTML geldi: {response.url}")
                with partial.open("wb") as fh:
                    for chunk in response.iter_content(chunk_size=1024 * 1024):
                        if chunk:
                            size += len(chunk)
                            if size > 3 * 1024**3:
                                raise RuntimeError("3 GiB indirme sınırı aşıldı")
                            fh.write(chunk)
                            digest.update(chunk)
            if size < minimum_bytes:
                raise RuntimeError(f"Arşiv beklenenden küçük ({size} bayt)")
            partial.replace(path)
            return {"download_url": url, "size_bytes": size, "sha256": digest.hexdigest()}
        except (requests.RequestException, RuntimeError) as e:
            print("İndirme hatası:", repr(e), "deneme", attempt + 1, flush=True)
            partial.unlink(missing_ok=True)
            if attempt == 2:
                raise
            time.sleep(5 * (attempt + 1))


def audio_tracks(path):
    listing = subprocess.run(["7z", "l", "-slt", str(path.resolve())], check=True, capture_output=True, text=True, errors="replace")
    names = re.findall(r"(?m)^Path = (.+\.mp3)\s*$", listing.stdout, re.I)
    return list(dict.fromkeys(names))


def main():
    metadata = {
        "publication": "King Fahd Glorious Quran Printing Complex",
        "mushaf_edition": "Madinah Mushaf 1441H - Hafs - raw publisher PDF mirror",
        "audio_reciter": "Maher Al-Muaiqly - Hafs",
        "usage_rights_audio": "https://qc-dev.qurancomplex.gov.sa/quran-audios/",
        "usage_rights_mushaf": "https://dm.qurancomplex.gov.sa/copyright/",
    }

    pdf_path = ROOT / "madinah-mushaf-hafs-1441-source.pdf"
    metadata["mushaf"] = download(MUSHAF_URL, pdf_path, 30_000_000)
    with pdf_path.open("rb") as fh:
        if fh.read(4) != b"%PDF":
            raise RuntimeError("İndirilen mushaf dosyası PDF değil")
    metadata["mushaf"]["pdf_pages"] = len(PdfReader(str(pdf_path)).pages)
    print("Mushaf ham PDF sayfa sayısı:", metadata["mushaf"]["pdf_pages"], flush=True)
    if not 604 <= metadata["mushaf"]["pdf_pages"] <= 700:
        raise RuntimeError("Beklenmeyen mushaf PDF sayfa sayısı")

    audio_url, detail_page = discover_sura_url()
    archive_path = ROOT / "maher-hafs-complete-surahs.archive"
    metadata["audio"] = download(audio_url, archive_path, 200_000_000)
    metadata["audio"]["publisher_page"] = detail_page
    tracks = audio_tracks(archive_path)
    metadata["audio"]["mp3_file_count"] = len(tracks)
    if len(tracks) != 114:
        raise RuntimeError(f"114 sure doğrulanamadı, MP3 sayısı: {len(tracks)}")
    metadata["audio"]["sample_file_names"] = tracks[:3]

    (ROOT / "SOURCE-MANIFEST.json").write_text(json.dumps(metadata, indent=2, ensure_ascii=False), encoding="utf-8")
    print("Kaynak kontrolü başarılı: ses dosyaları 114 adet, mushaf kaynak PDF kaydedildi.", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"HAZIRLIK BAŞARISIZ: {exc}", file=sys.stderr, flush=True)
        sys.exit(1)
