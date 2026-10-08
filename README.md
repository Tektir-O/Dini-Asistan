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
- Erkek / bayan okuyucu seçim arayüzü (henüz ses çalmıyor)
- Türkçe Meal ekranı (mealler doğrulanana kadar bilgilendirme gösterir)

## APK

GitHub Actions sayfasındaki Dini Asistanim APK iş akışı kurulum dosyası üretir. Başarılı çalıştırmada Dini-Asistanim-Debug-APK adlı artifact bulunur. Derleme sırasında kaynak Mushaf sayfaları ve ayet/sayfa indeksi APK içine yerleştirilir. Telefon uygulaması bu içerikler için indirme yapmaz ve internet izni istemez.

Geliştirme ortamı: Java 17, Android SDK 35, Gradle 8.11.1, Node 22.

Yerel derleme adımları:
1. npm install
2. npm run generate:index
3. bash scripts/prepare_mushaf.sh
4. gradle :app:assembleDebug

## Önemli

Bu bir ilk geliştirme sürümüdür. Yedi Arapça okuyucunun tam sesleri, beş mealin gerçek metin ve sayfaları, kadın/erkek Türkçe seslendirmeler, ayet dokunma/kelime takibi, zamanlayıcı ve TikTok için MP4 paylaşımı henüz kodlanmadı. Çalışmayan özellikler kullanıcıya çalışıyormuş gibi gösterilmez.

Mushaf görselleri: sufone/medina-mushaf, png-d150 dizini.
Sure/ayet/sayfa verisi: quran-center/quran-meta v7.0.0 (Hafs).
Üçüncü taraf görsel, meal ve seslerin dağıtım izni yayından önce ayrıca doğrulanmalıdır.

Hedef: Kullanıcı için tek kurulum, tamamen çevrimdışı kullanım. Önce küçük prototipler gerçek Android cihazlarda doğrulanacak; tam paket yalnızca zorunlu 7 Arapça okuyucu ve 5 mealin bütünlüğü kesinleştiğinde yayımlanacaktır.
