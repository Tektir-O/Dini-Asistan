# Dini Asistan Premium: kaynaklar ve sayfa tilaveti

## Tasarım
Kur'an ekranı zümrüt yeşili-altın çerçeve, 604 sayfalık Mushaf, oynatıcı ve ayet seçimi içerir. Sola kaydırma sonraki sayfaya, sağa kaydırma önceki sayfaya geçer. Mushaf İndir, Tilavet İndir, Meal İndir ve Video Oluştur düğmeleri şimdilik pasiftir.

## Gerçek ayet kayıtları
Tam sure sesinden tahmini saniye bölümü yapılmaz. 114 surenin toplam 6236 ayeti ayrı kaydedilmiş ses dosyalarından alınır. Her ayet, 604 sayfalık Medine Mushafı sayfa başlangıç indeksiyle eşleştirilir. Her sayfa için tek Opus kayıt dosyası ve ayet sınırı indeksleri hazırlanır. Sure dinleme de aynı indekslenmiş sayfa kayıtlarını kullanır, gereksiz ses kopyası yoktur.

Bir ayet iki basılı sayfaya bölünüyorsa, kelime düzeyinde doğru ses hizası olmadan ayet kaydı kesilmez. Ayetin tamamı başladığı sayfanın sesine atanır. Bu sınırlama bilinçlidir; kullanıcı yanlış kesilmiş tilavet duymamalıdır.

## Veri kaynağı ve yeniden dağıtım uyarısı
- Sayfa/ayet başlangıçları: https://github.com/ouryhamdalaye/madinah-mushaf-json (CC BY-SA 4.0). Yukarı kaynağın zonetecde/mushaf-layout için ayrıca bir açık lisans bulunmadığı uyarısı da geçerlidir.
- Mahir el-Muaykılî bağımsız ayet kayıtları: https://everyayah.com/data/Maher_AlMuaiqly_64kbps/
- Mushaf sayfaları: 1441H Medine Mushafı kaynak PDF'sinin 4-607. PDF sayfalarından.
- Üçüncü taraf ses aynasının yayın/yeniden dağıtım hakları henüz kesin doğrulanmadı. Mağaza yayını öncesi izinler kontrol edilmelidir.

## Teknik testler
- Uygulama Android 26+ Kotlin yerel UI; ağ izni yok.
- Her derlemede Kotlin sıra ve sınır testleri.
- 604 sayfa, 114 sure, 6236 benzersiz ayet ve doğru sırayla yerleşim kontrolü.
- MP3 süre başlıkları farklı olabildiğinden birleştirilen Opus klibin gerçek süresine göre ayet aralığı kalibrasyonu.
- 1,2,3,195,254,301,604 gibi kritik sayfalarda medya çözümlenebilirlik denetimi.
- Android emülatöründe APK kurulumu, ekran, ilk ayet sesi ve dört pasif düğmenin testleri.

Derleme: .github/workflows/build-ayah-premium.yml
Hazırlama: scripts/build_ayah_pages.py
Kontrol: scripts/validate_ayah_page_assets.py
