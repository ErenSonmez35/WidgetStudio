package com.eren.widgetstudio

import com.eren.widgetstudio.catalog.Block
import com.eren.widgetstudio.catalog.RenderEnv
import com.eren.widgetstudio.catalog.Tap
import com.eren.widgetstudio.catalog.WidgetSpecs
import com.eren.widgetstudio.catalog.newDesign
import com.eren.widgetstudio.data.BatteryInfo
import com.eren.widgetstudio.data.DeviceInfo
import com.eren.widgetstudio.data.NetworkInfo
import com.eren.widgetstudio.data.NetworkKind
import com.eren.widgetstudio.data.RamInfo
import com.eren.widgetstudio.data.StorageInfo
import com.eren.widgetstudio.data.SystemProbe
import com.eren.widgetstudio.data.TodoItem
import com.eren.widgetstudio.data.WeatherApi
import com.eren.widgetstudio.data.WidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

private object FakeProbe : SystemProbe {
    override fun battery() = BatteryInfo(80, true, "USB", 31.5, 4.1, "İyi")
    override fun storage() = StorageInfo(64_000_000_000, 128_000_000_000)
    override fun ram() = RamInfo(3_000_000_000, 6_000_000_000)
    override fun network() = NetworkInfo(NetworkKind.WIFI, true, 300)
    override fun device() = DeviceInfo("Test Phone", "14", 34, "2025-01-05", 3_725_000, 1080, 2400)
}

private val UTC = ZoneId.of("UTC")
private fun env(date: LocalDate = LocalDate.of(2026, 10, 7), second: Int = 12 * 3600) =
    RenderEnv(date.atStartOfDay(UTC).toInstant().toEpochMilli() + second * 1000L, UTC, FakeProbe)

private fun Block.flatten(): List<Block> = listOf(this) + when (this) {
    is Block.Group -> children.flatMap { it.flatten() }
    is Block.Split -> left.flatten() + right.flatten()
    is Block.Table -> rows.flatten().flatMap { it.flatten() }
    is Block.Scroll -> children.flatMap { it.flatten() }
    else -> emptyList()
}

class CatalogTest {
    @Test fun everyTypeHasASpec() {
        assertEquals(emptyList<WidgetType>(), WidgetSpecs.missing())
    }

    @Test fun everyTypeRendersWithDefaults() {
        WidgetType.entries.forEach { type ->
            val d = newDesign(type)
            assertEquals(type, d.type)
            val blocks = WidgetSpecs.of(type).render(d, env())
            assertTrue("$type boş içerik üretti", blocks.isNotEmpty())
        }
    }

    @Test fun everyTapActionIsHandledOrIsRefresh() {
        WidgetType.entries.forEach { type ->
            val spec = WidgetSpecs.of(type)
            val d = newDesign(type)
            val taps = spec.render(d, env()).flatMap { it.flatten() }.mapNotNull {
                when (it) {
                    is Block.Group -> it.tap
                    is Block.Btn -> it.tap
                    else -> null
                }
            }
            taps.filter { it.action != "refresh" }.forEach { tap ->
                assertTrue("$type '${tap.action}' eylemini işlemiyor", spec.onAction(d, tap.action, tap.arg, env()) != null)
            }
        }
    }

    @Test fun todoToggles() {
        val d = newDesign(WidgetType.TODO).copy(todos = listOf(TodoItem("a"), TodoItem("b")))
        val after = WidgetSpecs.of(WidgetType.TODO).onAction(d, "toggle", 1, env())!!
        assertEquals(listOf(false, true), after.todos.map { it.done })
    }

