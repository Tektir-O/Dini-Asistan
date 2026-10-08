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

İndirme `.github/workflows/prepare-offline-sources.yml` ile çalışır. Mushaf PDF'si ayrı; 114 sure üç grupta indirilir, her MP3 doğrulanır ve APK'ya uygun Ogg Opus ses biçimine çevrilir. Eksik sure varsa işlem başarısız sayılır. Kontrolden geçen dosyalar GitHub Actions **Artifacts** bölümüne kaynak paketi olarak yüklenir. GitHub Releases yetki engeli (HTTP 403) nedeniyle kullanılmıyor.

**Kaynak ve haklar:** Resmî Kur'an Basım Kompleksi ses sunucusu erişilemediğinden Haramain Recordings tarafından yüklenmiş, Kral Fehd Kompleksi kayıtları olarak tanıtılmış [Internet Archive aynası](https://archive.org/details/HaramainMaahir) kullanılıyor. Bu aynanın resmî izinli nüshayla tam aynılığı **bağımsız olarak henüz doğrulanmadı**. APK'yı halka dağıtmadan önce doğrulanması gerekir.

**Mushaf kaynağı:** 640 PDF sayfalı ham baskı; bunun içindeki gerçek 604 mushaf sayfası **henüz ayrıştırılıp doğrulanmadı**. 640 sayfalı PDF ile 604 matbu sayfa aynı değildir.

**Durum:** Bu yalnızca kaynak dosyalarının indirilmesi ve kontrol edilmesi işidir; APK henüz oluşturulmadı.

Ayrıntılar: [assets/README.md](assets/README.md).
