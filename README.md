# Widget Stüdyo

Kendi ana ekran widget'larını uygulama içinde tasarlayıp kullanman için **tamamen açık kaynak** (MIT), **çevrimdışı çalışan** bir Android uygulaması.
Hesap yok, reklam yok, takip yok. Verilerin yalnızca telefonunda durur.

> English: an open-source (MIT), offline-first Android app to design your own home-screen widgets. 26 widget types, every colour/size is customizable. Build with `./gradlew assembleDebug`. See [CONTRIBUTING.md](CONTRIBUTING.md) to add your own widget in one file.

## Widget'lar (26)

| Grup | Widget | Özellik |
|---|---|---|
| **Zaman** | 🕒 Saat & Tarih | Canlı saat, 24/12 saat, saniye |
| | 🌍 Dünya Saati | 2-4 şehir, canlı |
| | 📅 Büyük Tarih | Gün, haftanın günü, hafta no, yılın günü |
| | 🗓️ Aylık Takvim | Bugün vurgulu, hafta başlangıcı seçilebilir |
| | ⏳ Zaman İlerlemesi | Gün / hafta / ay / yıl yüzdesi |
| | ⏱️ Kronometre | Ana ekrandan başlat/duraklat/sıfırla |
| | ⏲️ Zamanlayıcı | 1-120 dk geri sayım (pomodoro için) |
| **Planlama** | ✅ Yapılacaklar | Widget'tan dokunarak işaretle, kaydırılabilir |
| | 🔁 Günlük Alışkanlıklar | İşaretler her gün sıfırlanır, ilerleme çubuğu |
| | 🎯 Geri Sayım | Hedef tarihe kaç gün kaldı |
| | 📈 Gün Sayacı | Bir tarihten beri kaç gün geçti |
| | 🎂 Yaş Hesaplayıcı | Yıl-ay-gün, doğum gününe kalan gün |
| | 📝 Not | Serbest metin, sarı not kâğıdı gibi |
| | 💬 Günün Sözü | Hazır atasözleri ya da kendi listen; dokununca değişir |
| **Araçlar** | 🔢 Sayaç | +/− butonları, hedef, birim, günlük sıfırlama |
| | 💧 Su Takibi | Günlük bardak hedefi |
| | 🎲 Zar / Yazı-Tura | d2 … d100, son atışlar |
| | ⚙️ Hızlı Ayarlar | Wi-Fi, Bluetooth, ekran, ses, kamera… kısayolları (izin gerekmez) |
| **Cihaz** | 📊 Sistem Bilgisi | Pil, depolama, RAM |
| | 🔋 Pil Detayı | Yüzde, şarj durumu, sıcaklık, voltaj, sağlık |
| | 📶 Bağlantı | Wi-Fi / mobil, internet var mı |
| | 📱 Cihaz Bilgisi | Model, Android sürümü, açık kalma süresi |
| **Hava & Doğa** | ⛅ Hava Durumu | Open-Meteo, şehir arama, isteğe bağlı nem/rüzgâr |
| | 🌦️ 5 Günlük Tahmin | |
| | 🌅 Gün Doğumu / Batımı | |
| | 🌙 Ay Evresi | Cihazda hesaplanır, internet gerekmez |

Her tasarımda ayarlanabilenler: arka plan rengi ve opaklığı (şeffaf widget), yazı ve vurgu rengi (paletten ya da `#RRGGBB`), yazı boyutu, köşe yuvarlaklığı, iç boşluk ve hizalama. Editörde canlı önizleme var.
Aynı tasarımı birden fazla widget'ta kullanabilirsin; tasarımı değiştirince tüm kopyalar güncellenir. **Çoğalt** ile bir tasarımı kopyalayıp varyasyon yapabilirsin.

## Gizlilik ve veri

