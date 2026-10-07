# Katkı rehberi

Widget Stüdyo MIT lisanslıdır: istediğin gibi kullanabilir, değiştirebilir, kendi sürümünü yayınlayabilirsin.
Katkılar (yeni widget, hata düzeltme, çeviri, tasarım) memnuniyetle karşılanır.

## Geliştirme ortamı

- Android Studio (Ladybug veya yeni) ya da yalnızca JDK 17 + Android SDK 35
- Derleme: `./gradlew assembleDebug`
- Birim testleri (telefon gerekmez): `./gradlew testDebugUnitTest`

Telefonun yoksa fork'unda GitHub Actions APK derler (`.github/workflows/build.yml`).

## Mimari (kısaca)

```
catalog/   Widget TANIMLARI. Android'e bağımlı değil, JVM'de test edilir.
  Block.kt        Ekranda ne gösterileceğinin tarifi (metin, ilerleme çubuğu, buton, tablo…)
  WidgetSpec.kt   Bir widget türünün arayüzü + kayıt defteri (WidgetSpecs)
  *Specs.kt       Tüm widget'lar (Zaman, Planlama, Araçlar, Cihaz, Doğa)
logic/     Saf hesaplamalar (ay evresi, tarih matematiği, sözler, saat dilimleri)
data/      Model (WidgetDesign), depolama (SharedPreferences + JSON), hava durumu API'si, cihaz bilgisi
widget/    Ana ekran widget'ı (Jetpack Glance). BlockRenderer.kt blokları çizer.
ui/        Uygulama içi düzenleyici. TypeOptions.kt her türün ayar alanlarını içerir.
```

Fikir: **bir widget, ekranda ne göstereceğini `Block` listesi olarak tarif eder.**
Aynı liste hem ana ekranda (Glance) hem uygulama içi önizlemede (Compose) çizilir; ayrı ayrı iki kez yazmazsın.

## Yeni widget eklemek (4 adım)

1. **`data/Model.kt` → `WidgetType`**: bir satır ekle: `MY_WIDGET("Adı", "🧩", "Araçlar")`.
2. **`catalog/…Specs.kt`**: `WidgetSpec` uygulayan bir `object` yaz ve ilgili `all` listesine ekle.

   ```kotlin
   object HelloSpec : WidgetSpec {
       override val type = WidgetType.MY_WIDGET

       override fun defaults(base: WidgetDesign) = base.copy(label = "Merhaba")

       override fun render(d: WidgetDesign, env: RenderEnv): List<Block> = listOf(
           title(d.label),
           big("${env.today.dayOfMonth}"),
           Block.Btn("Say", Tap("count")),
       )

       // Dokunma eylemi: yeni tasarımı döndür (değişiklik yoksa null)
       override fun onAction(d: WidgetDesign, action: String, arg: Int, env: RenderEnv) =
           if (action == "count") d.copy(counter = d.counter + 1) else null
   }
   ```
3. **`ui/TypeOptions.kt`**: `when` içine ayar alanlarını ekle (ortak kontroller `ui/Controls.kt`'de).
   Ayar gerekmiyorsa `HintText("…")` yeter.
4. **Test**: `CatalogTest` her türün varsayılanla çizildiğini ve her dokunma eyleminin işlendiğini zaten denetler;
   kendi mantığın için bir test daha ekle.

### Veri saklama

- Basit açma/kapama ayarları için `d.opt("anahtar", varsayılan)` / `d.withOpt(...)` kullan: model değişmez.
- `WidgetDesign` içindeki genel alanlar (`label`, `text`, `unit`, `counter`, `goal`, `step`, `targetEpochDay`, `zones`…) türler arası paylaşılır.
- Gerçekten yeni alan gerekiyorsa **varsayılan değeri olan** bir alan ekle; eski kayıtlar bozulmaz.
- Tüm veri yalnızca cihazdadır. Yeni bir ağ çağrısı veya izin eklersen PR açıklamasında nedenini yaz.

### Glance sınırları (cihazda widget hiç çizilmezse buna bak)

- Bir `Row`/`Column` en fazla **10 çocuk** alabilir. `CatalogTest.noContainerExceedsGlanceChildLimit` bunu denetler.
- Canlı saat/kronometre için `Block.Clock` / `Block.Chrono` kullan (sistem günceller, pil yemez).
  Diğer her şey statiktir; 15 dakikada bir, gün değişince ve dokununca yenilenir.
- Boyutlar `sp`'dir ve tasarımın yazı boyutu çarpanıyla ölçeklenir.

## Kod stili

- Mevcut kodun yorum yoğunluğunu ve adlandırmasını izle. Kullanıcıya görünen metinler Türkçe.
- Küçük, odaklı PR'lar tercih edilir.

## Sürümler

`main` dalına her push, GitHub Actions ile bir APK derler ve Releases'e koyar.
