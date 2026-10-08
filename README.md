# Dini Asistan — Çevrimdışı Kur'an

Native Android Kur'an uygulaması. Uygulama çalışırken internet kullanmaz.

## Özellikler

- Standart 604 sayfalık Medine Mushafı
- Mushaf sayfaları APK içine gömülür
- 1–604 doğrudan sayfa geçişi
- Önceki / sonraki sayfa
- Kaydırma hareketiyle sayfa değiştirme
- 114 surelik yerel sure listesi ve sure başlangıç sayfasına gitme
- Son okunan sayfayı cihazda hatırlama
- AndroidManifest içinde INTERNET izni yok
- WebView, PWA, service worker veya çevrimiçi API yok

## Mushaf görselleri

Build sırasında 604 PNG, SakinaDevGroup/mushaf-madani-cdn deposundaki `light/p1.png` … `light/p604.png` dosyalarından alınır ve APK'nın `assets/mushaf/` dizinine gömülür.

Kaynak:
https://github.com/SakinaDevGroup/mushaf-madani-cdn

Görseller KFGQPC tabanlı Medine Mushafı düzenidir. Yeniden dağıtım/yayın öncesinde ilgili KFGQPC kullanım şartları ayrıca kontrol edilmelidir.

## Yerel geliştirme

Önce Mushaf dosyalarını indir:

```bash
python3 scripts/download_mushaf.py
python3 scripts/validate_assets.py
```

Ardından Android Studio ile aç veya Gradle ile:

```bash
gradle :app:assembleDebug
```

GitHub Actions her `main` push'unda indirilebilir `Dini-Asistan-offline.apk` artifact'i üretir.
