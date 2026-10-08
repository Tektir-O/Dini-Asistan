# Dini Asistan — Android uygulaması

## İlk sürümün hazır işlevleri

- Türkçe **Ana Menü** ve **Kategoriler → Kur’an** kartı
- Ekranın altında **Ana Menü / Kur’an / Ayarlar** gezinme çubuğu
- Mushaf okuyucu altyapısı, 1–604 sayfa ileri/geri, kaldığı sayfayı hatırlama
- Sayfa ses kontrolleri: **Başlat**, **Duraklat**, **Durdur**
- Sayfa sona erince **otomatik olarak durma**; sonraki sayfaya geçmez
- Sonraki/önceki sayfa düğmesine basıldığında oynatıcı durur
- İnternet izni yok. Dosyalar yalnızca APK'nın içinden okunur
- Sayfa görselleri belleğe uygun çözünürlükte açılır

## Önemli: içerik henüz gömülmedi

Bu ilk derlenebilir Android **altyapısıdır**, nihai tam APK **değildir**. Ses ve görüntülerin hepsinin dosyaları doğrulama aşamasında; büyük arşiv GitHub Actions Artifacts içinde saklanır. UI'daki sayfaları ve sesleri etkinleştirmek için yayın hakkı ve sayfa eşleştirmesi doğrulanmış dosyalar gerekir.

Dosya düzeni:

```
app/src/main/assets/
  mushaf/001.webp ... 604.webp           # DOĞRULANMIŞ basılı sayfalar
  audio/surah/001.opus ... 114.opus       # lisansı doğrulanmış surah sesleri
  quran/page-audio-index.json            # 604 sayfaya ait doğru ses zamanları
```

Ses zamanlama dosyası biçimi:

```json
{
  "schemaVersion": 1,
  "reciter": "maher-al-muaiqly-hafs",
  "pages": {
    "1": [
      {"surah": 1, "fromMs": 0, "toMs": 1000}
    ]
  }
}
```

**Bu örnekteki 1000ms gerçek sure süresi değildir.** Yalnızca JSON biçimini göstermek içindir. Zamanları tahmin ederek kullanmayın; ayet sınırları doğrulanmalı. Aynı sayfa birden fazla sure içerdiğinde dizide birden çok aralık olur. Kod sayfa bitiminde durur ve asla otomatik sonraki sayfaya geçmez.

Daha da önemlisi, kimi ayetler iki matbu sayfaya yayılır. Dolayısıyla yalnızca ayet başlangıç/bitişleri ile gerçek sayfa sonu her zaman birebir bulunmaz. Bu durumda kelime/zaman hizalama verisi gerekir.

**Kaynak PDF'de 640 sayfa** bulunduğu doğrulandı. Basılı 604 sayfanın bu PDF'deki yerleri ayrı bir görsel denetimden geçmeden 604 adet webp üretip "tam mushaf" denmeyecek.

## GitHub'da derleme

[Android APK Actions iş akışı](https://github.com/Tektir-O/Dini-Asistan/actions/workflows/android-build.yml)

Bu iş akışı:
1. Android SDK ve Gradle yükler,
2. Kotlin birim testlerini çalıştırır,
3. Android debug APK üretir,
4. manifest içinde `android.permission.INTERNET` olmadığını kontrol eder,
5. APK'yı Actions artifact olarak saklar.

GitHub Releases'a yazma erişimi olmadığı için APK, **Actions → Artifacts** bölümünde bulunur. Bu sürümde sesler ve mushaf görselleri henüz gömülü olmadığı için **tam içerik içermeyen geliştirme APK'sıdır**.

## Yayın izni

- Resmî mushaf: Kral Fehd Kur’an Basım Kompleksi. İzin verilen format, kaynak ve kullanım şartları belgelenmeli.
- Ses aynası Haramain Recordings/Internet Archive üzerinden alınmış. Bunun resmî izinli sesle birebir aynı kayıtlardan oluştuğu henüz kesinleşmedi. **Dağıtım lisansı doğrulanmadan nihai APK yayımlanmamalı.**