- Tüm tasarımlar **yalnızca cihazdaki** uygulama deposunda durur. Bulut yedeği kapalıdır (`allowBackup=false`).
- İnternet **yalnızca hava durumu / tahmin / gün doğumu** widget'ları için kullanılır ve sadece seçtiğin şehrin koordinatını [Open-Meteo](https://open-meteo.com)'ya gönderir (anahtar ve hesap yok). Bu widget'ları kullanmazsan uygulama hiç ağa çıkmaz.
- Konum izni **istenmez**. "Bağlantı" widget'ı yalnızca bağlantı türünü okur (ağ adı veya konum okumaz).
- **Yedekleme:** ana ekran menüsü (⋮) → *Tasarımları yedekle* ile bir JSON dosyası kaydedersin; *Yedekten yükle* ile geri alırsın (telefon değiştirirken işe yarar). Dosya okunabilir bir JSON'dur, istersen elle de düzenleyebilirsin.

## Derleme

### Bilgisayarda

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # birim testleri (telefon gerekmez)
```

Android Studio ile klasörü açman yeterli.

### GitHub Actions ile (bilgisayarsız)

1. Repo'yu fork'la ya da kendi hesabına yükle (`.github` klasörü de gitmeli; gizli klasör olduğu için web arayüzünde sürükle-bırak atlayabilir).
2. `main` dalına push yapınca **Actions → APK Derle** çalışır (yaklaşık 4-6 dk). Elle başlatmak için *Run workflow*.
3. APK **Releases** altında ve çalışmanın **Artifacts** bölümünde olur.

Pull request'lerde ve diğer dallarda `Test` iş akışı testleri çalıştırır.

## Telefona kurulum

1. APK'yı telefonda aç. "Bilinmeyen kaynaklardan yükleme" izni istenirse tarayıcıya/dosya yöneticisine izin ver.
2. Uygulamayı aç, **Yeni widget** ile bir tür seç, tasarla ve kaydet.
3. Ana ekranda boş bir yere uzun bas → **Widget'lar** → **Widget Stüdyo** → ekrana sürükle → açılan listeden tasarımını seç.
4. Sonradan değiştirmek için: widget'a uzun bas → yeniden yapılandır (Android 12+). Ya da uygulamada tasarımı düzenle.

### İmza anahtarı

`app/debug.keystore`, Android'in herkese açık standart debug anahtarıdır (gizli değildir). Repoda durması sayesinde her yeni sürüm eskisinin üzerine kurulur ve tasarımların korunur.
Kendi sürümünü yayınlayacaksan kendi anahtarını üretip `app/build.gradle.kts` içindeki `signingConfigs` bölümünü ona çevir (anahtarı repoya koyma; GitHub Secrets kullan).

## Notlar

- **Xiaomi / Huawei / Samsung** gibi cihazlarda hava durumu ve sistem bilgisinin arka planda yenilenmesi için: Ayarlar → Uygulamalar → Widget Stüdyo → Pil → **Kısıtlama yok**. Saat, kronometre ve geri sayım bundan etkilenmez.
- Saat, dünya saati ve kronometre sistem tarafından canlı güncellenir. Diğerleri 15 dakikada bir, gün değişince ve widget'a dokunulduğunda yenilenir.
- Zamanlayıcı süre dolunca ses/bildirim vermez; sayaç eksiye geçer.
- Android'in yeni sürümlerinde bazı launcher'lar widget boyutunu kısıtlayabilir; Takvim ve 5 günlük tahmin için en az 4×2 hücre önerilir.

## Katkı

Yeni widget eklemek, tek bir dosyaya bir `WidgetSpec` yazmak kadar kolay. Adımlar için [CONTRIBUTING.md](CONTRIBUTING.md).

## Lisans

[MIT](LICENSE): kullan, değiştir, dağıt, sat; yeter ki lisans metnini koru.

## Proje yapısı

```
app/src/main/java/com/eren/widgetstudio/
  catalog/   widget tanımları (Block, WidgetSpec ve tüm widget'lar)
  logic/     saf hesaplamalar (ay evresi, tarih, sözler)
  data/      model, depolama, hava API'si, cihaz bilgisi
  widget/    ana ekran widget'ı (Glance)
  ui/        düzenleyici ve önizleme (Compose)
app/src/test/  JVM birim testleri
```
