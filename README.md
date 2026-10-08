# Dini Asistanım

Türkçe arayüzlü, internet olmadan kullanılabilen Android Kur'an okuyucusu.

## İlk geliştirme sürümü v0.1

Uygulama şu özellikleri hedefleyen gerçek Android kaynak kodunu içerir:
- Doğrudan ana ekran: Başlıklar, Kategoriler, Kur'an
- Medine Mushafı: 604 PNG sayfa, telefonla paketlenen yerel dosyalar
- Sayfa ileri / geri ve iki parmakla yakınlaştırma
- 114 sure ve 30 cüz listesi
- Sure numarası + ayet numarasıyla ilgili sayfayı açma
- Kaldığın sayfayı ve yer imini cihazda hatırlama
- Tek Arapça okuyucu: Ali el-Huzeyfi (Hafs); resmî ses arşivi hazırlandığında dinleme açılacak
- Tek Türkçe meal hedefi: QuranEnc, Rowad Tercüme Merkezi (v1.0.4); lisans koşulları doğrulanmıştır, henüz içerik derlemesi yapılmadı

## APK

GitHub Actions sayfasındaki Dini Asistanim APK iş akışı kurulum dosyası üretir. Başarılı çalıştırmada Dini-Asistanim-Debug-APK adlı artifact bulunur. Derleme sırasında kaynak Mushaf sayfaları ve ayet/sayfa indeksi APK içine yerleştirilir. Telefon uygulaması bu içerikler için indirme yapmaz ve internet izni istemez.

Geliştirme ortamı: Java 17, Android SDK 35, Gradle 8.11.1, Node 22.

Yerel derleme adımları:
1. npm install
2. npm run generate:index
3. bash scripts/prepare_mushaf.sh
4. gradle :app:assembleDebug

## Önemli

Bu bir ilk geliştirme sürümüdür. Bir resmî okuyucunun tam ses paketi, tek Türkçe mealin çevrimdışı içeriği, ayet dokunma/kelime takibi, zamanlayıcı ve TikTok için MP4 paylaşımı henüz eklenmedi. Çalışmayan özellikler kullanıcıya çalışıyormuş gibi gösterilmez.

Mushaf görselleri: sufone/medina-mushaf, png-d150 dizini.
Sure/ayet/sayfa verisi: quran-center/quran-meta v7.0.0 (Hafs).
Üçüncü taraf görsel, meal ve seslerin dağıtım izni yayından önce ayrıca doğrulanmalıdır.

Hedef: Kullanıcı için tek kurulum, tamamen çevrimdışı kullanım. Önce bir okuyucu + bir meal + 604 sayfalık Mushafın eksiksizliği ve paylaşım izinleri doğrulanacak. Kaynaklar ve kullanım sınırları için CONTENT_RIGHTS.md dosyasına bakın.
