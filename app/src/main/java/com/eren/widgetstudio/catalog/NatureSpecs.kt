package com.eren.widgetstudio.catalog

import com.eren.widgetstudio.data.WeatherApi
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.WidgetType
import com.eren.widgetstudio.data.opt
import com.eren.widgetstudio.logic.DateMath
import com.eren.widgetstudio.logic.MoonPhase
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.roundToInt

object NatureSpecs {
    val all: List<WidgetSpec> = listOf(WeatherSpec, ForecastSpec, SunSpec, MoonSpec)
}

/** Şehir seçilmemiş ya da veri henüz gelmemişse gösterilen ortak içerik. */
private fun weatherPlaceholder(d: WidgetDesign): List<Block> = listOf(
    Block.Group(
        listOf(
            Block.Label(if (d.city.isBlank()) "Şehir seçilmedi" else d.city, 14f, Tone.ACCENT),
            body(if (d.city.isBlank()) "Düzenleyiciden şehir ara" else "Güncellemek için dokun", 12f),
        ),
        tap = Tap(ACTION_REFRESH),
        center = true,
    ),
)

object WeatherSpec : WidgetSpec {
    override val type = WidgetType.WEATHER
    override val wide = false

    override fun defaults(base: WidgetDesign) = base.copy(bgColor = 0xFF1A3A5C, accentColor = 0xFFF9E2AF)

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val w = d.weather ?: return weatherPlaceholder(d)
        val (emoji, desc) = WeatherApi.describe(w.code)
        return listOf(
            Block.Group(
                buildList<Block> {
                    add(
                        Block.Group(
                            listOf(
                                Block.Label(emoji, 30f),
                                Block.Gap(6),
                                Block.Label("${w.temp.roundToInt()}°", 38f, Tone.TEXT),
                            ),
                            horizontal = true,
                        ),
                    )
                    add(Block.Label(d.city, 14f, Tone.ACCENT, bold = true))
                    add(body("$desc · ↑${w.max.roundToInt()}° ↓${w.min.roundToInt()}°", 12f))
                    if (d.opt("details")) {
                        val parts = listOfNotNull(
                            w.feelsLike?.let { "Hissedilen ${it.roundToInt()}°" },
                            if (w.humidity >= 0) "Nem %${w.humidity}" else null,
                            if (w.wind >= 0) "Rüzgâr ${w.wind.roundToInt()} km/s" else null,
                        )
                        if (parts.isNotEmpty()) add(muted(parts.joinToString(" · "), 11f))
                    }
                },
                tap = Tap(ACTION_REFRESH),
                center = d.centered,
            ),
        )
    }
}

object ForecastSpec : WidgetSpec {
    override val type = WidgetType.FORECAST

    override fun defaults(base: WidgetDesign) = base.copy(bgColor = 0xFF1A3A5C, accentColor = 0xFFF9E2AF, padding = 12)

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val days = d.weather?.daily.orEmpty()
        if (days.isEmpty()) return weatherPlaceholder(d)
        val today = env.today
        val columns = days.take(5).map { day ->
            val date = runCatching { LocalDate.parse(day.date) }.getOrNull()
            val name = when (date) {
                null -> "—"
                today -> "Bugün"
                else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, TR)
            }
            Block.Group(
                listOf(
                    Block.Label(name, 11f, Tone.ACCENT, bold = true),
                    Block.Gap(2),
                    Block.Label(WeatherApi.describe(day.code).first, 22f),
                    Block.Label("${day.max.roundToInt()}°", 13f, Tone.TEXT, bold = true),
                    Block.Label("${day.min.roundToInt()}°", 12f, Tone.MUTED),
                ),
                center = true,
            ) as Block
        }
        return listOf(
            Block.Group(
                listOf(
                    Block.Label(d.city, 13f, Tone.TEXT, bold = true),
                    Block.Gap(4),
                    Block.Table(listOf(columns)),
                ),
                tap = Tap(ACTION_REFRESH),
                center = true,
            ),
        )
    }
}

object SunSpec : WidgetSpec {
    override val type = WidgetType.SUN
    override val wide = false

    override fun defaults(base: WidgetDesign) = base.copy(bgColor = 0xFF3B2A4A, accentColor = 0xFFFAB387)

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val w = d.weather
        if (w == null || w.sunrise.isBlank() || w.sunset.isBlank()) return weatherPlaceholder(d)
        val rise = DateMath.minutesOfDay(w.sunrise)
        val set = DateMath.minutesOfDay(w.sunset)
        val length = if (rise != null && set != null && set > rise) {
            val m = set - rise
            "Gün uzunluğu ${m / 60} sa ${m % 60} dk"
        } else ""
        return listOf(
            Block.Group(
                listOf(
                    Block.Label(d.city, 13f, Tone.ACCENT, bold = true),
                    Block.Gap(4),
                    Block.Label("🌅 ${w.sunrise}", 24f, Tone.TEXT),
                    Block.Label("🌇 ${w.sunset}", 24f, Tone.TEXT),
                    Block.Gap(2),
                    muted(length, 11f),
                ),
                tap = Tap(ACTION_REFRESH),
                center = true,
            ),
        )
    }
}

object MoonSpec : WidgetSpec {
    override val type = WidgetType.MOON
    override val wide = false

    override fun defaults(base: WidgetDesign) = base.copy(bgColor = 0xFF11111B, accentColor = 0xFFF9E2AF)

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val p = MoonPhase.at(env.nowMs)
        val next = if (p.fraction < 0.5) "Dolunaya ${p.daysToFull.roundToInt()} gün" else "Yeni aya ${p.daysToNew.roundToInt()} gün"
        return listOf(
            Block.Group(
                listOf(
                    Block.Label(p.emoji, 52f),
                    Block.Label(p.name, 15f, Tone.ACCENT, bold = true),
                    muted("Aydınlık %${(p.illumination * 100).roundToInt()}", 12f),
                    muted(next, 11f),
                ),
                center = true,
            ),
        )
    }
}
