package com.eren.widgetstudio.data

import kotlinx.serialization.Serializable
import java.util.UUID

/** Widget türleri. Yeni tür eklemek için CONTRIBUTING.md'ye bak. */
@Serializable
enum class WidgetType(
    val label: String,
    val emoji: String,
    val group: String,
    val needsWeather: Boolean = false,
) {
    // Zaman
    CLOCK("Saat & Tarih", "🕒", "Zaman"),
    WORLD_CLOCK("Dünya Saati", "🌍", "Zaman"),
    DATE("Büyük Tarih", "📅", "Zaman"),
    CALENDAR("Aylık Takvim", "🗓️", "Zaman"),
    TIME_PROGRESS("Zaman İlerlemesi", "⏳", "Zaman"),
    STOPWATCH("Kronometre", "⏱️", "Zaman"),
    TIMER("Zamanlayıcı", "⏲️", "Zaman"),

    // Planlama
    TODO("Yapılacaklar", "✅", "Planlama"),
    HABIT("Günlük Alışkanlıklar", "🔁", "Planlama"),
    COUNTDOWN("Geri Sayım", "🎯", "Planlama"),
    DAYS_SINCE("Gün Sayacı", "📈", "Planlama"),
    AGE("Yaş Hesaplayıcı", "🎂", "Planlama"),
    NOTE("Not", "📝", "Planlama"),
    QUOTE("Günün Sözü", "💬", "Planlama"),

    // Araçlar
    COUNTER("Sayaç", "🔢", "Araçlar"),
    WATER("Su Takibi", "💧", "Araçlar"),
    DICE("Zar / Yazı-Tura", "🎲", "Araçlar"),
    QUICK_SETTINGS("Hızlı Ayarlar", "⚙️", "Araçlar"),

    // Cihaz
    SYSTEM("Sistem Bilgisi", "📊", "Cihaz"),
    BATTERY("Pil Detayı", "🔋", "Cihaz"),
    NETWORK("Bağlantı", "📶", "Cihaz"),
    DEVICE("Cihaz Bilgisi", "📱", "Cihaz"),

    // Hava & doğa
    WEATHER("Hava Durumu", "⛅", "Hava & Doğa", needsWeather = true),
    FORECAST("5 Günlük Tahmin", "🌦️", "Hava & Doğa", needsWeather = true),
    SUN("Gün Doğumu / Batımı", "🌅", "Hava & Doğa", needsWeather = true),
    MOON("Ay Evresi", "🌙", "Hava & Doğa"),
}

@Serializable
data class TodoItem(val text: String, val done: Boolean = false)

@Serializable
data class DayForecast(val date: String, val code: Int, val max: Double, val min: Double)

@Serializable
data class WeatherData(
    val temp: Double,
    val code: Int,
    val max: Double,
    val min: Double,
    val updatedAt: Long,
    val feelsLike: Double? = null,
    val humidity: Int = -1,
    val wind: Double = -1.0,
    val sunrise: String = "",
    val sunset: String = "",
    val daily: List<DayForecast> = emptyList(),
)

const val SCHEMA_VERSION = 2

/**
 * Kullanıcının uygulama içinde tasarladığı tek bir widget.
 *
 * Alanlar türler arasında paylaşılır (ör. [label] hem sayaçta hem geri sayımda başlıktır).
 * Yeni bir widget yeni bir alan istiyorsa önce [opts] / mevcut alanlara bak; gerçekten
 * gerekirse varsayılan değeri olan bir alan ekle (eski kayıtlar bozulmaz).
 */
@Serializable
data class WidgetDesign(
    val id: String = UUID.randomUUID().toString(),
    val schemaVersion: Int = SCHEMA_VERSION,
    val name: String = "Yeni widget",
    val type: WidgetType = WidgetType.CLOCK,

    // Görünüm
    val bgColor: Long = 0xFF1E1E2E,
    val bgAlpha: Float = 1f,
    val textColor: Long = 0xFFFFFFFF,
    val accentColor: Long = 0xFF89B4FA,
    val cornerRadius: Int = 24,
    val fontScale: Float = 1f,
    val padding: Int = 16,
    val centered: Boolean = true,

    // Saat
    val clock24h: Boolean = true,
    val clockSeconds: Boolean = false,
    val clockShowDate: Boolean = true,

    // Yapılacaklar / alışkanlıklar
    val todoTitle: String = "Yapılacaklar",
    val todos: List<TodoItem> = emptyList(),

    // Hava durumu
    val city: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val weather: WeatherData? = null,

    // Sistem
    val showBattery: Boolean = true,
    val showStorage: Boolean = true,
    val showRam: Boolean = true,

    // Genel amaçlı alanlar (türe göre anlamı değişir)
    val label: String = "",
    val text: String = "",
    val unit: String = "",
    val targetEpochDay: Long = 0,
    val counter: Int = 0,
    val goal: Int = 0,
    val step: Int = 1,
    val stateDay: Long = 0,
    val running: Boolean = false,
    val runStartMs: Long = 0,
    val elapsedMs: Long = 0,
    val timerMinutes: Int = 25,
    val zones: List<String> = emptyList(),
    val opts: Map<String, Boolean> = emptyMap(),
)

fun WidgetDesign.opt(key: String, default: Boolean = false): Boolean = opts[key] ?: default

fun WidgetDesign.withOpt(key: String, value: Boolean): WidgetDesign = copy(opts = opts + (key to value))

fun WidgetDesign.bgArgb(): Int =
    ((bgAlpha.coerceIn(0f, 1f) * 255).toInt() shl 24) or (bgColor.toInt() and 0x00FFFFFF)

fun WidgetDesign.textArgb(): Int = textColor.toInt()

fun WidgetDesign.accentArgb(): Int = accentColor.toInt()

fun WidgetDesign.clockFormat(): String = when {
    clock24h && clockSeconds -> "HH:mm:ss"
    clock24h -> "HH:mm"
    clockSeconds -> "h:mm:ss a"
    else -> "h:mm a"
}

const val DATE_FORMAT = "EEEE, d MMMM"
