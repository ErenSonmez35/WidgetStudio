package com.eren.widgetstudio

import com.eren.widgetstudio.logic.DateMath
import com.eren.widgetstudio.logic.MoonPhase
import com.eren.widgetstudio.logic.Quotes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class LogicTest {
    private fun ms(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDate.of(y, m, d).atTime(h, min).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test fun moonNewAndFull() {
        val newMoon = MoonPhase.at(ms(2000, 1, 6, 18, 14))
        assertEquals(0.0, newMoon.illumination, 0.01)
        assertEquals("Yeni Ay", newMoon.name)
        val full = MoonPhase.at(ms(2000, 1, 21, 4, 40))
        assertTrue(full.illumination > 0.98)
        assertEquals("Dolunay", full.name)
        // 2024-04-08 yeni ay (tam güneş tutulması) ~18:21 UTC
        assertTrue(MoonPhase.at(ms(2024, 4, 8, 18, 21)).illumination < 0.02)
    }

    @Test fun moonBeforeReferenceStillValid() {
        val p = MoonPhase.at(ms(1990, 5, 5, 0, 0))
        assertTrue(p.age in 0.0..MoonPhase.SYNODIC_DAYS)
    }

    @Test fun ageAndBirthday() {
        val birth = LocalDate.of(2000, 2, 29)
        val today = LocalDate.of(2026, 10, 7)
        val age = DateMath.age(birth, today)
        assertEquals(26, age.years)
        assertEquals(DateMath.nextBirthday(birth, today), LocalDate.of(2027, 2, 28))
        assertEquals(LocalDate.of(2026, 10, 7), DateMath.nextBirthday(LocalDate.of(1990, 10, 7), today))
    }

    @Test fun progressBounds() {
        val p = DateMath.progress(LocalDate.of(2026, 10, 5), 12 * 3600) // Pazartesi öğlen
        assertEquals(0.5f, p.day, 0.001f)
        assertEquals(0.5f / 7, p.week, 0.001f)
        val ny = DateMath.progress(LocalDate.of(2026, 1, 1), 0)
        assertEquals(0f, ny.year, 0f)
    }

    @Test fun durations() {
        assertEquals("1:02:05", DateMath.formatDuration(3_725_000))
        assertEquals("1:05", DateMath.formatDuration(65_000))
        assertEquals("-0:05", DateMath.formatDuration(-5_000))
        assertEquals(418, DateMath.minutesOfDay("06:58"))
        assertEquals(null, DateMath.minutesOfDay("abc"))
    }

    @Test fun quotesPool() {
        assertEquals(Quotes.BUILT_IN, Quotes.pool("  \n "))
        assertEquals(listOf("a", "b"), Quotes.pool("a\n\n b "))
        assertEquals("b", Quotes.pick(listOf("a", "b"), 3, 0))
        assertEquals("a", Quotes.pick(listOf("a", "b"), 3, 1))
        assertEquals("b", Quotes.pick(listOf("a", "b"), 0, -1)) // negatif ofset
    }
}
