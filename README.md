# Dini Asistan — internetsiz Kur'an uygulaması

Bu GitHub deposu, internetsiz çalışan Android APK için yeniden başlatıldı.

## Hedef
- Medine Mushafı (Hafs): **604 matbu sayfa**.
- Mahir el-Muaykılî tilaveti (Hafs): **114 tam sure**.
- Uygulama ilk açılışından itibaren **internetsiz** çalışacak. Sonradan içerik indirmeyecek.

## Kaynak indirme

Mevcut doğrulanmış kaynak paketi: [Actions kaynak paketi (ZIP)](https://github.com/Tektir-O/Dini-Asistan/actions/runs/37764358206/artifacts/11543808346). Kaynaklar sınırlı süre saklanır ve indirmek için GitHub oturumu gerekebilir.

Kapsamlı ses/PDF test raporu: [114 sure ve mushaf teknik doğrulaması](https://github.com/Tektir-O/Dini-Asistan/actions/runs/37770329111/artifacts/11548261700).
[GitHub Actions kaynak hazırlama işlemi](https://github.com/Tektir-O/Dini-Asistan/actions/workflows/prepare-offline-sources.yml)

İndirme `.github/workflows/prepare-offline-sources.yml` ile çalışır; gömülü APK derlemesi `.github/workflows/android-offline-all.yml` ile yapılır. Mushaf PDF'si ayrı; 114 sure üç grupta indirilir, her MP3 doğrulanır ve APK'ya uygun Ogg Opus ses biçimine çevrilir. Eksik sure varsa işlem başarısız sayılır. Kontrolden geçen dosyalar GitHub Actions **Artifacts** bölümüne kaynak paketi olarak yüklenir. GitHub Releases yetki engeli (HTTP 403) nedeniyle kullanılmıyor.

**Kaynak ve haklar:** Resmî Kur'an Basım Kompleksi ses sunucusu erişilemediğinden Haramain Recordings tarafından yüklenmiş, Kral Fehd Kompleksi kayıtları olarak tanıtılmış [Internet Archive aynası](https://archive.org/details/HaramainMaahir) kullanılıyor. Bu aynanın resmî izinli nüshayla tam aynılığı **bağımsız olarak henüz doğrulanmadı**. APK'yı halka dağıtmadan önce doğrulanması gerekir.

**Mushaf kaynağı:** 640 PDF sayfalı ham baskı; bunun içindeki gerçek 604 mushaf sayfası **henüz ayrıştırılıp doğrulanmadı**. 640 sayfalı PDF ile 604 matbu sayfa aynı değildir.

**Durum:** 8 Ekim 2026 tarihinde tam 604 görsel sayfası ve 114 surenin sesini tek bir internetsiz Android geliştirme APK'sında paketleyen derleme başarıyla tamamlandı. **Sayfaya özel ses zamanları ve resmî yayın izni eşleşmesi henüz tamamlanmadı.** APK: [GitHub Actions indirme](https://github.com/Tektir-O/Dini-Asistan/actions/runs/37789860388/artifacts/11556237925). Ayrıntılar: [ANDROID_README.md](ANDROID_README.md).

Ayrıntılar: [assets/README.md](assets/README.md).

---

## Bağlantıdan MP3 (isteğe bağlı çevrimiçi özellik)

Ana menüde **Bağlantıdan MP3** ekranı eklendi. Uygulama, indirme hakkı olan tekil YouTube video bağlantısını ayrı bir HTTPS sunucusuna gönderip MP3 dosyasını Android İndirilenler/DiniAsistan klasörüne kaydetmeyi hedefler (Android 9 ve öncesinde uygulamaya özel müzik klasörü). Çeviri sunucusu [mp3_service](mp3_service/README.md) içindedir ve Docker ile ayrıca kurulmalıdır. Sunucu kurulmadan bu özellik çalışmaz. Gerçek video indirebilme başarısı garanti edilmez.

**Kur’an’ın çevrimdışı yapısı değişmedi:** Mushaf/tilavet dosyaları yine APK içinden okunur; ağ izni yalnızca isteğe bağlı bağlantıdan MP3 özelliği için eklendi.

Otomatik HTTP ve Android birim testleri vardır, ancak fiziksel telefon + canlı YouTube dönüştürme testi ayrıca gereklidir.
