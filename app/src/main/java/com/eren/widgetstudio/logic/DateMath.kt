package com.eren.widgetstudio.logic

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** Tarih hesapları. Android'e bağımlı değil, JVM'de test edilir. */
object DateMath {

    fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)

    data class Age(val years: Int, val months: Int, val days: Int)

    fun age(birth: LocalDate, today: LocalDate): Age {
        val p = Period.between(birth, if (today.isBefore(birth)) birth else today)
        return Age(p.years, p.months, p.days)
    }

    /** Bugün dahil, bir sonraki doğum günü (29 Şubat doğumlular için 28 Şubat'a düşer). */
    fun nextBirthday(birth: LocalDate, today: LocalDate): LocalDate {
        val thisYear = birth.withYear(today.year)
        return if (thisYear.isBefore(today)) birth.withYear(today.year + 1) else thisYear
    }

    /** Günün, haftanın (Pazartesi başlangıçlı), ayın ve yılın ne kadarının geçtiği (0..1). */
    data class Progress(val day: Float, val week: Float, val month: Float, val year: Float)

    fun progress(date: LocalDate, secondOfDay: Int): Progress {
        val dayF = (secondOfDay / 86_400f).coerceIn(0f, 1f)
        return Progress(
            day = dayF,
            week = (((date.dayOfWeek.value - 1) + dayF) / 7f).coerceIn(0f, 1f),
            month = (((date.dayOfMonth - 1) + dayF) / date.lengthOfMonth()).coerceIn(0f, 1f),
            year = (((date.dayOfYear - 1) + dayF) / date.lengthOfYear()).coerceIn(0f, 1f),
        )
    }

    /** 3725000 ms -> "1:02:05"; 65000 -> "1:05"; negatif değerlerde başa "-" konur. */
    fun formatDuration(ms: Long): String {
        val negative = ms < 0
        val totalSec = Math.abs(ms) / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        val body = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
        return if (negative) "-$body" else body
    }

    /** "06:58" -> 418 dakika; geçersizse null. */
    fun minutesOfDay(hhmm: String): Int? {
        val parts = hhmm.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }
}