    @Test fun habitResetsOnNewDay() {
        val spec = WidgetSpecs.of(WidgetType.HABIT)
        val d = newDesign(WidgetType.HABIT)
        val day1 = env(LocalDate.of(2026, 10, 7))
        val checked = spec.onAction(d, "toggle", 0, day1)!!
        assertTrue(checked.todos[0].done)
        // Ertesi gün işaret sıfırlanmış görünmeli
        val day2 = env(LocalDate.of(2026, 10, 8))
        val blocks = spec.render(checked, day2).flatMap { it.flatten() }
        assertFalse(blocks.filterIsInstance<Block.Label>().any { it.text == "☑" })
        // Ertesi gün başka bir maddeyi işaretlemek eskisini geri getirmemeli
        val next = spec.onAction(checked, "toggle", 1, day2)!!
        assertEquals(listOf(false, true, false), next.todos.map { it.done })
    }

    @Test fun counterAndDailyReset() {
        val spec = WidgetSpecs.of(WidgetType.WATER)
        val d = newDesign(WidgetType.WATER)
        val d1 = spec.onAction(d, "inc", 0, env(LocalDate.of(2026, 10, 7)))!!
        val d2 = spec.onAction(d1, "inc", 0, env(LocalDate.of(2026, 10, 7)))!!
        assertEquals(2, d2.counter)
        val nextDay = spec.onAction(d2, "inc", 0, env(LocalDate.of(2026, 10, 8)))!!
        assertEquals(1, nextDay.counter)
    }

    @Test fun counterNeverGoesNegative() {
        val spec = WidgetSpecs.of(WidgetType.COUNTER)
        val d = newDesign(WidgetType.COUNTER)
        assertEquals(0, spec.onAction(d, "dec", 0, env())!!.counter)
    }

    @Test fun stopwatchStartStopReset() {
        val spec = WidgetSpecs.of(WidgetType.STOPWATCH)
        val start = env(second = 100)
        val running = spec.onAction(newDesign(WidgetType.STOPWATCH), "toggle", 0, start)!!
        assertTrue(running.running)
        val later = RenderEnv(start.nowMs + 5_000, UTC, FakeProbe)
        val chrono = spec.render(running, later).filterIsInstance<Block.Chrono>().single()
        assertEquals(5_000, chrono.ms)
        val stopped = spec.onAction(running, "toggle", 0, later)!!
        assertFalse(stopped.running)
        assertEquals(5_000, stopped.elapsedMs)
        assertEquals(0, spec.onAction(stopped, "reset", 0, later)!!.elapsedMs)
    }

    @Test fun timerCountsDownFromMinutes() {
        val spec = WidgetSpecs.of(WidgetType.TIMER)
        val d = newDesign(WidgetType.TIMER).copy(timerMinutes = 10)
        val chrono = spec.render(d, env()).filterIsInstance<Block.Chrono>().single()
        assertEquals(600_000, chrono.ms)
        assertTrue(chrono.countDown)
    }

    @Test fun countdownTexts() {
        val spec = WidgetSpecs.of(WidgetType.COUNTDOWN)
        val d = newDesign(WidgetType.COUNTDOWN).copy(targetEpochDay = LocalDate.of(2026, 10, 17).toEpochDay())
        val labels = spec.render(d, env()).filterIsInstance<Block.Label>().map { it.text }
        assertTrue(labels.contains("10"))
        assertTrue(labels.contains("gün kaldı"))
        val today = spec.render(d.copy(targetEpochDay = env().epochDay), env()).filterIsInstance<Block.Label>().map { it.text }
        assertTrue(today.contains("Bugün!"))
    }

    @Test fun calendarHas7ColumnsAndHighlightsToday() {
        val blocks = WidgetSpecs.of(WidgetType.CALENDAR).render(newDesign(WidgetType.CALENDAR), env())
        val table = blocks.filterIsInstance<Block.Table>().single()
        assertTrue(table.rows.all { it.size == 7 })
        // Ekim 2026 Perşembe başlar; Pazartesi başlangıçlı -> 3 boşluk. 31 gün -> 5 hafta + başlık
        assertEquals(6, table.rows.size)
        val filled = table.rows.flatten().filterIsInstance<Block.Group>().filter { it.fill != null }
        assertEquals(1, filled.size)
        assertEquals("7", (filled[0].children[0] as Block.Label).text)
    }

