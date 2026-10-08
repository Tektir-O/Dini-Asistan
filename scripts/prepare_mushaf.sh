#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/app/src/main/assets/mushaf/pages"
MANIFEST="$ROOT/app/src/main/assets/mushaf/page-sha256.txt"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
git clone --quiet --depth 1 https://github.com/sufone/medina-mushaf.git "$TMP/source"
mkdir -p "$DEST"
: > "$MANIFEST"
for i in $(seq 1 604); do
  original="$TMP/source/png-d150/$i.png"
  test -s "$original" || { echo "Mushaf sayfası eksik: $i"; exit 1; }
  printf -v number '%03d.png' "$i"
  cp "$original" "$DEST/$number"
  (cd "$DEST" && sha256sum "$number") >> "$MANIFEST"
done
test "$(find "$DEST" -name '*.png' -type f | wc -l)" -eq 604
echo "604 Mushaf sayfası kurulum paketine eklendi."
