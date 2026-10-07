package com.eren.widgetstudio.logic

import kotlin.math.PI
import kotlin.math.cos

/** Ay evresi: bilinen bir yeni aydan (2000-01-06 18:14 UTC) sinodik dönemle hesaplanır, internet gerekmez. */
object MoonPhase {
    const val SYNODIC_DAYS = 29.530588853
    private const val REF_NEW_MOON_MS = 947_182_440_000L
    private const val DAY_MS = 86_400_000.0

    data class Phase(
        val age: Double,
        val fraction: Double,
        val illumination: Double,
        val name: String,
        val emoji: String,
        val daysToFull: Double,
        val daysToNew: Double,
    )

    private val NAMES = listOf(
        "Yeni Ay" to "🌑", "Büyüyen Hilal" to "🌒", "İlk Dördün" to "🌓", "Büyüyen Şişkin Ay" to "🌔",
        "Dolunay" to "🌕", "Küçülen Şişkin Ay" to "🌖", "Son Dördün" to "🌗", "Küçülen Hilal" to "🌘",
    )

    private fun mod(x: Double, m: Double): Double = ((x % m) + m) % m

    fun at(epochMs: Long): Phase {
        val age = mod((epochMs - REF_NEW_MOON_MS) / DAY_MS, SYNODIC_DAYS)
        val f = age / SYNODIC_DAYS
        val (name, emoji) = NAMES[((f * 8) + 0.5).toInt() % 8]
        return Phase(
            age = age,
            fraction = f,
            illumination = (1 - cos(2 * PI * f)) / 2,
            name = name,
            emoji = emoji,
            daysToFull = mod(0.5 - f, 1.0) * SYNODIC_DAYS,
            daysToNew = mod(1.0 - f, 1.0) * SYNODIC_DAYS,
        )
    }
}