    @Test fun systemWidgetUsesProbe() {
        val blocks = WidgetSpecs.of(WidgetType.SYSTEM).render(newDesign(WidgetType.SYSTEM), env())
        val text = blocks.flatMap { it.flatten() }.filterIsInstance<Block.Label>().map { it.text }
        assertTrue(text.any { it.contains("%80") })
        assertTrue(text.any { it.contains("64 / 128 GB") })
    }

    @Test fun diceStaysInRange() {
        val spec = WidgetSpecs.of(WidgetType.DICE)
        var d = newDesign(WidgetType.DICE)
        repeat(200) { i ->
            d = spec.onAction(d, "roll", 0, RenderEnv(1_000L * i, UTC, FakeProbe))!!
            assertTrue(d.counter in 1..6)
        }
        assertTrue(d.text.split(" ").size <= 5)
    }

    @Test fun quoteNextChangesOffset() {
        val spec = WidgetSpecs.of(WidgetType.QUOTE)
        val d = newDesign(WidgetType.QUOTE)
        assertEquals(1, spec.onAction(d, "next", 0, env())!!.counter)
    }

    @Test fun quickSettingsDefaultsToFourButtons() {
        val table = WidgetSpecs.of(WidgetType.QUICK_SETTINGS)
            .render(newDesign(WidgetType.QUICK_SETTINGS), env()).filterIsInstance<Block.Table>().single()
        assertEquals(4, table.rows.flatten().count { it is Block.Btn })
    }

    @Test fun weatherParsing() {
        val body = """
            {"current":{"temperature_2m":18.4,"apparent_temperature":17.1,"relative_humidity_2m":60,"weather_code":3,"wind_speed_10m":12.5},
             "daily":{"time":["2026-10-07","2026-10-08"],"weather_code":[3,61],"temperature_2m_max":[20.1,18.0],"temperature_2m_min":[11.0,10.5],
             "sunrise":["2026-10-07T07:01","2026-10-08T07:02"],"sunset":["2026-10-07T18:22","2026-10-08T18:20"]}}
        """.trimIndent()
        val w = WeatherApi.parseForecast(body, now = 1L)
        assertEquals(18.4, w.temp, 0.001)
        assertEquals("07:01", w.sunrise)
        assertEquals("18:22", w.sunset)
        assertEquals(2, w.daily.size)
        assertEquals(60, w.humidity)
    }

    /** Glance, bir Row/Column içinde en fazla 10 çocuğa izin verir; aşılırsa widget hiç çizilmez. */
    @Test fun noContainerExceedsGlanceChildLimit() {
        fun check(type: WidgetType, blocks: List<Block>, where: String) {
            assertTrue("$type $where: ${blocks.size} çocuk (en fazla 10)", blocks.size <= 10)
            blocks.forEach { b ->
                when (b) {
                    is Block.Group -> check(type, b.children, "Group")
                    is Block.Table -> {
                        check(type, b.rows.map { Block.Gap(0) }, "Table satırları")
                        b.rows.forEach { row -> check(type, row, "Table hücreleri") }
                        b.rows.flatten().forEach { check(type, listOf(it), "hücre") }
                    }
                    is Block.Scroll -> Unit // LazyColumn sınırsızdır
                    else -> Unit
                }
            }
        }
        WidgetType.entries.forEach { type ->
            val spec = WidgetSpecs.of(type)
            var d = newDesign(type)
            // En kalabalık yapılandırmalar
            d = when (type) {
                WidgetType.WORLD_CLOCK -> d.copy(zones = listOf("Europe/Istanbul", "Europe/London", "Asia/Tokyo", "America/New_York"))
                WidgetType.QUICK_SETTINGS -> WidgetSpecs.of(type).let {
                    com.eren.widgetstudio.catalog.QuickSettingsSpec.SHORTCUTS.fold(d) { acc, sc ->
                        acc.copy(opts = acc.opts + ("sc_${sc.key}" to true))
                    }
                }
                WidgetType.WEATHER -> d.copy(opts = d.opts + ("details" to true))
                else -> d
            }
            check(type, spec.render(d, env()), "kök")
        }
    }
}
