package com.eren.widgetstudio.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class WidgetType(val label: String, val emoji: String) {
    CLOCK("Saat & Tarih", "🕒"),
    TODO("Yapılacaklar", "✅"),
    WEATHER("Hava Durumu", "⛅"),
    SYSTEM("Sistem Bilgisi", "📊"),
}

@Serializable
data class TodoItem(val text: String, val done: Boolean = false)

@Serializable
data class WeatherData(
    val temp: Double,
    val code: Int,
    val max: Double,
    val min: Double,
    val updatedAt: Long,
)

/** Kullanıcının uygulama içinde tasarladığı tek bir widget. */
@Serializable
data class WidgetDesign(
    val id: String = UUID.randomUUID().toString(),
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

    // Yapılacaklar
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
)

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

/** Yeni tasarım için türüne uygun başlangıç değerleri. */
fun newDesign(type: WidgetType): WidgetDesign = when (type) {
    WidgetType.CLOCK -> WidgetDesign(name = type.label, type = type)
    WidgetType.TODO -> WidgetDesign(
        name = type.label, type = type, centered = false,
        todos = listOf(TodoItem("İlk görevim"), TodoItem("Widget'a dokunarak işaretle")),
    )
    WidgetType.WEATHER -> WidgetDesign(name = type.label, type = type, bgColor = 0xFF1A3A5C, accentColor = 0xFFF9E2AF)
    WidgetType.SYSTEM -> WidgetDesign(name = type.label, type = type, centered = false, accentColor = 0xFFA6E3A1)
}
