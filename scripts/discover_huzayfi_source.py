#!/usr/bin/env python3
"""Inspect official King Fahd Quran Complex Huzayfi/Hafs pages for legitimate archive URLs.

Diagnostic-only. Never downloads, redistributes or substitutes recordings.
"""
import html
import re
import urllib.request
from html.parser import HTMLParser
from urllib.parse import urljoin, urlparse


SOURCES = [
    "https://qurancomplex.gov.sa/category/kfgqpc-quran-audio/recite/moratal/hafs/huthify/",
    "https://qc-dev.qurancomplex.gov.sa/quran-audios/",
]
KEYWORDS = ("الحذيفي", "حفص", "سور", "تحميل", "802.21", "huthify", "hudha", "huth", "download", ".zip", "mp3")


class Extract(HTMLParser):
    def __init__(self):
        super().__init__()
        self.anchor = None
        self.links = []

    def handle_starttag(self, tag, attrs):
        if tag == "a":
            props = dict(attrs)
            self.anchor = [props.get("href", ""), []]

    def handle_data(self, data):
        if self.anchor is not None:
            self.anchor[1].append(data)

    def handle_endtag(self, tag):
        if tag == "a" and self.anchor is not None:
            href, parts = self.anchor
            self.links.append((href, " ".join(parts).strip()))
            self.anchor = None


def inspect(url):
    print("PROBING", url, flush=True)
    request = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Quran offline audio source integrity check)"})
    with urllib.request.urlopen(request, timeout=25) as response:
        content = response.read(2_000_000).decode("utf-8", "replace")
    print("HTML_LEN", len(content), flush=True)
    text = re.sub(r"<[^>]*>", " ", content)
    text = html.unescape(re.sub(r"\\s+", " ", text))
    for word in ("802.21", "الحذيفي", "حفص"):
        index = text.find(word)
        if index != -1:
            print("CONTEXT", word, repr(text[max(0, index - 200):index + 550]), flush=True)
    parser = Extract()
    parser.feed(content)
    candidates = []
    for target, label in parser.links:
        if any(k.lower() in (target + " " + label).lower() for k in KEYWORDS):
            candidates.append((urljoin(url, target), label))
    print("CANDIDATE_LINKS", len(candidates), flush=True)
    for href, label in candidates[:70]:
        parsed = urlparse(href)
        if parsed.scheme not in ("https", "http"):
            continue
        print("LINK", repr(label[:110]), href[:350], flush=True)


def main():
    for source in SOURCES:
        try:
            inspect(source)
        except Exception as exc:
            print("PROBE_ERROR", source, repr(exc), flush=True)


    # Historical mirror attributed to the King Fahd Complex recording:
    # https://aelaa.net/Fa/viewtopic.php?f=288&t=690
    mirror = "https://archive.org/metadata/zyfyhfssssddddd"
    print("MIRROR_METADATA", mirror, flush=True)
    try:
        import json
        with urllib.request.urlopen(urllib.request.Request(mirror, headers={"User-Agent": "DiniAsistanim/1.0"}), timeout=18) as response:
            data = json.load(response)
        meta = data.get("metadata", {})
        print("MIRROR_TITLE", repr(meta.get("title")), flush=True)
        print("MIRROR_CREATOR", repr(meta.get("creator")), flush=True)
        print("MIRROR_LICENSE", repr(meta.get("licenseurl")), flush=True)
        files = data.get("files", [])
        print("MIRROR_FILE_COUNT", len(files), flush=True)
        for f in files:
            name = str(f.get("name", ""))
            if name.endswith((".zip", ".mp3")):
                print("MIRROR_FILE", name, f.get("size"), f.get("md5"), flush=True)
                if len(name) > 100:
                    break
    except Exception as exc:
        print("MIRROR_PROBE_ERROR", repr(exc), flush=True)

if __name__ == "__main__":
    main()
