# Widget Stüdyo

Kendi ana ekran widget'larını uygulama içinde tasarlayıp kullanman için kişisel bir Android uygulaması.

## Neler var

| Widget | Özellikler |
|---|---|
| 🕒 Saat & Tarih | Canlı saat (sistem güncelliyor, pil yemez), 24/12 saat, saniye, Türkçe tarih |
| ✅ Yapılacaklar | Widget üzerinden dokunarak işaretleme, kaydırılabilir liste |
| ⛅ Hava Durumu | Open-Meteo (ücretsiz, anahtar gerekmez), şehir arama, konum izni gerekmez |
| 📊 Sistem Bilgisi | Pil, depolama, RAM; ilerleme çubuklarıyla |

Her tasarımda ayarlanabilenler: arka plan rengi ve opaklığı (şeffaf widget), yazı ve vurgu rengi (paletten ya da `#RRGGBB`), yazı boyutu, köşe yuvarlaklığı, iç boşluk ve hizalama. Editörde canlı önizleme var.

Aynı tasarımı birden fazla widget'ta kullanabilirsin. Tasarımı değiştirdiğinde ana ekrandaki tüm kopyaları güncellenir.

## GitHub Actions ile derleme

1. GitHub'da **private** bir repo oluştur (ör. `widget-studio`).
2. Bu klasörün içeriğini repoya yükle:
   ```bash
   cd WidgetStudio
   git init -b main
   git add .
   git commit -m "İlk sürüm"
   git remote add origin https://github.com/KULLANICI_ADIN/widget-studio.git
   git push -u origin main
   ```
   > `.github` klasörünün de yüklendiğinden emin ol. Gizli klasör olduğu için web arayüzünden sürükle-bırak yaparken atlanabiliyor.
3. Push yapınca **Actions** sekmesinde "APK Derle" otomatik çalışır (yaklaşık 4-6 dk). Elle başlatmak için: Actions → APK Derle → *Run workflow*.
4. Bittiğinde APK iki yerde olur:
   - **Releases** (önerilen): telefondan GitHub uygulamasıyla ya da tarayıcıdan doğrudan indirilebilir.
   - Actions çalışmasının altındaki **Artifacts** (zip içinde gelir).

## Telefona kurulum

1. APK'yı telefonda aç. "Bilinmeyen kaynaklardan yükleme" izni istenirse tarayıcıya/dosya yöneticisine izin ver.
2. Uygulamayı aç, **Yeni widget** ile bir tasarım oluştur ve kaydet.
3. Ana ekranda boş bir yere uzun bas → **Widget'lar** → **Widget Stüdyo** → ekrana sürükle → açılan listeden tasarımını seç.
4. Sonradan değiştirmek için: widget'a uzun bas → yeniden yapılandır (Android 12+). Ya da uygulamada tasarımı düzenle.

Repoda sabit bir imza anahtarı (`app/debug.keystore`) var. Bu sayede her yeni sürüm eskisinin üzerine kurulur ve tasarımların korunur. Repo private kaldığı sürece sorun yok. Public yapacaksan bu dosyayı GitHub Secrets'a taşımak gerekir.

## Notlar

- **Xiaomi / Huawei / Samsung** gibi cihazlarda hava durumu ve sistem bilgisinin arka planda yenilenmesi için: Ayarlar → Uygulamalar → Widget Stüdyo → Pil → **Kısıtlama yok**. Saat bundan etkilenmez.
- Hava durumu ve sistem widget'ına dokununca veriler hemen yenilenir. Diğer widget'lara dokununca uygulama açılır.

## Proje yapısı

```
app/src/main/java/com/eren/widgetstudio/
├── MainActivity.kt          Tasarım listesi
├── ConfigureActivity.kt     Widget eklerken tasarım seçici
├── data/                    Model, depolama, hava durumu, sistem bilgisi
├── ui/                      Editör ve canlı önizleme (Compose)
└── widget/                  Ana ekran widget'ı (Jetpack Glance) ve yenileme
```

Teknolojiler: Kotlin, Jetpack Compose (Material 3), Jetpack Glance, WorkManager, kotlinx.serialization. Minimum Android 8.0.
