# Bağlantıdan MP3 dönüştürücü servisi

Android uygulamasındaki **Bağlantıdan MP3** ekranı, yetkili bir YouTube video bağlantısını bu sunucuya HTTPS üzerinden gönderir. Sunucu yt-dlp + FFmpeg ile MP3 üretir. İşlem internet bağlantısı gerektirir.

## Kurulum (Docker)

1. mp3_service klasöründe: docker build -t dini-mp3 .
2. En az 16 karakterlik güçlü ve gizli bir API anahtarı oluştur.
3. Sunucuda: docker run --rm -p 127.0.0.1:8080:8080 -e DINI_MP3_API_KEY=RASTGELE_EN_AZ_16_KARAKTER dini-mp3
4. Caddy veya nginx ile servisi HTTPS üzerinden yayımla. 8080 portunu doğrudan internete açma.
5. Android MP3 ekranına HTTPS sunucu adresini (ör. https://mp3.example.com), API anahtarını ve video bağlantısını gir.

API: GET /health ve POST /convert (Bearer API key, JSON gövde: {"url":"https://youtu.be/VIDEO_ID"}). Başarı: Content-Type audio/mpeg.

Sadece tekil HTTPS YouTube bağlantıları desteklenir; playlistler, özel, yaş kısıtlı veya erişime kapalı içerikler çalışmayabilir. YouTube indirmeleri zaman içinde değişebilir. Bu araç yalnızca indirme hakkınız olan içerikler için kullanılmalıdır.

## Test

mp3_service klasöründe: python -m unittest -v test_app

Testler ağdan gerçek video indirmez; URL doğrulama, yetkilendirme, hata yanıtları ve sahte MP3 aktarımını kontrol eder. Gerçek YouTube indirmesi, cihaz kurulumu ve ses kalitesi testi ayrıca gereklidir.

**Önemli:** GitHub kod deposudur; sunucuyu otomatik olarak barındırmaz. Sunucu kurulmadıkça uygulama linki MP3'e çeviremez. Mushaf/tilavet bölümleri çevrimdışı kalır.
