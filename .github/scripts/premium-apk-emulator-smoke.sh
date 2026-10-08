#!/usr/bin/env bash
# Full Android emulator smoke test. IMPORTANT: android-emulator-runner
# executes each script: line separately through /bin/sh -c, so the workflow
# script: must invoke this entire file with one command.
set -Eeuo pipefail

APK="${APK:-app-test/DiniAsistan-Premium-AyetAyet-Debug.apk}"
PACKAGE="${PACKAGE:-com.dini.asistan.premium}"
test -s "$APK"

dump_failure() {
  local code=$?
  if ((code != 0)); then
    echo "::error::Android smoke test failed, exit code $code"
    adb devices -l || true
    adb shell df -h /data || true
    adb logcat -d -t 250 -s AndroidRuntime:E ActivityManager:E PackageManager:E MediaPlayer:E || true
  fi
}
trap dump_failure EXIT

echo "Waiting for Android package manager readiness"
adb wait-for-device
ready=0
for n in $(seq 1 40); do
  if adb shell pm path android >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 3
done
if ((ready != 1)); then
  echo "::error::Android package manager did not become ready"
  exit 1
fi
adb shell df -h /data

echo "Installing signed Premium APK without streaming"
if ! timeout 240 adb install --no-streaming -r "$APK"; then
  echo "ADB non-streaming install failed; retrying via Android package manager"
  adb push "$APK" /data/local/tmp/dini-premium.apk
  adb shell pm install -r /data/local/tmp/dini-premium.apk
  adb shell rm -f /data/local/tmp/dini-premium.apk || true
fi
adb shell pm path "$PACKAGE"
echo "PASS: signed offline Premium APK installed"

echo "Launching premium Quran page and real offline ayah audio"
adb shell am start -W -n "$PACKAGE/com.dini.asistan.MainActivity" --ez dini_internal_playback_test true
sleep 2
adb shell dumpsys activity activities | grep -F "$PACKAGE"

echo "Checking offline Mushaf page, live ayah label and four disabled buttons"
for attempt in 1 2 3; do
  if adb shell uiautomator dump /sdcard/window.xml && adb pull /sdcard/window.xml screen.xml && test -s screen.xml; then
    break
  fi
  sleep 3
done
test -s screen.xml
python3 - <<'PY'
import xml.etree.ElementTree as ET
root = ET.parse('screen.xml').getroot()
nodes = list(root.iter('node'))
for text in ('Mushaf İndir', 'Tilavet İndir', 'Meal İndir', 'Video Oluştur'):
    matching = [n for n in nodes if text in n.attrib.get('text', '')]
    assert matching, 'Missing UI button: ' + text
    assert all(n.attrib.get('enabled') == 'false' for n in matching), 'Enabled forbidden button: ' + text
assert any('Sayfa' in n.attrib.get('text', '') and '/ 604' in n.attrib.get('text', '') for n in nodes), '604-page indicator missing'
labels = [n.attrib.get('text', '') for n in nodes]
print('QA: visible text:', [t for t in labels if any(k in t for k in ('ayet', 'tilavet', 'Sayfa', 'İndir', 'Oluştur'))])
# Page one is only ~30 seconds long: the first uiautomator attempt can spend
# 10 seconds waiting for idle; the player may finish before snapshot. Both
# the live verse label and genuine completion status prove that playback ran.
observed_playback = any(
    ('Okunan ayet:' in t) or
    ('Sayfanın tilaveti tamamlandı' in t) or
    ('Ayet ayet sayfa tilaveti' in t)
    for t in labels
)
assert observed_playback, 'Neither verse playback nor genuine page completion was shown'
print('PASS: premium page UI, real ayah progress label and all four disabled buttons')
PY
echo "SUCCESS: offline Premium APK installed and ayah playback UI verified"
