# Tek APK — sesler ve mushaf gömülü geliştirme paketi (8 Ekim 2026)

**[Gömülü 604 görsel sayfa + 114 sure içeren APK indir (GitHub Actions)](https://github.com/Tektir-O/Dini-Asistan/actions/runs/37789860388/artifacts/11556237925)**

GitHub Actions v0.2.0 düzeltme derlemesi başarıyla tamamlandı. Mushaf sağdan sola gezinir: sola kaydır veya soldaki İleri Sayfa düğmesine basınca sayfa numarası artar, sağa kaydır veya sağdaki Geri Sayfa düğmesine basınca azalır. 604 sayfanın süslemeli yazı çerçevesi, metni kırpmadan yatay olarak hizalandı. APK içinde 604 numaralı WebP sayfası, Mahir el-Muaykılî'nin 114 adet Ogg Opus ses dosyası ve internet izni bulunmadığı **teknik olarak** doğrulandı. Tamamı uygulamanın içine gömülü, uygulama açıldıktan sonra ağdan indirme yok. Android'de fiziksel telefonla kurulum/oynatma testi henüz yapılmadı.

**Bu, yayın hakları ve sayfa bazlı tilavet açısından nihai sürüm değildir.**

- Mushaf görselleri, 640 sayfalık 1441H kaynak PDF'nin **4–607** PDF sayfalarından 604 WebP olarak türetilmiştir. İlk ve son basılı sayfa eşleşmesi ayrıca görsel ve içerik denetimi gerektirir.
- 114 surenin sesleri tam kayıttır ve **Tilavet** ekranından çevrimdışı oynatılabilir.
- Mushaf sayfasındaki **Başlat** düğmesi, o sayfaya karşılık gelen **doğrulanmış ses zaman aralıkları eksikse** çalmayı reddeder. Sure kaydından tahmini bölüm kesmez. Bu nedenle **tek sayfayı tam okuma işlevi henüz tamamlanmadı**.
- Kral Fehd Kompleksi resmî ses kayıtlarına ücretsiz uygulama kullanım izni veriyor; kullanılan **Haramain/Internet Archive aynasındaki dosyaların o resmî kayıtlarla birebir aynı olduğu henüz doğrulanmadı**. Üçüncü taraf aynadaki sesler için yeniden dağıtım/yayın hakkı kesinleşmeden APK'nın halka açık yayınında hak iddia edilmemelidir.
- Bu APK **debug imzalı bir test sürümüdür**, imzalı mağaza sürümü değildir.

**Kurulum:** GitHub Actions bağlantısındaki ZIP dosyasını indir, ZIP içindeki `app-debug.apk` dosyasını Android telefona çıkar ve kur. Büyük APK için cihazında birkaç GB boş alan bırak. GitHub oturum açman gerekebilir.

---

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

Gömülü APK'da 604 görsel sayfa ve 114 sure bulunuyor. Ancak mushaf sayfa indeksinin görsel denetimi, sayfaya özel ses başlangıç/bitiş zamanları ve yayın haklarının resmî kaynakla birebir eşleştirilmesi henüz tamamlanmadı.

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
