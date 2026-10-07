package com.eren.widgetstudio.catalog

import com.eren.widgetstudio.data.DATE_FORMAT
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.WidgetType
import com.eren.widgetstudio.data.clockFormat
import com.eren.widgetstudio.data.opt
import com.eren.widgetstudio.logic.DateMath
import com.eren.widgetstudio.logic.Zones
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale
import kotlin.math.roundToInt

internal val TR: Locale = Locale.forLanguageTag("tr-TR")

object TimeSpecs {
    val all: List<WidgetSpec> = listOf(
        ClockSpec, WorldClockSpec, DateSpec, CalendarSpec, TimeProgressSpec, StopwatchSpec, TimerSpec,
    )
}

object ClockSpec : WidgetSpec {
    override val type = WidgetType.CLOCK
    override val wide = false

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> = buildList<Block> {
        add(Block.Clock(d.clockFormat(), 44f))
        if (d.clockShowDate) {
            add(Block.Gap(4))
            add(Block.Clock(DATE_FORMAT, 14f, Tone.ACCENT))
        }
    }
}

object WorldClockSpec : WidgetSpec {
    override val type = WidgetType.WORLD_CLOCK
    override val alignTop = true

    override fun defaults(base: WidgetDesign) = base.copy(zones = Zones.DEFAULT, centered = false)

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val zones = d.zones.ifEmpty { Zones.DEFAULT }
        val format = if (d.clock24h) "HH:mm" else "h:mm a"
        return spaced(
            zones.map { id ->
                Block.Split(
                    Block.Group(
                        listOf(
                            Block.Label(Zones.label(id), 15f, Tone.ACCENT, bold = true),
                            Block.Clock("EEE", 11f, Tone.MUTED, id),
                        ),
                    ),
                    Block.Clock(format, 26f, Tone.TEXT, id),
                )
            },
            gap = 6,
        )
    }
}

object DateSpec : WidgetSpec {
    override val type = WidgetType.DATE
    override val wide = false

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> = buildList<Block> {
        add(Block.Clock("d", 64f))
        add(Block.Clock("EEEE", 18f, Tone.ACCENT))
        add(Block.Clock("MMMM yyyy", 14f))
        val date = env.today
        if (d.opt("week", true)) {
            add(Block.Gap(4))
            add(muted("Hafta ${date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)} · Yılın ${date.dayOfYear}. günü"))
        }
    }
}

object CalendarSpec : WidgetSpec {
    override val type = WidgetType.CALENDAR

    override fun defaults(base: WidgetDesign) = base.copy(padding = 12)

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val today = env.today
        val mondayFirst = d.opt("mondayFirst", true)
        val first = DayOfWeek.of(if (mondayFirst) 1 else 7)
        val monthStart = today.withDayOfMonth(1)
        val offset = ((monthStart.dayOfWeek.value - first.value) + 7) % 7

        val header = (0 until 7).map { i ->
            muted(first.plus(i.toLong()).getDisplayName(TextStyle.NARROW, TR), 11f)
        }
        val cells = MutableList<Block>(offset) { Block.Gap(0) } +
            (1..today.lengthOfMonth()).map { day ->
                val isToday = day == today.dayOfMonth
                Block.Group(
                    listOf(Block.Label(day.toString(), 12f, if (isToday) Tone.ON_ACCENT else Tone.TEXT, bold = isToday)),
                    fill = if (isToday) Tone.ACCENT else null,
                    center = true,
                ) as Block
            }
        val padded = cells + List((7 - cells.size % 7) % 7) { Block.Gap(0) }
        val weeks = padded.chunked(7)

        val monthName = today.month.getDisplayName(TextStyle.FULL, TR)
        return listOf(
            Block.Label("$monthName ${today.year}", 14f, Tone.ACCENT, bold = true),
            Block.Gap(4),
            Block.Table(listOf(header) + weeks),
        )
    }
}

object TimeProgressSpec : WidgetSpec {
    override val type = WidgetType.TIME_PROGRESS
    override val alignTop = true

    override fun defaults(base: WidgetDesign) = base.copy(centered = false)

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val p = DateMath.progress(env.today, env.secondOfDay)
        fun row(label: String, f: Float) = meterRow(label, "%${(f * 100).roundToInt()}", f)
        val rows = buildList<Block> {
            if (d.opt("day", true)) add(row("☀️ Gün", p.day))
            if (d.opt("week", true)) add(row("📆 Hafta", p.week))
            if (d.opt("month", true)) add(row("🗓️ Ay", p.month))
            if (d.opt("year", true)) add(row("🌐 Yıl ${env.today.year}", p.year))
        }
        return if (rows.isEmpty()) listOf(body("Gösterilecek bilgi seçilmedi")) else spaced(rows)
    }
}

/** Kronometre ve zamanlayıcı ortak durum mantığını kullanır. */
private fun WidgetDesign.totalElapsed(now: Long): Long = elapsedMs + if (running) (now - runStartMs).coerceAtLeast(0) else 0

private fun WidgetDesign.toggleRun(now: Long): WidgetDesign =
    if (running) copy(running = false, elapsedMs = totalElapsed(now), runStartMs = 0)
    else copy(running = true, runStartMs = now)

private fun WidgetDesign.resetRun(): WidgetDesign = copy(running = false, elapsedMs = 0, runStartMs = 0)

object StopwatchSpec : WidgetSpec {
    override val type = WidgetType.STOPWATCH

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> = listOf(
        Block.Chrono(d.totalElapsed(env.nowMs), d.running, countDown = false, size = 40f),
        Block.Gap(8),
        Block.Group(
            listOf(
                Block.Btn(if (d.running) "⏸ Duraklat" else "▶ Başlat", Tap("toggle")),
                Block.Btn("↺", Tap("reset")),
            ),
            horizontal = true,
        ),
    )

    override fun onAction(d: WidgetDesign, action: String, arg: Int, env: RenderEnv) = when (action) {
        "toggle" -> d.toggleRun(env.nowMs)
        "reset" -> d.resetRun()
        else -> null
    }
}

object TimerSpec : WidgetSpec {
    override val type = WidgetType.TIMER

    override fun defaults(base: WidgetDesign) = base.copy(timerMinutes = 25, accentColor = 0xFFF38BA8)

    override fun render(d: WidgetDesign, env: RenderEnv): List<Block> {
        val remaining = d.timerMinutes * 60_000L - d.totalElapsed(env.nowMs)
        return listOf(
            muted("${d.timerMinutes} dakika"),
            Block.Chrono(remaining, d.running, countDown = true, size = 40f),
            Block.Gap(8),
            Block.Group(
                listOf(
                    Block.Btn(if (d.running) "⏸ Duraklat" else "▶ Başlat", Tap("toggle")),
                    Block.Btn("↺", Tap("reset")),
                ),
                horizontal = true,
            ),
        )
    }

    override fun onAction(d: WidgetDesign, action: String, arg: Int, env: RenderEnv) = when (action) {
        "toggle" -> d.toggleRun(env.nowMs)
        "reset" -> d.resetRun()
        else -> null
    }
}
