# Mushaf ve tilavet kaynakları, lisans ve doğrulama

## Mushaf
- Yayıncı olarak belirtilen kurum: **Kral Fehd Kur'an Basım Kompleksi**.
- Resmî dijital mushaf: https://dm.qurancomplex.gov.sa/
- Resmî kullanım koşulları: https://dm.qurancomplex.gov.sa/rights/
- 1441H, Hafs PDF aynası: https://archive.org/download/MushafMadinaHafsGreen1441/MushafMadinaHafsGreen1441.pdf

**PDF'nin 640 sayfası var.** Matbu mushafın 604 sayfasını tek tek doğrulayıp ayırmadan APK'da 604 sayfa tamamlandı denmeyecek.

## Ses / tam sure
- Hafız: **Mahir el-Muaykılî**, Hafs.
- Kurumun ücretsiz kullanım izni açıklaması: https://qc-dev.qurancomplex.gov.sa/quran-audios/
- Resmî Hafs arşivi: https://qurancomplex.gov.sa/category/moratal/hafs/
- Resmî siteye bağlantı zaman aşımından dolayı kullanılan alternatif kaynak: https://archive.org/details/HaramainMaahir
- Aynanın yükleyicisi **Haramain Recordings**, açıklamasında bunları **King Fahd Qur'an Complex Recording** olarak tanımlıyor. Bu bilgi yükleyici beyanıdır; resmî ses dosyalarıyla birebir özdeşliği şu an kesinleşmiş değildir.
- 001.mp3 ile 114.mp3 arasındaki 114 tam sure indirilir; süre ve dosya boyu doğrulanır, sonra APK'da az yer kaplaması için 64 kbps tek kanal Ogg Opus biçimine dönüştürülür. Tilavetin hiçbir suresi atlanmaz.

**Yayın hakkı:** Kurum kendi sayfasında listelenen kayıtların ücretsiz uygulama ve medya kullanımına izin veriyor. Başka bir kişinin yüklediği kopyanın aynı izinli kayıt olduğundan emin olmadan APK'yı halka dağıtma kararı alınmamalıdır. Bu hak kayıt sahipliğinin geliştiriciye devri anlamına gelmez.

## Teknik
- `scripts/download_mirror.py`: doğrulamalı indirici.
- `.github/workflows/prepare-offline-sources.yml`: otomatik kaynak hazırlama işlemi.
- SHA-256 listesi: başarılı işin `SOURCE-MANIFEST.json` dosyası.
- Dosyalar Git geçmişine commit edilmez; GitHub Actions Artifacts bölümünde saklanır. Releases'a aktarım GitHub'ın HTTP 403 yetki reddi nedeniyle şimdilik kapalıdır.
- Bu dosyalar henüz uygulamaya gömülmedi. APK ayrıca hazırlanıp uçak modunda test edilecektir.
