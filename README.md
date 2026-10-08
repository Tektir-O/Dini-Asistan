# Dini Asistan — çevrimdışı Kur'an uygulaması

Bu depo, internetsiz çalışan bir Android Kur'an uygulaması için yeniden başlatılmıştır.

## İçerikler

- **Mushaf:** Medine Mushafı, Hafs rivayeti. 604 Kur'an sayfası hedefleniyor.
- **Tilavet:** Mahir el-Muaykılî, Hafs rivayeti. 114 surenin tamamı hedefleniyor.
- **İnternet:** Nihai APK kurulduktan sonra hiçbir kaynak indirmeyecek. Sesler ve sayfalar APK'ya gömülecek.

**Durum:** İndirilen dosyalar doğrulanmadan "tamamlandı" denmez. Büyük dosyalar Git tarihine değil, GitHub Actions çıktısına (başarılıysa Releases) eklenir.

**İndirme:** `.github/workflows/prepare-offline-sources.yml` çalışması mushaf PDF'si ve resmî tilavet arşivini indirip kontrol eder. PDF'nin matbu 604 sayfa dışındaki kapak/ek sayfaları ayrıştırılmadan APK'ya gömülmez. Ses arşivinde tam 114 MP3 doğrulanır.

Ayrıntılar: [assets/README.md](assets/README.md).
